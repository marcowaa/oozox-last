import * as zod from "zod";

// --- Auth Schemas ---
export const RegisterRequestSchema = zod.object({
  name: zod.string().trim().min(2, "Name must be at least 2 characters").max(100),
  email: zod.string().email("Please provide a valid email address"),
  phone: zod.string().trim().min(8, "Phone number must be at least 8 digits").max(20),
  password: zod.string().min(6, "Password must be at least 6 characters"),
  role: zod.enum(["USER", "ADMIN", "MERCHANT"]).default("USER"),
});

export const LoginRequestSchema = zod.object({
  emailOrPhone: zod.string().min(1, "Email or phone number is required"),
  password: zod.string().min(1, "Password is required"),
});

// --- Invoice Schemas ---
export const InvoiceItemSchema = zod.object({
  name: zod.string().min(1, "Item name is required"),
  quantity: zod.number().positive("Quantity must be greater than zero").default(1),
  unitPrice: zod.number().nonnegative("Unit price cannot be negative"),
  karat: zod.enum(["0", "14", "18", "21", "24"]).or(zod.number().int()).default(0),
});

export const CreateInvoiceRequestSchema = zod.object({
  receiverId: zod.string().min(1, "Receiver ID is required"),
  items: zod.array(InvoiceItemSchema).min(1, "Invoice must contain at least one item"),
  taxAmount: zod.number().nonnegative().default(0),
  discount: zod.number().nonnegative().default(0),
  paymentMethod: zod
    .enum(["CASH", "BANK_TRANSFER", "INSTAPAY", "VODAFONE_CASH", "GOLD_SETTLEMENT"])
    .default("CASH"),
  dueDate: zod.string().or(zod.date()),
  notes: zod.string().max(500).nullish().default(""),
});

export const UpdateInvoiceStatusRequestSchema = zod.object({
  status: zod.enum(["PENDING", "PAID", "OVERDUE", "CANCELLED"]),
});

export const HealthCheckResponse = zod.object({
  status: zod.string(),
});
