const crypto = require('crypto');
const { hashOtpCode, timingSafeHexEqual } = require('../../src/services/otp/telegramGatewayAdapter');
const { setOtpAdaptersForTest, resetOtpAdapters } = require('../../src/services/otp/otpProviderRegistry');

function installMockOtpAdapters(options = {}) {
  const state = {
    sent: [],
    revoked: [],
    verifiedChecks: [],
    failChannels: new Set(options.failChannels || []),
    twilioCodesByRef: new Map(),
    lastCodeByPhone: new Map(),
  };

  setOtpAdaptersForTest({
    TELEGRAM: {
      channel: 'TELEGRAM',
      providerType: 'TELEGRAM_GATEWAY',
      isConfigured: () => true,
      async sendChallenge({ phoneE164, requestId }) {
        if (state.failChannels.has('TELEGRAM')) {
          return { ok: false, retryable: true, error: 'SIMULATED_TELEGRAM_FAIL' };
        }
        const code = options.fixedCode || '654321';
        const codeSalt = crypto.randomBytes(16).toString('hex');
        const codeHash = hashOtpCode(code, codeSalt);
        const providerRef = `tg_${requestId}`;
        state.lastCodeByPhone.set(phoneE164, code);
        state.sent.push({ channel: 'TELEGRAM', phoneE164, providerRef, code });
        return {
          ok: true,
          providerType: 'TELEGRAM_GATEWAY',
          providerRef,
          codeHash,
          codeSalt,
          estimatedCostUsd: 0.01,
        };
      },
      async verifyChallenge({ challenge, code }) {
        if (!challenge?.codeHash || !challenge?.codeSalt) return { approved: false };
        const computed = hashOtpCode(String(code).trim(), challenge.codeSalt);
        return { approved: timingSafeHexEqual(computed, challenge.codeHash) };
      },
      async revokeChallenge({ challenge }) {
        state.revoked.push(challenge.providerRef);
        return { ok: true };
      },
    },
    WHATSAPP: createMockTwilio('WHATSAPP', state, options),
    SMS: createMockTwilio('SMS', state, options),
  });

  return state;
}

function createMockTwilio(channel, state, options) {
  return {
    channel,
    providerType: 'TWILIO_VERIFY',
    isConfigured: () => true,
    async sendChallenge({ phoneE164 }) {
      if (state.failChannels.has(channel)) {
        return { ok: false, retryable: true, error: `SIMULATED_${channel}_FAIL` };
      }
      const code = options.fixedCode || '778899';
      const providerRef = `VE_${channel}_${crypto.randomUUID().slice(0, 8)}`;
      state.twilioCodesByRef.set(providerRef, code);
      state.lastCodeByPhone.set(phoneE164, code);
      state.sent.push({ channel, phoneE164, providerRef, code });
      return {
        ok: true,
        providerType: 'TWILIO_VERIFY',
        providerRef,
        codeHash: null,
        codeSalt: null,
        estimatedCostUsd: channel === 'WHATSAPP' ? 0.055 : 0.07,
      };
    },
    async verifyChallenge({ challenge, code }) {
      const expected = state.twilioCodesByRef.get(challenge.providerRef);
      state.verifiedChecks.push({ providerRef: challenge.providerRef, code });
      return { approved: Boolean(expected && expected === String(code).trim()) };
    },
  };
}

module.exports = {
  installMockOtpAdapters,
  resetOtpAdapters,
};
