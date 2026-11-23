const mongoose = require('mongoose');

const otpChallengeSchema = new mongoose.Schema(
  {
    tokenHash: { type: String, required: true, unique: true },
    phone: { type: String, required: true, index: true },
    channel: { type: String, required: true, enum: ['SMS', 'WHATSAPP', 'TELEGRAM'] },
    provider: { type: String, required: true, enum: ['twilio', 'telegram'] },
    status: {
      type: String,
      enum: ['PENDING', 'VERIFIED', 'LOCKED', 'EXPIRED', 'REPLACED'],
      default: 'PENDING',
      index: true,
    },
    attempts: { type: Number, default: 0, min: 0 },
    codeHash: { type: String, select: false },
    createdAt: { type: Date, default: Date.now },
    expiresAt: { type: Date, required: true },
    verifiedAt: { type: Date, default: null },
  },
  { versionKey: false }
);

otpChallengeSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });
otpChallengeSchema.index({ phone: 1, channel: 1, status: 1, createdAt: -1 });

module.exports = mongoose.model('OtpChallenge', otpChallengeSchema);
