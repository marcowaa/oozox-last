const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const dotenv = require('dotenv');

dotenv.config();

const authRoutes = require('./routes/authRoutes');
const invoiceRoutes = require('./routes/invoiceRoutes');
const notificationRoutes = require('./routes/notificationRoutes');
const healthRoutes = require('./routes/healthRoutes');
const i18nRoutes = require('./routes/i18nRoutes');
const businessRoutes = require('./routes/businessRoutes');
const hrRoutes = require('./routes/hrRoutes');
const taskRoutes = require('./routes/taskRoutes');
const backupRoutes = require('./routes/backupRoutes');
const transferRoutes = require('./routes/transferRoutes');
const callRoutes = require('./routes/callRoutes');
const aiRoutes = require('./routes/aiRoutes');
const { groupRouter, projectRouter, chatRouter, contentRouter } = require('./routes/workspaceRoutes');
const { protect } = require('./middlewares/authMiddleware');
const { validate, schemas } = require('./middlewares/validateMiddleware');
const { syncFcmToken } = require('./controllers/otpController');
const { sanitizeNoSql } = require('./middlewares/sanitizeMiddleware');
const { i18nMiddleware } = require('./middlewares/i18nMiddleware');
const { notFound, errorHandler } = require('./middlewares/errorMiddleware');
const { structuredAccessLogger, getOptionalSentry } = require('./utils/monitoring');

const app = express();
getOptionalSentry();

app.set('trust proxy', Number(process.env.TRUST_PROXY_HOPS) || 1);
app.disable('x-powered-by');
app.use(helmet());

// Safe CORS configuration: never combine wildcard '*' with credentials: true
const rawOrigins = process.env.CLIENT_ORIGIN
  ? process.env.CLIENT_ORIGIN.split(',').map((o) => o.trim()).filter(Boolean)
  : [];

app.use(
  cors({
    origin: (origin, callback) => {
      // Allow native mobile clients / CLI / same-origin requests without Origin header
      if (!origin) return callback(null, true);
      if (rawOrigins.includes(origin)) return callback(null, true);
      if (
        rawOrigins.length === 0 &&
        /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(origin)
      ) {
        return callback(null, true);
      }
      return callback(new Error('CORS origin not allowed'));
    },
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: [
      'Content-Type',
      'Authorization',
      'Accept-Language',
      'X-App-Language',
      'If-None-Match',
      'X-Device-Id',
      'X-Device-Name',
      'X-Platform',
      'X-Device-Fingerprint',
      'X-Signaling-Timestamp',
      'X-Signaling-Signature',
      'X-Request-Timestamp',
      'X-Request-Signature',
    ],
    exposedHeaders: ['ETag', 'Content-Language', 'X-I18n-Version'],
    credentials: true,
  })
);

app.use(i18nMiddleware);

// Rate Limiting to protect against DoS
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.API_RATE_LIMIT_MAX) || 500,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) =>
    res.status(429).json({
      success: false,
      message: req.t ? req.t('api.rate_limit') : 'Too many requests, please try again later.',
    }),
});
app.use('/api', limiter);

// Route-scoped 16MB parser for encrypted backups/transfer, 4MB for AI vision, 1MB default elsewhere
const largeJsonParser = express.json({ limit: '16mb' });
const aiJsonParser = express.json({ limit: '4mb' });
const defaultJsonParser = express.json({
  limit: '1mb',
  verify: (req, _res, buf) => {
    req.rawBody = buf;
  },
});

app.use((req, res, next) => {
  if (req.path.startsWith('/api/backups') || req.path.startsWith('/api/transfer')) {
    return largeJsonParser(req, res, next);
  }
  if (req.path.startsWith('/api/ai')) {
    return aiJsonParser(req, res, next);
  }
  return defaultJsonParser(req, res, next);
});
app.use(express.urlencoded({ extended: true, limit: '1mb' }));
app.use(sanitizeNoSql);

if (process.env.NODE_ENV === 'production') {
  app.use(structuredAccessLogger);
} else if (process.env.NODE_ENV !== 'test') {
  app.use(morgan('dev'));
}

app.get('/', (req, res) => {
  res.json({
    project: 'OOZOX API Platform',
    status: 'Running',
    version: '1.0.0',
    language: req.lang,
    documentation: '/api/health',
    i18n: '/api/i18n/languages',
  });
});

app.use('/api/health', healthRoutes);
app.use('/api/i18n', i18nRoutes);
app.use('/api/auth', authRoutes);
app.post('/api/devices/fcm-token', protect, validate(schemas.syncFcmToken), syncFcmToken);
app.use('/api/backups', backupRoutes);
app.use('/api/transfer', transferRoutes);
app.use('/api/calls', callRoutes);
app.use('/api/ai', aiRoutes);
app.use('/api/groups', groupRouter);
app.use('/api/projects', projectRouter);
app.use('/api/chats', chatRouter);
app.use('/api/content', contentRouter);
app.use('/api/invoices', invoiceRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/business', businessRoutes);
app.use('/api/hr', hrRoutes);
app.use('/api/tasks', taskRoutes);

app.use(notFound);
app.use(errorHandler);

module.exports = app;
