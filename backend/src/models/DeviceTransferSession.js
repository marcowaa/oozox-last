const mongoose = require('mongoose');
const crypto = require('crypto');

const deviceTransferSessionSchema = new mongoose.Schema(
  {
    transferId: {
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
    // New phone receiving the full E2E encrypted account & media transfer
    targetDeviceId: {
      type: String,
      required: true,
      trim: true,
      maxlength: 128,
    },
    targetDeviceName: {
      type: String,
      default: 'New Device',
      trim: true,
      maxlength: 120,
    },
    targetEcdhPubKey: {
      type: String,
      required: true,
    },
    // Old phone briefly kept alive ONLY to authorize & upload encrypted transfer chunks
    sourceDeviceId: {
      type: String,
      default: '',
      trim: true,
      maxlength: 128,
    },
    sourceDeviceName: {
      type: String,
      default: '',
      trim: true,
      maxlength: 120,
    },
    sourceEcdhPubKey: {
      type: String,
      default: '',
    },
    approvalMethod: {
      type: String,
      enum: ['NONE', 'QR_SCAN', 'DEVICE_PUSH_APPROVAL'],
      default: 'NONE',
    },
    qrNonceHash: {
      type: String,
      required: true,
      select: false,
    },
    status: {
      type: String,
      enum: [
        'PENDING_APPROVAL',
        'APPROVED_READY',
        'UPLOADING',
        'READY_FOR_DOWNLOAD',
        'COMPLETED',
        'CANCELLED',
        'EXPIRED',
      ],
      default: 'PENDING_APPROVAL',
      index: true,
    },
    totalChunks: {
      type: Number,
      default: 0,
      min: 0,
    },
    uploadedChunkIndexes: {
      type: [Number],
      default: [],
    },
    // Encrypted manifest JSON (encrypted by client-derived AES-GCM session key)
    encryptedManifestBase64: {
      type: String,
      default: '',
    },
    manifestIvBase64: {
      type: String,
      default: '',
    },
    manifestSha256: {
      type: String,
      default: '',
    },
    summary: {
      totalRecords: { type: Number, default: 0 },
      mediaFilesCount: { type: Number, default: 0 },
      totalBytes: { type: Number, default: 0 },
    },
    oldDeviceRevokedAt: {
      type: Date,
      default: null,
    },
    completedAt: {
      type: Date,
      default: null,
    },
    expiresAt: {
      type: Date,
      required: true,
      index: { expires: 0 },
    },
  },
  { timestamps: true }
);

deviceTransferSessionSchema.index({ userId: 1, status: 1, createdAt: -1 });

module.exports = mongoose.model('DeviceTransferSession', deviceTransferSessionSchema);
