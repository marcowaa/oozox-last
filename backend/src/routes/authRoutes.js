const express = require('express');
const rateLimit = require('express-rate-limit');
const router = express.Router();
const {
  registerUser,
  loginUser,
  getMe,
  updateLanguagePreference,
  searchUsers,
} = require('../controllers/authController');
const {
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
} = require('../controllers/otpController');
const { protect } = require('../middlewares/authMiddleware');
const { validate, schemas } = require('../middlewares/validateMiddleware');

// Strict brute-force limiter on authentication endpoints
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.LOGIN_RATE_LIMIT_MAX) || 10,
  skipSuccessfulRequests: true,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) =>
    res.status(429).json({
      success: false,
      message: req.t ? req.t('api.login_rate_limited') : 'Too many login attempts',
    }),
});

const registerLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.REGISTER_RATE_LIMIT_MAX) || 20,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) =>
    res.status(429).json({
      success: false,
      message: req.t ? req.t('api.register_rate_limited') : 'Too many registration requests',
    }),
});

const otpIpLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.OTP_IP_RATE_LIMIT_MAX) || 25,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) =>
    res.status(429).json({
      success: false,
      code: 'OTP_IP_RATE_LIMITED',
      message: req.t ? req.t('api.rate_limit') : 'Too many OTP requests, please try again later.',
    }),
});

router.post('/register', registerLimiter, validate(schemas.register), registerUser);
router.post('/login', loginLimiter, validate(schemas.login), loginUser);
router.post('/trusted-login', loginLimiter, validate(schemas.trustedLogin), trustedDeviceLogin);
router.post('/otp/request', otpIpLimiter, validate(schemas.otpRequest), requestOtp);
router.post('/otp/verify', loginLimiter, validate(schemas.otpVerify), verifyOtp);
router.post('/recover', otpIpLimiter, validate(schemas.recoverAccount), recoverAccount);
router.post('/session/refresh', validate(schemas.refreshSession), refreshSession);
router.post('/webhooks/telegram-gateway', telegramGatewayWebhook);

router.get('/me', protect, getMe);
router.patch('/language', protect, validate(schemas.updateLanguage), updateLanguagePreference);
router.get('/search', protect, validate(schemas.searchUsersQuery, 'query'), searchUsers);

router.get('/devices', protect, listTrustedDevices);
router.delete('/devices/:deviceId', protect, revokeTrustedDevice);
router.get('/sessions', protect, listSessions);
router.delete('/sessions/:sessionId', protect, revokeSession);
router.post('/change-password', protect, validate(schemas.changePassword), changePassword);
router.post('/change-phone', protect, otpIpLimiter, validate(schemas.changePhone), initiateChangePhone);

module.exports = router;
