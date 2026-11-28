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
    principalType: {
      type: String,
      enum: ['USER', 'EMPLOYEE'],
      default: 'USER',
      index: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: function requiredUserId() {
        return this.principalType !== 'EMPLOYEE';
      },
      default: null,
      index: true,
    },
    employeeId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Employee',
      default: null,
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
      enum: ['OTP', 'PASSWORD_OTP', 'TRUSTED_DEVICE', 'LEGACY_PASSWORD', 'EMPLOYEE_PASSWORD'],
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
