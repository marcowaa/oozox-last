/* eslint-disable no-console */
const dotenv = require('dotenv');
const mongoose = require('mongoose');
const connectDB = require('../config/db');
const { migrateLegacyUsers } = require('../services/migrationService');

dotenv.config();

async function main() {
  try {
    await connectDB();
    const res = await migrateLegacyUsers({ markExistingAsVerified: true });
    console.log('[OOZOX Migration] Legacy users migrated:', res);
    await mongoose.connection.close();
    process.exit(0);
  } catch (err) {
    console.error('[OOZOX Migration] Failed:', err.message);
    process.exit(1);
  }
}

main();
