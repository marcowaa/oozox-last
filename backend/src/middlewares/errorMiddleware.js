const { errorResponse } = require('../utils/response');
const { resolveRequestedLang, t } = require('../i18n');
const { emitStructuredLog, captureOperationalException } = require('../utils/monitoring');

const notFound = (req, res, next) => {
  const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);
  return errorResponse(res, 404, translate('api.route_not_found', { url: req.originalUrl }));
};

const errorHandler = (err, req, res, next) => {
  const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);

  let statusCode = res.statusCode === 200 ? 500 : res.statusCode;
  let message = statusCode >= 500
    ? translate('api.internal_server_error')
    : (err.message || translate('api.internal_server_error'));

  // Handle Mongoose duplicate key error (code 11000)
  if (err.code === 11000) {
    statusCode = 400;
    const field = Object.keys(err.keyValue || {})[0] || 'field';
    message = translate('api.duplicate_field', { field });
  }

  // Handle Mongoose CastError (invalid ObjectId)
  if (err.name === 'CastError') {
    statusCode = 400;
    message = translate('api.invalid_id_value', { value: err.value });
  }

  // Handle Mongoose ValidationError
  if (err.name === 'ValidationError') {
    statusCode = 400;
    message = translate('api.validation_error');
  }

  const cleanPath = (req.originalUrl || req.url || '').split('?')[0];
  emitStructuredLog(statusCode >= 500 ? 'error' : 'warn', 'http_error', {
    requestId: req.requestId || '',
    method: req.method,
    path: cleanPath,
    status: statusCode,
    errorName: err.name || 'Error',
    message: err.message || 'Unhandled error',
  });

  if (statusCode >= 500) {
    captureOperationalException(err, {
      title: 'OOZOX API 5xx Exception',
      method: req.method,
      path: cleanPath,
      requestId: req.requestId || '',
    });
  }

  return errorResponse(
    res,
    statusCode,
    message,
    process.env.NODE_ENV === 'development' ? err.stack : null
  );
};

module.exports = {
  notFound,
  errorHandler,
};
