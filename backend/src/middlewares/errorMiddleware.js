const { errorResponse } = require('../utils/response');

const notFound = (req, res, next) => {
  return errorResponse(res, 404, `المسار المطلوب غير موجود: ${req.originalUrl}`);
};

const errorHandler = (err, req, res, next) => {
  console.error('[OOZOX-SERVER-ERROR]', err);

  let statusCode = res.statusCode === 200 ? 500 : res.statusCode;
  let message = err.message || 'حدث خطأ داخلي في الخادم';

  // Handle Mongoose duplicate key error (code 11000)
  if (err.code === 11000) {
    statusCode = 400;
    const field = Object.keys(err.keyValue)[0];
    message = `القيمة المدخلة في حقل (${field}) مستخدمة بالفعل ومسجلة مسبقاً`;
  }

  // Handle Mongoose CastError (invalid ObjectId)
  if (err.name === 'CastError') {
    statusCode = 400;
    message = `المعرف المطلوب (${err.value}) غير صالح`;
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
