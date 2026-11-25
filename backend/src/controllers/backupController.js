const crypto = require('crypto');
const EncryptedBackup = require('../models/EncryptedBackup');
const { successResponse, errorResponse } = require('../utils/response');

const MAX_BACKUPS_PER_USER = Math.max(2, Number(process.env.MAX_CLOUD_BACKUPS_PER_USER) || 10);

function computeSha256(str) {
  return crypto.createHash('sha256').update(str, 'utf8').digest('hex');
}

// @desc    List authenticated user's real encrypted cloud backups
// @route   GET /api/backups
// @access  Private
const listBackups = async (req, res, next) => {
  try {
    const backups = await EncryptedBackup.find({ ownerId: req.user._id })
      .sort({ createdAt: -1 })
      .select('-vaultContent');

    return successResponse(res, 200, 'Backups fetched', {
      backups: backups.map((b) => ({
        backupId: b.backupId,
        backupType: b.backupType,
        formatVersion: b.formatVersion,
        fileName: b.fileName,
        sizeBytes: b.sizeBytes,
        payloadSha256: b.payloadSha256,
        deviceName: b.deviceName,
        stats: b.stats,
        createdAt: b.createdAt,
      })),
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Upload a real encrypted .oozox vault backup to server
// @route   POST /api/backups
// @access  Private
const uploadBackup = async (req, res, next) => {
  try {
    const {
      fileName = `oozox_backup_${Date.now()}.oozox`,
      backupType = 'FULL_VAULT_V3',
      formatVersion = 3,
      phoneHash = '',
      vaultContent,
      deviceName = 'Android Device',
      stats = {},
    } = req.body || {};

    if (typeof vaultContent !== 'string' || vaultContent.trim().length < 20) {
      return errorResponse(res, 400, 'Valid encrypted vaultContent is required');
    }

    // Parse outer JSON envelope ONLY (server never decrypts client passphrase ciphertext)
    let parsedEnvelope;
    try {
      parsedEnvelope = JSON.parse(vaultContent);
    } catch {
      return errorResponse(res, 400, 'Backup content must be a valid .oozox encrypted JSON envelope');
    }

    const magic = String(parsedEnvelope?.magic || '');
    if (
      !['OOZOX_ENCRYPTED_VAULT_V1', 'OOZOX_ENCRYPTED_VAULT_V2', 'OOZOX_ENCRYPTED_VAULT_V3'].includes(magic)
    ) {
      return errorResponse(res, 400, 'Unsupported or invalid OOZOX vault header');
    }

    const sizeBytes = Buffer.byteLength(vaultContent, 'utf8');
    const payloadSha256 = computeSha256(vaultContent);
    const normalizedPhone = req.user.phoneE164 || req.user.phone || '';
    const computedPhoneHash =
      phoneHash ||
      computeSha256(normalizedPhone.replace(/[^0-9+]/g, '').trim());

    const doc = await EncryptedBackup.create({
      ownerId: req.user._id,
      phoneHash: computedPhoneHash,
      backupType: ['FULL_VAULT_V3', 'FULL_VAULT_V2', 'SUB_ACCOUNTS_MIGRATION', 'MANUAL_SNAPSHOT'].includes(
        backupType
      )
        ? backupType
        : 'FULL_VAULT_V3',
      formatVersion: Number(parsedEnvelope.version || formatVersion || 3),
      fileName: String(fileName).trim().slice(0, 200),
      sizeBytes,
      payloadSha256,
      vaultContent,
      deviceName: String(deviceName).trim().slice(0, 120),
      stats: {
        totalRecords: Number(stats.totalRecords ?? parsedEnvelope?.stats?.totalRecords ?? 0),
        chatsCount: Number(stats.chatsCount ?? parsedEnvelope?.stats?.chatsCount ?? 0),
        messagesCount: Number(stats.messagesCount ?? parsedEnvelope?.stats?.messagesCount ?? 0),
        mediaFilesCount: Number(stats.mediaFilesCount ?? parsedEnvelope?.stats?.mediaFilesCount ?? 0),
        recordsCount: Number(stats.recordsCount ?? parsedEnvelope?.stats?.dailyRecords ?? 0),
        complaintsCount: Number(stats.complaintsCount ?? parsedEnvelope?.stats?.complaintsCount ?? 0),
        subProfilesCount: Number(stats.subProfilesCount ?? parsedEnvelope?.stats?.subProfiles ?? 0),
        workGroupsCount: Number(stats.workGroupsCount ?? parsedEnvelope?.stats?.workGroups ?? 0),
        projectsCount: Number(stats.projectsCount ?? parsedEnvelope?.stats?.projects ?? 0),
      },
    });

    // Enforce bounded per-user cloud retention (oldest removed first)
    const userBackups = await EncryptedBackup.find({ ownerId: req.user._id })
      .sort({ createdAt: -1 })
      .select('_id');
    if (userBackups.length > MAX_BACKUPS_PER_USER) {
      const idsToDelete = userBackups.slice(MAX_BACKUPS_PER_USER).map((b) => b._id);
      await EncryptedBackup.deleteMany({ _id: { $in: idsToDelete } });
    }

    return successResponse(res, 201, 'Encrypted cloud backup uploaded and verified', {
      backup: {
        backupId: doc.backupId,
        backupType: doc.backupType,
        formatVersion: doc.formatVersion,
        fileName: doc.fileName,
        sizeBytes: doc.sizeBytes,
        payloadSha256: doc.payloadSha256,
        deviceName: doc.deviceName,
        stats: doc.stats,
        createdAt: doc.createdAt,
      },
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Download encrypted .oozox vault backup (IDOR-protected)
// @route   GET /api/backups/:backupId/download
// @access  Private
const downloadBackup = async (req, res, next) => {
  try {
    const backup = await EncryptedBackup.findOne({ backupId: req.params.backupId }).select('+vaultContent');
    if (!backup) {
      return errorResponse(res, 404, 'Backup not found');
    }
    if (String(backup.ownerId) !== String(req.user._id)) {
      return errorResponse(res, 403, 'Access denied to this backup');
    }

    return successResponse(res, 200, 'Encrypted backup ready', {
      backupId: backup.backupId,
      fileName: backup.fileName,
      formatVersion: backup.formatVersion,
      sizeBytes: backup.sizeBytes,
      payloadSha256: backup.payloadSha256,
      stats: backup.stats,
      createdAt: backup.createdAt,
      vaultContent: backup.vaultContent,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Delete a cloud backup owned by current user
// @route   DELETE /api/backups/:backupId
// @access  Private
const deleteBackup = async (req, res, next) => {
  try {
    const backup = await EncryptedBackup.findOne({ backupId: req.params.backupId });
    if (!backup) {
      return errorResponse(res, 404, 'Backup not found');
    }
    if (String(backup.ownerId) !== String(req.user._id)) {
      return errorResponse(res, 403, 'Access denied to this backup');
    }

    await backup.deleteOne();
    return successResponse(res, 200, 'Cloud backup deleted', { backupId: req.params.backupId });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  listBackups,
  uploadBackup,
  downloadBackup,
  deleteBackup,
};
