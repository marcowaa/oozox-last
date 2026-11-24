const { createTelegramGatewayAdapter } = require('./telegramGatewayAdapter');
const { createTwilioVerifyAdapter } = require('./twilioVerifyAdapter');

const DEFAULT_FALLBACK_ORDER = ['TELEGRAM', 'WHATSAPP', 'SMS'];

let customAdapters = null;

function getOtpAdapters() {
  if (customAdapters) {
    return customAdapters;
  }
  return {
    TELEGRAM: createTelegramGatewayAdapter(),
    WHATSAPP: createTwilioVerifyAdapter('WHATSAPP'),
    SMS: createTwilioVerifyAdapter('SMS'),
  };
}

function setOtpAdaptersForTest(adaptersMap) {
  customAdapters = adaptersMap;
}

function resetOtpAdapters() {
  customAdapters = null;
}

function resolveChannelOrder(preferredChannel = 'AUTO', allowFallback = true) {
  const normalized = String(preferredChannel || 'AUTO').toUpperCase();
  if (normalized === 'AUTO' || !DEFAULT_FALLBACK_ORDER.includes(normalized)) {
    return [...DEFAULT_FALLBACK_ORDER];
  }
  if (!allowFallback) {
    return [normalized];
  }
  return [normalized, ...DEFAULT_FALLBACK_ORDER.filter((c) => c !== normalized)];
}

module.exports = {
  DEFAULT_FALLBACK_ORDER,
  getOtpAdapters,
  setOtpAdaptersForTest,
  resetOtpAdapters,
  resolveChannelOrder,
};
