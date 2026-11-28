const crypto = require('crypto');

const LOG_LEVEL_WEIGHT = {
  error: 0,
  warn: 1,
  info: 2,
  debug: 3,
};

function getConfiguredLogLevel() {
  const raw = String(process.env.LOG_LEVEL || 'info').trim().toLowerCase();
  return Object.prototype.hasOwnProperty.call(LOG_LEVEL_WEIGHT, raw) ? raw : 'info';
}

function shouldLog(level) {
  const current = getConfiguredLogLevel();
  return (LOG_LEVEL_WEIGHT[level] ?? 2) <= (LOG_LEVEL_WEIGHT[current] ?? 2);
}

function emitStructuredLog(level, event, fields = {}) {
  if (!shouldLog(level)) return;
  const entry = {
    ts: new Date().toISOString(),
    level,
    service: 'oozox-api',
    event,
    ...fields,
  };
  const line = JSON.stringify(entry);
  if (level === 'error') {
    process.stderr.write(line + '\n');
  } else {
    process.stdout.write(line + '\n');
  }
}

function structuredAccessLogger(req, res, next) {
  const requestId = req.headers['x-request-id'] || crypto.randomUUID();
  req.requestId = String(requestId).slice(0, 64);
  res.setHeader('X-Request-Id', req.requestId);

  const startedAt = process.hrtime.bigint();
  res.on('finish', () => {
    const durationMs = Number(process.hrtime.bigint() - startedAt) / 1e6;
    const pathOnly = (req.originalUrl || req.url || '').split('?')[0];
    emitStructuredLog(res.statusCode >= 500 ? 'error' : 'info', 'http_access', {
      requestId: req.requestId,
      method: req.method,
      path: pathOnly,
      status: res.statusCode,
      durationMs: Number(durationMs.toFixed(2)),
      ip: req.ip || '',
    });
  });

  next();
}

let sentryClient = null;
let sentryInitialized = false;

function getOptionalSentry() {
  if (sentryInitialized) return sentryClient;
  sentryInitialized = true;
  const dsn = String(process.env.SENTRY_DSN || '').trim();
  if (!dsn) return null;
  try {
    // Optional dependency: works cleanly if @sentry/node is installed on server or omitted
    // eslint-disable-next-line global-require
    const Sentry = require('@sentry/node');
    Sentry.init({
      dsn,
      environment: process.env.NODE_ENV || 'production',
      tracesSampleRate: 0.1,
    });
    sentryClient = Sentry;
    emitStructuredLog('info', 'sentry_initialized', { enabled: true });
  } catch {
    emitStructuredLog('warn', 'sentry_sdk_not_installed', { enabled: false });
  }
  return sentryClient;
}

async function sendTelegramOperationalAlert(title, details = '') {
  const token = String(process.env.ALERT_TELEGRAM_BOT_TOKEN || '').trim();
  const chatId = String(process.env.ALERT_TELEGRAM_CHAT_ID || '').trim();
  if (!token || !chatId) return false;

  const text = `[OOZOX ALERT] ${title}\n${String(details || '').slice(0, 1200)}`.trim();
  try {
    const resp = await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chat_id: chatId,
        text,
        disable_web_page_preview: true,
      }),
      signal: AbortSignal.timeout(5000),
    });
    return resp.ok;
  } catch {
    return false;
  }
}

function captureOperationalException(err, context = {}) {
  const sentry = getOptionalSentry();
  if (sentry && typeof sentry.captureException === 'function') {
    sentry.captureException(err, { extra: context });
  }
  if (process.env.NODE_ENV === 'production') {
    sendTelegramOperationalAlert(
      context.title || 'API 5xx Error',
      `${err?.message || 'Unknown error'} (${context.method || ''} ${context.path || ''})`
    ).catch(() => {});
  }
}

module.exports = {
  emitStructuredLog,
  structuredAccessLogger,
  getOptionalSentry,
  sendTelegramOperationalAlert,
  captureOperationalException,
};
