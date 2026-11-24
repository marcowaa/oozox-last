const User = require('../models/User');
const { generatePreAuthToken } = require('../utils/jwt');
const { successResponse, errorResponse } = require('../utils/response');
const { escapeRegex } = require('../middlewares/sanitizeMiddleware');
const { normalizeLang } = require('../i18n');
const { normalizePhoneE164, looksLikePhone, maskPhone } = require('../utils/phoneUtils');
const { requestOtpChallenge } = require('../services/otp/otpOrchestrator');
const { verifyTrustedDevice, issueAuthSession } = require('../services/deviceTrustService');
const { buildClientUserPayload, resolveClientMeta } = require('./otpController');

// @desc    Register a new user (creates unverified account and initiates mandatory server OTP)
// @route   POST /api/auth/register
// @access  Public
const registerUser = async (req, res, next) => {
  try {
    const {
      name,
      email,
      phone,
      password,
      role,
      preferredLanguage,
      preferredChannel = 'AUTO',
      autoSendOtp = true,
    } = req.body;
    const normalizedEmail = email.trim().toLowerCase();
    const phoneE164 = normalizePhoneE164(phone);
    if (!phoneE164) {
      return errorResponse(res, 400, req.t('api.otp_invalid_phone'));
    }

    // Strict RBAC guard: public registration may only assign USER or MERCHANT
    const safeRole = role === 'MERCHANT' ? 'MERCHANT' : 'USER';
    const userLang = normalizeLang(preferredLanguage || req.lang);
    const meta = resolveClientMeta(req);

    const existing = await User.findOne({
      $or: [{ email: normalizedEmail }, { phoneE164 }, { phone: phoneE164 }, { phone: phone.trim() }],
    });

    let user = null;
    if (existing) {
      // Allow re-trying unverified registration for the exact same email+phone without leaking verified accounts
      if (
        existing.isVerified === false &&
        existing.email === normalizedEmail &&
        (existing.phoneE164 === phoneE164 || existing.phone === phoneE164)
      ) {
        existing.name = name.trim();
        existing.password = password;
        existing.role = safeRole;
        existing.preferredLanguage = userLang;
        user = await existing.save();
      } else {
        return errorResponse(
          res,
          400,
          existing.email === normalizedEmail
            ? req.t('api.email_already_registered')
            : req.t('api.phone_already_registered')
        );
      }
    } else {
      user = await User.create({
        name: name.trim(),
        email: normalizedEmail,
        phone: phoneE164,
        phoneE164,
        password,
        role: safeRole,
        isVerified: false,
        preferredLanguage: userLang,
      });
    }

    const preAuthToken = generatePreAuthToken(user._id, { purpose: 'REGISTER' });
    let otpChallenge = null;
    let otpDeliveryError = null;

    if (autoSendOtp !== false) {
      try {
        otpChallenge = await requestOtpChallenge({
          rawPhone: phoneE164,
          purpose: 'REGISTER',
          preferredChannel,
          allowFallback: true,
          userId: user._id,
          lang: userLang,
          metadata: meta,
        });
      } catch (otpErr) {
        otpDeliveryError = {
          code: otpErr.code || 'OTP_DELIVERY_UNAVAILABLE',
          message: otpErr.message,
          retryAfterSeconds: otpErr.retryAfterSeconds,
          attemptedChannels: otpErr.attemptedChannels,
        };
      }
    }

    return successResponse(res, 201, req.t('api.register_otp_required'), {
      requiresOtp: true,
      reason: 'REGISTER_VERIFICATION',
      preAuthToken,
      maskedPhone: maskPhone(phoneE164),
      availableChannels: ['TELEGRAM', 'WHATSAPP', 'SMS'],
      otpChallenge,
      otpDeliveryError,
      user: buildClientUserPayload(user),
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Authenticate user (enforces OTP on phone login, unverified accounts, and untrusted devices)
// @route   POST /api/auth/login
// @access  Public
const loginUser = async (req, res, next) => {
  try {
    const {
      emailOrPhone,
      password,
      deviceId,
      deviceTrustToken,
      hardwareFingerprint,
      preferredChannel = 'AUTO',
      autoSendOtp = false,
    } = req.body;

    if (typeof emailOrPhone !== 'string' || typeof password !== 'string') {
      return errorResponse(res, 400, req.t('api.invalid_login_payload'));
    }

    const input = emailOrPhone.trim();
    const isPhoneLogin = looksLikePhone(input);
    const normalizedPhone = isPhoneLogin ? normalizePhoneE164(input) : null;

    const user = await User.findOne({
      $or: [
        { email: input.toLowerCase() },
        { phone: input },
        ...(normalizedPhone ? [{ phoneE164: normalizedPhone }, { phone: normalizedPhone }] : []),
      ],
    }).select('+password');

    if (!user) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    const isMatch = await user.matchPassword(password);
    if (!isMatch) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    const meta = resolveClientMeta(req);
    const effectiveDeviceId = deviceId || meta.deviceId;
    let otpReason = null;

    if (user.isVerified !== true) {
      otpReason = 'ACCOUNT_UNVERIFIED';
    } else if (isPhoneLogin) {
      // Policy decision: phone login ALWAYS requires server-side OTP
      otpReason = 'PHONE_LOGIN';
    } else {
      const trustCheck = await verifyTrustedDevice({
        user,
        deviceId: effectiveDeviceId,
        deviceTrustToken,
        hardwareFingerprint: hardwareFingerprint || meta.hardwareFingerprint,
        ipAddress: meta.ipAddress,
      });
      if (!trustCheck.trusted) {
        otpReason = trustCheck.reason || 'UNTRUSTED_DEVICE';
      }
    }

    if (otpReason) {
      const purpose = user.isVerified !== true ? 'REGISTER' : 'LOGIN';
      const preAuthToken = generatePreAuthToken(user._id, { purpose, reason: otpReason });
      let otpChallenge = null;
      let otpDeliveryError = null;

      if (autoSendOtp) {
        try {
          otpChallenge = await requestOtpChallenge({
            rawPhone: user.phoneE164 || user.phone,
            purpose,
            preferredChannel,
            allowFallback: true,
            userId: user._id,
            lang: user.preferredLanguage || req.lang || 'ar',
            metadata: meta,
          });
        } catch (otpErr) {
          otpDeliveryError = {
            code: otpErr.code || 'OTP_DELIVERY_UNAVAILABLE',
            message: otpErr.message,
            retryAfterSeconds: otpErr.retryAfterSeconds,
          };
        }
      }

      return successResponse(res, 200, req.t('api.login_otp_required'), {
        requiresOtp: true,
        reason: otpReason,
        purpose,
        preAuthToken,
        maskedPhone: maskPhone(user.phoneE164 || user.phone),
        availableChannels: ['TELEGRAM', 'WHATSAPP', 'SMS'],
        otpChallenge,
        otpDeliveryError,
        user: buildClientUserPayload(user),
      });
    }

    const sessionPayload = await issueAuthSession({
      user,
      deviceId: effectiveDeviceId,
      authMethod: 'TRUSTED_DEVICE',
      otpVerifiedAt: null,
      userAgent: meta.userAgent,
      ipAddress: meta.ipAddress,
    });

    return successResponse(res, 200, req.t('api.login_success'), {
      requiresOtp: false,
      user: buildClientUserPayload(user),
      token: sessionPayload.token,
      refreshToken: sessionPayload.refreshToken,
      sessionId: sessionPayload.sessionId,
      expiresAt: sessionPayload.expiresAt,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get current user profile
// @route   GET /api/auth/me
// @access  Private
const getMe = async (req, res, next) => {
  try {
    const user = await User.findById(req.user._id);
    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }
    return successResponse(res, 200, req.t('api.profile_fetched'), { user });
  } catch (error) {
    next(error);
  }
};

// @desc    Update preferred notification/UI language for authenticated user
// @route   PATCH /api/auth/language
// @access  Private
const updateLanguagePreference = async (req, res, next) => {
  try {
    const lang = normalizeLang(req.body.language);
    const user = await User.findByIdAndUpdate(
      req.user._id,
      { preferredLanguage: lang },
      { new: true }
    );
    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }
    return successResponse(res, 200, req.t('api.language_preference_updated'), {
      preferredLanguage: user.preferredLanguage,
      user,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Search users by phone or email
// @route   GET /api/auth/search
// @access  Private
const searchUsers = async (req, res, next) => {
  try {
    const qRaw = typeof req.query.q === 'string' ? req.query.q.trim() : '';
    if (qRaw.length < 2) {
      return successResponse(res, 200, req.t('api.search_empty'), { users: [] });
    }

    // ReDoS-safe escaped regex bounded to 64 chars
    const safePattern = escapeRegex(qRaw.slice(0, 64));
    const regex = new RegExp(safePattern, 'i');
    const users = await User.find({
      _id: { $ne: req.user._id },
      $or: [{ name: regex }, { phone: regex }, { email: regex }],
    })
      .select('name phone email role avatar preferredLanguage')
      .limit(20);

    return successResponse(res, 200, req.t('api.search_results'), { users });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  registerUser,
  loginUser,
  getMe,
  updateLanguagePreference,
  searchUsers,
};
