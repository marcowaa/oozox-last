const mongoose = require('mongoose');
const crypto = require('crypto');

const encryptedBackupSchema = new mongoose.Schema(
  {
    backupId: {
      type: String,
      required: true,
      unique: true,
      default: () => crypto.randomUUID(),
    },
    ownerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    phoneHash: {
      type: String,
      required: true,
      index: true,
    },
    backupType: {
      type: String,
      enum: ['FULL_VAULT_V3', 'FULL_VAULT_V2', 'SUB_ACCOUNTS_MIGRATION', 'MANUAL_SNAPSHOT'],
      default: 'FULL_VAULT_V3',
    },
    formatVersion: {
      type: Number,
      default: 3,
      min: 1,
      max: 10,
    },
    fileName: {
      type: String,
      required: true,
      trim: true,
      maxlength: 200,
    },
    sizeBytes: {
      type: Number,
      required: true,
      min: 1,
    },
    payloadSha256: {
      type: String,
      required: true,
    },
    // Opaque encrypted .oozox vault envelope (client-side PBKDF2 + AES-GCM ciphertext)
    vaultContent: {
      type: String,
      required: true,
      select: false,
    },
    deviceName: {
      type: String,
      default: 'Android Device',
      trim: true,
      maxlength: 120,
    },
    stats: {
      totalRecords: { type: Number, default: 0 },
      chatsCount: { type: Number, default: 0 },
      messagesCount: { type: Number, default: 0 },
      mediaFilesCount: { type: Number, default: 0 },
      recordsCount: { type: Number, default: 0 },
      complaintsCount: { type: Number, default: 0 },
      subProfilesCount: { type: Number, default: 0 },
      workGroupsCount: { type: Number, default: 0 },
      projectsCount: { type: Number, default: 0 },
    },
  },
  { timestamps: true }
);

encryptedBackupSchema.index({ ownerId: 1, createdAt: -1 });

module.exports = mongoose.model('EncryptedBackup', encryptedBackupSchema);
