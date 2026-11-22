const express = require('express');
const router = express.Router();
const {
  getBusinessProfile,
  upsertBusinessProfile,
  getBusinessSummary,
} = require('../controllers/businessController');
const { protectEnterprise, requirePermission } = require('../middlewares/authMiddleware');
const { validate, schemas } = require('../middlewares/validateMiddleware');

router.use(protectEnterprise);

router.get('/profile', getBusinessProfile);
router.put(
  '/profile',
  requirePermission('manageBusiness'),
  validate(schemas.upsertBusinessProfile),
  upsertBusinessProfile
);
router.get('/summary', getBusinessSummary);

module.exports = router;
