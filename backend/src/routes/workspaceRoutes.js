const express = require('express');
const { protect, authorize } = require('../middlewares/authMiddleware');
const {
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
} = require('../controllers/workspaceController');

const groupRouter = express.Router();
groupRouter.use(protect);
groupRouter.get('/', listWorkGroups);
groupRouter.post('/', createWorkGroup);

const projectRouter = express.Router();
projectRouter.use(protect);
projectRouter.get('/', listProjects);
projectRouter.post('/', createProject);
projectRouter.patch('/:projectId', updateProject);

const chatRouter = express.Router();
chatRouter.use(protect);
chatRouter.get('/', listConversations);
chatRouter.post('/', createOrGetConversation);
chatRouter.get('/:conversationId/messages', listMessages);
chatRouter.post('/:conversationId/messages', sendMessage);
chatRouter.post('/:conversationId/read', markConversationRead);

const contentRouter = express.Router();
contentRouter.get('/', getPublicCmsContent);
contentRouter.put('/', protect, authorize('ADMIN'), updateCmsContent);

module.exports = {
  groupRouter,
  projectRouter,
  chatRouter,
  contentRouter,
};
