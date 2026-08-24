const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');

const ASSET_INDEX = '1.8.9-1.8.json';
const MIN_ASSET_OBJECTS = 400;

function isReplayJarName(name) {
  return /replay/i.test(name || '');
}

function removeReplayJars(libsDir) {
  if (!fs.existsSync(libsDir)) return 0;
  let removed = 0;
  for (const name of fs.readdirSync(libsDir)) {
    if (!name.endsWith('.jar') || !isReplayJarName(name)) continue;
    try {
      fs.unlinkSync(path.join(libsDir, name));
      removed++;
    } catch (_) { /* ignore */ }
  }
  return removed;
}

function countAssetObjects(assetsDir) {
  const objectsDir = path.join(assetsDir, 'objects');
  if (!fs.existsSync(objectsDir)) return 0;
  let count = 0;
  const stack = [objectsDir];
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
      if (entry.isDirectory()) stack.push(full);
      else count++;
    }
  }
  return count;
}

function resolveGradleAssetsDir() {
  const home = process.env.USERPROFILE || process.env.HOME;
  if (!home) return null;
  const candidate = path.join(home, '.gradle', 'caches', 'quilt-loom', 'assets');
  return fs.existsSync(path.join(candidate, 'indexes', ASSET_INDEX)) ? candidate : null;
}

function copyDirRecursive(src, dest) {
  if (!fs.existsSync(src)) return;
  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const from = path.join(src, entry.name);
    const to = path.join(dest, entry.name);
    if (entry.isDirectory()) copyDirRecursive(from, to);
    else if (!fs.existsSync(to)) fs.copyFileSync(from, to);
  }
}

function syncAssets(bundleRoot, sourceAssetsDir) {
  const dest = path.join(bundleRoot, 'assets');
  const indexPath = path.join(dest, 'indexes', ASSET_INDEX);
  const needsCopy = !fs.existsSync(indexPath) || countAssetObjects(dest) < MIN_ASSET_OBJECTS;
  if (!needsCopy || !sourceAssetsDir) {
    return { copied: false, objectCount: countAssetObjects(dest) };
  }
  if (fs.existsSync(dest)) {
    fs.rmSync(dest, { recursive: true, force: true });
  }
  copyDirRecursive(sourceAssetsDir, dest);
  return { copied: true, objectCount: countAssetObjects(dest) };
}

function rewriteBundledLaunchConfig(bundleRoot) {
  const script = path.join(__dirname, 'scripts', 'rewrite-bundled-launch.ps1');
  if (!fs.existsSync(script)) {
    throw new Error('Launcher repair script is missing. Reinstall Nitro Client.');
  }
  childProcess.execFileSync(
    'powershell.exe',
    ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', script, '-BundleDir', bundleRoot],
    { windowsHide: true, stdio: 'pipe' }
  );
}

function validateBundledInstall(clientRoot) {
  const issues = [];
  const libsDir = path.join(clientRoot, 'libs');
  const clientJar = path.join(libsDir, 'nitro-client.jar');
  const launchConfig = path.join(clientRoot, '.nitro-launch.json');
  const assetsIndex = path.join(clientRoot, 'assets', 'indexes', ASSET_INDEX);

  if (!fs.existsSync(clientJar)) issues.push('missing nitro-client.jar');
  if (!fs.existsSync(launchConfig)) issues.push('missing launch config');
  if (!fs.existsSync(assetsIndex)) issues.push('missing Minecraft assets');
  else if (countAssetObjects(path.join(clientRoot, 'assets')) < MIN_ASSET_OBJECTS) {
    issues.push('incomplete Minecraft assets');
  }
  if (fs.existsSync(libsDir)) {
    const jars = fs.readdirSync(libsDir).filter((n) => n.endsWith('.jar'));
    if (jars.length < 50) issues.push('incomplete game libraries');
    if (jars.some(isReplayJarName)) issues.push('outdated replay mod files');
  } else {
    issues.push('missing game libraries');
  }
  return issues;
}

function syncFromLauncherBundle(installGameDir, sourceGameDir) {
  if (!sourceGameDir || !fs.existsSync(sourceGameDir)) return { synced: 0 };
  let synced = 0;
  const pairs = [
    ['libs', 'libs'],
    ['natives', 'natives'],
    ['classpath-order.txt', 'classpath-order.txt'],
    ['remapClasspath.txt', 'remapClasspath.txt'],
    ['log4j2.xml', 'log4j2.xml'],
    ['launch.cfg', 'launch.cfg']
  ];

  for (const [rel, relDest] of pairs) {
    const from = path.join(sourceGameDir, rel);
    const to = path.join(installGameDir, relDest);
    if (!fs.existsSync(from)) continue;
    if (rel.endsWith('.txt') || rel.endsWith('.xml') || rel.endsWith('.cfg')) {
      fs.mkdirSync(path.dirname(to), { recursive: true });
      fs.copyFileSync(from, to);
      synced++;
      continue;
    }
    if (!fs.existsSync(to)) fs.mkdirSync(to, { recursive: true });
    for (const entry of fs.readdirSync(from, { withFileTypes: true })) {
      const srcPath = path.join(from, entry.name);
      const destPath = path.join(to, entry.name);
      if (isReplayJarName(entry.name)) continue;
      if (entry.isDirectory()) {
        if (!fs.existsSync(destPath)) {
          copyDirRecursive(srcPath, destPath);
          synced++;
        }
      } else if (!fs.existsSync(destPath)) {
        fs.copyFileSync(srcPath, destPath);
        synced++;
      }
    }
  }
  return { synced };
}

function diagnoseLaunchFailure(message, runDir) {
  const raw = message || 'Launch failed';
  const log = runDir ? readLogSnippet(runDir) : '';

  if (raw.includes('Java 8') || raw.includes('JAVA8')) {
    return {
      message: 'Java 8 is required for Nitro 1.8.9. Open Settings → Java setup.',
      code: 'JAVA8_MISSING'
    };
  }
  if (raw.includes('incomplete') || raw.includes('nitro-client.jar') || raw.includes('main class')) {
    return {
      message: 'Game files are incomplete. Open Settings → Repair install, then try Play again.',
      code: 'BUNDLE_INCOMPLETE'
    };
  }
  if (log.includes('NullPointerException') && log.includes('SplashScreen')) {
    return {
      message: 'The loading screen crashed. Run Repair install in Settings to update game files.',
      code: 'GAME_CRASH'
    };
  }
  if (raw.includes('exited immediately') || (log.includes('Stopping server') && !log.includes('Loading '))) {
    return {
      message: 'Minecraft closed during startup. Run Repair install in Settings, then try Play again.',
      code: 'EARLY_EXIT'
    };
  }
  if (raw.includes('Gradle failed')) {
    return { message: raw, code: 'GRADLE_FAIL' };
  }
  if (log.includes('Launch error')) {
    return {
      message: 'Minecraft crashed on startup. Run Repair install in Settings.',
      code: 'GAME_CRASH'
    };
  }
  return { message: raw, code: 'LAUNCH_FAIL' };
}

function readLogSnippet(runDir) {
  const logPath = path.join(runDir, 'logs', 'latest.log');
  if (!fs.existsSync(logPath)) return '';
  try {
    return fs.readFileSync(logPath, 'utf8').slice(-1200);
  } catch (_) {
    return '';
  }
}

module.exports = {
  ASSET_INDEX,
  MIN_ASSET_OBJECTS,
  removeReplayJars,
  countAssetObjects,
  resolveGradleAssetsDir,
  syncAssets,
  rewriteBundledLaunchConfig,
  validateBundledInstall,
  syncFromLauncherBundle,
  diagnoseLaunchFailure,
  isReplayJarName
};
