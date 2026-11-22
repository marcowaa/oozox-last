const mongoose = require('mongoose');

const payrollAdjustmentSchema = new mongoose.Schema(
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
    },
    employee: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Employee',
      required: true,
      index: true,
    },
    type: {
      type: String,
      enum: ['BONUS', 'DEDUCTION'],
      required: true,
    },
    amount: {
      type: Number,
      required: true,
      min: 0.01,
    },
    reason: {
      type: String,
      required: true,
      trim: true,
      maxlength: 250,
    },
    effectiveDate: {
      type: String,
      required: true,
      trim: true,
    },
    monthKey: {
      type: String,
      required: true,
      trim: true,
      index: true,
    },
    createdByName: {
      type: String,
      trim: true,
      default: 'المدير',
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('PayrollAdjustment', payrollAdjustmentSchema);
