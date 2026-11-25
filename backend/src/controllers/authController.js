const User = require('../models/User');
const { successResponse, errorResponse } = require('../utils/response');
const { escapeRegex } = require('../middlewares/sanitizeMiddleware');
const { normalizeLang } = require('../i18n');

// @desc    Get current authenticated user profile
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

// @desc    Update user profile fields (name, optional contact/business email, avatar, language)
// @route   PATCH /api/auth/me
// @access  Private
const updateMyProfile = async (req, res, next) => {
  try {
    const user = await User.findById(req.user._id);
    if (!user) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    const { name, email, avatar, preferredLanguage } = req.body;
    if (typeof name === 'string' && name.trim().length >= 2) {
      user.name = name.trim();
    }
    if (email !== undefined) {
      if (email === '') {
        user.email = undefined;
      } else {
        const cleanEmail = String(email).trim().toLowerCase();
        const existingEmailOwner = await User.findOne({
          _id: { $ne: user._id },
          email: cleanEmail,
        });
        if (existingEmailOwner) {
          return errorResponse(res, 400, req.t('api.email_already_registered'));
        }
        user.email = cleanEmail;
      }
    }
    if (typeof avatar === 'string') {
      user.avatar = avatar.trim();
    }
    if (preferredLanguage) {
      user.preferredLanguage = normalizeLang(preferredLanguage);
    }

    await user.save();
    return successResponse(res, 200, req.t('api.profile_updated') || req.t('api.profile_fetched'), {
      user,
    });
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

// @desc    Search users by name, phone, or preserved business email (for invoicing/chats/teams)
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
      $or: [{ name: regex }, { phone: regex }, { phoneE164: regex }, { email: regex }],
    })
      .select('name phone phoneE164 email role avatar preferredLanguage')
      .limit(20);

    return successResponse(res, 200, req.t('api.search_results'), { users });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getMe,
  updateMyProfile,
  updateLanguagePreference,
  searchUsers,
};
