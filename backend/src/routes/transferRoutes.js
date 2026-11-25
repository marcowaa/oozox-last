const express = require('express');
const router = express.Router();
const { protect } = require('../middlewares/authMiddleware');
const {
  initiateTransfer,
  listPendingTransfers,
  approveTransfer,
  uploadTransferChunk,
  finalizeTransferUpload,
  getTransferStatus,
  downloadTransferChunk,
  completeTransfer,
  cancelTransfer,
  revokeOtherDevices,
} = require('../controllers/transferController');

const chunkUploadParser = express.json({ limit: '16mb' });

router.use(protect);

router.post('/initiate', initiateTransfer);
router.post('/revoke-other-devices', revokeOtherDevices);
router.get('/pending', listPendingTransfers);
router.post('/:transferId/approve', approveTransfer);
router.post('/:transferId/chunks', chunkUploadParser, uploadTransferChunk);
router.post('/:transferId/finalize-upload', finalizeTransferUpload);
router.get('/:transferId/status', getTransferStatus);
router.get('/:transferId/chunks/:chunkIndex', downloadTransferChunk);
router.post('/:transferId/complete', completeTransfer);
router.post('/:transferId/cancel', cancelTransfer);

module.exports = router;
