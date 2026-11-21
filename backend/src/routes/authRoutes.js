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

router.post('/register', registerLimiter, validate(schemas.register), registerUser);
router.post('/login', loginLimiter, validate(schemas.login), loginUser);
router.get('/me', protect, getMe);
router.patch('/language', protect, validate(schemas.updateLanguage), updateLanguagePreference);
router.get('/search', protect, validate(schemas.searchUsersQuery, 'query'), searchUsers);

module.exports = router;
