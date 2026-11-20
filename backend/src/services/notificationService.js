const Notification = require('../models/Notification');

/**
 * Send and store internal in-app notification
 */
const createNotification = async ({
  recipientId,
  senderId = null,
  title,
  message,
  type = 'INVOICE_CREATED',
  relatedInvoiceId = null,
}) => {
  try {
    const notification = await Notification.create({
      recipient: recipientId,
      sender: senderId,
      title,
      message,
      type,
      relatedInvoice: relatedInvoiceId,
    });
    return notification;
  } catch (error) {
    console.error('[NOTIFICATION-SERVICE-ERROR]', error.message);
    return null;
  }
};

module.exports = {
  createNotification,
};
