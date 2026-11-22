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

  // Business Setup Schemas
  upsertBusinessProfile: z.object({
    companyName: z.string().trim().min(2).max(140),
    activityType: z.string().trim().min(2).max(80),
    customActivityLabel: z.string().trim().max(120).optional().default(''),
    commercialRegistration: z.string().trim().max(60).optional().default(''),
    taxNumber: z.string().trim().max(60).optional().default(''),
    phone: z.string().trim().max(30).optional().default(''),
    address: z.string().trim().max(250).optional().default(''),
    currency: z.string().trim().min(1).max(12).optional().default('EGP'),
    workStartTime: z.string().trim().max(20).optional().default('09:00'),
    workEndTime: z.string().trim().max(20).optional().default('17:00'),
    hqLatitude: z.coerce.number().min(-90).max(90).nullable().optional(),
    hqLongitude: z.coerce.number().min(-180).max(180).nullable().optional(),
    geofenceRadiusMeters: z.coerce.number().int().min(50).max(10000).optional().default(250),
    companyGroupName: z.string().trim().max(140).optional(),
  }),

  // HR & Employee Schemas
  createEmployee: z.object({
    fullName: z.string().trim().min(2).max(120),
    username: z.string().trim().min(2).max(60),
    phone: z.string().trim().max(30).optional().default(''),
    password: z.string().min(4).max(128),
    roleTitle: z.string().trim().min(2).max(80).optional().default('موظف'),
    department: z.string().trim().max(80).optional().default('العمليات'),
    baseSalary: z.coerce.number().finite().min(0).max(100_000_000).optional().default(0),
    salaryCurrency: z.string().trim().max(12).optional().default('EGP'),
    geofenceEnforced: z.boolean().optional().default(true),
    permissions: z
      .object({
        manageBusiness: z.boolean().optional(),
        manageStaff: z.boolean().optional(),
        managePayroll: z.boolean().optional(),
        manageProjects: z.boolean().optional(),
        assignTasks: z.boolean().optional(),
        manageAccounting: z.boolean().optional(),
        createInvoices: z.boolean().optional(),
        approveDiscounts: z.boolean().optional(),
        viewReports: z.boolean().optional(),
        directChat: z.boolean().optional(),
      })
      .optional(),
  }),

  updateEmployee: z.object({
    fullName: z.string().trim().min(2).max(120).optional(),
    phone: z.string().trim().max(30).optional(),
    password: z.string().min(4).max(128).optional(),
    roleTitle: z.string().trim().min(2).max(80).optional(),
    department: z.string().trim().max(80).optional(),
    baseSalary: z.coerce.number().finite().min(0).max(100_000_000).optional(),
    salaryCurrency: z.string().trim().max(12).optional(),
    geofenceEnforced: z.boolean().optional(),
    isActive: z.boolean().optional(),
    permissions: z
      .object({
        manageBusiness: z.boolean().optional(),
        manageStaff: z.boolean().optional(),
        managePayroll: z.boolean().optional(),
        manageProjects: z.boolean().optional(),
        assignTasks: z.boolean().optional(),
        manageAccounting: z.boolean().optional(),
        createInvoices: z.boolean().optional(),
        approveDiscounts: z.boolean().optional(),
        viewReports: z.boolean().optional(),
        directChat: z.boolean().optional(),
      })
      .optional(),
  }),

  employeeLogin: z.object({
    username: z.string().trim().min(1).max(60),
    password: z.string().min(1).max(128),
    businessId: z.string().regex(OBJECT_ID_REGEX).optional(),
  }),

  createPayrollAdjustment: z.object({
    employeeId: z.string().regex(OBJECT_ID_REGEX, 'api.val_invalid_object_id'),
    type: z.enum(['BONUS', 'DEDUCTION']),
    amount: z.coerce.number().finite().positive().max(100_000_000),
    reason: z.string().trim().min(2).max(250),
    effectiveDate: z.string().trim().min(8).max(20).optional(),
  }),

  createEmployeeLeave: z.object({
    employeeId: z.string().regex(OBJECT_ID_REGEX, 'api.val_invalid_object_id'),
    leaveType: z.enum(['ANNUAL', 'SICK', 'EMERGENCY', 'UNPAID']).default('ANNUAL'),
    startDate: z.string().trim().min(8).max(20),
    endDate: z.string().trim().min(8).max(20),
    notes: z.string().trim().max(300).optional().default(''),
  }),

  attendanceGeoReport: z.object({
    employeeId: z.string().regex(OBJECT_ID_REGEX).optional(),
    gpsEnabled: z.boolean(),
    latitude: z.coerce.number().min(-90).max(90).nullable().optional(),
    longitude: z.coerce.number().min(-180).max(180).nullable().optional(),
  }),

  // Task Assignment Schemas (supports NOW, HOURS, DAYS without artificial 4h cap)
  createWorkTask: z.object({
    projectId: z.string().trim().min(1).max(120),
    projectName: z.string().trim().min(1).max(140),
    workGroupId: z.string().trim().max(120).optional().default('wg_company_main'),
    title: z.string().trim().min(2).max(160),
    description: z.string().trim().max(1200).optional().default(''),
    timingMode: z.enum(['NOW', 'HOURS', 'DAYS']),
    durationValue: z.coerce.number().int().min(0).max(3650).default(1),
    assignees: z.array(z.string().trim().min(1).max(140)).min(1).max(50),
    priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']).optional().default('HIGH'),
    financialAmount: z.coerce.number().finite().min(0).max(1_000_000_000).optional().default(0),
    financialNotes: z.string().trim().max(300).optional().default(''),
  }),

  updateWorkTaskStatus: z.object({
    status: z.enum(['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']),
  }),
};

module.exports = {
  validate,
  schemas,
};
