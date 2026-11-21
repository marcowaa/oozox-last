const mongoose = require('mongoose');
const Invoice = require('../models/Invoice');
const User = require('../models/User');
const { createNotification } = require('../services/notificationService');
const { successResponse, errorResponse } = require('../utils/response');
const { escapeRegex } = require('../middlewares/sanitizeMiddleware');
const { t } = require('../i18n');

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
      return errorResponse(res, 400, req.t('api.val_invalid_receiver_id'));
    }

    if (senderId.toString() === receiverId.toString()) {
      return errorResponse(res, 400, req.t('api.cannot_invoice_self'));
    }

    const receiver = await User.findById(receiverId);
    if (!receiver) {
      return errorResponse(res, 404, req.t('api.receiver_not_found'));
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

    await invoice.populate('sender', 'name phone email role avatar preferredLanguage');
    await invoice.populate('receiver', 'name phone email role avatar preferredLanguage');

    const recipientLang = receiver.preferredLanguage || req.lang || 'ar';
    await createNotification({
      recipientId: receiver._id,
      senderId: req.user._id,
      title: t(recipientLang, 'api.notif_invoice_created_title'),
      message: t(recipientLang, 'api.notif_invoice_created_body', {
        sender: req.user.name,
        invoiceNumber: invoice.invoiceNumber,
        amount: invoice.totalAmount,
      }),
      type: 'INVOICE_CREATED',
      relatedInvoiceId: invoice._id,
      titleKey: 'api.notif_invoice_created_title',
      messageKey: 'api.notif_invoice_created_body',
      messageParams: {
        sender: req.user.name,
        invoiceNumber: invoice.invoiceNumber,
        amount: invoice.totalAmount,
      },
      language: recipientLang,
    });

    return successResponse(res, 201, req.t('api.invoice_created'), { invoice });
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

    return successResponse(res, 200, req.t('api.invoices_fetched'), {
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
      return errorResponse(res, 404, req.t('api.invoice_not_found'));
    }

    const userId = req.user._id.toString();
    const isSender = invoice.sender._id.toString() === userId;
    const isReceiver = invoice.receiver._id.toString() === userId;
    const isAdmin = req.user.role === 'ADMIN';

    if (!isSender && !isReceiver && !isAdmin) {
      return errorResponse(res, 403, req.t('api.invoice_forbidden_view'));
    }

    return successResponse(res, 200, req.t('api.invoice_fetched'), { invoice });
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
      .populate('sender', 'name phone preferredLanguage')
      .populate('receiver', 'name phone preferredLanguage');

    if (!invoice) {
      return errorResponse(res, 404, req.t('api.invoice_not_found'));
    }

    const userId = req.user._id.toString();
    const isSender = invoice.sender._id.toString() === userId;
    const isReceiver = invoice.receiver._id.toString() === userId;
    const isAdmin = req.user.role === 'ADMIN';

    if (!isSender && !isReceiver && !isAdmin) {
      return errorResponse(res, 403, req.t('api.invoice_forbidden_update'));
    }

    // Strict financial state machine: prevent reverting PAID / CANCELLED invoices
    if (invoice.status === status) {
      return successResponse(res, 200, req.t('api.invoice_status_unchanged'), { invoice });
    }

    const allowedNext = ALLOWED_STATUS_TRANSITIONS[invoice.status] || new Set();
    if (!allowedNext.has(status) && !isAdmin) {
      return errorResponse(
        res,
        400,
        req.t('api.invoice_invalid_transition', { from: invoice.status, to: status })
      );
    }

    invoice.status = status;
    await invoice.save();

    const targetRecipient = isSender ? invoice.receiver : invoice.sender;
    const recipientLang = targetRecipient.preferredLanguage || req.lang || 'ar';
    const statusKey =
      status === 'PAID'
        ? 'api.invoice_status_paid'
        : status === 'OVERDUE'
        ? 'api.invoice_status_overdue'
        : status === 'CANCELLED'
        ? 'api.invoice_status_cancelled'
        : 'api.invoice_status_pending';

    const localizedStatusLabel = t(recipientLang, statusKey);
    const notifType =
      status === 'PAID'
        ? 'INVOICE_PAID'
        : status === 'OVERDUE'
        ? 'INVOICE_OVERDUE'
        : status === 'CANCELLED'
        ? 'INVOICE_CANCELLED'
        : 'INVOICE_CREATED';

    await createNotification({
      recipientId: targetRecipient._id,
      senderId: req.user._id,
      title: t(recipientLang, 'api.notif_invoice_status_title'),
      message: t(recipientLang, 'api.notif_invoice_status_body', {
        invoiceNumber: invoice.invoiceNumber,
        status: localizedStatusLabel,
      }),
      type: notifType,
      relatedInvoiceId: invoice._id,
      titleKey: 'api.notif_invoice_status_title',
      messageKey: 'api.notif_invoice_status_body',
      messageParams: {
        invoiceNumber: invoice.invoiceNumber,
        statusKey,
      },
      language: recipientLang,
    });

    return successResponse(res, 200, req.t('api.invoice_status_updated'), { invoice });
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
      return errorResponse(res, 404, req.t('api.invoice_not_found'));
    }

    const userId = req.user._id.toString();
    const isSender = invoice.sender.toString() === userId;
    const isAdmin = req.user.role === 'ADMIN';

    if (!isSender && !isAdmin) {
      return errorResponse(res, 403, req.t('api.invoice_forbidden_delete'));
    }

    if (invoice.status === 'PAID' && !isAdmin) {
      return errorResponse(res, 400, req.t('api.invoice_cannot_delete_paid'));
    }

    await Invoice.findByIdAndDelete(req.params.id);

    return successResponse(res, 200, req.t('api.invoice_deleted'));
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
