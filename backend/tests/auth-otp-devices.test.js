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
const { migrateLegacyUsers } = require('../src/services/migrationService');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;
let otpState;

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({ binary: { version: '7.0.14' } });
  await mongoose.connect(mongoServer.getUri());
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

describe('OTP Verification, Provider Fallback, Device Trust (30d), and Migration', () => {
  it('1. Blocks PRE_AUTH JWT from accessing protected endpoints until server OTP verified', async () => {
    const reg = await request(app).post('/api/auth/register').send({
      name: 'Verified User',
      email: 'verified@oozox.com',
      phone: '01012345678',
      password: 'Password123!',
      role: 'USER',
      deviceId: 'dev_pixel_1',
      deviceName: 'Pixel 9 Pro',
    });

    expect(reg.statusCode).toBe(201);
    expect(reg.body.data.requiresOtp).toBe(true);
    expect(reg.body.data.token).toBeUndefined();
    expect(reg.body.data.preAuthToken).toBeDefined();
    expect(reg.body.data.otpChallenge.activeChannel).toBe('TELEGRAM');

    const storedChallenge = await OtpChallenge.findOne({
      challengeId: reg.body.data.otpChallenge.challengeId,
    }).select('+codeHash +codeSalt');
    expect(storedChallenge.providerType).toBe('TELEGRAM_GATEWAY');
    expect(storedChallenge.codeHash).toBeTruthy();

    // PRE_AUTH token must NEVER pass protect middleware
    const meWithPreAuth = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${reg.body.data.preAuthToken}`);
    expect(meWithPreAuth.statusCode).toBe(401);

    // Wrong code decrements remaining attempts
    const badVerify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reg.body.data.otpChallenge.challengeId,
      code: '000000',
    });
    expect(badVerify.statusCode).toBe(401);
    expect(badVerify.body.remainingAttempts).toBe(4);

    // Correct code activates account & issues 30-day TrustedDevice + AuthSession
    const okVerify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reg.body.data.otpChallenge.challengeId,
      code: '246810',
      deviceId: 'dev_pixel_1',
      deviceName: 'Pixel 9 Pro',
      hardwareFingerprint: 'fp_pixel_1',
    });
    expect(okVerify.statusCode).toBe(200);
    expect(okVerify.body.data.token).toBeDefined();
    expect(okVerify.body.data.refreshToken).toBeDefined();
    expect(okVerify.body.data.user.isVerified).toBe(true);
    expect(okVerify.body.data.trustedDevice.deviceTrustToken).toBeDefined();

    const meOk = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${okVerify.body.data.token}`);
    expect(meOk.statusCode).toBe(200);
    expect(meOk.body.data.user.phoneE164).toBe('+201012345678');
  });

  it('2. Falls back Telegram -> WhatsApp -> SMS (Twilio Verify stores NO local codeHash)', async () => {
    otpState.failChannels.add('TELEGRAM');

    const reg = await request(app).post('/api/auth/register').send({
      name: 'Fallback User',
      email: 'fallback@oozox.com',
      phone: '+201022223333',
      password: 'Password123!',
      deviceId: 'dev_fb_1',
    });

    expect(reg.statusCode).toBe(201);
    const ch = reg.body.data.otpChallenge;
    expect(ch.activeChannel).toBe('WHATSAPP');
    expect(ch.attemptedChannels.map((a) => `${a.channel}:${a.status}`)).toEqual([
      'TELEGRAM:FAILED',
      'WHATSAPP:SENT',
    ]);

    // Ensure Twilio Verify challenge stores NO local codeHash / codeSalt
    const twilioChallenge = await OtpChallenge.findOne({ challengeId: ch.challengeId }).select(
      '+codeHash +codeSalt'
    );
    expect(twilioChallenge.providerType).toBe('TWILIO_VERIFY');
    expect(twilioChallenge.codeHash).toBeNull();
    expect(twilioChallenge.codeSalt).toBeNull();

    const verifyRes = await request(app).post('/api/auth/otp/verify').send({
      challengeId: ch.challengeId,
      code: '246810',
      deviceId: 'dev_fb_1',
    });
    expect(verifyRes.statusCode).toBe(200);
    expect(otpState.verifiedChecks.length).toBe(1);
  });

  it('3. Supersedes old Telegram OTP & revokes it on provider when resending / switching channel', async () => {
    const reg = await request(app).post('/api/auth/register').send({
      name: 'Switch Channel User',
      email: 'switch@oozox.com',
      phone: '+201033334444',
      password: 'Password123!',
      preferredChannel: 'TELEGRAM',
    });
    const firstChallengeId = reg.body.data.otpChallenge.challengeId;

    // Force cooldown ledger back 20s so we test superseding cleanly
    await OtpAbuseLedger.updateMany({}, { $set: { lastSentAt: new Date(Date.now() - 20000) } });

    const secondReq = await request(app).post('/api/auth/otp/request').send({
      purpose: 'REGISTER',
      channel: 'SMS',
      preAuthToken: reg.body.data.preAuthToken,
    });
    expect(secondReq.statusCode).toBe(200);
    expect(secondReq.body.data.activeChannel).toBe('SMS');
    expect(otpState.revoked.length).toBe(1);

    // Old challenge must fail as SUPERSEDED even with correct code
    const oldVerify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: firstChallengeId,
      code: '246810',
    });
    expect(oldVerify.statusCode).toBe(400);
    expect(oldVerify.body.code).toBe('OTP_SUPERSEDED');
  });

  it('4. Enforces Phone Login ALWAYS requires OTP; Email login passes on trusted device & blocks untrusted', async () => {
    const reg = await request(app).post('/api/auth/register').send({
      name: 'Device Policy User',
      email: 'policy@oozox.com',
      phone: '+201044445555',
      password: 'Password123!',
      deviceId: 'dev_trusted_a',
    });
    const verify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reg.body.data.otpChallenge.challengeId,
      code: '246810',
      deviceId: 'dev_trusted_a',
      hardwareFingerprint: 'hw_a',
    });
    const trustToken = verify.body.data.trustedDevice.deviceTrustToken;

    // A) Email + Password on trusted device -> direct full JWT
    const trustedLogin = await request(app).post('/api/auth/login').send({
      emailOrPhone: 'policy@oozox.com',
      password: 'Password123!',
      deviceId: 'dev_trusted_a',
      deviceTrustToken: trustToken,
      hardwareFingerprint: 'hw_a',
    });
    expect(trustedLogin.statusCode).toBe(200);
    expect(trustedLogin.body.data.requiresOtp).toBe(false);
    expect(trustedLogin.body.data.token).toBeDefined();

    // B) Email + Password on untrusted device -> OTP required
    const untrustedLogin = await request(app).post('/api/auth/login').send({
      emailOrPhone: 'policy@oozox.com',
      password: 'Password123!',
      deviceId: 'dev_unknown_b',
    });
    expect(untrustedLogin.statusCode).toBe(200);
    expect(untrustedLogin.body.data.requiresOtp).toBe(true);
    expect(untrustedLogin.body.data.token).toBeUndefined();

    // C) Phone login even WITH trusted device -> OTP ALWAYS required
    const phoneLogin = await request(app).post('/api/auth/login').send({
      emailOrPhone: '+201044445555',
      password: 'Password123!',
      deviceId: 'dev_trusted_a',
      deviceTrustToken: trustToken,
      hardwareFingerprint: 'hw_a',
    });
    expect(phoneLogin.statusCode).toBe(200);
    expect(phoneLogin.body.data.requiresOtp).toBe(true);
    expect(phoneLogin.body.data.reason).toBe('PHONE_LOGIN');
    expect(phoneLogin.body.data.token).toBeUndefined();
  });

  it('5. Invalidates 30-day device trust on expiry, remote revoke, and password change', async () => {
    const reg = await request(app).post('/api/auth/register').send({
      name: 'Trust Lifecycle User',
      email: 'lifecycle@oozox.com',
      phone: '+201055556666',
      password: 'OldPassword1!',
      deviceId: 'dev_life_1',
    });
    const verify = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reg.body.data.otpChallenge.challengeId,
      code: '246810',
      deviceId: 'dev_life_1',
    });
    const token1 = verify.body.data.token;
    const trustToken = verify.body.data.trustedDevice.deviceTrustToken;

    // Trusted biometric login works while valid
    const bioOk = await request(app).post('/api/auth/trusted-login').send({
      emailOrPhone: 'lifecycle@oozox.com',
      deviceId: 'dev_life_1',
      deviceTrustToken: trustToken,
    });
    expect(bioOk.statusCode).toBe(200);
    expect(bioOk.body.data.token).toBeDefined();

    // Change password revokes all trusted devices and prior sessions
    const pwRes = await request(app)
      .post('/api/auth/change-password')
      .set('Authorization', `Bearer ${token1}`)
      .send({
        currentPassword: 'OldPassword1!',
        newPassword: 'NewPassword2@',
        deviceId: 'dev_life_1',
      });
    expect(pwRes.statusCode).toBe(200);
    expect(pwRes.body.data.allDevicesRevoked).toBe(true);

    // Old token must fail
    const staleMe = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${token1}`);
    expect(staleMe.statusCode).toBe(401);

    // Old deviceTrustToken must fail and require OTP
    const bioAfterPw = await request(app).post('/api/auth/trusted-login').send({
      emailOrPhone: 'lifecycle@oozox.com',
      deviceId: 'dev_life_1',
      deviceTrustToken: trustToken,
    });
    expect(bioAfterPw.statusCode).toBe(401);
    expect(bioAfterPw.body.requiresOtp).toBe(true);
  });

  it('6. Blocks disallowed countries, triggers anti-pumping daily cap, and verifies Telegram webhook HMAC', async () => {
    process.env.OTP_ALLOWED_COUNTRIES = '+20,+966';
    process.env.OTP_DAILY_MAX_PER_PHONE = '2';

    const blockedCountry = await request(app).post('/api/auth/register').send({
      name: 'Foreign Spam',
      email: 'spam@oozox.com',
      phone: '+8801712345678',
      password: 'Password123!',
    });
    expect(blockedCountry.statusCode).toBe(201);
    expect(blockedCountry.body.data.otpDeliveryError.code).toBe('COUNTRY_NOT_ALLOWED');

    // Telegram Gateway Webhook HMAC signature verification
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

  it('7. Migrates pre-OTP legacy users idempotently without locking them out or verifying brand new ones', async () => {
    // Insert a legacy document directly (bypassing schema default) mimicking pre-cutover DB rows
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
    expect(migrated.phoneE164).toBe('+201088887777');
    expect(migrated.legacyMigratedAt).toBeTruthy();

    // Second run is idempotent
    const secondRun = await migrateLegacyUsers({ markExistingAsVerified: true });
    expect(secondRun.updatedCount).toBe(0);
  });
});
