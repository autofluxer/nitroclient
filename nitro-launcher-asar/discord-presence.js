const fs = require('fs');
const path = require('path');
const RPC = require('discord-rpc');

const DEFAULT_CONFIG = {
  applicationId: '1520708712241823876',
  assetKey: 'nitro_icon',
  server: 'nitrosmp.lol',
  joinUrl: 'https://nitrosmp.lol',
  joinLabel: 'Join Nitro SMP',
  imageUrl: ''
};

const BRAND = 'Nitro Client';
const MC_VERSION = '1.21.11';

function loadConfig() {
  try {
    const raw = fs.readFileSync(path.join(__dirname, 'discord-config.json'), 'utf8');
    return { ...DEFAULT_CONFIG, ...JSON.parse(raw) };
  } catch (_) {
    return { ...DEFAULT_CONFIG };
  }
}

const config = loadConfig();
const CLIENT_ID = process.env.NITRO_DISCORD_APP_ID || config.applicationId;

let client = null;
let connected = false;
let gamePid = null;
let retryTimer = null;
let lastDetails = null;
let lastState = null;
let startedAt = null;

function registerClient() {
  try {
    RPC.register(CLIENT_ID);
  } catch (_) {
    /* already registered */
  }
}

function resolveImageKey() {
  if (config.imageUrl && /^https?:\/\//i.test(config.imageUrl)) {
    return config.imageUrl;
  }
  return config.assetKey;
}

function getActivityPid() {
  return gamePid && gamePid > 0 ? gamePid : process.pid;
}

function stopRetryTimer() {
  if (retryTimer) {
    clearInterval(retryTimer);
    retryTimer = null;
  }
}

function startActivityRetry() {
  stopRetryTimer();
  let attempts = 0;
  retryTimer = setInterval(() => {
    if (!client || !connected || !lastState || attempts >= 20) {
      stopRetryTimer();
      return;
    }
    attempts += 1;
    applyActivity(lastDetails, lastState, { silent: true }).catch(() => {});
  }, 30000);
}

async function connectDiscord() {
  if (connected) return true;
  if (client) return false;

  registerClient();
  client = new RPC.Client({ transport: 'ipc' });

  client.on('ready', () => {
    connected = true;
    setLauncherIdle().catch(() => {});
  });

  try {
    await client.login({ clientId: CLIENT_ID });
    return true;
  } catch (err) {
    client = null;
    connected = false;
    console.warn('[Nitro] Discord RPC unavailable:', err?.message || err);
    return false;
  }
}

async function applyActivity(details, state, opts = {}) {
  if (!client || !connected) return;
  lastDetails = details;
  lastState = state;
  if (!startedAt) startedAt = Date.now();

  try {
    await client.setActivity({
      details: details || BRAND,
      state: state || `Main Menu · ${MC_VERSION}`,
      startTimestamp: startedAt,
      largeImageKey: resolveImageKey(),
      largeImageText: BRAND,
      buttons: [
        { label: 'Join Discord', url: 'https://discord.gg/nitrosmp' },
        { label: config.joinLabel, url: config.joinUrl }
      ]
    }, getActivityPid());

    if (!opts.silent) {
      startActivityRetry();
    }
  } catch (err) {
    // Retry without buttons (some apps aren't verified for button slots)
    try {
      await client.setActivity({
        details: details || BRAND,
        state: state || `Main Menu · ${MC_VERSION}`,
        startTimestamp: startedAt,
        largeImageKey: resolveImageKey(),
        largeImageText: BRAND
      }, getActivityPid());
    } catch (err2) {
      if (!opts.silent) {
        console.warn('[Nitro] Discord setActivity failed:', err2?.message || err2);
      }
    }
  }
}

/** Launcher open / idle. */
async function setLauncherIdle() {
  await applyActivity(BRAND, `In Launcher · ${MC_VERSION}`);
}

/** Main menu (legacy alias). */
async function setMainMenu() {
  await applyActivity(BRAND, `Main Menu · ${MC_VERSION}`);
}

/**
 * Generic presence update.
 * @param {string} stateLine second line (server · version)
 * @param {{details?: string, silent?: boolean}} opts
 */
async function setPresence(stateLine, opts = {}) {
  if (!connected) {
    await connectDiscord();
  }
  const details = opts.details || BRAND;
  const state = stateLine || `Main Menu · ${MC_VERSION}`;
  return applyActivity(details, state, opts);
}

/** Game is launching / running from launcher view. */
async function setPlayingGame(serverHint) {
  const server = serverHint || config.server || 'Minecraft';
  await applyActivity(BRAND, `Playing on ${server} · ${MC_VERSION}`);
}

/** @deprecated */
async function setPlaying(line, opts = {}) {
  return setPresence(line, opts);
}

function setGamePid(pid) {
  if (pid && pid > 0) {
    gamePid = pid;
  }
}

async function disconnectDiscord() {
  connected = false;
  stopRetryTimer();
  lastDetails = null;
  lastState = null;
  startedAt = null;
  gamePid = null;

  if (!client) return;

  try {
    await client.clearActivity();
    await client.destroy();
  } catch (_) {
    /* ignore */
  }

  client = null;
}

module.exports = {
  connectDiscord,
  setPresence,
  setPlaying,
  setPlayingGame,
  setLauncherIdle,
  setMainMenu,
  disconnectDiscord,
  setGamePid
};
