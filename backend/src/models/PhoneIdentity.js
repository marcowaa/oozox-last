const mongoose = require('mongoose');

const phoneIdentitySchema = new mongoose.Schema(
  {
    phone: { type: String, required: true, unique: true, trim: true },
    verifiedAt: { type: Date, default: null },
    telegramChatId: { type: String, default: null },
    telegramUserId: { type: String, default: null },
    telegramLinkedAt: { type: Date, default: null },
  },
  { timestamps: true, versionKey: false }
);

phoneIdentitySchema.index(
  { telegramChatId: 1 },
  { unique: true, sparse: true }
);

module.exports = mongoose.model('PhoneIdentity', phoneIdentitySchema);
