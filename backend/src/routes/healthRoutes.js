const express = require('express');
const net = require('net');
const router = express.Router();
const mongoose = require('mongoose');
const { successResponse, errorResponse } = require('../utils/response');
const { verifyToken } = require('../utils/jwt');
const User = require('../models/User');
const AuthSession = require('../models/AuthSession');

function normalizeIp(rawIp) {
  if (!rawIp || typeof rawIp !== 'string') return '';
  let ip = rawIp.trim();
  if (ip.startsWith('::ffff:')) {
    ip = ip.slice(7);
  }
  return ip;
}

function isPrivateOrLoopbackIp(rawIp) {
  const ip = normalizeIp(rawIp);
  if (!ip) return false;
  if (ip === '127.0.0.1' || ip === '::1') return true;
  if (net.isIPv4(ip)) {
    const parts = ip.split('.').map(Number);
    if (parts[0] === 10) return true;
    if (parts[0] === 127) return true;
    if (parts[0] === 192 && parts[1] === 168) return true;
    if (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) return true;
  }
  if (net.isIPv6(ip)) {
    const lower = ip.toLowerCase();
    if (lower.startsWith('fc') || lower.startsWith('fd') || lower.startsWith('fe80:')) {
      return true;
    }
  }
  return false;
}

async function authorizeHealthDetails(req, res, next) {
  const authHeader = req.headers.authorization || '';
  if (authHeader.startsWith('Bearer ')) {
    const token = authHeader.split(' ')[1];
    try {
      const decoded = verifyToken(token);
      if (decoded.scope === 'PRE_AUTH' || !decoded.id) {
        return errorResponse(res, 401, 'Unauthorized');
      }
      const user = await User.findById(decoded.id).select('role isVerified passwordChangedAt');
      if (!user || !user.isVerified) {
        return errorResponse(res, 401, 'Unauthorized');
      }
      if (user.passwordChangedAt && decoded.iat && decoded.iat * 1000 < user.passwordChangedAt.getTime() - 1000) {
        return errorResponse(res, 401, 'Unauthorized');
      }
      if (decoded.sid) {
        const session = await AuthSession.findOne({ sessionId: decoded.sid, userId: user._id });
        if (!session || session.revokedAt || !session.expiresAt || session.expiresAt <= new Date()) {
          return errorResponse(res, 401, 'Unauthorized');
        }
      }
      if (user.role !== 'ADMIN') {
        return errorResponse(res, 403, 'Admin authorization required');
      }
      return next();
    } catch {
      return errorResponse(res, 401, 'Unauthorized');
    }
  }

  // Allow direct internal Docker / localhost network calls only when NOT proxied from an external client
  const forwarded = req.headers['x-forwarded-for'] || req.headers['x-real-ip'];
  const remoteAddr = req.socket?.remoteAddress || req.connection?.remoteAddress || '';
  if (!forwarded && isPrivateOrLoopbackIp(remoteAddr)) {
    return next();
  }

  return errorResponse(res, 401, 'Unauthorized');
}

/**
 * @route   GET /api/health
 * @desc    Minimal public health endpoint (status only, zero host/port/memory leaks)
 * @access  Public
 */
router.get('/', (req, res) => {
  const isHealthy = mongoose.connection.readyState === 1;
  const payload = { status: isHealthy ? 'HEALTHY' : 'DEGRADED' };

  if (!isHealthy) {
    return errorResponse(res, 503, 'Service Degraded', payload);
  }
  return successResponse(res, 200, 'OK', payload);
});

/**
 * @route   GET /api/health/details
 * @desc    Detailed diagnostics for ADMIN users or direct internal network checks
 * @access  Private (ADMIN or internal network)
 */
router.get('/details', authorizeHealthDetails, (req, res) => {
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

  return successResponse(res, 200, 'OOZOX Production API Detailed Health', healthData);
});

module.exports = router;
