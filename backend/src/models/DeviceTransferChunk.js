const mongoose = require('mongoose');

const deviceTransferChunkSchema = new mongoose.Schema(
  {
    transferId: {
      type: String,
      required: true,
      index: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    chunkIndex: {
      type: Number,
      required: true,
      min: 0,
    },
    totalChunks: {
      type: Number,
      required: true,
      min: 1,
    },
    cipherChunkBase64: {
      type: String,
      required: true,
    },
    ivBase64: {
      type: String,
      required: true,
    },
    chunkSha256: {
      type: String,
      required: true,
    },
    byteSize: {
      type: Number,
      required: true,
      min: 1,
    },
    expiresAt: {
      type: Date,
      required: true,
      index: { expires: 0 },
    },
  },
  { timestamps: true }
);

deviceTransferChunkSchema.index({ transferId: 1, chunkIndex: 1 }, { unique: true });

module.exports = mongoose.model('DeviceTransferChunk', deviceTransferChunkSchema);
