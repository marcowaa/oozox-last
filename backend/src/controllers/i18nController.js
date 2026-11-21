const {
  SUPPORTED_LANGUAGE_CODES,
  MASTER_VERSION,
  normalizeLang,
  getBundle,
  getCatalogSummary,
} = require('../i18n');
const { successResponse, errorResponse } = require('../utils/response');

// @desc    Get all 14 supported languages & metadata
// @route   GET /api/i18n/languages
// @access  Public
const getSupportedLanguages = (req, res) => {
  const summary = getCatalogSummary();
  res.setHeader('Cache-Control', 'public, max-age=300');
  res.setHeader('X-I18n-Version', MASTER_VERSION);
  return successResponse(res, 200, req.t('api.i18n_languages_loaded'), summary);
};

// @desc    Get i18n catalog version and per-language ETags
// @route   GET /api/i18n/version
// @access  Public
const getI18nVersion = (req, res) => {
  const summary = getCatalogSummary();
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('X-I18n-Version', MASTER_VERSION);
  return successResponse(res, 200, req.t('api.i18n_version_loaded'), summary);
};

// @desc    Get full translation bundle for a specific language (with ETag/304 support)
// @route   GET /api/i18n/:lang
// @access  Public
const getLanguageBundle = (req, res) => {
  const requestedRaw = String(req.params.lang || '').trim();
  const baseCode = requestedRaw.toLowerCase().split(/[-_]/)[0];

  if (!SUPPORTED_LANGUAGE_CODES.includes(baseCode) && !SUPPORTED_LANGUAGE_CODES.includes(requestedRaw.toLowerCase())) {
    return errorResponse(res, 404, req.t('api.i18n_unsupported_lang', { lang: requestedRaw }));
  }

  const code = normalizeLang(requestedRaw);
  const bundle = getBundle(code);

  res.setHeader('ETag', bundle.etag);
  res.setHeader('Cache-Control', 'public, max-age=300');
  res.setHeader('Content-Language', code);
  res.setHeader('X-I18n-Version', bundle.version);

  const clientETag = req.headers['if-none-match'];
  if (clientETag && clientETag.trim() === bundle.etag) {
    return res.status(304).end();
  }

  return successResponse(res, 200, req.t('api.i18n_bundle_loaded'), bundle);
};

module.exports = {
  getSupportedLanguages,
  getI18nVersion,
  getLanguageBundle,
};
