const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const os = require('os');

const SESSION_FILE = 'microsoft-session.enc';

function getSessionPath(userDataDir) {
  return path.join(userDataDir, SESSION_FILE);
}

function getFallbackKey() {
  const seed = [
    os.hostname(),
    os.userInfo().username,
    process.platform,
    'nitro-client-session-v1'
  ].join('|');
  return crypto.createHash('sha256').update(seed).digest();
}

function createSecureStore(safeStorage) {
  return {
    isAvailable() {
      return safeStorage?.isEncryptionAvailable?.() === true;
    },

    save(userDataDir, payload) {
      const json = JSON.stringify(payload);
      const filePath = getSessionPath(userDataDir);

      if (safeStorage?.isEncryptionAvailable?.()) {
        const encrypted = safeStorage.encryptString(json);
        fs.writeFileSync(filePath, encrypted);
        return;
      }

      const iv = crypto.randomBytes(12);
      const key = getFallbackKey();
      const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
      const encrypted = Buffer.concat([cipher.update(json, 'utf8'), cipher.final()]);
      const tag = cipher.getAuthTag();
      fs.writeFileSync(filePath, Buffer.concat([iv, tag, encrypted]));
    },

    load(userDataDir) {
      const filePath = getSessionPath(userDataDir);
      if (!fs.existsSync(filePath)) return null;

      const raw = fs.readFileSync(filePath);

      if (safeStorage?.isEncryptionAvailable?.()) {
        try {
          const json = safeStorage.decryptString(raw);
          return JSON.parse(json);
        } catch (_) {
          return null;
        }
      }

      try {
        const iv = raw.subarray(0, 12);
        const tag = raw.subarray(12, 28);
        const data = raw.subarray(28);
        const key = getFallbackKey();
        const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
        decipher.setAuthTag(tag);
        const json = Buffer.concat([decipher.update(data), decipher.final()]).toString('utf8');
        return JSON.parse(json);
      } catch (_) {
        return null;
      }
    },

    clear(userDataDir) {
      const filePath = getSessionPath(userDataDir);
      try {
        fs.unlinkSync(filePath);
      } catch (_) { /* ignore */ }

      const legacy = path.join(userDataDir, 'microsoft-session.json');
      try {
        fs.unlinkSync(legacy);
      } catch (_) { /* ignore */ }
    }
  };
}

module.exports = {
  createSecureStore,
  getSessionPath
};
