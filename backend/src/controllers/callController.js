const crypto = require('crypto');
const CallRecord = require('../models/CallRecord');
const User = require('../models/User');
const Notification = require('../models/Notification');
const { successResponse, errorResponse } = require('../utils/response');
const {
  getRtcConfig,
  getMonthKey,
  getOrCreateMonthLedger,
  syncCoturnMetrics,
  recomputeLedgerCost,
  evaluateBudgetThresholds,
  checkCanAuthorizeNewCall,
} = require('../services/callBudgetService');
const { sendDataPushToUser } = require('../services/fcmPushService');

function getSignalingHmacSecret() {
  const configured = (process.env.INTERNAL_SIGNALING_HMAC_SECRET || '').trim();
  if (process.env.NODE_ENV === 'production') {
    if (!configured || configured.length < 32) {
      throw new Error('FATAL: INTERNAL_SIGNALING_HMAC_SECRET must be set (>= 32 chars) in production.');
    }
    return configured;
  }
  if (configured) return configured;
  const jwtFallback = (process.env.JWT_SECRET || '').trim();
  if (jwtFallback) return jwtFallback;
  throw new Error('INTERNAL_SIGNALING_HMAC_SECRET or JWT_SECRET must be configured.');
}

function assertSignalingSecretConfigured() {
  return getSignalingHmacSecret();
}

function signCallAuthToken({ callId, callerId, calleeId, callType, exp }) {
  const secret = getSignalingHmacSecret();
  const canonical = `${callId}:${callerId}:${calleeId}:${callType}:${exp}`;
  return crypto.createHmac('sha256', secret).update(canonical).digest('hex');
}

function verifyInternalSignalingHmac(req) {
  let secret;
  try {
    secret = getSignalingHmacSecret();
  } catch {
    return false;
  }
  if (process.env.NODE_ENV === 'production' && secret.length < 32) {
    return false;
  }

  const timestamp = String(req.headers['x-signaling-timestamp'] || '').trim();
  const signature = String(req.headers['x-signaling-signature'] || '').trim();
  if (!timestamp || !signature) return false;

  const tsNum = Number(timestamp);
  if (!Number.isFinite(tsNum) || Math.abs(Math.floor(Date.now() / 1000) - tsNum) > 300) {
    return false;
  }
  const raw = req.rawBody ? req.rawBody.toString('utf8') : JSON.stringify(req.body || {});
  const expected = crypto.createHmac('sha256', secret).update(`${timestamp}.${raw}`).digest('hex');
  try {
    return crypto.timingSafeEqual(Buffer.from(signature, 'hex'), Buffer.from(expected, 'hex'));
  } catch {
    return false;
  }
}

// @desc    Authorize initiating a 1-on-1 audio/video call (checks 100% monthly RTC budget gate & daily user cap)
// @route   POST /api/calls/authorize
// @access  Private
const authorizeCall = async (req, res, next) => {
  try {
    const { calleeId, callType = 'AUDIO', callId } = req.body || {};
    if (!calleeId) {
      return errorResponse(res, 400, 'calleeId is required');
    }
    if (String(calleeId) === String(req.user._id)) {
      return errorResponse(res, 400, 'Cannot start a call with yourself');
    }

    const callee = await User.findById(calleeId).select('name phone phoneE164 avatar preferredLanguage');
    if (!callee) {
      return errorResponse(res, 404, req.t('api.user_not_found'));
    }

    const gate = await checkCanAuthorizeNewCall(req.user);
    if (!gate.allowed) {
      return res.status(429).json({
        success: false,
        code: gate.code,
        message: gate.message,
        usagePct: Number(gate.usagePct.toFixed(1)),
      });
    }

    // Optional per-user daily limit (disabled when RTC_DAILY_USER_LIMIT_MINUTES=0)
    if (gate.dailyUserLimitMinutes > 0) {
      const startOfDay = new Date();
      startOfDay.setUTCHours(0, 0, 0, 0);
      const userCallsToday = await CallRecord.aggregate([
        {
          $match: {
            callerId: req.user._id,
            acceptedAt: { $gte: startOfDay },
          },
        },
        { $group: { _id: null, totalMinutes: { $sum: '$billedMinutes' } } },
      ]);
      const usedMinutes = userCallsToday[0]?.totalMinutes || 0;
      if (usedMinutes >= gate.dailyUserLimitMinutes) {
        return res.status(429).json({
          success: false,
          code: 'RTC_DAILY_USER_LIMIT_EXCEEDED',
          message: `بلغت الحد اليومي للمكالمات (${gate.dailyUserLimitMinutes} دقيقة).`,
        });
      }
    }

    const resolvedCallId = String(callId || crypto.randomUUID()).trim().slice(0, 128);
    const normalizedType = String(callType).toUpperCase() === 'VIDEO' ? 'VIDEO' : 'AUDIO';
    const monthKey = getMonthKey();

    let record = await CallRecord.findOne({ callId: resolvedCallId });
    if (!record) {
      record = await CallRecord.create({
        callId: resolvedCallId,
        callerId: req.user._id,
        calleeId: callee._id,
        callType: normalizedType,
        status: 'AUTHORIZED',
        billingMonthKey: monthKey,
        authorizedAt: new Date(),
      });

      gate.ledger.totalCallsCount += 1;
      await gate.ledger.save();
    }

    const exp = Math.floor(Date.now() / 1000) + 120;
    const callAuthSignature = signCallAuthToken({
      callId: record.callId,
      callerId: String(req.user._id),
      calleeId: String(callee._id),
      callType: normalizedType,
      exp,
    });

    // Dispatch high-priority FCM push so locked/background callee phone rings immediately
    await sendDataPushToUser({
      userId: callee._id,
      highPriority: true,
      data: {
        type: 'incoming_call',
        callId: record.callId,
        callerId: String(req.user._id),
        callerName: req.user.name || req.user.phone || 'OOZOX Caller',
        callerPhone: req.user.phoneE164 || req.user.phone || '',
        callType: normalizedType,
        sentAt: String(Date.now()),
      },
      notification: {
        title: normalizedType === 'VIDEO' ? 'مكالمة فيديو واردة' : 'مكالمة صوتية واردة',
        body: `مكالمة من ${req.user.name || req.user.phone || 'مستخدم OOZOX'}`,
      },
    });

    return successResponse(res, 200, 'Call authorized', {
      allowed: true,
      callId: record.callId,
      callType: normalizedType,
      exp,
      callAuthSignature,
      usagePct: Number(gate.usagePct.toFixed(1)),
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Idempotent HMAC-signed call lifecycle webhook from Go Signaling Server
// @route   POST /api/calls/events
// @access  Internal Signed Signaling Server
const recordCallLifecycleEvent = async (req, res, next) => {
  try {
    if (!verifyInternalSignalingHmac(req)) {
      return errorResponse(res, 401, 'Invalid signaling HMAC signature');
    }

    const {
      callId,
      eventId,
      eventType,
      callerId,
      calleeId,
      callType = 'AUDIO',
      timestamp,
      endReason = '',
      turnUsed = false,
      estimatedRelayMb = 0,
    } = req.body || {};

    if (!callId || !eventType) {
      return errorResponse(res, 400, 'callId and eventType are required');
    }

    const normalizedEvent = String(eventType).toUpperCase();
    const evTime = timestamp ? new Date(timestamp) : new Date();
    const idempKey = String(eventId || `${callId}:${normalizedEvent}`);

    let record = await CallRecord.findOne({ callId });
    if (!record) {
      if (process.env.NODE_ENV === 'production') {
        return errorResponse(res, 404, 'CallRecord not found (must be pre-authorized via /api/calls/authorize)');
      }
      if (!callerId || !calleeId) {
        return errorResponse(res, 404, 'CallRecord not found and callerId/calleeId missing');
      }
      record = await CallRecord.create({
        callId,
        callerId,
        calleeId,
        callType: String(callType).toUpperCase() === 'VIDEO' ? 'VIDEO' : 'AUDIO',
        status: 'AUTHORIZED',
        billingMonthKey: getMonthKey(evTime),
        authorizedAt: evTime,
      });
    }

    // Strict idempotency guard
    if (record.processedEventIds.includes(idempKey)) {
      return successResponse(res, 200, 'Duplicate call event acknowledged idempotently', {
        call: record,
        duplicate: true,
      });
    }
    record.processedEventIds.push(idempKey);

    const cfg = getRtcConfig();
    const ledger = await getOrCreateMonthLedger(record.billingMonthKey);

    if (normalizedEvent === 'RINGING') {
      if (!record.ringingAt) record.ringingAt = evTime;
      if (record.status === 'AUTHORIZED') record.status = 'RINGING';
    } else if (normalizedEvent === 'ACCEPTED' || normalizedEvent === 'CALL_ACCEPTED') {
      // Billing starts ONLY at CALL_ACCEPTED
      if (!record.acceptedAt) {
        record.acceptedAt = evTime;
        ledger.answeredCallsCount += 1;
        await ledger.save();
      }
      record.status = 'ACTIVE';
    } else if (normalizedEvent === 'ENDED' || normalizedEvent === 'CALL_ENDED') {
      record.endedAt = evTime;
      record.status = 'ENDED';
      record.turnUsed = Boolean(turnUsed || record.turnUsed);

      if (record.acceptedAt) {
        const rawDurSec = Math.max(0, Math.round((evTime.getTime() - record.acceptedAt.getTime()) / 1000));
        const maxDurSec = cfg.maxCallDurationSeconds || 14400;
        const capped = rawDurSec > maxDurSec;
        const durSec = capped ? maxDurSec : rawDurSec;
        const billedMinutes = durSec > 0 ? Math.ceil(durSec / 60) : 0;
        record.billedDurationSeconds = durSec;
        record.billedMinutes = billedMinutes;
        record.endReason = capped ? 'MAX_DURATION' : String(endReason || 'COMPLETED').slice(0, 80);
        record.estimatedRelayMb = Number(estimatedRelayMb) || 0;
        record.estimatedCostUsd = Number((billedMinutes * cfg.costPerMinuteUsd).toFixed(6));

        ledger.totalBilledMinutes += billedMinutes;
        recomputeLedgerCost(ledger, cfg);
        await ledger.save();
        await syncCoturnMetrics(ledger);
        await evaluateBudgetThresholds(ledger, cfg);
      } else {
        // Call was never accepted (hung up during ringing) -> 0 billed duration
        record.endReason = String(endReason || 'COMPLETED').slice(0, 80);
        record.billedDurationSeconds = 0;
        record.billedMinutes = 0;
        record.estimatedCostUsd = 0;
      }

      await sendDataPushToUser({
        userId: record.calleeId,
        highPriority: true,
        data: {
          type: 'call_ended',
          callId: record.callId,
          reason: record.endReason || 'ENDED',
        },
      });
    } else if (['REJECTED', 'MISSED', 'CANCELLED', 'FAILED'].includes(normalizedEvent)) {
      record.endedAt = evTime;
      record.status = normalizedEvent;
      record.endReason = String(endReason || normalizedEvent).slice(0, 80);
      record.billedDurationSeconds = 0;
      record.billedMinutes = 0;
      record.estimatedCostUsd = 0;

      if (normalizedEvent === 'MISSED') {
        await Notification.create({
          recipient: record.calleeId,
          sender: record.callerId,
          title: 'مكالمة فائتة',
          message: `فاتتك مكالمة ${record.callType === 'VIDEO' ? 'فيديو' : 'صوتية'}.`,
          type: 'INCOMING_CALL',
        }).catch(() => {});
      }

      await sendDataPushToUser({
        userId: record.calleeId,
        highPriority: true,
        data: {
          type: 'call_ended',
          callId: record.callId,
          reason: normalizedEvent,
        },
      });
    }

    await record.save();
    return successResponse(res, 200, 'Call event processed', { call: record });
  } catch (err) {
    next(err);
  }
};

// @desc    Public/user budget availability status (without leaking admin financial internals)
// @route   GET /api/calls/status
// @access  Private
const getUserCallAvailability = async (req, res, next) => {
  try {
    const gate = await checkCanAuthorizeNewCall(req.user);
    return successResponse(res, 200, 'Call service status fetched', {
      canStartNewCalls: gate.allowed,
      blockedReason: gate.allowed ? null : gate.message,
      dailyUserLimitMinutes: gate.dailyUserLimitMinutes || 0,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Admin Monthly RTC Call Budget, Coturn Metrics & Threshold Alerts (50% / 80% / 100%)
// @route   GET /api/calls/budget
// @access  Private (ADMIN only)
const getAdminCallBudget = async (req, res, next) => {
  try {
    const cfg = getRtcConfig();
    const monthKey = String(req.query.month || getMonthKey());
    const ledger = await syncCoturnMetrics(await getOrCreateMonthLedger(monthKey));
    recomputeLedgerCost(ledger, cfg);
    const usagePct = Number(((ledger.estimatedCostUsd / cfg.monthlyBudgetUsd) * 100).toFixed(2));

    const recentAlerts = await Notification.find({ type: 'RTC_BUDGET_ALERT' })
      .sort({ createdAt: -1 })
      .limit(15)
      .select('title message createdAt');

    return successResponse(res, 200, 'RTC call budget telemetry fetched', {
      monthKey: ledger.monthKey,
      monthlyBudgetUsd: cfg.monthlyBudgetUsd,
      estimatedCostUsd: ledger.estimatedCostUsd,
      usagePct,
      thresholds: {
        alert1Pct: cfg.alert1Pct,
        alert2Pct: cfg.alert2Pct,
        hardStopPct: 100,
      },
      alertsTriggered: ledger.alertsTriggered,
      newCallsBlocked: usagePct >= 100,
      blockedNewCallsAt: ledger.blockedNewCallsAt,
      totalCallsCount: ledger.totalCallsCount,
      answeredCallsCount: ledger.answeredCallsCount,
      totalBilledMinutes: ledger.totalBilledMinutes,
      turnBytesTotal: ledger.turnBytesTotal,
      coturnRestartCount: ledger.coturnRestartCount,
      lastScrapedAt: ledger.lastScrapedAt,
      recentAlerts,
    });
  } catch (err) {
    next(err);
  }
};

// @desc    Paginated call logs (user sees own calls; ADMIN sees all platform calls with filters)
// @route   GET /api/calls/history
// @access  Private
const getCallHistory = async (req, res, next) => {
  try {
    const page = Math.max(1, Number(req.query.page) || 1);
    const limit = Math.min(100, Math.max(1, Number(req.query.limit) || 20));
    const status = req.query.status ? String(req.query.status).toUpperCase() : '';
    const callType = req.query.callType ? String(req.query.callType).toUpperCase() : '';

    const filter = {};
    if (req.user.role !== 'ADMIN' || req.query.scope === 'mine') {
      filter.$or = [{ callerId: req.user._id }, { calleeId: req.user._id }];
    }
    if (status) filter.status = status;
    if (callType) filter.callType = callType;

    const [calls, total] = await Promise.all([
      CallRecord.find(filter)
        .populate('callerId', 'name phone phoneE164 avatar')
        .populate('calleeId', 'name phone phoneE164 avatar')
        .sort({ createdAt: -1 })
        .skip((page - 1) * limit)
        .limit(limit),
      CallRecord.countDocuments(filter),
    ]);

    return successResponse(res, 200, 'Call history fetched', {
      calls,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.max(1, Math.ceil(total / limit)),
      },
    });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  authorizeCall,
  recordCallLifecycleEvent,
  getUserCallAvailability,
  getAdminCallBudget,
  getCallHistory,
  assertSignalingSecretConfigured,
};
