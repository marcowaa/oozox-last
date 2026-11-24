const User = require('../models/User');
const { normalizePhoneE164 } = require('../utils/phoneUtils');

/**
 * Idempotent migration for pre-OTP legacy users:
 * - Marks existing unmigrated accounts created before server-side OTP rollout as verified
 *   if they do not already have explicit legacyMigratedAt, OR normalizes phoneE164 safely
 *   so existing active accounts are NOT locked out when User.isVerified default flips to false.
 */
async function migrateLegacyUsers({ markExistingAsVerified = true } = {}) {
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
  migrateLegacyUsers,
};
