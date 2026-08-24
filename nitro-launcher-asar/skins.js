const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const crypto = require('crypto');

const MAX_SKINS = 40;
const MAX_BYTES = 2 * 1024 * 1024;

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function downloadBuffer(url, hops = 0) {
  return new Promise((resolve, reject) => {
    if (hops > 4) {
      reject(new Error('Too many redirects'));
      return;
    }
    const lib = String(url).startsWith('http://') ? http : https;
    const req = lib.get(url, {
      timeout: 10000,
      headers: { 'User-Agent': 'NitroClient/2.6.7 (skins)' }
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        const next = new URL(res.headers.location, url).toString();
        res.resume();
        downloadBuffer(next, hops + 1).then(resolve, reject);
        return;
      }
      if (res.statusCode >= 400) {
        reject(new Error('Could not download skin'));
        return;
      }
      const chunks = [];
      let size = 0;
      res.on('data', (c) => {
        size += c.length;
        if (size > MAX_BYTES) {
          req.destroy();
          reject(new Error('Skin file is too large'));
          return;
        }
        chunks.push(c);
      });
      res.on('end', () => resolve(Buffer.concat(chunks)));
    });
    req.on('error', reject);
    req.on('timeout', () => {
      req.destroy(new Error('Skin download timed out'));
    });
  });
}

function isPng(buf) {
  return Buffer.isBuffer(buf) && buf.length > 24
    && buf[0] === 0x89 && buf[1] === 0x50 && buf[2] === 0x4e && buf[3] === 0x47;
}

function defaultState() {
  return { activeId: '', items: [] };
}

function createSkinsService(userData, hooks) {
  const dir = path.join(userData, 'skins');
  const file = path.join(userData, 'nitro-skins.json');
  ensureDir(dir);
  let state = load();

  function load() {
    try {
      const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
      return {
        activeId: String(raw.activeId || ''),
        items: Array.isArray(raw.items) ? raw.items : []
      };
    } catch (_) {
      return defaultState();
    }
  }

  function save() {
    fs.writeFileSync(file, JSON.stringify(state, null, 2));
  }

  function pngPath(id) {
    return path.join(dir, id + '.png');
  }

  function toItem(row) {
    let dataUrl = '';
    try {
      const buf = fs.readFileSync(pngPath(row.id));
      dataUrl = 'data:image/png;base64,' + buf.toString('base64');
    } catch (_) { /* missing file */ }
    return { ...row, dataUrl };
  }

  function snapshot() {
    return {
      activeId: state.activeId,
      items: state.items.map(toItem)
    };
  }

  function addBuffer(buf, meta) {
    if (!isPng(buf)) throw new Error('That file is not a PNG skin');
    if (state.items.length >= MAX_SKINS) throw new Error('Skin library is full (40)');
    const id = crypto.randomBytes(6).toString('hex');
    fs.writeFileSync(pngPath(id), buf);
    const item = {
      id,
      name: String(meta.name || 'Custom skin').slice(0, 40),
      model: meta.model === 'slim' ? 'slim' : 'classic',
      source: meta.source || 'file',
      username: String(meta.username || '').slice(0, 16),
      createdAt: Date.now()
    };
    state.items.unshift(item);
    if (!state.activeId) state.activeId = id;
    save();
    return snapshot();
  }

  async function addFromUsername(name) {
    const username = String(name || '').trim();
    if (!/^[A-Za-z0-9_]{3,16}$/.test(username)) {
      throw new Error('Enter a Minecraft username (3–16 letters)');
    }
    const buf = await downloadBuffer('https://mc-heads.net/skin/' + encodeURIComponent(username));
    return addBuffer(buf, {
      name: username + "'s skin",
      source: 'username',
      username
    });
  }

  async function addFromUrl(url) {
    const href = String(url || '').trim();
    if (!/^https?:\/\//i.test(href)) throw new Error('Enter a username or a skin image URL');
    const buf = await downloadBuffer(href);
    let name = 'Custom skin';
    try {
      name = path.basename(new URL(href).pathname).replace(/\.png$/i, '') || name;
    } catch (_) { /* keep default */ }
    return addBuffer(buf, { name: name.slice(0, 40), source: 'url' });
  }

  async function addFromQuery(query) {
    const value = String(query || '').trim();
    if (!value) throw new Error('Enter a username or skin URL');
    if (/^https?:\/\//i.test(value)) return addFromUrl(value);
    return addFromUsername(value);
  }

  function addFromFile(filePath) {
    const src = String(filePath || '');
    if (!src || !fs.existsSync(src)) throw new Error('Skin file not found');
    const buf = fs.readFileSync(src);
    const name = path.basename(src, path.extname(src)).slice(0, 40) || 'Custom skin';
    return addBuffer(buf, { name, source: 'file' });
  }

  function rename(id, name) {
    const item = state.items.find((s) => s.id === id);
    if (!item) throw new Error('Skin not found');
    item.name = String(name || item.name).slice(0, 40);
    save();
    return snapshot();
  }

  function setModel(id, model) {
    const item = state.items.find((s) => s.id === id);
    if (!item) throw new Error('Skin not found');
    item.model = model === 'slim' ? 'slim' : 'classic';
    save();
    return snapshot();
  }

  function remove(id) {
    state.items = state.items.filter((s) => s.id !== id);
    if (state.activeId === id) state.activeId = state.items[0]?.id || '';
    try { fs.unlinkSync(pngPath(id)); } catch (_) { /* ignore */ }
    save();
    return snapshot();
  }

  function writeCslConfig(root) {
    const cfg = path.join(root, 'CustomSkinLoader.json');
    if (fs.existsSync(cfg)) return;
    fs.writeFileSync(cfg, JSON.stringify({
      version: '14.15',
      enable: true,
      loadlist: [
        { name: 'LocalSkin', type: 'Legacy', root: 'LocalSkin' },
        { name: 'Mojang', type: 'MojangAPI' }
      ]
    }, null, 2));
  }

  function applyToGame(id) {
    const item = state.items.find((s) => s.id === id);
    if (!item) throw new Error('Select a skin first');
    const png = pngPath(id);
    if (!fs.existsSync(png)) throw new Error('Skin file is missing');
    const username = (hooks.getUsername?.() || 'Player').replace(/[^\w]/g, '') || 'Player';
    const dirs = hooks.getGameDirs?.() || [];
    for (const gameDir of dirs) {
      if (!gameDir || !fs.existsSync(gameDir)) continue;
      const csl = path.join(gameDir, 'CustomSkinLoader');
      const local = path.join(csl, 'LocalSkin', 'skins');
      ensureDir(local);
      writeCslConfig(csl);
      fs.copyFileSync(png, path.join(local, username + '.png'));
    }
    state.activeId = id;
    save();
    return snapshot();
  }

  return {
    snapshot,
    addFromQuery,
    addFromFile,
    rename,
    setModel,
    remove,
    applyToGame
  };
}

module.exports = { createSkinsService };
