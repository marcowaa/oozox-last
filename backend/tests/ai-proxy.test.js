const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const AiUsageLedger = require('../src/models/AiUsageLedger');
const OtpAbuseLedger = require('../src/models/OtpAbuseLedger');
const { ensureSafeUserEmailPartialIndex } = require('../src/services/migrationService');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;
const originalFetch = global.fetch;

async function otpLogin({ phone, deviceId }) {
  await OtpAbuseLedger.updateMany({}, { $set: { lastSentAt: new Date(Date.now() - 20000) } });
  const reqRes = await request(app).post('/api/auth/otp/request').send({ phone, deviceId });
  const verRes = await request(app).post('/api/auth/otp/verify').send({
    challengeId: reqRes.body.data.challengeId,
    code: '667788',
    deviceId,
  });
  return verRes.body.data;
}

function mockGeminiSuccess({ text = 'تحليل مالي مختصر', promptTokenCount = 100, candidatesTokenCount = 50 } = {}) {
  return jest.fn(async (_url, _opts) => ({
    ok: true,
    status: 200,
    json: async () => ({
      candidates: [{ content: { parts: [{ text }] } }],
      usageMetadata: { promptTokenCount, candidatesTokenCount },
    }),
  }));
}

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({ binary: { version: '7.0.14' } });
  await mongoose.connect(mongoServer.getUri());
  await ensureSafeUserEmailPartialIndex();
  installMockOtpAdapters({ fixedCode: '667788' });
});

beforeEach(async () => {
  process.env.GEMINI_API_KEY = 'test-server-gemini-secret-key';
  process.env.GEMINI_MODEL = 'gemini-2.5-flash';
  process.env.GEMINI_USER_DAILY_LIMIT = '3';
  process.env.GEMINI_USER_MONTHLY_LIMIT = '10';
  process.env.GEMINI_MONTHLY_BUDGET_USD = '50';

  await Promise.all([
    User.deleteMany({}),
    AiUsageLedger.deleteMany({}),
    OtpAbuseLedger.deleteMany({}),
  ]);
});

afterEach(() => {
  global.fetch = originalFetch;
  jest.restoreAllMocks();
});

afterAll(async () => {
  global.fetch = originalFetch;
  resetOtpAdapters();
  await mongoose.connection.close();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('Server-Side Gemini AI Proxy (Quotas, Monthly Cost Cap, Zero Prompt Logging)', () => {
  it('1. Rejects unauthenticated requests and returns 503 when GEMINI_API_KEY is missing', async () => {
    const unauth = await request(app).post('/api/ai/chat').send({ prompt: 'Hello' });
    expect(unauth.statusCode).toBe(401);

    const user = await otpLogin({ phone: '+201077770001', deviceId: 'ai_dev_1' });
    process.env.GEMINI_API_KEY = '';

    const res503 = await request(app)
      .post('/api/ai/chat')
      .set('Authorization', `Bearer ${user.token}`)
      .send({ prompt: 'What is 21k gold outlook?' });
    expect(res503.statusCode).toBe(503);
    expect(res503.body.code).toBe('AI_NOT_CONFIGURED');
  });

  it('2. Sends x-goog-api-key header (never in URL), enforces per-user daily quota, and NEVER logs prompts', async () => {
    const user = await otpLogin({ phone: '+201077770002', deviceId: 'ai_dev_2' });
    const fetchMock = mockGeminiSuccess({ text: 'السوق مستقر اليوم.' });
    global.fetch = fetchMock;

    const logSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
    const errSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

    const secretMarker = 'CONFIDENTIAL_SALARY_LEDGER_XYZ_9911';
    for (let i = 0; i < 3; i++) {
      const res = await request(app)
        .post('/api/ai/chat')
        .set('Authorization', `Bearer ${user.token}`)
        .send({ prompt: `${secretMarker} request ${i}`, mode: 'ACCOUNTANT' });
      expect(res.statusCode).toBe(200);
      expect(res.body.data.text).toBe('السوق مستقر اليوم.');
    }

    // Verify API key was sent ONLY in header, never in URL
    expect(fetchMock).toHaveBeenCalledTimes(3);
    const [calledUrl, calledOpts] = fetchMock.mock.calls[0];
    expect(calledUrl).not.toContain('key=');
    expect(calledUrl).not.toContain('test-server-gemini-secret-key');
    expect(calledOpts.headers['x-goog-api-key']).toBe('test-server-gemini-secret-key');

    // 4th request exceeds per-user daily limit (=3)
    const blockedDaily = await request(app)
      .post('/api/ai/chat')
      .set('Authorization', `Bearer ${user.token}`)
      .send({ prompt: secretMarker });
    expect(blockedDaily.statusCode).toBe(429);
    expect(blockedDaily.body.code).toBe('AI_DAILY_QUOTA_EXCEEDED');

    // Verify prompt content was NEVER written to any log
    const allLogs = [...logSpy.mock.calls, ...errSpy.mock.calls, ...warnSpy.mock.calls]
      .flat()
      .map(String)
      .join(' ');
    expect(allLogs).not.toContain(secretMarker);
  });

  it('3. Enforces per-user monthly quota across days and global monthly cost cap across users', async () => {
    process.env.GEMINI_USER_DAILY_LIMIT = '10';
    process.env.GEMINI_USER_MONTHLY_LIMIT = '2';
    process.env.GEMINI_MONTHLY_BUDGET_USD = '0.01';

    const userA = await otpLogin({ phone: '+201077770003', deviceId: 'ai_dev_3' });
    const userB = await otpLogin({ phone: '+201077770004', deviceId: 'ai_dev_4' });

    // Large token count pushes global cost over $0.01 on flash pricing (~$0.014)
    global.fetch = mockGeminiSuccess({
      text: 'OK',
      promptTokenCount: 20000,
      candidatesTokenCount: 10000,
    });

    const first = await request(app)
      .post('/api/ai/chat')
      .set('Authorization', `Bearer ${userA.token}`)
      .send({ prompt: 'First global budget check' });
    expect(first.statusCode).toBe(200);

    // Second user hits global monthly cost cap immediately
    const globalBlocked = await request(app)
      .post('/api/ai/chat')
      .set('Authorization', `Bearer ${userB.token}`)
      .send({ prompt: 'Should fail due to global cost cap' });
    expect(globalBlocked.statusCode).toBe(429);
    expect(globalBlocked.body.code).toBe('AI_MONTHLY_BUDGET_EXCEEDED');

    // Raise global budget so userA can hit their per-user monthly limit (=2)
    process.env.GEMINI_MONTHLY_BUDGET_USD = '100';
    const second = await request(app)
      .post('/api/ai/chat')
      .set('Authorization', `Bearer ${userA.token}`)
      .send({ prompt: 'Second request for userA' });
    expect(second.statusCode).toBe(200);

    const thirdMonthlyBlocked = await request(app)
      .post('/api/ai/chat')
      .set('Authorization', `Bearer ${userA.token}`)
      .send({ prompt: 'Third request exceeds monthly cap' });
    expect(thirdMonthlyBlocked.statusCode).toBe(429);
    expect(thirdMonthlyBlocked.body.code).toBe('AI_MONTHLY_QUOTA_EXCEEDED');
  });
});
