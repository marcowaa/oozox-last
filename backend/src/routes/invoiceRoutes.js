const express = require('express');
const router = express.Router();
const {
  createInvoice,
  getInvoices,
  getInvoiceById,
  updateInvoiceStatus,
  deleteInvoice,
} = require('../controllers/invoiceController');
const { protect } = require('../middlewares/authMiddleware');
const { validate, schemas } = require('../middlewares/validateMiddleware');

// All invoice routes are protected by JWT
router.use(protect);

router
  .route('/')
  .post(validate(schemas.createInvoice), createInvoice)
  .get(validate(schemas.listInvoicesQuery, 'query'), getInvoices);

router
  .route('/:id')
  .get(validate(schemas.objectIdParam, 'params'), getInvoiceById)
  .delete(validate(schemas.objectIdParam, 'params'), deleteInvoice);

router.patch(
  '/:id/status',
  validate(schemas.objectIdParam, 'params'),
  validate(schemas.updateInvoiceStatus),
  updateInvoiceStatus
);

module.exports = router;
