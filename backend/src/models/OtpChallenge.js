const mongoose = require('mongoose');
const crypto = require('crypto');

const otpChallengeSchema = new mongoose.Schema(
  {
    challengeId: {
      type: String,
      required: true,
      unique: true,
      default: () => crypto.randomUUID(),
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
      index: true,
    },
    phoneE164: {
      type: String,
      required: true,
      index: true,
    },
    purpose: {
      type: String,
      enum: ['REGISTER', 'LOGIN', 'DEVICE_TRUST', 'CHANGE_PHONE', 'RECOVER', 'SENSITIVE_ACTION'],
      required: true,
      index: true,
    },
    requestedChannel: {
      type: String,
      enum: ['AUTO', 'TELEGRAM', 'WHATSAPP', 'SMS'],
      default: 'AUTO',
    },
    activeChannel: {
      type: String,
      enum: ['TELEGRAM', 'WHATSAPP', 'SMS'],
      required: true,
    },
    attemptedChannels: [
      {
        channel: { type: String },
        status: { type: String },
        error: { type: String, default: '' },
        at: { type: Date, default: Date.now },
      },
    ],
    providerType: {
      type: String,
      enum: ['TELEGRAM_GATEWAY', 'TWILIO_VERIFY'],
      required: true,
    },
    providerRef: {
      type: String,
      default: '',
      index: true,
    },
    // Stored ONLY for Telegram Gateway (self-generated OTP). Always null for Twilio Verify.
    codeHash: {
      type: String,
      default: null,
      select: false,
    },
    codeSalt: {
      type: String,
      default: null,
      select: false,
    },
    status: {
      type: String,
      enum: ['PENDING', 'VERIFIED', 'FAILED', 'EXPIRED', 'LOCKED', 'SUPERSEDED', 'CANCELLED'],
      default: 'PENDING',
      index: true,
    },
    attempts: {
      type: Number,
      default: 0,
    },
    maxAttempts: {
      type: Number,
      default: 5,
    },
    lockedUntil: {
      type: Date,
      default: null,
    },
    expiresAt: {
      type: Date,
      required: true,
      index: true,
    },
    verifiedAt: {
      type: Date,
      default: null,
    },
    deliveryStatus: {
      type: String,
      default: 'SENT',
    },
    metadata: {
      deviceId: { type: String, default: '' },
      deviceName: { type: String, default: '' },
      platform: { type: String, default: 'ANDROID' },
      ipAddress: { type: String, default: '' },
      estimatedCostUsd: { type: Number, default: 0 },
    },
  },
  {
    timestamps: true,
  }
);

otpChallengeSchema.index({ phoneE164: 1, purpose: 1, status: 1 });

module.exports = mongoose.model('OtpChallenge', otpChallengeSchema);
