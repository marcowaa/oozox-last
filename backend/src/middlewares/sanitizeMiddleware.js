/**
 * Security sanitization & defensive input helpers
 */

const isPlainObject = (val) =>
  val !== null && typeof val === 'object' && !Array.isArray(val) && !(val instanceof Date);

/**
 * Recursively strips keys starting with '$' or containing '.' to block NoSQL operator injection.
 * Returns { cleanValue, hadUnsafeKeys }.
 */
const sanitizeObject = (value) => {
  let hadUnsafeKeys = false;

  const walk = (node) => {
    if (Array.isArray(node)) {
      return node.map(walk);
    }
    if (isPlainObject(node)) {
      const cleaned = {};
      for (const [key, val] of Object.entries(node)) {
        if (key.startsWith('$') || key.includes('.')) {
          hadUnsafeKeys = true;
          continue;
        }
        cleaned[key] = walk(val);
      }
      return cleaned;
    }
    return node;
  };

  return {
    cleanValue: walk(value),
    hadUnsafeKeys,
  };
};

/**
 * Express middleware: strips MongoDB '$' / '.' operator keys from req.body, req.query, and req.params.
 * Rejects requests whose body or query explicitly contain raw Mongo operator keys (e.g. {"$ne": null})
 * with 400 Bad Request, or sanitizes cleanly.
 */
const sanitizeNoSql = (req, res, next) => {
  let detected = false;

  if (req.body && typeof req.body === 'object') {
    const resBody = sanitizeObject(req.body);
    if (resBody.hadUnsafeKeys) detected = true;
    req.body = resBody.cleanValue;
  }

  if (req.query && typeof req.query === 'object') {
    const resQuery = sanitizeObject(req.query);
    if (resQuery.hadUnsafeKeys) detected = true;
    req.query = resQuery.cleanValue;
  }

  if (req.params && typeof req.params === 'object') {
    const resParams = sanitizeObject(req.params);
    if (resParams.hadUnsafeKeys) detected = true;
    req.params = resParams.cleanValue;
  }

  if (detected) {
    return res.status(400).json({
      success: false,
      message: 'تم رفض الطلب لاحتوائه على رموز أو معاملات غير مصرح بها (Unsafe input rejected)',
      errors: [{ field: 'payload', message: 'Prohibited operator key detected' }],
    });
  }

  next();
};

/**
 * Escapes all RegExp metacharacters to prevent ReDoS / regex injection attacks.
 */
const escapeRegex = (str = '') => {
  if (typeof str !== 'string') return '';
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

module.exports = {
  sanitizeNoSql,
  sanitizeObject,
  escapeRegex,
};
