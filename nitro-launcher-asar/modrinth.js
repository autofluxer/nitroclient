const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const dns = require('dns');

try {
  dns.setDefaultResultOrder('ipv4first');
} catch (_) { /* Node < 17 */ }

const USER_AGENT = 'NitroClient/2.6.7 (https://nitrosmp.lol; support@nitrosmp.lol)';

function fetchJson(url, redirectsLeft = 5) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    const req = lib.get(url, {
      headers: {
        'User-Agent': USER_AGENT,
        Accept: 'application/json'
      },
      timeout: 15000
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        if (redirectsLeft <= 0) {
          reject(new Error('Too many redirects'));
          return;
        }
        const next = res.headers.location.startsWith('http')
          ? res.headers.location
          : new URL(res.headers.location, url).toString();
        fetchJson(next, redirectsLeft - 1).then(resolve).catch(reject);
        return;
      }
      let body = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        if (res.statusCode && res.statusCode >= 400) {
          reject(new Error(`Modrinth HTTP ${res.statusCode}`));
          return;
        }
        try {
          resolve(JSON.parse(body || '{}'));
        } catch (err) {
          reject(err);
        }
      });
    });
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Modrinth request timed out'));
    });
    req.on('error', reject);
  });
}

function downloadFile(url, dest, onProgress, redirectsLeft = 5) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    const file = fs.createWriteStream(dest);
    const req = lib.get(url, {
      headers: { 'User-Agent': USER_AGENT },
      timeout: 60000
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        file.close();
        fs.unlink(dest, () => {});
        if (redirectsLeft <= 0) {
          reject(new Error('Too many redirects'));
          return;
        }
        const next = res.headers.location.startsWith('http')
          ? res.headers.location
          : new URL(res.headers.location, url).toString();
        downloadFile(next, dest, onProgress, redirectsLeft - 1).then(resolve).catch(reject);
        return;
      }
      if (res.statusCode !== 200) {
        file.close();
        fs.unlink(dest, () => {});
        reject(new Error(`Download failed (${res.statusCode})`));
        return;
      }
      const total = parseInt(res.headers['content-length'] || '0', 10);
      let received = 0;
      res.on('data', (chunk) => {
        received += chunk.length;
        if (onProgress && total > 0) onProgress(Math.round((received / total) * 100));
      });
      res.pipe(file);
      file.on('finish', () => file.close(resolve));
    });
    req.on('timeout', () => {
      req.destroy();
      file.close();
      fs.unlink(dest, () => {});
      reject(new Error('Download timed out'));
    });
    req.on('error', (err) => {
      file.close();
      fs.unlink(dest, () => {});
      reject(err);
    });
  });
}

function buildFacets(mcVersion, loader) {
  const facets = [['project_type:mod']];
  if (mcVersion) facets.push([`versions:${mcVersion}`]);
  if (loader) facets.push([`categories:${loader}`]);
  return encodeURIComponent(JSON.stringify(facets));
}

async function searchMods(query, mcVersion, loader = 'fabric', limit = 40, offset = 0, index = 'downloads') {
  const q = encodeURIComponent(query || '');
  const facets = buildFacets(mcVersion, loader);
  const sort = encodeURIComponent(index || (query ? 'relevance' : 'downloads'));
  const url =
    `https://api.modrinth.com/v2/search?query=${q}&limit=${Math.min(100, Math.max(1, limit || 40))}`
    + `&offset=${offset || 0}&index=${sort}&facets=${facets}`;
  const data = await fetchJson(url);
  return (data.hits || []).map((hit) => ({
    id: hit.project_id,
    slug: hit.slug,
    title: hit.title,
    description: hit.description,
    author: hit.author,
    downloads: hit.downloads,
    iconUrl: hit.icon_url,
    categories: hit.categories || []
  }));
}

async function getModVersions(projectId, mcVersion, loader = 'fabric') {
  const gameVersions = encodeURIComponent(JSON.stringify([mcVersion]));
  const loaders = encodeURIComponent(JSON.stringify([loader]));
  const url =
    `https://api.modrinth.com/v2/project/${projectId}/version?game_versions=${gameVersions}&loaders=${loaders}`;
  const versions = await fetchJson(url);
  return (versions || []).map((v) => ({
    id: v.id,
    name: v.name,
    versionNumber: v.version_number,
    date: v.date_published,
    downloads: v.downloads,
    files: (v.files || []).map((f) => ({
      url: f.url,
      filename: f.filename,
      primary: !!f.primary,
      size: f.size
    }))
  }));
}

async function installModVersion(versionId, modsDir, onProgress) {
  fs.mkdirSync(modsDir, { recursive: true });
  // unused helper kept for API compatibility if called with version object path
  throw new Error('Use installLatestMod');
}

async function installLatestMod(projectId, mcVersion, loader, modsDir, onProgress) {
  fs.mkdirSync(modsDir, { recursive: true });
  const versions = await getModVersions(projectId, mcVersion, loader);
  if (!versions.length) {
    throw new Error('No compatible Modrinth version for this Minecraft build');
  }
  const file = versions[0].files.find((f) => f.primary) || versions[0].files[0];
  if (!file?.url) {
    throw new Error('Modrinth file missing download URL');
  }
  const dest = path.join(modsDir, file.filename);
  if (fs.existsSync(dest)) {
    return { path: dest, alreadyInstalled: true };
  }
  await downloadFile(file.url, dest, onProgress);
  return { path: dest, alreadyInstalled: false };
}

function listInstalledMods(modsDir) {
  if (!fs.existsSync(modsDir)) return [];
  return fs.readdirSync(modsDir)
    .filter((name) => name.toLowerCase().endsWith('.jar'))
    .map((name) => {
      const full = path.join(modsDir, name);
      const st = fs.statSync(full);
      return { name, size: st.size, path: full };
    });
}

function uninstallMod(modsDir, fileName) {
  const safe = path.basename(String(fileName || ''));
  if (!safe || !safe.toLowerCase().endsWith('.jar')) {
    throw new Error('Invalid mod file');
  }
  const full = path.join(modsDir, safe);
  if (!fs.existsSync(full)) {
    throw new Error('Mod not found');
  }
  fs.unlinkSync(full);
  return { ok: true, name: safe };
}

module.exports = {
  searchMods,
  getModVersions,
  installLatestMod,
  installModVersion,
  listInstalledMods,
  uninstallMod,
  fetchJson,
  downloadFile
};
