const mongoose = require('mongoose');
const app = require('./app');
const connectDB = require('./config/db');
const { assertEncryptionKeyConfigured } = require('./utils/fieldCrypto');
const { getJwtSecret } = require('./utils/jwt');

const PORT = process.env.PORT || 5000;

if (process.env.NODE_ENV === 'production') {
  assertEncryptionKeyConfigured();
  getJwtSecret();
}

// Connect to MongoDB
connectDB();

const server = app.listen(PORT, () => {
  console.log(
    `===================================================\n` +
    `OOZOX Enterprise Production Server is running\n` +
    `Port: ${PORT}\n` +
    `Environment: ${process.env.NODE_ENV || 'production'}\n` +
    `Security: JWT + Helmet + RateLimit + CORS Enabled\n` +
    `===================================================`
  );
});

const gracefulShutdown = async (signal) => {
  console.log(`[SHUTDOWN] Received ${signal}. Closing HTTP server & database connections...`);
  server.close(async () => {
    try {
      await mongoose.connection.close(false);
      console.log('[SHUTDOWN] MongoDB connection closed cleanly.');
      process.exit(0);
    } catch (err) {
      console.error('[SHUTDOWN] Error closing MongoDB connection:', err.message);
      process.exit(1);
    }
  });

  setTimeout(() => {
    console.error('[SHUTDOWN] Forced exit after 10s timeout.');
    process.exit(1);
  }, 10000).unref();
};

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

// Handle unhandled promise rejections
process.on('unhandledRejection', (err) => {
  console.error(`[FATAL-UNHANDLED-REJECTION] ${err.message}`);
  server.close(() => process.exit(1));
});

module.exports = server;
