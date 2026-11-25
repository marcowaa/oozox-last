const mongoose = require('mongoose');

const callBudgetLedgerSchema = new mongoose.Schema(
  {
    monthKey: {
      type: String,
      required: true,
      unique: true,
    },
    monthlyBudgetUsd: {
      type: Number,
      default: 100,
      min: 1,
    },
    totalCallsCount: {
      type: Number,
      default: 0,
      min: 0,
    },
    answeredCallsCount: {
      type: Number,
      default: 0,
      min: 0,
    },
    totalBilledMinutes: {
      type: Number,
      default: 0,
      min: 0,
    },
    // Cumulative TURN relay bytes accounted across Coturn restarts
    turnBytesTotal: {
      type: Number,
      default: 0,
      min: 0,
    },
    lastCoturnRawBytes: {
      type: Number,
      default: 0,
      min: 0,
    },
    coturnRestartCount: {
      type: Number,
      default: 0,
      min: 0,
    },
    estimatedCostUsd: {
      type: Number,
      default: 0,
      min: 0,
    },
    alertsTriggered: {
      type: [Number],
      default: [],
    },
    lastScrapedAt: {
      type: Date,
      default: null,
    },
    blockedNewCallsAt: {
      type: Date,
      default: null,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('CallBudgetLedger', callBudgetLedgerSchema);
