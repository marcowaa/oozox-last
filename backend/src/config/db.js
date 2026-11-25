const mongoose = require('mongoose');
const { ensureSafeUserEmailPartialIndex } = require('../services/migrationService');

const connectDB = async () => {
  try {
    const mongoUri = (process.env.MONGO_URI || '').trim();
    if (process.env.NODE_ENV === 'production' && !mongoUri) {
      throw new Error('MONGO_URI environment variable is strictly required in production.');
    }

    const resolvedUri = mongoUri || 'mongodb://localhost:27017/oozox_db';
    const conn = await mongoose.connect(resolvedUri, {
      autoIndex: true,
      serverSelectionTimeoutMS: 10000,
      socketTimeoutMS: 45000,
    });
    await ensureSafeUserEmailPartialIndex().catch((err) => {
      console.warn(`[OOZOX-DATABASE-WARN] Email partial index check skipped: ${err.message}`);
    });
    console.log(`[OOZOX-DATABASE] MongoDB Connected: ${conn.connection.host}`);
  } catch (error) {
    console.error(`[OOZOX-DATABASE-ERROR] Database connection failed: ${error.message}`);
    process.exit(1);
  }
};

module.exports = connectDB;
