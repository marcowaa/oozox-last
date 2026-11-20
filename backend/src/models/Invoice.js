const mongoose = require('mongoose');
const { encryptField, decryptField } = require('../utils/fieldCrypto');

const invoiceItemSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Item name/description is required'],
      trim: true,
    },
    quantity: {
      type: Number,
      required: [true, 'Item quantity is required'],
      min: [0.01, 'Quantity must be greater than zero'],
      default: 1,
    },
    unitPrice: {
      type: Number,
      required: [true, 'Item unit price is required'],
      min: [0, 'Unit price cannot be negative'],
    },
    total: {
      type: Number,
      required: true,
    },
    karat: {
      type: Number,
      enum: [0, 14, 18, 21, 24],
      default: 0, // 0 for standard non-gold items
    },
  },
  { _id: false }
);

const invoiceSchema = new mongoose.Schema(
  {
    invoiceNumber: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      index: true,
    },
    sender: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: [true, 'Invoice sender is required'],
      index: true,
    },
    receiver: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: [true, 'Invoice receiver is required'],
      index: true,
    },
    items: {
      type: [invoiceItemSchema],
      validate: [
        (val) => val && val.length > 0,
        'Invoice must contain at least one item',
      ],
    },
    subTotal: {
      type: Number,
      required: true,
      min: [0, 'Subtotal cannot be negative'],
    },
    taxAmount: {
      type: Number,
      default: 0,
      min: [0, 'Tax amount cannot be negative'],
    },
    discount: {
      type: Number,
      default: 0,
      min: [0, 'Discount cannot be negative'],
    },
    totalAmount: {
      type: Number,
      required: true,
      min: [0, 'Total amount cannot be negative'],
    },
    status: {
      type: String,
      enum: ['PENDING', 'PAID', 'OVERDUE', 'CANCELLED'],
      default: 'PENDING',
      index: true,
    },
    paymentMethod: {
      type: String,
      enum: ['CASH', 'BANK_TRANSFER', 'INSTAPAY', 'VODAFONE_CASH', 'GOLD_SETTLEMENT'],
      default: 'CASH',
    },
    dueDate: {
      type: Date,
      required: [true, 'Due date is required'],
    },
    notes: {
      type: String,
      trim: true,
      maxlength: [500, 'Notes cannot exceed 500 characters'],
    },
  },
  {
    timestamps: true,
  }
);

// Pre-save hook to calculate item totals and invoice numbers if needed
invoiceSchema.pre('validate', function (next) {
  if (this.items && this.items.length > 0) {
    let calculatedSubTotal = 0;
    this.items.forEach((item) => {
      item.total = Number((item.quantity * item.unitPrice).toFixed(2));
      calculatedSubTotal += item.total;
    });
    this.subTotal = Number(calculatedSubTotal.toFixed(2));
    const calculatedTotal = this.subTotal + (this.taxAmount || 0) - (this.discount || 0);
    this.totalAmount = Number(Math.max(0, calculatedTotal).toFixed(2));
  }
  next();
});

// Transparent AES-256-GCM field-level encryption at rest for sensitive invoice notes
invoiceSchema.pre('save', function (next) {
  if (this.notes) {
    this.notes = encryptField(this.notes);
  }
  next();
});

invoiceSchema.post('save', function (doc) {
  if (doc && doc.notes) {
    doc.notes = decryptField(doc.notes);
  }
});

invoiceSchema.post('init', function (doc) {
  if (doc && doc.notes) {
    doc.notes = decryptField(doc.notes);
  }
});

module.exports = mongoose.model('Invoice', invoiceSchema);
