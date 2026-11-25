const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const { normalizePhoneE164 } = require('../utils/phoneUtils');

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      default: 'مستخدم أوزوكس',
      trim: true,
      maxlength: [100, 'Name cannot exceed 100 characters'],
    },
    // Preserved strictly for user profile, invoices, contacts & business records (NOT required or used for auth)
    email: {
      type: String,
      required: false,
      default: undefined,
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
    // Optional legacy column preserved for non-breaking schema compatibility
    password: {
      type: String,
      required: false,
      default: undefined,
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

// Partial unique index on email ONLY when non-empty string exists (allows phone-only users without email, preserves uniqueness for non-auth email profiles)
userSchema.index(
  { email: 1 },
  {
    name: 'email_1',
    unique: true,
    partialFilterExpression: { email: { $type: 'string', $gt: '' } },
  }
);

userSchema.pre('validate', function (next) {
  if (typeof this.email === 'string' && this.email.trim() === '') {
    this.email = undefined;
  } else if (this.email === null) {
    this.email = undefined;
  }
  if (typeof this.name !== 'string' || this.name.trim() === '') {
    this.name = 'مستخدم أوزوكس';
  }
  if (this.phone && (!this.phoneE164 || this.isModified('phone'))) {
    const normalized = normalizePhoneE164(this.phone);
    if (normalized) {
      this.phoneE164 = normalized;
    }
  }
  next();
});

// Encrypt legacy password if explicitly modified and present
userSchema.pre('save', async function (next) {
  if (!this.isModified('password') || !this.password) {
    return next();
  }
  const salt = await bcrypt.genSalt(12);
  this.password = await bcrypt.hash(this.password, salt);
  if (!this.isNew) {
    this.passwordChangedAt = new Date();
  }
  next();
});

userSchema.methods.matchPassword = async function (enteredPassword) {
  if (!this.password || !enteredPassword) return false;
  return await bcrypt.compare(enteredPassword, this.password);
};

module.exports = mongoose.model('User', userSchema);
