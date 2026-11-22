const BusinessProfile = require('../models/BusinessProfile');
const Employee = require('../models/Employee');
const PayrollAdjustment = require('../models/PayrollAdjustment');
const EmployeeLeave = require('../models/EmployeeLeave');
const AttendanceRecord = require('../models/AttendanceRecord');
const WorkTask = require('../models/WorkTask');
const Invoice = require('../models/Invoice');
const { successResponse, errorResponse } = require('../utils/response');

// Resolve active business for authenticated owner OR scoped employee
async function resolveBusinessForActor(req) {
  if (req.isEmployeeActor && req.actorBusinessId) {
    return BusinessProfile.findById(req.actorBusinessId);
  }
  return BusinessProfile.findOne({ owner: req.actorOwnerId }).sort({ createdAt: -1 });
}

// @desc    Get active business profile (and auto-linked staff group metadata)
// @route   GET /api/business/profile
// @access  Private (Owner or Employee)
const getBusinessProfile = async (req, res, next) => {
  try {
    const profile = await resolveBusinessForActor(req);
    return successResponse(res, 200, req.t('api.business_profile_fetched'), { profile });
  } catch (error) {
    next(error);
  }
};

// @desc    Create or update enterprise business profile & HQ geofence
// @route   PUT /api/business/profile
// @access  Private (Owner or Employee with manageBusiness)
const upsertBusinessProfile = async (req, res, next) => {
  try {
    const ownerId = req.actorOwnerId;
    const payload = { ...req.body, owner: ownerId, isSetupCompleted: true };
    if (!payload.companyGroupName) {
      payload.companyGroupName = `${payload.companyName} - مجموعة كل الموظفين`;
    }

    let profile = await resolveBusinessForActor(req);
    if (profile) {
      if (String(profile.owner) !== String(ownerId)) {
        return errorResponse(res, 403, req.t('api.auth_forbidden_role', { role: 'ACTOR' }));
      }
      Object.assign(profile, payload);
      await profile.save();
    } else {
      profile = await BusinessProfile.create(payload);
    }

    return successResponse(res, 200, req.t('api.business_profile_saved'), { profile });
  } catch (error) {
    next(error);
  }
};

// @desc    Unified Executive Business Dashboard Summary (Projects, Tasks, HR, Payroll, Invoices)
// @route   GET /api/business/summary
// @access  Private
const getBusinessSummary = async (req, res, next) => {
  try {
    const profile = await resolveBusinessForActor(req);
    const ownerId = req.actorOwnerId;
    const todayStr = new Date().toISOString().slice(0, 10);
    const monthKey = todayStr.slice(0, 7);

    const [employees, adjustments, leaves, attendanceToday, tasks, invoices] = await Promise.all([
      profile ? Employee.find({ business: profile._id }) : Promise.resolve([]),
      profile ? PayrollAdjustment.find({ business: profile._id, monthKey }) : Promise.resolve([]),
      profile ? EmployeeLeave.find({ business: profile._id, status: 'APPROVED' }) : Promise.resolve([]),
      profile ? AttendanceRecord.find({ business: profile._id, date: todayStr }) : Promise.resolve([]),
      profile ? WorkTask.find({ business: profile._id }) : Promise.resolve([]),
      Invoice.find({ $or: [{ sender: ownerId }, { receiver: ownerId }] }),
    ]);

    const totalBaseSalaries = employees.reduce((acc, e) => acc + (e.baseSalary || 0), 0);
    const totalBonuses = adjustments
      .filter((a) => a.type === 'BONUS')
      .reduce((acc, a) => acc + a.amount, 0);
    const totalDeductions = adjustments
      .filter((a) => a.type === 'DEDUCTION')
      .reduce((acc, a) => acc + a.amount, 0);
    const netMonthlyPayroll = Math.max(0, totalBaseSalaries + totalBonuses - totalDeductions);

    const onLeaveCount = leaves.filter(
      (l) => l.startDate <= todayStr && l.endDate >= todayStr
    ).length;

    const geofenceAlertsCount = attendanceToday.filter(
      (a) => a.managerAlerted || !a.inBounds || !a.gpsEnabled
    ).length;

    const totalRevenue = invoices
      .filter((i) => String(i.sender) === String(ownerId) && i.status !== 'CANCELLED')
      .reduce((acc, i) => acc + (i.totalAmount || 0), 0);

    return successResponse(res, 200, req.t('api.business_summary_fetched'), {
      profile,
      metrics: {
        employeesCount: employees.length,
        activeClockedInCount: attendanceToday.filter((a) => a.status === 'CLOCKED_IN').length,
        onLeaveTodayCount: onLeaveCount,
        geofenceAlertsTodayCount: geofenceAlertsCount,
        totalBaseSalaries,
        totalBonusesThisMonth: totalBonuses,
        totalDeductionsThisMonth: totalDeductions,
        netMonthlyPayroll,
        openTasksCount: tasks.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED').length,
        completedTasksCount: tasks.filter((t) => t.status === 'COMPLETED').length,
        totalInvoicesCount: invoices.length,
        totalRevenue,
      },
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  resolveBusinessForActor,
  getBusinessProfile,
  upsertBusinessProfile,
  getBusinessSummary,
};
