const { verifyToken } = require('../utils/jwt');
const User = require('../models/User');
const { errorResponse } = require('../utils/response');
const { resolveRequestedLang, t } = require('../i18n');

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
    const user = await User.findById(decoded.id).select('-password');

    if (!user) {
      return errorResponse(res, 401, translate('api.auth_user_not_found'));
    }

    req.user = user;
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
    if (!user) {
      return errorResponse(res, 401, translate('api.auth_user_not_found'));
    }
    req.user = user;
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
