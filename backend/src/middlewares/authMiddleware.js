const { verifyToken } = require('../utils/jwt');
const User = require('../models/User');
const AuthSession = require('../models/AuthSession');
const { errorResponse } = require('../utils/response');
const { resolveRequestedLang, t } = require('../i18n');

async function validateFullAccessSession(decoded, user) {
  if (!decoded || decoded.scope === 'PRE_AUTH') {
    return { ok: false, status: 401, key: 'api.auth_token_invalid' };
  }
  if (!user) {
    return { ok: false, status: 401, key: 'api.auth_user_not_found' };
  }
  if (user.isVerified !== true) {
    return { ok: false, status: 403, key: 'api.auth_account_unverified' };
  }
  if (user.passwordChangedAt && decoded.iat) {
    const tokenIssuedMs = decoded.iat * 1000;
    if (tokenIssuedMs < user.passwordChangedAt.getTime() - 1000) {
      return { ok: false, status: 401, key: 'api.auth_token_invalid' };
    }
  }
  if (decoded.sid) {
    const session = await AuthSession.findOne({ sessionId: decoded.sid, userId: user._id });
    if (!session || session.revokedAt || !session.expiresAt || session.expiresAt <= new Date()) {
      return { ok: false, status: 401, key: 'api.auth_token_invalid' };
    }
  }
  return { ok: true };
}

const protect = async (req, res, next) => {
  const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);
  let token;

  if (
    req.headers.authorization &&
    req.headers.authorization.startsWith('Bearer')
  ) {
    token = req.headers.authorization.split(' ')[1];
  }

  if (!token) {
    return errorResponse(res, 401, translate('api.auth_token_missing'));
  }

  try {
    const decoded = verifyToken(token);
    if (decoded.scope === 'PRE_AUTH') {
      return errorResponse(res, 401, translate('api.auth_token_invalid'));
    }

    const user = await User.findById(decoded.id).select('-password');
    const check = await validateFullAccessSession(decoded, user);
    if (!check.ok) {
      return errorResponse(res, check.status, translate(check.key));
    }

    req.user = user;
    req.authSessionId = decoded.sid || null;
    next();
  } catch (error) {
    return errorResponse(res, 401, translate('api.auth_token_invalid'));
  }
};

const authorize = (...roles) => {
  return (req, res, next) => {
    const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);
    if (!req.user || !roles.includes(req.user.role)) {
      return errorResponse(
        res,
        403,
        translate('api.auth_forbidden_role', { role: req.user ? req.user.role : translate('api.unknown_role') })
      );
    }
    next();
  };
};

const protectEnterprise = async (req, res, next) => {
  const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);
  let token;

  if (req.headers.authorization && req.headers.authorization.startsWith('Bearer')) {
    token = req.headers.authorization.split(' ')[1];
  }

  if (!token) {
    return errorResponse(res, 401, translate('api.auth_token_missing'));
  }

  try {
    const decoded = verifyToken(token);
    if (decoded.scope === 'PRE_AUTH') {
      return errorResponse(res, 401, translate('api.auth_token_invalid'));
    }

    if (decoded.tokenType === 'EMPLOYEE' || decoded.role === 'EMPLOYEE') {
      const Employee = require('../models/Employee');
      const employee = await Employee.findById(decoded.id);
      if (!employee || !employee.isActive) {
        return errorResponse(res, 401, translate('api.auth_user_not_found'));
      }
      req.employee = employee;
      req.actorOwnerId = employee.owner;
      req.actorBusinessId = employee.business;
      req.isEmployeeActor = true;
      return next();
    }

    const user = await User.findById(decoded.id).select('-password');
    const check = await validateFullAccessSession(decoded, user);
    if (!check.ok) {
      return errorResponse(res, check.status, translate(check.key));
    }
    req.user = user;
    req.authSessionId = decoded.sid || null;
    req.actorOwnerId = user._id;
    req.isEmployeeActor = false;
    return next();
  } catch (error) {
    return errorResponse(res, 401, translate('api.auth_token_invalid'));
  }
};

const requirePermission = (permissionKey) => {
  return (req, res, next) => {
    const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);
    if (!req.isEmployeeActor && req.user) {
      return next();
    }
    if (req.isEmployeeActor && req.employee && req.employee.permissions && req.employee.permissions[permissionKey] === true) {
      return next();
    }
    return errorResponse(
      res,
      403,
      translate('api.auth_forbidden_role', { role: req.employee ? req.employee.roleTitle : translate('api.unknown_role') })
    );
  };
};

module.exports = {
  protect,
  authorize,
  protectEnterprise,
  requirePermission,
};
