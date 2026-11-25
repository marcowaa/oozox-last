const mongoose = require('mongoose');
const crypto = require('crypto');

const workGroupSchema = new mongoose.Schema(
  {
    groupId: {
      type: String,
      required: true,
      unique: true,
      default: () => crypto.randomUUID(),
    },
    name: {
      type: String,
      required: true,
      trim: true,
      maxlength: 140,
    },
    description: {
      type: String,
      default: '',
      trim: true,
      maxlength: 500,
    },
    ownerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    members: [
      {
        userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
        role: { type: String, enum: ['OWNER', 'ADMIN', 'MEMBER'], default: 'MEMBER' },
        joinedAt: { type: Date, default: Date.now },
      },
    ],
    companyGroupName: {
      type: String,
      default: '',
      trim: true,
      maxlength: 140,
    },
    isDirectMessagesLocked: {
      type: Boolean,
      default: false,
    },
    avatar: {
      type: String,
      default: '',
    },
    isArchived: {
      type: Boolean,
      default: false,
    },
  },
  { timestamps: true }
);

workGroupSchema.index({ 'members.userId': 1, updatedAt: -1 });

module.exports = mongoose.model('WorkGroup', workGroupSchema);
