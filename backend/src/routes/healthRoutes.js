const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const { successResponse, errorResponse } = require('../utils/response');

/**
 * @route   GET /api/health
 * @desc    Comprehensive Production Health Check & Monitoring
 * @access  Public
 */
router.get('/', (req, res) => {
  const readyStates = {
    0: 'DISCONNECTED',
    1: 'CONNECTED',
    2: 'CONNECTING',
    3: 'DISCONNECTING',
  };

  const dbStateCode = mongoose.connection.readyState;
  const dbStateText = readyStates[dbStateCode] || 'UNKNOWN';
  const isHealthy = dbStateCode === 1;

  const healthData = {
    status: isHealthy ? 'HEALTHY' : 'DEGRADED',
    timestamp: new Date().toISOString(),
    uptimeSeconds: Math.floor(process.uptime()),
    database: {
      status: dbStateText,
      name: mongoose.connection.name || 'oozox_db',
      host: mongoose.connection.host || 'localhost',
      port: mongoose.connection.port || 27017,
      isHealthy,
    },
    system: {
      nodeVersion: process.version,
      platform: process.platform,
      memoryUsageMB: {
        rss: (process.memoryUsage().rss / 1024 / 1024).toFixed(2),
        heapTotal: (process.memoryUsage().heapTotal / 1024 / 1024).toFixed(2),
        heapUsed: (process.memoryUsage().heapUsed / 1024 / 1024).toFixed(2),
      },
    },
    environment: process.env.NODE_ENV || 'production',
    version: '1.0.0',
  };

  if (!isHealthy) {
    return errorResponse(res, 503, 'Database Connection Degraded', healthData);
  }

  return successResponse(res, 200, 'OOZOX Production API is Healthy and Operational', healthData);
});

module.exports = router;
