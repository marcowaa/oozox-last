const mongoose = require('mongoose');

const workTaskSchema = new mongoose.Schema(
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
    projectId: {
      type: String,
      required: true,
      trim: true,
    },
    projectName: {
      type: String,
      required: true,
      trim: true,
      maxlength: 140,
    },
    workGroupId: {
      type: String,
      trim: true,
      default: 'wg_company_main',
    },
    title: {
      type: String,
      required: true,
      trim: true,
      maxlength: 160,
    },
    description: {
      type: String,
      trim: true,
      maxlength: 1200,
      default: '',
    },
    timingMode: {
      type: String,
      enum: ['NOW', 'HOURS', 'DAYS'],
      default: 'HOURS',
    },
    durationValue: {
      type: Number,
      default: 1,
      min: 0,
      max: 3650,
    },
    deadlineDurationText: {
      type: String,
      trim: true,
      default: '',
    },
    deadlineAt: {
      type: Date,
      required: true,
    },
    assignees: [
      {
        type: String,
        trim: true,
      },
    ],
    assigneeEmployeeIds: [
      {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Employee',
      },
    ],
    priority: {
      type: String,
      enum: ['LOW', 'MEDIUM', 'HIGH', 'URGENT'],
      default: 'HIGH',
    },
    status: {
      type: String,
      enum: ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
      default: 'PENDING',
    },
    financialAmount: {
      type: Number,
      default: 0,
      min: 0,
    },
    financialNotes: {
      type: String,
      trim: true,
      maxlength: 300,
      default: '',
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('WorkTask', workTaskSchema);
