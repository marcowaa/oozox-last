const express = require('express');
const router = express.Router();
const { protect, authorize } = require('../middlewares/authMiddleware');
const {
  authorizeCall,
  recordCallLifecycleEvent,
  getUserCallAvailability,
  getAdminCallBudget,
  getCallHistory,
} = require('../controllers/callController');

// Internal HMAC-authenticated signaling webhook (Go Signaling -> Backend)
router.post('/events', recordCallLifecycleEvent);

router.use(protect);
router.post('/authorize', authorizeCall);
router.get('/status', getUserCallAvailability);
router.get('/history', getCallHistory);
router.get('/budget', authorize('ADMIN'), getAdminCallBudget);

module.exports = router;
