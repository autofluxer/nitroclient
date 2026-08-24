const fs = require('fs');
const os = require('os');
const path = require('path');
const { HostingError, createHostingProvider } = require('./hosting-provider');

const PHASES = [
  { phase: 'prepare', percent: 12, line: 'Preparing world…' },
  { phase: 'upload', percent: 38, line: 'Packing world data…' },
  { phase: 'allocate', percent: 62, line: 'Opening a host session…' },
  { phase: 'boot', percent: 84, line: 'Starting the world…' },
  { phase: 'ready', percent: 100, line: 'World is live' }
];

function now() {
  return Date.now();
}

function safeName(name) {
  return String(name || '').trim().slice(0, 40) || 'New World';
}

function joinCode() {
  return 'NITRO-' + Math.random().toString(36).slice(2, 6).toUpperCase();
}

function readIconDataUrl(file) {
  try {
    if (!file || !fs.existsSync(file)) return '';
    const buf = fs.readFileSync(file);
    if (!buf.length) return '';
    return 'data:image/png;base64,' + buf.toString('base64');
  } catch (_) {
    return '';
  }
}

let lastCpuSample = null;

function sampleHostMetrics() {
  const cpus = os.cpus() || [];
  let idle = 0;
  let total = 0;
  for (const cpu of cpus) {
    const t = cpu.times || {};
    idle += t.idle || 0;
    total += (t.user || 0) + (t.nice || 0) + (t.sys || 0) + (t.idle || 0) + (t.irq || 0);
  }
  let cpu = 0;
  if (lastCpuSample && total > lastCpuSample.total) {
    cpu = Math.max(0, Math.min(100, Math.round((1 - (idle - lastCpuSample.idle) / (total - lastCpuSample.total)) * 1000) / 10));
  }
  lastCpuSample = { idle, total };
  const ramMax = os.totalmem();
  const ramUsed = Math.max(0, ramMax - os.freemem());
  return {
    cpu,
    ramUsed,
    ramMax,
    netIn: null,
    netOut: null,
    source: 'host-machine'
  };
}

function defaultState() {
  return {
    provider: 'local-deferred',
    apiUrl: '',
    apiToken: '',
    createdWorlds: [],
    lastWorldId: '',
    lastConfig: { mode: 'survival', cheats: false },
    session: null
  };
}

function createHostingService({ userData, getSaveRoots, getHostName, sendInvite, emit }) {
  const file = path.join(userData, 'nitro-hosting.json');
  let store = load();
  let provider = createHostingProvider(store);
  let starting = false;

  function load() {
    try {
      const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
      return { ...defaultState(), ...raw, createdWorlds: Array.isArray(raw.createdWorlds) ? raw.createdWorlds : [] };
    } catch (_) {
      return defaultState();
    }
  }

  function save() {
    const out = {
      provider: store.provider,
      apiUrl: store.apiUrl,
      apiToken: store.apiToken,
      createdWorlds: store.createdWorlds,
      lastWorldId: store.lastWorldId,
      lastConfig: store.lastConfig,
      session: store.session
    };
    fs.writeFileSync(file, JSON.stringify(out, null, 2));
  }

  function push(extra) {
    try { emit({ ...snapshot(), ...extra }); } catch (_) { /* ignore */ }
  }

  function listSaveWorlds() {
    const roots = typeof getSaveRoots === 'function' ? getSaveRoots() : [];
    const worlds = [];
    const seen = new Set();
    for (const root of roots) {
      const saves = path.join(root, 'saves');
      let names = [];
      try { names = fs.readdirSync(saves); } catch (_) { continue; }
      for (const name of names) {
        const worldDir = path.join(saves, name);
        let stat;
        try { stat = fs.statSync(worldDir); } catch (_) { continue; }
        if (!stat.isDirectory()) continue;
        const key = worldDir.toLowerCase();
        if (seen.has(key)) continue;
        seen.add(key);
        worlds.push({
          id: 'save:' + worldDir,
          name,
          source: 'saves',
          path: worldDir,
          icon: readIconDataUrl(path.join(worldDir, 'icon.png')),
          lastPlayed: stat.mtimeMs,
          mode: 'Survival',
          version: '',
          cheats: false
        });
      }
    }
    return worlds.sort((a, b) => (b.lastPlayed || 0) - (a.lastPlayed || 0));
  }

  function listWorlds() {
    const created = (store.createdWorlds || []).map((w) => ({
      id: w.id,
      name: w.name,
      source: 'created',
      path: w.path || '',
      icon: w.icon || '',
      lastPlayed: w.updatedAt || w.createdAt,
      mode: w.mode || 'Survival',
      version: 'Nitro',
      cheats: !!w.cheats
    }));
    return [...created, ...listSaveWorlds()];
  }

  function findWorld(id) {
    return listWorlds().find((w) => w.id === id) || null;
  }

  function snapshot() {
    const session = store.session;
    const worlds = listWorlds();
    return {
      worlds,
      selectedWorldId: store.lastWorldId || worlds[0]?.id || '',
      config: { ...store.lastConfig },
      session: session ? { ...session } : null,
      machine: sampleHostMetrics(),
      providerId: provider.id,
      capabilities: provider.capabilities || { publicJoin: false },
      starting,
      hostName: (typeof getHostName === 'function' ? getHostName() : 'Player') || 'Player'
    };
  }

  function setConfig(partial) {
    store.lastConfig = {
      mode: partial.mode || store.lastConfig.mode || 'survival',
      cheats: partial.cheats != null ? !!partial.cheats : !!store.lastConfig.cheats
    };
    save();
    return snapshot();
  }

  function selectWorld(id) {
    if (id && findWorld(id)) store.lastWorldId = id;
    save();
    return snapshot();
  }

  function createWorld({ name, mode, cheats } = {}) {
    const world = {
      id: 'new:' + Date.now().toString(36),
      name: safeName(name),
      mode: mode === 'creative' ? 'Creative' : (mode === 'adventure' ? 'Adventure' : 'Survival'),
      cheats: !!cheats,
      createdAt: now(),
      updatedAt: now(),
      path: '',
      icon: ''
    };
    store.createdWorlds.unshift(world);
    store.lastWorldId = world.id;
    store.lastConfig.mode = world.mode.toLowerCase();
    store.lastConfig.cheats = world.cheats;
    save();
    push();
    return snapshot();
  }

  function wait(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async function start({ worldId, mode, cheats } = {}) {
    if (starting) throw new HostingError('BUSY', 'Already starting a world');
    if (store.session?.status === 'online' || store.session?.status === 'starting') {
      throw new HostingError('BUSY', 'A world is already hosting');
    }
    const world = findWorld(worldId || store.lastWorldId);
    if (!world) throw new HostingError('NO_WORLD', 'Select a world first');
    setConfig({ mode, cheats });
    store.lastWorldId = world.id;
    starting = true;
    store.session = {
      id: '',
      status: 'starting',
      worldId: world.id,
      worldName: world.name,
      mode: store.lastConfig.mode,
      cheats: store.lastConfig.cheats,
      address: '',
      port: 0,
      joinCode: joinCode(),
      players: [],
      playersOnline: 0,
      playersMax: 8,
      metrics: sampleHostMetrics(),
      progress: { phase: 'prepare', percent: 0, line: 'Starting…' },
      startedAt: 0,
      providerId: provider.id,
      publicJoin: !!(provider.capabilities && provider.capabilities.publicJoin)
    };
    save();
    push({ event: 'starting' });

    try {
      for (const step of PHASES.slice(0, 2)) {
        store.session.progress = step;
        push({ event: 'progress' });
        await wait(220);
      }

      const lease = await provider.allocateSession({
        world,
        config: store.lastConfig,
        hostName: snapshot().hostName
      });
      store.session.id = lease.id;
      store.session.joinCode = lease.joinCode || store.session.joinCode;
      store.session.address = lease.address || '';
      store.session.port = lease.port || 0;
      store.session.progress = PHASES[2];
      push({ event: 'progress' });

      await provider.uploadWorld({
        world,
        session: lease,
        onProgress: (info) => {
          if (!store.session) return;
          store.session.progress = {
            phase: info.phase || 'upload',
            percent: info.percent ?? 50,
            line: info.line || 'Uploading…'
          };
          push({ event: 'progress' });
        }
      });

      store.session.progress = PHASES[3];
      push({ event: 'progress' });
      await wait(180);
      const live = await provider.startSession(lease);
      store.session.status = 'online';
      store.session.startedAt = now();
      store.session.address = live.address || store.session.address;
      store.session.port = live.port || store.session.port;
      store.session.joinCode = live.joinCode || store.session.joinCode;
      store.session.progress = PHASES[4];
      await refreshLive(false);
      starting = false;
      save();
      push({ event: 'online' });
      return snapshot();
    } catch (err) {
      starting = false;
      store.session = {
        ...(store.session || {}),
        status: 'error',
        error: err.message || 'Could not start hosting'
      };
      save();
      push({ event: 'error' });
      throw err;
    }
  }

  async function stop() {
    const session = store.session;
    if (!session) return snapshot();
    store.session = { ...session, status: 'stopping' };
    push({ event: 'stopping' });
    try {
      if (session.id) await provider.stopSession(session);
    } catch (_) { /* still clear local session */ }
    store.session = null;
    starting = false;
    save();
    push({ event: 'stopped' });
    return snapshot();
  }

  async function refreshLive(notify = true) {
    const session = store.session;
    if (!session || (session.status !== 'online' && session.status !== 'starting')) return snapshot();
    session.metrics = sampleHostMetrics();
    if (session.id) {
      try {
        const remote = await provider.fetchStatus(session);
        if (remote) {
          session.status = remote.status || session.status;
          session.players = Array.isArray(remote.players) ? remote.players : session.players;
          session.playersOnline = remote.playersOnline != null ? remote.playersOnline : session.players.length;
          session.playersMax = remote.playersMax || session.playersMax;
          if (remote.address) session.address = remote.address;
          if (remote.joinCode) session.joinCode = remote.joinCode;
        }
      } catch (_) { /* keep last known */ }
      try {
        const metrics = await provider.fetchMetrics(session);
        if (metrics && typeof metrics === 'object') {
          session.metrics = { ...session.metrics, ...metrics, source: 'provider' };
        }
      } catch (_) { /* host-machine metrics stay */ }
    }
    save();
    if (notify) push({ event: 'tick' });
    return snapshot();
  }

  function joinInfo() {
    const session = store.session;
    if (!session) return { address: '', joinCode: '', line: '' };
    const host = (session.address || '').trim();
    const port = session.port && session.port !== 25565 ? ':' + session.port : '';
    const address = host ? host + port : '';
    const line = address
      ? ('Join ' + session.worldName + ' — ' + address + (session.joinCode ? '  ·  ' + session.joinCode : ''))
      : ('I\'m hosting ' + session.worldName + ' on Nitro'
        + (session.joinCode ? ' — code ' + session.joinCode : '')
        + '. Public join opens when hosting is connected.');
    return { address, joinCode: session.joinCode || '', line, worldName: session.worldName };
  }

  async function inviteFriend(name) {
    const info = joinInfo();
    if (!store.session || store.session.status !== 'online') {
      throw new HostingError('OFFLINE', 'Start hosting before inviting friends');
    }
    if (typeof sendInvite !== 'function') {
      throw new HostingError('NO_FRIENDS', 'Friends are not available');
    }
    await sendInvite(name, info.line);
    store.session.invites = Array.isArray(store.session.invites) ? store.session.invites : [];
    if (!store.session.invites.includes(name)) store.session.invites.push(name);
    save();
    push();
    return snapshot();
  }

  function reloadProvider() {
    provider = createHostingProvider(store);
  }

  return {
    snapshot,
    listWorlds,
    selectWorld,
    createWorld,
    setConfig,
    start,
    stop,
    refreshLive,
    joinInfo,
    inviteFriend,
    reloadProvider,
    HostingError
  };
}

module.exports = { createHostingService, HostingError, sampleHostMetrics };
