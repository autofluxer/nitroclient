const { app, BrowserWindow, ipcMain, shell, safeStorage } = require('electron');
const path = require('path');
const childProcess = require('child_process');
const { spawn } = childProcess;
const fs = require('fs');
const { buildVersions, getRecommendedModernId } = require('./versions');
const discordPresence = require('./discord-presence');
const { pingServer, DEFAULT_HOST } = require('./server-status');
const { loadLauncherMeta } = require('./launcher-meta');
const { isOwnerBuild } = require('./owner-mode');
const ownerConfig = require('./owner-config');
const ownerPublish = require('./owner-publish');
const { applyPreset, listPresets, getGameDataDir } = require('./client-presets');
const msAuth = require('./microsoft-auth');
const { createSecureStore } = require('./session-store');
const runtimeManager = require('./runtime-manager');
const serverHub = require('./server-hub');
const modToggles = require('./mod-toggles');
const bundleRepair = require('./bundle-repair');
const fabricSetup = require('./fabric-setup');
const modrinth = require('./modrinth');
const spotifyAuth = require('./spotify-auth');
const { createFriendsService } = require('./friends');
const { createSkinsService } = require('./skins');
const { createHostingService } = require('./hosting');
const { dialog } = require('electron');

const secureStore = createSecureStore(safeStorage);
msAuth.initSecureStore(secureStore);

const APP_VERSION = require('./package.json').version;

const gotSingleInstanceLock = app.requestSingleInstanceLock();
if (!gotSingleInstanceLock) {
  app.quit();
}

const SETTINGS_PATH = path.join(app.getPath('userData'), 'nitro-settings.json');
const JAVA_HOME = process.env.JAVA_HOME || 'C:\\Program Files\\Java\\jdk-21';
const WIN_CREATE_NO_WINDOW = 0x08000000;
const WIN_DETACHED_PROCESS = 0x00000008;

let mainWindow;
let lastProgressKey = '';
let lastProgressAt = 0;
let launchSession = { aborted: false, processes: [] };
let launchInFlight = false;
let friends;
let skins;
let hosting;

function resetLaunchSession() {
  launchSession.aborted = false;
  launchSession.processes = [];
  launchSession.mclc = null;
  launchSession.joinServer = null;
}

function registerLaunchProcess(child) {
  if (!child || !child.pid) return;
  launchSession.processes.push(child);
  child.on('exit', () => {
    launchSession.processes = launchSession.processes.filter((p) => p !== child);
  });
}

function killLaunchProcess(child) {
  if (!child?.pid) return;
  try {
    if (process.platform === 'win32') {
      childProcess.spawnSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], {
        windowsHide: true,
        shell: false,
        stdio: 'ignore'
      });
    } else {
      child.kill('SIGKILL');
    }
  } catch (_) { /* ignore */ }
}

function cancelActiveLaunch() {
  launchSession.aborted = true;
  for (const child of [...launchSession.processes]) {
    killLaunchProcess(child);
  }
  launchSession.processes = [];
  launchSession.mclc = null;
  sendLaunchUpdate({ line: 'Launch cancelled', replace: true, percent: 0, phase: 'cancelled' });
}

function throwIfCancelled() {
  if (launchSession.aborted) {
    const err = new Error('Launch cancelled');
    err.code = 'CANCELLED';
    throw err;
  }
}

function isPidRunning(pid) {
  if (!pid) return false;
  try {
    if (process.platform === 'win32') {
      const out = childProcess.execSync(`tasklist /FI "PID eq ${pid}" /NH`, {
        encoding: 'utf8',
        windowsHide: true
      });
      return out.includes(String(pid));
    }
    process.kill(pid, 0);
    return true;
  } catch (_) {
    return false;
  }
}

function patchSpawnForSilentWindows() {
  if (process.platform !== 'win32' || spawn.__nitroPatched) return;

  const original = childProcess.spawn;
  childProcess.spawn = function patchedSpawn(command, args, options) {
    let cmd = command;
    if (typeof cmd === 'string' && /java\.exe$/i.test(cmd)) {
      cmd = toJavaw(cmd);
    }

    const opts = { ...(options || {}) };
    const argStr = Array.isArray(args) ? args.join(' ') : '';
    const isGameLaunch = /KnotClient|net\.minecraft\.client\.main\.Main|fabricmc\.loader/i.test(argStr);
    if (!isGameLaunch) {
      opts.windowsHide = true;
      opts.shell = false;
      opts.creationFlags = (opts.creationFlags || 0) | WIN_CREATE_NO_WINDOW;
    }
    return original.call(this, cmd, args, opts);
  };
  childProcess.spawn.__nitroPatched = true;
  spawn.__nitroPatched = true;
}

function writeLaunchLog(lines) {
  try {
    const stamp = new Date().toISOString();
    const body = Array.isArray(lines) ? lines.join('\n') : String(lines);
    fs.writeFileSync(getLaunchLogPath(), `[${stamp}]\n${body}\n`, { flag: 'a' });
  } catch (_) { /* ignore */ }
}

function buildLaunchEnv(extra = {}) {
  const env = { ...process.env, ...extra };
  delete env.NITRO_AUTO_JOIN;
  delete env.NITRO_LAUNCHER_JOIN;
  if (launchSession.joinServer) {
    env.NITRO_AUTO_JOIN = String(launchSession.joinServer);
    env.NITRO_LAUNCHER_JOIN = '1';
  }
  return env;
}

function buildJvmArgs(config, memoryMb = 4096) {
  const max = Math.max(1024, memoryMb || 4096);
  const min = Math.min(1024, Math.floor(max / 2));
  const filtered = (config.jvmArgs || []).filter((arg) => !/^-Xm[sx]/i.test(arg));
  return [
    `-Xms${min}M`,
    `-Xmx${max}M`,
    '-XX:+UseG1GC',
    '-XX:+ParallelRefProcEnabled',
    '-XX:MaxGCPauseMillis=200',
    ...filtered
  ];
}

function formatLaunchError(err, version) {
  const message = err?.message || String(err);
  const lines = [
    `Version: ${version?.label || version?.id || 'unknown'}`,
    `Error: ${message}`
  ];
  if (message.includes('Java 8')) lines.push('Fix: Install Java 8 and ensure it is on PATH.');
  if (message.includes('Gradle failed')) lines.push('Fix: Try Repair install in Settings, or run gradlew.bat runClient once.');
  if (message.includes('exited immediately')) lines.push('Fix: Open last launch log or client logs/latest.log for details.');
  return lines.join('\n');
}

function friendlyLaunchError(err, runDir) {
  const diagnosed = bundleRepair.diagnoseLaunchFailure(err?.message || String(err), runDir);
  return Object.assign(new Error(diagnosed.message), { code: diagnosed.code });
}

async function repairInstall() {
  const clientRoot = getClientRoot();
  const bundled = runtimeManager.isBundledClientRoot(clientRoot);

  if (bundled) {
    const installRoot = getInstallRoot();
    const installGame = path.join(installRoot, 'game');
    const bundledSource = path.join(__dirname, 'game');
    const runDir = getPlayerRunDir();
    const issuesBefore = bundleRepair.validateBundledInstall(clientRoot);

    sendLaunchUpdate({ line: 'Checking game files…', replace: true, percent: 10, phase: 'prepare' });
    bundleRepair.syncFromLauncherBundle(installGame, bundledSource);

    sendLaunchUpdate({ line: 'Removing outdated files…', replace: true, percent: 22, phase: 'prepare' });
    const removedReplay = bundleRepair.removeReplayJars(path.join(clientRoot, 'libs'));

    sendLaunchUpdate({ line: 'Verifying Minecraft assets…', replace: true, percent: 34, phase: 'prepare' });
    const assets = bundleRepair.syncAssets(clientRoot, bundleRepair.resolveGradleAssetsDir());
    if (assets.copied) {
      sendLaunchUpdate({ line: `Copied Minecraft assets (${assets.objectCount} files)…`, replace: true, percent: 48, phase: 'prepare' });
    }

    sendLaunchUpdate({ line: 'Refreshing launch config…', replace: true, percent: 58, phase: 'prepare' });
    try {
      bundleRepair.rewriteBundledLaunchConfig(clientRoot);
    } catch (err) {
      throw new Error('Could not refresh launch config: ' + (err.message || err));
    }

    prepareBundledRunDir(clientRoot, runDir, { repair: true });
    sendLaunchUpdate({ line: 'Checking Java 8…', replace: true, percent: 72, phase: 'prepare' });
    await runtimeManager.ensureBundledJava8(installRoot, (pct, line) => {
      sendLaunchUpdate({ line: line || 'Setting up Java 8…', replace: true, percent: Math.max(72, Math.min(92, pct)), phase: 'prepare' });
    });

    const issuesAfter = bundleRepair.validateBundledInstall(clientRoot);
    if (issuesAfter.length > 0) {
      throw new Error('Repair finished but issues remain: ' + issuesAfter.join(', ') + '. Run REBUILD-NITRO.bat on your Desktop.');
    }

    sendLaunchUpdate({ line: 'Repair complete — ready to play', replace: true, percent: 100, phase: 'done' });
    return {
      ok: true,
      clientRoot,
      bundled: true,
      removedReplay,
      assetsCopied: assets.copied,
      fixedIssues: issuesBefore
    };
  }

  const configPath = path.join(clientRoot, '.nitro-launch.json');
  if (fs.existsSync(configPath)) {
    fs.unlinkSync(configPath);
  }

  sendLaunchUpdate({ line: 'Repairing Nitro client files…', replace: true, percent: 12, phase: 'prepare' });
  await runGradle(clientRoot, ['classes', 'writeNitroLaunchConfig']);
  throwIfCancelled();

  const config = readNitroLaunchConfig(clientRoot);
  if (!validateLaunchConfig(config)) {
    throw new Error('Repair finished but launch config is still invalid.');
  }
  return { ok: true, clientRoot };
}

function getEffectiveGameDir() {
  const clientRoot = getClientRoot();
  if (runtimeManager.isBundledClientRoot(clientRoot)) {
    return prepareBundledRunDir(clientRoot, getPlayerRunDir());
  }
  return getGameDataDir(clientRoot);
}

function getLaunchLogPath() {
  return path.join(app.getPath('userData'), 'last-launch.log');
}

function getStandardInstallDir() {
  const localAppData = process.env.LOCALAPPDATA;
  return localAppData ? path.join(localAppData, 'Nitro Client') : null;
}

function getInstallRoot() {
  if (app.isPackaged) {
    const standard = getStandardInstallDir();
    if (standard && fs.existsSync(path.join(standard, 'game', 'libs'))) {
      return standard;
    }
    return path.dirname(process.execPath);
  }
  return path.resolve(__dirname, '..');
}

function getPlayerRunDir() {
  return path.join(app.getPath('userData'), 'nitro-189');
}

function getBundledGameDir() {
  const standard = getStandardInstallDir();
  const installRoot = getInstallRoot();
  const candidates = [
    standard && path.join(standard, 'game'),
    path.join(installRoot, 'game'),
    path.join(process.resourcesPath || installRoot, 'game'),
    path.join(__dirname, 'game')
  ].filter(Boolean);

  const seen = new Set();
  for (const candidate of candidates) {
    const resolved = path.resolve(candidate);
    if (seen.has(resolved)) continue;
    seen.add(resolved);
    if (runtimeManager.isBundledClientRoot(resolved)) return resolved;
  }
  return null;
}

function getClientRoot() {
  const bundled = getBundledGameDir();
  if (bundled) {
    const settings = loadSettings();
    if (settings.clientRoot !== bundled) {
      saveSettings({ ...settings, clientRoot: bundled });
    }
    return bundled;
  }

  const settings = loadSettings();
  if (settings.clientRoot) {
    if (runtimeManager.isBundledClientRoot(settings.clientRoot) || fs.existsSync(path.join(settings.clientRoot, 'gradlew.bat'))) {
      return settings.clientRoot;
    }
  }

  const candidates = [
    path.resolve(__dirname, '..'),
    path.join(path.dirname(process.execPath), 'game')
  ];
  for (const c of candidates) {
    if (fs.existsSync(path.join(c, 'gradlew.bat'))) {
      saveSettings({ ...loadSettings(), clientRoot: c });
      return c;
    }
  }
  return path.resolve(__dirname, '..');
}

const NITRO_VERSIONS = buildVersions();

function getVersionEntry(versionId) {
  const settings = loadSettings();
  const id = versionId || settings.selectedVersion || getRecommendedModernId();
  return NITRO_VERSIONS.find((v) => v.id === id) || NITRO_VERSIONS[0];
}

function getModsDirForVersion(versionId) {
  const version = getVersionEntry(versionId);
  if (!version) return path.join(getMinecraftDir(), 'nitro-1.21.11', 'mods');
  if (version.profile === 'nitro-modern') {
    return path.join(getMinecraftDir(), 'nitro-' + version.mc, 'mods');
  }
  if (version.profile === 'nitro-vanilla') {
    return path.join(getMinecraftDir(), version.mc, 'mods');
  }
  return path.join(getPlayerRunDir(), 'mods');
}

function getLoaderForVersion(versionId) {
  const version = getVersionEntry(versionId);
  if (!version) return 'fabric';
  if (version.profile === 'nitro-modern') return 'fabric';
  if (version.profile === 'nitro-vanilla') return 'fabric';
  return null;
}

function getMinecraftDir() {
  return path.join(app.getPath('userData'), 'nitroclient');
}

function loadSettings() {
  try {
    if (fs.existsSync(SETTINGS_PATH)) {
      const data = JSON.parse(fs.readFileSync(SETTINGS_PATH, 'utf8'));
      if (data.selectedVersion === 'nitro-1.21.11') {
        data.selectedVersion = 'nitro-modern-1.21.11';
      }
      return data;
    }
  } catch (_) { /* ignore */ }
  return {
    username: 'Player',
    selectedVersion: getRecommendedModernId() || 'nitro-1.8.9',
    memory: 4096,
    modPreset: 'pvp',
    performanceMode: false,
    onboardingComplete: false,
    loginMode: 'offline',
    rememberMicrosoftLogin: true,
    favoriteServers: [],
    lastServer: null,
    resourcePackPath: ''
  };
}

function saveSettings(data) {
  fs.writeFileSync(SETTINGS_PATH, JSON.stringify(data, null, 2));
  try {
    if (friends && data?.username) friends.setIdentity(data.username);
  } catch (_) { /* ignore */ }
}

function sendLaunchUpdate(payload) {
  if (payload?.line) {
    try {
      writeLaunchLog([String(payload.line)]);
    } catch (_) { /* ignore */ }
  }
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('launch-progress', payload);
  }
}

function sendLog(line, replace = false) {
  let percent = null;
  const match = line && line.match(/(\d+)%/);
  if (match) percent = parseInt(match[1], 10);

  sendLaunchUpdate({
    line: line || '',
    replace,
    percent,
    phase: percent != null ? 'download' : 'prepare'
  });
}

function sendProgress(type, task, total) {
  const now = Date.now();
  const percentBucket = (typeof total === 'number' && total > 0 && typeof task === 'number')
    ? Math.floor((task / total) * 20)
    : 0;
  const key = `${type}:${percentBucket}`;
  if (key === lastProgressKey && now - lastProgressAt < 750) return;
  lastProgressKey = key;
  lastProgressAt = now;

  let text;
  let percent = null;
  if (typeof total === 'number' && total > 0 && typeof task === 'number') {
    percent = Math.min(100, Math.round((task / total) * 100));
    text = `Downloading ${type}… ${percent}%`;
  } else {
    text = `Preparing ${type}…`;
  }

  sendLaunchUpdate({ line: text, replace: true, percent, phase: 'download' });
}

function toJavaw(javaPath) {
  if (!javaPath) return 'javaw';
  if (/javaw\.exe$/i.test(javaPath)) return javaPath;
  if (/java\.exe$/i.test(javaPath)) return javaPath.replace(/java\.exe$/i, 'javaw.exe');
  if (/[/\\]bin[/\\]java$/i.test(javaPath)) return javaPath.replace(/[/\\]java$/i, path.sep + 'javaw.exe');
  return javaPath;
}

function resolveJavaw(preferredJava) {
  const candidates = [
    preferredJava && toJavaw(preferredJava),
    path.join(JAVA_HOME, 'bin', 'javaw.exe'),
    process.env.JAVA_HOME && path.join(process.env.JAVA_HOME, 'bin', 'javaw.exe'),
    'C:\\Program Files\\Java\\jdk-21\\bin\\javaw.exe',
    'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot\\bin\\javaw.exe'
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (candidate === 'javaw') continue;
    if (fs.existsSync(candidate)) return candidate;
  }
  return 'javaw';
}

function resolveJava(preferredJava) {
  const candidates = [
    preferredJava,
    path.join(JAVA_HOME, 'bin', 'java.exe'),
    process.env.JAVA_HOME && path.join(process.env.JAVA_HOME, 'bin', 'java.exe')
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) return candidate;
  }
  return path.join(JAVA_HOME, 'bin', 'java.exe');
}

function spawnHidden(command, args, options = {}) {
  const winFlags = process.platform === 'win32'
    ? { creationFlags: WIN_CREATE_NO_WINDOW | WIN_DETACHED_PROCESS }
    : {};

  return spawn(command, args, {
    detached: true,
    stdio: 'ignore',
    shell: false,
    windowsHide: true,
    ...winFlags,
    ...options
  });
}

function spawnGame(command, args, options = {}) {
  const javaw = toJavaw(command);
  const winFlags = process.platform === 'win32'
    ? { creationFlags: WIN_DETACHED_PROCESS }
    : {};

  return spawn(javaw, args, {
    detached: true,
    stdio: 'ignore',
    shell: false,
    windowsHide: false,
    ...winFlags,
    ...options
  });
}

function restoreLauncher() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  if (mainWindow.isMinimized()) mainWindow.restore();
  mainWindow.show();
  mainWindow.focus();
  try { friends?.setIdle(); } catch (_) { /* ignore */ }
}

function isNitroGameRunning() {
  if (process.platform !== 'win32') return false;
  try {
    const out = childProcess.execSync(
      'powershell.exe -NoProfile -WindowStyle Hidden -Command "if (Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -match \'Minecraft|Nitro Client\' }) { exit 0 } else { exit 1 }"',
      { windowsHide: true, shell: false, stdio: 'pipe' }
    );
    return true;
  } catch (_) {
    return false;
  }
}

function watchGameProcess(pid, runDir, onGameVisible) {
  let seenAlive = false;
  let minimized = false;
  let ticks = 0;
  const startedAt = Date.now();
  const timer = setInterval(() => {
    ticks++;
    const alive = pid ? isProcessAlive(pid) : isNitroGameRunning();
    const gameVisible = isNitroGameRunning();
    if (alive) seenAlive = true;

    if (runDir && isGameLogFailure(runDir, startedAt)) {
      clearInterval(timer);
      restoreLauncher();
      sendLaunchUpdate({
        line: 'Minecraft crashed on startup. Try Settings → Repair install.',
        replace: true,
        percent: 0,
        phase: 'error'
      });
      return;
    }

    if (!minimized && gameVisible && typeof onGameVisible === 'function') {
      minimized = true;
      onGameVisible();
    }

    const spawnErrPath = runDir ? path.join(runDir, 'logs', 'launcher-spawn.err') : '';
    const spawnFailed = spawnErrPath && fs.existsSync(spawnErrPath) && (() => {
      try {
        const err = fs.readFileSync(spawnErrPath, 'utf8').trim();
        return err.length > 0 && Date.now() - fs.statSync(spawnErrPath).mtimeMs < 20000;
      } catch (_) {
        return false;
      }
    })();

    if (!seenAlive && ticks >= 2 && Date.now() - startedAt >= 4000) {
      clearInterval(timer);
      restoreLauncher();
      return;
    }

    if (spawnFailed && !seenAlive) {
      clearInterval(timer);
      restoreLauncher();
      return;
    }

    if ((seenAlive && !alive) || ticks > 3600) {
      clearInterval(timer);
      if (seenAlive && !alive) {
        // Game closed — show launcher presence again
        discordPresence.setGamePid(null);
        discordPresence.setLauncherIdle().catch(() => {});
      }
      restoreLauncher();
    }
  }, 1000);
}

function handOffToGame(pid, runDir, joinServer) {
  if (pid) discordPresence.setGamePid(pid);
  const server = joinServer || DEFAULT_HOST || 'nitrosmp.lol';
  discordPresence.setPlayingGame(server).catch(() => {});
  watchGameProcess(pid, runDir, () => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.minimize();
    }
  });
}

function readNitroLaunchConfig(clientRoot) {
  const configPath = path.join(clientRoot, '.nitro-launch.json');
  if (!fs.existsSync(configPath)) return null;
  try {
    const raw = fs.readFileSync(configPath, 'utf8').replace(/^\uFEFF/, '');
    return JSON.parse(raw);
  } catch (_) {
    return null;
  }
}

function defaultBundledLaunchConfig() {
  return {
    version: 2,
    bundled: true,
    java: 'BUNDLED_JAVA8',
    classpath: '',
    mainClass: 'io.github.solclient.wrapper.Launcher',
    jvmArgs: [],
    args: ['--version', 'Quilt Loom', '--assetIndex', '1.8.9-1.8', '--accessToken', '0', '--username', 'Player']
  };
}

function findNewestMtime(dir) {
  if (!fs.existsSync(dir)) return 0;
  let newest = 0;
  const stack = [dir];
  while (stack.length) {
    const current = stack.pop();
    let entries;
    try {
      entries = fs.readdirSync(current, { withFileTypes: true });
    } catch (_) {
      continue;
    }
    for (const entry of entries) {
      const full = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(full);
        continue;
      }
      try {
        const mtime = fs.statSync(full).mtimeMs;
        if (mtime > newest) newest = mtime;
      } catch (_) { /* ignore */ }
    }
  }
  return newest;
}

function isLaunchConfigFresh(clientRoot) {
  const configPath = path.join(clientRoot, '.nitro-launch.json');
  const classesDir = path.join(clientRoot, 'build', 'classes', 'java', 'main');
  if (!fs.existsSync(configPath) || !fs.existsSync(classesDir)) return false;

  const configMtime = fs.statSync(configPath).mtimeMs;
  return findNewestMtime(classesDir) <= configMtime;
}

function validateLaunchConfig(config) {
  if (!config?.java || !config?.classpath || !config?.mainClass) return false;
  if (!fs.existsSync(config.java)) return false;

  for (const entry of config.classpath.split(path.delimiter)) {
    if (entry && !fs.existsSync(entry)) return false;
  }

  return true;
}

function remapBundledPath(entryPath, bundleRoot, configBundleDir) {
  if (!entryPath) return entryPath;
  const normalized = entryPath.replace(/\//g, path.sep);
  if (fs.existsSync(normalized)) return normalized;

  const bundleDir = (configBundleDir || '').replace(/\//g, path.sep);
  if (bundleDir && normalized.startsWith(bundleDir)) {
    const relative = normalized.slice(bundleDir.length).replace(/^[/\\]+/, '');
    const candidate = path.join(bundleRoot, relative);
    if (fs.existsSync(candidate)) return candidate;
  }

  const libsCandidate = path.join(bundleRoot, 'libs', path.basename(normalized));
  if (fs.existsSync(libsCandidate)) return libsCandidate;

  return normalized;
}

function resolveBundledClasspath(config, bundleRoot) {
  const libsDir = path.join(bundleRoot, 'libs');
  const orderFile = path.join(bundleRoot, 'classpath-order.txt');
  const entries = [];
  const seen = new Set();

  const addEntry = (entryPath) => {
    if (!entryPath) return;
    const resolved = path.resolve(entryPath);
    if (!fs.existsSync(resolved) || seen.has(resolved)) return;
    if (bundleRepair.isReplayJarName(path.basename(resolved))) return;
    seen.add(resolved);
    entries.push(resolved);
  };

  if (fs.existsSync(orderFile)) {
    for (const name of fs.readFileSync(orderFile, 'utf8').split(/\r?\n/)) {
      const trimmed = name.trim();
      if (!trimmed) continue;
      addEntry(path.join(libsDir, trimmed));
    }
  } else if (config.classpath) {
    for (const entry of config.classpath.split(path.delimiter)) {
      addEntry(remapBundledPath(entry, bundleRoot, config.bundleDir));
    }
  }

  const clientJar = path.join(libsDir, 'nitro-client.jar');
  if (fs.existsSync(clientJar)) {
    const resolvedClient = path.resolve(clientJar);
    const withoutClient = entries.filter((entry) => entry !== resolvedClient);
    entries.length = 0;
    entries.push(resolvedClient, ...withoutClient);
    seen.add(resolvedClient);
  }

  if (fs.existsSync(libsDir)) {
    for (const name of fs.readdirSync(libsDir).filter((file) => file.endsWith('.jar'))) {
      addEntry(path.join(libsDir, name));
    }
  }

  return entries.join(path.delimiter);
}

function resolveBundledLaunchConfig(config, clientRoot) {
  const bundleRoot = path.resolve(clientRoot);
  const bundlePath = bundleRoot.replace(/\\/g, '/');
  const classpath = resolveBundledClasspath(config, bundleRoot);

  const natives = path.join(bundleRoot, 'natives');
  const assets = path.join(bundleRoot, 'assets');
  const remap = path.join(bundleRoot, 'remapClasspath.txt');
  const log4j = path.join(bundleRoot, 'log4j2.xml');
  const launchCfg = path.join(bundleRoot, 'launch.cfg');

  const jvmArgs = (config.jvmArgs || []).map((arg) => {
    if (arg.startsWith('-Djava.library.path=')) return `-Djava.library.path=${natives.replace(/\\/g, '/')}`;
    if (arg.startsWith('-Dorg.lwjgl.librarypath=')) return `-Dorg.lwjgl.librarypath=${natives.replace(/\\/g, '/')}`;
    if (arg.startsWith('-Dloader.remapClasspathFile=')) return `-Dloader.remapClasspathFile=${remap.replace(/\\/g, '/')}`;
    if (arg.startsWith('-Dlog4j.configurationFile=')) return `-Dlog4j.configurationFile=${log4j.replace(/\\/g, '/')}`;
    if (arg.startsWith('-Dfabric.dli.config=') && fs.existsSync(launchCfg)) {
      return `-Dfabric.dli.config=${launchCfg.replace(/\\/g, '/')}`;
    }
    return arg;
  });
  if (!jvmArgs.some((arg) => arg.startsWith('-Dmixin.service='))) {
    jvmArgs.push('-Dmixin.service=io.github.solclient.wrapper.WrapperMixinService');
  }

  const args = [...(config.args || [])];
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--assetsDir' && i + 1 < args.length) {
      args[i + 1] = assets.replace(/\\/g, '/');
    }
  }

  return {
    ...config,
    classpath,
    jvmArgs,
    args,
    bundleDir: bundlePath,
    workingDir: bundlePath
  };
}

function listJava8Installs() {
  return runtimeManager.listSystemJava8Installs();
}

function resolveGameJava(configJava, installRoot) {
  const bundled = runtimeManager.findExistingJava8(installRoot || getInstallRoot(), listJava8Installs());
  const candidates = [
    bundled,
    configJava && configJava !== 'BUNDLED_JAVA8' ? configJava : null,
    process.env.JAVA8_HOME && path.join(process.env.JAVA8_HOME, 'bin', 'java.exe'),
    ...listJava8Installs()
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) return candidate;
  }

  throw new Error('Java 8 not found. Use Setup Java in Settings or install JDK 8.');
}

function copyDirRecursive(src, dest) {
  if (!fs.existsSync(src)) return;
  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const from = path.join(src, entry.name);
    const to = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyDirRecursive(from, to);
    } else if (!fs.existsSync(to)) {
      fs.copyFileSync(from, to);
    }
  }
}

function copyFileIfMissing(from, to) {
  if (!fs.existsSync(from)) return;
  fs.mkdirSync(path.dirname(to), { recursive: true });
  if (!fs.existsSync(to)) {
    fs.copyFileSync(from, to);
  }
}

function sanitizePlayerConfig(runDir) {
  const solModsPath = path.join(runDir, 'config', 'sol-client', 'mods.json');
  if (!fs.existsSync(solModsPath)) return;
  try {
    const data = JSON.parse(fs.readFileSync(solModsPath, 'utf8'));
    if (data.cosmetics) {
      delete data.cosmetics;
      fs.writeFileSync(solModsPath, JSON.stringify(data, null, 2));
    }
  } catch (_) {
    // ignore malformed config
  }
}

function refreshBundledConfigTemplates(bundleRoot, runDir) {
  const template = path.join(bundleRoot, 'config-template');
  if (!fs.existsSync(template)) return;

  copyFileIfMissing(
    path.join(template, 'nitro-client', 'mods.json'),
    path.join(runDir, 'config', 'nitro-client', 'mods.json')
  );
  copyFileIfMissing(
    path.join(template, 'sol-client', 'chat-filters.json'),
    path.join(runDir, 'config', 'sol-client', 'chat-filters.json')
  );
  sanitizePlayerConfig(runDir);
}

function prepareBundledRunDir(bundleRoot, runDir, { repair = false } = {}) {
  fs.mkdirSync(runDir, { recursive: true });
  const template = path.join(bundleRoot, 'config-template');
  if (fs.existsSync(template)) {
    copyDirRecursive(template, path.join(runDir, 'config'));
  }
  if (repair) {
    refreshBundledConfigTemplates(bundleRoot, runDir);
  }
  fs.mkdirSync(path.join(runDir, 'resourcepacks'), { recursive: true });
  fs.mkdirSync(path.join(runDir, 'saves'), { recursive: true });
  return runDir;
}

function patchBundledLaunchConfig(config, bundleRoot, runDir, javaPath) {
  return {
    ...config,
    java: javaPath,
    workingDir: runDir.replace(/\\/g, '/'),
    bundleDir: bundleRoot.replace(/\\/g, '/')
  };
}

function runGradle(clientRoot, tasks) {
  const wrapperJar = path.join(clientRoot, 'gradle', 'wrapper', 'gradle-wrapper.jar');
  if (!fs.existsSync(wrapperJar)) {
    return Promise.reject(new Error('Gradle wrapper missing in client folder'));
  }

  const java = resolveJava(path.join(JAVA_HOME, 'bin', 'java.exe'));
  if (!fs.existsSync(java)) {
    return Promise.reject(new Error('Java 21 not found for Gradle at ' + java));
  }

  return new Promise((resolve, reject) => {
    throwIfCancelled();

    const child = childProcess.spawn(java, [
      '-cp', wrapperJar,
      'org.gradle.wrapper.GradleWrapperMain',
      ...tasks,
      '--daemon',
      '-q'
    ], {
      cwd: clientRoot,
      env: {
        ...process.env,
        JAVA_HOME,
        PATH: JAVA_HOME + '\\bin;' + (process.env.PATH || ''),
        GRADLE_OPTS: '-Dorg.gradle.console=plain',
        NITRO_USERNAME: launchSession.username || process.env.NITRO_USERNAME || 'Player'
      },
      windowsHide: true,
      shell: false,
      stdio: ['ignore', 'pipe', 'pipe']
    });

    registerLaunchProcess(child);

    let stderr = '';
    child.stderr?.on('data', (chunk) => {
      stderr = (stderr + chunk.toString()).slice(-1200);
    });

    child.on('error', (err) => {
      reject(err);
    });

    child.on('close', (code) => {
      if (launchSession.aborted) {
        reject(Object.assign(new Error('Launch cancelled'), { code: 'CANCELLED' }));
        return;
      }
      if (code !== 0) {
        const tail = stderr.trim().split('\n').slice(-2).join(' ').trim();
        reject(new Error('Gradle failed (' + code + ')' + (tail ? ': ' + tail : '')));
        return;
      }
      resolve();
    });
  });
}

async function ensureLaunchConfig(clientRoot) {
  const bundled = runtimeManager.isBundledClientRoot(clientRoot);
  let config = readNitroLaunchConfig(clientRoot);

  if (bundled) {
    if (!config) {
      config = defaultBundledLaunchConfig();
    }
    const resolved = resolveBundledLaunchConfig(config, clientRoot);
    const clientJar = path.join(clientRoot, 'libs', 'nitro-client.jar');
    if (!fs.existsSync(clientJar)) {
      throw new Error('Nitro client is outdated (missing nitro-client.jar). Run REBUILD-NITRO.bat on your Desktop.');
    }
    let java;
    try {
      java = resolveGameJava(config.java || 'BUNDLED_JAVA8', getInstallRoot());
    } catch (err) {
      throw Object.assign(
        new Error('Java 8 is required. Click Play again to auto-download, or use Settings -> Java setup.'),
        { code: 'JAVA8_MISSING' }
      );
    }
    if (!validateLaunchConfig({ ...resolved, java })) {
      const missing = resolved.classpath
        .split(path.delimiter)
        .filter((entry) => entry && !fs.existsSync(entry));
      writeLaunchLog([
        'Bundled launch validation failed',
        `clientRoot: ${clientRoot}`,
        `java: ${java}`,
        `missing (${missing.length}): ${missing.slice(0, 5).join('\n')}`
      ]);
      throw new Error(
        `Bundled Nitro Client files are incomplete (${missing.length} missing). Run REBUILD-NITRO.bat on your Desktop.`
      );
    }
    return resolved;
  }

  if (config && isLaunchConfigFresh(clientRoot) && validateLaunchConfig(config)) {
    return config;
  }

  if (app.isPackaged) {
    const bundled = getBundledGameDir();
    if (!bundled) {
      throw new Error(
        'Nitro Client game files not found. Use the Nitro Client shortcut on your Desktop (not a copied .exe), or run REBUILD-NITRO.bat.'
      );
    }
    throw new Error('Bundled Nitro Client files are incomplete. Run REBUILD-NITRO.bat on your Desktop.');
  }

  sendLaunchUpdate({ line: 'Building client files…', replace: true, percent: 22, phase: 'prepare' });
  await runGradle(clientRoot, ['classes', 'writeNitroLaunchConfig']);
  throwIfCancelled();

  config = readNitroLaunchConfig(clientRoot);
  if (!validateLaunchConfig(config)) {
    throw new Error('Launch config is invalid after build. Try Repair install in Settings.');
  }
  return config;
}

function buildLaunchArgs(config, username, mclcAuth) {
  const args = [...(config.args || [])];

  const setArg = (flag, value) => {
    const idx = args.indexOf(flag);
    if (idx !== -1 && idx + 1 < args.length) {
      args[idx + 1] = String(value);
    } else {
      args.push(flag, String(value));
    }
  };

  const removeArg = (flag) => {
    const idx = args.indexOf(flag);
    if (idx !== -1) args.splice(idx, 2);
  };

  if (mclcAuth) {
    setArg('--username', mclcAuth.name || username);
    setArg('--accessToken', mclcAuth.access_token);
    setArg('--uuid', mclcAuth.uuid);
    setArg('--userType', mclcAuth.meta?.type === 'msa' ? 'msa' : 'mojang');
    setArg('--userProperties', mclcAuth.user_properties || '{}');
  } else {
    setArg('--username', username);
    setArg('--accessToken', '0');
    removeArg('--uuid');
    removeArg('--userType');
    removeArg('--userProperties');
  }

  return args;
}

function prewarmGradle189(clientRoot, writeConfig = false) {
  const wrapperJar = path.join(clientRoot, 'gradle', 'wrapper', 'gradle-wrapper.jar');
  if (!fs.existsSync(wrapperJar)) return;

  const java = resolveJava(path.join(JAVA_HOME, 'bin', 'java.exe'));
  if (!fs.existsSync(java)) return;

  const tasks = writeConfig ? ['classes', 'writeNitroLaunchConfig', '--daemon', '-q'] : ['classes', '--daemon', '-q'];
  const child = spawnHidden(java, ['-cp', wrapperJar, 'org.gradle.wrapper.GradleWrapperMain', ...tasks], {
    cwd: clientRoot,
    env: {
      ...process.env,
      JAVA_HOME,
      PATH: JAVA_HOME + '\\bin;' + (process.env.PATH || ''),
      GRADLE_OPTS: '-Dorg.gradle.console=plain'
    }
  });
  child.unref();
}

function readGameLogTail(runDir, lines = 6, sinceMs = 0) {
  const logPath = path.join(runDir, 'logs', 'latest.log');
  if (!fs.existsSync(logPath)) return '';
  try {
    const stat = fs.statSync(logPath);
    if (sinceMs && stat.mtimeMs < sinceMs) return '';
    const content = fs.readFileSync(logPath, 'utf8');
    return content.trim().split('\n').slice(-lines).join(' ').slice(-240);
  } catch (_) {
    return '';
  }
}

function isGameLogFailure(runDir, startedAt) {
  const logPath = path.join(runDir, 'logs', 'latest.log');
  if (!fs.existsSync(logPath)) return false;
  try {
    const stat = fs.statSync(logPath);
    if (stat.mtimeMs < startedAt - 1500) return false;
    const tail = fs.readFileSync(logPath, 'utf8').slice(-2400);
    return /Launch error|MixinTransformerError|ClassNotFoundException|NoClassDefFoundError/i.test(tail);
  } catch (_) {
    return false;
  }
}

function isGameLogHealthy(runDir, startedAt) {
  const logPath = path.join(runDir, 'logs', 'latest.log');
  if (!fs.existsSync(logPath)) return false;
  try {
    const stat = fs.statSync(logPath);
    if (stat.mtimeMs < startedAt - 1500 || stat.size <= 40) return false;
    if (isGameLogFailure(runDir, startedAt)) return false;
    const tail = fs.readFileSync(logPath, 'utf8').slice(-1200);
    return /(Loading \d+ mods|LWJGL Version|Setting user:|Reloading ResourceManager|textures-atlas|OpenGL)/i.test(tail);
  } catch (_) {
    return false;
  }
}

function isProcessAlive(pid) {
  if (!pid) return false;
  try {
    process.kill(pid, 0);
    return true;
  } catch (_) {
    return false;
  }
}

function waitForGameProcess(child, runDir, timeoutMs = 15000) {
  const startedAt = Date.now();
  const spawnErrPath = path.join(runDir, 'logs', 'launcher-spawn.err');
  return new Promise((resolve) => {
    let aliveTicks = 0;
    const tick = () => {
      if (launchSession.aborted) {
        resolve(false);
        return;
      }

      if (fs.existsSync(spawnErrPath)) {
        try {
          const err = fs.readFileSync(spawnErrPath, 'utf8').trim();
          if (err.length > 0 && fs.statSync(spawnErrPath).mtimeMs >= startedAt - 1000) {
            resolve(false);
            return;
          }
        } catch (_) { /* ignore */ }
      }

      const pidAlive = child?.pid && isProcessAlive(child.pid);
      const gameRunning = isNitroGameRunning();
      const logPath = path.join(runDir, 'logs', 'latest.log');
      let logHealthy = isGameLogHealthy(runDir, startedAt);
      if (fs.existsSync(logPath) && isGameLogFailure(runDir, startedAt)) {
        resolve(false);
        return;
      }

      if (pidAlive || gameRunning || logHealthy) {
        aliveTicks++;
        if (aliveTicks >= 4 || gameRunning || logHealthy) {
          resolve(true);
          return;
        }
      } else {
        aliveTicks = 0;
      }

      if (Date.now() - startedAt >= timeoutMs) {
        resolve(false);
        return;
      }
      setTimeout(tick, 500);
    };
    setTimeout(tick, 800);
  });
}

function launchNitro189Fast(username, clientRoot, config, memoryMb, mclcAuth, runDir) {
  const installRoot = getInstallRoot();
  const gameJava = resolveGameJava(config.java, installRoot);
  const javaw = toJavaw(gameJava);
  if (!fs.existsSync(javaw)) {
    throw new Error('Java 8 launcher missing: ' + javaw);
  }

  const workingDir = runDir || config.workingDir || clientRoot;
  fs.mkdirSync(path.join(workingDir, 'logs'), { recursive: true });
  const spawnLog = path.join(workingDir, 'logs', 'launcher-spawn.err');
  try {
    fs.writeFileSync(spawnLog, '');
  } catch (_) { /* ignore */ }
  const env = buildLaunchEnv({
    NITRO_USERNAME: username
  });

  const args = [
    ...buildJvmArgs(config, memoryMb),
    '-cp', config.classpath,
    config.mainClass,
    ...buildLaunchArgs(config, username, mclcAuth)
  ];

  writeLaunchLog([
    'Spawning Nitro 1.8.9',
    `java: ${javaw}`,
    `cwd: ${workingDir}`,
    `main: ${config.mainClass}`
  ]);

  const child = spawn(javaw, args, {
    detached: true,
    cwd: workingDir,
    env,
    shell: false,
    windowsHide: false,
    stdio: 'ignore',
    ...(process.platform === 'win32' ? { creationFlags: WIN_DETACHED_PROCESS } : {})
  });

  child.unref();
  return child;
}

async function launchNitro189Gradle(username, clientRoot) {
  launchSession.username = username;
  sendLaunchUpdate({ line: 'Starting via Gradle…', replace: true, percent: 48, phase: 'prepare' });
  await runGradle(clientRoot, ['runClient']);
  return { pid: null, type: 'nitro-189-gradle' };
}

async function launchNitro189(username, memoryMb, mclcAuth) {
  const clientRoot = getClientRoot();
  const bundled = runtimeManager.isBundledClientRoot(clientRoot);
  const gradlew = path.join(clientRoot, 'gradlew.bat');

  if (!bundled && !fs.existsSync(gradlew)) {
    throw new Error('Nitro Client files not found. Download from https://nitrosmp.lol');
  }

  launchSession.username = username;
  sendLaunchUpdate({ line: 'Starting Nitro 1.8.9…', replace: true, percent: 8, phase: 'prepare' });
  throwIfCancelled();

  let runDir = getGameDataDir(clientRoot);
  if (bundled) {
    runDir = prepareBundledRunDir(clientRoot, getPlayerRunDir());
    sendLaunchUpdate({ line: 'Checking Java 8…', replace: true, percent: 14, phase: 'prepare' });
    try {
      await runtimeManager.ensureBundledJava8(getInstallRoot(), (pct, line) => {
        sendLaunchUpdate({ line: line || 'Setting up Java 8…', replace: true, percent: Math.max(14, Math.min(40, pct)), phase: 'prepare' });
      });
    } catch (err) {
      throw Object.assign(new Error(err.message || 'Java 8 setup failed'), { code: 'JAVA8_MISSING' });
    }
  }

  let config;
  try {
    config = await ensureLaunchConfig(clientRoot);
    if (bundled) {
      const javaPath = resolveGameJava(config.java, getInstallRoot());
      config = patchBundledLaunchConfig(config, clientRoot, runDir, javaPath);
    }
    throwIfCancelled();

    sendLaunchUpdate({ line: 'Launching…', replace: true, percent: 72, phase: 'launch' });
    const launchStartedAt = Date.now();
    const child = launchNitro189Fast(username, clientRoot, config, memoryMb, mclcAuth, runDir);
    const alive = await waitForGameProcess(child, runDir);
    throwIfCancelled();

    if (!alive) {
      const hint = readGameLogTail(runDir, 8, launchStartedAt - 5000);
      if (hint && /(Loading \d+ mods|LWJGL Version|Setting user:|Reloading ResourceManager|textures-atlas)/i.test(hint)) {
        sendLaunchUpdate({ line: 'Minecraft is opening…', replace: true, percent: 100, phase: 'done' });
        return { pid: child.pid, type: bundled ? 'nitro-189-bundled' : 'nitro-189-fast' };
      }
      const spawnErr = fs.existsSync(path.join(runDir, 'logs', 'launcher-spawn.err'))
        ? fs.readFileSync(path.join(runDir, 'logs', 'launcher-spawn.err'), 'utf8').trim().slice(-400)
        : '';
      const diagnosed = bundleRepair.diagnoseLaunchFailure(spawnErr || hint || 'Minecraft exited immediately after launch', runDir);
      throw Object.assign(new Error(diagnosed.message), { code: diagnosed.code });
    }

    sendLaunchUpdate({ line: 'Minecraft is opening…', replace: true, percent: 100, phase: 'done' });
    return { pid: child.pid, type: bundled ? 'nitro-189-bundled' : 'nitro-189-fast' };
  } catch (err) {
    if (err.code === 'CANCELLED' || launchSession.aborted) throw err;
    if (bundled || app.isPackaged) throw err;
    sendLaunchUpdate({ line: 'Retrying via Gradle…', replace: true, percent: 40, phase: 'prepare' });
    return launchNitro189Gradle(username, clientRoot);
  }
}

function waitForJavaGameProcess(timeoutMs = 25000) {
  return new Promise((resolve) => {
    const startedAt = Date.now();
    const tick = () => {
      if (launchSession.aborted) {
        resolve(false);
        return;
      }
      if (isNitroGameRunning()) {
        resolve(true);
        return;
      }
      if (Date.now() - startedAt >= timeoutMs) {
        resolve(false);
        return;
      }
      setTimeout(tick, 500);
    };
    setTimeout(tick, 800);
  });
}

async function launchNitro211(versionMc, username, memoryMb, mclcAuth, options = {}) {
  throwIfCancelled();

  const { Client, Authenticator } = require('minecraft-launcher-core');
  const gameDir = options.gameDir || path.join(getMinecraftDir(), versionMc);
  if (!fs.existsSync(gameDir)) {
    fs.mkdirSync(gameDir, { recursive: true });
  }

  const launcher = new Client();
  launchSession.mclc = launcher;
  const debugLines = [];

  launcher.on('progress', (e) => {
    if (launchSession.aborted) return;
    const task = typeof e.task === 'number' ? e.task : e.current;
    sendProgress(e.type || 'files', task, e.total);
  });
  launcher.on('debug', (line) => {
    const text = String(line || '').trim();
    if (!text) return;
    debugLines.push(text);
    if (debugLines.length > 40) debugLines.shift();
    if (/mod|fabric|forge|loading|error|warn/i.test(text)) {
      sendLaunchUpdate({ line: 'DBG: ' + text, replace: false, phase: 'launch', consoleOnly: true });
    }
  });
  launcher.on('data', (line) => {
    const text = String(line || '').trim();
    if (!text) return;
    sendLaunchUpdate({ line: text, replace: false, phase: 'launch', consoleOnly: true });
  });
  launcher.on('close', (code) => {
    sendLaunchUpdate({ line: `Minecraft exited (${code})`, replace: false, phase: 'done', consoleOnly: true });
  });

  sendLaunchUpdate({ line: 'Preparing Minecraft ' + versionMc + '…', replace: true, percent: 10, phase: 'prepare' });

  const versionSpec = options.fabric
    ? { number: versionMc, type: 'release', custom: options.fabricVersion || fabricSetup.fabricVersionId(versionMc) }
    : { number: versionMc, type: 'release' };

  const javaPath = resolveJavaw();
  if (!fs.existsSync(javaPath) && javaPath !== 'javaw') {
    throw Object.assign(new Error('Java 21 not found for Minecraft ' + versionMc + '. Install JDK 21.'), {
      code: 'JAVA21_MISSING'
    });
  }

  const launchOptions = {
    authorization: mclcAuth || Authenticator.getAuth(username),
    root: gameDir,
    javaPath,
    version: versionSpec,
    memory: { max: memoryMb + 'M', min: '512M' },
    overrides: {
      detached: true,
      gameDirectory: gameDir,
      cwd: gameDir,
      maxSockets: 16
    }
  };

  if (launchSession.joinServer) {
    const [host, portText] = launchSession.joinServer.split(':');
    launchOptions.server = {
      host,
      port: parseInt(portText, 10) || 25565
    };
  }

  writeLaunchLog([
    `Launching ${versionMc}${options.fabric ? ' (Fabric)' : ''}`,
    `gameDir: ${gameDir}`,
    `java: ${javaPath}`,
    `profile: ${versionSpec.custom || versionSpec.number}`
  ]);

  const proc = await launcher.launch(launchOptions);

  throwIfCancelled();

  if (!proc) {
    const hint = debugLines.slice(-6).join(' | ') || 'Minecraft Launcher Core returned no process.';
    throw Object.assign(new Error('Minecraft failed to start. ' + hint), { code: 'LAUNCH_FAIL' });
  }

  registerLaunchProcess(proc);

  // Warm launches already have a live pid — don't block the UI for 30s.
  if (proc.pid && isProcessAlive(proc.pid)) {
    sendLaunchUpdate({ line: 'Minecraft is opening…', replace: true, percent: 100, phase: 'done' });
    return { pid: proc.pid, type: options.fabric ? 'nitro-211-mod' : 'nitro-211' };
  }

  sendLaunchUpdate({ line: 'Waiting for Minecraft…', replace: true, percent: 92, phase: 'launch' });
  const alive = await waitForJavaGameProcess(12000);
  if (!alive) {
    const hint = debugLines.slice(-6).join(' | ') || 'No Minecraft process detected.';
    throw Object.assign(new Error('Minecraft exited immediately or never opened. ' + hint), { code: 'EARLY_EXIT' });
  }

  sendLaunchUpdate({ line: 'Minecraft is opening…', replace: true, percent: 100, phase: 'done' });
  return { pid: proc.pid ?? null, type: options.fabric ? 'nitro-211-mod' : 'nitro-211' };
}

function getNitro121ModJar() {
  // Prefer durable installs / workspace builds over portable temp extracts.
  // Freshest-mtime alone is wrong: extracting the .exe stamps an old jar with "now".
  const candidates = [
    path.join(__dirname, '..', 'nitro-1.21', 'build', 'libs', 'nitro-client-121-1.1.23.jar'),
    path.join(__dirname, '..', 'nitro-1.21', 'build', 'libs', 'nitro-client-121-1.0.0.jar'),
    path.join(app.getPath('appData'), 'nitroclient', 'nitroclient', 'nitro-1.21.11', 'mods', 'nitro-client-121.jar'),
    path.join(__dirname, 'game-121', 'mods', 'nitro-client-121.jar'),
    path.join(process.resourcesPath || '', 'game-121', 'mods', 'nitro-client-121.jar'),
  ];
  const existing = [];
  for (const candidate of candidates) {
    try {
      if (!candidate || !fs.existsSync(candidate)) continue;
      existing.push({ path: candidate, mtimeMs: fs.statSync(candidate).mtimeMs });
    } catch (_) {}
  }
  if (!existing.length) return null;
  // Prefer anything outside %TEMP% (portable unpack dir), then newest mtime.
  const tempRoot = (process.env.TEMP || process.env.TMP || '').toLowerCase();
  existing.sort((a, b) => {
    const aTemp = tempRoot && a.path.toLowerCase().startsWith(tempRoot) ? 1 : 0;
    const bTemp = tempRoot && b.path.toLowerCase().startsWith(tempRoot) ? 1 : 0;
    if (aTemp !== bTemp) return aTemp - bTemp;
    return b.mtimeMs - a.mtimeMs;
  });
  return existing[0].path;
}

function sameFileQuick(a, b) {
  try {
    if (!fs.existsSync(a) || !fs.existsSync(b)) return false;
    const sa = fs.statSync(a);
    const sb = fs.statSync(b);
    return sa.size === sb.size && Math.abs(sa.mtimeMs - sb.mtimeMs) < 2;
  } catch (_) {
    return false;
  }
}

function ensureNitro121ModInstalled(gameDir) {
  const modJar = getNitro121ModJar();
  if (!modJar) {
    throw Object.assign(new Error('Nitro 1.21.11 mod is missing. Rebuild the launcher or run nitro-1.21 build.'), {
      code: 'MOD121_MISSING'
    });
  }
  const modsDir = path.join(gameDir, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });
  const target = path.join(modsDir, 'nitro-client-121.jar');
  // Always refresh when source is newer/different — prevents stale portable extracts from winning.
  if (!sameFileQuick(modJar, target)) {
    fs.copyFileSync(modJar, target);
    try {
      const srcStat = fs.statSync(modJar);
      fs.utimesSync(target, srcStat.atime, srcStat.mtime);
    } catch (_) {
      // best-effort mtime preserve for next warm launch skip
    }
  }
  return target;
}

async function launchNitro211Mod(versionMc, username, memoryMb, mclcAuth) {
  const gameDir = path.join(getMinecraftDir(), 'nitro-' + versionMc);
  if (!fs.existsSync(gameDir)) {
    fs.mkdirSync(gameDir, { recursive: true });
  }

  sendLaunchUpdate({ line: 'Preparing ' + versionMc + '…', replace: true, percent: 12, phase: 'prepare' });
  const [fabricVersionId] = await Promise.all([
    fabricSetup.prepareModernFabricInstall(gameDir, versionMc),
    Promise.resolve(ensureNitro121ModInstalled(gameDir))
  ]);

  if (launchSession.joinServer) {
    process.env.NITRO_AUTO_JOIN = String(launchSession.joinServer);
    process.env.NITRO_LAUNCHER_JOIN = '1';
  }
  try {
    return await launchNitro211(versionMc, username, memoryMb, mclcAuth, {
      fabric: true,
      fabricVersion: fabricVersionId,
      gameDir
    });
  } finally {
    delete process.env.NITRO_AUTO_JOIN;
    delete process.env.NITRO_LAUNCHER_JOIN;
  }
}

function createWindow() {
  const owner = isOwnerBuild();
  mainWindow = new BrowserWindow({
    width: 1180,
    height: 720,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: '#0b0d10',
    title: owner ? 'Nitro Owner' : 'Nitro Client',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    frame: false,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      backgroundThrottling: false
    }
  });

  mainWindow.once('ready-to-show', () => mainWindow.show());

  const notifyWindowState = () => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    mainWindow.webContents.send('window-state', {
      minimized: mainWindow.isMinimized(),
      maximized: mainWindow.isMaximized(),
      focused: mainWindow.isFocused(),
      visible: mainWindow.isVisible()
    });
  };
  mainWindow.on('minimize', notifyWindowState);
  mainWindow.on('restore', notifyWindowState);
  mainWindow.on('show', notifyWindowState);
  mainWindow.on('maximize', notifyWindowState);
  mainWindow.on('unmaximize', notifyWindowState);
  mainWindow.on('focus', notifyWindowState);
  mainWindow.on('blur', notifyWindowState);
  mainWindow.on('resize', () => {
    // Debounce-ish: renderer recovers layout after OS resize/restore.
    if (!mainWindow.isMinimized()) notifyWindowState();
  });

  mainWindow.loadFile('index.html');
}

app.whenReady().then(() => {
  patchSpawnForSilentWindows();
  if (process.platform === 'win32') {
    app.setAppUserModelId(isOwnerBuild() ? 'com.nitroclient.owner' : 'com.nitroclient.launcher');
  }
  friends = createFriendsService(app.getPath('userData'), (payload) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('friends-updated', payload);
    }
  });
  try {
    friends.setIdentity(loadSettings().username || 'Player');
    friends.start();
  } catch (_) { /* ignore */ }
  skins = createSkinsService(app.getPath('userData'), {
    getUsername: () => loadSettings().username || 'Player',
    getGameDirs: () => {
      const root = getMinecraftDir();
      return [
        path.join(root, 'nitro-1.21.11'),
        path.join(root, 'nitroclient', 'nitro-1.21.11')
      ];
    }
  });
  hosting = createHostingService({
    userData: app.getPath('userData'),
    getSaveRoots: () => {
      const root = getMinecraftDir();
      return [
        root,
        path.join(root, 'nitro-1.21.11'),
        path.join(root, 'nitro-1.21'),
        path.join(root, 'nitroclient', 'nitro-1.21.11')
      ];
    },
    getHostName: () => loadSettings().username || 'Player',
    sendInvite: (name, text) => {
      if (!friends) throw new Error('Friends are not ready');
      return friends.send(name, text);
    },
    emit: (payload) => {
      if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('hosting-updated', payload);
      }
    }
  });
  createWindow();
  if (!isOwnerBuild()) {
    discordPresence.connectDiscord().catch(() => {});
  }
  const clientRoot = getClientRoot();
  if (!runtimeManager.isBundledClientRoot(clientRoot)) {
    prewarmGradle189(clientRoot, false);
  }
});

app.on('second-instance', () => {
  restoreLauncher();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  } else {
    restoreLauncher();
  }
});

app.on('window-all-closed', () => {
  discordPresence.disconnectDiscord().catch(() => {});
  try { friends?.stop(); } catch (_) { /* ignore */ }
  if (process.platform !== 'darwin') app.quit();
});

ipcMain.handle('get-versions', () => NITRO_VERSIONS);
ipcMain.handle('get-settings', () => loadSettings());
ipcMain.handle('save-settings', (_, data) => {
  const next = { ...loadSettings(), ...data };
  saveSettings(next);
  try {
    if (typeof next.startWithWindows === 'boolean') {
      app.setLoginItemSettings({ openAtLogin: !!next.startWithWindows, path: process.execPath });
    }
  } catch (_) { /* ignore */ }
  return loadSettings();
});

ipcMain.handle('window-minimize', () => {
  if (mainWindow && !mainWindow.isDestroyed()) mainWindow.minimize();
});

ipcMain.handle('window-toggle-maximize', () => {
  if (!mainWindow || mainWindow.isDestroyed()) return false;
  if (mainWindow.isMaximized()) {
    mainWindow.unmaximize();
    return false;
  }
  mainWindow.maximize();
  return true;
});

ipcMain.handle('window-close', () => {
  if (mainWindow && !mainWindow.isDestroyed()) mainWindow.close();
});

ipcMain.handle('cancel-launch', () => {
  cancelActiveLaunch();
  discordPresence.setGamePid(null);
  discordPresence.setLauncherIdle().catch(() => {});
  return { cancelled: true };
});

ipcMain.handle('launch-game', async (_, { versionId, username, memory, joinServer, loginMode }) => {
  if (launchInFlight) {
    throw Object.assign(new Error('A launch is already in progress. Wait a moment or press Stop.'), {
      code: 'LAUNCH_BUSY'
    });
  }
  launchInFlight = true;

  const version = NITRO_VERSIONS.find((v) => v.id === versionId);
  if (!version) {
    launchInFlight = false;
    throw new Error('Unknown version');
  }

  const settings = loadSettings();
  const mode = loginMode || settings.loginMode || 'offline';
  let name = username?.trim().slice(0, 16) || settings.username || 'Player';
  let mclcAuth = null;

  if (mode === 'microsoft') {
    try {
      const remember = settings.rememberMicrosoftLogin !== false;
      const auth = await msAuth.resolveMicrosoftAuth(app.getPath('userData'), mainWindow, {
        remember
      });
      mclcAuth = auth.mclc;
      name = auth.username;
    } catch (err) {
      launchInFlight = false;
      throw new Error('Microsoft sign-in failed. Try again from Settings → Account.');
    }
  } else if (!name) {
    launchInFlight = false;
    throw new Error('Enter a username');
  }

  const memoryMb = memory || 4096;
  const settingsAfter = loadSettings();
  if (joinServer) {
    saveSettings(serverHub.recordLastServer(settingsAfter, joinServer.split(':')[0], joinServer));
  }
  saveSettings({ ...settingsAfter, username: name, selectedVersion: versionId, memory: memoryMb, loginMode: mode });
  lastProgressKey = '';
  lastProgressAt = 0;
  resetLaunchSession();
  launchSession.username = name;
  launchSession.joinServer = joinServer && String(joinServer).trim() ? String(joinServer).trim() : null;
  launchSession.mclcAuth = mclcAuth;

  try {
    if (version.profile === 'nitro-full') {
      const current = loadSettings();
      applyPreset(getClientRoot(), current.modPreset || 'pvp', !!current.performanceMode, getEffectiveGameDir());
      const packPath = current.resourcePackPath;
      if (packPath && fs.existsSync(packPath)) {
        try {
          settingsIo.applyResourcePack(getClientRoot(), packPath, getEffectiveGameDir());
        } catch (_) { /* ignore */ }
      }
    }

    let result;
    if (version.profile === 'nitro-full') {
      result = await launchNitro189(name, memoryMb, mclcAuth);
    } else if (version.profile === 'nitro-modern') {
      result = await launchNitro211Mod(version.mc, name, memoryMb, mclcAuth);
    } else if (version.profile === 'nitro-vanilla') {
      result = await launchNitro211(version.mc, name, memoryMb, mclcAuth);
    } else {
      throw new Error('Unsupported launch profile: ' + version.profile);
    }
    try { friends?.setPlaying(joinServer || ''); } catch (_) { /* ignore */ }
    handOffToGame(
      result?.pid || null,
      version.profile === 'nitro-full' ? getEffectiveGameDir() : null,
      joinServer || DEFAULT_HOST
    );
    return result;
  } catch (err) {
    restoreLauncher();
    writeLaunchLog(formatLaunchError(err, version));
    if (err.code === 'CANCELLED' || launchSession.aborted) {
      throw Object.assign(new Error('Launch cancelled'), { code: 'CANCELLED' });
    }
    const runDir = version.profile === 'nitro-full' ? getPlayerRunDir() : null;
    throw friendlyLaunchError(err, runDir);
  } finally {
    launchInFlight = false;
  }
});

ipcMain.handle('get-microsoft-account', () => msAuth.getMicrosoftAccount(app.getPath('userData')));

ipcMain.handle('get-auth-security', () => msAuth.getSecurityInfo(secureStore.isAvailable()));

ipcMain.handle('microsoft-login', async (_, { remember } = {}) => {
  const settings = loadSettings();
  const keep = remember !== undefined ? remember : settings.rememberMicrosoftLogin !== false;
  const auth = await msAuth.resolveMicrosoftAuth(app.getPath('userData'), mainWindow, {
    forceLogin: true,
    remember: keep
  });
  saveSettings({ ...loadSettings(), loginMode: 'microsoft', username: auth.username, rememberMicrosoftLogin: keep });
  return msAuth.getMicrosoftAccount(app.getPath('userData'));
});

ipcMain.handle('microsoft-logout', () => {
  msAuth.clearSession(app.getPath('userData'));
  const settings = loadSettings();
  saveSettings({ ...settings, loginMode: 'offline' });
  return { ok: true };
});

ipcMain.handle('get-presets', () => listPresets());

ipcMain.handle('apply-preset', (_, { preset, performanceMode }) => {
  const settings = loadSettings();
  const result = applyPreset(
    getClientRoot(),
    preset || settings.modPreset || 'pvp',
    !!performanceMode,
    getEffectiveGameDir()
  );
  saveSettings({
    ...settings,
    modPreset: result.preset,
    performanceMode: !!performanceMode
  });
  return result;
});

ipcMain.handle('copy-text', (_, text) => {
  const { clipboard } = require('electron');
  if (text) clipboard.writeText(String(text));
});

ipcMain.handle('get-server-status', async () => {
  try {
    return await pingServer(DEFAULT_HOST);
  } catch (err) {
    return { online: false, host: DEFAULT_HOST, error: err.message || 'Offline' };
  }
});

ipcMain.handle('get-launcher-meta', async () => loadLauncherMeta(APP_VERSION));

ipcMain.handle('get-owner-mode', () => ({
  enabled: isOwnerBuild()
}));

ipcMain.handle('owner-unlock', (_, password) => {
  if (!isOwnerBuild()) {
    return { ok: false, error: 'Not an owner build' };
  }
  const ok = ownerConfig.verifyOwnerPassword(password);
  return ok ? { ok: true } : { ok: false, error: 'Wrong password' };
});

ipcMain.handle('owner-get-live-config', () => {
  if (!isOwnerBuild()) throw new Error('Owner build only');
  return ownerConfig.loadWorkingCopy(app.getPath('userData'));
});

ipcMain.handle('owner-save-live-config', (_, config) => {
  if (!isOwnerBuild()) throw new Error('Owner build only');
  return ownerConfig.saveWorkingCopy(app.getPath('userData'), config);
});

ipcMain.handle('owner-get-publish-settings', () => {
  if (!isOwnerBuild()) throw new Error('Owner build only');
  return ownerConfig.loadPublishSettings(app.getPath('userData'));
});

ipcMain.handle('owner-save-publish-settings', (_, settings) => {
  if (!isOwnerBuild()) throw new Error('Owner build only');
  return ownerConfig.savePublishSettings(app.getPath('userData'), settings);
});

ipcMain.handle('owner-publish-live-config', async (_, { config, publishUrl, publishToken } = {}) => {
  if (!isOwnerBuild()) throw new Error('Owner build only');
  const userData = app.getPath('userData');
  const working = ownerConfig.saveWorkingCopy(userData, config || ownerConfig.loadWorkingCopy(userData));
  const pub = ownerConfig.loadPublishSettings(userData);
  const url = publishUrl || pub.publishUrl;
  const token = publishToken != null ? publishToken : pub.publishToken;
  try {
    const result = await ownerPublish.publishLiveConfig(working, url, token);
    ownerConfig.savePublishSettings(userData, {
      ...pub,
      publishUrl: url,
      publishToken: token,
      lastPublishAt: Date.now(),
      lastPublishError: ''
    });
    return { ok: true, method: result.method, config: result.config, at: Date.now() };
  } catch (err) {
    ownerConfig.savePublishSettings(userData, {
      ...pub,
      publishUrl: url,
      publishToken: token,
      lastPublishError: err.message || 'Publish failed'
    });
    throw err;
  }
});

ipcMain.handle('owner-export-live-config', async (_, config) => {
  if (!isOwnerBuild()) throw new Error('Owner build only');
  const working = ownerConfig.saveWorkingCopy(
    app.getPath('userData'),
    config || ownerConfig.loadWorkingCopy(app.getPath('userData'))
  );
  return ownerPublish.exportLiveConfigFile(mainWindow, working);
});

ipcMain.handle('repair-install', async () => {
  resetLaunchSession();
  try {
    return await repairInstall();
  } catch (err) {
    writeLaunchLog('Repair failed: ' + (err.message || err));
    throw err;
  }
});

ipcMain.handle('open-external', (_, url) => {
  if (url && /^https?:\/\//i.test(url)) {
    shell.openExternal(url);
  }
});

ipcMain.handle('open-launch-log', () => {
  const logPath = getLaunchLogPath();
  if (!fs.existsSync(logPath)) {
    fs.writeFileSync(logPath, 'No launch attempts logged yet.\n');
  }
  return shell.openPath(logPath);
});

ipcMain.handle('read-launch-log', () => {
  const logPath = getLaunchLogPath();
  if (!fs.existsSync(logPath)) {
    return { path: logPath, text: 'No launch log yet. Hit Play to start logging downloads, mods, and startup.\n' };
  }
  try {
    const text = fs.readFileSync(logPath, 'utf8');
    // Keep the console readable — last ~120KB
    const clipped = text.length > 120000 ? text.slice(text.length - 120000) : text;
    return { path: logPath, text: clipped };
  } catch (err) {
    return { path: logPath, text: 'Could not read launch log: ' + (err.message || 'error') };
  }
});

ipcMain.handle('clear-launch-log', () => {
  const logPath = getLaunchLogPath();
  fs.writeFileSync(logPath, '');
  return { ok: true };
});

ipcMain.handle('ping-server', async (_, host) => {
  const raw = String(host || '').trim();
  if (!raw) return { online: false, error: 'Missing host' };
  const [h, portText] = raw.split(':');
  const port = parseInt(portText, 10) || 25565;
  try {
    return await pingServer(h, port);
  } catch (err) {
    return { online: false, host: h, port, error: err.message || 'Offline' };
  }
});

ipcMain.handle('open-minecraft-folder', () => shell.openPath(getMinecraftDir()));
ipcMain.handle('open-client-folder', () => {
  const bundled = getBundledGameDir();
  const runDir = getPlayerRunDir();
  if (runtimeManager.isBundledClientRoot(getClientRoot()) && fs.existsSync(runDir)) {
    return shell.openPath(runDir);
  }
  return shell.openPath(getClientRoot());
});

ipcMain.handle('get-environment', () => {
  const installRoot = getInstallRoot();
  const clientRoot = getClientRoot();
  return runtimeManager.inspectEnvironment(installRoot, clientRoot, listJava8Installs);
});

ipcMain.handle('setup-java8', async () => {
  resetLaunchSession();
  try {
    const javaPath = await runtimeManager.ensureBundledJava8(getInstallRoot(), (pct, line) => {
      sendLaunchUpdate({ line: line || 'Setting up Java 8…', replace: true, percent: pct, phase: 'prepare' });
    });
    return { ok: true, javaPath };
  } catch (err) {
    throw friendlyLaunchError(err);
  }
});

ipcMain.handle('get-server-hub', () => {
  const settings = loadSettings();
  return {
    servers: serverHub.mergeFavoriteServers(settings),
    lastServer: settings.lastServer || null,
    favorites: settings.favoriteServers || []
  };
});

ipcMain.handle('toggle-favorite-server', (_, server) => {
  const settings = loadSettings();
  const next = serverHub.toggleFavorite(settings, server);
  saveSettings(next);
  return serverHub.mergeFavoriteServers(next);
});

ipcMain.handle('get-mod-toggles', () => modToggles.listModToggles(getClientRoot(), getEffectiveGameDir()));

ipcMain.handle('set-mod-toggle', (_, { id, enabled }) => {
  return modToggles.setModToggle(getClientRoot(), id, enabled, getEffectiveGameDir());
});

ipcMain.handle('export-settings', async () => {
  const result = await dialog.showSaveDialog(mainWindow, {
    title: 'Export Nitro settings',
    defaultPath: 'nitro-settings-backup.json',
    filters: [{ name: 'JSON', extensions: ['json'] }]
  });
  if (result.canceled || !result.filePath) return { cancelled: true };
  return settingsIo.exportSettings(getClientRoot(), app.getPath('userData'), result.filePath, getEffectiveGameDir());
});

ipcMain.handle('import-settings', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Import Nitro settings',
    filters: [{ name: 'JSON', extensions: ['json'] }],
    properties: ['openFile']
  });
  if (result.canceled || !result.filePaths?.[0]) return { cancelled: true };
  return settingsIo.importSettings(getClientRoot(), app.getPath('userData'), result.filePaths[0], getEffectiveGameDir());
});

ipcMain.handle('pick-resource-pack', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Select resource pack',
    properties: ['openFile', 'openDirectory'],
    filters: [{ name: 'Resource pack', extensions: ['zip'] }]
  });
  if (result.canceled || !result.filePaths?.[0]) return { cancelled: true };
  const packPath = result.filePaths[0];
  saveSettings({ ...loadSettings(), resourcePackPath: packPath });
  return { path: packPath };
});

ipcMain.handle('apply-resource-pack', async () => {
  const settings = loadSettings();
  if (!settings.resourcePackPath) throw new Error('No resource pack selected.');
  return settingsIo.applyResourcePack(getClientRoot(), settings.resourcePackPath, getEffectiveGameDir());
});

ipcMain.handle('download-launcher-update', async () => {
  const meta = await loadLauncherMeta(APP_VERSION);
  if (!meta.downloadUrl) {
    throw new Error('Update download is not available yet.');
  }
  shell.openExternal(meta.downloadUrl);
  return { ok: true, url: meta.downloadUrl };
});

ipcMain.handle('get-client-root', () => getClientRoot());

ipcMain.handle('modrinth-search', async (_, { query, mcVersion, loader, limit, offset, index } = {}) => {
  const version = getVersionEntry();
  const mc = mcVersion || version?.mc || '1.21.11';
  const load = loader || getLoaderForVersion(version?.id) || 'fabric';
  const sort = index || ((query && String(query).trim()) ? 'relevance' : 'downloads');
  return modrinth.searchMods(query || '', mc, load, limit || 40, offset || 0, sort);
});

ipcMain.handle('modrinth-install', async (_, { projectId, mcVersion, loader }) => {
  const version = getVersionEntry();
  const mc = mcVersion || version?.mc || '1.21.11';
  const load = loader || getLoaderForVersion(version?.id) || 'fabric';
  if (version.profile === 'nitro-full') {
    throw new Error('Modrinth installs require a Fabric profile (Nitro Modern).');
  }
  const modsDir = getModsDirForVersion(version.id);
  fs.mkdirSync(modsDir, { recursive: true });
  const send = (pct) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('mod-install-progress', { projectId, percent: pct });
    }
  };
  return modrinth.installLatestMod(projectId, mc, load, modsDir, send);
});

ipcMain.handle('list-installed-mods', (_, { versionId } = {}) => {
  const modsDir = getModsDirForVersion(versionId);
  return modrinth.listInstalledMods(modsDir);
});

ipcMain.handle('open-mods-folder', (_, { versionId } = {}) => {
  const modsDir = getModsDirForVersion(versionId);
  fs.mkdirSync(modsDir, { recursive: true });
  return shell.openPath(modsDir);
});

ipcMain.handle('uninstall-mod', (_, { versionId, fileName } = {}) => {
  const modsDir = getModsDirForVersion(versionId);
  return modrinth.uninstallMod(modsDir, fileName);
});

function spotifyGameDir() {
  try {
    return getEffectiveGameDir();
  } catch (_) {
    return path.join(app.getPath('userData'), 'nitro-189');
  }
}

ipcMain.handle('spotify-save-client-id', (_, clientId) => {
  const dir = spotifyGameDir();
  spotifyAuth.saveClientId(dir, clientId);
  return { ok: true, clientId: spotifyAuth.getClientId(dir) };
});

ipcMain.handle('spotify-connect', async (_, { clientId } = {}) => {
  return spotifyAuth.beginLogin(spotifyGameDir(), clientId);
});

ipcMain.handle('spotify-status', async () => {
  return spotifyAuth.getStatus(spotifyGameDir());
});

ipcMain.handle('spotify-disconnect', () => {
  return spotifyAuth.disconnect(spotifyGameDir());
});

ipcMain.handle('friends-state', () => friends?.snapshot() || { me: 'Player', friends: [], incoming: [], outgoing: [], chats: {}, onlineCount: 0 });
ipcMain.handle('friends-search', async (_, name) => friends.search(name));
ipcMain.handle('friends-add', async (_, name) => friends.add(name));
ipcMain.handle('friends-accept', async (_, name) => friends.accept(name));
ipcMain.handle('friends-decline', (_, name) => friends.decline(name));
ipcMain.handle('friends-remove', (_, name) => friends.remove(name));
ipcMain.handle('friends-send', async (_, { name, text } = {}) => friends.send(name, text));
ipcMain.handle('friends-open-chat', (_, name) => friends.openChat(name));
ipcMain.handle('hosting-state', () => hosting?.snapshot());
ipcMain.handle('hosting-select-world', (_, id) => hosting.selectWorld(id));
ipcMain.handle('hosting-create-world', (_, opts) => hosting.createWorld(opts || {}));
ipcMain.handle('hosting-config', (_, opts) => hosting.setConfig(opts || {}));
ipcMain.handle('hosting-start', async (_, opts) => hosting.start(opts || {}));
ipcMain.handle('hosting-stop', async () => hosting.stop());
ipcMain.handle('hosting-refresh', async () => hosting.refreshLive());
ipcMain.handle('hosting-join-info', () => hosting.joinInfo());
ipcMain.handle('hosting-invite', async (_, name) => hosting.inviteFriend(name));
ipcMain.handle('skins-state', () => skins?.snapshot() || { activeId: '', items: [] });
ipcMain.handle('skins-add', async (_, query) => skins.addFromQuery(query));
ipcMain.handle('skins-add-file', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    title: 'Choose a Minecraft skin',
    properties: ['openFile'],
    filters: [{ name: 'Minecraft skin', extensions: ['png'] }]
  });
  if (result.canceled || !result.filePaths?.[0]) return { cancelled: true };
  return skins.addFromFile(result.filePaths[0]);
});
ipcMain.handle('skins-rename', (_, { id, name } = {}) => skins.rename(id, name));
ipcMain.handle('skins-model', (_, { id, model } = {}) => skins.setModel(id, model));
ipcMain.handle('skins-remove', (_, id) => skins.remove(id));
ipcMain.handle('skins-apply', (_, id) => skins.applyToGame(id));

ipcMain.handle('spotify-playback', async (_, { action } = {}) => {
  return spotifyAuth.playback(spotifyGameDir(), action);
});
