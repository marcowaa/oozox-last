const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const { normalizePhoneE164 } = require('../utils/phoneUtils');

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Please provide user full name'],
      trim: true,
      maxlength: [100, 'Name cannot exceed 100 characters'],
    },
    email: {
      type: String,
      required: [true, 'Please provide a valid email address'],
      unique: true,
      trim: true,
      lowercase: true,
      match: [
        /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
        'Please provide a valid email address',
      ],
    },
    phone: {
      type: String,
      required: [true, 'Please provide phone number'],
      unique: true,
      trim: true,
    },
    phoneE164: {
      type: String,
      trim: true,
      unique: true,
      sparse: true,
      index: true,
    },
    password: {
      type: String,
      required: [true, 'Please provide a secure password'],
      minlength: [6, 'Password must be at least 6 characters'],
      select: false,
    },
    role: {
      type: String,
      enum: ['USER', 'ADMIN', 'MERCHANT'],
      default: 'USER',
    },
    avatar: {
      type: String,
      default: '',
    },
    isVerified: {
      type: Boolean,
      default: false,
    },
    phoneVerifiedAt: {
      type: Date,
      default: null,
    },
    passwordChangedAt: {
      type: Date,
      default: null,
    },
    legacyMigratedAt: {
      type: Date,
      default: null,
    },
    dailyCallLimitMinutes: {
      type: Number,
      default: 0,
      min: 0,
    },
    fcmTokens: [
      {
        token: { type: String, required: true },
        deviceId: { type: String, default: '' },
        platform: { type: String, default: 'ANDROID' },
        updatedAt: { type: Date, default: Date.now },
      },
    ],
    preferredLanguage: {
      type: String,
      enum: ['ar', 'en', 'fr', 'es', 'tr', 'de', 'zh', 'hi', 'ru', 'ja', 'pt', 'it', 'ur', 'fa'],
      default: 'ar',
    },
  },
  {
    timestamps: true,
    toJSON: {
      transform(doc, ret) {
        delete ret.password;
        delete ret.fcmTokens;
        return ret;
      },
    },
  }
);

userSchema.pre('validate', function (next) {
  if (this.phone && (!this.phoneE164 || this.isModified('phone'))) {
    const normalized = normalizePhoneE164(this.phone);
    if (normalized) {
      this.phoneE164 = normalized;
    }
  }
  next();
});

// Encrypt password using bcrypt before saving & track password change timestamp
userSchema.pre('save', async function (next) {
  if (!this.isModified('password')) {
    return next();
  }
  const salt = await bcrypt.genSalt(12);
  this.password = await bcrypt.hash(this.password, salt);
  if (!this.isNew) {
    this.passwordChangedAt = new Date();
  }
  next();
});

// Method to match user password
userSchema.methods.matchPassword = async function (enteredPassword) {
  return await bcrypt.compare(enteredPassword, this.password);
};

module.exports = mongoose.model('User', userSchema);
