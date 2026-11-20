const { z } = require('zod');
const { errorResponse } = require('../utils/response');

const OBJECT_ID_REGEX = /^[0-9a-fA-F]{24}$/;

/**
 * Universal Zod schema validation middleware for Express
 * @param {z.ZodSchema} schema - Zod validation schema
 * @param {'body' | 'query' | 'params'} source - Request property to validate (default: 'body')
 */
const validate = (schema, source = 'body') => {
  return (req, res, next) => {
    try {
      const result = schema.safeParse(req[source]);
      if (!result.success) {
        const errorDetails = result.error.errors.map((issue) => ({
          field: issue.path.join('.'),
          message: issue.message,
          code: issue.code,
        }));
        return errorResponse(res, 400, 'بيانات الإدخال غير صالحة (Validation Error)', errorDetails);
      }
      req[source] = result.data;
      next();
    } catch (err) {
      return errorResponse(res, 500, 'خطأ أثناء التحقق من صحة البيانات', err.message);
    }
  };
};

const schemas = {
  // Common route parameter validation
  objectIdParam: z.object({
    id: z.string().regex(OBJECT_ID_REGEX, 'معرّف غير صالح (Invalid ObjectId format)'),
  }),

  // Auth Schemas: Public registration NEVER allows ADMIN privilege escalation
  register: z.object({
    name: z.string().trim().min(2, 'الاسم يجب أن يكون حرفين على الأقل').max(100),
    email: z.string().trim().toLowerCase().email('يرجى إدخال بريد إلكتروني صالح').max(150),
    phone: z.string().trim().min(8, 'رقم الهاتف يجب ألا يقل عن 8 أرقام').max(20),
    password: z
      .string()
      .min(6, 'كلمة المرور يجب ألا تقل عن 6 أحرف')
      .max(128, 'كلمة المرور طويلة جداً'),
    role: z
      .enum(['USER', 'MERCHANT'], {
        errorMap: () => ({ message: 'الدور المسموح للتسجيل العام هو USER أو MERCHANT فقط' }),
      })
      .default('USER'),
  }),

  login: z.object({
    emailOrPhone: z.string().trim().min(1, 'البريد الإلكتروني أو رقم الهاتف مطلوب').max(150),
    password: z.string().min(1, 'كلمة المرور مطلوبة').max(128),
  }),

  searchUsersQuery: z.object({
    q: z.string().trim().max(64, 'نص البحث طويل جداً').optional().default(''),
  }),

  // Invoice Schemas
  createInvoice: z.object({
    receiverId: z.string().regex(OBJECT_ID_REGEX, 'معرّف الطرف المستلم غير صالح'),
    items: z
      .array(
        z.object({
          name: z.string().trim().min(1, 'اسم البند مطلوب').max(150),
          quantity: z.coerce
            .number()
            .finite('الكمية غير صالحة')
            .positive('الكمية يجب أن تكون أكبر من صفر')
            .max(1_000_000, 'الكمية تتجاوز الحد المسموح'),
          unitPrice: z.coerce
            .number()
            .finite('السعر غير صالح')
            .min(0, 'السعر الإفرادي لا يمكن أن يكون سالباً')
            .max(1_000_000_000, 'السعر يتجاوز الحد المسموح'),
          karat: z.coerce
            .number()
            .refine((val) => [0, 14, 18, 21, 24].includes(val), {
              message: 'عيار الذهب غير صالح (يجب أن يكون 14 أو 18 أو 21 أو 24 أو 0)',
            })
            .default(0),
        })
      )
      .min(1, 'يجب أن تحتوي الفاتورة على بند واحد على الأقل')
      .max(50, 'الحد الأقصى لبنود الفاتورة هو 50 بنداً'),
    taxAmount: z.coerce.number().finite().min(0).max(1_000_000_000).default(0),
    discount: z.coerce.number().finite().min(0).max(1_000_000_000).default(0),
    paymentMethod: z
      .enum(['CASH', 'BANK_TRANSFER', 'INSTAPAY', 'VODAFONE_CASH', 'GOLD_SETTLEMENT'])
      .default('CASH'),
    dueDate: z.string().min(1).or(z.date()),
    notes: z.string().trim().max(500).nullish().default(''),
  }),

  listInvoicesQuery: z.object({
    role: z.enum(['sent', 'received', 'all']).optional(),
    status: z.string().trim().max(20).optional(),
    page: z.coerce.number().int().min(1).max(10000).default(1),
    limit: z.coerce.number().int().min(1).max(100).default(20),
    search: z.string().trim().max(64).optional(),
  }),

  updateInvoiceStatus: z.object({
    status: z.enum(['PENDING', 'PAID', 'OVERDUE', 'CANCELLED'], {
      errorMap: () => ({ message: 'حالة الفاتورة غير صالحة' }),
    }),
  }),
};

module.exports = {
  validate,
  schemas,
};
