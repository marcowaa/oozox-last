const {
  OtpError,
  requestOtp: requestOtpService,
  verifyOtp: verifyOtpService,
  createTelegramLink,
} = require('../services/otpService');

const handleOtpError = (res, error, next) => {
  if (error instanceof OtpError) {
    return res.status(error.statusCode).json({
      success: false,
      code: error.code,
      message: error.message,
    });
  }
  return next(error);
};

const requestOtp = async (req, res, next) => {
  try {
    const data = await requestOtpService(req.body);
    return res.status(200).json({
      success: true,
      message: 'Verification code sent.',
      data,
    });
  } catch (error) {
    return handleOtpError(res, error, next);
  }
};

const verifyOtp = async (req, res, next) => {
  try {
    const data = await verifyOtpService(req.body);
    return res.status(200).json({
      success: true,
      message: 'Phone number verified.',
      data,
    });
  } catch (error) {
    return handleOtpError(res, error, next);
  }
};

const startTelegramLink = async (req, res, next) => {
  try {
    const header = req.headers.authorization || '';
    const match = header.match(/^Bearer\s+(.+)$/i);
    if (!match) {
      throw new OtpError(401, 'PHONE_PROOF_REQUIRED', 'A recent phone verification is required.');
    }

    const data = await createTelegramLink(match[1]);
    return res.status(200).json({
      success: true,
      message: 'Open the Telegram link to connect your account.',
      data,
    });
  } catch (error) {
    return handleOtpError(res, error, next);
  }
};

module.exports = { requestOtp, verifyOtp, startTelegramLink };
