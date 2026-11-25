const mongoose = require('mongoose');
const crypto = require('crypto');
const { encryptField, decryptField } = require('../utils/fieldCrypto');

const projectWorkspaceSchema = new mongoose.Schema(
  {
    projectId: {
      type: String,
      required: true,
      unique: true,
      default: () => crypto.randomUUID(),
    },
    title: {
      type: String,
      required: true,
      trim: true,
      maxlength: 160,
    },
    description: {
      type: String,
      default: '',
      maxlength: 2000,
      set: encryptField,
      get: decryptField,
    },
    category: {
      type: String,
      default: 'GENERAL',
      trim: true,
      maxlength: 60,
    },
    status: {
      type: String,
      enum: ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED'],
      default: 'ACTIVE',
      index: true,
    },
    budgetAmount: {
      type: Number,
      default: 0,
      min: 0,
    },
    currency: {
      type: String,
      default: 'EGP',
      trim: true,
      maxlength: 12,
    },
    ownerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    workGroupId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'WorkGroup',
      default: null,
    },
    members: [
      {
        userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
        role: { type: String, enum: ['OWNER', 'MANAGER', 'CONTRIBUTOR', 'VIEWER'], default: 'CONTRIBUTOR' },
        addedAt: { type: Date, default: Date.now },
      },
    ],
    milestones: [
      {
        milestoneId: { type: String, default: () => crypto.randomUUID() },
        title: { type: String, required: true, trim: true, maxlength: 140 },
        completed: { type: Boolean, default: false },
        dueDate: { type: Date, default: null },
      },
    ],
    progressPercent: {
      type: Number,
      default: 0,
      min: 0,
      max: 100,
    },
  },
  {
    timestamps: true,
    toJSON: { getters: true },
    toObject: { getters: true },
  }
);

projectWorkspaceSchema.index({ 'members.userId': 1, updatedAt: -1 });

module.exports = mongoose.model('ProjectWorkspace', projectWorkspaceSchema);
