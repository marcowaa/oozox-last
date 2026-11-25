const mongoose = require('mongoose');

const callRecordSchema = new mongoose.Schema(
  {
    callId: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    callerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    calleeId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    callType: {
      type: String,
      enum: ['AUDIO', 'VIDEO'],
      default: 'AUDIO',
    },
    status: {
      type: String,
      enum: ['AUTHORIZED', 'RINGING', 'ACCEPTED', 'ACTIVE', 'ENDED', 'REJECTED', 'MISSED', 'CANCELLED', 'FAILED'],
      default: 'AUTHORIZED',
      index: true,
    },
    billingMonthKey: {
      type: String,
      required: true,
      index: true,
    },
    authorizedAt: {
      type: Date,
      default: Date.now,
    },
    ringingAt: {
      type: Date,
      default: null,
    },
    // Billing strictly starts when callee accepts (ACCEPTED), NOT during ringing
    acceptedAt: {
      type: Date,
      default: null,
    },
    endedAt: {
      type: Date,
      default: null,
    },
    billedDurationSeconds: {
      type: Number,
      default: 0,
      min: 0,
    },
    billedMinutes: {
      type: Number,
      default: 0,
      min: 0,
    },
    estimatedRelayMb: {
      type: Number,
      default: 0,
      min: 0,
    },
    estimatedCostUsd: {
      type: Number,
      default: 0,
      min: 0,
    },
    endReason: {
      type: String,
      default: '',
      maxlength: 80,
    },
    turnUsed: {
      type: Boolean,
      default: false,
    },
    processedEventIds: {
      type: [String],
      default: [],
    },
  },
  { timestamps: true }
);

callRecordSchema.index({ billingMonthKey: 1, createdAt: -1 });
callRecordSchema.index({ callerId: 1, acceptedAt: -1 });

module.exports = mongoose.model('CallRecord', callRecordSchema);
