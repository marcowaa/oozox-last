const mongoose = require('mongoose');

const telegramLinkSchema = new mongoose.Schema(
  {
    tokenHash: { type: String, required: true, unique: true },
    phone: { type: String, required: true, index: true },
    createdAt: { type: Date, default: Date.now },
    expiresAt: { type: Date, required: true },
    usedAt: { type: Date, default: null },
  },
  { versionKey: false }
);

telegramLinkSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

module.exports = mongoose.model('TelegramLink', telegramLinkSchema);
