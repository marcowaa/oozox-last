const request = require('supertest');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const User = require('../src/models/User');
const Employee = require('../src/models/Employee');
const AuthSession = require('../src/models/AuthSession');
const OtpChallenge = require('../src/models/OtpChallenge');
const OtpAbuseLedger = require('../src/models/OtpAbuseLedger');
const { protectEnterprise } = require('../src/middlewares/authMiddleware');
const { requestOtpChallenge } = require('../src/services/otp/otpOrchestrator');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');
const { getJwtSecret, generateEmployeeToken } = require('../src/utils/jwt');

describe('Step 2 Security Hardening Unit Verification (No mongod binary required)', () => {
  afterEach(() => {
    jest.restoreAllMocks();
    resetOtpAdapters();
  });

  it('1. Public /api/health returns minimal status only & /api/health/details enforces admin/internal guard', async () => {
    const prevReadyState = mongoose.connection._readyState;
    mongoose.connection._readyState = 1;
    try {
      const publicRes = await request(app).get('/api/health');
      expect(publicRes.statusCode).toBe(200);
      expect(publicRes.body.data).toEqual({ status: 'HEALTHY' });
      expect(publicRes.body.data.database).toBeUndefined();
      expect(publicRes.body.data.system).toBeUndefined();
      expect(publicRes.body.data.uptimeSeconds).toBeUndefined();

      const forwardedUnauth = await request(app)
        .get('/api/health/details')
        .set('X-Forwarded-For', '198.51.100.25');
      expect(forwardedUnauth.statusCode).toBe(401);

      // Non-admin Bearer token rejected with 403
      const userId = new mongoose.Types.ObjectId();
      jest.spyOn(User, 'findById').mockImplementation((id) => ({
        select: jest.fn().mockResolvedValue(
          String(id) === String(userId)
            ? { _id: userId, role: 'USER', isVerified: true }
            : { _id: id, role: 'ADMIN', isVerified: true }
        ),
      }));
      const userToken = jwt.sign({ id: String(userId), role: 'USER' }, getJwtSecret(), { algorithm: 'HS256' });
      const nonAdminRes = await request(app)
        .get('/api/health/details')
        .set('X-Forwarded-For', '198.51.100.25')
        .set('Authorization', `Bearer ${userToken}`);
      expect(nonAdminRes.statusCode).toBe(403);

      // Admin Bearer token succeeds with detailed payload
      const adminId = new mongoose.Types.ObjectId();
      const adminToken = jwt.sign({ id: String(adminId), role: 'ADMIN' }, getJwtSecret(), { algorithm: 'HS256' });
      const adminRes = await request(app)
        .get('/api/health/details')
        .set('X-Forwarded-For', '198.51.100.25')
        .set('Authorization', `Bearer ${adminToken}`);
      expect(adminRes.statusCode).toBe(200);
      expect(adminRes.body.data.database.status).toBe('CONNECTED');
      expect(adminRes.body.data.system.nodeVersion).toBe(process.version);
    } finally {
      mongoose.connection._readyState = prevReadyState;
    }
  });

  it('2. protectEnterprise rejects employee tokens without valid AuthSession or after revocation', async () => {
    const empId = new mongoose.Types.ObjectId();
    const ownerId = new mongoose.Types.ObjectId();
    const bizId = new mongoose.Types.ObjectId();
    const employeeDoc = {
      _id: empId,
      owner: ownerId,
      business: bizId,
      isActive: true,
      passwordChangedAt: null,
      tokenRevokedBefore: null,
    };

    jest.spyOn(Employee, 'findById').mockResolvedValue(employeeDoc);
    jest.spyOn(User, 'findById').mockReturnValue({
      select: jest.fn().mockResolvedValue({ _id: ownerId, isVerified: true }),
    });

    const invokeMiddleware = async (token) => {
      const req = { headers: { authorization: `Bearer ${token}` }, t: (k) => k };
      let statusSent = 200;
      let bodySent = null;
      const res = {
        status(code) {
          statusSent = code;
          return this;
        },
        json(payload) {
          bodySent = payload;
          return this;
        },
      };
      let nextCalled = false;
      await protectEnterprise(req, res, () => {
        nextCalled = true;
      });
      return { statusSent, bodySent, nextCalled, req };
    };

    // Missing sid -> 401
    const noSidToken = generateEmployeeToken(employeeDoc);
    const noSidRes = await invokeMiddleware(noSidToken);
    expect(noSidRes.nextCalled).toBe(false);
    expect(noSidRes.statusSent).toBe(401);

    // Revoked AuthSession -> 401
    const tokenWithSid = generateEmployeeToken(employeeDoc, 'sess-emp-1');
    jest.spyOn(AuthSession, 'findOne').mockResolvedValue({
      sessionId: 'sess-emp-1',
      employeeId: empId,
      revokedAt: new Date(),
      expiresAt: new Date(Date.now() + 60000),
    });
    const revokedRes = await invokeMiddleware(tokenWithSid);
    expect(revokedRes.nextCalled).toBe(false);
    expect(revokedRes.statusSent).toBe(401);

    // Active AuthSession + valid owner -> passes
    AuthSession.findOne.mockResolvedValue({
      sessionId: 'sess-emp-1',
      employeeId: empId,
      revokedAt: null,
      expiresAt: new Date(Date.now() + 60000),
    });
    const validRes = await invokeMiddleware(tokenWithSid);
    expect(validRes.nextCalled).toBe(true);
    expect(validRes.req.authSessionId).toBe('sess-emp-1');
    expect(validRes.req.isEmployeeActor).toBe(true);
  });

  it('3. OTP orchestrator enforces minimum 10s gap after cancel before channel switch and counts daily + cost ledgers', async () => {
    installMockOtpAdapters({ fixedCode: '554433' });
    process.env.OTP_RESEND_COOLDOWN_SECONDS = '60';
    process.env.OTP_DAILY_MAX_PER_PHONE = '5';
    process.env.OTP_MONTHLY_BUDGET_USD = '100';

    const phoneLedger = {
      key: 'PHONE_DAY:+201077778888:2026-11-28',
      kind: 'PHONE_DAY',
      sendCount: 1,
      lastSentAt: new Date(Date.now() - 3500),
      lockedUntil: null,
      save: jest.fn().mockResolvedValue(true),
    };
    const monthLedger = {
      key: 'OTP_MONTH:2026-11',
      kind: 'OTP_MONTH',
      sendCount: 1,
      twilioSendCount: 0,
      telegramSendCount: 1,
      estimatedCostUsd: 0.01,
      alertsTriggered: [],
      save: jest.fn().mockResolvedValue(true),
    };

    jest.spyOn(OtpAbuseLedger, 'findOneAndUpdate').mockImplementation((query) => {
      if (String(query.key).startsWith('PHONE_DAY:')) return Promise.resolve(phoneLedger);
      return Promise.resolve(monthLedger);
    });
    jest.spyOn(OtpChallenge, 'findOne').mockReturnValue({
      sort: jest.fn().mockReturnValue({
        select: jest.fn().mockResolvedValue({ status: 'CANCELLED', activeChannel: 'TELEGRAM' }),
      }),
    });
    jest.spyOn(OtpChallenge, 'find').mockResolvedValue([]);
    jest.spyOn(OtpChallenge, 'create').mockImplementation((doc) => Promise.resolve(doc));

    // < 10s after send -> MUST throw 429 OTP_COOLDOWN
    await expect(
      requestOtpChallenge({
        rawPhone: '+201077778888',
        purpose: 'LOGIN',
        preferredChannel: 'WHATSAPP',
      })
    ).rejects.toMatchObject({
      statusCode: 429,
      code: 'OTP_COOLDOWN',
    });

    // >= 10s after send -> switch allowed, daily sendCount and cost ledger incremented
    phoneLedger.lastSentAt = new Date(Date.now() - 10500);
    const ok = await requestOtpChallenge({
      rawPhone: '+201077778888',
      purpose: 'LOGIN',
      preferredChannel: 'WHATSAPP',
    });
    expect(ok.activeChannel).toBe('WHATSAPP');
    expect(phoneLedger.sendCount).toBe(2);
    expect(monthLedger.sendCount).toBe(2);
    expect(monthLedger.estimatedCostUsd).toBeGreaterThan(0.01);
  });

  it('4. Express validation middleware works cleanly with Zod v4 on bad payloads', async () => {
    const badRes = await request(app)
      .post('/api/auth/otp/request')
      .send({ channel: 'CARRIER_PIGEON', phone: '12' });
    expect(badRes.statusCode).toBe(400);
    expect(badRes.body.success).toBe(false);
    expect(Array.isArray(badRes.body.errors)).toBe(true);
    expect(badRes.body.errors.length).toBeGreaterThanOrEqual(1);
  });
});
