const http = require('http');
const https = require('https');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { shell } = require('electron');
const { URL, URLSearchParams } = require('url');

const REDIRECT_URI = 'http://127.0.0.1:43821/callback';
const CALLBACK_PORT = 43821;
const SCOPES = [
  'user-read-currently-playing',
  'user-read-playback-state',
  'user-modify-playback-state',
  'user-read-recently-played',
  'playlist-read-private',
  'user-library-read'
].join(' ');

let activeServer = null;

function configDir(gameDir) {
  return path.join(gameDir, 'config');
}

function clientIdPath(gameDir) {
  return path.join(configDir(gameDir), 'nitro-spotify.json');
}

function tokensPath(gameDir) {
  return path.join(configDir(gameDir), 'nitro-spotify-tokens.json');
}

function readJson(file, fallback = {}) {
  try {
    if (!fs.existsSync(file)) return { ...fallback };
    return { ...fallback, ...JSON.parse(fs.readFileSync(file, 'utf8')) };
  } catch (_) {
    return { ...fallback };
  }
}

function writeJson(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(data, null, 2), 'utf8');
}

function saveClientId(gameDir, clientId) {
  writeJson(clientIdPath(gameDir), { clientId: String(clientId || '').trim() });
}

function getClientId(gameDir) {
  const env = process.env.NITRO_SPOTIFY_CLIENT_ID;
  if (env && String(env).trim()) return String(env).trim();
  return String(readJson(clientIdPath(gameDir)).clientId || '').trim();
}

function loadTokens(gameDir) {
  return readJson(tokensPath(gameDir), {
    accessToken: '',
    refreshToken: '',
    expiresAtEpochMs: 0,
    tokenType: 'Bearer',
    scope: ''
  });
}

function saveTokens(gameDir, tokens) {
  writeJson(tokensPath(gameDir), tokens);
}

function clearTokens(gameDir) {
  const file = tokensPath(gameDir);
  if (fs.existsSync(file)) fs.unlinkSync(file);
}

function pkceVerifier() {
  return crypto.randomBytes(32).toString('base64url');
}

function pkceChallenge(verifier) {
  return crypto.createHash('sha256').update(verifier).digest('base64url');
}

function postForm(url, body) {
  return new Promise((resolve, reject) => {
    const data = new URLSearchParams(body).toString();
    const req = https.request(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(data)
      }
    }, (res) => {
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', (c) => { raw += c; });
      res.on('end', () => {
        if (res.statusCode >= 400) {
          reject(new Error(`Spotify token HTTP ${res.statusCode}`));
          return;
        }
        try {
          resolve(JSON.parse(raw || '{}'));
        } catch (err) {
          reject(err);
        }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

function getJson(url, accessToken) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'application/json'
      }
    }, (res) => {
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', (c) => { raw += c; });
      res.on('end', () => {
        if (res.statusCode === 204) {
          resolve(null);
          return;
        }
        if (res.statusCode >= 400) {
          reject(new Error(`Spotify API HTTP ${res.statusCode}`));
          return;
        }
        try {
          resolve(JSON.parse(raw || '{}'));
        } catch (err) {
          reject(err);
        }
      });
    });
    req.on('error', reject);
  });
}

function stopServer() {
  if (!activeServer) return;
  try { activeServer.close(); } catch (_) { /* ignore */ }
  activeServer = null;
}

function waitForCallback(expectedState, timeoutMs = 180000) {
  return new Promise((resolve, reject) => {
    stopServer();
    const timer = setTimeout(() => {
      stopServer();
      reject(new Error('Spotify login timed out'));
    }, timeoutMs);

    activeServer = http.createServer((req, res) => {
      try {
        const u = new URL(req.url, REDIRECT_URI);
        if (u.pathname !== '/callback') {
          res.writeHead(404);
          res.end('Not found');
          return;
        }
        const error = u.searchParams.get('error');
        const code = u.searchParams.get('code');
        const state = u.searchParams.get('state');
        const html = `<!doctype html><html><body style="font-family:Segoe UI,sans-serif;background:#0b1018;color:#fff;display:grid;place-items:center;height:100vh;margin:0">
          <div style="text-align:center"><h1>Nitro Client</h1><p>You can close this tab and return to the launcher.</p></div>
        </body></html>`;
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(html);
        clearTimeout(timer);
        stopServer();
        if (error) {
          reject(new Error(error === 'access_denied' ? 'Spotify login cancelled' : 'Spotify login failed'));
          return;
        }
        if (!code || state !== expectedState) {
          reject(new Error('Spotify login failed'));
          return;
        }
        resolve(code);
      } catch (err) {
        clearTimeout(timer);
        stopServer();
        reject(err);
      }
    });

    activeServer.on('error', (err) => {
      clearTimeout(timer);
      stopServer();
      reject(err);
    });

    activeServer.listen(CALLBACK_PORT, '127.0.0.1');
  });
}

async function beginLogin(gameDir, clientIdInput) {
  const clientId = String(clientIdInput || getClientId(gameDir) || '').trim();
  if (!clientId) {
    throw new Error('Add your Spotify Client ID first (developer.spotify.com/dashboard).');
  }
  saveClientId(gameDir, clientId);

  const verifier = pkceVerifier();
  const challenge = pkceChallenge(verifier);
  const state = crypto.randomBytes(16).toString('hex');

  const authUrl = 'https://accounts.spotify.com/authorize'
    + `?client_id=${encodeURIComponent(clientId)}`
    + '&response_type=code'
    + `&redirect_uri=${encodeURIComponent(REDIRECT_URI)}`
    + `&scope=${encodeURIComponent(SCOPES)}`
    + `&state=${encodeURIComponent(state)}`
    + '&code_challenge_method=S256'
    + `&code_challenge=${encodeURIComponent(challenge)}`;

  const codePromise = waitForCallback(state);
  await shell.openExternal(authUrl);
  const code = await codePromise;

  const tokenJson = await postForm('https://accounts.spotify.com/api/token', {
    grant_type: 'authorization_code',
    code,
    redirect_uri: REDIRECT_URI,
    client_id: clientId,
    code_verifier: verifier
  });

  const tokens = {
    accessToken: tokenJson.access_token || '',
    refreshToken: tokenJson.refresh_token || '',
    expiresAtEpochMs: Date.now() + ((tokenJson.expires_in || 3600) * 1000),
    tokenType: tokenJson.token_type || 'Bearer',
    scope: tokenJson.scope || SCOPES
  };
  if (!tokens.refreshToken) {
    throw new Error('Spotify did not return a refresh token');
  }
  saveTokens(gameDir, tokens);
  return getStatus(gameDir);
}

async function ensureAccessToken(gameDir) {
  const clientId = getClientId(gameDir);
  let tokens = loadTokens(gameDir);
  if (!tokens.refreshToken) throw new Error('Spotify is not connected');
  if (tokens.accessToken && Date.now() + 30000 < (tokens.expiresAtEpochMs || 0)) {
    return tokens.accessToken;
  }
  const json = await postForm('https://accounts.spotify.com/api/token', {
    grant_type: 'refresh_token',
    refresh_token: tokens.refreshToken,
    client_id: clientId
  });
  tokens = {
    ...tokens,
    accessToken: json.access_token || tokens.accessToken,
    refreshToken: json.refresh_token || tokens.refreshToken,
    expiresAtEpochMs: Date.now() + ((json.expires_in || 3600) * 1000),
    tokenType: json.token_type || tokens.tokenType,
    scope: json.scope || tokens.scope
  };
  saveTokens(gameDir, tokens);
  return tokens.accessToken;
}

async function getStatus(gameDir) {
  const tokens = loadTokens(gameDir);
  const connected = !!(tokens.refreshToken && String(tokens.refreshToken).trim());
  const status = {
    connected,
    clientId: getClientId(gameDir),
    track: null,
    displayName: ''
  };
  if (!connected) return status;
  try {
    const access = await ensureAccessToken(gameDir);
    const me = await getJson('https://api.spotify.com/v1/me', access);
    status.displayName = me?.display_name || me?.id || 'Spotify';
    const playing = await getJson('https://api.spotify.com/v1/me/player/currently-playing', access);
    if (playing?.item) {
      status.track = {
        name: playing.item.name || '',
        artist: (playing.item.artists || []).map((a) => a.name).filter(Boolean).join(', '),
        albumArt: playing.item.album?.images?.[0]?.url || '',
        isPlaying: !!playing.is_playing
      };
    }
  } catch (_) {
    /* still report connected */
  }
  return status;
}

async function playback(gameDir, action) {
  const access = await ensureAccessToken(gameDir);
  const map = {
    previous: { method: 'POST', path: '/v1/me/player/previous' },
    next: { method: 'POST', path: '/v1/me/player/next' },
    pause: { method: 'PUT', path: '/v1/me/player/pause' },
    play: { method: 'PUT', path: '/v1/me/player/play' }
  };
  const op = map[action];
  if (!op) throw new Error('Unknown Spotify action');
  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: 'api.spotify.com',
      path: op.path,
      method: op.method,
      headers: { Authorization: `Bearer ${access}`, 'Content-Length': 0 }
    }, (res) => {
      res.resume();
      if (res.statusCode >= 400 && res.statusCode !== 404) {
        reject(new Error(`Spotify control failed (${res.statusCode})`));
        return;
      }
      resolve({ ok: true });
    });
    req.on('error', reject);
    req.end();
  });
}

function disconnect(gameDir) {
  stopServer();
  clearTokens(gameDir);
  return { connected: false, clientId: getClientId(gameDir), track: null };
}

module.exports = {
  beginLogin,
  getStatus,
  playback,
  disconnect,
  saveClientId,
  getClientId,
  REDIRECT_URI
};
