const WorkGroup = require('../models/WorkGroup');
const ProjectWorkspace = require('../models/ProjectWorkspace');
const ChatConversation = require('../models/ChatConversation');
const ChatMessage = require('../models/ChatMessage');
const CmsContent = require('../models/CmsContent');
const User = require('../models/User');
const { successResponse, errorResponse } = require('../utils/response');
const { publishRealtimeEvent } = require('../services/realtimeBus');
const { sendDataPushToUser } = require('../services/fcmPushService');

function isGroupMember(group, userId) {
  const uid = String(userId);
  return (
    String(group.ownerId) === uid ||
    (Array.isArray(group.members) && group.members.some((m) => String(m.userId?._id || m.userId) === uid))
  );
}

function isProjectMember(project, userId) {
  const uid = String(userId);
  return (
    String(project.ownerId) === uid ||
    (Array.isArray(project.members) && project.members.some((m) => String(m.userId?._id || m.userId) === uid))
  );
}

// ==================== WORK GROUPS ====================

const listWorkGroups = async (req, res, next) => {
  try {
    const groups = await WorkGroup.find({
      isArchived: false,
      $or: [{ ownerId: req.user._id }, { 'members.userId': req.user._id }],
    })
      .populate('members.userId', 'name phone phoneE164 avatar')
      .sort({ updatedAt: -1 });
    return successResponse(res, 200, 'Work groups fetched', { groups });
  } catch (err) {
    next(err);
  }
};

const createWorkGroup = async (req, res, next) => {
  try {
    const { name, description = '', memberIds = [], companyGroupName = '', isDirectMessagesLocked = false } =
      req.body || {};
    if (!name || String(name).trim().length < 2) {
      return errorResponse(res, 400, 'Group name is required');
    }

    const uniqueIds = new Set([String(req.user._id), ...(Array.isArray(memberIds) ? memberIds.map(String) : [])]);
    const members = Array.from(uniqueIds).map((uid) => ({
      userId: uid,
      role: uid === String(req.user._id) ? 'OWNER' : 'MEMBER',
    }));

    const group = await WorkGroup.create({
      name: String(name).trim(),
      description: String(description || '').trim(),
      ownerId: req.user._id,
      members,
      companyGroupName: String(companyGroupName || '').trim(),
      isDirectMessagesLocked: Boolean(isDirectMessagesLocked),
    });

    await publishRealtimeEvent({
      type: 'group:updated',
      targetUserIds: Array.from(uniqueIds),
      payload: { groupId: group.groupId, action: 'CREATED', name: group.name },
    });

    return successResponse(res, 201, 'Work group created', { group });
  } catch (err) {
    next(err);
  }
};

// ==================== PROJECTS ====================

const listProjects = async (req, res, next) => {
  try {
    const projects = await ProjectWorkspace.find({
      $or: [{ ownerId: req.user._id }, { 'members.userId': req.user._id }],
    })
      .populate('members.userId', 'name phone phoneE164 avatar')
      .sort({ updatedAt: -1 });
    return successResponse(res, 200, 'Projects fetched', { projects });
  } catch (err) {
    next(err);
  }
};

const createProject = async (req, res, next) => {
  try {
    const {
      title,
      description = '',
      category = 'GENERAL',
      budgetAmount = 0,
      currency = 'EGP',
      workGroupId = null,
      memberIds = [],
    } = req.body || {};
    if (!title || String(title).trim().length < 2) {
      return errorResponse(res, 400, 'Project title is required');
    }

    const uniqueIds = new Set([String(req.user._id), ...(Array.isArray(memberIds) ? memberIds.map(String) : [])]);
    const members = Array.from(uniqueIds).map((uid) => ({
      userId: uid,
      role: uid === String(req.user._id) ? 'OWNER' : 'CONTRIBUTOR',
    }));

    const project = await ProjectWorkspace.create({
      title: String(title).trim(),
      description: String(description || '').trim(),
      category: String(category || 'GENERAL').trim(),
      budgetAmount: Number(budgetAmount) || 0,
      currency: String(currency || 'EGP').trim(),
      ownerId: req.user._id,
      workGroupId: workGroupId || null,
      members,
    });

    await publishRealtimeEvent({
      type: 'project:update',
      targetUserIds: Array.from(uniqueIds),
      payload: { projectId: project.projectId, title: project.title, status: project.status },
    });

    return successResponse(res, 201, 'Project created', { project });
  } catch (err) {
    next(err);
  }
};

const updateProject = async (req, res, next) => {
  try {
    const project = await ProjectWorkspace.findOne({ projectId: req.params.projectId });
    if (!project) {
      return errorResponse(res, 404, 'Project not found');
    }
    if (!isProjectMember(project, req.user._id)) {
      return errorResponse(res, 403, 'Access denied to this project');
    }

    const { title, description, status, progressPercent, budgetAmount, milestones } = req.body || {};
    if (title && String(title).trim().length >= 2) project.title = String(title).trim();
    if (description !== undefined) project.description = String(description);
    if (status && ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED'].includes(status)) {
      project.status = status;
    }
    if (progressPercent !== undefined) {
      project.progressPercent = Math.max(0, Math.min(100, Number(progressPercent) || 0));
    }
    if (budgetAmount !== undefined) {
      project.budgetAmount = Math.max(0, Number(budgetAmount) || 0);
    }
    if (Array.isArray(milestones)) {
      project.milestones = milestones;
    }

    await project.save();

    const targetIds = project.members.map((m) => String(m.userId));
    await publishRealtimeEvent({
      type: 'project:update',
      targetUserIds: targetIds,
      payload: {
        projectId: project.projectId,
        status: project.status,
        progressPercent: project.progressPercent,
      },
    });

    return successResponse(res, 200, 'Project updated', { project });
  } catch (err) {
    next(err);
  }
};

// ==================== CHATS & REALTIME MESSAGING ====================

const listConversations = async (req, res, next) => {
  try {
    const conversations = await ChatConversation.find({
      participants: req.user._id,
    })
      .populate('participants', 'name phone phoneE164 avatar')
      .sort({ lastMessageAt: -1 });
    return successResponse(res, 200, 'Conversations fetched', { conversations });
  } catch (err) {
    next(err);
  }
};

const createOrGetConversation = async (req, res, next) => {
  try {
    const { type = 'DIRECT', peerUserId, participantIds = [], title = '', workGroupId = null, projectId = null } =
      req.body || {};

    if (type === 'DIRECT') {
      if (!peerUserId) {
        return errorResponse(res, 400, 'peerUserId is required for DIRECT chat');
      }
      const peer = await User.findById(peerUserId).select('_id');
      if (!peer) {
        return errorResponse(res, 404, req.t('api.user_not_found'));
      }

      let conv = await ChatConversation.findOne({
        type: 'DIRECT',
        participants: { $all: [req.user._id, peer._id], $size: 2 },
      }).populate('participants', 'name phone phoneE164 avatar');

      if (!conv) {
        conv = await ChatConversation.create({
          type: 'DIRECT',
          participants: [req.user._id, peer._id],
        });
        await conv.populate('participants', 'name phone phoneE164 avatar');
      }
      return successResponse(res, 200, 'Conversation ready', { conversation: conv });
    }

    const allParticipants = Array.from(
      new Set([String(req.user._id), ...(Array.isArray(participantIds) ? participantIds.map(String) : [])])
    );
    const conv = await ChatConversation.create({
      type: type === 'PROJECT' ? 'PROJECT' : 'GROUP',
      title: String(title || '').trim() || 'مجموعة عمل',
      participants: allParticipants,
      workGroupId: workGroupId || null,
      projectId: projectId || null,
    });
    await conv.populate('participants', 'name phone phoneE164 avatar');
    return successResponse(res, 201, 'Group conversation created', { conversation: conv });
  } catch (err) {
    next(err);
  }
};

const listMessages = async (req, res, next) => {
  try {
    const conv = await ChatConversation.findOne({ conversationId: req.params.conversationId });
    if (!conv) {
      return errorResponse(res, 404, 'Conversation not found');
    }
    if (!conv.participants.some((p) => String(p) === String(req.user._id))) {
      return errorResponse(res, 403, 'Access denied to this conversation');
    }

    const limit = Math.min(100, Math.max(1, Number(req.query.limit) || 50));
    const messages = await ChatMessage.find({
      conversationId: conv.conversationId,
      deletedAt: null,
    })
      .sort({ createdAt: -1 })
      .limit(limit);

    return successResponse(res, 200, 'Messages fetched', {
      messages: messages.reverse(),
    });
  } catch (err) {
    next(err);
  }
};

const sendMessage = async (req, res, next) => {
  try {
    const conv = await ChatConversation.findOne({ conversationId: req.params.conversationId });
    if (!conv) {
      return errorResponse(res, 404, 'Conversation not found');
    }
    if (!conv.participants.some((p) => String(p) === String(req.user._id))) {
      return errorResponse(res, 403, 'Access denied to this conversation');
    }

    const { content, messageType = 'TEXT', clientTempId = '', mediaMeta = {} } = req.body || {};
    if (typeof content !== 'string' || !content.trim()) {
      return errorResponse(res, 400, 'Message content is required');
    }

    // Deduplicate optimistic retries from mobile SyncQueue
    if (clientTempId) {
      const existing = await ChatMessage.findOne({
        conversationId: conv.conversationId,
        senderId: req.user._id,
        clientTempId: String(clientTempId),
      });
      if (existing) {
        return successResponse(res, 200, 'Message already delivered', { message: existing, duplicate: true });
      }
    }

    const msg = await ChatMessage.create({
      conversationId: conv.conversationId,
      senderId: req.user._id,
      clientTempId: String(clientTempId || '').slice(0, 128),
      messageType: ['TEXT', 'IMAGE', 'VIDEO', 'VOICE', 'DOCUMENT', 'INVOICE_TAG', 'SYSTEM'].includes(messageType)
        ? messageType
        : 'TEXT',
      content: content.trim(),
      mediaMeta,
      deliveredTo: [req.user._id],
      readBy: [req.user._id],
    });

    conv.lastMessagePreview = messageType === 'TEXT' ? content.trim().slice(0, 140) : `[${messageType}]`;
    conv.lastMessageAt = msg.createdAt;
    await conv.save();

    const recipients = conv.participants.filter((p) => String(p) !== String(req.user._id));
    await publishRealtimeEvent({
      type: 'chat:message',
      targetUserIds: conv.participants.map(String),
      payload: {
        conversationId: conv.conversationId,
        messageId: msg.messageId,
        senderId: String(req.user._id),
        senderName: req.user.name,
        messageType: msg.messageType,
        content: msg.content,
        mediaMeta: msg.mediaMeta,
        createdAt: msg.createdAt,
      },
    });

    await Promise.all(
      recipients.map((rid) =>
        sendDataPushToUser({
          userId: rid,
          highPriority: false,
          data: {
            type: 'chat_message',
            conversationId: conv.conversationId,
            messageId: msg.messageId,
            senderId: String(req.user._id),
            senderName: req.user.name || 'OOZOX',
          },
          notification: {
            title: req.user.name || 'رسالة جديدة في OOZOX',
            body: msg.messageType === 'TEXT' ? msg.content.slice(0, 100) : `[${msg.messageType}]`,
          },
        })
      )
    );

    return successResponse(res, 201, 'Message sent', { message: msg });
  } catch (err) {
    next(err);
  }
};

const markConversationRead = async (req, res, next) => {
  try {
    const conv = await ChatConversation.findOne({ conversationId: req.params.conversationId });
    if (!conv) {
      return errorResponse(res, 404, 'Conversation not found');
    }
    if (!conv.participants.some((p) => String(p) === String(req.user._id))) {
      return errorResponse(res, 403, 'Access denied to this conversation');
    }

    await ChatMessage.updateMany(
      { conversationId: conv.conversationId, readBy: { $ne: req.user._id } },
      { $addToSet: { readBy: req.user._id, deliveredTo: req.user._id } }
    );

    await publishRealtimeEvent({
      type: 'chat:read',
      targetUserIds: conv.participants.map(String),
      payload: {
        conversationId: conv.conversationId,
        readerId: String(req.user._id),
        readAt: new Date().toISOString(),
      },
    });

    return successResponse(res, 200, 'Conversation marked read', { conversationId: conv.conversationId });
  } catch (err) {
    next(err);
  }
};

// ==================== WEB PORTAL CMS ====================

const getPublicCmsContent = async (_req, res, next) => {
  try {
    let doc = await CmsContent.findOne({ key: 'primary' });
    if (!doc) {
      doc = await CmsContent.create({ key: 'primary' });
    }
    return successResponse(res, 200, 'CMS content fetched', { content: doc });
  } catch (err) {
    next(err);
  }
};

const updateCmsContent = async (req, res, next) => {
  try {
    const { hero, heroTitleAr, heroTitleEn, themeConfig, goldRates, features, announcements, tutorials, jobs } =
      req.body || {};
    const updateFields = { updatedBy: req.user._id };
    if (hero && typeof hero === 'object') {
      updateFields.hero = hero;
      if (typeof hero.title === 'string') {
        updateFields.heroTitleAr = hero.title;
      }
    }
    if (typeof heroTitleAr === 'string') {
      updateFields.heroTitleAr = heroTitleAr.trim();
      if (!updateFields.hero) {
        updateFields['hero.title'] = heroTitleAr.trim();
      }
    }
    if (typeof heroTitleEn === 'string') {
      updateFields.heroTitleEn = heroTitleEn.trim();
    }
    if (themeConfig && typeof themeConfig === 'object') updateFields.themeConfig = themeConfig;
    if (goldRates && typeof goldRates === 'object') updateFields.goldRates = goldRates;
    if (Array.isArray(features)) updateFields.features = features;
    if (Array.isArray(announcements)) updateFields.announcements = announcements;
    if (Array.isArray(tutorials)) updateFields.tutorials = tutorials;
    if (Array.isArray(jobs)) updateFields.jobs = jobs;

    const doc = await CmsContent.findOneAndUpdate(
      { key: 'primary' },
      { $set: updateFields },
      { upsert: true, new: true }
    );
    return successResponse(res, 200, 'CMS content updated', { content: doc });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  listWorkGroups,
  createWorkGroup,
  listProjects,
  createProject,
  updateProject,
  listConversations,
  createOrGetConversation,
  listMessages,
  sendMessage,
  markConversationRead,
  getPublicCmsContent,
  updateCmsContent,
};
