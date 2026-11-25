const User = require('../models/User');
const { normalizePhoneE164 } = require('../utils/phoneUtils');

/**
 * Safely upgrades an existing strict `email_1` unique index to a partial unique index
 * that ignores missing/null/empty email values while preserving every historical email address.
 */
async function ensureSafeUserEmailPartialIndex() {
  try {
    const collection = User.collection;
    const indexes = await collection.indexes();
    const existingEmailIdx = indexes.find((idx) => idx.name === 'email_1');

    if (existingEmailIdx && !existingEmailIdx.partialFilterExpression) {
      await collection.dropIndex('email_1');
    }

    if (!existingEmailIdx || !existingEmailIdx.partialFilterExpression) {
      await collection.createIndex(
        { email: 1 },
        {
          name: 'email_1',
          unique: true,
          partialFilterExpression: { email: { $type: 'string', $gt: '' } },
        }
      );
    }
    return { ok: true };
  } catch (err) {
    // NamespaceNotFound (code 26) is benign before first insert on empty DBs
    if (err && err.code === 26) {
      return { ok: true, skipped: true };
    }
    throw err;
  }
}

/**
 * Idempotent migration for pre-OTP legacy users:
 * - Ensures partial unique index for User.email so phone-only accounts never hit E11000.
 * - Normalizes phoneE164 safely and preserves existing verified accounts & emails.
 */
async function migrateLegacyUsers({ markExistingAsVerified = true } = {}) {
  await ensureSafeUserEmailPartialIndex();

  const candidates = await User.find({
    legacyMigratedAt: null,
  });

  let updatedCount = 0;
  for (const user of candidates) {
    let changed = false;

    if (!user.phoneE164 && user.phone) {
      const normalized = normalizePhoneE164(user.phone);
      if (normalized) {
        user.phoneE164 = normalized;
        changed = true;
      }
    }

    if (markExistingAsVerified && user.isVerified !== true) {
      user.isVerified = true;
      user.phoneVerifiedAt = user.phoneVerifiedAt || user.createdAt || new Date();
      changed = true;
    } else if (user.isVerified === true && !user.phoneVerifiedAt) {
      user.phoneVerifiedAt = user.createdAt || new Date();
      changed = true;
    }

    user.legacyMigratedAt = new Date();
    changed = true;

    if (changed) {
      await user.save();
      updatedCount += 1;
    }
  }

  return { scanned: candidates.length, updatedCount };
}

module.exports = {
  ensureSafeUserEmailPartialIndex,
  migrateLegacyUsers,
};
