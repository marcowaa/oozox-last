const WorkTask = require('../models/WorkTask');
const { successResponse, errorResponse } = require('../utils/response');
const { resolveBusinessForActor } = require('./businessController');
const BusinessProfile = require('../models/BusinessProfile');

function computeTaskDeadline(timingMode, durationValue, lang = 'ar') {
  const now = Date.now();
  if (timingMode === 'NOW') {
    return {
      deadlineAt: new Date(now + 30 * 60 * 1000),
      deadlineDurationText: lang === 'en' ? 'Immediately (Now)' : 'الآن / حالاً (فوري)',
    };
  }
  const safeVal = Math.max(1, Number(durationValue) || 1);
  if (timingMode === 'DAYS') {
    return {
      deadlineAt: new Date(now + safeVal * 24 * 60 * 60 * 1000),
      deadlineDurationText: lang === 'en' ? `Within ${safeVal} day(s)` : `خلال ${safeVal} يوم`,
    };
  }
  return {
    deadlineAt: new Date(now + safeVal * 60 * 60 * 1000),
    deadlineDurationText: lang === 'en' ? `Within ${safeVal} hour(s)` : `خلال ${safeVal} ساعة`,
  };
}

// @desc    Create & assign work task (supports NOW, HOURS, DAYS)
// @route   POST /api/tasks
// @access  Private (assignTasks)
const createWorkTask = async (req, res, next) => {
  try {
    let business = await resolveBusinessForActor(req);
    if (!business && !req.isEmployeeActor && req.user) {
      business = await BusinessProfile.create({
        owner: req.user._id,
        companyName: req.user.name || 'الشركة الرئيسية',
        activityType: 'GENERAL_TRADING',
      });
    }

    const { timingMode, durationValue } = req.body;
    const { deadlineAt, deadlineDurationText } = computeTaskDeadline(timingMode, durationValue, req.lang);

    const task = await WorkTask.create({
      business: business._id,
      owner: business.owner,
      projectId: req.body.projectId,
      projectName: req.body.projectName,
      workGroupId: req.body.workGroupId || business.companyGroupId || 'wg_company_main',
      title: req.body.title,
      description: req.body.description || '',
      timingMode,
      durationValue,
      deadlineDurationText,
      deadlineAt,
      assignees: req.body.assignees,
      priority: timingMode === 'NOW' ? 'URGENT' : req.body.priority || 'HIGH',
      financialAmount: req.body.financialAmount || 0,
      financialNotes: req.body.financialNotes || '',
    });

    return successResponse(res, 201, req.t('api.task_created'), { task });
  } catch (error) {
    next(error);
  }
};

// @desc    List tasks for business (filter optionally by projectId or workGroupId)
// @route   GET /api/tasks
// @access  Private
const listWorkTasks = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) {
      return successResponse(res, 200, req.t('api.tasks_fetched'), { tasks: [] });
    }

    const query = { business: business._id };
    if (typeof req.query.projectId === 'string' && req.query.projectId.trim()) {
      query.projectId = req.query.projectId.trim();
    }
    if (typeof req.query.workGroupId === 'string' && req.query.workGroupId.trim()) {
      query.workGroupId = req.query.workGroupId.trim();
    }

    const tasks = await WorkTask.find(query).sort({ createdAt: -1 }).limit(200);
    return successResponse(res, 200, req.t('api.tasks_fetched'), { tasks });
  } catch (error) {
    next(error);
  }
};

// @desc    Update task status
// @route   PATCH /api/tasks/:id/status
// @access  Private
const updateWorkTaskStatus = async (req, res, next) => {
  try {
    const business = await resolveBusinessForActor(req);
    if (!business) return errorResponse(res, 404, req.t('api.task_not_found'));

    const task = await WorkTask.findOneAndUpdate(
      { _id: req.params.id, business: business._id },
      { $set: { status: req.body.status } },
      { new: true }
    );
    if (!task) return errorResponse(res, 404, req.t('api.task_not_found'));

    return successResponse(res, 200, req.t('api.task_updated'), { task });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  createWorkTask,
  listWorkTasks,
  updateWorkTaskStatus,
};
