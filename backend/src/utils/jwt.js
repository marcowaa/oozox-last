const crypto = require('crypto');
const jwt = require('jsonwebtoken');

let ephemeralFallbackSecret = null;

const getJwtSecret = () => {
  const configured = process.env.JWT_SECRET && process.env.JWT_SECRET.trim();
  if (configured) {
    if (process.env.NODE_ENV === 'production' && configured.length < 32) {
      throw new Error('JWT_SECRET must be at least 32 characters in production');
    }
    return configured;
  }

  if (process.env.NODE_ENV === 'production') {
    throw new Error('FATAL: JWT_SECRET environment variable must be configured in production');
  }

  // Safe process-ephemeral fallback ONLY for local dev / automated unit tests
  if (!ephemeralFallbackSecret) {
    ephemeralFallbackSecret = crypto.randomBytes(48).toString('hex');
  }
  return ephemeralFallbackSecret;
};

const generateToken = (userId, role) => {
  return jwt.sign(
    {
      id: userId,
      sub: String(userId),
      role,
    },
    getJwtSecret(),
    {
      algorithm: 'HS256',
      expiresIn: process.env.JWT_EXPIRES_IN || '7d',
    }
  );
};

const generateEmployeeToken = (employee) => {
  return jwt.sign(
    {
      id: employee._id,
      sub: String(employee._id),
      role: 'EMPLOYEE',
      tokenType: 'EMPLOYEE',
      businessId: String(employee.business),
      ownerId: String(employee.owner),
    },
    getJwtSecret(),
    {
      algorithm: 'HS256',
      expiresIn: process.env.JWT_EXPIRES_IN || '7d',
    }
  );
};

const verifyToken = (token) => {
  return jwt.verify(token, getJwtSecret(), {
    algorithms: ['HS256'],
  });
};

module.exports = {
  generateToken,
  generateEmployeeToken,
  verifyToken,
  getJwtSecret,
};
