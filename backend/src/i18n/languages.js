const SUPPORTED_LANGUAGES = [
  { code: 'ar', name: 'Arabic', nativeName: 'العربية', flag: '🇸🇦', codeBadge: 'AR', direction: 'rtl', locale: 'ar-EG' },
  { code: 'en', name: 'English', nativeName: 'English', flag: '🇺🇸', codeBadge: 'EN', direction: 'ltr', locale: 'en-US' },
  { code: 'fr', name: 'French', nativeName: 'Français', flag: '🇫🇷', codeBadge: 'FR', direction: 'ltr', locale: 'fr-FR' },
  { code: 'es', name: 'Spanish', nativeName: 'Español', flag: '🇪🇸', codeBadge: 'ES', direction: 'ltr', locale: 'es-ES' },
  { code: 'tr', name: 'Turkish', nativeName: 'Türkçe', flag: '🇹🇷', codeBadge: 'TR', direction: 'ltr', locale: 'tr-TR' },
  { code: 'de', name: 'German', nativeName: 'Deutsch', flag: '🇩🇪', codeBadge: 'DE', direction: 'ltr', locale: 'de-DE' },
  { code: 'zh', name: 'Chinese', nativeName: '中文', flag: '🇨🇳', codeBadge: 'ZH', direction: 'ltr', locale: 'zh-CN' },
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', flag: '🇮🇳', codeBadge: 'HI', direction: 'ltr', locale: 'hi-IN' },
  { code: 'ru', name: 'Russian', nativeName: 'Русский', flag: '🇷🇺', codeBadge: 'RU', direction: 'ltr', locale: 'ru-RU' },
  { code: 'ja', name: 'Japanese', nativeName: '日本語', flag: '🇯🇵', codeBadge: 'JA', direction: 'ltr', locale: 'ja-JP' },
  { code: 'pt', name: 'Portuguese', nativeName: 'Português', flag: '🇵🇹', codeBadge: 'PT', direction: 'ltr', locale: 'pt-PT' },
  { code: 'it', name: 'Italian', nativeName: 'Italiano', flag: '🇮🇹', codeBadge: 'IT', direction: 'ltr', locale: 'it-IT' },
  { code: 'ur', name: 'Urdu', nativeName: 'اردو', flag: '🇵🇰', codeBadge: 'UR', direction: 'rtl', locale: 'ur-PK' },
  { code: 'fa', name: 'Persian', nativeName: 'فارسی', flag: '🇮🇷', codeBadge: 'FA', direction: 'rtl', locale: 'fa-IR' },
];

const SUPPORTED_LANGUAGE_CODES = SUPPORTED_LANGUAGES.map((l) => l.code);
const LANGUAGE_MAP = Object.fromEntries(SUPPORTED_LANGUAGES.map((l) => [l.code, l]));
const DEFAULT_LANGUAGE = 'ar';
const FALLBACK_LANGUAGE = 'en';

module.exports = {
  SUPPORTED_LANGUAGES,
  SUPPORTED_LANGUAGE_CODES,
  LANGUAGE_MAP,
  DEFAULT_LANGUAGE,
  FALLBACK_LANGUAGE,
};
