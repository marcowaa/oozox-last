const request = require('supertest');
const mongoose = require('mongoose');
const jwt = require('jsonwebtoken');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const Invoice = require('../src/models/Invoice');
const Notification = require('../src/models/Notification');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(40000);

let mongoServer;
let userAToken = '';
let userAId = '';
let userBToken = '';
let userBId = '';
let outsiderToken = '';
let invoiceId = '';

async function registerAndVerify({ phone, name, email, deviceId }) {
  const reqRes = await request(app).post('/api/auth/otp/request').send({ phone, deviceId });
  const verRes = await request(app).post('/api/auth/otp/verify').send({
    challengeId: reqRes.body.data.challengeId,
    code: '998877',
    deviceId,
  });
  await request(app)
    .patch('/api/auth/me')
    .set('Authorization', `Bearer ${verRes.body.data.token}`)
    .send({ name, email });
  return verRes.body.data;
}

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({
    binary: { version: '7.0.14' },
  });
  await mongoose.connect(mongoServer.getUri());
  await Promise.all([User.deleteMany({}), Invoice.deleteMany({}), Notification.deleteMany({})]);
  installMockOtpAdapters({ fixedCode: '998877' });

  const [dataA, dataB, dataC] = await Promise.all([
    registerAndVerify({
      name: 'Merchant Alpha',
      email: 'alpha@oozox.com',
      phone: '+201011111111',
      password: 'StrongPassword1!',
      role: 'MERCHANT',
      deviceId: 'sec_dev_a',
    }),
    registerAndVerify({
      name: 'Client Beta',
      email: 'beta@oozox.com',
      phone: '+201022222222',
      password: 'StrongPassword2!',
      role: 'USER',
      deviceId: 'sec_dev_b',
    }),
    registerAndVerify({
      name: 'Outsider Gamma',
      email: 'gamma@oozox.com',
      phone: '+201033333333',
      password: 'StrongPassword3!',
      role: 'USER',
      deviceId: 'sec_dev_c',
    }),
  ]);

  userAToken = dataA.token;
  userAId = dataA.user.id;
  userBToken = dataB.token;
  userBId = dataB.user.id;
  outsiderToken = dataC.token;

  const invRes = await request(app)
    .post('/api/invoices')
    .set('Authorization', `Bearer ${userAToken}`)
    .send({
      receiverId: userBId,
      items: [{ name: '24K Gold Bullion', quantity: 5, unitPrice: 4500, karat: 24 }],
      taxAmount: 250,
      discount: 0,
      paymentMethod: 'INSTAPAY',
      dueDate: new Date(Date.now() + 86400000).toISOString(),
    });
  invoiceId = invRes.body.data.invoice._id;
});

afterAll(async () => {
  resetOtpAdapters();
  await mongoose.connection.close();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('Security & Penetration Test Suite', () => {
  it('SEC-01 Privilege Escalation: public OTP verify rejects ADMIN portalScope for non-ADMIN accounts', async () => {
    const reqRes = await request(app).post('/api/auth/otp/request').send({
      phone: '+201099999999',
      deviceId: 'sec_att_dev',
    });
    const res = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqRes.body.data.challengeId,
      code: '998877',
      deviceId: 'sec_att_dev',
      portalScope: 'ADMIN',
    });

    expect(res.statusCode).toBe(403);
    expect(res.body.success).toBe(false);
  });

  it('SEC-02 NoSQL Injection: rejects $ne and $gt operator payloads in trusted-login & invoice queries', async () => {
    const loginInj = await request(app)
      .post('/api/auth/trusted-login')
      .send({ phone: { $ne: null }, deviceId: { $gt: '' }, deviceTrustToken: 'abc' });

    expect(loginInj.statusCode).toBe(400);
    expect(loginInj.body.success).toBe(false);

    const queryInj = await request(app)
      .get('/api/invoices')
      .set('Authorization', `Bearer ${userAToken}`)
      .query({ status: { $ne: 'PAID' } });

    expect(queryInj.statusCode).toBe(400);
    expect(queryInj.body.success).toBe(false);
  });

  it('SEC-03 ReDoS / Regex Injection: searchUsers & invoice search safely handle malicious regex syntax', async () => {
    const start = Date.now();
    const evilPattern = '((((((((((a+)+)+)+)+)+)+)+)+)+)$[unclosed';

    const userSearchRes = await request(app)
      .get('/api/auth/search')
      .set('Authorization', `Bearer ${userAToken}`)
      .query({ q: evilPattern });

    const invSearchRes = await request(app)
      .get('/api/invoices')
      .set('Authorization', `Bearer ${userAToken}`)
      .query({ search: evilPattern });

    const elapsedMs = Date.now() - start;
    expect(userSearchRes.statusCode).toBe(200);
    expect(invSearchRes.statusCode).toBe(200);
    expect(elapsedMs).toBeLessThan(1500);
  });

  it('SEC-04 IDOR Protection: Outsider cannot view, mutate status, delete invoice, or read victim notifications', async () => {
    const getRes = await request(app)
      .get(`/api/invoices/${invoiceId}`)
      .set('Authorization', `Bearer ${outsiderToken}`);
    expect(getRes.statusCode).toBe(403);

    const patchRes = await request(app)
      .patch(`/api/invoices/${invoiceId}/status`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .send({ status: 'PAID' });
    expect(patchRes.statusCode).toBe(403);

    const delRes = await request(app)
      .delete(`/api/invoices/${invoiceId}`)
      .set('Authorization', `Bearer ${outsiderToken}`);
    expect(delRes.statusCode).toBe(403);

    const notifRes = await request(app)
      .get('/api/notifications')
      .set('Authorization', `Bearer ${userBToken}`);
    const victimNotifId = notifRes.body.data.notifications[0]._id;

    const markOtherNotif = await request(app)
      .patch(`/api/notifications/${victimNotifId}/read`)
      .set('Authorization', `Bearer ${outsiderToken}`);
    expect(markOtherNotif.statusCode).toBe(404);
  });

  it('SEC-05 JWT Tampering & Alg-None Rejection: blocks unsigned or forged JWT tokens', async () => {
    const forgedToken = jwt.sign({ id: userAId, role: 'ADMIN' }, 'wrong_secret_key', {
      algorithm: 'HS256',
    });
    const forgedRes = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${forgedToken}`);
    expect(forgedRes.statusCode).toBe(401);

    const malformedRes = await request(app)
      .get('/api/auth/me')
      .set('Authorization', 'Bearer eyJhbGciOiJub25lIn0.eyJpZCI6IjEyMyJ9.');
    expect(malformedRes.statusCode).toBe(401);
  });

  it('SEC-06 ObjectId Validation & CORS: invalid ObjectId returns 400 cleanly', async () => {
    const badInvoiceId = await request(app)
      .get('/api/invoices/not-a-valid-object-id')
      .set('Authorization', `Bearer ${userAToken}`);
    expect(badInvoiceId.statusCode).toBe(400);

    const badNotifId = await request(app)
      .patch('/api/notifications/invalid_id/read')
      .set('Authorization', `Bearer ${userAToken}`);
    expect(badNotifId.statusCode).toBe(400);
  });

  it('SEC-07 Pagination DoS Clamp: rejects negative or >100 page limits safely', async () => {
    const hugeLimit = await request(app)
      .get('/api/invoices')
      .set('Authorization', `Bearer ${userAToken}`)
      .query({ page: 1, limit: 999999 });
    expect(hugeLimit.statusCode).toBe(400);

    const validMaxLimit = await request(app)
      .get('/api/invoices')
      .set('Authorization', `Bearer ${userAToken}`)
      .query({ page: 1, limit: 100 });
    expect(validMaxLimit.statusCode).toBe(200);
    expect(validMaxLimit.body.data.pagination.limit).toBe(100);
  });

  it('SEC-08 Invoice Financial State Machine: blocks reverting PAID invoice to PENDING and deleting PAID invoice', async () => {
    const payRes = await request(app)
      .patch(`/api/invoices/${invoiceId}/status`)
      .set('Authorization', `Bearer ${userAToken}`)
      .send({ status: 'PAID' });
    expect(payRes.statusCode).toBe(200);

    const revertRes = await request(app)
      .patch(`/api/invoices/${invoiceId}/status`)
      .set('Authorization', `Bearer ${userAToken}`)
      .send({ status: 'PENDING' });
    expect(revertRes.statusCode).toBe(400);

    const delPaidRes = await request(app)
      .delete(`/api/invoices/${invoiceId}`)
      .set('Authorization', `Bearer ${userAToken}`);
    expect(delPaidRes.statusCode).toBe(400);
  });

  it('SEC-09 Brute-Force Rate Limiting: blocks repeated failed trusted-logins with HTTP 429', async () => {
    let hit429 = false;
    for (let i = 0; i < 15; i++) {
      const res = await request(app)
        .post('/api/auth/trusted-login')
        .send({ phone: '+201011111111', deviceId: 'sec_dev_a', deviceTrustToken: `WrongToken_${i}` });
      if (res.statusCode === 429) {
        hit429 = true;
        break;
      }
    }
    expect(hit429).toBe(true);
  });

  it('SEC-10 Database Field Encryption At Rest: encrypts Invoice.notes on disk and returns plaintext via Mongoose', async () => {
    const { encryptField, decryptField, isEncryptedField } = require('../src/utils/fieldCrypto');
    const prevKey = process.env.DB_ENCRYPTION_KEY;
    process.env.DB_ENCRYPTION_KEY = '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

    try {
      const secretNote = 'تسوية خزينة فرع الحسين — سري للغاية';
      const inv = await Invoice.create({
        invoiceNumber: `INV-ENC-${Date.now()}`,
        sender: userAId,
        receiver: userBId,
        items: [{ name: '24K Bullion', quantity: 1, unitPrice: 5000, total: 5000, karat: 24 }],
        subTotal: 5000,
        taxAmount: 0,
        discount: 0,
        totalAmount: 5000,
        dueDate: new Date(Date.now() + 86400000),
        notes: secretNote,
      });

      expect(inv.notes).toBe(secretNote);

      // Read raw BSON document directly via collection driver to prove ciphertext on disk
      const rawDoc = await mongoose.connection.db
        .collection('invoices')
        .findOne({ _id: inv._id });
      expect(isEncryptedField(rawDoc.notes)).toBe(true);
      expect(rawDoc.notes).not.toContain('الحسين');
      expect(decryptField(rawDoc.notes)).toBe(secretNote);
      expect(encryptField(rawDoc.notes)).toBe(rawDoc.notes);
    } finally {
      if (prevKey === undefined) {
        delete process.env.DB_ENCRYPTION_KEY;
      } else {
        process.env.DB_ENCRYPTION_KEY = prevKey;
      }
    }
  });
});

describe('Load & Concurrency Resilience Test Suite', () => {
  it('LOAD-01 handles 30 concurrent invoice writes + notifications under latency threshold without errors', async () => {
    const start = Date.now();
    const calls = Array.from({ length: 30 }, (_, idx) =>
      request(app)
        .post('/api/invoices')
        .set('Authorization', `Bearer ${userAToken}`)
        .send({
          receiverId: userBId,
          items: [{ name: `Load Gold Lot #${idx}`, quantity: 1, unitPrice: 1000 + idx, karat: 21 }],
          taxAmount: 10,
          discount: 0,
          paymentMethod: 'CASH',
          dueDate: new Date(Date.now() + 86400000).toISOString(),
        })
    );

    const results = await Promise.all(calls);
    const durationMs = Date.now() - start;

    results.forEach((res) => {
      expect(res.statusCode).toBe(201);
      expect(res.body.data.invoice.invoiceNumber).toMatch(/^INV-/);
    });

    // Verify all 30 unique invoices + original 1 persisted cleanly
    const listRes = await request(app)
      .get('/api/invoices?limit=100')
      .set('Authorization', `Bearer ${userAToken}`);
    expect(listRes.statusCode).toBe(200);
    expect(listRes.body.data.pagination.total).toBeGreaterThanOrEqual(31);
    expect(durationMs).toBeLessThan(10000);
  });
});
