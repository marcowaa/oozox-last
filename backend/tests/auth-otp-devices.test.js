const crypto = require('crypto');
const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const OtpChallenge = require('../src/models/OtpChallenge');
const OtpAbuseLedger = require('../src/models/OtpAbuseLedger');
const TrustedDevice = require('../src/models/TrustedDevice');
const AuthSession = require('../src/models/AuthSession');
const Notification = require('../src/models/Notification');
const { migrateLegacyUsers, ensureSafeUserEmailPartialIndex } = require('../src/services/migrationService');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;
let otpState;

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({ binary: { version: '7.0.14' } });
  await mongoose.connect(mongoServer.getUri());
  await ensureSafeUserEmailPartialIndex();
});

beforeEach(async () => {
  process.env.OTP_RESEND_COOLDOWN_SECONDS = '15';
  process.env.OTP_DAILY_MAX_PER_PHONE = '5';
  process.env.OTP_MAX_ATTEMPTS = '5';
  process.env.OTP_MONTHLY_BUDGET_USD = '100';
  await Promise.all([
    User.deleteMany({}),
    OtpChallenge.deleteMany({}),
    OtpAbuseLedger.deleteMany({}),
    TrustedDevice.deleteMany({}),
    AuthSession.deleteMany({}),
    Notification.deleteMany({}),
  ]);
  otpState = installMockOtpAdapters({ fixedCode: '246810' });
});

afterEach(() => {
  resetOtpAdapters();
});

afterAll(async () => {
  await mongoose.connection.close();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('Phone-Only OTP Auth, Provider Fallback, Email Preservation, Admin Gate & Device Trust (30d)', () => {
  it('1. Auto-provisions phone-only users without email/password, blocks PRE_AUTH, and issues 30d device trust', async () => {
    const reqRes = await request(app).post('/api/auth/otp/request').send({
      phone: '01012345678',
      channel: 'AUTO',
      deviceId: 'dev_pixel_1',
      deviceName: 'Pixel 9 Pro',
    });

    expect(reqRes.statusCode).toBe(200);
    expect(reqRes.body.data.activeChannel).toBe('TELEGRAM');
    expect(reqRes.body.data.isExistingUser).toBe(false);

    const storedChallenge = await OtpChallenge.findOne({
      challengeId: reqRes.body.data.challengeId,
    }).select('+codeHash +codeSalt');
    expect(storedChallenge.providerType).toBe('TELEGRAM_GATEWAY');
    expect(storedChallenge.codeHash).toBeTruthy();

    // Wrong code decrements remaining attempts
    const badVerify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqRes.body.data.challengeId,
      code: '000000',
      deviceId: 'dev_pixel_1',
    });
    expect(badVerify.statusCode).toBe(401);
    expect(badVerify.body.remainingAttempts).toBe(4);

    // Correct code creates user, activates session & issues 30-day TrustedDevice
    const okVerify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqRes.body.data.challengeId,
      code: '246810',
      deviceId: 'dev_pixel_1',
      deviceName: 'Pixel 9 Pro',
      hardwareFingerprint: 'fp_pixel_1',
    });
    expect(okVerify.statusCode).toBe(200);
    expect(okVerify.body.data.token).toBeDefined();
    expect(okVerify.body.data.refreshToken).toBeDefined();
    expect(okVerify.body.data.isExistingUser).toBe(false);
    expect(okVerify.body.data.user.isVerified).toBe(true);
    expect(okVerify.body.data.user.email).toBe('');
    expect(okVerify.body.data.trustedDevice.deviceTrustToken).toBeDefined();

    // Creating a 2nd phone-only user without email must NEVER collide on E11000
    const reqRes2 = await request(app).post('/api/auth/otp/request').send({
      phone: '01099887766',
      deviceId: 'dev_pixel_2',
    });
    const okVerify2 = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqRes2.body.data.challengeId,
      code: '246810',
      deviceId: 'dev_pixel_2',
    });
    expect(okVerify2.statusCode).toBe(200);
    expect(okVerify2.body.data.user.phoneE164).toBe('+201099887766');

    const meOk = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${okVerify.body.data.token}`);
    expect(meOk.statusCode).toBe(200);
    expect(meOk.body.data.user.phoneE164).toBe('+201012345678');
  });

  it('2. Falls back Telegram -> WhatsApp -> SMS (Twilio Verify stores NO local codeHash)', async () => {
    otpState.failChannels.add('TELEGRAM');

    const reqRes = await request(app).post('/api/auth/otp/request').send({
      phone: '+201022223333',
      deviceId: 'dev_fb_1',
    });

    expect(reqRes.statusCode).toBe(200);
    expect(reqRes.body.data.activeChannel).toBe('WHATSAPP');
    expect(reqRes.body.data.attemptedChannels.map((a) => `${a.channel}:${a.status}`)).toEqual([
      'TELEGRAM:FAILED',
      'WHATSAPP:SENT',
    ]);

    const twilioChallenge = await OtpChallenge.findOne({
      challengeId: reqRes.body.data.challengeId,
    }).select('+codeHash +codeSalt');
    expect(twilioChallenge.providerType).toBe('TWILIO_VERIFY');
    expect(twilioChallenge.codeHash).toBeNull();
    expect(twilioChallenge.codeSalt).toBeNull();

    const verifyRes = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqRes.body.data.challengeId,
      code: '246810',
      deviceId: 'dev_fb_1',
    });
    expect(verifyRes.statusCode).toBe(200);
    expect(otpState.verifiedChecks.length).toBe(1);
  });

  it('3. Supersedes old Telegram OTP & revokes it on provider when resending / switching channel', async () => {
    const firstReq = await request(app).post('/api/auth/otp/request').send({
      phone: '+201033334444',
      channel: 'TELEGRAM',
      deviceId: 'dev_sw_1',
    });
    const firstChallengeId = firstReq.body.data.challengeId;

    await OtpAbuseLedger.updateMany({}, { $set: { lastSentAt: new Date(Date.now() - 20000) } });

    const secondReq = await request(app).post('/api/auth/otp/request').send({
      phone: '+201033334444',
      channel: 'SMS',
      deviceId: 'dev_sw_1',
    });
    expect(secondReq.statusCode).toBe(200);
    expect(secondReq.body.data.activeChannel).toBe('SMS');
    expect(otpState.revoked.length).toBe(1);

    const oldVerify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: firstChallengeId,
      code: '246810',
    });
    expect(oldVerify.statusCode).toBe(400);
    expect(oldVerify.body.code).toBe('OTP_SUPERSEDED');
  });

  it('4. Enforces Phone + TrustedDevice biometric unlock, detects otherActiveDevices, and preserves optional profile email', async () => {
    const reqA = await request(app).post('/api/auth/otp/request').send({
      phone: '+201044445555',
      deviceId: 'dev_trusted_a',
      deviceName: 'Old Phone A',
    });
    const verA = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqA.body.data.challengeId,
      code: '246810',
      deviceId: 'dev_trusted_a',
      deviceName: 'Old Phone A',
      hardwareFingerprint: 'hw_a',
    });
    expect(verA.body.data.isExistingUser).toBe(false);
    expect(verA.body.data.hasOtherActiveDevice).toBe(false);
    const trustTokenA = verA.body.data.trustedDevice.deviceTrustToken;

    // Optional contact email saved on profile (not used for auth)
    const patchEmail = await request(app)
      .patch('/api/auth/me')
      .set('Authorization', `Bearer ${verA.body.data.token}`)
      .send({ name: 'Tariq Business', email: 'tariq@business.example' });
    expect(patchEmail.statusCode).toBe(200);
    expect(patchEmail.body.data.user.email).toBe('tariq@business.example');

    // Trusted biometric login passes on dev_trusted_a
    const trustedLogin = await request(app).post('/api/auth/trusted-login').send({
      phone: '+201044445555',
      deviceId: 'dev_trusted_a',
      deviceTrustToken: trustTokenA,
      hardwareFingerprint: 'hw_a',
    });
    expect(trustedLogin.statusCode).toBe(200);
    expect(trustedLogin.body.data.token).toBeDefined();

    // Untrusted phone B is rejected by /trusted-login and requires server OTP
    const untrustedBio = await request(app).post('/api/auth/trusted-login').send({
      phone: '+201044445555',
      deviceId: 'dev_new_b',
      deviceTrustToken: 'fake_token',
    });
    expect(untrustedBio.statusCode).toBe(401);
    expect(untrustedBio.body.requiresOtp).toBe(true);

    // Logging in on Phone B via OTP detects existing user + Old Phone A active!
    await OtpAbuseLedger.updateMany({}, { $set: { lastSentAt: new Date(Date.now() - 20000) } });
    const reqB = await request(app).post('/api/auth/otp/request').send({
      phone: '+201044445555',
      deviceId: 'dev_new_b',
      deviceName: 'New Phone B',
    });
    expect(reqB.body.data.isExistingUser).toBe(true);

    const verB = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqB.body.data.challengeId,
      code: '246810',
      deviceId: 'dev_new_b',
      deviceName: 'New Phone B',
    });
    expect(verB.statusCode).toBe(200);
    expect(verB.body.data.isExistingUser).toBe(true);
    expect(verB.body.data.hasOtherActiveDevice).toBe(true);
    expect(verB.body.data.otherActiveDevices.some((d) => d.deviceId === 'dev_trusted_a')).toBe(true);
    expect(verB.body.data.user.email).toBe('tariq@business.example');
  });

  it('5. Enforces Admin portalScope RBAC via Phone OTP and confirms obsolete email/password auth routes are removed', async () => {
    // Obsolete routes cleanly return 404
    const obsoleteReg = await request(app).post('/api/auth/register').send({ phone: '+201012345678' });
    const obsoleteLogin = await request(app).post('/api/auth/login').send({ phone: '+201012345678' });
    const obsoleteRecover = await request(app).post('/api/auth/recover').send({});
    expect(obsoleteReg.statusCode).toBe(404);
    expect(obsoleteLogin.statusCode).toBe(404);
    expect(obsoleteRecover.statusCode).toBe(404);

    // Normal USER cannot log in with portalScope: 'ADMIN'
    const userReq = await request(app).post('/api/auth/otp/request').send({ phone: '+201055556666' });
    const userAdminAttempt = await request(app).post('/api/auth/otp/verify').send({
      challengeId: userReq.body.data.challengeId,
      code: '246810',
      deviceId: 'web_admin',
      portalScope: 'ADMIN',
    });
    expect(userAdminAttempt.statusCode).toBe(403);

    // Real ADMIN user passes portalScope: 'ADMIN' via Phone OTP
    await User.findOneAndUpdate({ phoneE164: '+201055556666' }, { $set: { role: 'ADMIN' } });
    await OtpAbuseLedger.updateMany({}, { $set: { lastSentAt: new Date(Date.now() - 20000) } });
    const adminReq = await request(app).post('/api/auth/otp/request').send({ phone: '+201055556666' });
    const adminVer = await request(app).post('/api/auth/otp/verify').send({
      challengeId: adminReq.body.data.challengeId,
      code: '246810',
      deviceId: 'web_admin',
      platform: 'WEB',
      portalScope: 'ADMIN',
    });
    expect(adminVer.statusCode).toBe(200);
    expect(adminVer.body.data.user.role).toBe('ADMIN');
    expect(adminVer.body.data.token).toBeDefined();
  });

  it('6. Blocks disallowed countries & verifies Telegram Gateway webhook HMAC', async () => {
    process.env.OTP_ALLOWED_COUNTRIES = '+20,+966';

    const blockedCountry = await request(app).post('/api/auth/otp/request').send({
      phone: '+8801712345678',
    });
    expect(blockedCountry.statusCode).toBe(403);
    expect(blockedCountry.body.code).toBe('COUNTRY_NOT_ALLOWED');

    process.env.TELEGRAM_GATEWAY_TOKEN = 'test_tg_secret_token';
    const rawPayload = JSON.stringify({ request_id: 'tg_req_1', delivery_status: { status: 'delivered' } });
    const ts = String(Math.floor(Date.now() / 1000));
    const secretKey = crypto.createHash('sha256').update('test_tg_secret_token', 'utf8').digest();
    const sig = crypto
      .createHmac('sha256', secretKey)
      .update(`${ts}\n${rawPayload}`, 'utf8')
      .digest('hex');

    const badWebhook = await request(app)
      .post('/api/auth/webhooks/telegram-gateway')
      .set('X-Request-Timestamp', ts)
      .set('X-Request-Signature', '0000000000000000000000000000000000000000000000000000000000000000')
      .send(JSON.parse(rawPayload));
    expect(badWebhook.statusCode).toBe(401);

    const goodWebhook = await request(app)
      .post('/api/auth/webhooks/telegram-gateway')
      .set('X-Request-Timestamp', ts)
      .set('X-Request-Signature', sig)
      .set('Content-Type', 'application/json')
      .send(rawPayload);
    expect(goodWebhook.statusCode).toBe(200);
    expect(goodWebhook.body.ok).toBe(true);
  });

  it('7. Migrates pre-OTP legacy users idempotently while preserving their historical emails intact', async () => {
    await User.collection.insertOne({
      name: 'Legacy Merchant',
      email: 'legacy@oozox.com',
      phone: '01088887777',
      password: '$2a$12$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ012',
      role: 'MERCHANT',
      isVerified: true,
      createdAt: new Date('2025-01-01T00:00:00Z'),
    });

    const firstRun = await migrateLegacyUsers({ markExistingAsVerified: true });
    expect(firstRun.updatedCount).toBe(1);

    const migrated = await User.findOne({ email: 'legacy@oozox.com' });
    expect(migrated.isVerified).toBe(true);
    expect(migrated.email).toBe('legacy@oozox.com');
    expect(migrated.phoneE164).toBe('+201088887777');
    expect(migrated.legacyMigratedAt).toBeTruthy();

    const secondRun = await migrateLegacyUsers({ markExistingAsVerified: true });
    expect(secondRun.updatedCount).toBe(0);
  });
});
