const express = require('express');
const rateLimit = require('express-rate-limit');
const { z } = require('zod');
const { validate } = require('../middlewares/validateMiddleware');
const {
  requestOtp,
  verifyOtp,
  startTelegramLink,
} = require('../controllers/otpController');

const router = express.Router();

const requestLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.OTP_REQUEST_RATE_LIMIT_MAX) || 8,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (_req, res) =>
    res.status(429).json({
      success: false,
      code: 'OTP_REQUEST_RATE_LIMITED',
      message: 'Too many verification requests. Try again later.',
    }),
});

const verifyLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.OTP_VERIFY_RATE_LIMIT_MAX) || 20,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (_req, res) =>
    res.status(429).json({
      success: false,
      code: 'OTP_VERIFY_RATE_LIMITED',
      message: 'Too many verification attempts. Try again later.',
    }),
});

const requestSchema = z.object({
  phone: z.string().trim().min(9).max(16).regex(/^\+[1-9]\d{7,14}$/),
  channel: z.enum(['sms', 'whatsapp', 'telegram']),
});

const verifySchema = z.object({
  challengeId: z.string().min(32).max(64),
  code: z.string().trim().regex(/^\d{6}$/),
});

router.post('/request', requestLimiter, validate(requestSchema), requestOtp);
router.post('/verify', verifyLimiter, validate(verifySchema), verifyOtp);
router.post('/telegram/link/start', requestLimiter, startTelegramLink);

module.exports = router;
