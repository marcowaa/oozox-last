const express = require('express');
const router = express.Router();
const {
  getSupportedLanguages,
  getI18nVersion,
  getLanguageBundle,
} = require('../controllers/i18nController');

router.get('/languages', getSupportedLanguages);
router.get('/version', getI18nVersion);
router.get('/:lang', getLanguageBundle);

module.exports = router;
