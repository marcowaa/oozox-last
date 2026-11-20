const { verifyToken } = require('../utils/jwt');
const User = require('../models/User');
const { errorResponse } = require('../utils/response');

const protect = async (req, res, next) => {
  let token;

  if (
    req.headers.authorization &&
    req.headers.authorization.startsWith('Bearer')
  ) {
    token = req.headers.authorization.split(' ')[1];
  }

  if (!token) {
    return errorResponse(res, 401, 'غير مصرح بالوصول: يرجى تسجيل الدخول وإرفاق التوكن (Token Missing)');
  }

  try {
    const decoded = verifyToken(token);
    const user = await User.findById(decoded.id).select('-password');

    if (!user) {
      return errorResponse(res, 401, 'المستخدم المرتبط بهذا التوكن غير موجود');
    }

    req.user = user;
    next();
  } catch (error) {
    return errorResponse(res, 401, 'جلسة العمل منتهية أو التوكن غير صالح (Token Expired / Invalid)');
  }
};

const authorize = (...roles) => {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return errorResponse(
        res,
        403,
        `غير مصرح لهذا الدور (${req.user ? req.user.role : 'مجهول'}) بتنفيذ هذا الإجراء`
      );
    }
    next();
  };
};

module.exports = {
  protect,
  authorize,
};
