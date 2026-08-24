const fs = require('fs');
const path = require('path');
const https = require('https');
const crypto = require('crypto');

const ONLINE_MS = 90_000;
const MAX_TEXT = 400;
const MAX_CHAT = 80;
// Temporary: find/add by username even without live presence (cracked/offline accounts).
const ALLOW_NAME_SEARCH = true;
// ntfy.sh times out in some regions (including this network). Publish/poll every host.
const RELAYS = ['ntfy.envs.net', 'ntfy.adminforge.de', 'ntfy.sh'];

function now() {
  return Date.now();
}

function normName(name) {
  return String(name || '').trim().slice(0, 16);
}

function keyName(name) {
  return normName(name).toLowerCase();
}

function validName(name) {
  return /^[A-Za-z0-9_]{3,16}$/.test(normName(name));
}

function topic(kind, key) {
  const digest = crypto.createHash('sha256')
    .update('nitro-friends:' + kind + ':' + key, 'utf8')
    .digest('hex')
    .slice(0, 16);
  return 'nfr' + kind.charAt(0) + digest;
}

function profileTopic(name) {
  return topic('p', keyName(name));
}

function inboxTopic(name) {
  return topic('i', keyName(name));
}

function chatTopic(a, b) {
  return topic('c', [keyName(a), keyName(b)].sort().join('|'));
}

function requestRelay(host, method, urlPath, body, timeoutMs) {
  return new Promise((resolve, reject) => {
    const payload = body == null ? null : Buffer.from(body, 'utf8');
    const req = https.request({
      hostname: host,
      path: urlPath,
      method,
      headers: {
        'User-Agent': 'NitroClient/2.6.7',
        ...(payload ? {
          'Content-Type': 'text/plain; charset=utf-8',
          'Content-Length': payload.length
        } : { 'Accept': 'application/x-ndjson, application/json' })
      },
      timeout: timeoutMs
    }, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const raw = Buffer.concat(chunks).toString('utf8');
        if (res.statusCode >= 400) {
          reject(new Error('Friends network HTTP ' + res.statusCode));
          return;
        }
        resolve(raw);
      });
    });
    req.on('error', reject);
    req.on('timeout', () => {
      req.destroy(new Error('Friends network timeout'));
    });
    if (payload) req.write(payload);
    req.end();
  });
}

function parsePoll(raw) {
  const out = [];
  const pushRow = (row) => {
    if (!row || typeof row.message !== 'string') return;
    try {
      const msg = JSON.parse(row.message);
      if (msg && msg.v === 1 && msg.t) out.push(msg);
    } catch (_) { /* ignore */ }
  };
  const trim = String(raw || '').trim();
  if (!trim) return out;
  if (trim.startsWith('[')) {
    try {
      const arr = JSON.parse(trim);
      if (Array.isArray(arr)) arr.forEach(pushRow);
      return out;
    } catch (_) { /* fall through */ }
  }
  for (const line of trim.split('\n')) {
    const piece = line.trim();
    if (!piece) continue;
    try { pushRow(JSON.parse(piece)); } catch (_) { /* ignore */ }
  }
  return out;
}

async function publish(topicName, message) {
  const pathName = '/' + encodeURIComponent(topicName);
  const payload = JSON.stringify(message);
  const results = await Promise.allSettled(
    RELAYS.map((host) => requestRelay(host, 'POST', pathName, payload, host === 'ntfy.sh' ? 800 : 4000))
  );
  if (!results.some((r) => r.status === 'fulfilled')) {
    throw new Error('Friends network unavailable');
  }
}

async function poll(topicName, since) {
  const pathName = '/' + encodeURIComponent(topicName)
    + '/json?poll=1&since=' + encodeURIComponent(since || '12h');
  const out = [];
  await new Promise((resolve) => {
    let pending = RELAYS.length;
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      resolve();
    };
    const timer = setTimeout(finish, 2500);
    for (const host of RELAYS) {
      requestRelay(host, 'GET', pathName, null, host === 'ntfy.sh' ? 800 : 2400)
        .then((raw) => { out.push(...parsePoll(raw)); })
        .catch(() => {})
        .finally(() => {
          pending -= 1;
          if (pending <= 0) {
            clearTimeout(timer);
            finish();
          }
        });
    }
  });
  return out;
}

function withTimeout(promise, ms, fallback) {
  return Promise.race([
    promise,
    new Promise((resolve) => setTimeout(() => resolve(fallback), ms))
  ]);
}

function defaultState() {
  return {
    me: 'Player',
    friends: {},
    incoming: [],
    outgoing: [],
    chats: {}
  };
}

function createFriendsService(userData, emit) {
  const file = path.join(userData, 'nitro-friends.json');
  let state = load();
  let playingServer = '';
  let playing = false;
  let timer = null;
  let lastInbox = 0;
  let lastPresence = 0;
  let lastChat = 0;
  let lastOutgoing = 0;
  let activeChat = '';

  function load() {
    try {
      const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
      return {
        ...defaultState(),
        ...raw,
        friends: raw.friends && typeof raw.friends === 'object' ? raw.friends : {},
        incoming: Array.isArray(raw.incoming) ? raw.incoming : [],
        outgoing: Array.isArray(raw.outgoing) ? raw.outgoing : [],
        chats: raw.chats && typeof raw.chats === 'object' ? raw.chats : {}
      };
    } catch (_) {
      return defaultState();
    }
  }

  function save() {
    fs.writeFileSync(file, JSON.stringify(state, null, 2));
  }

  function snapshot() {
    const friends = Object.values(state.friends).map((f) => ({
      ...f,
      online: isOnline(f.lastSeen),
      statusText: statusText(f)
    })).sort((a, b) => Number(b.online) - Number(a.online) || a.name.localeCompare(b.name));
    return {
      me: state.me,
      friends,
      incoming: state.incoming,
      outgoing: state.outgoing,
      chats: state.chats,
      onlineCount: friends.filter((f) => f.online).length
    };
  }

  function push() {
    try { emit(snapshot()); } catch (_) { /* ignore */ }
  }

  function isOnline(lastSeen) {
    return lastSeen && (now() - lastSeen) < ONLINE_MS;
  }

  function statusText(friend) {
    if (isOnline(friend.lastSeen)) {
      if (friend.status === 'ingame' && friend.server) return 'In game · ' + friend.server;
      if (friend.status === 'ingame') return 'In game';
      return 'Online';
    }
    if (friend.lastSeen) return 'Offline';
    return 'Offline';
  }

  function setIdentity(name) {
    const next = validName(name) ? normName(name) : 'Player';
    if (state.me !== next) {
      state.me = next;
      save();
    }
    heartbeat().catch(() => {});
    push();
    return snapshot();
  }

  function setPlaying(server) {
    playing = true;
    playingServer = String(server || '').trim().slice(0, 64);
    heartbeat().catch(() => {});
  }

  function setIdle() {
    playing = false;
    playingServer = '';
    heartbeat().catch(() => {});
  }

  async function heartbeat() {
    if (!validName(state.me)) return;
    await publish(profileTopic(state.me), {
      v: 1,
      t: 'p',
      n: state.me,
      s: playing ? 'ingame' : 'online',
      sv: playing ? playingServer : '',
      at: now()
    });
  }

  async function republishOutgoing() {
    if (!validName(state.me) || !state.outgoing.length) return;
    for (const row of state.outgoing) {
      const to = normName(row.to);
      if (!validName(to)) continue;
      try {
        await publish(inboxTopic(to), {
          v: 1, t: 'req', n: state.me, to, at: now()
        });
      } catch (_) { /* keep trying next tick */ }
    }
  }

  function applyPresence(msg) {
    const key = keyName(msg.n);
    if (!key || key === keyName(state.me) || !state.friends[key]) return;
    state.friends[key].lastSeen = msg.at || now();
    state.friends[key].status = msg.s || 'online';
    state.friends[key].server = String(msg.sv || '').slice(0, 64);
    if (msg.n) state.friends[key].name = normName(msg.n);
  }

  function addIncoming(from) {
    const name = normName(from);
    const key = keyName(name);
    if (!key || key === keyName(state.me)) return;
    if (state.friends[key]) return;
    if (state.incoming.some((r) => keyName(r.from) === key)) return;
    state.incoming.push({ from: name, at: now() });
  }

  function becomeFriends(name) {
    const display = normName(name);
    const key = keyName(display);
    if (!key || key === keyName(state.me)) return;
    if (!state.friends[key]) {
      state.friends[key] = {
        name: display,
        addedAt: now(),
        lastSeen: 0,
        status: 'offline',
        server: ''
      };
    }
    state.incoming = state.incoming.filter((r) => keyName(r.from) !== key);
    state.outgoing = state.outgoing.filter((r) => keyName(r.to) !== key);
    if (!state.chats[key]) state.chats[key] = [];
  }

  function appendChat(from, to, text, id, at) {
    const other = keyName(from) === keyName(state.me) ? to : from;
    const key = keyName(other);
    if (!state.friends[key] && keyName(from) !== keyName(state.me) && keyName(to) !== keyName(state.me)) {
      return;
    }
    if (!state.chats[key]) state.chats[key] = [];
    if (id && state.chats[key].some((m) => m.id === id)) return;
    state.chats[key].push({
      id: id || crypto.randomBytes(4).toString('hex'),
      from: normName(from),
      text: String(text || '').slice(0, MAX_TEXT),
      at: at || now()
    });
    if (state.chats[key].length > MAX_CHAT) {
      state.chats[key] = state.chats[key].slice(-MAX_CHAT);
    }
  }

  async function syncInbox() {
    if (!validName(state.me)) return;
    const msgs = await poll(inboxTopic(state.me), '24h');
    let changed = false;
    for (const msg of msgs) {
      if (msg.t === 'req' && keyName(msg.to) === keyName(state.me)) {
        const before = state.incoming.length;
        addIncoming(msg.n);
        if (state.incoming.length !== before) changed = true;
      }
      if (msg.t === 'ok' && keyName(msg.to) === keyName(state.me)) {
        const key = keyName(msg.n);
        if (!state.friends[key]) {
          becomeFriends(msg.n);
          changed = true;
        }
      }
    }
    if (changed) {
      save();
      push();
    }
  }

  async function syncFriendsPresence() {
    const names = Object.values(state.friends).map((f) => f.name);
    for (const name of names) {
      try {
        const msgs = await poll(profileTopic(name), '30m');
        const latest = msgs.filter((m) => m.t === 'p').pop();
        if (latest) applyPresence(latest);
      } catch (_) { /* ignore */ }
    }
    save();
    push();
  }

  async function syncChat(name) {
    if (!name || !validName(state.me)) return;
    const msgs = await poll(chatTopic(state.me, name), '12h');
    let changed = false;
    for (const msg of msgs) {
      if (msg.t !== 'm') continue;
      const before = (state.chats[keyName(name)] || []).length;
      appendChat(msg.n, msg.to, msg.x, msg.id, msg.at);
      if ((state.chats[keyName(name)] || []).length !== before) changed = true;
    }
    if (changed) {
      save();
      push();
    }
  }

  async function tick() {
    const t = now();
    try {
      if (t - lastInbox > 8000) {
        lastInbox = t;
        await syncInbox();
      }
      if (t - lastPresence > 15000) {
        lastPresence = t;
        await syncFriendsPresence();
      }
      if (activeChat && t - lastChat > 4000) {
        lastChat = t;
        await syncChat(activeChat);
      }
      if (t - lastOutgoing > 25000) {
        lastOutgoing = t;
        await republishOutgoing();
      }
    } catch (_) { /* ignore */ }
  }

  async function search(name) {
    const display = normName(name);
    if (!validName(display)) throw new Error('Enter a username (3–16 letters)');
    if (keyName(display) === keyName(state.me)) throw new Error('That is your own username');
    const friend = state.friends[keyName(display)];
    let latest = null;
    try {
      const msgs = await withTimeout(poll(profileTopic(display), '12h'), 2800, []);
      latest = (msgs || []).filter((m) => m.t === 'p').pop() || null;
    } catch (_) { /* still return the username */ }
    const found = ALLOW_NAME_SEARCH || !!latest || !!friend;
    return {
      name: latest?.n || display,
      found,
      online: latest ? isOnline(latest.at) : false,
      status: latest?.s || 'offline',
      server: latest?.sv || '',
      lastSeen: latest?.at || 0,
      alreadyFriend: !!friend,
      pending: state.outgoing.some((r) => keyName(r.to) === keyName(display))
        || state.incoming.some((r) => keyName(r.from) === keyName(display))
    };
  }

  async function add(name) {
    const display = normName(name);
    if (!validName(display)) throw new Error('Enter a username (3–16 letters)');
    if (keyName(display) === keyName(state.me)) throw new Error('You cannot add yourself');
    if (state.friends[keyName(display)]) return snapshot();
    if (!ALLOW_NAME_SEARCH) {
      let latest = null;
      try {
        const msgs = await withTimeout(poll(profileTopic(display), '12h'), 2800, []);
        latest = (msgs || []).filter((m) => m.t === 'p').pop() || null;
      } catch (_) { /* ignore */ }
      if (!latest) throw new Error('That player is not using Nitro Client');
    }
    if (!state.outgoing.some((r) => keyName(r.to) === keyName(display))) {
      state.outgoing.push({ to: display, at: now() });
    }
    save();
    try {
      await publish(inboxTopic(display), {
        v: 1, t: 'req', n: state.me, to: display, at: now()
      });
    } catch (_) { /* request stays queued and is retried */ }
    push();
    return snapshot();
  }

  async function accept(name) {
    const display = normName(name);
    becomeFriends(display);
    save();
    await publish(inboxTopic(display), {
      v: 1, t: 'ok', n: state.me, to: display, at: now()
    });
    push();
    return snapshot();
  }

  function decline(name) {
    state.incoming = state.incoming.filter((r) => keyName(r.from) !== keyName(name));
    save();
    push();
    return snapshot();
  }

  function remove(name) {
    const key = keyName(name);
    delete state.friends[key];
    delete state.chats[key];
    save();
    if (keyName(activeChat) === key) activeChat = '';
    push();
    return snapshot();
  }

  async function send(name, text) {
    const display = normName(name);
    const clean = String(text || '').replace(/[\u0000-\u001f]/g, ' ').trim().slice(0, MAX_TEXT);
    if (!clean) throw new Error('Type a message');
    if (!state.friends[keyName(display)]) throw new Error('Add them as a friend first');
    const id = crypto.randomBytes(5).toString('hex');
    appendChat(state.me, display, clean, id, now());
    save();
    await publish(chatTopic(state.me, display), {
      v: 1, t: 'm', n: state.me, to: display, x: clean, id, at: now()
    });
    push();
    return snapshot();
  }

  function openChat(name) {
    activeChat = normName(name);
    syncChat(activeChat).catch(() => {});
    return snapshot();
  }

  function start() {
    if (timer) return;
    heartbeat().catch(() => {});
    syncInbox().catch(() => {});
    timer = setInterval(() => {
      heartbeat().catch(() => {});
      tick();
    }, 5000);
  }

  function stop() {
    if (timer) clearInterval(timer);
    timer = null;
  }

  return {
    start,
    stop,
    setIdentity,
    setPlaying,
    setIdle,
    search,
    add,
    accept,
    decline,
    remove,
    send,
    openChat,
    snapshot
  };
}

module.exports = { createFriendsService, validName, normName };
