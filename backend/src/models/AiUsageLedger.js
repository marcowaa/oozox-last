const mongoose = require('mongoose');

const aiUsageLedgerSchema = new mongoose.Schema(
  {
    scopeKey: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    scopeType: {
      type: String,
      enum: ['GLOBAL_MONTH', 'USER_DAILY', 'USER_MONTHLY'],
      required: true,
    },
    periodKey: {
      type: String,
      required: true,
      index: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
      index: true,
    },
    requestsCount: {
      type: Number,
      default: 0,
      min: 0,
    },
    inputTokens: {
      type: Number,
      default: 0,
      min: 0,
    },
    outputTokens: {
      type: Number,
      default: 0,
      min: 0,
    },
    estimatedCostUsd: {
      type: Number,
      default: 0,
      min: 0,
    },
    lastRequestAt: {
      type: Date,
      default: null,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('AiUsageLedger', aiUsageLedgerSchema);
