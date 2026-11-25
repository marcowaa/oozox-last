const mongoose = require('mongoose');
const crypto = require('crypto');
const { encryptField, decryptField } = require('../utils/fieldCrypto');

const chatMessageSchema = new mongoose.Schema(
  {
    messageId: {
      type: String,
      required: true,
      unique: true,
      default: () => crypto.randomUUID(),
    },
    clientTempId: {
      type: String,
      default: '',
      index: true,
    },
    conversationId: {
      type: String,
      required: true,
      index: true,
    },
    senderId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    messageType: {
      type: String,
      enum: ['TEXT', 'IMAGE', 'VIDEO', 'VOICE', 'DOCUMENT', 'INVOICE_TAG', 'SYSTEM'],
      default: 'TEXT',
    },
    // Encrypted at rest via DB_ENCRYPTION_KEY (AES-256-GCM)
    content: {
      type: String,
      required: true,
      maxlength: 12000,
      set: encryptField,
      get: decryptField,
    },
    mediaMeta: {
      fileName: { type: String, default: '' },
      mimeType: { type: String, default: '' },
      sizeBytes: { type: Number, default: 0 },
      durationMs: { type: Number, default: 0 },
      mediaUrl: { type: String, default: '' },
      sha256: { type: String, default: '' },
    },
    deliveredTo: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    readBy: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    deletedAt: { type: Date, default: null },
  },
  {
    timestamps: true,
    toJSON: { getters: true },
    toObject: { getters: true },
  }
);

chatMessageSchema.index({ conversationId: 1, createdAt: -1 });

module.exports = mongoose.model('ChatMessage', chatMessageSchema);
