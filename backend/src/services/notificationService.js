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
  titleKey = null,
  messageKey = null,
  messageParams = null,
  language = 'ar',
}) => {
  try {
    const notification = await Notification.create({
      recipient: recipientId,
      sender: senderId,
      title,
      message,
      type,
      relatedInvoice: relatedInvoiceId,
      titleKey,
      messageKey,
      messageParams,
      language,
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
