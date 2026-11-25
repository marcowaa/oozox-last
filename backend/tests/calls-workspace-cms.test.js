const crypto = require('crypto');
const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const CallRecord = require('../src/models/CallRecord');
const CallBudgetLedger = require('../src/models/CallBudgetLedger');
const Notification = require('../src/models/Notification');
const WorkGroup = require('../src/models/WorkGroup');
const ProjectWorkspace = require('../src/models/ProjectWorkspace');
const ChatConversation = require('../src/models/ChatConversation');
const ChatMessage = require('../src/models/ChatMessage');
const CmsContent = require('../src/models/CmsContent');
const OtpAbuseLedger = require('../src/models/OtpAbuseLedger');
const { setCoturnMetricsFetcher } = require('../src/services/callBudgetService');
const { ensureSafeUserEmailPartialIndex } = require('../src/services/migrationService');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;

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

function signSignalingEvent(bodyObj, secret) {
  const ts = String(Math.floor(Date.now() / 1000));
  const raw = JSON.stringify(bodyObj);
  const sig = crypto.createHmac('sha256', secret).update(`${ts}.${raw}`).digest('hex');
  return { ts, sig, raw };
}

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({ binary: { version: '7.0.14' } });
  await mongoose.connect(mongoServer.getUri());
  await ensureSafeUserEmailPartialIndex();
  installMockOtpAdapters({ fixedCode: '667788' });
});

beforeEach(async () => {
  process.env.INTERNAL_SIGNALING_HMAC_SECRET = 'test_signaling_hmac_secret';
  process.env.RTC_MONTHLY_BUDGET_USD = '10';
  process.env.RTC_ALERT_THRESHOLD_1_PCT = '50';
  process.env.RTC_ALERT_THRESHOLD_2_PCT = '80';
  process.env.RTC_COST_PER_MINUTE_USD = '1.0'; // High per-min rate for deterministic threshold testing
  process.env.RTC_TURN_COST_PER_GB_USD = '1.0';
  process.env.RTC_DAILY_USER_LIMIT_MINUTES = '0';
  setCoturnMetricsFetcher(null);

  await Promise.all([
    User.deleteMany({}),
    CallRecord.deleteMany({}),
    CallBudgetLedger.deleteMany({}),
    Notification.deleteMany({}),
    WorkGroup.deleteMany({}),
    ProjectWorkspace.deleteMany({}),
    ChatConversation.deleteMany({}),
    ChatMessage.deleteMany({}),
    CmsContent.deleteMany({}),
    OtpAbuseLedger.deleteMany({}),
  ]);
});

afterAll(async () => {
  setCoturnMetricsFetcher(null);
  resetOtpAdapters();
  await mongoose.connection.close();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('Calls Budget Enforcement (50/80/100), Coturn Deltas, Workspace Sync & CMS', () => {
  it('1. Bills ONLY ACCEPTED->ENDED calls, ignores duplicates idempotently, fires 50% & 80% alerts once, and blocks new calls at 100%', async () => {
    const caller = await otpLogin({ phone: '+201066661111', deviceId: 'call_u1' });
    const callee = await otpLogin({ phone: '+201066662222', deviceId: 'call_u2' });
    const adminLogin = await otpLogin({ phone: '+201066669999', deviceId: 'call_admin' });
    await User.findByIdAndUpdate(adminLogin.user.id, { $set: { role: 'ADMIN' } });

    // 1) Ringing -> hung up without answer => 0 billed minutes!
    const authMissed = await request(app)
      .post('/api/calls/authorize')
      .set('Authorization', `Bearer ${caller.token}`)
      .send({ calleeId: callee.user.id, callType: 'AUDIO', callId: 'call_ring_only' });
    expect(authMissed.statusCode).toBe(200);

    for (const ev of [
      { callId: 'call_ring_only', eventId: 'ev_r1', eventType: 'RINGING', timestamp: '2026-11-01T10:00:00Z' },
      { callId: 'call_ring_only', eventId: 'ev_r2', eventType: 'MISSED', timestamp: '2026-11-01T10:00:30Z' },
    ]) {
      const signed = signSignalingEvent(ev, process.env.INTERNAL_SIGNALING_HMAC_SECRET);
      const res = await request(app)
        .post('/api/calls/events')
        .set('X-Signaling-Timestamp', signed.ts)
        .set('X-Signaling-Signature', signed.sig)
        .set('Content-Type', 'application/json')
        .send(signed.raw);
      expect(res.statusCode).toBe(200);
    }
    const missedRec = await CallRecord.findOne({ callId: 'call_ring_only' });
    expect(missedRec.billedMinutes).toBe(0);

    // 2) Accepted call lasting 5m 10s (billed 6 mins = $6 -> crosses 50% threshold on $10 budget)
    const authCall1 = await request(app)
      .post('/api/calls/authorize')
      .set('Authorization', `Bearer ${caller.token}`)
      .send({ calleeId: callee.user.id, callType: 'VIDEO', callId: 'call_billed_1' });
    expect(authCall1.statusCode).toBe(200);

    const acceptEv = {
      callId: 'call_billed_1',
      eventId: 'ev_b1_acc',
      eventType: 'ACCEPTED',
      timestamp: '2026-11-01T11:00:00Z',
    };
    const endEv = {
      callId: 'call_billed_1',
      eventId: 'ev_b1_end',
      eventType: 'ENDED',
      timestamp: '2026-11-01T11:05:10Z',
    };
    for (const ev of [acceptEv, endEv, endEv]) {
      // Sending endEv twice verifies idempotency
      const signed = signSignalingEvent(ev, process.env.INTERNAL_SIGNALING_HMAC_SECRET);
      const res = await request(app)
        .post('/api/calls/events')
        .set('X-Signaling-Timestamp', signed.ts)
        .set('X-Signaling-Signature', signed.sig)
        .set('Content-Type', 'application/json')
        .send(signed.raw);
      expect(res.statusCode).toBe(200);
    }

    let budgetRes = await request(app)
      .get('/api/calls/budget')
      .set('Authorization', `Bearer ${adminLogin.token}`);
    expect(budgetRes.statusCode).toBe(200);
    expect(budgetRes.body.data.totalBilledMinutes).toBe(6);
    expect(budgetRes.body.data.alertsTriggered).toContain(50);
    expect(budgetRes.body.data.newCallsBlocked).toBe(false);

    // 3) Authorize an ongoing call BEFORE hitting 100%, then push usage to >=100% via 4 more mins
    const activeCall = await request(app)
      .post('/api/calls/authorize')
      .set('Authorization', `Bearer ${caller.token}`)
      .send({ calleeId: callee.user.id, callType: 'AUDIO', callId: 'call_ongoing_before_100' });
    expect(activeCall.statusCode).toBe(200);

    const accOngoing = {
      callId: 'call_ongoing_before_100',
      eventId: 'ev_on_acc',
      eventType: 'ACCEPTED',
      timestamp: '2026-11-01T12:00:00Z',
    };
    const sAcc = signSignalingEvent(accOngoing, process.env.INTERNAL_SIGNALING_HMAC_SECRET);
    await request(app)
      .post('/api/calls/events')
      .set('X-Signaling-Timestamp', sAcc.ts)
      .set('X-Signaling-Signature', sAcc.sig)
      .set('Content-Type', 'application/json')
      .send(sAcc.raw);

    // Another 3-min call reaches $9 (90% -> triggers 80% alert once)
    await request(app)
      .post('/api/calls/authorize')
      .set('Authorization', `Bearer ${caller.token}`)
      .send({ calleeId: callee.user.id, callType: 'AUDIO', callId: 'call_billed_2' });
    for (const ev of [
      { callId: 'call_billed_2', eventId: 'ev_b2_acc', eventType: 'ACCEPTED', timestamp: '2026-11-01T12:01:00Z' },
      { callId: 'call_billed_2', eventId: 'ev_b2_end', eventType: 'ENDED', timestamp: '2026-11-01T12:04:00Z' },
    ]) {
      const s = signSignalingEvent(ev, process.env.INTERNAL_SIGNALING_HMAC_SECRET);
      await request(app)
        .post('/api/calls/events')
        .set('X-Signaling-Timestamp', s.ts)
        .set('X-Signaling-Signature', s.sig)
        .set('Content-Type', 'application/json')
        .send(s.raw);
    }

    // Ongoing call ends at 2 mins -> total $11 (>=100%), ongoing call still settles normally!
    const endOngoing = {
      callId: 'call_ongoing_before_100',
      eventId: 'ev_on_end',
      eventType: 'ENDED',
      timestamp: '2026-11-01T12:02:00Z',
    };
    const sEnd = signSignalingEvent(endOngoing, process.env.INTERNAL_SIGNALING_HMAC_SECRET);
    const ongoingEndRes = await request(app)
      .post('/api/calls/events')
      .set('X-Signaling-Timestamp', sEnd.ts)
      .set('X-Signaling-Signature', sEnd.sig)
      .set('Content-Type', 'application/json')
      .send(sEnd.raw);
    expect(ongoingEndRes.statusCode).toBe(200);
    expect(ongoingEndRes.body.data.call.billedMinutes).toBe(2);

    // Now NEW calls must be blocked at 100%, with 50, 80, 100 alerts emitted once each
    const blockedNew = await request(app)
      .post('/api/calls/authorize')
      .set('Authorization', `Bearer ${caller.token}`)
      .send({ calleeId: callee.user.id, callType: 'AUDIO', callId: 'call_blocked_new' });
    expect(blockedNew.statusCode).toBe(429);
    expect(blockedNew.body.code).toBe('RTC_MONTHLY_BUDGET_EXCEEDED');

    budgetRes = await request(app)
      .get('/api/calls/budget')
      .set('Authorization', `Bearer ${adminLogin.token}`);
    expect(budgetRes.body.data.alertsTriggered).toEqual(expect.arrayContaining([50, 80, 100]));
    expect(budgetRes.body.data.newCallsBlocked).toBe(true);
  });

  it('2. Tracks Coturn Prometheus byte deltas accurately across Coturn restarts (counter reset handling)', async () => {
    const adminLogin = await otpLogin({ phone: '+201066669998', deviceId: 'coturn_admin' });
    await User.findByIdAndUpdate(adminLogin.user.id, { $set: { role: 'ADMIN' } });

    // First scrape: 2 GiB
    let sampleText = 'turn_traffic_rcvb 1073741824\nturn_traffic_sentb 1073741824\n';
    setCoturnMetricsFetcher(async () => sampleText);

    const res1 = await request(app)
      .get('/api/calls/budget')
      .set('Authorization', `Bearer ${adminLogin.token}`);
    expect(res1.statusCode).toBe(200);
    expect(res1.body.data.turnBytesTotal).toBe(2147483648);
    expect(res1.body.data.coturnRestartCount).toBe(0);

    // Coturn restarts and counter drops back to 0.5 GiB -> total must ADD 0.5 GiB = 2.5 GiB cleanly
    sampleText = 'turn_traffic_rcvb 268435456\nturn_traffic_sentb 268435456\n';
    const res2 = await request(app)
      .get('/api/calls/budget')
      .set('Authorization', `Bearer ${adminLogin.token}`);
    expect(res2.statusCode).toBe(200);
    expect(res2.body.data.turnBytesTotal).toBe(2147483648 + 536870912);
    expect(res2.body.data.coturnRestartCount).toBe(1);
  });

  it('3. Syncs WorkGroups, Projects, encrypted Chats with IDOR protection, and Admin-only CMS updates', async () => {
    const user1 = await otpLogin({ phone: '+201055551111', deviceId: 'ws_u1' });
    const user2 = await otpLogin({ phone: '+201055552222', deviceId: 'ws_u2' });
    const outsider = await otpLogin({ phone: '+201055553333', deviceId: 'ws_u3' });
    const adminLogin = await otpLogin({ phone: '+201055559999', deviceId: 'ws_admin' });
    await User.findByIdAndUpdate(adminLogin.user.id, { $set: { role: 'ADMIN' } });

    // WorkGroup
    const grpRes = await request(app)
      .post('/api/groups')
      .set('Authorization', `Bearer ${user1.token}`)
      .send({ name: 'فريق المبيعات', memberIds: [user2.user.id] });
    expect(grpRes.statusCode).toBe(201);

    // Project
    const projRes = await request(app)
      .post('/api/projects')
      .set('Authorization', `Bearer ${user1.token}`)
      .send({ title: 'برج النيل', capitalBudget: 500000, memberIds: [user2.user.id] });
    expect(projRes.statusCode).toBe(201);

    // Chat conversation & encrypted message
    const convRes = await request(app)
      .post('/api/chats')
      .set('Authorization', `Bearer ${user1.token}`)
      .send({ peerUserId: user2.user.id });
    expect(convRes.statusCode).toBe(200);
    const convId = convRes.body.data.conversation.conversationId;

    const secretChatText = 'توريد ذهب عيار 21 بقيمة 120000 جنيه';
    const msgRes = await request(app)
      .post(`/api/chats/${convId}/messages`)
      .set('Authorization', `Bearer ${user1.token}`)
      .send({ content: secretChatText, hashtags: ['#gold', '#sale'] });
    expect(msgRes.statusCode).toBe(201);
    expect(msgRes.body.data.message.content).toBe(secretChatText);

    // Outsider cannot read messages (403 IDOR check)
    const idorChat = await request(app)
      .get(`/api/chats/${convId}/messages`)
      .set('Authorization', `Bearer ${outsider.token}`);
    expect(idorChat.statusCode).toBe(403);

    // Public CMS read works; USER update forbidden; ADMIN update succeeds
    const pubCms = await request(app).get('/api/content');
    expect(pubCms.statusCode).toBe(200);

    const userPutCms = await request(app)
      .put('/api/content')
      .set('Authorization', `Bearer ${user1.token}`)
      .send({ heroTitleAr: 'اختراق' });
    expect(userPutCms.statusCode).toBe(403);

    const adminPutCms = await request(app)
      .put('/api/content')
      .set('Authorization', `Bearer ${adminLogin.token}`)
      .send({ heroTitleAr: 'منصة أوزوكس الموحدة للإنتاج' });
    expect(adminPutCms.statusCode).toBe(200);
    expect(adminPutCms.body.data.content.heroTitleAr).toBe('منصة أوزوكس الموحدة للإنتاج');
  });
});
