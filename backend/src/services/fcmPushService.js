const User = require('../models/User');
const TrustedDevice = require('../models/TrustedDevice');

let adminSdk = null;
let initialized = false;
let initFailed = false;

function getFirebaseMessaging() {
  if (initialized) return adminSdk;
  if (initFailed) return null;

  try {
    const rawJson = (process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '').trim();
    const filePath = (process.env.FIREBASE_SERVICE_ACCOUNT_PATH || '').trim();

    if (!rawJson && !filePath) {
      return null;
    }

    // Lazy-require so unit tests run cleanly without Firebase credentials
    // eslint-disable-next-line global-require
    const admin = require('firebase-admin');
    if (admin.apps && admin.apps.length > 0) {
      adminSdk = admin.messaging();
      initialized = true;
      return adminSdk;
    }

    let serviceAccount = null;
    if (rawJson) {
      serviceAccount = JSON.parse(rawJson);
    } else if (filePath) {
      // eslint-disable-next-line global-require, import/no-dynamic-require
      serviceAccount = require(filePath);
    }

    if (!serviceAccount) {
      return null;
    }

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    adminSdk = admin.messaging();
    initialized = true;
    return adminSdk;
  } catch (err) {
    initFailed = true;
    console.warn(`[FCM-PUSH] Firebase Admin disabled/unavailable: ${err.message}`);
    return null;
  }
}

// Optional test hook for unit tests verifying FCM dispatch payloads without live Google credentials
let testPushHook = null;
function setFcmTestHook(fn) {
  testPushHook = typeof fn === 'function' ? fn : null;
}

async function resolveUserFcmTokens(userId, { targetDeviceId = null, excludeDeviceId = null } = {}) {
  const tokens = new Set();
  const user = await User.findById(userId).select('fcmTokens');
  if (user && Array.isArray(user.fcmTokens)) {
    for (const entry of user.fcmTokens) {
      if (!entry || !entry.token) continue;
      if (targetDeviceId && entry.deviceId !== targetDeviceId) continue;
      if (excludeDeviceId && entry.deviceId === excludeDeviceId) continue;
      tokens.add(entry.token);
    }
  }

  const deviceFilter = { userId, revokedAt: null, fcmToken: { $nin: [null, ''] } };
  if (targetDeviceId) deviceFilter.deviceId = targetDeviceId;
  if (excludeDeviceId) deviceFilter.deviceId = { $ne: excludeDeviceId };
  const devices = await TrustedDevice.find(deviceFilter).select('fcmToken');
  for (const d of devices) {
    if (d.fcmToken) tokens.add(d.fcmToken);
  }
  return Array.from(tokens);
}

async function sendDataPushToUser({
  userId,
  data = {},
  notification = null,
  highPriority = true,
  targetDeviceId = null,
  excludeDeviceId = null,
}) {
  const stringData = {};
  for (const [k, v] of Object.entries(data || {})) {
    if (v !== undefined && v !== null) {
      stringData[k] = typeof v === 'string' ? v : JSON.stringify(v);
    }
  }

  if (testPushHook) {
    try {
      await testPushHook({ userId: String(userId), data: stringData, notification, targetDeviceId, excludeDeviceId });
    } catch {
      // ignore test hook failure
    }
  }

  const messaging = getFirebaseMessaging();
  if (!messaging) {
    return { sent: 0, skipped: true, reason: 'FCM_NOT_CONFIGURED' };
  }

  const tokens = await resolveUserFcmTokens(userId, { targetDeviceId, excludeDeviceId });
  if (tokens.length === 0) {
    return { sent: 0, skipped: true, reason: 'NO_FCM_TOKENS' };
  }

  try {
    const payload = {
      tokens,
      data: stringData,
      android: {
        priority: highPriority ? 'high' : 'normal',
      },
      ...(notification ? { notification } : {}),
    };
    const response = await messaging.sendEachForMulticast(payload);
    return {
      sent: response.successCount || 0,
      failed: response.failureCount || 0,
      skipped: false,
    };
  } catch (err) {
    console.warn(`[FCM-PUSH] Send failed: ${err.message}`);
    return { sent: 0, failed: tokens.length, error: err.message };
  }
}

module.exports = {
  setFcmTestHook,
  resolveUserFcmTokens,
  sendDataPushToUser,
};
