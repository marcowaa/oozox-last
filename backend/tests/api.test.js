const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const Invoice = require('../src/models/Invoice');
const Notification = require('../src/models/Notification');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;
let senderToken = '';
let senderId = '';
let senderDeviceTrustToken = '';
let receiverToken = '';
let receiverId = '';
let createdInvoiceId = '';

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({
    binary: { version: '7.0.14' },
  });
  await mongoose.connect(mongoServer.getUri());
  await User.deleteMany({});
  await Invoice.deleteMany({});
  await Notification.deleteMany({});
  installMockOtpAdapters({ fixedCode: '112233' });
});

afterAll(async () => {
  resetOtpAdapters();
  await mongoose.connection.close();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('1. Health Check Endpoint', () => {
  it('GET /api/health should return HEALTHY status', async () => {
    const res = await request(app).get('/api/health');
    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.status).toBe('HEALTHY');
  });
});

describe('2. Authentication Flow (/api/auth)', () => {
  it('POST /api/auth/register + OTP verify - should register and activate Sender user', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({
        name: 'Sender Trader',
        email: 'sender@oozox.com',
        phone: '+201000000001',
        password: 'password123',
        role: 'MERCHANT',
        deviceId: 'api_test_sender_dev',
      });

    expect(res.statusCode).toEqual(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.requiresOtp).toBe(true);
    expect(res.body.data.user.email).toBe('sender@oozox.com');

    const verifyRes = await request(app).post('/api/auth/otp/verify').send({
      challengeId: res.body.data.otpChallenge.challengeId,
      code: '112233',
      deviceId: 'api_test_sender_dev',
    });
    expect(verifyRes.statusCode).toEqual(200);
    expect(verifyRes.body.data.token).toBeDefined();
    senderToken = verifyRes.body.data.token;
    senderId = verifyRes.body.data.user.id;
    senderDeviceTrustToken = verifyRes.body.data.trustedDevice.deviceTrustToken;
  });

  it('POST /api/auth/register + OTP verify - should register Receiver user successfully', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({
        name: 'Receiver Client',
        email: 'receiver@oozox.com',
        phone: '+201000000002',
        password: 'password123',
        role: 'USER',
        deviceId: 'api_test_recv_dev',
      });

    expect(res.statusCode).toEqual(201);
    expect(res.body.success).toBe(true);
    const verifyRes = await request(app).post('/api/auth/otp/verify').send({
      challengeId: res.body.data.otpChallenge.challengeId,
      code: '112233',
      deviceId: 'api_test_recv_dev',
    });
    expect(verifyRes.statusCode).toEqual(200);
    receiverToken = verifyRes.body.data.token;
    receiverId = verifyRes.body.data.user.id;
  });

  it('POST /api/auth/login - should authenticate user on trusted device with email+password', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({
        emailOrPhone: 'sender@oozox.com',
        password: 'password123',
        deviceId: 'api_test_sender_dev',
        deviceTrustToken: senderDeviceTrustToken,
      });

    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.token).toBeDefined();
  });

  it('POST /api/auth/login - should reject invalid credentials with 401', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({
        emailOrPhone: 'sender@oozox.com',
        password: 'wrongpassword',
      });

    expect(res.statusCode).toEqual(401);
    expect(res.body.success).toBe(false);
  });

  it('GET /api/auth/me - should fetch user profile with valid Bearer token', async () => {
    const res = await request(app)
      .get('/api/auth/me')
      .set('Authorization', `Bearer ${senderToken}`);

    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.user.email).toBe('sender@oozox.com');
  });
});

describe('3. Invoices Management Flow (/api/invoices)', () => {
  it('POST /api/invoices - should create invoice and auto-notify receiver', async () => {
    const res = await request(app)
      .post('/api/invoices')
      .set('Authorization', `Bearer ${senderToken}`)
      .send({
        receiverId: receiverId,
        items: [
          {
            name: 'Gold Ingot 24k 10g',
            quantity: 2,
            unitPrice: 4200,
            karat: 24,
          },
          {
            name: 'Manufacturing & Stamping Fee',
            quantity: 2,
            unitPrice: 150,
            karat: 0,
          },
        ],
        taxAmount: 50,
        discount: 100,
        paymentMethod: 'INSTAPAY',
        dueDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        notes: 'Official OOZOX Gold Invoicing test entry',
      });

    expect(res.statusCode).toEqual(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.invoice.invoiceNumber).toMatch(/^INV-/);
    expect(res.body.data.invoice.totalAmount).toBe(8650);
    createdInvoiceId = res.body.data.invoice._id;
  });

  it('POST /api/invoices - should reject self-invoicing with 400', async () => {
    const res = await request(app)
      .post('/api/invoices')
      .set('Authorization', `Bearer ${senderToken}`)
      .send({
        receiverId: senderId,
        items: [{ name: 'Test item', quantity: 1, unitPrice: 500 }],
        dueDate: new Date().toISOString(),
      });

    expect(res.statusCode).toEqual(400);
    expect(res.body.success).toBe(false);
  });

  it('GET /api/invoices - should list invoices for receiver', async () => {
    const res = await request(app)
      .get('/api/invoices')
      .set('Authorization', `Bearer ${receiverToken}`);

    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.invoices.length).toBeGreaterThanOrEqual(1);
    expect(res.body.data.invoices[0]._id).toBe(createdInvoiceId);
  });

  it('GET /api/notifications - receiver should have received automated invoice notification', async () => {
    const res = await request(app)
      .get('/api/notifications')
      .set('Authorization', `Bearer ${receiverToken}`);

    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.notifications.length).toBeGreaterThanOrEqual(1);
    expect(res.body.data.notifications[0].type).toBe('INVOICE_CREATED');
  });

  it('PATCH /api/invoices/:id/status - should update invoice status to PAID', async () => {
    const res = await request(app)
      .patch(`/api/invoices/${createdInvoiceId}/status`)
      .set('Authorization', `Bearer ${senderToken}`)
      .send({ status: 'PAID' });

    expect(res.statusCode).toEqual(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.invoice.status).toBe('PAID');
  });
});
