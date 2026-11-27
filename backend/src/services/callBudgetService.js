const CallBudgetLedger = require('../models/CallBudgetLedger');
const CallRecord = require('../models/CallRecord');
const Notification = require('../models/Notification');
const User = require('../models/User');
const { sendDataPushToUser } = require('./fcmPushService');

function getRtcConfig() {
  return {
    monthlyBudgetUsd: Math.max(1, Number(process.env.RTC_MONTHLY_BUDGET_USD) || 100),
    alert1Pct: Math.max(1, Math.min(99, Number(process.env.RTC_ALERT_THRESHOLD_1_PCT) || 50)),
    alert2Pct: Math.max(1, Math.min(99, Number(process.env.RTC_ALERT_THRESHOLD_2_PCT) || 80)),
    costPerMinuteUsd: Math.max(0, Number(process.env.RTC_COST_PER_MINUTE_USD) || 0.004),
    turnCostPerGbUsd: Math.max(0, Number(process.env.RTC_TURN_COST_PER_GB_USD) || 0.09),
    dailyUserLimitMinutes: Math.max(0, Number(process.env.RTC_DAILY_USER_LIMIT_MINUTES) || 0),
    maxCallDurationSeconds: Math.max(60, Number(process.env.RTC_MAX_CALL_DURATION_SECONDS) || 14400),
    coturnMetricsUrl: (process.env.COTURN_METRICS_URL || '').trim(),
  };
}

function getMonthKey(date = new Date()) {
  return date.toISOString().slice(0, 7);
}

let customMetricsFetcher = null;
function setCoturnMetricsFetcher(fn) {
  customMetricsFetcher = typeof fn === 'function' ? fn : null;
}

/**
 * Parses Prometheus text exposition from Coturn and extracts cumulative relay bytes.
 */
function parseCoturnPrometheusBytes(metricsText) {
  if (typeof metricsText !== 'string' || !metricsText.trim()) return null;
  let totalBytes = 0;
  let foundAny = false;
  const lines = metricsText.split(/\r?\n/);
  for (const raw of lines) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    // Match turn_traffic_rcvb, turn_traffic_sentb, turn_total_traffic_bytes, etc.
    const match = line.match(
      /^(turn_traffic_rcvb|turn_traffic_sentb|turn_traffic_rcv_bytes|turn_traffic_sent_bytes|turn_total_traffic_bytes)(?:\{[^}]*\})?\s+([0-9eE.+_-]+)/
    );
    if (match) {
      const val = Number(match[2]);
      if (Number.isFinite(val) && val >= 0) {
        totalBytes += val;
        foundAny = true;
      }
    }
  }
  return foundAny ? Math.round(totalBytes) : null;
}

async function getOrCreateMonthLedger(monthKey = getMonthKey()) {
  const cfg = getRtcConfig();
  try {
    return await CallBudgetLedger.findOneAndUpdate(
      { monthKey },
      { $setOnInsert: { monthKey, monthlyBudgetUsd: cfg.monthlyBudgetUsd } },
      { upsert: true, new: true }
    );
  } catch (err) {
    if (err && err.code === 11000) {
      return CallBudgetLedger.findOne({ monthKey });
    }
    throw err;
  }
}

/**
 * Safely reconciles Coturn Prometheus cumulative byte counters, handling Coturn restarts
 * (counter resets when currentRaw < lastCoturnRawBytes) without ever subtracting or producing negative deltas.
 */
async function syncCoturnMetrics(ledgerArg = null) {
  const cfg = getRtcConfig();
  const ledger = ledgerArg || (await getOrCreateMonthLedger());
  if (!customMetricsFetcher && !cfg.coturnMetricsUrl) {
    return ledger;
  }

  try {
    let rawText = null;
    if (customMetricsFetcher) {
      rawText = await customMetricsFetcher();
    } else if (cfg.coturnMetricsUrl) {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 3000);
      try {
        const res = await fetch(cfg.coturnMetricsUrl, { signal: controller.signal });
        if (res.ok) {
          rawText = await res.text();
        }
      } finally {
        clearTimeout(t);
      }
    }

    const currentRawBytes = parseCoturnPrometheusBytes(rawText);
    if (currentRawBytes === null) {
      return ledger;
    }

    const prevRaw = ledger.lastCoturnRawBytes || 0;
    let deltaBytes = 0;
    if (currentRawBytes >= prevRaw) {
      deltaBytes = currentRawBytes - prevRaw;
    } else {
      // Coturn restarted and Prometheus counter reset back to 0
      deltaBytes = currentRawBytes;
      ledger.coturnRestartCount = (ledger.coturnRestartCount || 0) + 1;
    }

    ledger.lastCoturnRawBytes = currentRawBytes;
    ledger.turnBytesTotal = (ledger.turnBytesTotal || 0) + Math.max(0, deltaBytes);
    ledger.lastScrapedAt = new Date();
    recomputeLedgerCost(ledger, cfg);
    await ledger.save();
    await evaluateBudgetThresholds(ledger, cfg);
  } catch {
    // Non-fatal if Coturn metrics endpoint temporarily unreachable
  }

  return ledger;
}

function recomputeLedgerCost(ledger, cfg = getRtcConfig()) {
  const turnGb = (ledger.turnBytesTotal || 0) / (1024 * 1024 * 1024);
  const turnUsd = turnGb * cfg.turnCostPerGbUsd;
  const durationUsd = (ledger.totalBilledMinutes || 0) * cfg.costPerMinuteUsd;
  ledger.monthlyBudgetUsd = cfg.monthlyBudgetUsd;
  ledger.estimatedCostUsd = Number((turnUsd + durationUsd).toFixed(6));
  return ledger.estimatedCostUsd;
}

async function evaluateBudgetThresholds(ledger, cfg = getRtcConfig()) {
  const budget = Math.max(1, cfg.monthlyBudgetUsd);
  const pct = (ledger.estimatedCostUsd / budget) * 100;
  const thresholds = [cfg.alert1Pct, cfg.alert2Pct, 100];
  const newlyCrossed = thresholds.filter(
    (t) => pct >= t && !ledger.alertsTriggered.includes(t)
  );
  if (newlyCrossed.length === 0) return newlyCrossed;

  ledger.alertsTriggered.push(...newlyCrossed);
  if (pct >= 100 && !ledger.blockedNewCallsAt) {
    ledger.blockedNewCallsAt = new Date();
  }
  await ledger.save();

  const admins = await User.find({ role: 'ADMIN' }).select('_id preferredLanguage').limit(25);
  if (admins.length === 0) return newlyCrossed;

  const highest = Math.max(...newlyCrossed);
  const isBlocked = highest >= 100;
  const title = isBlocked
    ? 'إيقاف بدء المكالمات الجديدة (100% من الميزانية)'
    : `تنبيه استهلاك المكالمات (${highest}% من الميزانية الشهرية)`;
  const body = isBlocked
    ? `بلغ استهلاك مكالمات TURN/WebRTC الشهرية ${pct.toFixed(1)}% ($${ledger.estimatedCostUsd.toFixed(2)} من $${budget.toFixed(2)}). تم منع المكالمات الجديدة فقط مع استمرار المكالمات الجارية.`
    : `بلغ استهلاك المكالمات الشهرية ${pct.toFixed(1)}% ($${ledger.estimatedCostUsd.toFixed(2)} من $${budget.toFixed(2)}).`;

  await Notification.insertMany(
    admins.map((a) => ({
      recipient: a._id,
      title,
      message: body,
      type: 'RTC_BUDGET_ALERT',
      language: a.preferredLanguage || 'ar',
    }))
  ).catch(() => {});

  await Promise.all(
    admins.map((a) =>
      sendDataPushToUser({
        userId: a._id,
        highPriority: true,
        data: {
          type: 'rtc_budget_alert',
          thresholdPct: String(highest),
          usagePct: pct.toFixed(1),
          monthKey: ledger.monthKey,
        },
        notification: { title, body },
      })
    )
  );

  return newlyCrossed;
}

async function checkCanAuthorizeNewCall(user) {
  const cfg = getRtcConfig();
  const ledger = await syncCoturnMetrics(await getOrCreateMonthLedger());
  recomputeLedgerCost(ledger, cfg);
  await evaluateBudgetThresholds(ledger, cfg);

  const pct = (ledger.estimatedCostUsd / cfg.monthlyBudgetUsd) * 100;
  if (pct >= 100) {
    return {
      allowed: false,
      code: 'RTC_MONTHLY_BUDGET_EXCEEDED',
      message:
        'تم بلوغ الحد الشهري لميزانية المكالمات (100%). لا يمكن بدء مكالمات جديدة حالياً؛ المكالمات الجارية لن تنقطع.',
      ledger,
      usagePct: pct,
    };
  }

  return {
    allowed: true,
    ledger,
    usagePct: pct,
    dailyUserLimitMinutes:
      (user && user.dailyCallLimitMinutes > 0 ? user.dailyCallLimitMinutes : cfg.dailyUserLimitMinutes) || 0,
  };
}

async function closeExpiredActiveCalls(options = {}) {
  const cfg = getRtcConfig();
  const maxDurSec = Math.max(60, Number(options.maxDurationSeconds) || cfg.maxCallDurationSeconds);
  const nowDate = options.now instanceof Date ? options.now : new Date();
  const cutoff = new Date(nowDate.getTime() - maxDurSec * 1000);

  const activeFilter = {
    status: { $in: ['ACCEPTED', 'ACTIVE'] },
    acceptedAt: { $ne: null, $lte: cutoff },
  };
  if (!options.ignoreUpdatedAt) {
    activeFilter.updatedAt = { $lte: cutoff };
  }

  const expiredActive = await CallRecord.find(activeFilter).limit(100);
  let closedCount = 0;

  for (const record of expiredActive) {
    const endTime = new Date(record.acceptedAt.getTime() + maxDurSec * 1000);
    const billedMinutes = Math.ceil(maxDurSec / 60);
    record.status = 'ENDED';
    record.endedAt = endTime;
    record.endReason = 'MAX_DURATION';
    record.billedDurationSeconds = maxDurSec;
    record.billedMinutes = billedMinutes;
    record.estimatedCostUsd = Number((billedMinutes * cfg.costPerMinuteUsd).toFixed(6));
    if (!record.processedEventIds.includes(`${record.callId}:MAX_DURATION`)) {
      record.processedEventIds.push(`${record.callId}:MAX_DURATION`);
    }
    await record.save();

    const ledger = await getOrCreateMonthLedger(record.billingMonthKey);
    ledger.totalBilledMinutes += billedMinutes;
    recomputeLedgerCost(ledger, cfg);
    await ledger.save();
    await evaluateBudgetThresholds(ledger, cfg);

    await Promise.all([
      sendDataPushToUser({
        userId: record.callerId,
        highPriority: true,
        data: { type: 'call_ended', callId: record.callId, reason: 'MAX_DURATION' },
      }),
      sendDataPushToUser({
        userId: record.calleeId,
        highPriority: true,
        data: { type: 'call_ended', callId: record.callId, reason: 'MAX_DURATION' },
      }),
    ]).catch(() => {});

    closedCount += 1;
  }

  // Also sweep abandoned AUTHORIZED/RINGING records older than maxDurSec (0 billed minutes)
  const stalePreFilter = {
    status: { $in: ['AUTHORIZED', 'RINGING'] },
    authorizedAt: { $lte: cutoff },
  };
  if (!options.ignoreUpdatedAt) {
    stalePreFilter.updatedAt = { $lte: cutoff };
  }
  const staleRes = await CallRecord.updateMany(stalePreFilter, {
    $set: {
      status: 'MISSED',
      endedAt: nowDate,
      endReason: 'MAX_DURATION',
      billedDurationSeconds: 0,
      billedMinutes: 0,
      estimatedCostUsd: 0,
    },
  });

  return {
    closedActive: closedCount,
    closedPreAnswer: staleRes?.modifiedCount || 0,
  };
}

module.exports = {
  getRtcConfig,
  getMonthKey,
  setCoturnMetricsFetcher,
  parseCoturnPrometheusBytes,
  getOrCreateMonthLedger,
  syncCoturnMetrics,
  recomputeLedgerCost,
  evaluateBudgetThresholds,
  checkCanAuthorizeNewCall,
  closeExpiredActiveCalls,
};
