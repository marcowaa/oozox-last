const express = require('express');
const { telegramWebhook } = require('../controllers/telegramWebhookController');

const router = express.Router();

router.post('/', telegramWebhook);

module.exports = router;
