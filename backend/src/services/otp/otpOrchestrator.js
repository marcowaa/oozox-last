const crypto = require('crypto');
const OtpChallenge = require('../../models/OtpChallenge');
const OtpAbuseLedger = require('../../models/OtpAbuseLedger');
const Notification = require('../../models/Notification');
const User = require('../../models/User');
const { isAllowedOtpCountry, maskPhone, normalizePhoneE164 } = require('../../utils/phoneUtils');
const { getOtpAdapters, resolveChannelOrder } = require('./otpProviderRegistry');

function getOtpPolicyConfig() {
  return {
    ttlSeconds: Math.max(60, Number(process.env.OTP_TTL_SECONDS) || 300),
    resendCooldownSeconds: Math.max(15, Number(process.env.OTP_RESEND_COOLDOWN_SECONDS) || 60),
    dailyMaxPerPhone: Math.max(1, Number(process.env.OTP_DAILY_MAX_PER_PHONE) || 5),
    maxAttempts: Math.max(3, Number(process.env.OTP_MAX_ATTEMPTS) || 5),
    lockMinutes: Math.max(1, Number(process.env.OTP_LOCK_MINUTES) || 15),
    monthlyBudgetUsd: Math.max(1, Number(process.env.OTP_MONTHLY_BUDGET_USD) || 100),
  };
}

function getDateKey(date = new Date()) {
  return date.toISOString().slice(0, 10);
}

function getMonthKey(date = new Date()) {
  return date.toISOString().slice(0, 7);
}

async function maybeNotifyAdminsOnOtpSpend(monthLedger, monthlyBudgetUsd) {
  if (!monthLedger || monthlyBudgetUsd <= 0) return;
  const pct = (monthLedger.estimatedCostUsd / monthlyBudgetUsd) * 100;
  const thresholds = [50, 80, 100];
  const newlyCrossed = thresholds.filter(
    (threshold) => pct >= threshold && !monthLedger.alertsTriggered.includes(threshold)
  );
  if (newlyCrossed.length === 0) return;

  monthLedger.alertsTriggered.push(...newlyCrossed);
  await monthLedger.save();

  const admins = await User.find({ role: 'ADMIN' }).select('_id preferredLanguage').limit(20);
  if (admins.length === 0) return;

  const highest = Math.max(...newlyCrossed);
  const docs = admins.map((admin) => ({
    recipient: admin._id,
    title: `تنبيه إنفاق OTP (${highest}%)`,
    message: `بلغ استهلاك رسائل التحقق (Twilio/Telegram) ${pct.toFixed(1)}% من الميزانية الشهرية ($${monthLedger.estimatedCostUsd.toFixed(2)} / $${monthlyBudgetUsd.toFixed(2)}).`,
    type: 'OTP_BUDGET_ALERT',
    language: admin.preferredLanguage || 'ar',
  }));
  await Notification.insertMany(docs);
}

async function supersedePendingChallenges(phoneE164, purpose, adapters) {
  const pending = await OtpChallenge.find({
    phoneE164,
    purpose,
    status: 'PENDING',
  });

  for (const old of pending) {
    old.status = 'SUPERSEDED';
    await old.save();
    if (old.providerType === 'TELEGRAM_GATEWAY' && adapters?.TELEGRAM?.revokeChallenge) {
      await adapters.TELEGRAM.revokeChallenge({ challenge: old }).catch(() => {});
    }
  }
}

async function findOrCreateLedger(key, kind) {
  try {
    return await OtpAbuseLedger.findOneAndUpdate(
      { key },
      { $setOnInsert: { key, kind } },
      { upsert: true, new: true }
    );
  } catch (err) {
    if (err && err.code === 11000) {
      return await OtpAbuseLedger.findOne({ key });
    }
    throw err;
  }
}

async function requestOtpChallenge({
  rawPhone,
  purpose,
  preferredChannel = 'AUTO',
  allowFallback = true,
  userId = null,
  lang = 'ar',
  metadata = {},
}) {
  const phoneE164 = normalizePhoneE164(rawPhone);
  if (!phoneE164) {
    const err = new Error('Invalid E.164 phone number');
    err.statusCode = 400;
    err.code = 'INVALID_PHONE';
    throw err;
  }

  if (!isAllowedOtpCountry(phoneE164)) {
    const err = new Error('OTP delivery is not enabled for this country prefix');
    err.statusCode = 403;
    err.code = 'COUNTRY_NOT_ALLOWED';
    throw err;
  }

  const policy = getOtpPolicyConfig();
  const now = new Date();
  const dayKey = `PHONE_DAY:${phoneE164}:${getDateKey(now)}`;
  const monthKey = `OTP_MONTH:${getMonthKey(now)}`;

  const phoneLedger = await findOrCreateLedger(dayKey, 'PHONE_DAY');

  if (phoneLedger.lockedUntil && phoneLedger.lockedUntil > now) {
    const waitSec = Math.ceil((phoneLedger.lockedUntil.getTime() - now.getTime()) / 1000);
    const err = new Error(`Too many failed attempts. Retry in ${waitSec}s`);
    err.statusCode = 429;
    err.code = 'OTP_LOCKED';
    err.retryAfterSeconds = waitSec;
    throw err;
  }

  if (phoneLedger.lastSentAt) {
    const elapsedSec = (now.getTime() - phoneLedger.lastSentAt.getTime()) / 1000;
    if (elapsedSec < policy.resendCooldownSeconds) {
      const waitSec = Math.ceil(policy.resendCooldownSeconds - elapsedSec);
      const err = new Error(`Please wait ${waitSec}s before requesting another code`);
      err.statusCode = 429;
      err.code = 'OTP_COOLDOWN';
      err.retryAfterSeconds = waitSec;
      throw err;
    }
  }

  if (phoneLedger.sendCount >= policy.dailyMaxPerPhone) {
    const err = new Error('Daily OTP limit reached for this phone number');
    err.statusCode = 429;
    err.code = 'OTP_DAILY_LIMIT';
    throw err;
  }

  const monthLedger = await findOrCreateLedger(monthKey, 'OTP_MONTH');
  if (monthLedger.estimatedCostUsd >= policy.monthlyBudgetUsd) {
    const err = new Error('Monthly OTP delivery budget reached');
    err.statusCode = 503;
    err.code = 'OTP_BUDGET_EXCEEDED';
    throw err;
  }

  const adapters = getOtpAdapters();
  const channelOrder = resolveChannelOrder(preferredChannel, allowFallback);

  // Supersede previous active challenges BEFORE issuing a new one
  await supersedePendingChallenges(phoneE164, purpose, adapters);

  const challengeId = crypto.randomUUID();
  const attemptedChannels = [];
  let chosenResult = null;
  let activeChannel = null;

  for (const ch of channelOrder) {
    const adapter = adapters[ch];
    if (!adapter) {
      attemptedChannels.push({ channel: ch, status: 'SKIPPED', error: 'ADAPTER_MISSING', at: new Date() });
      continue;
    }
    const sendRes = await adapter.sendChallenge({
      phoneE164,
      ttlSeconds: policy.ttlSeconds,
      lang,
      requestId: `${challengeId}:${ch}`,
    });

    if (sendRes && sendRes.ok) {
      attemptedChannels.push({ channel: ch, status: 'SENT', error: '', at: new Date() });
      chosenResult = sendRes;
      activeChannel = ch;
      break;
    } else {
      attemptedChannels.push({
        channel: ch,
        status: 'FAILED',
        error: sendRes?.error || 'SEND_FAILED',
        at: new Date(),
      });
    }
  }

  if (!chosenResult || !activeChannel) {
    const err = new Error('Unable to deliver verification code through any configured channel');
    err.statusCode = 503;
    err.code = 'OTP_DELIVERY_UNAVAILABLE';
    err.attemptedChannels = attemptedChannels;
    throw err;
  }

  const expiresAt = new Date(now.getTime() + policy.ttlSeconds * 1000);
  const challenge = await OtpChallenge.create({
    challengeId,
    userId,
    phoneE164,
    purpose,
    requestedChannel: String(preferredChannel || 'AUTO').toUpperCase(),
    activeChannel,
    attemptedChannels,
    providerType: chosenResult.providerType,
    providerRef: chosenResult.providerRef || '',
    codeHash: chosenResult.providerType === 'TELEGRAM_GATEWAY' ? chosenResult.codeHash : null,
    codeSalt: chosenResult.providerType === 'TELEGRAM_GATEWAY' ? chosenResult.codeSalt : null,
    status: 'PENDING',
    attempts: 0,
    maxAttempts: policy.maxAttempts,
    expiresAt,
    metadata: {
      deviceId: metadata.deviceId || '',
      deviceName: metadata.deviceName || '',
      platform: metadata.platform || 'ANDROID',
      ipAddress: metadata.ipAddress || '',
      estimatedCostUsd: chosenResult.estimatedCostUsd || 0,
    },
  });

  phoneLedger.sendCount += 1;
  phoneLedger.lastSentAt = now;
  await phoneLedger.save();

  monthLedger.sendCount += 1;
  if (chosenResult.providerType === 'TWILIO_VERIFY') {
    monthLedger.twilioSendCount += 1;
  } else {
    monthLedger.telegramSendCount += 1;
  }
  monthLedger.estimatedCostUsd += chosenResult.estimatedCostUsd || 0;
  await monthLedger.save();
  await maybeNotifyAdminsOnOtpSpend(monthLedger, policy.monthlyBudgetUsd).catch(() => {});

  return {
    challengeId: challenge.challengeId,
    activeChannel: challenge.activeChannel,
    attemptedChannels: challenge.attemptedChannels,
    expiresAt: challenge.expiresAt,
    cooldownSeconds: policy.resendCooldownSeconds,
    maxAttempts: challenge.maxAttempts,
    maskedPhone: maskPhone(phoneE164),
    phoneE164,
  };
}

async function verifyOtpChallenge({ challengeId, code, phoneE164 }) {
  const cleanCode = String(code || '').replace(/\D/g, '').trim();
  if (cleanCode.length < 4 || cleanCode.length > 8) {
    const err = new Error('Invalid OTP code format');
    err.statusCode = 400;
    err.code = 'INVALID_OTP_FORMAT';
    throw err;
  }

  const query = challengeId
    ? { challengeId }
    : { phoneE164, status: 'PENDING' };

  const challenge = await OtpChallenge.findOne(query)
    .select('+codeHash +codeSalt')
    .sort({ createdAt: -1 });

  if (!challenge) {
    const err = new Error('Verification challenge not found');
    err.statusCode = 404;
    err.code = 'OTP_CHALLENGE_NOT_FOUND';
    throw err;
  }

  if (phoneE164 && challenge.phoneE164 !== phoneE164) {
    const err = new Error('Phone number does not match verification challenge');
    err.statusCode = 400;
    err.code = 'OTP_PHONE_MISMATCH';
    throw err;
  }

  if (challenge.status === 'SUPERSEDED') {
    const err = new Error('This verification code was superseded by a newer request');
    err.statusCode = 400;
    err.code = 'OTP_SUPERSEDED';
    throw err;
  }

  if (challenge.status === 'VERIFIED') {
    const err = new Error('Verification challenge already used');
    err.statusCode = 400;
    err.code = 'OTP_ALREADY_USED';
    throw err;
  }

  const now = new Date();
  if (challenge.status === 'LOCKED' || (challenge.lockedUntil && challenge.lockedUntil > now)) {
    const waitSec = Math.max(
      1,
      Math.ceil(((challenge.lockedUntil || now).getTime() - now.getTime()) / 1000)
    );
    const err = new Error(`Too many wrong attempts. Try again in ${waitSec}s`);
    err.statusCode = 429;
    err.code = 'OTP_LOCKED';
    err.retryAfterSeconds = waitSec;
    throw err;
  }

  if (challenge.expiresAt <= now || challenge.status === 'EXPIRED') {
    challenge.status = 'EXPIRED';
    await challenge.save();
    const err = new Error('Verification code expired');
    err.statusCode = 400;
    err.code = 'OTP_EXPIRED';
    throw err;
  }

  const adapters = getOtpAdapters();
  const adapter = adapters[challenge.activeChannel];
  if (!adapter) {
    const err = new Error('Verification adapter unavailable');
    err.statusCode = 503;
    err.code = 'OTP_ADAPTER_UNAVAILABLE';
    throw err;
  }

  const checkResult = await adapter.verifyChallenge({ challenge, code: cleanCode });
  if (!checkResult?.approved) {
    challenge.attempts += 1;
    const policy = getOtpPolicyConfig();
    if (challenge.attempts >= challenge.maxAttempts) {
      const lockUntil = new Date(now.getTime() + policy.lockMinutes * 60 * 1000);
      challenge.status = 'LOCKED';
      challenge.lockedUntil = lockUntil;
      await challenge.save();

      await OtpAbuseLedger.findOneAndUpdate(
        { key: `PHONE_DAY:${challenge.phoneE164}:${getDateKey(now)}` },
        { $set: { lockedUntil } }
      );

      const err = new Error('Maximum OTP attempts exceeded; temporarily locked');
      err.statusCode = 429;
      err.code = 'OTP_LOCKED';
      err.remainingAttempts = 0;
      err.retryAfterSeconds = policy.lockMinutes * 60;
      throw err;
    }

    await challenge.save();
    const remaining = Math.max(0, challenge.maxAttempts - challenge.attempts);
    const err = new Error(`Incorrect verification code (${remaining} attempts remaining)`);
    err.statusCode = 401;
    err.code = 'OTP_INVALID_CODE';
    err.remainingAttempts = remaining;
    throw err;
  }

  challenge.status = 'VERIFIED';
  challenge.verifiedAt = now;
  await challenge.save();

  // Record successful Twilio Verify approval cost accurately if needed
  const monthKey = `OTP_MONTH:${getMonthKey(now)}`;
  await OtpAbuseLedger.findOneAndUpdate(
    { key: monthKey },
    {
      $inc: {
        verifiedCount: 1,
        estimatedCostUsd: challenge.providerType === 'TWILIO_VERIFY' ? 0.05 : 0,
      },
    }
  );

  return challenge;
}

module.exports = {
  getOtpPolicyConfig,
  requestOtpChallenge,
  verifyOtpChallenge,
};
