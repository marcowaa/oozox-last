const crypto = require('crypto');

const DEFAULT_TELEGRAM_GATEWAY_URL = 'https://gatewayapi.telegram.org';

function hashOtpCode(code, salt) {
  return crypto.createHmac('sha256', salt).update(String(code), 'utf8').digest('hex');
}

function timingSafeHexEqual(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length || a.length === 0) {
    return false;
  }
  try {
    return crypto.timingSafeEqual(Buffer.from(a, 'hex'), Buffer.from(b, 'hex'));
  } catch {
    return false;
  }
}

function verifyWebhookSignature({
  token = (process.env.TELEGRAM_GATEWAY_TOKEN || '').trim(),
  timestampHeader,
  rawBody,
  signatureHeader,
  maxAgeSeconds = 300,
}) {
  if (!token || !timestampHeader || !signatureHeader || !rawBody) {
    return false;
  }
  const ts = Number(timestampHeader);
  if (!Number.isFinite(ts) || Math.abs(Math.floor(Date.now() / 1000) - ts) > maxAgeSeconds) {
    return false;
  }

  const secretKey = crypto.createHash('sha256').update(token, 'utf8').digest();
  const bodyBuf = Buffer.isBuffer(rawBody) ? rawBody : Buffer.from(String(rawBody), 'utf8');
  const dataCheck = Buffer.concat([Buffer.from(`${timestampHeader}\n`, 'utf8'), bodyBuf]);
  const expectedHex = crypto.createHmac('sha256', secretKey).update(dataCheck).digest('hex');
  return timingSafeHexEqual(expectedHex, String(signatureHeader).trim().toLowerCase());
}

function createTelegramGatewayAdapter(options = {}) {
  const fetchImpl = options.fetchImpl || global.fetch;

  return {
    channel: 'TELEGRAM',
    providerType: 'TELEGRAM_GATEWAY',

    isConfigured() {
      const token = (options.token ?? process.env.TELEGRAM_GATEWAY_TOKEN ?? '').trim();
      return Boolean(token);
    },

    async sendChallenge({ phoneE164, ttlSeconds = 300, requestId }) {
      const token = (options.token ?? process.env.TELEGRAM_GATEWAY_TOKEN ?? '').trim();
      const baseUrl = (
        options.baseUrl ??
        process.env.TELEGRAM_GATEWAY_BASE_URL ??
        DEFAULT_TELEGRAM_GATEWAY_URL
      )
        .trim()
        .replace(/\/$/, '');
      const callbackUrl = (
        options.callbackUrl ??
        process.env.TELEGRAM_GATEWAY_CALLBACK_URL ??
        ''
      ).trim();

      if (!token) {
        return {
          ok: false,
          retryable: true,
          error: 'TELEGRAM_GATEWAY_NOT_CONFIGURED',
        };
      }

      const code = String(crypto.randomInt(100000, 1000000));
      const codeSalt = crypto.randomBytes(16).toString('hex');
      const codeHash = hashOtpCode(code, codeSalt);
      const boundedTtl = Math.min(3600, Math.max(30, Number(ttlSeconds) || 300));

      const body = {
        phone_number: phoneE164,
        request_id: requestId,
        code,
        ttl: boundedTtl,
      };
      if (callbackUrl) {
        body.callback_url = callbackUrl;
      }

      try {
        const res = await fetchImpl(`${baseUrl}/sendVerificationMessage`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(body),
        });

        const payload = await res.json().catch(() => ({}));
        if (!res.ok || payload.ok === false) {
          return {
            ok: false,
            retryable: true,
            error: payload.error || `TELEGRAM_HTTP_${res.status}`,
          };
        }

        const providerRef = payload?.result?.request_id || requestId;
        const unitCost = Number(process.env.TELEGRAM_GATEWAY_COST_USD || 0.01);

        return {
          ok: true,
          providerType: 'TELEGRAM_GATEWAY',
          providerRef,
          codeHash,
          codeSalt,
          estimatedCostUsd: Number.isFinite(unitCost) ? unitCost : 0.01,
        };
      } catch (err) {
        return {
          ok: false,
          retryable: true,
          error: err.message || 'TELEGRAM_NETWORK_ERROR',
        };
      }
    },

    async verifyChallenge({ challenge, code }) {
      if (!challenge?.codeHash || !challenge?.codeSalt) {
        return { approved: false, reason: 'MISSING_LOCAL_HASH' };
      }
      const computed = hashOtpCode(String(code).trim(), challenge.codeSalt);
      const approved = timingSafeHexEqual(computed, challenge.codeHash);
      return { approved };
    },

    async revokeChallenge({ challenge }) {
      const token = (options.token ?? process.env.TELEGRAM_GATEWAY_TOKEN ?? '').trim();
      const baseUrl = (
        options.baseUrl ??
        process.env.TELEGRAM_GATEWAY_BASE_URL ??
        DEFAULT_TELEGRAM_GATEWAY_URL
      )
        .trim()
        .replace(/\/$/, '');

      if (!token || !challenge?.providerRef) {
        return { ok: false };
      }

      try {
        const res = await fetchImpl(`${baseUrl}/revokeVerificationMessage`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ request_id: challenge.providerRef }),
        });
        return { ok: res.ok };
      } catch {
        return { ok: false };
      }
    },
  };
}

module.exports = {
  createTelegramGatewayAdapter,
  verifyWebhookSignature,
  hashOtpCode,
  timingSafeHexEqual,
};
