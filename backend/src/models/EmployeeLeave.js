const mongoose = require('mongoose');

const employeeLeaveSchema = new mongoose.Schema(
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
    leaveType: {
      type: String,
      enum: ['ANNUAL', 'SICK', 'EMERGENCY', 'UNPAID'],
      default: 'ANNUAL',
    },
    startDate: {
      type: String,
      required: true,
      trim: true,
    },
    endDate: {
      type: String,
      required: true,
      trim: true,
    },
    daysCount: {
      type: Number,
      default: 1,
      min: 1,
    },
    status: {
      type: String,
      enum: ['PENDING', 'APPROVED', 'REJECTED'],
      default: 'APPROVED',
    },
    notes: {
      type: String,
      trim: true,
      maxlength: 300,
      default: '',
    },
    approvedByName: {
      type: String,
      trim: true,
      default: 'المدير',
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('EmployeeLeave', employeeLeaveSchema);
