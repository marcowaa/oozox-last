const mongoose = require('mongoose');
const crypto = require('crypto');

const authSessionSchema = new mongoose.Schema(
  {
    sessionId: {
      type: String,
      required: true,
      unique: true,
      default: () => crypto.randomUUID(),
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    deviceId: {
      type: String,
      default: '',
      index: true,
    },
    refreshTokenHash: {
      type: String,
      required: true,
      select: false,
    },
    authMethod: {
      type: String,
      enum: ['OTP', 'PASSWORD_OTP', 'TRUSTED_DEVICE', 'LEGACY_PASSWORD'],
      default: 'OTP',
    },
    otpVerifiedAt: {
      type: Date,
      default: null,
    },
    lastStepUpAt: {
      type: Date,
      default: Date.now,
    },
    issuedAt: {
      type: Date,
      default: Date.now,
    },
    expiresAt: {
      type: Date,
      required: true,
      index: true,
    },
    revokedAt: {
      type: Date,
      default: null,
      index: true,
    },
    userAgent: {
      type: String,
      default: '',
    },
    ipAddress: {
      type: String,
      default: '',
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('AuthSession', authSessionSchema);
