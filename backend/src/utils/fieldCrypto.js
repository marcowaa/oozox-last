const crypto = require('crypto');

const PREFIX = 'ENC_GCM_V1:';
const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 12;

function getEncryptionKeyBuffer() {
  const raw = (process.env.DB_ENCRYPTION_KEY || '').trim();
  if (!raw) {
    if (process.env.NODE_ENV === 'production') {
      throw new Error('[FATAL-CRYPTO] DB_ENCRYPTION_KEY is required in production for database field encryption.');
    }
    return null;
  }

  if (raw.length < 32) {
    throw new Error('[FATAL-CRYPTO] DB_ENCRYPTION_KEY must be at least 32 characters (64 hex chars recommended).');
  }

  if (/^[0-9a-fA-F]{64}$/.test(raw)) {
    return Buffer.from(raw, 'hex');
  }

  return crypto.createHash('sha256').update(raw, 'utf8').digest();
}

function isEncryptedField(value) {
  return typeof value === 'string' && value.startsWith(PREFIX);
}

function encryptField(plainText) {
  if (typeof plainText !== 'string' || plainText.length === 0) {
    return plainText;
  }
  if (isEncryptedField(plainText)) {
    return plainText;
  }

  const key = getEncryptionKeyBuffer();
  if (!key) {
    return plainText;
  }

  const iv = crypto.randomBytes(IV_LENGTH);
  const cipher = crypto.createCipheriv(ALGORITHM, key, iv);
  const encrypted = Buffer.concat([cipher.update(plainText, 'utf8'), cipher.final()]);
  const authTag = cipher.getAuthTag();

  return `${PREFIX}${iv.toString('base64')}:${authTag.toString('base64')}:${encrypted.toString('base64')}`;
}

function decryptField(cipherText) {
  if (!isEncryptedField(cipherText)) {
    return cipherText;
  }

  const key = getEncryptionKeyBuffer();
  if (!key) {
    return '[ENCRYPTED_FIELD]';
  }

  try {
    const payload = cipherText.slice(PREFIX.length);
    const parts = payload.split(':');
    if (parts.length !== 3) {
      return '[ENCRYPTED_FIELD_MALFORMED]';
    }

    const [ivB64, tagB64, dataB64] = parts;
    const iv = Buffer.from(ivB64, 'base64');
    const authTag = Buffer.from(tagB64, 'base64');
    const encrypted = Buffer.from(dataB64, 'base64');

    const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
    decipher.setAuthTag(authTag);
    const decrypted = Buffer.concat([decipher.update(encrypted), decipher.final()]);
    return decrypted.toString('utf8');
  } catch {
    return '[ENCRYPTED_FIELD_UNREADABLE]';
  }
}

module.exports = {
  encryptField,
  decryptField,
  isEncryptedField,
  assertEncryptionKeyConfigured: getEncryptionKeyBuffer,
};
