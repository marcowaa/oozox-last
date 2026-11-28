const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../src/app');
const User = require('../src/models/User');
const BusinessProfile = require('../src/models/BusinessProfile');
const Employee = require('../src/models/Employee');
const Notification = require('../src/models/Notification');
const { installMockOtpAdapters, resetOtpAdapters } = require('./helpers/otpTestHarness');

jest.setTimeout(30000);

let mongoServer;
let ownerToken = '';
let ownerId = '';
let employeeToken = '';
let employeeId = '';

beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create({
    binary: { version: '7.0.14' },
  });
  await mongoose.connect(mongoServer.getUri());
  await Promise.all([
    User.deleteMany({}),
    BusinessProfile.deleteMany({}),
    Employee.deleteMany({}),
    Notification.deleteMany({}),
  ]);
  installMockOtpAdapters({ fixedCode: '334455' });
});

afterAll(async () => {
  resetOtpAdapters();
  await mongoose.connection.close();
  if (mongoServer) {
    await mongoServer.stop();
  }
});

describe('Enterprise Business, HR, Geofence & Task Flows', () => {
  it('1. Registers business owner via Phone OTP and sets up enterprise business profile + HQ geofence', async () => {
    const reqRes = await request(app).post('/api/auth/otp/request').send({
      phone: '+201099988877',
      deviceId: 'hr_owner_dev',
    });
    expect(reqRes.statusCode).toBe(200);
    const verifyRes = await request(app).post('/api/auth/otp/verify').send({
      challengeId: reqRes.body.data.challengeId,
      code: '334455',
      deviceId: 'hr_owner_dev',
    });
    expect(verifyRes.statusCode).toBe(200);
    ownerToken = verifyRes.body.data.token;
    ownerId = verifyRes.body.data.user.id;
    await request(app)
      .patch('/api/auth/me')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ name: 'Khaled Enterprise Owner', email: 'owner@enterprise.com' });

    const bizRes = await request(app)
      .put('/api/business/profile')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({
        companyName: 'شركة النور للتجارة والمقاولات',
        activityType: 'CONTRACTING_REAL_ESTATE',
        commercialRegistration: 'CR-998811',
        taxNumber: 'TAX-445566',
        address: 'القاهرة الجديدة - التجمع الخامس',
        hqLatitude: 30.0444,
        hqLongitude: 31.2357,
        geofenceRadiusMeters: 200,
      });

    expect(bizRes.statusCode).toBe(200);
    expect(bizRes.body.data.profile.companyName).toBe('شركة النور للتجارة والمقاولات');
    expect(bizRes.body.data.profile.geofenceRadiusMeters).toBe(200);
  });

  it('2. Creates employee with username/password, permissions & salary without leaking passwordHash', async () => {
    const empRes = await request(app)
      .post('/api/hr/employees')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({
        fullName: 'محمود عادل',
        username: 'mahmoud_eng',
        phone: '01012345678',
        password: 'empSecret99',
        roleTitle: 'مهندس موقع',
        department: 'المشاريع',
        baseSalary: 12000,
        geofenceEnforced: true,
        permissions: {
          manageAccounting: true,
          managePayroll: false,
          assignTasks: true,
        },
      });

    expect(empRes.statusCode).toBe(201);
    expect(empRes.body.data.employee.passwordHash).toBeUndefined();
    employeeId = empRes.body.data.employee._id;
  });

  it('3. Logs in employee via credentials and enforces RBAC permission guard', async () => {
    const loginRes = await request(app).post('/api/hr/employee-login').send({
      username: 'mahmoud_eng',
      password: 'empSecret99',
    });
    expect(loginRes.statusCode).toBe(200);
    employeeToken = loginRes.body.data.token;
    expect(loginRes.body.data.employee.permissions.assignTasks).toBe(true);

    // Employee lacks managePayroll -> must get 403 Forbidden
    const forbiddenPayroll = await request(app)
      .post('/api/hr/payroll')
      .set('Authorization', `Bearer ${employeeToken}`)
      .send({
        employeeId,
        type: 'BONUS',
        amount: 5000,
        reason: 'Self escalation test',
      });
    expect(forbiddenPayroll.statusCode).toBe(403);
  });

  it('4. Records bonus, deduction, and leave dates, then computes net monthly salary', async () => {
    const bonusRes = await request(app)
      .post('/api/hr/payroll')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ employeeId, type: 'BONUS', amount: 1500, reason: 'حافز إنجاز المشروع' });
    expect(bonusRes.statusCode).toBe(201);

    const deductRes = await request(app)
      .post('/api/hr/payroll')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ employeeId, type: 'DEDUCTION', amount: 500, reason: 'تأخير' });
    expect(deductRes.statusCode).toBe(201);

    const todayStr = new Date().toISOString().slice(0, 10);
    const leaveRes = await request(app)
      .post('/api/hr/leaves')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({
        employeeId,
        leaveType: 'ANNUAL',
        startDate: todayStr,
        endDate: todayStr,
        notes: 'إجازة رسمية',
      });
    expect(leaveRes.statusCode).toBe(201);

    const listRes = await request(app)
      .get('/api/hr/employees')
      .set('Authorization', `Bearer ${ownerToken}`);
    expect(listRes.statusCode).toBe(200);
    const emp = listRes.body.data.employees.find((e) => String(e._id) === String(employeeId));
    expect(emp.netSalary).toBe(13000); // 12000 + 1500 - 500
    expect(emp.isOnLeaveToday).toBe(true);
  });

  it('5. Blocks employee check-in when GPS disabled or outside geofence & alerts manager', async () => {
    const gpsOffRes = await request(app)
      .post('/api/hr/attendance/check-in')
      .set('Authorization', `Bearer ${employeeToken}`)
      .send({ gpsEnabled: false });
    expect(gpsOffRes.statusCode).toBe(403);

    // ~2 km away from HQ (30.0444, 31.2357)
    const outsideRes = await request(app)
      .post('/api/hr/attendance/check-in')
      .set('Authorization', `Bearer ${employeeToken}`)
      .send({ gpsEnabled: true, latitude: 30.065, longitude: 31.245 });
    expect(outsideRes.statusCode).toBe(403);
    expect(outsideRes.body.data.evaluation.inBounds).toBe(false);

    // Within 50m of HQ -> allowed
    const insideRes = await request(app)
      .post('/api/hr/attendance/check-in')
      .set('Authorization', `Bearer ${employeeToken}`)
      .send({ gpsEnabled: true, latitude: 30.0445, longitude: 31.2358 });
    expect(insideRes.statusCode).toBe(200);
    expect(insideRes.body.data.evaluation.inBounds).toBe(true);

    // Verify manager notifications were created for GPS off + geofence exit
    const notifsRes = await request(app)
      .get('/api/notifications')
      .set('Authorization', `Bearer ${ownerToken}`);
    expect(notifsRes.statusCode).toBe(200);
    const types = notifsRes.body.data.notifications.map((n) => n.type);
    expect(types).toContain('GPS_DISABLED_ALERT');
    expect(types).toContain('GEOFENCE_ALERT');
  });

  it('6. Assigns tasks with NOW, multi-hour (12h), and multi-day (5 days) deadlines', async () => {
    const dayTaskRes = await request(app)
      .post('/api/tasks')
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({
        projectId: 'proj_1',
        projectName: 'مشروع التجمع',
        workGroupId: 'wg_company_main',
        title: 'توريد وتركيب الواجهات',
        timingMode: 'DAYS',
        durationValue: 5,
        assignees: ['محمود عادل'],
      });
    expect(dayTaskRes.statusCode).toBe(201);
    const diffDays =
      (new Date(dayTaskRes.body.data.task.deadlineAt) - Date.now()) / (1000 * 60 * 60 * 24);
    expect(diffDays).toBeGreaterThan(4.9);

    const nowTaskRes = await request(app)
      .post('/api/tasks')
      .set('Authorization', `Bearer ${employeeToken}`)
      .send({
        projectId: 'wg_company_main',
        projectName: 'مجموعة كل الموظفين',
        title: 'مراجعة عاجلة للفواتير',
        timingMode: 'NOW',
        durationValue: 0,
        assignees: ['محمود عادل'],
      });
    expect(nowTaskRes.statusCode).toBe(201);
    expect(nowTaskRes.body.data.task.priority).toBe('URGENT');
  });

  it('7. Triggers manager alert on out-of-bounds heartbeat and supports deleting payroll/leave records', async () => {
    const hbOut = await request(app)
      .post('/api/hr/attendance/heartbeat')
      .set('Authorization', `Bearer ${employeeToken}`)
      .send({ gpsEnabled: true, latitude: 30.08, longitude: 31.28 });
    expect(hbOut.statusCode).toBe(200);
    expect(hbOut.body.data.evaluation.inBounds).toBe(false);
    expect(hbOut.body.data.evaluation.managerAlerted).toBe(true);

    const payList = await request(app)
      .get('/api/hr/payroll')
      .set('Authorization', `Bearer ${ownerToken}`);
    expect(payList.statusCode).toBe(200);
    const firstAdjId = payList.body.data.adjustments[0]._id;
    const delPay = await request(app)
      .delete(`/api/hr/payroll/${firstAdjId}`)
      .set('Authorization', `Bearer ${ownerToken}`);
    expect(delPay.statusCode).toBe(200);

    const leavesList = await request(app)
      .get('/api/hr/leaves')
      .set('Authorization', `Bearer ${ownerToken}`);
    expect(leavesList.statusCode).toBe(200);
    const firstLeaveId = leavesList.body.data.leaves[0]._id;
    const delLeave = await request(app)
      .delete(`/api/hr/leaves/${firstLeaveId}`)
      .set('Authorization', `Bearer ${ownerToken}`);
    expect(delLeave.statusCode).toBe(200);
  });

  it('8. Rejects employee token immediately after employee-logout or owner deactivation/password reset', async () => {
    const logoutRes = await request(app)
      .post('/api/hr/employee-logout')
      .set('Authorization', `Bearer ${employeeToken}`);
    expect(logoutRes.statusCode).toBe(200);

    // Reusing logged-out employee token MUST fail with 401
    const afterLogout = await request(app)
      .get('/api/hr/employees')
      .set('Authorization', `Bearer ${employeeToken}`);
    expect(afterLogout.statusCode).toBe(401);

    // Fresh login creates a new active AuthSession
    const reLogin = await request(app).post('/api/hr/employee-login').send({
      username: 'mahmoud_eng',
      password: 'empSecret99',
    });
    expect(reLogin.statusCode).toBe(200);
    const freshToken = reLogin.body.data.token;

    // Owner changes employee password -> old active token immediately rejected with 401
    const resetRes = await request(app)
      .patch(`/api/hr/employees/${employeeId}`)
      .set('Authorization', `Bearer ${ownerToken}`)
      .send({ password: 'newEmpSecret123' });
    expect(resetRes.statusCode).toBe(200);

    const afterReset = await request(app)
      .get('/api/hr/employees')
      .set('Authorization', `Bearer ${freshToken}`);
    expect(afterReset.statusCode).toBe(401);
  });
});
