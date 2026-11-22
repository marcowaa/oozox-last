const express = require('express');
const router = express.Router();
const {
  createWorkTask,
  listWorkTasks,
  updateWorkTaskStatus,
} = require('../controllers/taskController');
const { protectEnterprise, requirePermission } = require('../middlewares/authMiddleware');
const { validate, schemas } = require('../middlewares/validateMiddleware');

router.use(protectEnterprise);

router.get('/', listWorkTasks);
router.post(
  '/',
  requirePermission('assignTasks'),
  validate(schemas.createWorkTask),
  createWorkTask
);
router.patch(
  '/:id/status',
  validate(schemas.objectIdParam, 'params'),
  validate(schemas.updateWorkTaskStatus),
  updateWorkTaskStatus
);

module.exports = router;
