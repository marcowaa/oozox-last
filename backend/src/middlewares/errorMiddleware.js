const { errorResponse } = require('../utils/response');
const { resolveRequestedLang, t } = require('../i18n');

const notFound = (req, res, next) => {
  const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);
  return errorResponse(res, 404, translate('api.route_not_found', { url: req.originalUrl }));
};

const errorHandler = (err, req, res, next) => {
  console.error('[OOZOX-SERVER-ERROR]', err);
  const translate = typeof req.t === 'function' ? req.t : (k, v) => t(resolveRequestedLang(req), k, v);

  let statusCode = res.statusCode === 200 ? 500 : res.statusCode;
  let message = err.message || translate('api.internal_server_error');

  // Handle Mongoose duplicate key error (code 11000)
  if (err.code === 11000) {
    statusCode = 400;
    const field = Object.keys(err.keyValue)[0];
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
    message = Object.values(err.errors)
      .map((val) => val.message)
      .join(', ');
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
