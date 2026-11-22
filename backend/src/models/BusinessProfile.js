const mongoose = require('mongoose');

const businessProfileSchema = new mongoose.Schema(
  {
    owner: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    companyName: {
      type: String,
      required: [true, 'Company name is required'],
      trim: true,
      maxlength: 140,
    },
    activityType: {
      type: String,
      required: true,
      trim: true,
      maxlength: 80,
      default: 'GENERAL_TRADING',
    },
    customActivityLabel: {
      type: String,
      trim: true,
      maxlength: 120,
      default: '',
    },
    commercialRegistration: {
      type: String,
      trim: true,
      maxlength: 60,
      default: '',
    },
    taxNumber: {
      type: String,
      trim: true,
      maxlength: 60,
      default: '',
    },
    phone: {
      type: String,
      trim: true,
      maxlength: 30,
      default: '',
    },
    address: {
      type: String,
      trim: true,
      maxlength: 250,
      default: '',
    },
    currency: {
      type: String,
      trim: true,
      maxlength: 12,
      default: 'EGP',
    },
    workStartTime: {
      type: String,
      trim: true,
      default: '09:00',
    },
    workEndTime: {
      type: String,
      trim: true,
      default: '17:00',
    },
    hqLatitude: {
      type: Number,
      default: null,
      min: -90,
      max: 90,
    },
    hqLongitude: {
      type: Number,
      default: null,
      min: -180,
      max: 180,
    },
    geofenceRadiusMeters: {
      type: Number,
      default: 250,
      min: 50,
      max: 10000,
    },
    companyGroupId: {
      type: String,
      trim: true,
      default: 'wg_company_main',
    },
    companyGroupName: {
      type: String,
      trim: true,
      maxlength: 140,
      default: 'مجموعة موظفي الشركة',
    },
    isSetupCompleted: {
      type: Boolean,
      default: true,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('BusinessProfile', businessProfileSchema);
