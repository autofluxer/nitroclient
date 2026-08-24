const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');

const LOCAL_META = path.join(__dirname, 'launcher-meta.json');

function readLocalMeta() {
  try {
    return JSON.parse(fs.readFileSync(LOCAL_META, 'utf8'));
  } catch (_) {
    return { launcherVersion: '1.0.0', news: [], changelog: [], partners: [] };
  }
}

function fetchJson(url, timeoutMs = 5000) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    const req = lib.get(url, { timeout: timeoutMs }, (res) => {
      if (res.statusCode !== 200) {
        res.resume();
        reject(new Error('HTTP ' + res.statusCode));
        return;
      }
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(body));
        } catch (err) {
          reject(err);
        }
      });
    });
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Timeout'));
    });
    req.on('error', reject);
  });
}

function parseVersion(value) {
  const parts = String(value || '0').split('.').map((n) => parseInt(n, 10) || 0);
  return { major: parts[0] || 0, minor: parts[1] || 0, patch: parts[2] || 0 };
}

function isNewerVersion(remote, local) {
  const a = parseVersion(remote);
  const b = parseVersion(local);
  if (a.major !== b.major) return a.major > b.major;
  if (a.minor !== b.minor) return a.minor > b.minor;
  return a.patch > b.patch;
}

function normalizePartner(server) {
  if (!server || !server.host) return null;
  const host = String(server.host).trim();
  if (!host) return null;
  return {
    id: server.id || host.toLowerCase().replace(/\W+/g, '-'),
    name: server.name || host,
    host,
    tag: server.tag || (server.featured ? 'OFFICIAL' : 'PARTNER'),
    featured: !!server.featured || String(server.tag || '').toUpperCase() === 'OFFICIAL',
    description: server.description || '',
    icon: server.icon || `https://api.mcsrvstat.us/icon/${host}`
  };
}

function mergePartners(remotePartners, localPartners) {
  const source = Array.isArray(remotePartners) && remotePartners.length
    ? remotePartners
    : (Array.isArray(localPartners) ? localPartners : []);
  return source.map(normalizePartner).filter(Boolean);
}

async function loadLauncherMeta(appVersion) {
  const local = readLocalMeta();
  const currentVersion = appVersion || local.launcherVersion || '1.0.0';
  let remote = null;

  const remoteUrl = local.remoteMetaUrl;
  if (remoteUrl) {
    try {
      remote = await fetchJson(remoteUrl);
    } catch (_) {
      /* use local fallback */
    }
  }

  const merged = {
    launcherVersion: currentVersion,
    news: remote?.news?.length ? remote.news : (local.news || []),
    changelog: remote?.changelog?.length ? remote.changelog : (local.changelog || []),
    partners: mergePartners(remote?.partners, local.partners),
    remoteMetaUrl: remoteUrl || '',
    downloadUrl: remote?.downloadUrl || local.downloadUrl || '',
    updatedAt: remote?.updatedAt || local.updatedAt || 0,
    updateAvailable: false,
    latestVersion: currentVersion
  };

  const latest = remote?.launcherVersion || local.launcherVersion;
  if (latest && isNewerVersion(latest, currentVersion)) {
    merged.updateAvailable = true;
    merged.latestVersion = latest;
    if (remote?.downloadUrl) merged.downloadUrl = remote.downloadUrl;
  }

  return merged;
}

module.exports = {
  loadLauncherMeta,
  readLocalMeta,
  isNewerVersion,
  normalizePartner,
  mergePartners,
  fetchJson
};
