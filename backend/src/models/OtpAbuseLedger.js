const mongoose = require('mongoose');

const otpAbuseLedgerSchema = new mongoose.Schema(
  {
    key: {
      type: String,
      required: true,
      unique: true,
    },
    kind: {
      type: String,
      enum: ['PHONE_DAY', 'OTP_MONTH'],
      required: true,
    },
    sendCount: {
      type: Number,
      default: 0,
    },
    verifiedCount: {
      type: Number,
      default: 0,
    },
    twilioSendCount: {
      type: Number,
      default: 0,
    },
    telegramSendCount: {
      type: Number,
      default: 0,
    },
    estimatedCostUsd: {
      type: Number,
      default: 0,
    },
    alertsTriggered: {
      type: [Number],
      default: [],
    },
    lastSentAt: {
      type: Date,
      default: null,
    },
    lockedUntil: {
      type: Date,
      default: null,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('OtpAbuseLedger', otpAbuseLedgerSchema);
