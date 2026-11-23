const crypto = require('crypto');
const {
  consumeTelegramStart,
  sendTelegramMessage,
} = require('../services/otpService');

const safeEqual = (left, right) => {
  const a = Buffer.from(String(left || ''));
  const b = Buffer.from(String(right || ''));
  return a.length > 0 && a.length === b.length && crypto.timingSafeEqual(a, b);
};

const telegramWebhook = async (req, res) => {
  const expectedSecret = (process.env.TELEGRAM_WEBHOOK_SECRET || '').trim();
  const suppliedSecret = req.get('X-Telegram-Bot-Api-Secret-Token') || '';

  if (!expectedSecret || !safeEqual(expectedSecret, suppliedSecret)) {
    return res.status(403).json({ success: false });
  }

  const message = req.body && req.body.message;
  const chatId = message && message.chat && message.chat.id;
  const telegramUserId = message && message.from && message.from.id;
  const text = typeof (message && message.text) === 'string' ? message.text.trim() : '';
  const startMatch = text.match(/^\/start(?:@\w+)?\s+([A-Za-z0-9_-]{20,64})$/);

  if (!startMatch || chatId === undefined || chatId === null) {
    return res.status(200).json({ success: true, ignored: true });
  }

  try {
    const linked = await consumeTelegramStart({
      token: startMatch[1],
      chatId,
      telegramUserId,
    });

    if (linked) {
      await sendTelegramMessage(
        chatId,
        'Your Telegram account is now linked to your verified OOZOX phone number. You can return to OOZOX and choose Telegram for future verification.'
      );
    } else {
      await sendTelegramMessage(
        chatId,
        'This OOZOX account-link request is invalid or expired. Start the linking process again from the OOZOX app.'
      );
    }

    return res.status(200).json({ success: true });
  } catch (error) {
    console.error('[OTP] Telegram webhook processing failed:', error.message);
    return res.status(200).json({ success: true });
  }
};

module.exports = { telegramWebhook };
