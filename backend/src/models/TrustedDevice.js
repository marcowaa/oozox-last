const mongoose = require('mongoose');

const trustedDeviceSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    deviceId: {
      type: String,
      required: true,
      trim: true,
      maxlength: 128,
    },
    deviceName: {
      type: String,
      default: 'OOZOX Device',
      trim: true,
      maxlength: 120,
    },
    platform: {
      type: String,
      enum: ['ANDROID', 'WEB', 'IOS', 'OTHER'],
      default: 'ANDROID',
    },
    hardwareFingerprintHash: {
      type: String,
      required: true,
      select: false,
    },
    trustTokenHash: {
      type: String,
      required: true,
      select: false,
    },
    trustVersion: {
      type: Number,
      default: 1,
    },
    trustedAt: {
      type: Date,
      default: Date.now,
    },
    trustedUntil: {
      type: Date,
      required: true,
      index: true,
    },
    lastActiveAt: {
      type: Date,
      default: Date.now,
    },
    lastIp: {
      type: String,
      default: '',
    },
    fcmToken: {
      type: String,
      default: '',
    },
    revokedAt: {
      type: Date,
      default: null,
      index: true,
    },
    revokeReason: {
      type: String,
      default: '',
    },
  },
  { timestamps: true }
);

trustedDeviceSchema.index({ userId: 1, deviceId: 1 }, { unique: true });

module.exports = mongoose.model('TrustedDevice', trustedDeviceSchema);
