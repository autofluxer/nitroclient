/**
 * Hosting provider contract for Nitro World Hosting.
 *
 * Swap the adapter in hosting.js — do not put provider-specific HTTP,
 * upload, or server-control logic in the UI.
 *
 * Required methods:
 *   allocateSession({ world, config, hostName }) -> SessionLease
 *   uploadWorld({ world, session, onProgress }) -> void
 *   startSession(session) -> SessionLease
 *   stopSession(session) -> void
 *   fetchStatus(session) -> { status, players, playersOnline, playersMax }
 *   fetchMetrics(session) -> { cpu, ramUsed, ramMax, netIn, netOut }
 *
 * SessionLease:
 *   { id, address, port, joinCode, capabilities }
 */

class HostingError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'HostingError';
    this.code = code;
  }
}

function notConfigured(action) {
  return new HostingError(
    'NOT_CONFIGURED',
    action + ' needs a hosting provider. Set apiUrl in nitro-hosting.json or pass a provider.'
  );
}

function createRemoteHostProvider(config = {}) {
  const baseUrl = String(config.apiUrl || '').replace(/\/+$/, '');
  const token = String(config.apiToken || '');

  async function api(method, pathname, body) {
    if (!baseUrl) throw notConfigured(method + ' ' + pathname);
    const headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      'User-Agent': 'NitroClient/hosting'
    };
    if (token) headers.Authorization = 'Bearer ' + token;
    const res = await fetch(baseUrl + pathname, {
      method,
      headers,
      body: body == null ? undefined : JSON.stringify(body)
    });
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (_) { data = { raw: text }; }
    if (!res.ok) {
      throw new HostingError(
        data?.code || 'PROVIDER_HTTP',
        data?.message || ('Hosting provider HTTP ' + res.status)
      );
    }
    return data;
  }

  return {
    id: 'remote-http',
    capabilities: { publicJoin: true, metrics: true, upload: true },
    async allocateSession(req) {
      return api('POST', '/v1/sessions', req);
    },
    async uploadWorld({ world, session, onProgress }) {
      onProgress?.({ phase: 'upload', percent: 10, line: 'Uploading world…' });
      await api('POST', '/v1/sessions/' + encodeURIComponent(session.id) + '/world', {
        worldId: world.id,
        name: world.name,
        path: world.path || ''
      });
      onProgress?.({ phase: 'upload', percent: 70, line: 'World uploaded' });
    },
    async startSession(session) {
      return api('POST', '/v1/sessions/' + encodeURIComponent(session.id) + '/start', {});
    },
    async stopSession(session) {
      return api('POST', '/v1/sessions/' + encodeURIComponent(session.id) + '/stop', {});
    },
    async fetchStatus(session) {
      return api('GET', '/v1/sessions/' + encodeURIComponent(session.id));
    },
    async fetchMetrics(session) {
      return api('GET', '/v1/sessions/' + encodeURIComponent(session.id) + '/metrics');
    }
  };
}

/**
 * Local adapter used until a remote hosting API is configured.
 * It owns the session lifecycle and join-code so the UI can run,
 * but it does not open ports or start a public Minecraft server.
 */
function createLocalDeferredProvider() {
  const sessions = new Map();

  function lease(id) {
    const row = sessions.get(id);
    if (!row) throw new HostingError('NO_SESSION', 'Hosting session is gone');
    return row;
  }

  return {
    id: 'local-deferred',
    capabilities: { publicJoin: false, metrics: false, upload: false },
    async allocateSession({ world, config, hostName }) {
      const id = 'nhs_' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
      const joinCode = 'NITRO-' + Math.random().toString(36).slice(2, 6).toUpperCase();
      const row = {
        id,
        address: '',
        port: 0,
        joinCode,
        hostName: hostName || 'Player',
        worldId: world.id,
        worldName: world.name,
        mode: config.mode || 'survival',
        cheats: !!config.cheats,
        capabilities: { publicJoin: false, metrics: false, upload: false },
        status: 'allocated',
        players: [],
        createdAt: Date.now()
      };
      sessions.set(id, row);
      return { ...row };
    },
    async uploadWorld({ onProgress }) {
      onProgress?.({ phase: 'prepare', percent: 35, line: 'Reading world files…' });
    },
    async startSession(session) {
      const row = lease(session.id);
      row.status = 'online';
      row.startedAt = Date.now();
      if (row.hostName && !row.players.some((p) => p.name === row.hostName)) {
        row.players = [{ name: row.hostName, host: true }];
      }
      return { ...row };
    },
    async stopSession(session) {
      sessions.delete(session.id);
    },
    async fetchStatus(session) {
      const row = lease(session.id);
      return {
        status: row.status,
        players: row.players,
        playersOnline: row.players.length,
        playersMax: 8,
        address: row.address,
        joinCode: row.joinCode
      };
    },
    async fetchMetrics() {
      return null;
    }
  };
}

function createHostingProvider(config = {}) {
  if (config.provider === 'remote' || (config.apiUrl && config.provider !== 'local-deferred')) {
    return createRemoteHostProvider(config);
  }
  return createLocalDeferredProvider();
}

module.exports = {
  HostingError,
  createHostingProvider,
  createRemoteHostProvider,
  createLocalDeferredProvider
};
