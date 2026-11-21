const Notification = require('../models/Notification');
const { successResponse, errorResponse } = require('../utils/response');

// @desc    Get current user notifications
// @route   GET /api/notifications
// @access  Private
const getNotifications = async (req, res, next) => {
  try {
    const rawNotifications = await Notification.find({ recipient: req.user._id })
      .populate('sender', 'name phone avatar')
      .populate('relatedInvoice', 'invoiceNumber totalAmount status')
      .sort({ createdAt: -1 })
      .limit(50);

    const notifications = rawNotifications.map((doc) => {
      const obj = doc.toObject();
      if (obj.titleKey) {
        obj.title = req.t(obj.titleKey);
      }
      if (obj.messageKey) {
        const params = { ...(obj.messageParams || {}) };
        if (params.statusKey) {
          params.status = req.t(params.statusKey);
        }
        obj.message = req.t(obj.messageKey, params);
      }
      return obj;
    });

    const unreadCount = await Notification.countDocuments({
      recipient: req.user._id,
      isRead: false,
    });

    return successResponse(res, 200, req.t('api.notifications_fetched'), {
      notifications,
      unreadCount,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Mark single notification as read
// @route   PATCH /api/notifications/:id/read
// @access  Private
const markAsRead = async (req, res, next) => {
  try {
    const notification = await Notification.findOneAndUpdate(
      { _id: req.params.id, recipient: req.user._id },
      { isRead: true },
      { new: true }
    );

    if (!notification) {
      return errorResponse(res, 404, req.t('api.notification_not_found'));
    }

    return successResponse(res, 200, req.t('api.notification_marked_read'), { notification });
  } catch (error) {
    next(error);
  }
};

// @desc    Mark all notifications as read
// @route   PATCH /api/notifications/read-all
// @access  Private
const markAllAsRead = async (req, res, next) => {
  try {
    await Notification.updateMany(
      { recipient: req.user._id, isRead: false },
      { isRead: true }
    );

    return successResponse(res, 200, req.t('api.notifications_all_read'));
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getNotifications,
  markAsRead,
  markAllAsRead,
};
