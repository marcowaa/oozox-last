const mongoose = require('mongoose');
const Invoice = require('../models/Invoice');
const User = require('../models/User');
const { createNotification } = require('../services/notificationService');
const { successResponse, errorResponse } = require('../utils/response');
const { escapeRegex } = require('../middlewares/sanitizeMiddleware');

const ALLOWED_STATUS_TRANSITIONS = {
  PENDING: new Set(['PAID', 'OVERDUE', 'CANCELLED']),
  OVERDUE: new Set(['PAID', 'CANCELLED']),
  PAID: new Set([]),
  CANCELLED: new Set([]),
};

// Helper to generate a unique invoice number
const generateInvoiceNumber = async () => {
  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const randomSuffix = Math.floor(1000 + Math.random() * 9000);
  const invoiceNumber = `INV-${dateStr}-${randomSuffix}`;
  const exists = await Invoice.findOne({ invoiceNumber });
  if (exists) {
    return generateInvoiceNumber();
  }
  return invoiceNumber;
};

// @desc    Create a new Invoice
// @route   POST /api/invoices
// @access  Private
const createInvoice = async (req, res, next) => {
  try {
    const senderId = req.user._id;
    const { receiverId, items, taxAmount, discount, paymentMethod, dueDate, notes } = req.body;

    if (!mongoose.Types.ObjectId.isValid(receiverId)) {
      return errorResponse(res, 400, 'معرّف الطرف المستلم غير صالح');
    }

    if (senderId.toString() === receiverId.toString()) {
      return errorResponse(res, 400, 'لا يمكنك إصدار فاتورة لنفسك (Cannot invoice yourself)');
    }

    const receiver = await User.findById(receiverId);
    if (!receiver) {
      return errorResponse(res, 404, 'المستخدم المستلم المحدد غير مسجل في النظام');
    }

    const invoiceNumber = await generateInvoiceNumber();

    const invoice = new Invoice({
      invoiceNumber,
      sender: senderId,
      receiver: receiverId,
      items,
      taxAmount: taxAmount || 0,
      discount: discount || 0,
      paymentMethod: paymentMethod || 'CASH',
      dueDate,
      notes: notes || '',
    });

    await invoice.save();

    await invoice.populate('sender', 'name phone email role avatar');
    await invoice.populate('receiver', 'name phone email role avatar');

    await createNotification({
      recipientId: receiver._id,
      senderId: req.user._id,
      title: 'فاتورة جديدة واردة 📄',
      message: `أصدر لك ${req.user.name} فاتورة جديدة برقم ${invoice.invoiceNumber} بقيمة إجمالية ${invoice.totalAmount} ج.م`,
      type: 'INVOICE_CREATED',
      relatedInvoiceId: invoice._id,
    });

    return successResponse(res, 201, 'تم إنشاء الفاتورة وإرسال الإشعار بنجاح', { invoice });
  } catch (error) {
    next(error);
  }
};

// @desc    Get all invoices for current user (sent or received)
// @route   GET /api/invoices
// @access  Private
const getInvoices = async (req, res, next) => {
  try {
    const userId = req.user._id;
    const { role, status, page = 1, limit = 20, search } = req.query;

    const query = {
      $or: [{ sender: userId }, { receiver: userId }],
    };

    if (role === 'sent') {
      delete query.$or;
      query.sender = userId;
    } else if (role === 'received') {
      delete query.$or;
      query.receiver = userId;
    }

    if (status && ['PENDING', 'PAID', 'OVERDUE', 'CANCELLED'].includes(status.toUpperCase())) {
      query.status = status.toUpperCase();
    }

    if (typeof search === 'string' && search.trim()) {
      const safePattern = escapeRegex(search.trim().slice(0, 64));
      query.invoiceNumber = new RegExp(safePattern, 'i');
    }

    const pageNum = Math.max(1, parseInt(page, 10) || 1);
    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10) || 20));
    const skip = (pageNum - 1) * limitNum;

    const total = await Invoice.countDocuments(query);
    const invoices = await Invoice.find(query)
      .populate('sender', 'name phone email avatar')
      .populate('receiver', 'name phone email avatar')
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limitNum);

    return successResponse(res, 200, 'تم جلب الفواتير بنجاح', {
      invoices,
      pagination: {
        total,
        page: pageNum,
        pages: Math.ceil(total / limitNum),
        limit: limitNum,
      },
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get single invoice by ID
// @route   GET /api/invoices/:id
// @access  Private
const getInvoiceById = async (req, res, next) => {
  try {
    const invoice = await Invoice.findById(req.params.id)
      .populate('sender', 'name phone email avatar')
      .populate('receiver', 'name phone email avatar');

    if (!invoice) {
      return errorResponse(res, 404, 'الفاتورة المطلوبة غير موجودة');
    }

    const userId = req.user._id.toString();
    const isSender = invoice.sender._id.toString() === userId;
    const isReceiver = invoice.receiver._id.toString() === userId;
    const isAdmin = req.user.role === 'ADMIN';

    if (!isSender && !isReceiver && !isAdmin) {
      return errorResponse(res, 403, 'غير مصرح لك بالوصول إلى بيانات هذه الفاتورة (Forbidden)');
    }

    return successResponse(res, 200, 'تم جلب بيانات الفاتورة', { invoice });
  } catch (error) {
    next(error);
  }
};

// @desc    Update invoice status (e.g. PAID, OVERDUE, CANCELLED)
// @route   PATCH /api/invoices/:id/status
// @access  Private
const updateInvoiceStatus = async (req, res, next) => {
  try {
    const { status } = req.body;
    const invoice = await Invoice.findById(req.params.id)
      .populate('sender', 'name phone')
      .populate('receiver', 'name phone');

    if (!invoice) {
      return errorResponse(res, 404, 'الفاتورة المطلوبة غير موجودة');
    }

    const userId = req.user._id.toString();
    const isSender = invoice.sender._id.toString() === userId;
    const isReceiver = invoice.receiver._id.toString() === userId;
    const isAdmin = req.user.role === 'ADMIN';

    if (!isSender && !isReceiver && !isAdmin) {
      return errorResponse(res, 403, 'غير مصرح لك بتعديل حالة هذه الفاتورة');
    }

    // Strict financial state machine: prevent reverting PAID / CANCELLED invoices
    if (invoice.status === status) {
      return successResponse(res, 200, 'حالة الفاتورة محدثة مسبقاً', { invoice });
    }

    const allowedNext = ALLOWED_STATUS_TRANSITIONS[invoice.status] || new Set();
    if (!allowedNext.has(status) && !isAdmin) {
      return errorResponse(
        res,
        400,
        `لا يمكن تغيير حالة الفاتورة من ${invoice.status} إلى ${status} حفاظاً على السجل المالي`
      );
    }

    invoice.status = status;
    await invoice.save();

    const targetRecipientId = isSender ? invoice.receiver._id : invoice.sender._id;
    const statusArabic =
      status === 'PAID'
        ? 'مدفوعة ومسددة بالكامل ✅'
        : status === 'OVERDUE'
        ? 'متأخرة عن السداد ⚠️'
        : status === 'CANCELLED'
        ? 'ملغاة ❌'
        : 'قيد الانتظار ⏳';

    await createNotification({
      recipientId: targetRecipientId,
      senderId: req.user._id,
      title: 'تحديث حالة الفاتورة 🔄',
      message: `تم تحديث حالة الفاتورة (${invoice.invoiceNumber}) إلى: ${statusArabic}`,
      type: 'INVOICE_PAID',
      relatedInvoiceId: invoice._id,
    });

    return successResponse(res, 200, 'تم تحديث حالة الفاتورة بنجاح', { invoice });
  } catch (error) {
    next(error);
  }
};

// @desc    Delete invoice (Only sender if PENDING/OVERDUE, or ADMIN)
// @route   DELETE /api/invoices/:id
// @access  Private
const deleteInvoice = async (req, res, next) => {
  try {
    const invoice = await Invoice.findById(req.params.id);

    if (!invoice) {
      return errorResponse(res, 404, 'الفاتورة المطلوبة غير موجودة');
    }

    const userId = req.user._id.toString();
    const isSender = invoice.sender.toString() === userId;
    const isAdmin = req.user.role === 'ADMIN';

    if (!isSender && !isAdmin) {
      return errorResponse(res, 403, 'فقط منشئ الفاتورة أو مدير النظام يمكنه حذفها');
    }

    if (invoice.status === 'PAID' && !isAdmin) {
      return errorResponse(res, 400, 'لا يمكن حذف فاتورة مسددة بالكامل حفاظاً على السجلات المالية');
    }

    await Invoice.findByIdAndDelete(req.params.id);

    return successResponse(res, 200, 'تم حذف الفاتورة بنجاح');
  } catch (error) {
    next(error);
  }
};

module.exports = {
  createInvoice,
  getInvoices,
  getInvoiceById,
  updateInvoiceStatus,
  deleteInvoice,
};
