const AiUsageLedger = require('../models/AiUsageLedger');

const ALLOWED_GEMINI_MODELS = new Set([
  'gemini-2.5-flash',
  'gemini-2.5-pro',
  'gemini-2.5-flash-lite',
  'gemini-2.0-flash',
  'gemini-1.5-flash',
  'gemini-1.5-pro',
  'gemini-3.1-pro-preview',
  'gemini-3.1-flash-lite-preview',
  'gemini-3.5-flash',
]);

const DEFAULT_SYSTEM_PROMPT =
  'أنت المحاسب والمستشار المالي الذكي المعتمد لمنصة OOZOX، وخبير استراتيجيات تداول الذهب، والتحوط من التضخم، ومحاسبة تكاليف المقاولات وإدارة الديون والمقاصة. قدم إجابات دقيقة ومختصرة وواضحة.';

function getGeminiConfig() {
  const apiKey = String(process.env.GEMINI_API_KEY || '').trim();
  const envDefaultModel = String(process.env.GEMINI_MODEL || 'gemini-2.5-flash').trim();
  const defaultModel = ALLOWED_GEMINI_MODELS.has(envDefaultModel) ? envDefaultModel : 'gemini-2.5-flash';
  const userDailyLimit = Math.max(1, Number(process.env.GEMINI_USER_DAILY_LIMIT) || 30);
  const userMonthlyLimit = Math.max(userDailyLimit, Number(process.env.GEMINI_USER_MONTHLY_LIMIT) || 300);
  const monthlyBudgetUsd = Math.max(0.001, Number(process.env.GEMINI_MONTHLY_BUDGET_USD) || 50);
  const timeoutMs = Math.max(5000, Number(process.env.GEMINI_TIMEOUT_MS) || 30000);

  return {
    apiKey,
    defaultModel,
    userDailyLimit,
    userMonthlyLimit,
    monthlyBudgetUsd,
    timeoutMs,
  };
}

function resolveSafeModel(requestedModel, useThinkingMode, defaultModel) {
  if (useThinkingMode) {
    return ALLOWED_GEMINI_MODELS.has('gemini-2.5-pro') ? 'gemini-2.5-pro' : defaultModel;
  }
  const clean = String(requestedModel || '').trim();
  if (clean && ALLOWED_GEMINI_MODELS.has(clean)) {
    return clean;
  }
  return defaultModel;
}

function getPeriodKeys(now = new Date()) {
  const iso = now.toISOString();
  return {
    dayKey: iso.slice(0, 10),
    monthKey: iso.slice(0, 7),
  };
}

function estimateTokenCostUsd(model, inputTokens, outputTokens) {
  const isPro = String(model).toLowerCase().includes('pro');
  const inRate = isPro ? 0.0000025 : 0.00000035;
  const outRate = isPro ? 0.00001 : 0.00000105;
  const cost = inputTokens * inRate + outputTokens * outRate;
  return Number(Math.max(0.000001, cost).toFixed(6));
}

async function ensureLedgerDoc(scopeKey, scopeType, periodKey, userId = null) {
  try {
    await AiUsageLedger.updateOne(
      { scopeKey },
      {
        $setOnInsert: {
          scopeKey,
          scopeType,
          periodKey,
          userId,
          requestsCount: 0,
          inputTokens: 0,
          outputTokens: 0,
          estimatedCostUsd: 0,
        },
      },
      { upsert: true }
    );
  } catch (err) {
    // Ignore duplicate key race on concurrent upsert
    if (err && err.code !== 11000) {
      throw err;
    }
  }
}

function buildLimitError(statusCode, code, message, extra = {}) {
  const err = new Error(message);
  err.statusCode = statusCode;
  err.code = code;
  Object.assign(err, extra);
  return err;
}

async function reserveAiQuota(userId, config, now = new Date()) {
  const { dayKey, monthKey } = getPeriodKeys(now);
  const uid = String(userId);
  const userDailyKey = `USER:${uid}:DAILY:${dayKey}`;
  const userMonthlyKey = `USER:${uid}:MONTHLY:${monthKey}`;
  const globalMonthKey = `GLOBAL:${monthKey}`;

  await Promise.all([
    ensureLedgerDoc(userDailyKey, 'USER_DAILY', dayKey, userId),
    ensureLedgerDoc(userMonthlyKey, 'USER_MONTHLY', monthKey, userId),
    ensureLedgerDoc(globalMonthKey, 'GLOBAL_MONTH', monthKey, null),
  ]);

  const dailyDoc = await AiUsageLedger.findOneAndUpdate(
    { scopeKey: userDailyKey, requestsCount: { $lt: config.userDailyLimit } },
    { $inc: { requestsCount: 1 }, $set: { lastRequestAt: now } },
    { new: true }
  );
  if (!dailyDoc) {
    throw buildLimitError(
      429,
      'AI_DAILY_QUOTA_EXCEEDED',
      'Daily AI request limit reached for your account. Please try again tomorrow.'
    );
  }

  const monthlyDoc = await AiUsageLedger.findOneAndUpdate(
    { scopeKey: userMonthlyKey, requestsCount: { $lt: config.userMonthlyLimit } },
    { $inc: { requestsCount: 1 }, $set: { lastRequestAt: now } },
    { new: true }
  );
  if (!monthlyDoc) {
    await AiUsageLedger.updateOne({ scopeKey: userDailyKey }, { $inc: { requestsCount: -1 } });
    throw buildLimitError(
      429,
      'AI_MONTHLY_QUOTA_EXCEEDED',
      'Monthly AI request limit reached for your account.'
    );
  }

  const globalDoc = await AiUsageLedger.findOneAndUpdate(
    { scopeKey: globalMonthKey, estimatedCostUsd: { $lt: config.monthlyBudgetUsd } },
    { $inc: { requestsCount: 1 }, $set: { lastRequestAt: now } },
    { new: true }
  );
  if (!globalDoc) {
    await Promise.all([
      AiUsageLedger.updateOne({ scopeKey: userDailyKey }, { $inc: { requestsCount: -1 } }),
      AiUsageLedger.updateOne({ scopeKey: userMonthlyKey }, { $inc: { requestsCount: -1 } }),
    ]);
    throw buildLimitError(
      429,
      'AI_MONTHLY_BUDGET_EXCEEDED',
      'Platform monthly AI budget cap has been reached.'
    );
  }

  return {
    userDailyKey,
    userMonthlyKey,
    globalMonthKey,
    dailyUsed: dailyDoc.requestsCount,
    monthlyUsed: monthlyDoc.requestsCount,
  };
}

async function rollbackReservation(keys) {
  if (!keys) return;
  await Promise.all([
    AiUsageLedger.updateOne(
      { scopeKey: keys.userDailyKey, requestsCount: { $gt: 0 } },
      { $inc: { requestsCount: -1 } }
    ),
    AiUsageLedger.updateOne(
      { scopeKey: keys.userMonthlyKey, requestsCount: { $gt: 0 } },
      { $inc: { requestsCount: -1 } }
    ),
    AiUsageLedger.updateOne(
      { scopeKey: keys.globalMonthKey, requestsCount: { $gt: 0 } },
      { $inc: { requestsCount: -1 } }
    ),
  ]).catch(() => {});
}

async function recordUsageMetrics(keys, model, inputTokens, outputTokens) {
  const costUsd = estimateTokenCostUsd(model, inputTokens, outputTokens);
  const incPayload = {
    inputTokens,
    outputTokens,
    estimatedCostUsd: costUsd,
  };
  await Promise.all([
    AiUsageLedger.updateOne({ scopeKey: keys.userDailyKey }, { $inc: incPayload }),
    AiUsageLedger.updateOne({ scopeKey: keys.userMonthlyKey }, { $inc: incPayload }),
    AiUsageLedger.updateOne({ scopeKey: keys.globalMonthKey }, { $inc: incPayload }),
  ]);
  return costUsd;
}

function extractGeminiText(payload) {
  const candidates = Array.isArray(payload?.candidates) ? payload.candidates : [];
  if (!candidates.length) return '';
  const parts = Array.isArray(candidates[0]?.content?.parts) ? candidates[0].content.parts : [];
  return parts
    .map((p) => (typeof p?.text === 'string' ? p.text : ''))
    .join('')
    .trim();
}

async function invokeGeminiUpstream({ apiKey, model, requestBody, timeoutMs }) {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-goog-api-key': apiKey,
      },
      body: JSON.stringify(requestBody),
      signal: controller.signal,
    });

    if (!response.ok) {
      // Never log or reflect request/prompt details on upstream error
      throw buildLimitError(
        502,
        'AI_UPSTREAM_ERROR',
        `AI provider request failed with HTTP ${response.status}`
      );
    }

    return await response.json();
  } catch (err) {
    if (err.name === 'AbortError') {
      throw buildLimitError(504, 'AI_UPSTREAM_TIMEOUT', 'AI provider timed out');
    }
    if (err.statusCode) {
      throw err;
    }
    throw buildLimitError(502, 'AI_UPSTREAM_UNREACHABLE', 'Unable to reach AI provider');
  } finally {
    clearTimeout(timer);
  }
}

async function generateChat({
  userId,
  prompt,
  contextInfo = '',
  systemPrompt = '',
  model = '',
  useThinkingMode = false,
  mode = 'CHAT',
  targetLanguage = 'auto',
}) {
  const config = getGeminiConfig();
  if (!config.apiKey) {
    throw buildLimitError(
      503,
      'AI_NOT_CONFIGURED',
      'Server AI service is not configured (GEMINI_API_KEY missing).'
    );
  }

  const resolvedModel = resolveSafeModel(model, useThinkingMode, config.defaultModel);
  const reservation = await reserveAiQuota(userId, config);

  let effectiveSystem = String(systemPrompt || '').trim() || DEFAULT_SYSTEM_PROMPT;
  let combinedText = '';

  if (mode === 'TRANSLATE') {
    const hasArabic = /[\u0600-\u06FF]/.test(prompt);
    const resolvedTarget =
      targetLanguage === 'auto' ? (hasArabic ? 'en' : 'ar') : String(targetLanguage || 'ar');
    const targetLangLabel =
      resolvedTarget === 'ar'
        ? 'اللغة العربية الفصحى الطبيعية المعاصرة'
        : resolvedTarget === 'en'
          ? 'Fluent natural English'
          : resolvedTarget;
    effectiveSystem =
      `أنت خبير الترجمة الفورية والسياقية الاحترافية لمنصة OOZOX. ترجم الرسالة بدقة وفهم للسياق بأسلوب طبيعي 100% إلى اللغة: ${targetLangLabel}. أعد النص المترجم فقط دون مقدمات أو شروحات أو علامات اقتباس.`;
    const ctxSnippet = contextInfo ? `[سياق المحادثة]:\n${contextInfo}\n\n` : '';
    combinedText = `${effectiveSystem}\n\n${ctxSnippet}[الرسالة المطلوب ترجمتها]:\n${prompt}`;
  } else {
    const ctxSnippet = contextInfo ? `[سياق البيانات المالية]:\n${contextInfo}\n\n` : '';
    combinedText = `${effectiveSystem}\n\n${ctxSnippet}[طلب المستخدم]:\n${prompt}`;
  }

  const requestBody = {
    contents: [
      {
        parts: [{ text: combinedText }],
      },
    ],
  };

  if (useThinkingMode && resolvedModel.includes('pro')) {
    requestBody.generationConfig = {
      thinkingConfig: { thinkingBudget: 2048 },
    };
  }

  try {
    const data = await invokeGeminiUpstream({
      apiKey: config.apiKey,
      model: resolvedModel,
      requestBody,
      timeoutMs: config.timeoutMs,
    });

    const text = extractGeminiText(data);
    if (!text) {
      await rollbackReservation(reservation);
      throw buildLimitError(502, 'AI_EMPTY_RESPONSE', 'AI returned an empty response');
    }

    const usage = data?.usageMetadata || {};
    const inputTokens = Math.max(
      1,
      Number(usage.promptTokenCount) || Math.ceil(combinedText.length / 4)
    );
    const outputTokens = Math.max(
      1,
      Number(usage.candidatesTokenCount) || Math.ceil(text.length / 4)
    );
    await recordUsageMetrics(reservation, resolvedModel, inputTokens, outputTokens);

    return {
      text,
      model: resolvedModel,
      quota: {
        dailyUsed: reservation.dailyUsed,
        dailyLimit: config.userDailyLimit,
        monthlyUsed: reservation.monthlyUsed,
        monthlyLimit: config.userMonthlyLimit,
      },
    };
  } catch (err) {
    if (err.code !== 'AI_EMPTY_RESPONSE') {
      await rollbackReservation(reservation);
    }
    throw err;
  }
}

async function generateVision({
  userId,
  prompt,
  imageBase64,
  mimeType = 'image/jpeg',
  model = '',
  systemPrompt = '',
}) {
  const config = getGeminiConfig();
  if (!config.apiKey) {
    throw buildLimitError(
      503,
      'AI_NOT_CONFIGURED',
      'Server AI service is not configured (GEMINI_API_KEY missing).'
    );
  }

  const resolvedModel = resolveSafeModel(model, false, config.defaultModel);
  const reservation = await reserveAiQuota(userId, config);
  const effectiveSystem = String(systemPrompt || '').trim() || DEFAULT_SYSTEM_PROMPT;
  const fullText = `${effectiveSystem}\n\n${prompt}`;

  const requestBody = {
    contents: [
      {
        parts: [
          { text: fullText },
          {
            inlineData: {
              mimeType,
              data: imageBase64,
            },
          },
        ],
      },
    ],
  };

  try {
    const data = await invokeGeminiUpstream({
      apiKey: config.apiKey,
      model: resolvedModel,
      requestBody,
      timeoutMs: config.timeoutMs,
    });

    const text = extractGeminiText(data);
    if (!text) {
      await rollbackReservation(reservation);
      throw buildLimitError(502, 'AI_EMPTY_RESPONSE', 'AI returned an empty response');
    }

    const usage = data?.usageMetadata || {};
    const inputTokens = Math.max(
      1,
      Number(usage.promptTokenCount) || Math.ceil((fullText.length + 1024) / 4)
    );
    const outputTokens = Math.max(
      1,
      Number(usage.candidatesTokenCount) || Math.ceil(text.length / 4)
    );
    await recordUsageMetrics(reservation, resolvedModel, inputTokens, outputTokens);

    return {
      text,
      model: resolvedModel,
      quota: {
        dailyUsed: reservation.dailyUsed,
        dailyLimit: config.userDailyLimit,
        monthlyUsed: reservation.monthlyUsed,
        monthlyLimit: config.userMonthlyLimit,
      },
    };
  } catch (err) {
    if (err.code !== 'AI_EMPTY_RESPONSE') {
      await rollbackReservation(reservation);
    }
    throw err;
  }
}

async function getUserAiQuotaStatus(userId) {
  const config = getGeminiConfig();
  const { dayKey, monthKey } = getPeriodKeys();
  const uid = String(userId);
  const [dailyDoc, monthlyDoc, globalDoc] = await Promise.all([
    AiUsageLedger.findOne({ scopeKey: `USER:${uid}:DAILY:${dayKey}` }).lean(),
    AiUsageLedger.findOne({ scopeKey: `USER:${uid}:MONTHLY:${monthKey}` }).lean(),
    AiUsageLedger.findOne({ scopeKey: `GLOBAL:${monthKey}` }).lean(),
  ]);

  const globalCostUsd = Number(globalDoc?.estimatedCostUsd || 0);
  return {
    configured: Boolean(config.apiKey),
    defaultModel: config.defaultModel,
    userDailyUsed: Number(dailyDoc?.requestsCount || 0),
    userDailyLimit: config.userDailyLimit,
    userMonthlyUsed: Number(monthlyDoc?.requestsCount || 0),
    userMonthlyLimit: config.userMonthlyLimit,
    budgetExceeded: globalCostUsd >= config.monthlyBudgetUsd,
  };
}

module.exports = {
  ALLOWED_GEMINI_MODELS,
  getGeminiConfig,
  generateChat,
  generateVision,
  getUserAiQuotaStatus,
};
