const { resolveRequestedLang, t, LANGUAGE_MAP, DEFAULT_LANGUAGE } = require('../i18n');

const i18nMiddleware = (req, res, next) => {
  const lang = resolveRequestedLang(req);
  const meta = LANGUAGE_MAP[lang] || LANGUAGE_MAP[DEFAULT_LANGUAGE];

  req.lang = lang;
  req.langDirection = meta.direction;
  req.t = (key, vars) => t(lang, key, vars);

  res.setHeader('Content-Language', lang);
  next();
};

module.exports = {
  i18nMiddleware,
};
