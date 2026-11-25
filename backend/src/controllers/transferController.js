const crypto = require('crypto');
const DeviceTransferSession = require('../models/DeviceTransferSession');
const DeviceTransferChunk = require('../models/DeviceTransferChunk');
const AuthSession = require('../models/AuthSession');
const TrustedDevice = require('../models/TrustedDevice');
const Notification = require('../models/Notification');
const { successResponse, errorResponse } = require('../utils/response');
const { hashToken } = require('../utils/jwt');
const { revokeDeviceAndSessions } = require('../services/deviceTrustService');
const { sendDataPushToUser } = require('../services/fcmPushService');
const { publishRealtimeEvent } = require('../services/realtimeBus');

const TRANSFER_TTL_MS = Math.max(5 * 60 * 1000, (Number(process.env.TRANSFER_TTL_MINUTES) || 15) * 60 * 1000);

function getReqDeviceId(req) {
  return String(req.body?.deviceId || req.headers['x-device-id'] || '').trim().slice(0, 128);
}

function getReqDeviceName(req) {
  return String(req.body?.deviceName || req.headers['x-device-name'] || 'OOZOX Phone').trim().slice(0, 120);
}

// @desc    New phone initiates E2E encrypted account & media transfer session
// @route   POST /api/transfer/initiate
// @access  Private (called from newly authenticated phone)
const initiateTransfer = async (req, res, next) => {
  try {
    const targetDeviceId = getReqDeviceId(req);
    const targetDeviceName = getReqDeviceName(req);
    const { targetEcdhPubKey } = req.body || {};

    if (!targetDeviceId || typeof targetEcdhPubKey !== 'string' || targetEcdhPubKey.trim().length < 24) {
      return errorResponse(res, 400, 'target deviceId and targetEcdhPubKey are required');
    }

    // Cancel any stale pending session started by the same targetDeviceId
    await DeviceTransferSession.updateMany(
      {
        userId: req.user._id,
        targetDeviceId,
        status: { $in: ['PENDING_APPROVAL', 'APPROVED_READY', 'UPLOADING'] },
      },
      { $set: { status: 'CANCELLED' } }
    );

    const qrNonce = crypto.randomBytes(24).toString('hex');
    const qrNonceHash = hashToken(qrNonce);
    const expiresAt = new Date(Date.now() + TRANSFER_TTL_MS);

    const session = await DeviceTransferSession.create({
      userId: req.user._id,
      targetDeviceId,
      targetDeviceName,
      targetEcdhPubKey: targetEcdhPubKey.trim(),
      qrNonceHash,
      status: 'PENDING_APPROVAL',
      expiresAt,
    });

    const qrPayload = JSON.stringify({
      v: 1,
      type: 'OOZOX_XFER',
      transferId: session.transferId,
      qrNonce,
      targetDeviceId,
      targetDeviceName,
      targetEcdhPubKey: session.targetEcdhPubKey,
      expiresAt: expiresAt.toISOString(),
    });

    // Notify old device(s) immediately via both Go WS PubSub and FCM High-Priority Push
    await Promise.all([
      Notification.create({
        recipient: req.user._id,
        title: 'طلب نقل الحساب والوسائط بالكامل',
        message: `يطلب الجهاز (${targetDeviceName}) الموافقة على نقل الحساب والمحادثات والوسائط عبر تشفير E2E.`,
        type: 'ACCOUNT_TRANSFER_REQUEST',
        language: req.user.preferredLanguage || 'ar',
      }).catch(() => {}),
      publishRealtimeEvent({
        type: 'account_transfer_request',
        targetUserIds: [req.user._id],
        payload: {
          transferId: session.transferId,
          targetDeviceId,
          targetDeviceName,
          targetEcdhPubKey: session.targetEcdhPubKey,
          expiresAt: expiresAt.toISOString(),
        },
      }),
      sendDataPushToUser({
        userId: req.user._id,
        excludeDeviceId: targetDeviceId,
        highPriority: true,
        data: {
          type: 'account_transfer_request',
          transferId: session.transferId,
          targetDeviceId,
          targetDeviceName,
          expiresAt: expiresAt.toISOString(),
        },
        notification: {
          title: 'طلب نقل حساب OOZOX',
          body: `جهاز جديد (${targetDeviceName}) يطلب الموافقة لنقل المحادثات والوسائط.`,
        },
      }),
    ]);

    return successResponse(res, 201, 'Transfer session initiated', {
      transferId: session.transferId,
      status: session.status,
      expiresAt: session.expiresAt,
      qrPayload,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Old phone checks if a new phone is awaiting approval for transfer
// @route   GET /api/transfer/pending
// @access  Private
const listPendingTransfers = async (req, res, next) => {
  try {
    const currentDeviceId = getReqDeviceId(req);
    const now = new Date();
    const pending = await DeviceTransferSession.find({
      userId: req.user._id,
      status: 'PENDING_APPROVAL',
      expiresAt: { $gt: now },
      ...(currentDeviceId ? { targetDeviceId: { $ne: currentDeviceId } } : {}),
    })
      .sort({ createdAt: -1 })
      .limit(5);

    return successResponse(res, 200, 'Pending transfer requests fetched', {
      pending: pending.map((s) => ({
        transferId: s.transferId,
        targetDeviceId: s.targetDeviceId,
        targetDeviceName: s.targetDeviceName,
        targetEcdhPubKey: s.targetEcdhPubKey,
        createdAt: s.createdAt,
        expiresAt: s.expiresAt,
      })),
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Old phone approves transfer via scanned QR nonce OR in-app push approval
// @route   POST /api/transfer/:transferId/approve
// @access  Private (must originate from an active authenticated old phone, NOT targetDeviceId)
const approveTransfer = async (req, res, next) => {
  try {
    const sourceDeviceId = getReqDeviceId(req);
    const sourceDeviceName = getReqDeviceName(req);
    const { sourceEcdhPubKey, approvalMethod = 'DEVICE_PUSH_APPROVAL', qrNonce } = req.body || {};

    if (!sourceDeviceId || typeof sourceEcdhPubKey !== 'string' || sourceEcdhPubKey.trim().length < 24) {
      return errorResponse(res, 400, 'source deviceId and sourceEcdhPubKey are required');
    }

    const session = await DeviceTransferSession.findOne({ transferId: req.params.transferId }).select('+qrNonceHash');
    if (!session) {
      return errorResponse(res, 404, 'Transfer session not found');
    }
    if (String(session.userId) !== String(req.user._id)) {
      return errorResponse(res, 403, 'Unauthorized transfer access');
    }
    if (session.targetDeviceId === sourceDeviceId) {
      return errorResponse(res, 400, 'Source device must be a different phone than the target device');
    }
    if (session.expiresAt <= new Date() || session.status !== 'PENDING_APPROVAL') {
      return errorResponse(res, 409, `Transfer cannot be approved in status ${session.status}`);
    }

    if (approvalMethod === 'QR_SCAN') {
      if (!qrNonce || hashToken(String(qrNonce)) !== session.qrNonceHash) {
        return errorResponse(res, 403, 'Invalid or expired QR verification token');
      }
    }

    session.sourceDeviceId = sourceDeviceId;
    session.sourceDeviceName = sourceDeviceName;
    session.sourceEcdhPubKey = sourceEcdhPubKey.trim();
    session.approvalMethod = approvalMethod === 'QR_SCAN' ? 'QR_SCAN' : 'DEVICE_PUSH_APPROVAL';
    session.status = 'APPROVED_READY';
    session.expiresAt = new Date(Date.now() + TRANSFER_TTL_MS);
    await session.save();

    await publishRealtimeEvent({
      type: 'account_transfer_approved',
      targetUserIds: [req.user._id],
      targetDeviceId: session.targetDeviceId,
      payload: {
        transferId: session.transferId,
        sourceDeviceId: session.sourceDeviceId,
        sourceDeviceName: session.sourceDeviceName,
        sourceEcdhPubKey: session.sourceEcdhPubKey,
      },
    });

    return successResponse(res, 200, 'Transfer approved; ready for encrypted chunk upload', {
      transferId: session.transferId,
      status: session.status,
      targetEcdhPubKey: session.targetEcdhPubKey,
      sourceDeviceId: session.sourceDeviceId,
      expiresAt: session.expiresAt,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Old phone uploads a single AES-256-GCM encrypted transfer chunk (resumable / idempotent)
// @route   POST /api/transfer/:transferId/chunks
// @access  Private (must match sourceDeviceId)
const uploadTransferChunk = async (req, res, next) => {
  try {
    const sourceDeviceId = getReqDeviceId(req);
    const { chunkIndex, totalChunks, cipherChunkBase64, ivBase64, chunkSha256 } = req.body || {};

    const session = await DeviceTransferSession.findOne({ transferId: req.params.transferId });
    if (!session) {
      return errorResponse(res, 404, 'Transfer session not found');
    }
    if (String(session.userId) !== String(req.user._id) || session.sourceDeviceId !== sourceDeviceId) {
      return errorResponse(res, 403, 'Only the approved source phone may upload transfer chunks');
    }
    if (!['APPROVED_READY', 'UPLOADING'].includes(session.status)) {
      return errorResponse(res, 409, `Transfer upload not permitted in status ${session.status}`);
    }

    const idx = Number(chunkIndex);
    const total = Number(totalChunks);
    if (!Number.isInteger(idx) || idx < 0 || !Number.isInteger(total) || total < 1 || idx >= total) {
      return errorResponse(res, 400, 'Invalid chunkIndex or totalChunks');
    }
    if (typeof cipherChunkBase64 !== 'string' || !cipherChunkBase64 || typeof ivBase64 !== 'string' || !ivBase64) {
      return errorResponse(res, 400, 'Encrypted chunk payload and IV are required');
    }

    const expectedHash = crypto.createHash('sha256').update(cipherChunkBase64, 'utf8').digest('hex');
    if (chunkSha256 && chunkSha256 !== expectedHash) {
      return errorResponse(res, 400, 'Chunk checksum mismatch');
    }

    const chunkExpiresAt = new Date(Date.now() + TRANSFER_TTL_MS);
    const byteSize = Buffer.byteLength(cipherChunkBase64, 'utf8');

    await DeviceTransferChunk.findOneAndUpdate(
      { transferId: session.transferId, chunkIndex: idx },
      {
        $set: {
          userId: req.user._id,
          totalChunks: total,
          cipherChunkBase64,
          ivBase64,
          chunkSha256: expectedHash,
          byteSize,
          expiresAt: chunkExpiresAt,
        },
      },
      { upsert: true, new: true }
    );

    if (!session.uploadedChunkIndexes.includes(idx)) {
      session.uploadedChunkIndexes.push(idx);
      session.uploadedChunkIndexes.sort((a, b) => a - b);
    }
    session.totalChunks = total;
    session.status = 'UPLOADING';
    session.expiresAt = chunkExpiresAt;
    await session.save();

    return successResponse(res, 200, 'Encrypted chunk stored', {
      transferId: session.transferId,
      chunkIndex: idx,
      uploadedCount: session.uploadedChunkIndexes.length,
      totalChunks: total,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Old phone finalizes encrypted manifest and marks transfer ready for new phone
// @route   POST /api/transfer/:transferId/finalize-upload
// @access  Private (must match sourceDeviceId)
const finalizeTransferUpload = async (req, res, next) => {
  try {
    const sourceDeviceId = getReqDeviceId(req);
    const { totalChunks, encryptedManifestBase64 = '', manifestIvBase64 = '', manifestSha256 = '', summary = {} } =
      req.body || {};

    const session = await DeviceTransferSession.findOne({ transferId: req.params.transferId });
    if (!session) {
      return errorResponse(res, 404, 'Transfer session not found');
    }
    if (String(session.userId) !== String(req.user._id) || session.sourceDeviceId !== sourceDeviceId) {
      return errorResponse(res, 403, 'Unauthorized transfer finalization');
    }

    const expectedTotal = Number(totalChunks || session.totalChunks);
    if (expectedTotal < 1 || session.uploadedChunkIndexes.length < expectedTotal) {
      return errorResponse(res, 400, 'All encrypted chunks must be uploaded before finalizing');
    }

    session.totalChunks = expectedTotal;
    session.encryptedManifestBase64 = encryptedManifestBase64;
    session.manifestIvBase64 = manifestIvBase64;
    session.manifestSha256 = manifestSha256;
    session.summary = {
      totalRecords: Number(summary.totalRecords || 0),
      mediaFilesCount: Number(summary.mediaFilesCount || 0),
      totalBytes: Number(summary.totalBytes || 0),
    };
    session.status = 'READY_FOR_DOWNLOAD';
    session.expiresAt = new Date(Date.now() + TRANSFER_TTL_MS);
    await session.save();

    await Promise.all([
      publishRealtimeEvent({
        type: 'account_transfer_ready',
        targetUserIds: [req.user._id],
        targetDeviceId: session.targetDeviceId,
        payload: {
          transferId: session.transferId,
          totalChunks: session.totalChunks,
          summary: session.summary,
        },
      }),
      sendDataPushToUser({
        userId: req.user._id,
        targetDeviceId: session.targetDeviceId,
        highPriority: true,
        data: {
          type: 'account_transfer_ready',
          transferId: session.transferId,
          totalChunks: String(session.totalChunks),
        },
      }),
    ]);

    return successResponse(res, 200, 'Transfer ready for target device download', {
      transferId: session.transferId,
      status: session.status,
      totalChunks: session.totalChunks,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Poll transfer status & peer ECDH public key
// @route   GET /api/transfer/:transferId/status
// @access  Private (owner only)
const getTransferStatus = async (req, res, next) => {
  try {
    const session = await DeviceTransferSession.findOne({ transferId: req.params.transferId });
    if (!session) {
      return errorResponse(res, 404, 'Transfer session not found');
    }
    if (String(session.userId) !== String(req.user._id)) {
      return errorResponse(res, 403, 'Unauthorized transfer access');
    }

    return successResponse(res, 200, 'Transfer status fetched', {
      transferId: session.transferId,
      status: session.status,
      approvalMethod: session.approvalMethod,
      targetDeviceId: session.targetDeviceId,
      targetDeviceName: session.targetDeviceName,
      targetEcdhPubKey: session.targetEcdhPubKey,
      sourceDeviceId: session.sourceDeviceId,
      sourceDeviceName: session.sourceDeviceName,
      sourceEcdhPubKey: session.sourceEcdhPubKey,
      totalChunks: session.totalChunks,
      uploadedChunkIndexes: session.uploadedChunkIndexes,
      encryptedManifestBase64: session.encryptedManifestBase64,
      manifestIvBase64: session.manifestIvBase64,
      manifestSha256: session.manifestSha256,
      summary: session.summary,
      oldDeviceRevokedAt: session.oldDeviceRevokedAt,
      expiresAt: session.expiresAt,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    New phone downloads one encrypted chunk
// @route   GET /api/transfer/:transferId/chunks/:chunkIndex
// @access  Private (targetDeviceId only)
const downloadTransferChunk = async (req, res, next) => {
  try {
    const requesterDeviceId = getReqDeviceId(req);
    const session = await DeviceTransferSession.findOne({ transferId: req.params.transferId });
    if (!session) {
      return errorResponse(res, 404, 'Transfer session not found');
    }
    if (String(session.userId) !== String(req.user._id) || session.targetDeviceId !== requesterDeviceId) {
      return errorResponse(res, 403, 'Only the target phone may download transfer chunks');
    }

    const idx = Number(req.params.chunkIndex);
    const chunk = await DeviceTransferChunk.findOne({ transferId: session.transferId, chunkIndex: idx });
    if (!chunk) {
      return errorResponse(res, 404, 'Transfer chunk not found');
    }

    return successResponse(res, 200, 'Transfer chunk fetched', {
      transferId: session.transferId,
      chunkIndex: chunk.chunkIndex,
      totalChunks: chunk.totalChunks,
      cipherChunkBase64: chunk.cipherChunkBase64,
      ivBase64: chunk.ivBase64,
      chunkSha256: chunk.chunkSha256,
      byteSize: chunk.byteSize,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    New phone acknowledges atomic import; IMMEDIATELY revokes old device session & trust, purges relay chunks
// @route   POST /api/transfer/:transferId/complete
// @access  Private (targetDeviceId only)
const completeTransfer = async (req, res, next) => {
  try {
    const requesterDeviceId = getReqDeviceId(req);
    const session = await DeviceTransferSession.findOne({ transferId: req.params.transferId });
    if (!session) {
      return errorResponse(res, 404, 'Transfer session not found');
    }
    if (String(session.userId) !== String(req.user._id) || session.targetDeviceId !== requesterDeviceId) {
      return errorResponse(res, 403, 'Only the target phone may complete transfer');
    }

    if (session.status === 'COMPLETED') {
      return successResponse(res, 200, 'Transfer already completed', {
        transferId: session.transferId,
        status: session.status,
        oldDeviceRevokedAt: session.oldDeviceRevokedAt,
      });
    }

    const now = new Date();
    session.status = 'COMPLETED';
    session.completedAt = now;
    session.oldDeviceRevokedAt = now;
    await session.save();

    // Immediately log out & revoke trust for the old source phone (and delete ephemeral relay chunks)
    const oldDeviceId = session.sourceDeviceId;
    if (oldDeviceId) {
      await revokeDeviceAndSessions(req.user._id, oldDeviceId, 'TRANSFERRED_TO_NEW_DEVICE');
    }
    await DeviceTransferChunk.deleteMany({ transferId: session.transferId });

    await Promise.all([
      publishRealtimeEvent({
        type: 'force_logout',
        targetUserIds: [req.user._id],
        targetDeviceId: oldDeviceId,
        payload: {
          reason: 'TRANSFERRED_TO_NEW_DEVICE',
          transferId: session.transferId,
          newDeviceName: session.targetDeviceName,
        },
      }),
      sendDataPushToUser({
        userId: req.user._id,
        targetDeviceId: oldDeviceId,
        highPriority: true,
        data: {
          type: 'session_revoked',
          reason: 'TRANSFERRED_TO_NEW_DEVICE',
          transferId: session.transferId,
        },
        notification: {
          title: 'تم نقل حساب OOZOX',
          body: `اكتمل نقل الحساب والوسائط إلى ${session.targetDeviceName} وتم تسجيل خروج هذا الجهاز لأمانك.`,
        },
      }),
      Notification.create({
        recipient: req.user._id,
        title: 'اكتمل نقل الحساب وإغلاق الجلسة السابقة',
        message: `تم نقل كافة البيانات والوسائط بنجاح إلى (${session.targetDeviceName}) وإبطال جلسة الهاتف السابق تلقائياً.`,
        type: 'SECURITY_ALERT',
        language: req.user.preferredLanguage || 'ar',
      }).catch(() => {}),
    ]);

    return successResponse(res, 200, 'Transfer completed and previous phone logged out automatically', {
      transferId: session.transferId,
      status: session.status,
      revokedDeviceId: oldDeviceId,
      oldDeviceRevokedAt: session.oldDeviceRevokedAt,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Cancel transfer (or skip while optionally revoking lost/old device sessions)
// @route   POST /api/transfer/:transferId/cancel
// @access  Private
const cancelTransfer = async (req, res, next) => {
  try {
    const requesterDeviceId = getReqDeviceId(req);
    const { revokeOtherDevices = false } = req.body || {};

    const session = await DeviceTransferSession.findOne({ transferId: req.params.transferId });
    if (!session) {
      return errorResponse(res, 404, 'Transfer session not found');
    }
    if (String(session.userId) !== String(req.user._id)) {
      return errorResponse(res, 403, 'Unauthorized transfer access');
    }

    session.status = 'CANCELLED';
    await session.save();
    await DeviceTransferChunk.deleteMany({ transferId: session.transferId });

    let revokedOthersCount = 0;
    if (revokeOtherDevices && requesterDeviceId) {
      const now = new Date();
      const [tdRes, sesRes] = await Promise.all([
        TrustedDevice.updateMany(
          { userId: req.user._id, deviceId: { $ne: requesterDeviceId }, revokedAt: null },
          { $set: { revokedAt: now, revokeReason: 'NEW_DEVICE_REVOKED_OTHERS' } }
        ),
        AuthSession.updateMany(
          { userId: req.user._id, deviceId: { $ne: requesterDeviceId }, revokedAt: null },
          { $set: { revokedAt: now } }
        ),
      ]);
      revokedOthersCount = (tdRes.modifiedCount || 0) + (sesRes.modifiedCount || 0);
    }

    return successResponse(res, 200, 'Transfer cancelled cleanly', {
      transferId: session.transferId,
      status: session.status,
      revokedOthersCount,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Revoke all other devices/sessions except the current authenticated phone (used on Transfer Skip + Secure)
// @route   POST /api/transfer/revoke-other-devices
// @access  Private
const revokeOtherDevices = async (req, res, next) => {
  try {
    const currentDeviceId = getReqDeviceId(req);
    if (!currentDeviceId) {
      return errorResponse(res, 400, 'Current deviceId is required');
    }
    const now = new Date();
    const [tdRes, sesRes] = await Promise.all([
      TrustedDevice.updateMany(
        { userId: req.user._id, deviceId: { $ne: currentDeviceId }, revokedAt: null },
        { $set: { revokedAt: now, revokeReason: 'NEW_DEVICE_REVOKED_OTHERS' } }
      ),
      AuthSession.updateMany(
        {
          userId: req.user._id,
          revokedAt: null,
          ...(req.authSessionId ? { sessionId: { $ne: req.authSessionId } } : { deviceId: { $ne: currentDeviceId } }),
        },
        { $set: { revokedAt: now } }
      ),
    ]);

    await publishRealtimeEvent({
      type: 'force_logout',
      targetUserIds: [req.user._id],
      payload: {
        reason: 'NEW_DEVICE_REVOKED_OTHERS',
        keepDeviceId: currentDeviceId,
      },
    });

    return successResponse(res, 200, 'Other active devices revoked', {
      revokedDevicesCount: tdRes.modifiedCount || 0,
      revokedSessionsCount: sesRes.modifiedCount || 0,
    });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  initiateTransfer,
  listPendingTransfers,
  approveTransfer,
  uploadTransferChunk,
  finalizeTransferUpload,
  getTransferStatus,
  downloadTransferChunk,
  completeTransfer,
  cancelTransfer,
  revokeOtherDevices,
};
