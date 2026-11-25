const crypto = require('crypto');
const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const EncryptedBackup = require('../src/models/EncryptedBackup');
const DeviceTransferSession = require('../src/models/DeviceTransferSession');
const DeviceTransferChunk = require('../src/models/DeviceTransferChunk');
const TrustedDevice = require('../src/models/TrustedDevice');
const AuthSession = require('../src/models/AuthSession');
const OtpAbuseLedger = require('../src/models/OtpAbuseLedger');
const { ensureSafeUserEmailPartialIndex } = require('../src/services/migrationService');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;

async function otpLogin({ phone, deviceId, deviceName }) {
  await OtpAbuseLedger.updateMany({}, { $set: { lastSentAt: new Date(Date.now() - 120000) } });
  const reqRes = await request(app).post('/api/auth/otp/request').send({
    phone,
    deviceId,
    deviceName,
  });
  const verRes = await request(app).post('/api/auth/otp/verify').send({
    challengeId: reqRes.body.data.challengeId,
    code: '556677',
    deviceId,
    deviceName,
  });
  return verRes.body.data;
}

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({ binary: { version: '7.0.14' } });
  await mongoose.connect(mongoServer.getUri());
  await ensureSafeUserEmailPartialIndex();
  installMockOtpAdapters({ fixedCode: '556677' });
});

beforeEach(async () => {
  await Promise.all([
    User.deleteMany({}),
    EncryptedBackup.deleteMany({}),
    DeviceTransferSession.deleteMany({}),
    DeviceTransferChunk.deleteMany({}),
    TrustedDevice.deleteMany({}),
    AuthSession.deleteMany({}),
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

describe('Cloud Encrypted Backups (/api/backups) & E2E Cross-Device Transfer (/api/transfer)', () => {
  it('1. Uploads, lists, downloads, and deletes real encrypted .oozox backups with strict IDOR protection', async () => {
    const owner = await otpLogin({
      phone: '+201077771111',
      deviceId: 'bak_dev_1',
      deviceName: 'Galaxy S24',
    });
    const intruder = await otpLogin({
      phone: '+201077772222',
      deviceId: 'bak_dev_2',
      deviceName: 'Intruder Phone',
    });

    const vaultV3Envelope = JSON.stringify({
      magic: 'OOZOX_ENCRYPTED_VAULT_V3',
      version: 3,
      kdf: 'PBKDF2WithHmacSHA256',
      iterations: 120000,
      salt: 'c2FsdA==',
      iv: 'aXZ2dg==',
      ciphertext: 'ZW5jcnlwdGVkX3ZhdWx0X3BheWxvYWRfd2l0aF9tZWRpYQ==',
      stats: {
        totalRecords: 145,
        chatsCount: 12,
        messagesCount: 380,
        mediaFilesCount: 24,
      },
    });

    const upRes = await request(app)
      .post('/api/backups')
      .set('Authorization', `Bearer ${owner.token}`)
      .set('X-Device-Id', 'bak_dev_1')
      .send({
        fileName: 'oozox_full_v3.oozox',
        backupType: 'FULL_VAULT_V3',
        vaultContent: vaultV3Envelope,
        deviceName: 'Galaxy S24',
      });

    expect(upRes.statusCode).toBe(201);
    const backupId = upRes.body.data.backup.backupId;
    expect(backupId).toBeTruthy();

    // Returning user OTP verify now surfaces real backupSummary
    const returningLogin = await otpLogin({
      phone: '+201077771111',
      deviceId: 'bak_dev_3',
      deviceName: 'Pixel 9',
    });
    expect(returningLogin.isExistingUser).toBe(true);
    expect(returningLogin.backupSummary.hasCloudBackups).toBe(true);
    expect(returningLogin.backupSummary.cloudBackupsCount).toBe(1);

    // Owner lists & downloads encrypted blob
    const listRes = await request(app)
      .get('/api/backups')
      .set('Authorization', `Bearer ${owner.token}`);
    expect(listRes.statusCode).toBe(200);
    expect(listRes.body.data.backups).toHaveLength(1);
    expect(listRes.body.data.backups[0].vaultContent).toBeUndefined();

    const dlRes = await request(app)
      .get(`/api/backups/${backupId}/download`)
      .set('Authorization', `Bearer ${owner.token}`);
    expect(dlRes.statusCode).toBe(200);
    expect(dlRes.body.data.vaultContent).toBe(vaultV3Envelope);

    // IDOR: intruder gets 403 on download and delete
    const idorDl = await request(app)
      .get(`/api/backups/${backupId}/download`)
      .set('Authorization', `Bearer ${intruder.token}`);
    expect(idorDl.statusCode).toBe(403);

    const idorDel = await request(app)
      .delete(`/api/backups/${backupId}`)
      .set('Authorization', `Bearer ${intruder.token}`);
    expect(idorDel.statusCode).toBe(403);

    // Owner deletes cleanly
    const delOk = await request(app)
      .delete(`/api/backups/${backupId}`)
      .set('Authorization', `Bearer ${owner.token}`);
    expect(delOk.statusCode).toBe(200);
  });

  it('2. Executes full E2E QR/Push cross-device transfer and IMMEDIATELY revokes old phone sessions & chunks', async () => {
    // 1) Old phone logged in
    const oldPhone = await otpLogin({
      phone: '+201088881111',
      deviceId: 'phone_old_1',
      deviceName: 'iPhone/Android Old',
    });

    // 2) New phone logs in via OTP and gets hasOtherActiveDevice: true
    const newPhone = await otpLogin({
      phone: '+201088881111',
      deviceId: 'phone_new_2',
      deviceName: 'Pixel 9 Pro New',
    });
    expect(newPhone.hasOtherActiveDevice).toBe(true);

    // 3) New phone generates ECDH keypair & initiates transfer
    const targetKeyPair = crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
    const targetPubB64 = targetKeyPair.publicKey.export({ type: 'spki', format: 'der' }).toString('base64');

    const initRes = await request(app)
      .post('/api/transfer/initiate')
      .set('Authorization', `Bearer ${newPhone.token}`)
      .set('X-Device-Id', 'phone_new_2')
      .send({
        targetEcdhPubKey: targetPubB64,
        deviceName: 'Pixel 9 Pro New',
      });
    expect(initRes.statusCode).toBe(201);
    const { transferId, qrPayload } = initRes.body.data;
    const parsedQr = JSON.parse(qrPayload);
    expect(parsedQr.transferId).toBe(transferId);

    // Old phone sees pending transfer request
    const pendingRes = await request(app)
      .get('/api/transfer/pending')
      .set('Authorization', `Bearer ${oldPhone.token}`)
      .set('X-Device-Id', 'phone_old_1');
    expect(pendingRes.statusCode).toBe(200);
    expect(pendingRes.body.data.pending[0].transferId).toBe(transferId);

    // Target device CANNOT approve its own transfer
    const selfApprove = await request(app)
      .post(`/api/transfer/${transferId}/approve`)
      .set('Authorization', `Bearer ${newPhone.token}`)
      .set('X-Device-Id', 'phone_new_2')
      .send({
        sourceEcdhPubKey: targetPubB64,
        approvalMethod: 'QR_SCAN',
        qrNonce: parsedQr.qrNonce,
      });
    expect(selfApprove.statusCode).toBe(400);

    // 4) Old phone approves using scanned QR nonce + wrong nonce fails
    const badQrApprove = await request(app)
      .post(`/api/transfer/${transferId}/approve`)
      .set('Authorization', `Bearer ${oldPhone.token}`)
      .set('X-Device-Id', 'phone_old_1')
      .send({
        sourceEcdhPubKey: 'sourcePubBase64_placeholder_key_long_enough_for_check',
        approvalMethod: 'QR_SCAN',
        qrNonce: 'wrong_nonce',
      });
    expect(badQrApprove.statusCode).toBe(403);

    const sourceKeyPair = crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
    const sourcePubB64 = sourceKeyPair.publicKey.export({ type: 'spki', format: 'der' }).toString('base64');

    const okApprove = await request(app)
      .post(`/api/transfer/${transferId}/approve`)
      .set('Authorization', `Bearer ${oldPhone.token}`)
      .set('X-Device-Id', 'phone_old_1')
      .send({
        sourceEcdhPubKey: sourcePubB64,
        approvalMethod: 'QR_SCAN',
        qrNonce: parsedQr.qrNonce,
      });
    expect(okApprove.statusCode).toBe(200);

    // 5) Old phone encrypts payload chunks with derived AES key & uploads them
    const chunkCipher = Buffer.from('encrypted-chats-and-media-binary-chunk-0').toString('base64');
    const chunkHash = crypto.createHash('sha256').update(chunkCipher, 'utf8').digest('hex');

    const upChunkRes = await request(app)
      .post(`/api/transfer/${transferId}/chunks`)
      .set('Authorization', `Bearer ${oldPhone.token}`)
      .set('X-Device-Id', 'phone_old_1')
      .send({
        chunkIndex: 0,
        totalChunks: 1,
        cipherChunkBase64: chunkCipher,
        ivBase64: Buffer.from('123456789012').toString('base64'),
        chunkSha256: chunkHash,
      });
    expect(upChunkRes.statusCode).toBe(200);

    const finRes = await request(app)
      .post(`/api/transfer/${transferId}/finalize-upload`)
      .set('Authorization', `Bearer ${oldPhone.token}`)
      .set('X-Device-Id', 'phone_old_1')
      .send({
        totalChunks: 1,
        encryptedManifestBase64: Buffer.from('manifest').toString('base64'),
        manifestIvBase64: Buffer.from('iv1234567890').toString('base64'),
        summary: { totalRecords: 50, mediaFilesCount: 8, totalBytes: 2048 },
      });
    expect(finRes.statusCode).toBe(200);

    // 6) New phone downloads encrypted chunk & completes transfer
    const dlChunkRes = await request(app)
      .get(`/api/transfer/${transferId}/chunks/0`)
      .set('Authorization', `Bearer ${newPhone.token}`)
      .set('X-Device-Id', 'phone_new_2');
    expect(dlChunkRes.statusCode).toBe(200);
    expect(dlChunkRes.body.data.cipherChunkBase64).toBe(chunkCipher);

    const completeRes = await request(app)
      .post(`/api/transfer/${transferId}/complete`)
      .set('Authorization', `Bearer ${newPhone.token}`)
      .set('X-Device-Id', 'phone_new_2')
      .send({});
    expect(completeRes.statusCode).toBe(200);
    expect(completeRes.body.data.revokedDeviceId).toBe('phone_old_1');

    // 7) Old phone MUST be logged out immediately (401 on any protected API call), relay chunks deleted, new phone stays active!
    const oldPhoneMe = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${oldPhone.token}`);
    expect(oldPhoneMe.statusCode).toBe(401);

    const oldPhoneTrusted = await request(app).post('/api/auth/trusted-login').send({
      phone: '+201088881111',
      deviceId: 'phone_old_1',
      deviceTrustToken: oldPhone.trustedDevice.deviceTrustToken,
    });
    expect(oldPhoneTrusted.statusCode).toBe(401);

    const remainingChunks = await DeviceTransferChunk.countDocuments({ transferId });
    expect(remainingChunks).toBe(0);

    const newPhoneMe = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${newPhone.token}`);
    expect(newPhoneMe.statusCode).toBe(200);
  });

  it('3. Revokes other active devices when user chooses Skip + Secure on new phone', async () => {
    const oldPhone = await otpLogin({
      phone: '+201088883333',
      deviceId: 'lost_phone_a',
      deviceName: 'Lost Phone',
    });
    const newPhone = await otpLogin({
      phone: '+201088883333',
      deviceId: 'new_phone_b',
      deviceName: 'New Phone',
    });

    const revRes = await request(app)
      .post('/api/transfer/revoke-other-devices')
      .set('Authorization', `Bearer ${newPhone.token}`)
      .set('X-Device-Id', 'new_phone_b')
      .send({});
    expect(revRes.statusCode).toBe(200);
    expect(revRes.body.data.revokedSessionsCount).toBeGreaterThanOrEqual(1);

    const oldAttempt = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${oldPhone.token}`);
    expect(oldAttempt.statusCode).toBe(401);
  });
});
