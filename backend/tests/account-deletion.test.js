const crypto = require('crypto');
const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const AuthSession = require('../src/models/AuthSession');
const TrustedDevice = require('../src/models/TrustedDevice');
const EncryptedBackup = require('../src/models/EncryptedBackup');
const DeviceTransferSession = require('../src/models/DeviceTransferSession');
const DeviceTransferChunk = require('../src/models/DeviceTransferChunk');
const Notification = require('../src/models/Notification');
const Invoice = require('../src/models/Invoice');
const ChatConversation = require('../src/models/ChatConversation');
const ChatMessage = require('../src/models/ChatMessage');
const OtpAbuseLedger = require('../src/models/OtpAbuseLedger');
const { ensureSafeUserEmailPartialIndex } = require('../src/services/migrationService');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;

async function otpLogin({ phone, deviceId, fcmToken }) {
  await OtpAbuseLedger.updateMany({}, { $set: { lastSentAt: new Date(Date.now() - 20000) } });
  const reqRes = await request(app).post('/api/auth/otp/request').send({ phone, deviceId });
  const verRes = await request(app).post('/api/auth/otp/verify').send({
    challengeId: reqRes.body.data.challengeId,
    code: '667788',
    deviceId,
    fcmToken,
  });
  return verRes.body.data;
}

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({ binary: { version: '7.0.14' } });
  await mongoose.connect(mongoServer.getUri());
  await ensureSafeUserEmailPartialIndex();
  installMockOtpAdapters({ fixedCode: '667788' });
});

beforeEach(async () => {
  process.env.OTP_RESEND_COOLDOWN_SECONDS = '1';
  process.env.SENSITIVE_STEP_UP_SECONDS = '900';

  await Promise.all([
    User.deleteMany({}),
    AuthSession.deleteMany({}),
    TrustedDevice.deleteMany({}),
    EncryptedBackup.deleteMany({}),
    DeviceTransferSession.deleteMany({}),
    DeviceTransferChunk.deleteMany({}),
    Notification.deleteMany({}),
    Invoice.deleteMany({}),
    ChatConversation.deleteMany({}),
    ChatMessage.deleteMany({}),
    OtpAbuseLedger.deleteMany({}),
  ]);
});

afterAll(async () => {
  resetOtpAdapters();
  await mongoose.connection.close();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('Account Deletion (DELETE /api/auth/me) with OTP Step-Up & Last-Admin Guard', () => {
  it('1. Rejects DELETE /api/auth/me with 403 STEP_UP_OTP_REQUIRED when session lacks fresh OTP step-up', async () => {
    const login = await otpLogin({ phone: '+201088881111', deviceId: 'del_dev_1' });
    // Simulate stale / non-OTP session
    await AuthSession.updateOne(
      { sessionId: login.sessionId },
      { $set: { otpVerifiedAt: new Date(Date.now() - 3600 * 1000) } }
    );

    const denied = await request(app)
      .delete('/api/auth/me')
      .set('Authorization', `Bearer ${login.token}`)
      .send({});
    expect(denied.statusCode).toBe(403);
    expect(denied.body.requiresStepUpOtp).toBe(true);
    expect(denied.body.code).toBe('STEP_UP_OTP_REQUIRED');

    // Request fresh SENSITIVE_ACTION OTP & pass it to DELETE /api/auth/me
    const stepUpReq = await request(app)
      .post('/api/auth/otp/request')
      .set('Authorization', `Bearer ${login.token}`)
      .send({ purpose: 'SENSITIVE_ACTION' });
    expect(stepUpReq.statusCode).toBe(200);

    const deletedRes = await request(app)
      .delete('/api/auth/me')
      .set('Authorization', `Bearer ${login.token}`)
      .send({
        stepUpChallengeId: stepUpReq.body.data.challengeId,
        stepUpCode: '667788',
      });
    expect(deletedRes.statusCode).toBe(200);
    expect(deletedRes.body.data.deleted).toBe(true);
  });

  it('2. Returns 409 LAST_ADMIN_CANNOT_DELETE when last ADMIN attempts deletion, succeeds once another ADMIN exists', async () => {
    const admin1 = await otpLogin({ phone: '+201088882221', deviceId: 'admin_dev_1' });
    const admin2 = await otpLogin({ phone: '+201088882222', deviceId: 'admin_dev_2' });
    await User.findByIdAndUpdate(admin1.user.id, { $set: { role: 'ADMIN' } });

    const blocked409 = await request(app)
      .delete('/api/auth/me')
      .set('Authorization', `Bearer ${admin1.token}`)
      .send({});
    expect(blocked409.statusCode).toBe(409);
    expect(blocked409.body.code).toBe('LAST_ADMIN_CANNOT_DELETE');

    // Promote second admin; now admin1 can safely delete
    await User.findByIdAndUpdate(admin2.user.id, { $set: { role: 'ADMIN' } });
    const allowed = await request(app)
      .delete('/api/auth/me')
      .set('Authorization', `Bearer ${admin1.token}`)
      .send({});
    expect(allowed.statusCode).toBe(200);
    expect(allowed.body.data.deleted).toBe(true);
  });

  it('3. Revokes sessions, devices, FCM tokens, transfers, backups, and anonymizes user while keeping counterparty invoices/messages intact', async () => {
    const victim = await otpLogin({
      phone: '+201088883331',
      deviceId: 'victim_dev_1',
      fcmToken: 'fcm_token_victim_123456',
    });
    const partner = await otpLogin({ phone: '+201088883332', deviceId: 'partner_dev_1' });

    await EncryptedBackup.create({
      ownerId: victim.user.id,
      fileName: 'vault.oozox',
      phoneHash: crypto.createHash('sha256').update('+201088883331').digest('hex'),
      payloadSha256: crypto.createHash('sha256').update('cipher').digest('hex'),
      vaultContent: 'encrypted_blob',
      sizeBytes: 14,
    });
    const transfer = await DeviceTransferSession.create({
      userId: victim.user.id,
      targetDeviceId: 'new_phone',
      targetEcdhPubKey: 'pub_key_123',
      qrNonceHash: crypto.createHash('sha256').update('nonce').digest('hex'),
      expiresAt: new Date(Date.now() + 600000),
    });
    await DeviceTransferChunk.create({
      transferId: transfer.transferId,
      userId: victim.user.id,
      chunkIndex: 0,
      totalChunks: 1,
      cipherChunkBase64: 'YWJj',
      ivBase64: 'aXY=',
      chunkSha256: 'abc',
      expiresAt: new Date(Date.now() + 600000),
    });
    await Notification.create({
      recipient: victim.user.id,
      title: 'Personal Alert',
      message: 'Private body',
      type: 'SYSTEM',
    });

    const invoice = await Invoice.create({
      invoiceNumber: `INV-DEL-${Date.now()}`,
      sender: victim.user.id,
      receiver: partner.user.id,
      items: [{ name: 'Gold 21k', quantity: 1, unitPrice: 4000, total: 4000, karat: 21 }],
      subTotal: 4000,
      totalAmount: 4000,
      dueDate: new Date(Date.now() + 86400000),
    });

    const convo = await ChatConversation.create({
      type: 'DIRECT',
      participants: [victim.user.id, partner.user.id],
    });
    await ChatMessage.create({
      conversationId: convo.conversationId,
      senderId: victim.user.id,
      content: 'Agreed invoice terms',
    });

    const delRes = await request(app)
      .delete('/api/auth/me')
      .set('Authorization', `Bearer ${victim.token}`)
      .send({});
    expect(delRes.statusCode).toBe(200);

    // All sessions & trusted devices revoked; user PII scrubbed; FCM tokens cleared
    const activeSessions = await AuthSession.countDocuments({ userId: victim.user.id, revokedAt: null });
    const activeDevices = await TrustedDevice.countDocuments({ userId: victim.user.id, revokedAt: null });
    expect(activeSessions).toBe(0);
    expect(activeDevices).toBe(0);

    expect(await EncryptedBackup.countDocuments({ ownerId: victim.user.id })).toBe(0);
    expect(await DeviceTransferSession.countDocuments({ userId: victim.user.id })).toBe(0);
    expect(await DeviceTransferChunk.countDocuments({ transferId: transfer.transferId })).toBe(0);
    expect(await Notification.countDocuments({ recipient: victim.user.id })).toBe(0);

    const scrubbedUser = await User.findById(victim.user.id);
    expect(scrubbedUser).not.toBeNull();
    expect(scrubbedUser.name).toBe('Deleted User');
    expect(scrubbedUser.phone).toBe(`deleted_${victim.user.id}`);
    expect(scrubbedUser.phoneE164).toBeUndefined();
    expect(scrubbedUser.isVerified).toBe(false);
    expect(scrubbedUser.fcmTokens).toHaveLength(0);

    // Subsequent API access with old token fails
    const postDeleteMe = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${victim.token}`);
    expect(postDeleteMe.statusCode).toBe(401);

    // Counterparty invoice & chat remain valid and readable by partner
    const partnerInvoiceRes = await request(app)
      .get('/api/invoices')
      .set('Authorization', `Bearer ${partner.token}`);
    expect(partnerInvoiceRes.statusCode).toBe(200);
    const foundInv = partnerInvoiceRes.body.data.invoices.find(
      (x) => String(x._id) === String(invoice._id)
    );
    expect(foundInv).toBeDefined();
  });
});
