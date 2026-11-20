const User = require('../models/User');
const { generateToken } = require('../utils/jwt');
const { successResponse, errorResponse } = require('../utils/response');
const { escapeRegex } = require('../middlewares/sanitizeMiddleware');

// @desc    Register a new user
// @route   POST /api/auth/register
// @access  Public
const registerUser = async (req, res, next) => {
  try {
    const { name, email, phone, password, role } = req.body;
    const normalizedEmail = email.trim().toLowerCase();
    const normalizedPhone = phone.trim();
    // Strict RBAC guard: public registration may only assign USER or MERCHANT
    const safeRole = role === 'MERCHANT' ? 'MERCHANT' : 'USER';

    const userExists = await User.findOne({
      $or: [{ email: normalizedEmail }, { phone: normalizedPhone }],
    });

    if (userExists) {
      return errorResponse(
        res,
        400,
        userExists.email === normalizedEmail
          ? 'البريد الإلكتروني مسجل مسبقاً'
          : 'رقم الهاتف مسجل مسبقاً'
      );
    }

    const user = await User.create({
      name: name.trim(),
      email: normalizedEmail,
      phone: normalizedPhone,
      password,
      role: safeRole,
    });

    const token = generateToken(user._id, user.role);

    return successResponse(res, 201, 'تم إنشاء الحساب بنجاح', {
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        role: user.role,
        avatar: user.avatar,
      },
      token,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Authenticate user & get token
// @route   POST /api/auth/login
// @access  Public
const loginUser = async (req, res, next) => {
  try {
    const { emailOrPhone, password } = req.body;
    if (typeof emailOrPhone !== 'string' || typeof password !== 'string') {
      return errorResponse(res, 400, 'بيانات الدخول غير صالحة');
    }

    const input = emailOrPhone.trim();

    const user = await User.findOne({
      $or: [{ email: input.toLowerCase() }, { phone: input }],
    }).select('+password');

    if (!user) {
      return errorResponse(res, 401, 'بيانات الدخول غير صحيحة');
    }

    const isMatch = await user.matchPassword(password);
    if (!isMatch) {
      return errorResponse(res, 401, 'بيانات الدخول غير صحيحة');
    }

    const token = generateToken(user._id, user.role);

    return successResponse(res, 200, 'تم تسجيل الدخول بنجاح', {
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        role: user.role,
        avatar: user.avatar,
      },
      token,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get current user profile
// @route   GET /api/auth/me
// @access  Private
const getMe = async (req, res, next) => {
  try {
    const user = await User.findById(req.user._id);
    if (!user) {
      return errorResponse(res, 404, 'المستخدم غير موجود');
    }
    return successResponse(res, 200, 'تم جلب الملف الشخصي بنجاح', { user });
  } catch (error) {
    next(error);
  }
};

// @desc    Search users by phone or email
// @route   GET /api/auth/search
// @access  Private
const searchUsers = async (req, res, next) => {
  try {
    const qRaw = typeof req.query.q === 'string' ? req.query.q.trim() : '';
    if (qRaw.length < 2) {
      return successResponse(res, 200, 'قائمة فارغة', { users: [] });
    }

    // ReDoS-safe escaped regex bounded to 64 chars
    const safePattern = escapeRegex(qRaw.slice(0, 64));
    const regex = new RegExp(safePattern, 'i');
    const users = await User.find({
      _id: { $ne: req.user._id },
      $or: [{ name: regex }, { phone: regex }, { email: regex }],
    })
      .select('name phone email role avatar')
      .limit(20);

    return successResponse(res, 200, 'نتائج البحث', { users });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  registerUser,
  loginUser,
  getMe,
  searchUsers,
};
