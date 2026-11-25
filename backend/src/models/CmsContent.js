const mongoose = require('mongoose');

const cmsContentSchema = new mongoose.Schema(
  {
    key: {
      type: String,
      required: true,
      unique: true,
      default: 'primary',
    },
    heroTitleAr: { type: String, default: 'إدارة الأعمال والذهب والمحاسبة المشفرة' },
    heroTitleEn: { type: String, default: 'OOZOX Enterprise, Gold & Encrypted Finance' },
    hero: {
      badge: { type: String, default: 'منصة أوزوكس المالية والتجارية' },
      title: { type: String, default: 'إدارة الأعمال والذهب والمحاسبة المشفرة' },
      subtitle: { type: String, default: 'دخول موثق بالهاتف، مكالمات مؤمنة، ونسخ احتياطي مشفر بدون تعقيد.' },
      ctaPrimary: { type: String, default: 'الدخول بالهاتف' },
      ctaSecondary: { type: String, default: 'استكشف المنصة' },
    },
    themeConfig: {
      type: mongoose.Schema.Types.Mixed,
      default: null,
    },
    goldRates: {
      k24: { type: Number, default: 4650 },
      k21: { type: Number, default: 4070 },
      k18: { type: Number, default: 3488 },
      pound: { type: Number, default: 32560 },
      ounceUsd: { type: Number, default: 2680 },
      currency: { type: String, default: 'EGP' },
      updatedBy: { type: String, default: '' },
    },
    features: {
      type: [mongoose.Schema.Types.Mixed],
      default: [],
    },
    announcements: {
      type: [mongoose.Schema.Types.Mixed],
      default: [],
    },
    tutorials: {
      type: [mongoose.Schema.Types.Mixed],
      default: [],
    },
    jobs: {
      type: [mongoose.Schema.Types.Mixed],
      default: [],
    },
    updatedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('CmsContent', cmsContentSchema);
