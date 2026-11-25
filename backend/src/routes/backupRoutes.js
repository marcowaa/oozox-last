const express = require('express');
const router = express.Router();
const { protect } = require('../middlewares/authMiddleware');
const {
  listBackups,
  uploadBackup,
  downloadBackup,
  deleteBackup,
} = require('../controllers/backupController');

// Allow encrypted vault packages up to 16MB cleanly without raising global API payload limits
const backupUploadParser = express.json({ limit: '16mb' });

router.use(protect);

router.get('/', listBackups);
router.post('/', backupUploadParser, uploadBackup);
router.get('/:backupId/download', downloadBackup);
router.delete('/:backupId', deleteBackup);

module.exports = router;
