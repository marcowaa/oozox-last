const DEFAULT_ALLOWED_COUNTRY_CODES = [
  '+20',
  '+966',
  '+971',
  '+965',
  '+974',
  '+973',
  '+968',
  '+962',
  '+961',
  '+964',
  '+212',
  '+216',
  '+213',
  '+1',
  '+44',
  '+49',
  '+90',
];

function getAllowedCountryPrefixes() {
  const raw = (process.env.OTP_ALLOWED_COUNTRIES || '').trim();
  if (!raw) return DEFAULT_ALLOWED_COUNTRY_CODES;
  return raw
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => (item.startsWith('+') ? item : `+${item}`));
}

/**
 * Normalize raw phone input into canonical E.164 format (+<country><number>).
 * Handles common Arabic local formats (e.g. Egyptian 010... -> +2010..., Saudi 05... -> +9665...).
 */
function normalizePhoneE164(rawInput, defaultCountryCode = '+20') {
  if (typeof rawInput !== 'string') return null;
  let cleaned = rawInput
    .replace(/[\u0660-\u0669]/g, (d) => String(d.charCodeAt(0) - 0x0660))
    .replace(/[\u06F0-\u06F9]/g, (d) => String(d.charCodeAt(0) - 0x06f0))
    .replace(/[\s\-().]/g, '')
    .trim();

  if (!cleaned) return null;

  if (cleaned.startsWith('00')) {
    cleaned = `+${cleaned.slice(2)}`;
  }

  if (!cleaned.startsWith('+')) {
    if (cleaned.startsWith('01') && cleaned.length === 11) {
      cleaned = `+20${cleaned.slice(1)}`;
    } else if (cleaned.startsWith('05') && cleaned.length === 10) {
      cleaned = `+966${cleaned.slice(1)}`;
    } else if (cleaned.startsWith('0') && defaultCountryCode) {
      const prefix = defaultCountryCode.startsWith('+')
        ? defaultCountryCode
        : `+${defaultCountryCode}`;
      cleaned = `${prefix}${cleaned.replace(/^0+/, '')}`;
    } else if (/^\d{8,15}$/.test(cleaned)) {
      cleaned = `+${cleaned}`;
    }
  }

  if (!/^\+[1-9]\d{7,14}$/.test(cleaned)) {
    return null;
  }

  return cleaned;
}

function isAllowedOtpCountry(phoneE164) {
  if (!phoneE164 || !phoneE164.startsWith('+')) return false;
  const allowed = getAllowedCountryPrefixes();
  if (allowed.includes('*')) return true;
  return allowed.some((prefix) => phoneE164.startsWith(prefix));
}

function maskPhone(phoneE164) {
  if (!phoneE164 || phoneE164.length < 7) return '***';
  return `${phoneE164.slice(0, 5)}****${phoneE164.slice(-3)}`;
}

function looksLikePhone(input) {
  if (typeof input !== 'string') return false;
  const trimmed = input.trim();
  if (trimmed.includes('@')) return false;
  return Boolean(normalizePhoneE164(trimmed));
}

module.exports = {
  DEFAULT_ALLOWED_COUNTRY_CODES,
  getAllowedCountryPrefixes,
  normalizePhoneE164,
  isAllowedOtpCountry,
  maskPhone,
  looksLikePhone,
};
