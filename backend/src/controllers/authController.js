const User = require('../models/User');
const { generateToken } = require('../utils/jwt');
const { successResponse, errorResponse } = require('../utils/response');
const { escapeRegex } = require('../middlewares/sanitizeMiddleware');
const { normalizeLang } = require('../i18n');

// @desc    Register a new user
// @route   POST /api/auth/register
// @access  Public
const registerUser = async (req, res, next) => {
  try {
    const { name, email, phone, password, role, preferredLanguage } = req.body;
    const normalizedEmail = email.trim().toLowerCase();
    const normalizedPhone = phone.trim();
    // Strict RBAC guard: public registration may only assign USER or MERCHANT
    const safeRole = role === 'MERCHANT' ? 'MERCHANT' : 'USER';
    const userLang = normalizeLang(preferredLanguage || req.lang);

    const userExists = await User.findOne({
      $or: [{ email: normalizedEmail }, { phone: normalizedPhone }],
    });

    if (userExists) {
      return errorResponse(
        res,
        400,
        userExists.email === normalizedEmail
          ? req.t('api.email_already_registered')
          : req.t('api.phone_already_registered')
      );
    }

    const user = await User.create({
      name: name.trim(),
      email: normalizedEmail,
      phone: normalizedPhone,
      password,
      role: safeRole,
      preferredLanguage: userLang,
    });

    const token = generateToken(user._id, user.role);

    return successResponse(res, 201, req.t('api.register_success'), {
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        role: user.role,
        avatar: user.avatar,
        preferredLanguage: user.preferredLanguage,
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
      return errorResponse(res, 400, req.t('api.invalid_login_payload'));
    }

    const input = emailOrPhone.trim();

    const user = await User.findOne({
      $or: [{ email: input.toLowerCase() }, { phone: input }],
    }).select('+password');

    if (!user) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    const isMatch = await user.matchPassword(password);
    if (!isMatch) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    const token = generateToken(user._id, user.role);

    return successResponse(res, 200, req.t('api.login_success'), {
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        role: user.role,
        avatar: user.avatar,
        preferredLanguage: user.preferredLanguage || req.lang,
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
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }
    return successResponse(res, 200, req.t('api.profile_fetched'), { user });
  } catch (error) {
    next(error);
  }
};

// @desc    Update preferred notification/UI language for authenticated user
// @route   PATCH /api/auth/language
// @access  Private
const updateLanguagePreference = async (req, res, next) => {
  try {
    const lang = normalizeLang(req.body.language);
    const user = await User.findByIdAndUpdate(
      req.user._id,
      { preferredLanguage: lang },
      { new: true }
    );
    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }
    return successResponse(res, 200, req.t('api.language_preference_updated'), {
      preferredLanguage: user.preferredLanguage,
      user,
    });
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
      return successResponse(res, 200, req.t('api.search_empty'), { users: [] });
    }

    // ReDoS-safe escaped regex bounded to 64 chars
    const safePattern = escapeRegex(qRaw.slice(0, 64));
    const regex = new RegExp(safePattern, 'i');
    const users = await User.find({
      _id: { $ne: req.user._id },
      $or: [{ name: regex }, { phone: regex }, { email: regex }],
    })
      .select('name phone email role avatar preferredLanguage')
      .limit(20);

    return successResponse(res, 200, req.t('api.search_results'), { users });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  registerUser,
  loginUser,
  getMe,
  updateLanguagePreference,
  searchUsers,
};
