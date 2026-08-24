const { Auth, validate, tokenUtils } = require('msmc');

let secureStore = null;
let memorySession = null;

function initSecureStore(store) {
  secureStore = store;
}

function loadSession(userDataDir) {
  if (memorySession) return memorySession;
  if (!secureStore) return null;
  return secureStore.load(userDataDir);
}

function saveSession(userDataDir, mclc, remember = true) {
  const payload = { mclc, savedAt: Date.now() };
  memorySession = payload;

  if (!remember) return;

  if (secureStore) {
    secureStore.save(userDataDir, payload);
  }
}

function clearSession(userDataDir) {
  memorySession = null;
  if (secureStore) {
    secureStore.clear(userDataDir);
  }
}

function toPublicAccount(mclc) {
  return {
    username: mclc.name,
    uuid: mclc.uuid,
    type: mclc.meta?.type || 'msa',
    expiresAt: mclc.meta?.exp || null
  };
}

async function refreshSavedSession(userDataDir) {
  const saved = loadSession(userDataDir);
  if (!saved?.mclc) return null;

  const authManager = new Auth('none');
  try {
    const minecraft = await tokenUtils.fromMclcToken(authManager, saved.mclc, true);
    if (!minecraft) return null;

    const mclc = minecraft.mclc(true);
    saveSession(userDataDir, mclc, true);
    return {
      username: mclc.name,
      uuid: mclc.uuid,
      mclc
    };
  } catch (_) {
    return null;
  }
}

async function loginMicrosoft(parentWindow) {
  const authManager = new Auth('select_account');
  const windowProperties = parentWindow
    ? { parent: parentWindow, modal: true, width: 520, height: 720 }
    : undefined;

  const xboxManager = await authManager.launch('electron', windowProperties);
  const token = await xboxManager.getMinecraft();
  const mclc = token.mclc(true);

  return {
    username: mclc.name,
    uuid: mclc.uuid,
    mclc
  };
}

async function resolveMicrosoftAuth(userDataDir, parentWindow, options = {}) {
  const { forceLogin = false, remember = true } = options;

  if (!forceLogin) {
    const refreshed = await refreshSavedSession(userDataDir);
    if (refreshed) return refreshed;
  }

  const fresh = await loginMicrosoft(parentWindow);
  saveSession(userDataDir, fresh.mclc, remember);
  return fresh;
}

function getMicrosoftAccount(userDataDir) {
  const saved = loadSession(userDataDir);
  if (!saved?.mclc) return null;
  if (!validate(saved.mclc)) return { ...toPublicAccount(saved.mclc), expired: true };
  return { ...toPublicAccount(saved.mclc), expired: false };
}

function getSecurityInfo(safeStorageAvailable) {
  return {
    passwordNeverSeen: true,
    officialMicrosoftLogin: true,
    tokensStayLocal: true,
    encryptedStorage: safeStorageAvailable,
    sentToNitroServers: false,
    canRevokeAnytime: true
  };
}

module.exports = {
  initSecureStore,
  loginMicrosoft,
  resolveMicrosoftAuth,
  refreshSavedSession,
  getMicrosoftAccount,
  getSecurityInfo,
  clearSession,
  loadSession
};
