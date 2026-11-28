const express = require('express');
const rateLimit = require('express-rate-limit');
const router = express.Router();
const { handleAiChat, handleAiVision, getAiQuota } = require('../controllers/aiController');
const { protect } = require('../middlewares/authMiddleware');
const { validate, schemas } = require('../middlewares/validateMiddleware');

const aiRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: Number(process.env.AI_IP_RATE_LIMIT_PER_MIN) || 20,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) =>
    res.status(429).json({
      success: false,
      code: 'AI_RATE_LIMITED',
      message: req.t ? req.t('api.rate_limit') : 'Too many AI requests, please try again shortly.',
    }),
});

router.get('/quota', protect, getAiQuota);
router.post('/chat', protect, aiRateLimiter, validate(schemas.aiChat), handleAiChat);
router.post('/vision', protect, aiRateLimiter, validate(schemas.aiVision), handleAiVision);

module.exports = router;
