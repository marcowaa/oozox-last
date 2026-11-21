const { z } = require('zod');
const { errorResponse } = require('../utils/response');
const { resolveRequestedLang, t, SUPPORTED_LANGUAGE_CODES } = require('../i18n');

const OBJECT_ID_REGEX = /^[0-9a-fA-F]{24}$/;

/**
 * Universal Zod schema validation middleware for Express with server i18n
 * @param {z.ZodSchema} schema - Zod validation schema
 * @param {'body' | 'query' | 'params'} source - Request property to validate (default: 'body')
 */
const validate = (schema, source = 'body') => {
  return (req, res, next) => {
    const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);
    try {
      const result = schema.safeParse(req[source]);
      if (!result.success) {
        const errorDetails = result.error.errors.map((issue) => ({
          field: issue.path.join('.'),
          message: issue.message.startsWith('api.')
            ? translate(issue.message)
            : issue.message,
          code: issue.code,
        }));
        return errorResponse(res, 400, translate('api.validation_error'), errorDetails);
      }
      req[source] = result.data;
      next();
    } catch (err) {
      return errorResponse(res, 500, translate('api.validation_internal_error'), err.message);
    }
  };
};

const schemas = {
  // Common route parameter validation
  objectIdParam: z.object({
    id: z.string().regex(OBJECT_ID_REGEX, 'api.val_invalid_object_id'),
  }),

  // Auth Schemas: Public registration NEVER allows ADMIN privilege escalation
  register: z.object({
    name: z.string().trim().min(2, 'api.val_name_min').max(100),
    email: z.string().trim().toLowerCase().email('api.val_invalid_email').max(150),
    phone: z.string().trim().min(8, 'api.val_phone_min').max(20),
    password: z
      .string()
      .min(6, 'api.val_password_min')
      .max(128, 'api.val_password_max'),
    role: z
      .enum(['USER', 'MERCHANT'], {
        errorMap: () => ({ message: 'api.val_invalid_role' }),
      })
      .default('USER'),
    preferredLanguage: z
      .enum(SUPPORTED_LANGUAGE_CODES)
      .optional(),
  }),

  login: z.object({
    emailOrPhone: z.string().trim().min(1, 'api.val_email_or_phone_required').max(150),
    password: z.string().min(1, 'api.val_password_required').max(128),
  }),

  updateLanguage: z.object({
    language: z.enum(SUPPORTED_LANGUAGE_CODES, {
      errorMap: () => ({ message: 'api.i18n_unsupported_lang' }),
    }),
  }),

  searchUsersQuery: z.object({
    q: z.string().trim().max(64, 'api.val_search_too_long').optional().default(''),
  }),

  // Invoice Schemas
  createInvoice: z.object({
    receiverId: z.string().regex(OBJECT_ID_REGEX, 'api.val_invalid_receiver_id'),
    items: z
      .array(
        z.object({
          name: z.string().trim().min(1, 'api.val_item_name_required').max(150),
          quantity: z.coerce
            .number()
            .finite('api.val_quantity_invalid')
            .positive('api.val_quantity_positive')
            .max(1_000_000, 'api.val_quantity_max'),
          unitPrice: z.coerce
            .number()
            .finite('api.val_price_invalid')
            .min(0, 'api.val_price_non_negative')
            .max(1_000_000_000, 'api.val_price_max'),
          karat: z.coerce
            .number()
            .refine((val) => [0, 14, 18, 21, 24].includes(val), {
              message: 'api.val_karat_invalid',
            })
            .default(0),
        })
      )
      .min(1, 'api.val_items_min')
      .max(50, 'api.val_items_max'),
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
      errorMap: () => ({ message: 'api.val_invoice_status_invalid' }),
    }),
  }),
};

module.exports = {
  validate,
  schemas,
};
