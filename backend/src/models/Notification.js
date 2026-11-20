const mongoose = require('mongoose');
const { encryptField, decryptField } = require('../utils/fieldCrypto');

const notificationSchema = new mongoose.Schema(
  {
    recipient: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: [true, 'Notification recipient is required'],
      index: true,
    },
    sender: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },
    title: {
      type: String,
      required: [true, 'Notification title is required'],
      trim: true,
    },
    message: {
      type: String,
      required: [true, 'Notification message is required'],
      trim: true,
    },
    type: {
      type: String,
      enum: ['INVOICE_CREATED', 'INVOICE_PAID', 'INVOICE_OVERDUE', 'INVOICE_CANCELLED', 'SYSTEM'],
      default: 'INVOICE_CREATED',
    },
    relatedInvoice: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Invoice',
      default: null,
    },
    isRead: {
      type: Boolean,
      default: false,
      index: true,
    },
  },
  {
    timestamps: true,
  }
);

// Transparent AES-256-GCM field-level encryption at rest for notification messages
notificationSchema.pre('save', function (next) {
  if (this.message) {
    this.message = encryptField(this.message);
  }
  next();
});

notificationSchema.post('save', function (doc) {
  if (doc && doc.message) {
    doc.message = decryptField(doc.message);
  }
});

notificationSchema.post('init', function (doc) {
  if (doc && doc.message) {
    doc.message = decryptField(doc.message);
  }
});

module.exports = mongoose.model('Notification', notificationSchema);
