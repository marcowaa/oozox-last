const express = require('express');
const router = express.Router();
const {
  loginEmployee,
  listEmployees,
  createEmployee,
  updateEmployee,
  deleteEmployee,
  addPayrollAdjustment,
  listPayrollAdjustments,
  createEmployeeLeave,
  listEmployeeLeaves,
  checkInWithGeofence,
  geofenceHeartbeat,
  listAttendanceRecords,
} = require('../controllers/hrController');
const { protectEnterprise, requirePermission } = require('../middlewares/authMiddleware');
const { validate, schemas } = require('../middlewares/validateMiddleware');

// Public credentialed login for business employees
router.post('/employee-login', validate(schemas.employeeLogin), loginEmployee);

// Protected enterprise HR & attendance endpoints
router.use(protectEnterprise);

router.get('/employees', listEmployees);
router.post(
  '/employees',
  requirePermission('manageStaff'),
  validate(schemas.createEmployee),
  createEmployee
);
router.patch(
  '/employees/:id',
  validate(schemas.objectIdParam, 'params'),
  requirePermission('manageStaff'),
  validate(schemas.updateEmployee),
  updateEmployee
);
router.delete(
  '/employees/:id',
  validate(schemas.objectIdParam, 'params'),
  requirePermission('manageStaff'),
  deleteEmployee
);

router.get('/payroll', listPayrollAdjustments);
router.post(
  '/payroll',
  requirePermission('managePayroll'),
  validate(schemas.createPayrollAdjustment),
  addPayrollAdjustment
);

router.get('/leaves', listEmployeeLeaves);
router.post(
  '/leaves',
  requirePermission('manageStaff'),
  validate(schemas.createEmployeeLeave),
  createEmployeeLeave
);

router.get('/attendance', listAttendanceRecords);
router.post(
  '/attendance/check-in',
  validate(schemas.attendanceGeoReport),
  checkInWithGeofence
);
router.post(
  '/attendance/heartbeat',
  validate(schemas.attendanceGeoReport),
  geofenceHeartbeat
);

module.exports = router;
