const User = require('../models/User');
const TrustedDevice = require('../models/TrustedDevice');
const AuthSession = require('../models/AuthSession');
const OtpChallenge = require('../models/OtpChallenge');
const Notification = require('../models/Notification');
const { verifyToken, generatePreAuthToken } = require('../utils/jwt');
const { successResponse, errorResponse } = require('../utils/response');
const { normalizePhoneE164, maskPhone } = require('../utils/phoneUtils');
const { requestOtpChallenge, verifyOtpChallenge } = require('../services/otp/otpOrchestrator');
const { verifyWebhookSignature } = require('../services/otp/telegramGatewayAdapter');
const {
  issueTrustedDevice,
  verifyTrustedDevice,
  issueAuthSession,
  rotateRefreshSession,
  revokeAllTrustedDevicesAndSessions,
  revokeDeviceAndSessions,
  isSessionFreshForSensitiveOp,
  markSessionStepUp,
} = require('../services/deviceTrustService');

function buildClientUserPayload(user) {
  return {
    id: user._id,
    name: user.name,
    email: user.email,
    phone: user.phoneE164 || user.phone,
    phoneE164: user.phoneE164 || user.phone,
    role: user.role,
    avatar: user.avatar,
    isVerified: user.isVerified === true,
    preferredLanguage: user.preferredLanguage || 'ar',
  };
}

function resolveClientMeta(req) {
  const ipAddress = (req.headers['x-forwarded-for'] || req.ip || '').toString().split(',')[0].trim();
  const userAgent = (req.headers['user-agent'] || '').toString().slice(0, 250);
  const deviceId = String(req.body?.deviceId || req.headers['x-device-id'] || '').trim().slice(0, 128);
  const deviceName = String(req.body?.deviceName || req.headers['x-device-name'] || 'OOZOX Device').trim().slice(0, 120);
  const platformRaw = String(req.body?.platform || req.headers['x-platform'] || 'ANDROID').toUpperCase();
  const platform = ['ANDROID', 'WEB', 'IOS', 'OTHER'].includes(platformRaw) ? platformRaw : 'ANDROID';
  const hardwareFingerprint = String(req.body?.hardwareFingerprint || req.headers['x-device-fingerprint'] || '').trim();
  const fcmToken = String(req.body?.fcmToken || '').trim();
  return { ipAddress, userAgent, deviceId, deviceName, platform, hardwareFingerprint, fcmToken };
}

function extractBearer(req) {
  const authHeader = req.headers.authorization || '';
  if (authHeader.startsWith('Bearer ')) {
    return authHeader.split(' ')[1];
  }
  return '';
}

function resolvePreAuthUserFromReq(req) {
  const rawToken = req.body?.preAuthToken || extractBearer(req);
  if (!rawToken) return null;
  try {
    return verifyToken(rawToken);
  } catch {
    return null;
  }
}

// @desc    Request OTP challenge (Telegram -> WhatsApp -> SMS or user-selected channel)
// @route   POST /api/auth/otp/request
// @access  Public (PreAuth / Phone Login / Recovery) or Private (Change Phone / Sensitive Step-up)
const requestOtp = async (req, res, next) => {
  try {
    const { purpose, channel = 'AUTO', allowFallback = true, phone, newPhone } = req.body;
    const meta = resolveClientMeta(req);
    let targetUser = null;
    let rawPhone = phone || newPhone || '';

    const decodedPre = resolvePreAuthUserFromReq(req);
    if (decodedPre && decodedPre.id) {
      targetUser = await User.findById(decodedPre.id);
    }

    if (!targetUser && rawPhone && (purpose === 'LOGIN' || purpose === 'RECOVER' || purpose === 'REGISTER')) {
      const normalized = normalizePhoneE164(rawPhone);
      if (normalized) {
        targetUser = await User.findOne({ $or: [{ phoneE164: normalized }, { phone: normalized }, { phone: rawPhone.trim() }] });
      }
    }

    if (purpose === 'CHANGE_PHONE') {
      if (!targetUser || decodedPre?.scope === 'PRE_AUTH') {
        return errorResponse(res, 401, req.t('api.auth_token_missing'));
      }
      rawPhone = newPhone || phone;
      const normalizedNew = normalizePhoneE164(rawPhone);
      if (!normalizedNew) {
        return errorResponse(res, 400, req.t('api.otp_invalid_phone'));
      }
      const conflict = await User.findOne({
        _id: { $ne: targetUser._id },
        $or: [{ phoneE164: normalizedNew }, { phone: normalizedNew }],
      });
      if (conflict) {
        return errorResponse(res, 400, req.t('api.phone_already_registered'));
      }
    } else if (purpose === 'RECOVER') {
      if (!targetUser) {
        return errorResponse(res, 404, req.t('api.user_not_found'));
      }
      rawPhone = targetUser.phoneE164 || targetUser.phone;
    } else if (purpose === 'SENSITIVE_ACTION') {
      if (!targetUser || decodedPre?.scope === 'PRE_AUTH') {
        return errorResponse(res, 401, req.t('api.auth_token_missing'));
      }
      rawPhone = targetUser.phoneE164 || targetUser.phone;
    } else if (!rawPhone && targetUser) {
      rawPhone = targetUser.phoneE164 || targetUser.phone;
    }

    if ((purpose === 'REGISTER' || purpose === 'DEVICE_TRUST') && !targetUser) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    if (purpose === 'LOGIN' && !targetUser) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    const result = await requestOtpChallenge({
      rawPhone,
      purpose,
      preferredChannel: channel,
      allowFallback,
      userId: targetUser ? targetUser._id : null,
      lang: (targetUser && targetUser.preferredLanguage) || req.lang || 'ar',
      metadata: meta,
    });

    const preAuthToken = targetUser
      ? generatePreAuthToken(targetUser._id, { purpose, challengeId: result.challengeId })
      : null;

    return successResponse(res, 200, req.t('api.otp_sent'), {
      challengeId: result.challengeId,
      activeChannel: result.activeChannel,
      attemptedChannels: result.attemptedChannels,
      expiresAt: result.expiresAt,
      cooldownSeconds: result.cooldownSeconds,
      maxAttempts: result.maxAttempts,
      maskedPhone: result.maskedPhone,
      preAuthToken,
    });
  } catch (err) {
    if (err.statusCode) {
      return res.status(err.statusCode).json({
        success: false,
        code: err.code || 'OTP_ERROR',
        retryAfterSeconds: err.retryAfterSeconds,
        attemptedChannels: err.attemptedChannels,
        message: err.message,
      });
    }
    next(err);
  }
};

// @desc    Verify OTP challenge and complete signup / login / device trust / phone change / step-up
// @route   POST /api/auth/otp/verify
// @access  Public (with valid challenge) or Private
const verifyOtp = async (req, res, next) => {
  try {
    const { challengeId, code, phone, trustDevice = true } = req.body;
    const meta = resolveClientMeta(req);
    const normalizedPhone = phone ? normalizePhoneE164(phone) : undefined;

    const challenge = await verifyOtpChallenge({
      challengeId,
      code,
      phoneE164: normalizedPhone || undefined,
    });

    const decodedPre = resolvePreAuthUserFromReq(req);
    let user = null;
    if (challenge.userId) {
      user = await User.findById(challenge.userId);
    } else if (decodedPre && decodedPre.id) {
      user = await User.findById(decodedPre.id);
    } else {
      user = await User.findOne({
        $or: [{ phoneE164: challenge.phoneE164 }, { phone: challenge.phoneE164 }],
      });
    }

    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    if (challenge.purpose === 'CHANGE_PHONE') {
      if (!decodedPre || decodedPre.scope === 'PRE_AUTH' || String(decodedPre.id) !== String(user._id)) {
        return errorResponse(res, 403, req.t('api.auth_forbidden_role', { role: 'PRE_AUTH' }));
      }
      const conflict = await User.findOne({
        _id: { $ne: user._id },
        $or: [{ phoneE164: challenge.phoneE164 }, { phone: challenge.phoneE164 }],
      });
      if (conflict) {
        return errorResponse(res, 400, req.t('api.phone_already_registered'));
      }
      user.phone = challenge.phoneE164;
      user.phoneE164 = challenge.phoneE164;
      user.phoneVerifiedAt = new Date();
      await user.save();
      if (decodedPre.sid) {
        await markSessionStepUp(decodedPre.sid);
      }
      await Notification.create({
        recipient: user._id,
        title: req.t('api.security_phone_changed_title'),
        message: req.t('api.security_phone_changed_body', { phone: maskPhone(user.phoneE164) }),
        type: 'SECURITY_ALERT',
        language: user.preferredLanguage || 'ar',
      }).catch(() => {});

      return successResponse(res, 200, req.t('api.otp_phone_updated'), {
        user: buildClientUserPayload(user),
      });
    }

    if (challenge.purpose === 'SENSITIVE_ACTION') {
      if (!decodedPre || decodedPre.scope === 'PRE_AUTH' || String(decodedPre.id) !== String(user._id)) {
        return errorResponse(res, 403, req.t('api.auth_forbidden_role', { role: 'PRE_AUTH' }));
      }
      const updatedSession = await markSessionStepUp(decodedPre.sid);
      return successResponse(res, 200, req.t('api.otp_verified'), {
        stepUpVerified: true,
        lastStepUpAt: updatedSession?.lastStepUpAt || new Date(),
      });
    }

    if (challenge.purpose === 'RECOVER') {
      const recoveryToken = generatePreAuthToken(user._id, {
        purpose: 'RECOVER_PASSWORD',
        challengeId: challenge.challengeId,
      });
      return successResponse(res, 200, req.t('api.otp_verified'), {
        recoveryToken,
        challengeId: challenge.challengeId,
        userId: user._id,
      });
    }

    // REGISTER, LOGIN, or DEVICE_TRUST:
    if (!user.isVerified || !user.phoneVerifiedAt) {
      user.isVerified = true;
      user.phoneVerifiedAt = user.phoneVerifiedAt || new Date();
      if (!user.phoneE164) {
        user.phoneE164 = challenge.phoneE164;
      }
      await user.save();
    }

    const deviceIdToUse = meta.deviceId || challenge.metadata?.deviceId || '';
    let trustedDevicePayload = null;
    if (trustDevice !== false && deviceIdToUse) {
      trustedDevicePayload = await issueTrustedDevice({
        userId: user._id,
        deviceId: deviceIdToUse,
        deviceName: meta.deviceName || challenge.metadata?.deviceName || 'OOZOX Device',
        platform: meta.platform || challenge.metadata?.platform || 'ANDROID',
        hardwareFingerprint: meta.hardwareFingerprint,
        ipAddress: meta.ipAddress,
        fcmToken: meta.fcmToken,
      });
    }

    const sessionPayload = await issueAuthSession({
      user,
      deviceId: deviceIdToUse,
      authMethod: challenge.purpose === 'REGISTER' ? 'OTP' : 'PASSWORD_OTP',
      otpVerifiedAt: challenge.verifiedAt || new Date(),
      userAgent: meta.userAgent,
      ipAddress: meta.ipAddress,
    });

    return successResponse(res, 200, req.t('api.otp_verified'), {
      user: buildClientUserPayload(user),
      token: sessionPayload.token,
      refreshToken: sessionPayload.refreshToken,
      sessionId: sessionPayload.sessionId,
      expiresAt: sessionPayload.expiresAt,
      trustedDevice: trustedDevicePayload,
    });
  } catch (err) {
    if (err.statusCode) {
      return res.status(err.statusCode).json({
        success: false,
        code: err.code || 'OTP_VERIFY_ERROR',
        remainingAttempts: err.remainingAttempts,
        retryAfterSeconds: err.retryAfterSeconds,
        message: err.message,
      });
    }
    next(err);
  }
};

// @desc    Passwordless / Biometric login on a server-verified Trusted Device (30-day window)
// @route   POST /api/auth/trusted-login
// @access  Public (requires valid server-issued deviceTrustToken)
const trustedDeviceLogin = async (req, res, next) => {
  try {
    const { emailOrPhone, userId, deviceId, deviceTrustToken, hardwareFingerprint } = req.body;
    const meta = resolveClientMeta(req);

    let user = null;
    if (userId) {
      user = await User.findById(userId);
    } else if (emailOrPhone) {
      const input = String(emailOrPhone).trim();
      const e164 = normalizePhoneE164(input);
      user = await User.findOne({
        $or: [
          { email: input.toLowerCase() },
          { phone: input },
          ...(e164 ? [{ phoneE164: e164 }] : []),
        ],
      });
    }

    if (!user || user.isVerified !== true) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    const trustCheck = await verifyTrustedDevice({
      user,
      deviceId: deviceId || meta.deviceId,
      deviceTrustToken,
      hardwareFingerprint: hardwareFingerprint || meta.hardwareFingerprint,
      ipAddress: meta.ipAddress,
    });

    if (!trustCheck.trusted) {
      const preAuthToken = generatePreAuthToken(user._id, {
        purpose: 'DEVICE_TRUST',
        reason: trustCheck.reason,
      });
      return res.status(401).json({
        success: false,
        requiresOtp: true,
        reason: trustCheck.reason,
        preAuthToken,
        maskedPhone: maskPhone(user.phoneE164 || user.phone),
        message: req.t('api.device_trust_otp_required'),
      });
    }

    const sessionPayload = await issueAuthSession({
      user,
      deviceId: trustCheck.device.deviceId,
      authMethod: 'TRUSTED_DEVICE',
      otpVerifiedAt: null,
      userAgent: meta.userAgent,
      ipAddress: meta.ipAddress,
    });

    return successResponse(res, 200, req.t('api.login_success'), {
      user: buildClientUserPayload(user),
      token: sessionPayload.token,
      refreshToken: sessionPayload.refreshToken,
      sessionId: sessionPayload.sessionId,
      expiresAt: sessionPayload.expiresAt,
      trustedUntil: trustCheck.device.trustedUntil,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Rotate refresh token & issue new access JWT
// @route   POST /api/auth/session/refresh
// @access  Public (with valid refreshToken)
const refreshSession = async (req, res, next) => {
  try {
    const meta = resolveClientMeta(req);
    const rotated = await rotateRefreshSession({
      refreshToken: req.body.refreshToken,
      deviceId: meta.deviceId,
      ipAddress: meta.ipAddress,
      userAgent: meta.userAgent,
    });
    return successResponse(res, 200, req.t('api.session_refreshed'), {
      token: rotated.token,
      refreshToken: rotated.refreshToken,
      sessionId: rotated.sessionId,
      expiresAt: rotated.expiresAt,
      user: buildClientUserPayload(rotated.user),
    });
  } catch (err) {
    if (err.statusCode) {
      return errorResponse(res, err.statusCode, err.message);
    }
    next(err);
  }
};

// @desc    List user's trusted devices
// @route   GET /api/auth/devices
// @access  Private
const listTrustedDevices = async (req, res, next) => {
  try {
    const currentDeviceId = String(req.headers['x-device-id'] || '').trim();
    const devices = await TrustedDevice.find({ userId: req.user._id }).sort({ lastActiveAt: -1 });
    const now = new Date();
    const formatted = devices.map((d) => ({
      deviceId: d.deviceId,
      deviceName: d.deviceName,
      platform: d.platform,
      trustedAt: d.trustedAt,
      trustedUntil: d.trustedUntil,
      lastActiveAt: d.lastActiveAt,
      lastIp: d.lastIp,
      revokedAt: d.revokedAt,
      revokeReason: d.revokeReason,
      isActive: !d.revokedAt && d.trustedUntil > now,
      isCurrent: Boolean(currentDeviceId && d.deviceId === currentDeviceId),
    }));
    return successResponse(res, 200, req.t('api.devices_fetched'), { devices: formatted });
  } catch (err) {
    next(err);
  }
};

// @desc    Revoke a trusted device and all its sessions
// @route   DELETE /api/auth/devices/:deviceId
// @access  Private
const revokeTrustedDevice = async (req, res, next) => {
  try {
    const { deviceId } = req.params;
    const revoked = await revokeDeviceAndSessions(req.user._id, deviceId, 'USER_REVOKED');
    if (!revoked) {
      return errorResponse(res, 404, req.t('api.device_not_found'));
    }
    return successResponse(res, 200, req.t('api.device_revoked'), { deviceId: revoked.deviceId });
  } catch (err) {
    next(err);
  }
};

// @desc    List active auth sessions
// @route   GET /api/auth/sessions
// @access  Private
const listSessions = async (req, res, next) => {
  try {
    const sessions = await AuthSession.find({
      userId: req.user._id,
      revokedAt: null,
      expiresAt: { $gt: new Date() },
    }).sort({ createdAt: -1 });

    return successResponse(res, 200, req.t('api.sessions_fetched'), {
      sessions: sessions.map((s) => ({
        sessionId: s.sessionId,
        deviceId: s.deviceId,
        authMethod: s.authMethod,
        issuedAt: s.issuedAt,
        expiresAt: s.expiresAt,
        lastStepUpAt: s.lastStepUpAt,
        ipAddress: s.ipAddress,
        isCurrent: s.sessionId === req.authSessionId,
      })),
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Revoke a specific auth session
// @route   DELETE /api/auth/sessions/:sessionId
// @access  Private
const revokeSession = async (req, res, next) => {
  try {
    const session = await AuthSession.findOneAndUpdate(
      { userId: req.user._id, sessionId: req.params.sessionId, revokedAt: null },
      { $set: { revokedAt: new Date() } },
      { new: true }
    );
    if (!session) {
      return errorResponse(res, 404, req.t('api.session_not_found'));
    }
    return successResponse(res, 200, req.t('api.session_revoked'), { sessionId: session.sessionId });
  } catch (err) {
    next(err);
  }
};

// @desc    Recover account (step 1: send OTP to phone; step 2: confirm OTP + reset password)
// @route   POST /api/auth/recover
// @access  Public
const recoverAccount = async (req, res, next) => {
  try {
    const { phone, channel = 'AUTO', challengeId, code, newPassword } = req.body;
    const meta = resolveClientMeta(req);

    // Step 2: verify code + reset password
    if (challengeId && code && newPassword) {
      const challenge = await verifyOtpChallenge({ challengeId, code });
      if (challenge.purpose !== 'RECOVER') {
        return errorResponse(res, 400, req.t('api.otp_invalid_purpose'));
      }
      const user = await User.findById(challenge.userId).select('+password');
      if (!user) {
        return errorResponse(res, 404, req.t('api.user_not_found'));
      }
      user.password = newPassword;
      user.isVerified = true;
      user.phoneVerifiedAt = user.phoneVerifiedAt || new Date();
      user.passwordChangedAt = new Date();
      await user.save();

      await revokeAllTrustedDevicesAndSessions(user._id, 'ACCOUNT_RECOVERY');
      const trustedDevice = meta.deviceId
        ? await issueTrustedDevice({
            userId: user._id,
            deviceId: meta.deviceId,
            deviceName: meta.deviceName,
            platform: meta.platform,
            hardwareFingerprint: meta.hardwareFingerprint,
            ipAddress: meta.ipAddress,
            fcmToken: meta.fcmToken,
          })
        : null;
      const sessionPayload = await issueAuthSession({
        user,
        deviceId: meta.deviceId,
        authMethod: 'OTP',
        otpVerifiedAt: new Date(),
        userAgent: meta.userAgent,
        ipAddress: meta.ipAddress,
      });

      return successResponse(res, 200, req.t('api.password_recovered'), {
        user: buildClientUserPayload(user),
        token: sessionPayload.token,
        refreshToken: sessionPayload.refreshToken,
        sessionId: sessionPayload.sessionId,
        trustedDevice,
      });
    }

    // Step 1: request RECOVER OTP
    const normalized = normalizePhoneE164(phone || '');
    if (!normalized) {
      return errorResponse(res, 400, req.t('api.otp_invalid_phone'));
    }
    const user = await User.findOne({
      $or: [{ phoneE164: normalized }, { phone: normalized }, { phone: String(phone).trim() }],
    });
    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    const result = await requestOtpChallenge({
      rawPhone: user.phoneE164 || user.phone,
      purpose: 'RECOVER',
      preferredChannel: channel,
      allowFallback: true,
      userId: user._id,
      lang: user.preferredLanguage || req.lang || 'ar',
      metadata: meta,
    });

    return successResponse(res, 200, req.t('api.otp_sent'), result);
  } catch (err) {
    if (err.statusCode) {
      return res.status(err.statusCode).json({
        success: false,
        code: err.code || 'RECOVER_ERROR',
        retryAfterSeconds: err.retryAfterSeconds,
        remainingAttempts: err.remainingAttempts,
        message: err.message,
      });
    }
    next(err);
  }
};

// @desc    Change password (requires fresh session / step-up OTP; revokes all trusted devices & old sessions)
// @route   POST /api/auth/change-password
// @access  Private
const changePassword = async (req, res, next) => {
  try {
    const { currentPassword, newPassword, stepUpChallengeId, stepUpCode } = req.body;
    const meta = resolveClientMeta(req);

    const user = await User.findById(req.user._id).select('+password');
    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    const matchesCurrent = await user.matchPassword(currentPassword);
    if (!matchesCurrent) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    let isFresh = await isSessionFreshForSensitiveOp(req.authSessionId);
    if (!isFresh && stepUpChallengeId && stepUpCode) {
      const verified = await verifyOtpChallenge({
        challengeId: stepUpChallengeId,
        code: stepUpCode,
        phoneE164: user.phoneE164 || normalizePhoneE164(user.phone),
      });
      if (verified && verified.purpose === 'SENSITIVE_ACTION') {
        isFresh = true;
      }
    }

    if (!isFresh) {
      return res.status(403).json({
        success: false,
        requiresStepUpOtp: true,
        code: 'STEP_UP_OTP_REQUIRED',
        maskedPhone: maskPhone(user.phoneE164 || user.phone),
        message: req.t('api.step_up_otp_required'),
      });
    }

    user.password = newPassword;
    user.passwordChangedAt = new Date();
    await user.save();

    // Password change immediately invalidates all 30-day trusted devices and prior sessions
    await revokeAllTrustedDevicesAndSessions(user._id, 'PASSWORD_CHANGED');

    const newSession = await issueAuthSession({
      user,
      deviceId: meta.deviceId,
      authMethod: 'PASSWORD_OTP',
      otpVerifiedAt: new Date(),
      userAgent: meta.userAgent,
      ipAddress: meta.ipAddress,
    });

    await Notification.create({
      recipient: user._id,
      title: req.t('api.security_password_changed_title'),
      message: req.t('api.security_password_changed_body'),
      type: 'SECURITY_ALERT',
      language: user.preferredLanguage || 'ar',
    }).catch(() => {});

    return successResponse(res, 200, req.t('api.password_changed'), {
      token: newSession.token,
      refreshToken: newSession.refreshToken,
      sessionId: newSession.sessionId,
      allDevicesRevoked: true,
    });
  } catch (err) {
    if (err.statusCode) {
      return res.status(err.statusCode).json({
        success: false,
        code: err.code || 'CHANGE_PASSWORD_ERROR',
        remainingAttempts: err.remainingAttempts,
        message: err.message,
      });
    }
    next(err);
  }
};

// @desc    Initiate phone number change via server OTP to new phone
// @route   POST /api/auth/change-phone
// @access  Private
const initiateChangePhone = async (req, res, next) => {
  try {
    const { newPhone, channel = 'AUTO', stepUpChallengeId, stepUpCode } = req.body;
    const meta = resolveClientMeta(req);
    const normalizedNew = normalizePhoneE164(newPhone || '');
    if (!normalizedNew) {
      return errorResponse(res, 400, req.t('api.otp_invalid_phone'));
    }

    const user = await User.findById(req.user._id);
    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    let isFresh = await isSessionFreshForSensitiveOp(req.authSessionId);
    if (!isFresh && stepUpChallengeId && stepUpCode) {
      const verified = await verifyOtpChallenge({
        challengeId: stepUpChallengeId,
        code: stepUpCode,
        phoneE164: user.phoneE164 || normalizePhoneE164(user.phone),
      });
      if (verified && verified.purpose === 'SENSITIVE_ACTION') {
        isFresh = true;
      }
    }

    if (!isFresh) {
      return res.status(403).json({
        success: false,
        requiresStepUpOtp: true,
        code: 'STEP_UP_OTP_REQUIRED',
        maskedPhone: maskPhone(user.phoneE164 || user.phone),
        message: req.t('api.step_up_otp_required'),
      });
    }

    const conflict = await User.findOne({
      _id: { $ne: user._id },
      $or: [{ phoneE164: normalizedNew }, { phone: normalizedNew }],
    });
    if (conflict) {
      return errorResponse(res, 400, req.t('api.phone_already_registered'));
    }

    const result = await requestOtpChallenge({
      rawPhone: normalizedNew,
      purpose: 'CHANGE_PHONE',
      preferredChannel: channel,
      allowFallback: true,
      userId: user._id,
      lang: user.preferredLanguage || req.lang || 'ar',
      metadata: meta,
    });

    return successResponse(res, 200, req.t('api.otp_sent'), result);
  } catch (err) {
    if (err.statusCode) {
      return res.status(err.statusCode).json({
        success: false,
        code: err.code || 'CHANGE_PHONE_ERROR',
        retryAfterSeconds: err.retryAfterSeconds,
        message: err.message,
      });
    }
    next(err);
  }
};

// @desc    Telegram Gateway signed delivery status webhook
// @route   POST /api/auth/webhooks/telegram-gateway
// @access  Signed Telegram Gateway Webhook (X-Request-Timestamp + X-Request-Signature)
const telegramGatewayWebhook = async (req, res, next) => {
  try {
    const timestampHeader = req.headers['x-request-timestamp'];
    const signatureHeader = req.headers['x-request-signature'];
    const rawBody = req.rawBody || JSON.stringify(req.body || {});

    const validSig = verifyWebhookSignature({
      timestampHeader,
      signatureHeader,
      rawBody,
    });
    if (!validSig) {
      return res.status(401).json({ success: false, message: 'Invalid Telegram Gateway signature' });
    }

    const requestId = req.body?.request_id || '';
    const deliveryStatus = req.body?.delivery_status?.status || '';
    if (requestId && deliveryStatus) {
      await OtpChallenge.findOneAndUpdate(
        { providerRef: String(requestId), providerType: 'TELEGRAM_GATEWAY' },
        { $set: { deliveryStatus: String(deliveryStatus).slice(0, 40) } }
      );
    }

    return res.status(200).json({ ok: true });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  buildClientUserPayload,
  resolveClientMeta,
  requestOtp,
  verifyOtp,
  trustedDeviceLogin,
  refreshSession,
  listTrustedDevices,
  revokeTrustedDevice,
  listSessions,
  revokeSession,
  recoverAccount,
  changePassword,
  initiateChangePhone,
  telegramGatewayWebhook,
};
