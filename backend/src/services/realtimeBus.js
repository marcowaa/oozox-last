let publisher = null;
let redisAttempted = false;

function getRedisClient() {
  if (publisher) return publisher;
  if (redisAttempted) return null;
  redisAttempted = true;

  const redisUrl = (process.env.REDIS_URL || '').trim();
  const redisHost = (process.env.REDIS_HOST || '').trim();
  const redisAddr = (process.env.REDIS_ADDR || '').trim();

  if (!redisUrl && !redisHost && !redisAddr) {
    return null;
  }

  try {
    // eslint-disable-next-line global-require
    const Redis = require('ioredis');
    if (redisUrl) {
      publisher = new Redis(redisUrl, {
        maxRetriesPerRequest: 2,
        enableOfflineQueue: false,
        lazyConnect: true,
      });
    } else {
      const [host, portStr] = (redisAddr || `${redisHost}:6379`).split(':');
      publisher = new Redis({
        host: host || 'redis',
        port: Number(portStr) || 6379,
        password: process.env.REDIS_PASSWORD || undefined,
        db: Number(process.env.REDIS_DB) || 0,
        maxRetriesPerRequest: 2,
        enableOfflineQueue: false,
        lazyConnect: true,
      });
    }
    publisher.on('error', () => {});
    publisher.connect().catch(() => {});
    return publisher;
  } catch {
    return null;
  }
}

// Optional in-memory hook for automated test verification
let testPublishHook = null;
function setRealtimeTestHook(fn) {
  testPublishHook = typeof fn === 'function' ? fn : null;
}

/**
 * Publishes real-time platform events onto the Redis Pub/Sub channel consumed by Go Signaling Server
 * (`oozox:realtime:events`) so connected Android & Web WebSocket clients receive immediate updates.
 */
async function publishRealtimeEvent({ type, targetUserIds = [], targetDeviceId = '', payload = {} }) {
  const envelope = {
    type,
    targetUserIds: targetUserIds.map((id) => String(id)),
    targetDeviceId: targetDeviceId || '',
    payload,
    timestamp: Date.now(),
  };

  if (testPublishHook) {
    try {
      await testPublishHook(envelope);
    } catch {
      // ignore test hook errors
    }
  }

  const client = getRedisClient();
  if (!client) {
    return { published: false, reason: 'REDIS_NOT_CONFIGURED' };
  }

  try {
    await client.publish('oozox:realtime:events', JSON.stringify(envelope));
    return { published: true };
  } catch (err) {
    return { published: false, error: err.message };
  }
}

async function closeRealtimeBus() {
  if (publisher) {
    try {
      await publisher.quit();
    } catch {
      // ignore
    }
    publisher = null;
    redisAttempted = false;
  }
}

module.exports = {
  publishRealtimeEvent,
  setRealtimeTestHook,
  closeRealtimeBus,
};
