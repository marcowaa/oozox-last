function createTwilioVerifyAdapter(channel, options = {}) {
  const normalizedChannel = channel === 'WHATSAPP' ? 'WHATSAPP' : 'SMS';
  const twilioChannelParam = normalizedChannel === 'WHATSAPP' ? 'whatsapp' : 'sms';
  const fetchImpl = options.fetchImpl || global.fetch;

  function getCredentials() {
    const accountSid = (options.accountSid ?? process.env.TWILIO_ACCOUNT_SID ?? '').trim();
    const authToken = (options.authToken ?? process.env.TWILIO_AUTH_TOKEN ?? '').trim();
    const serviceSid = (options.serviceSid ?? process.env.TWILIO_VERIFY_SERVICE_SID ?? '').trim();
    return { accountSid, authToken, serviceSid };
  }

  function buildAuthHeader(accountSid, authToken) {
    return `Basic ${Buffer.from(`${accountSid}:${authToken}`, 'utf8').toString('base64')}`;
  }

  return {
    channel: normalizedChannel,
    providerType: 'TWILIO_VERIFY',

    isConfigured() {
      const { accountSid, authToken, serviceSid } = getCredentials();
      return Boolean(accountSid && authToken && serviceSid);
    },

    async sendChallenge({ phoneE164, lang = 'ar' }) {
      const { accountSid, authToken, serviceSid } = getCredentials();
      if (!accountSid || !authToken || !serviceSid) {
        return {
          ok: false,
          retryable: true,
          error: 'TWILIO_VERIFY_NOT_CONFIGURED',
        };
      }

      const form = new URLSearchParams();
      form.append('To', phoneE164);
      form.append('Channel', twilioChannelParam);
      if (lang) {
        form.append('Locale', lang === 'ar' ? 'ar' : 'en');
      }

      try {
        const res = await fetchImpl(
          `https://verify.twilio.com/v2/Services/${encodeURIComponent(serviceSid)}/Verifications`,
          {
            method: 'POST',
            headers: {
              Authorization: buildAuthHeader(accountSid, authToken),
              'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: form.toString(),
          }
        );

        const payload = await res.json().catch(() => ({}));
        if (!res.ok) {
          return {
            ok: false,
            retryable: true,
            error: payload.message || `TWILIO_HTTP_${res.status}`,
          };
        }

        const defaultCost = normalizedChannel === 'WHATSAPP' ? 0.055 : 0.07;
        const envCostKey =
          normalizedChannel === 'WHATSAPP'
            ? process.env.TWILIO_WHATSAPP_COST_USD
            : process.env.TWILIO_SMS_COST_USD;
        const unitCost = Number(envCostKey || defaultCost);

        // Note: Twilio Verify generates and verifies the OTP itself; no local codeHash is stored.
        return {
          ok: true,
          providerType: 'TWILIO_VERIFY',
          providerRef: payload.sid || '',
          codeHash: null,
          codeSalt: null,
          estimatedCostUsd: Number.isFinite(unitCost) ? unitCost : defaultCost,
        };
      } catch (err) {
        return {
          ok: false,
          retryable: true,
          error: err.message || 'TWILIO_NETWORK_ERROR',
        };
      }
    },

    async verifyChallenge({ challenge, code }) {
      const { accountSid, authToken, serviceSid } = getCredentials();
      if (!accountSid || !authToken || !serviceSid) {
        return { approved: false, error: 'TWILIO_VERIFY_NOT_CONFIGURED' };
      }

      const form = new URLSearchParams();
      form.append('To', challenge.phoneE164);
      form.append('Code', String(code).trim());
      if (challenge.providerRef) {
        form.append('VerificationSid', challenge.providerRef);
      }

      try {
        const res = await fetchImpl(
          `https://verify.twilio.com/v2/Services/${encodeURIComponent(serviceSid)}/VerificationChecks`,
          {
            method: 'POST',
            headers: {
              Authorization: buildAuthHeader(accountSid, authToken),
              'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: form.toString(),
          }
        );

        const payload = await res.json().catch(() => ({}));
        if (!res.ok) {
          return {
            approved: false,
            error: payload.message || `TWILIO_CHECK_HTTP_${res.status}`,
          };
        }

        return {
          approved: payload.status === 'approved',
          status: payload.status,
        };
      } catch (err) {
        return {
          approved: false,
          error: err.message || 'TWILIO_CHECK_NETWORK_ERROR',
        };
      }
    },
  };
}

module.exports = {
  createTwilioVerifyAdapter,
};
