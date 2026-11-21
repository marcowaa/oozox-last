const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const {
  SUPPORTED_LANGUAGES,
  SUPPORTED_LANGUAGE_CODES,
  LANGUAGE_MAP,
  DEFAULT_LANGUAGE,
  FALLBACK_LANGUAGE,
} = require('./languages');

const BUNDLES_DIR = path.join(__dirname, 'bundles');
const bundles = {};
const etags = {};

function loadBundles() {
  for (const code of SUPPORTED_LANGUAGE_CODES) {
    const filePath = path.join(BUNDLES_DIR, `${code}.json`);
    if (fs.existsSync(filePath)) {
      const raw = fs.readFileSync(filePath, 'utf8');
      bundles[code] = JSON.parse(raw);
      etags[code] = `"oozox-i18n-${code}-${crypto.createHash('sha256').update(raw).digest('hex').slice(0, 12)}"`;
    } else {
      bundles[code] = {};
      etags[code] = `"oozox-i18n-${code}-empty"`;
    }
  }
}

loadBundles();

const MASTER_VERSION = `2025.1.${crypto
  .createHash('sha256')
  .update(SUPPORTED_LANGUAGE_CODES.map((c) => etags[c]).join('|'))
  .digest('hex')
  .slice(0, 8)}`;

/**
 * Normalize an incoming language parameter or Accept-Language header
 * into one of the 14 canonical OOZOX language codes.
 */
function normalizeLang(input, defaultLang = DEFAULT_LANGUAGE) {
  if (!input || typeof input !== 'string') return defaultLang;

  const cleaned = input.trim();
  if (!cleaned) return defaultLang;

  // Parse comma-separated Accept-Language values ordered by quality score
  const candidates = cleaned
    .split(',')
    .map((part) => {
      const [tagPart, ...params] = part.trim().split(';');
      const tag = (tagPart || '').trim().toLowerCase();
      let q = 1.0;
      for (const p of params) {
        const match = p.trim().match(/^q=([0-9.]+)$/);
        if (match) q = parseFloat(match[1]);
      }
      return { tag, q: Number.isFinite(q) ? q : 1.0 };
    })
    .filter((c) => c.tag && c.q > 0)
    .sort((a, b) => b.q - a.q);

  for (const { tag } of candidates) {
    if (LANGUAGE_MAP[tag]) return tag;
    const base = tag.split(/[-_]/)[0];
    if (LANGUAGE_MAP[base]) return base;
  }

  return defaultLang;
}

function resolveRequestedLang(req) {
  const fromQuery = req?.query?.lang;
  if (typeof fromQuery === 'string' && fromQuery.trim()) {
    return normalizeLang(fromQuery);
  }
  const fromCustomHeader = req?.headers?.['x-app-language'];
  if (typeof fromCustomHeader === 'string' && fromCustomHeader.trim()) {
    return normalizeLang(fromCustomHeader);
  }
  const fromAccept = req?.headers?.['accept-language'];
  if (typeof fromAccept === 'string' && fromAccept.trim()) {
    return normalizeLang(fromAccept);
  }
  if (req?.user?.preferredLanguage && LANGUAGE_MAP[req.user.preferredLanguage]) {
    return req.user.preferredLanguage;
  }
  return DEFAULT_LANGUAGE;
}

function interpolate(template, vars) {
  if (typeof template !== 'string') return String(template ?? '');
  if (!vars) return template;

  if (Array.isArray(vars)) {
    let idx = 0;
    return template.replace(/%[sd]/g, () => {
      const val = idx < vars.length ? vars[idx] : '';
      idx += 1;
      return val !== undefined && val !== null ? String(val) : '';
    });
  }

  if (typeof vars === 'object') {
    let out = template.replace(/\{([a-zA-Z0-9_]+)\}/g, (_, k) =>
      Object.prototype.hasOwnProperty.call(vars, k) && vars[k] !== undefined && vars[k] !== null
        ? String(vars[k])
        : `{${k}}`
    );
    if (Object.prototype.hasOwnProperty.call(vars, '_args') && Array.isArray(vars._args)) {
      out = interpolate(out, vars._args);
    }
    return out;
  }

  return template;
}

/**
 * Translate a key into the target language with safe fallback chain (lang -> en -> ar -> key).
 */
function t(lang, key, vars) {
  const normalized = normalizeLang(lang);
  const raw =
    (bundles[normalized] && bundles[normalized][key]) ||
    (bundles[FALLBACK_LANGUAGE] && bundles[FALLBACK_LANGUAGE][key]) ||
    (bundles[DEFAULT_LANGUAGE] && bundles[DEFAULT_LANGUAGE][key]) ||
    key;
  return interpolate(raw, vars);
}

function getBundle(lang) {
  const code = normalizeLang(lang);
  const meta = LANGUAGE_MAP[code] || LANGUAGE_MAP[DEFAULT_LANGUAGE];
  // Merge fallback keys only if missing (all bundles have 100% key parity)
  const merged = {
    ...(bundles[DEFAULT_LANGUAGE] || {}),
    ...(bundles[FALLBACK_LANGUAGE] || {}),
    ...(bundles[code] || {}),
  };
  return {
    ...meta,
    version: MASTER_VERSION,
    etag: etags[code],
    keyCount: Object.keys(merged).length,
    strings: merged,
  };
}

function getCatalogSummary() {
  return {
    version: MASTER_VERSION,
    defaultLanguage: DEFAULT_LANGUAGE,
    fallbackLanguage: FALLBACK_LANGUAGE,
    languages: SUPPORTED_LANGUAGES.map((lang) => ({
      ...lang,
      etag: etags[lang.code],
      keyCount: Object.keys(bundles[lang.code] || {}).length,
    })),
  };
}

module.exports = {
  SUPPORTED_LANGUAGES,
  SUPPORTED_LANGUAGE_CODES,
  LANGUAGE_MAP,
  DEFAULT_LANGUAGE,
  FALLBACK_LANGUAGE,
  MASTER_VERSION,
  normalizeLang,
  resolveRequestedLang,
  t,
  getBundle,
  getCatalogSummary,
  reloadBundles: loadBundles,
};
