const crypto = require('crypto');
const TrustedDevice = require('../models/TrustedDevice');
const AuthSession = require('../models/AuthSession');
const User = require('../models/User');
const { generateToken, generateRefreshToken, hashToken } = require('../utils/jwt');

function getDeviceTrustDays() {
  return Math.max(1, Number(process.env.DEVICE_TRUST_DAYS) || 30);
}

function getRefreshTokenDays() {
  return Math.max(1, Number(process.env.REFRESH_TOKEN_DAYS) || 30);
}

function getStepUpMaxAgeSeconds() {
  return Math.max(60, Number(process.env.SENSITIVE_STEP_UP_SECONDS) || 900);
}

function timingSafeHexMatch(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length || !a.length) {
    return false;
  }
  try {
    return crypto.timingSafeEqual(Buffer.from(a, 'hex'), Buffer.from(b, 'hex'));
  } catch {
    return false;
  }
}

async function issueTrustedDevice({
  userId,
  deviceId,
  deviceName = 'OOZOX Device',
  platform = 'ANDROID',
  hardwareFingerprint = '',
  ipAddress = '',
  fcmToken = '',
}) {
  if (!deviceId || typeof deviceId !== 'string') {
    return null;
  }
  const cleanDeviceId = deviceId.trim().slice(0, 128);
  const rawTrustToken = crypto.randomBytes(32).toString('hex');
  const trustTokenHash = hashToken(rawTrustToken);
  const fpInput = (hardwareFingerprint || cleanDeviceId).trim();
  const hardwareFingerprintHash = hashToken(fpInput);
  const now = new Date();
  const trustedUntil = new Date(now.getTime() + getDeviceTrustDays() * 86400000);

  let device = await TrustedDevice.findOne({ userId, deviceId: cleanDeviceId });
  if (device) {
    device.deviceName = String(deviceName || device.deviceName || 'OOZOX Device').trim().slice(0, 120);
    device.platform = ['ANDROID', 'WEB', 'IOS', 'OTHER'].includes(platform) ? platform : 'ANDROID';
    device.hardwareFingerprintHash = hardwareFingerprintHash;
    device.trustTokenHash = trustTokenHash;
    device.trustVersion = (device.trustVersion || 1) + 1;
    device.trustedAt = now;
    device.trustedUntil = trustedUntil;
    device.lastActiveAt = now;
    device.lastIp = ipAddress || '';
    if (fcmToken) {
      device.fcmToken = fcmToken;
    }
    device.revokedAt = null;
    device.revokeReason = '';
    await device.save();
  } else {
    device = await TrustedDevice.create({
      userId,
      deviceId: cleanDeviceId,
      deviceName: String(deviceName || 'OOZOX Device').trim().slice(0, 120),
      platform: ['ANDROID', 'WEB', 'IOS', 'OTHER'].includes(platform) ? platform : 'ANDROID',
      hardwareFingerprintHash,
      trustTokenHash,
      trustVersion: 1,
      trustedAt: now,
      trustedUntil,
      lastActiveAt: now,
      lastIp: ipAddress || '',
      fcmToken: fcmToken || '',
    });
  }

  return {
    deviceId: device.deviceId,
    deviceName: device.deviceName,
    platform: device.platform,
    trustedAt: device.trustedAt,
    trustedUntil: device.trustedUntil,
    deviceTrustToken: rawTrustToken,
  };
}

async function verifyTrustedDevice({
  user,
  deviceId,
  deviceTrustToken,
  hardwareFingerprint = '',
  ipAddress = '',
}) {
  if (!user || !deviceId || !deviceTrustToken) {
    return { trusted: false, reason: 'MISSING_DEVICE_TRUST_CREDENTIALS' };
  }

  const cleanDeviceId = String(deviceId).trim().slice(0, 128);
  const device = await TrustedDevice.findOne({ userId: user._id, deviceId: cleanDeviceId }).select(
    '+trustTokenHash +hardwareFingerprintHash'
  );

  if (!device) {
    return { trusted: false, reason: 'DEVICE_NOT_TRUSTED' };
  }
  if (device.revokedAt) {
    return { trusted: false, reason: 'DEVICE_TRUST_REVOKED' };
  }
  const now = new Date();
  if (!device.trustedUntil || device.trustedUntil <= now) {
    device.revokedAt = now;
    device.revokeReason = 'EXPIRED_30_DAYS';
    await device.save();
    return { trusted: false, reason: 'DEVICE_TRUST_EXPIRED' };
  }
  if (user.passwordChangedAt && device.trustedAt <= user.passwordChangedAt) {
    device.revokedAt = now;
    device.revokeReason = 'PASSWORD_CHANGED';
    await device.save();
    return { trusted: false, reason: 'INVALIDATED_BY_PASSWORD_CHANGE' };
  }

  const expectedTokenHash = hashToken(deviceTrustToken);
  if (!timingSafeHexMatch(expectedTokenHash, device.trustTokenHash)) {
    return { trusted: false, reason: 'INVALID_TRUST_TOKEN' };
  }

  if (hardwareFingerprint) {
    const fpHash = hashToken(hardwareFingerprint.trim());
    if (!timingSafeHexMatch(fpHash, device.hardwareFingerprintHash)) {
      return { trusted: false, reason: 'HARDWARE_FINGERPRINT_MISMATCH' };
    }
  }

  device.lastActiveAt = now;
  if (ipAddress) {
    device.lastIp = ipAddress;
  }
  await device.save();

  return { trusted: true, device };
}

async function issueAuthSession({
  user,
  deviceId = '',
  authMethod = 'OTP',
  otpVerifiedAt = null,
  userAgent = '',
  ipAddress = '',
}) {
  const rawRefreshToken = generateRefreshToken();
  const refreshTokenHash = hashToken(rawRefreshToken);
  const now = new Date();
  const expiresAt = new Date(now.getTime() + getRefreshTokenDays() * 86400000);

  const session = await AuthSession.create({
    sessionId: crypto.randomUUID(),
    userId: user._id,
    deviceId: String(deviceId || '').trim().slice(0, 128),
    refreshTokenHash,
    authMethod,
    otpVerifiedAt,
    lastStepUpAt: otpVerifiedAt || now,
    issuedAt: now,
    expiresAt,
    userAgent: String(userAgent || '').slice(0, 250),
    ipAddress: String(ipAddress || '').slice(0, 64),
  });

  const token = generateToken(user._id, user.role, session.sessionId);

  return {
    token,
    refreshToken: rawRefreshToken,
    sessionId: session.sessionId,
    expiresAt: session.expiresAt,
    lastStepUpAt: session.lastStepUpAt,
  };
}

async function rotateRefreshSession({ refreshToken, deviceId = '', ipAddress = '', userAgent = '' }) {
  if (!refreshToken || typeof refreshToken !== 'string') {
    const err = new Error('Refresh token is required');
    err.statusCode = 400;
    throw err;
  }

  const refreshHash = hashToken(refreshToken.trim());
  const session = await AuthSession.findOne({ refreshTokenHash: refreshHash }).select(
    '+refreshTokenHash'
  );
  const now = new Date();
  if (!session || session.revokedAt || !session.expiresAt || session.expiresAt <= now) {
    const err = new Error('Session expired or revoked');
    err.statusCode = 401;
    err.code = 'SESSION_INVALID';
    throw err;
  }

  if (session.deviceId && deviceId && session.deviceId !== deviceId.trim()) {
    const err = new Error('Session device mismatch');
    err.statusCode = 401;
    err.code = 'DEVICE_MISMATCH';
    throw err;
  }

  const user = await User.findById(session.userId);
  if (!user || !user.isVerified) {
    const err = new Error('Account unavailable or unverified');
    err.statusCode = 401;
    throw err;
  }

  if (user.passwordChangedAt && session.issuedAt < user.passwordChangedAt) {
    session.revokedAt = now;
    await session.save();
    const err = new Error('Session invalidated by password change');
    err.statusCode = 401;
    err.code = 'PASSWORD_CHANGED';
    throw err;
  }

  const nextRefresh = generateRefreshToken();
  session.refreshTokenHash = hashToken(nextRefresh);
  session.expiresAt = new Date(now.getTime() + getRefreshTokenDays() * 86400000);
  if (ipAddress) session.ipAddress = String(ipAddress).slice(0, 64);
  if (userAgent) session.userAgent = String(userAgent).slice(0, 250);
  await session.save();

  const token = generateToken(user._id, user.role, session.sessionId);
  return {
    token,
    refreshToken: nextRefresh,
    sessionId: session.sessionId,
    expiresAt: session.expiresAt,
    user,
  };
}

async function revokeAllTrustedDevicesAndSessions(userId, reason = 'PASSWORD_CHANGED') {
  const now = new Date();
  await Promise.all([
    TrustedDevice.updateMany(
      { userId, revokedAt: null },
      { $set: { revokedAt: now, revokeReason: reason } }
    ),
    AuthSession.updateMany({ userId, revokedAt: null }, { $set: { revokedAt: now } }),
  ]);
}

async function revokeDeviceAndSessions(userId, deviceId, reason = 'USER_REVOKED') {
  const now = new Date();
  const cleanId = String(deviceId || '').trim();
  const device = await TrustedDevice.findOneAndUpdate(
    { userId, deviceId: cleanId, revokedAt: null },
    { $set: { revokedAt: now, revokeReason: reason } },
    { new: true }
  );
  await AuthSession.updateMany(
    { userId, deviceId: cleanId, revokedAt: null },
    { $set: { revokedAt: now } }
  );
  return device;
}

async function isSessionFreshForSensitiveOp(sessionId) {
  if (!sessionId) return false;
  const session = await AuthSession.findOne({ sessionId, revokedAt: null });
  if (!session || !session.lastStepUpAt) return false;
  const ageSec = (Date.now() - session.lastStepUpAt.getTime()) / 1000;
  return ageSec <= getStepUpMaxAgeSeconds();
}

async function markSessionStepUp(sessionId) {
  if (!sessionId) return null;
  return AuthSession.findOneAndUpdate(
    { sessionId, revokedAt: null },
    { $set: { lastStepUpAt: new Date(), otpVerifiedAt: new Date() } },
    { new: true }
  );
}

module.exports = {
  getDeviceTrustDays,
  getStepUpMaxAgeSeconds,
  issueTrustedDevice,
  verifyTrustedDevice,
  issueAuthSession,
  rotateRefreshSession,
  revokeAllTrustedDevicesAndSessions,
  revokeDeviceAndSessions,
  isSessionFreshForSensitiveOp,
  markSessionStepUp,
};
