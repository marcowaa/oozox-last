const mongoose = require('mongoose');
const crypto = require('crypto');

const chatConversationSchema = new mongoose.Schema(
  {
    conversationId: {
      type: String,
      required: true,
      unique: true,
      default: () => crypto.randomUUID(),
    },
    type: {
      type: String,
      enum: ['DIRECT', 'GROUP', 'PROJECT'],
      default: 'DIRECT',
    },
    title: {
      type: String,
      default: '',
      trim: true,
      maxlength: 140,
    },
    participants: [
      {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true,
      },
    ],
    workGroupId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'WorkGroup',
      default: null,
    },
    projectId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'ProjectWorkspace',
      default: null,
    },
    lastMessagePreview: {
      type: String,
      default: '',
      maxlength: 250,
    },
    lastMessageAt: {
      type: Date,
      default: Date.now,
      index: true,
    },
  },
  { timestamps: true }
);

chatConversationSchema.index({ participants: 1, lastMessageAt: -1 });

module.exports = mongoose.model('ChatConversation', chatConversationSchema);
