const BusinessProfile = require('../models/BusinessProfile');
const Employee = require('../models/Employee');
const PayrollAdjustment = require('../models/PayrollAdjustment');
const EmployeeLeave = require('../models/EmployeeLeave');
const AttendanceRecord = require('../models/AttendanceRecord');
const { createNotification } = require('../services/notificationService');
const { generateEmployeeToken } = require('../utils/jwt');
const { successResponse, errorResponse } = require('../utils/response');
const { resolveBusinessForActor } = require('./businessController');
const { t } = require('../i18n');

// Haversine earth distance formula in meters
function haversineDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c);
}

async function requireOrAutoCreateBusiness(req) {
  let business = await resolveBusinessForActor(req);
  if (!business && !req.isEmployeeActor && req.user) {
    business = await BusinessProfile.create({
      owner: req.user._id,
      companyName: req.user.name || 'الشركة الرئيسية',
      activityType: 'GENERAL_TRADING',
      companyGroupName: `${req.user.name || 'الشركة'} - مجموعة كل الموظفين`,
    });
  }
  return business;
}

// @desc    Employee credential login (username + password)
// @route   POST /api/hr/employee-login
// @access  Public
const loginEmployee = async (req, res, next) => {
  try {
    const { username, password, businessId } = req.body;
    const cleanUser = username.trim().toLowerCase();

    const query = { username: cleanUser, isActive: true };
    if (businessId) query.business = businessId;

    const employee = await Employee.findOne(query).select('+passwordHash').populate('business');
    if (!employee) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    const isMatch = await employee.matchPassword(password);
    if (!isMatch) {
      return errorResponse(res, 401, req.t('api.invalid_credentials'));
    }

    employee.lastLoginAt = new Date();
    await employee.save();

    const token = generateEmployeeToken(employee);
    return successResponse(res, 200, req.t('api.login_success'), {
      token,
      employee: {
        id: employee._id,
        fullName: employee.fullName,
        username: employee.username,
        phone: employee.phone,
        roleTitle: employee.roleTitle,
        department: employee.department,
        baseSalary: employee.baseSalary,
        salaryCurrency: employee.salaryCurrency,
        permissions: employee.permissions,
        geofenceEnforced: employee.geofenceEnforced,
        businessId: employee.business?._id || employee.business,
        businessProfile: employee.business,
      },
    });
  } catch (error) {
    next(error);
  }
};

// @desc    List employees with live salary net, leave & today's attendance status
// @route   GET /api/hr/employees
// @access  Private
const listEmployees = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) {
      return successResponse(res, 200, req.t('api.hr_employees_fetched'), { employees: [] });
    }

    const todayStr = new Date().toISOString().slice(0, 10);
    const monthKey = todayStr.slice(0, 7);

    const [employees, adjustments, leaves, attendance] = await Promise.all([
      Employee.find({ business: business._id }).sort({ createdAt: -1 }),
      PayrollAdjustment.find({ business: business._id, monthKey }),
      EmployeeLeave.find({ business: business._id }).sort({ startDate: -1 }),
      AttendanceRecord.find({ business: business._id, date: todayStr }),
    ]);

    const enriched = employees.map((emp) => {
      const empId = String(emp._id);
      const bonuses = adjustments
        .filter((a) => String(a.employee) === empId && a.type === 'BONUS')
        .reduce((s, a) => s + a.amount, 0);
      const deductions = adjustments
        .filter((a) => String(a.employee) === empId && a.type === 'DEDUCTION')
        .reduce((s, a) => s + a.amount, 0);
      const netSalary = Math.max(0, (emp.baseSalary || 0) + bonuses - deductions);
      const empLeaves = leaves.filter((l) => String(l.employee) === empId);
      const isOnLeaveToday = empLeaves.some(
        (l) => l.status === 'APPROVED' && l.startDate <= todayStr && l.endDate >= todayStr
      );
      const todayAttendance = attendance.find((a) => String(a.employee) === empId) || null;

      return {
        ...emp.toJSON(),
        monthlyBonuses: bonuses,
        monthlyDeductions: deductions,
        netSalary,
        isOnLeaveToday,
        leaves: empLeaves,
        todayAttendance,
      };
    });

    return successResponse(res, 200, req.t('api.hr_employees_fetched'), { employees: enriched });
  } catch (error) {
    next(error);
  }
};

// @desc    Create a new employee with username/password, permissions & salary
// @route   POST /api/hr/employees
// @access  Private (manageStaff)
const createEmployee = async (req, res, next) => {
  try {
    const business = await requireOrAutoCreateBusiness(req);
    const cleanUser = req.body.username.trim().toLowerCase();

    const duplicate = await Employee.findOne({ business: business._id, username: cleanUser });
    if (duplicate) {
      return errorResponse(res, 400, req.t('api.hr_username_taken'));
    }

    const employee = await Employee.create({
      business: business._id,
      owner: business.owner,
      fullName: req.body.fullName,
      username: cleanUser,
      phone: req.body.phone || '',
      passwordHash: req.body.password,
      roleTitle: req.body.roleTitle || 'موظف',
      department: req.body.department || 'العمليات',
      baseSalary: req.body.baseSalary || 0,
      salaryCurrency: req.body.salaryCurrency || business.currency || 'EGP',
      geofenceEnforced: req.body.geofenceEnforced !== undefined ? req.body.geofenceEnforced : true,
      permissions: req.body.permissions || {},
    });

    return successResponse(res, 201, req.t('api.hr_employee_created'), { employee });
  } catch (error) {
    next(error);
  }
};

// @desc    Update employee permissions, salary, credentials or status
// @route   PATCH /api/hr/employees/:id
// @access  Private (manageStaff)
const updateEmployee = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const employee = await Employee.findOne({ _id: req.params.id, business: business._id }).select('+passwordHash');
    if (!employee) return errorResponse(res, 404, req.t('api.user_not_found'));

    const { password, permissions, ...rest } = req.body;
    Object.assign(employee, rest);
    if (permissions) {
      employee.permissions = { ...(employee.permissions?.toObject ? employee.permissions.toObject() : employee.permissions), ...permissions };
    }
    if (password) {
      employee.passwordHash = password;
    }

    await employee.save();
    return successResponse(res, 200, req.t('api.hr_employee_updated'), { employee });
  } catch (error) {
    next(error);
  }
};

// @desc    Delete employee
// @route   DELETE /api/hr/employees/:id
// @access  Private (manageStaff)
const deleteEmployee = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const employee = await Employee.findOneAndDelete({ _id: req.params.id, business: business._id });
    if (!employee) return errorResponse(res, 404, req.t('api.user_not_found'));

    return successResponse(res, 200, req.t('api.hr_employee_deleted'), null);
  } catch (error) {
    next(error);
  }
};

// @desc    Add bonus or deduction for employee
// @route   POST /api/hr/payroll
// @access  Private (managePayroll)
const addPayrollAdjustment = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const employee = await Employee.findOne({ _id: req.body.employeeId, business: business._id });
    if (!employee) return errorResponse(res, 404, req.t('api.user_not_found'));

    const effectiveDate = req.body.effectiveDate || new Date().toISOString().slice(0, 10);
    const monthKey = effectiveDate.slice(0, 7);
    const actorName = req.isEmployeeActor ? req.employee.fullName : req.user.name;

    const adjustment = await PayrollAdjustment.create({
      business: business._id,
      owner: business.owner,
      employee: employee._id,
      type: req.body.type,
      amount: req.body.amount,
      reason: req.body.reason,
      effectiveDate,
      monthKey,
      createdByName: actorName,
    });

    return successResponse(res, 201, req.t('api.hr_payroll_saved'), { adjustment });
  } catch (error) {
    next(error);
  }
};

// @desc    Delete payroll adjustment
// @route   DELETE /api/hr/payroll/:id
// @access  Private (managePayroll)
const deletePayrollAdjustment = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const deleted = await PayrollAdjustment.findOneAndDelete({
      _id: req.params.id,
      business: business._id,
    });
    if (!deleted) return errorResponse(res, 404, req.t('api.user_not_found'));

    return successResponse(res, 200, req.t('api.hr_payroll_deleted'), null);
  } catch (error) {
    next(error);
  }
};

// @desc    List payroll adjustments for business
// @route   GET /api/hr/payroll
// @access  Private
const listPayrollAdjustments = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return successResponse(res, 200, req.t('api.hr_payroll_fetched'), { adjustments: [] });

    const adjustments = await PayrollAdjustment.find({ business: business._id })
      .populate('employee', 'fullName username roleTitle')
      .sort({ createdAt: -1 })
      .limit(200);

    return successResponse(res, 200, req.t('api.hr_payroll_fetched'), { adjustments });
  } catch (error) {
    next(error);
  }
};

// @desc    Assign leave / vacation dates to employee
// @route   POST /api/hr/leaves
// @access  Private (manageStaff)
const createEmployeeLeave = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const employee = await Employee.findOne({ _id: req.body.employeeId, business: business._id });
    if (!employee) return errorResponse(res, 404, req.t('api.user_not_found'));

    const sDate = new Date(req.body.startDate);
    const eDate = new Date(req.body.endDate);
    const daysCount = Math.max(1, Math.round((eDate - sDate) / (1000 * 60 * 60 * 24)) + 1);
    const actorName = req.isEmployeeActor ? req.employee.fullName : req.user.name;

    const leave = await EmployeeLeave.create({
      business: business._id,
      owner: business.owner,
      employee: employee._id,
      leaveType: req.body.leaveType,
      startDate: req.body.startDate,
      endDate: req.body.endDate,
      daysCount,
      status: 'APPROVED',
      notes: req.body.notes || '',
      approvedByName: actorName,
    });

    return successResponse(res, 201, req.t('api.hr_leave_saved'), { leave });
  } catch (error) {
    next(error);
  }
};

// @desc    Delete leave record
// @route   DELETE /api/hr/leaves/:id
// @access  Private (manageStaff)
const deleteEmployeeLeave = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const deleted = await EmployeeLeave.findOneAndDelete({
      _id: req.params.id,
      business: business._id,
    });
    if (!deleted) return errorResponse(res, 404, req.t('api.user_not_found'));

    return successResponse(res, 200, req.t('api.hr_leave_deleted'), null);
  } catch (error) {
    next(error);
  }
};

// @desc    List leaves for business
// @route   GET /api/hr/leaves
// @access  Private
const listEmployeeLeaves = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return successResponse(res, 200, req.t('api.hr_leaves_fetched'), { leaves: [] });

    const leaves = await EmployeeLeave.find({ business: business._id })
      .populate('employee', 'fullName username roleTitle')
      .sort({ startDate: -1 })
      .limit(200);

    return successResponse(res, 200, req.t('api.hr_leaves_fetched'), { leaves });
  } catch (error) {
    next(error);
  }
};

async function evaluateGeofenceAndNotifyManager({ business, employee, gpsEnabled, latitude, longitude, lang }) {
  if (!gpsEnabled || latitude == null || longitude == null) {
    await createNotification({
      recipientId: business.owner,
      title: t(lang, 'api.notif_gps_disabled_title'),
      message: t(lang, 'api.notif_gps_disabled_body', { employee: employee.fullName }),
      type: 'GPS_DISABLED_ALERT',
      titleKey: 'api.notif_gps_disabled_title',
      messageKey: 'api.notif_gps_disabled_body',
      messageParams: { employee: employee.fullName },
      language: lang,
    });
    return {
      gpsEnabled: false,
      inBounds: false,
      distanceMeters: null,
      status: 'GPS_DISABLED',
      managerAlerted: true,
      alertDetails: t(lang, 'api.hr_gps_required'),
    };
  }

  if (business.hqLatitude != null && business.hqLongitude != null && employee.geofenceEnforced !== false) {
    const distanceMeters = haversineDistanceMeters(
      latitude,
      longitude,
      business.hqLatitude,
      business.hqLongitude
    );
    const radius = business.geofenceRadiusMeters || 250;
    if (distanceMeters > radius) {
      await createNotification({
        recipientId: business.owner,
        title: t(lang, 'api.notif_geofence_exit_title'),
        message: t(lang, 'api.notif_geofence_exit_body', {
          employee: employee.fullName,
          distance: distanceMeters,
          radius,
        }),
        type: 'GEOFENCE_ALERT',
        titleKey: 'api.notif_geofence_exit_title',
        messageKey: 'api.notif_geofence_exit_body',
        messageParams: { employee: employee.fullName, distance: distanceMeters, radius },
        language: lang,
      });
      return {
        gpsEnabled: true,
        inBounds: false,
        distanceMeters,
        status: 'OUT_OF_BOUNDS',
        managerAlerted: true,
        alertDetails: t(lang, 'api.hr_outside_geofence', { distance: distanceMeters, radius }),
      };
    }
    return {
      gpsEnabled: true,
      inBounds: true,
      distanceMeters,
      status: 'CLOCKED_IN',
      managerAlerted: false,
      alertDetails: '',
    };
  }

  return {
    gpsEnabled: true,
    inBounds: true,
    distanceMeters: 0,
    status: 'CLOCKED_IN',
    managerAlerted: false,
    alertDetails: '',
  };
}

// @desc    Mandatory GPS Check-In to start work shift
// @route   POST /api/hr/attendance/check-in
// @access  Private (Employee or Owner testing on behalf)
const checkInWithGeofence = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const targetEmployeeId = req.isEmployeeActor ? req.employee._id : req.body.employeeId;
    const employee = await Employee.findOne({ _id: targetEmployeeId, business: business._id });
    if (!employee) return errorResponse(res, 404, req.t('api.user_not_found'));

    const evaluation = await evaluateGeofenceAndNotifyManager({
      business,
      employee,
      gpsEnabled: req.body.gpsEnabled,
      latitude: req.body.latitude,
      longitude: req.body.longitude,
      lang: req.lang || 'ar',
    });

    const todayStr = new Date().toISOString().slice(0, 10);
    const record = await AttendanceRecord.create({
      business: business._id,
      owner: business.owner,
      employee: employee._id,
      employeeName: employee.fullName,
      date: todayStr,
      status: evaluation.status,
      gpsEnabled: evaluation.gpsEnabled,
      latitude: req.body.latitude ?? null,
      longitude: req.body.longitude ?? null,
      distanceMeters: evaluation.distanceMeters,
      inBounds: evaluation.inBounds,
      outOfBoundsSince: evaluation.inBounds ? null : new Date(),
      managerAlerted: evaluation.managerAlerted,
      alertDetails: evaluation.alertDetails,
    });

    if (!evaluation.gpsEnabled || !evaluation.inBounds) {
      return res.status(403).json({
        success: false,
        message: evaluation.alertDetails,
        data: { record, evaluation },
      });
    }

    return successResponse(res, 200, req.t('api.hr_checkin_verified'), { record, evaluation });
  } catch (error) {
    next(error);
  }
};

// @desc    Periodic GPS heartbeat while clocked in — notifies manager if employee exits geofence
// @route   POST /api/hr/attendance/heartbeat
// @access  Private
const geofenceHeartbeat = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.user_not_found'));

    const targetEmployeeId = req.isEmployeeActor ? req.employee._id : req.body.employeeId;
    const employee = await Employee.findOne({ _id: targetEmployeeId, business: business._id });
    if (!employee) return errorResponse(res, 404, req.t('api.user_not_found'));

    const evaluation = await evaluateGeofenceAndNotifyManager({
      business,
      employee,
      gpsEnabled: req.body.gpsEnabled,
      latitude: req.body.latitude,
      longitude: req.body.longitude,
      lang: req.lang || 'ar',
    });

    const todayStr = new Date().toISOString().slice(0, 10);
    const record = await AttendanceRecord.findOneAndUpdate(
      { business: business._id, employee: employee._id, date: todayStr },
      {
        $set: {
          status: evaluation.status,
          gpsEnabled: evaluation.gpsEnabled,
          latitude: req.body.latitude ?? null,
          longitude: req.body.longitude ?? null,
          distanceMeters: evaluation.distanceMeters,
          inBounds: evaluation.inBounds,
          lastHeartbeatAt: new Date(),
          managerAlerted: evaluation.managerAlerted,
          alertDetails: evaluation.alertDetails,
        },
      },
      { new: true, upsert: true }
    );

    return successResponse(res, 200, evaluation.alertDetails || req.t('api.hr_checkin_verified'), {
      record,
      evaluation,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    List attendance logs and geofence violations for manager
// @route   GET /api/hr/attendance
// @access  Private
const listAttendanceRecords = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return successResponse(res, 200, req.t('api.hr_attendance_fetched'), { records: [] });

    const records = await AttendanceRecord.find({ business: business._id })
      .sort({ createdAt: -1 })
      .limit(150);

    return successResponse(res, 200, req.t('api.hr_attendance_fetched'), { records });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  loginEmployee,
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
};
