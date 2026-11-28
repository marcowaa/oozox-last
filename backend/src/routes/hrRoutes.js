const express = require('express');
const router = express.Router();
const {
  loginEmployee,
  logoutEmployee,
  listEmployees,
  createEmployee,
  updateEmployee,
  deleteEmployee,
  addPayrollAdjustment,
  deletePayrollAdjustment,
  listPayrollAdjustments,
  createEmployeeLeave,
  deleteEmployeeLeave,
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

router.post('/employee-logout', logoutEmployee);
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
router.delete(
  '/payroll/:id',
  validate(schemas.objectIdParam, 'params'),
  requirePermission('managePayroll'),
  deletePayrollAdjustment
);

router.get('/leaves', listEmployeeLeaves);
router.post(
  '/leaves',
  requirePermission('manageStaff'),
  validate(schemas.createEmployeeLeave),
  createEmployeeLeave
);
router.delete(
  '/leaves/:id',
  validate(schemas.objectIdParam, 'params'),
  requirePermission('manageStaff'),
  deleteEmployeeLeave
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
