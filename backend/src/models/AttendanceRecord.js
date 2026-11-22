const mongoose = require('mongoose');

const attendanceRecordSchema = new mongoose.Schema(
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
    employeeName: {
      type: String,
      required: true,
      trim: true,
    },
    date: {
      type: String,
      required: true,
      trim: true,
      index: true,
    },
    status: {
      type: String,
      enum: ['CLOCKED_IN', 'CLOCKED_OUT', 'OUT_OF_BOUNDS', 'GPS_DISABLED'],
      default: 'CLOCKED_IN',
    },
    gpsEnabled: {
      type: Boolean,
      required: true,
    },
    latitude: {
      type: Number,
      default: null,
    },
    longitude: {
      type: Number,
      default: null,
    },
    distanceMeters: {
      type: Number,
      default: null,
    },
    inBounds: {
      type: Boolean,
      default: false,
    },
    checkInAt: {
      type: Date,
      default: Date.now,
    },
    checkOutAt: {
      type: Date,
      default: null,
    },
    lastHeartbeatAt: {
      type: Date,
      default: Date.now,
    },
    outOfBoundsSince: {
      type: Date,
      default: null,
    },
    managerAlerted: {
      type: Boolean,
      default: false,
    },
    alertDetails: {
      type: String,
      trim: true,
      default: '',
    },
  },
  { timestamps: true }
);

attendanceRecordSchema.index({ business: 1, employee: 1, date: 1 });

module.exports = mongoose.model('AttendanceRecord', attendanceRecordSchema);
