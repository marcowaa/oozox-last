const { successResponse } = require('../utils/response');
const {
  generateChat,
  generateVision,
  getUserAiQuotaStatus,
} = require('../services/geminiProxyService');

// @desc    Server-side Gemini text/thinking/contextual-translation proxy
// @route   POST /api/ai/chat
// @access  Private
const handleAiChat = async (req, res, next) => {
  try {
    const {
      prompt,
      contextInfo = '',
      systemPrompt = '',
      model = '',
      useThinkingMode = false,
      mode = 'CHAT',
      targetLanguage = 'auto',
    } = req.body;

    // Security policy: NEVER log prompt contents, contextInfo, or translated text.
    const result = await generateChat({
      userId: req.user._id,
      prompt,
      contextInfo,
      systemPrompt,
      model,
      useThinkingMode,
      mode,
      targetLanguage,
    });

    return successResponse(res, 200, req.t('api.ai_completed') || 'AI response generated', result);
  } catch (err) {
    if (err.statusCode) {
      return res.status(err.statusCode).json({
        success: false,
        code: err.code || 'AI_PROXY_ERROR',
        message: err.message,
      });
    }
    next(err);
  }
};

// @desc    Server-side Gemini multimodal image/receipt analysis proxy
// @route   POST /api/ai/vision
// @access  Private
const handleAiVision = async (req, res, next) => {
  try {
    const {
      prompt,
      imageBase64,
      mimeType = 'image/jpeg',
      model = '',
      systemPrompt = '',
    } = req.body;

    // Security policy: NEVER log prompt contents or imageBase64 payloads.
    const result = await generateVision({
      userId: req.user._id,
      prompt,
      imageBase64,
      mimeType,
      model,
      systemPrompt,
    });

    return successResponse(res, 200, req.t('api.ai_completed') || 'AI vision analysis completed', result);
  } catch (err) {
    if (err.statusCode) {
      return res.status(err.statusCode).json({
        success: false,
        code: err.code || 'AI_PROXY_ERROR',
        message: err.message,
      });
    }
    next(err);
  }
};

// @desc    Fetch current user's server-enforced AI quota status
// @route   GET /api/ai/quota
// @access  Private
const getAiQuota = async (req, res, next) => {
  try {
    const quota = await getUserAiQuotaStatus(req.user._id);
    return successResponse(res, 200, req.t('api.ai_quota_fetched') || 'AI quota loaded', quota);
  } catch (err) {
    next(err);
  }
};

module.exports = {
  handleAiChat,
  handleAiVision,
  getAiQuota,
};
