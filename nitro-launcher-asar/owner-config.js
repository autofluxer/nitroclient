const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { normalizePartner, readLocalMeta } = require('./launcher-meta');

const DEFAULT_REMOTE = 'https://nitrosmp.lol/api/nitro-launcher.json';

function workingPath(userData) {
  return path.join(userData, 'nitro-owner-live.json');
}

function publishSettingsPath(userData) {
  return path.join(userData, 'nitro-owner-publish.json');
}

function authConfigPath() {
  return path.join(__dirname, 'owner-auth.json');
}

function hashPassword(password) {
  return crypto.createHash('sha256').update(String(password || ''), 'utf8').digest('hex');
}

function verifyOwnerPassword(password) {
  try {
    const auth = JSON.parse(fs.readFileSync(authConfigPath(), 'utf8'));
    const expected = String(auth.passwordSha256 || '').toLowerCase();
    if (!expected) return false;
    const actual = hashPassword(password);
    return expected.length === actual.length
      && crypto.timingSafeEqual(Buffer.from(expected, 'utf8'), Buffer.from(actual, 'utf8'));
  } catch (_) {
    return false;
  }
}

function defaultLiveConfig() {
  const local = readLocalMeta();
  return {
    launcherVersion: local.launcherVersion || '2.5.0',
    remoteMetaUrl: local.remoteMetaUrl || DEFAULT_REMOTE,
    downloadUrl: local.downloadUrl || 'https://nitrosmp.lol/download',
    partners: Array.isArray(local.partners) ? local.partners.map(normalizePartner).filter(Boolean) : [],
    news: Array.isArray(local.news) ? local.news : [],
    changelog: Array.isArray(local.changelog) ? local.changelog : [],
    updatedAt: Date.now()
  };
}

function loadWorkingCopy(userData) {
  try {
    const raw = JSON.parse(fs.readFileSync(workingPath(userData), 'utf8'));
    return sanitizeLiveConfig(raw);
  } catch (_) {
    const fresh = defaultLiveConfig();
    saveWorkingCopy(userData, fresh);
    return fresh;
  }
}

function sanitizeLiveConfig(input) {
  const base = defaultLiveConfig();
  const partners = Array.isArray(input?.partners)
    ? input.partners.map(normalizePartner).filter(Boolean)
    : base.partners;
  const news = Array.isArray(input?.news)
    ? input.news.map((n) => ({
      title: String(n?.title || n?.headline || 'Update').slice(0, 120),
      body: String(n?.body || n?.summary || n?.text || '').slice(0, 2000)
    }))
    : base.news;
  const changelog = Array.isArray(input?.changelog)
    ? input.changelog.map((c) => ({
      version: String(c?.version || '1.0.0').slice(0, 32),
      date: String(c?.date || '').slice(0, 32),
      items: Array.isArray(c?.items) ? c.items.map((i) => String(i).slice(0, 300)) : []
    }))
    : base.changelog;

  return {
    launcherVersion: String(input?.launcherVersion || base.launcherVersion),
    remoteMetaUrl: String(input?.remoteMetaUrl || base.remoteMetaUrl),
    downloadUrl: String(input?.downloadUrl || base.downloadUrl || ''),
    partners,
    news,
    changelog,
    updatedAt: Number(input?.updatedAt) || Date.now()
  };
}

function saveWorkingCopy(userData, config) {
  const clean = sanitizeLiveConfig({ ...config, updatedAt: Date.now() });
  fs.writeFileSync(workingPath(userData), JSON.stringify(clean, null, 2), 'utf8');
  return clean;
}

function loadPublishSettings(userData) {
  try {
    return JSON.parse(fs.readFileSync(publishSettingsPath(userData), 'utf8'));
  } catch (_) {
    const local = readLocalMeta();
    return {
      publishUrl: local.remoteMetaUrl || DEFAULT_REMOTE,
      publishToken: '',
      lastPublishAt: 0,
      lastPublishError: ''
    };
  }
}

function savePublishSettings(userData, settings) {
  const next = {
    publishUrl: String(settings?.publishUrl || DEFAULT_REMOTE).trim(),
    publishToken: String(settings?.publishToken || ''),
    lastPublishAt: Number(settings?.lastPublishAt) || 0,
    lastPublishError: String(settings?.lastPublishError || '')
  };
  fs.writeFileSync(publishSettingsPath(userData), JSON.stringify(next, null, 2), 'utf8');
  return next;
}

module.exports = {
  DEFAULT_REMOTE,
  hashPassword,
  verifyOwnerPassword,
  defaultLiveConfig,
  loadWorkingCopy,
  saveWorkingCopy,
  sanitizeLiveConfig,
  loadPublishSettings,
  savePublishSettings,
  workingPath
};
