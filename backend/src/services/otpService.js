const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const OtpChallenge = require('../models/OtpChallenge');
const PhoneIdentity = require('../models/PhoneIdentity');
const TelegramLink = require('../models/TelegramLink');
const { getJwtSecret } = require('../utils/jwt');

const OTP_TTL_SECONDS = Number(process.env.OTP_TTL_SECONDS) || 300;
const OTP_RESEND_COOLDOWN_SECONDS = Number(process.env.OTP_RESEND_COOLDOWN_SECONDS) || 60;
const OTP_MAX_ATTEMPTS = Number(process.env.OTP_MAX_ATTEMPTS) || 5;
const TELEGRAM_LINK_TTL_SECONDS = 10 * 60;

class OtpError extends Error {
  constructor(statusCode, code, message) {
    super(message);
    this.name = 'OtpError';
    this.statusCode = statusCode;
    this.code = code;
  }
}

const sha256 = (value) =>
  crypto.createHash('sha256').update(value, 'utf8').digest('hex');

const otpDigest = (challengeId, phone, code) => {
  const secret = process.env.OTP_HMAC_SECRET || getJwtSecret();
  return crypto
    .createHmac('sha256', secret)
    .update([challengeId, phone, code].join(':'))
    .digest('hex');
};

const safeEqual = (left, right) => {
  const a = Buffer.from(String(left));
  const b = Buffer.from(String(right));
  return a.length === b.length && crypto.timingSafeEqual(a, b);
};

const normalizePhone = (phone) => {
  if (typeof phone !== 'string') {
    throw new OtpError(400, 'OTP_PHONE_INVALID', 'Enter a valid phone number in international format.');
  }
  const normalized = phone.trim().replace(/[()\s-]/g, '');
  if (!/^\+[1-9]\d{7,14}$/.test(normalized)) {
    throw new OtpError(400, 'OTP_PHONE_INVALID', 'Enter a valid phone number in international format.');
  }
  return normalized;
};

const getTwilioCredentials = () => {
  const accountSid = (process.env.TWILIO_ACCOUNT_SID || '').trim();
  const username = (process.env.TWILIO_API_KEY_SID || accountSid).trim();
  const password = (process.env.TWILIO_API_KEY_SECRET || process.env.TWILIO_AUTH_TOKEN || '').trim();
  const serviceSid = (process.env.TWILIO_VERIFY_SERVICE_SID || '').trim();

  if (!username || !password || !serviceSid) {
    throw new OtpError(503, 'OTP_CHANNEL_NOT_CONFIGURED', 'This OTP delivery channel is not configured yet.');
  }

  return { username, password, serviceSid };
};

const twilioRequest = async (endpoint, fields) => {
  const { username, password, serviceSid } = getTwilioCredentials();
  const credentials = Buffer.from(username + ':' + password).toString('base64');
  const response = await fetch(
    'https://verify.twilio.com/v2/Services/' + encodeURIComponent(serviceSid) + '/' + endpoint,
    {
      method: 'POST',
      headers: {
        Authorization: 'Basic ' + credentials,
        'Content-Type': 'application/x-www-form-urlencoded',
        Accept: 'application/json',
      },
      body: new URLSearchParams(fields),
      signal: AbortSignal.timeout(10000),
    }
  );

  let payload = {};
  try {
    payload = await response.json();
  } catch (_) {
    payload = {};
  }

  return { response, payload };
};

const sendTwilioOtp = async (phone, channel) => {
  const providerChannel = channel === 'SMS' ? 'sms' : 'whatsapp';
  const { response, payload } = await twilioRequest('Verifications', {
    To: phone,
    Channel: providerChannel,
  });

  if (!response.ok) {
    console.error('[OTP] Twilio send failed; HTTP ' + response.status + ', provider code ' + (payload.code || 'unknown'));
    throw new OtpError(503, 'OTP_PROVIDER_SEND_FAILED', 'The verification provider could not send the code. Try again later.');
  }

  if (payload.status && payload.status !== 'pending') {
    throw new OtpError(503, 'OTP_PROVIDER_SEND_FAILED', 'The verification provider did not accept the request.');
  }
};

const checkTwilioOtp = async (phone, code) => {
  const { response, payload } = await twilioRequest('VerificationCheck', {
    To: phone,
    Code: code,
  });

  if (!response.ok) {
    if (response.status === 400 || response.status === 404) return false;
    console.error('[OTP] Twilio check failed; HTTP ' + response.status + ', provider code ' + (payload.code || 'unknown'));
    throw new OtpError(503, 'OTP_PROVIDER_CHECK_FAILED', 'The verification provider is temporarily unavailable.');
  }

  return payload.status === 'approved';
};

const sendTelegramMessage = async (chatId, text) => {
  const botToken = (process.env.TELEGRAM_BOT_TOKEN || '').trim();
  if (!botToken) {
    throw new OtpError(503, 'TELEGRAM_NOT_CONFIGURED', 'Telegram delivery is not configured yet.');
  }

  const response = await fetch('https://api.telegram.org/bot' + botToken + '/sendMessage', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ chat_id: chatId, text }),
    signal: AbortSignal.timeout(10000),
  });

  let payload = {};
  try {
    payload = await response.json();
  } catch (_) {
    payload = {};
  }

  if (!response.ok || payload.ok !== true) {
    console.error('[OTP] Telegram send failed; HTTP ' + response.status);
    throw new OtpError(503, 'OTP_PROVIDER_SEND_FAILED', 'Telegram could not receive the code. Try again later.');
  }
};

const issueChallenge = async ({ phone, channel, provider, codeHash }) => {
  const challengeId = crypto.randomBytes(32).toString('base64url');
  const now = new Date();
  const expiresAt = new Date(now.getTime() + OTP_TTL_SECONDS * 1000);

  await OtpChallenge.create({
    tokenHash: sha256(challengeId),
    phone,
    channel,
    provider,
    codeHash,
    createdAt: now,
    expiresAt,
    status: 'PENDING',
  });

  return { challengeId, expiresAt };
};

const requestOtp = async ({ phone: rawPhone, channel: rawChannel }) => {
  const phone = normalizePhone(rawPhone);
  const channel = String(rawChannel || '').toUpperCase();

  if (!['SMS', 'WHATSAPP', 'TELEGRAM'].includes(channel)) {
    throw new OtpError(400, 'OTP_CHANNEL_INVALID', 'Choose SMS, WhatsApp, or Telegram.');
  }

  const now = new Date();
  const cooldownStart = new Date(now.getTime() - OTP_RESEND_COOLDOWN_SECONDS * 1000);
  const recent = await OtpChallenge.findOne({
    phone,
    channel,
    status: 'PENDING',
    createdAt: { $gt: cooldownStart },
  }).select('_id');

  if (recent) {
    throw new OtpError(429, 'OTP_RESEND_COOLDOWN', 'Wait before requesting another code.');
  }

  let provider;
  let telegramChatId = null;

  if (channel === 'TELEGRAM') {
    const identity = await PhoneIdentity.findOne({ phone, telegramChatId: { $ne: null } }).select('telegramChatId');
    if (!identity) {
      throw new OtpError(
        409,
        'TELEGRAM_LINK_REQUIRED',
        'Verify this phone through SMS or WhatsApp first, then link your Telegram account.'
      );
    }
    telegramChatId = identity.telegramChatId;
    provider = 'telegram';
  } else {
    provider = 'twilio';
    getTwilioCredentials();
  }

  await OtpChallenge.updateMany(
    { phone, channel, status: 'PENDING' },
    { $set: { status: 'REPLACED' } }
  );

  const code = channel === 'TELEGRAM'
    ? String(crypto.randomInt(0, 1000000)).padStart(6, '0')
    : null;

  const challengeId = crypto.randomBytes(32).toString('base64url');
  const expiresAt = new Date(now.getTime() + OTP_TTL_SECONDS * 1000);

  await OtpChallenge.create({
    tokenHash: sha256(challengeId),
    phone,
    channel,
    provider,
    codeHash: code ? otpDigest(challengeId, phone, code) : undefined,
    createdAt: now,
    expiresAt,
    status: 'PENDING',
  });

  try {
    if (channel === 'TELEGRAM') {
      await sendTelegramMessage(
        telegramChatId,
        'OOZOX verification code: ' + code + '\nThis code expires in ' + Math.ceil(OTP_TTL_SECONDS / 60) + ' minutes. Do not share it.'
      );
    } else {
      await sendTwilioOtp(phone, channel);
    }
  } catch (error) {
    await OtpChallenge.deleteOne({ tokenHash: sha256(challengeId), status: 'PENDING' });
    throw error;
  }

  return {
    challengeId,
    channel,
    expiresInSeconds: OTP_TTL_SECONDS,
    resendAfterSeconds: OTP_RESEND_COOLDOWN_SECONDS,
  };
};

const createPhoneProofToken = (phone) =>
  jwt.sign(
    { phone, scope: 'otp:phone-verified' },
    getJwtSecret(),
    {
      algorithm: 'HS256',
      expiresIn: '10m',
      issuer: 'oozox-api',
      audience: 'oozox-telegram-link',
    }
  );

const verifyPhoneProofToken = (token) => {
  try {
    const payload = jwt.verify(token, getJwtSecret(), {
      algorithms: ['HS256'],
      issuer: 'oozox-api',
      audience: 'oozox-telegram-link',
    });
    if (payload.scope !== 'otp:phone-verified' || !payload.phone) {
      throw new Error('invalid proof scope');
    }
    return normalizePhone(payload.phone);
  } catch (_) {
    throw new OtpError(401, 'PHONE_PROOF_INVALID', 'A recent phone verification is required.');
  }
};

const verifyOtp = async ({ challengeId, code: rawCode }) => {
  if (typeof challengeId !== 'string' || challengeId.length < 32 || challengeId.length > 64) {
    throw new OtpError(400, 'OTP_INVALID_OR_EXPIRED', 'The code is invalid or has expired.');
  }
  if (typeof rawCode !== 'string' || !/^\d{6}$/.test(rawCode.trim())) {
    throw new OtpError(400, 'OTP_CODE_INVALID', 'Enter the 6-digit verification code.');
  }

  const now = new Date();
  const tokenHash = sha256(challengeId);
  const challenge = await OtpChallenge.findOneAndUpdate(
    {
      tokenHash,
      status: 'PENDING',
      attempts: { $lt: OTP_MAX_ATTEMPTS },
      expiresAt: { $gt: now },
    },
    { $inc: { attempts: 1 } },
    { new: true }
  ).select('+codeHash');

  if (!challenge) {
    throw new OtpError(400, 'OTP_INVALID_OR_EXPIRED', 'The code is invalid, expired, or has reached its attempt limit.');
  }

  let approved = false;
  if (challenge.provider === 'telegram') {
    const submittedHash = otpDigest(challengeId, challenge.phone, rawCode.trim());
    approved = safeEqual(submittedHash, challenge.codeHash || '');
  } else {
    approved = await checkTwilioOtp(challenge.phone, rawCode.trim());
  }

  if (!approved) {
    if (challenge.attempts >= OTP_MAX_ATTEMPTS) {
      await OtpChallenge.updateOne(
        { _id: challenge._id, status: 'PENDING' },
        { $set: { status: 'LOCKED' } }
      );
    }
    throw new OtpError(400, 'OTP_CODE_INCORRECT', 'The verification code is incorrect or expired.');
  }

  const completed = await OtpChallenge.findOneAndUpdate(
    { _id: challenge._id, status: 'PENDING' },
    { $set: { status: 'VERIFIED', verifiedAt: new Date() } },
    { new: true }
  );

  if (!completed) {
    throw new OtpError(400, 'OTP_INVALID_OR_EXPIRED', 'This verification request has already been used.');
  }

  await PhoneIdentity.findOneAndUpdate(
    { phone: challenge.phone },
    { $set: { verifiedAt: new Date() }, $setOnInsert: { phone: challenge.phone } },
    { upsert: true, new: true, setDefaultsOnInsert: true }
  );

  return {
    verified: true,
    phone: challenge.phone,
    phoneProofToken: createPhoneProofToken(challenge.phone),
  };
};

const createTelegramLink = async (phoneProofToken) => {
  const phone = verifyPhoneProofToken(phoneProofToken);
  const botUsername = (process.env.TELEGRAM_BOT_USERNAME || '').trim().replace(/^@/, '');

  if (!botUsername || !/^[A-Za-z0-9_]{5,32}$/.test(botUsername)) {
    throw new OtpError(503, 'TELEGRAM_NOT_CONFIGURED', 'Telegram account linking is not configured yet.');
  }

  const token = crypto.randomBytes(24).toString('base64url');
  const expiresAt = new Date(Date.now() + TELEGRAM_LINK_TTL_SECONDS * 1000);

  await TelegramLink.updateMany(
    { phone, usedAt: null },
    { $set: { usedAt: new Date() } }
  );

  await TelegramLink.create({
    tokenHash: sha256(token),
    phone,
    expiresAt,
  });

  return {
    telegramLinkUrl: 'https://t.me/' + botUsername + '?start=' + token,
    expiresInSeconds: TELEGRAM_LINK_TTL_SECONDS,
  };
};

const consumeTelegramStart = async ({ token, chatId, telegramUserId }) => {
  if (typeof token !== 'string' || token.length < 20 || token.length > 64) return false;
  const link = await TelegramLink.findOneAndUpdate(
    {
      tokenHash: sha256(token),
      usedAt: null,
      expiresAt: { $gt: new Date() },
    },
    { $set: { usedAt: new Date() } },
    { new: true }
  );

  if (!link) return false;

  await PhoneIdentity.findOneAndUpdate(
    { phone: link.phone },
    {
      $set: {
        telegramChatId: String(chatId),
        telegramUserId: String(telegramUserId || ''),
        telegramLinkedAt: new Date(),
      },
      $setOnInsert: { phone: link.phone },
    },
    { upsert: true, new: true, setDefaultsOnInsert: true }
  );

  return true;
};

module.exports = {
  OtpError,
  requestOtp,
  verifyOtp,
  createTelegramLink,
  verifyPhoneProofToken,
  consumeTelegramStart,
  sendTelegramMessage,
};
