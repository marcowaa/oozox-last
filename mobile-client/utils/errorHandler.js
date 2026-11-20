/**
 * Format and extract human-friendly error messages from Axios API responses
 */
export const getErrorMessage = (error, defaultMessage = 'حدث خطأ غير متوقع في الاتصال بالخادم') => {
  if (!error) return defaultMessage;

  if (error.response && error.response.data) {
    const { message, errors } = error.response.data;
    if (errors && Array.isArray(errors) && errors.length > 0) {
      return errors.map((e) => e.message).join('\n');
    }
    if (message) return message;
  }

  if (error.request) {
    return 'تعذر الاتصال بالخادم، يرجى التحقق من اتصالك بالإنترنت';
  }

  return error.message || defaultMessage;
};
