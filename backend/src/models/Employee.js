const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const permissionsSchema = new mongoose.Schema(
  {
    manageBusiness: { type: Boolean, default: false },
    manageStaff: { type: Boolean, default: false },
    managePayroll: { type: Boolean, default: false },
    manageProjects: { type: Boolean, default: false },
    assignTasks: { type: Boolean, default: false },
    manageAccounting: { type: Boolean, default: true },
    createInvoices: { type: Boolean, default: true },
    approveDiscounts: { type: Boolean, default: false },
    viewReports: { type: Boolean, default: false },
    directChat: { type: Boolean, default: true },
  },
  { _id: false }
);

const employeeSchema = new mongoose.Schema(
  {
    business: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'BusinessProfile',
      required: true,
      index: true,
    },
    owner: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    fullName: {
      type: String,
      required: true,
      trim: true,
      maxlength: 120,
    },
    username: {
      type: String,
      required: true,
      trim: true,
      lowercase: true,
      maxlength: 60,
    },
    phone: {
      type: String,
      trim: true,
      maxlength: 30,
      default: '',
    },
    passwordHash: {
      type: String,
      required: true,
      select: false,
    },
    roleTitle: {
      type: String,
      trim: true,
      maxlength: 80,
      default: 'موظف',
    },
    department: {
      type: String,
      trim: true,
      maxlength: 80,
      default: 'العمليات',
    },
    baseSalary: {
      type: Number,
      default: 0,
      min: 0,
    },
    salaryCurrency: {
      type: String,
      trim: true,
      maxlength: 12,
      default: 'EGP',
    },
    permissions: {
      type: permissionsSchema,
      default: () => ({}),
    },
    geofenceEnforced: {
      type: Boolean,
      default: true,
    },
    isActive: {
      type: Boolean,
      default: true,
    },
    companyGroupMemberId: {
      type: String,
      trim: true,
      default: null,
    },
    lastLoginAt: {
      type: Date,
      default: null,
    },
  },
  {
    timestamps: true,
    toJSON: {
      transform(doc, ret) {
        delete ret.passwordHash;
        return ret;
      },
    },
  }
);

employeeSchema.index({ business: 1, username: 1 }, { unique: true });

employeeSchema.pre('save', async function (next) {
  if (!this.isModified('passwordHash')) {
    return next();
  }
  const salt = await bcrypt.genSalt(12);
  this.passwordHash = await bcrypt.hash(this.passwordHash, salt);
  next();
});

employeeSchema.methods.matchPassword = async function (enteredPassword) {
  return bcrypt.compare(enteredPassword, this.passwordHash);
};

module.exports = mongoose.model('Employee', employeeSchema);
