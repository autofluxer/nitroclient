const fs = require('fs');
const path = require('path');
const https = require('https');
const childProcess = require('child_process');

const ADOPTIUM_JRE8_URL =
  'https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk';

function getBundledJavaCandidates(installRoot) {
  return [
    path.join(installRoot, 'runtime', 'jre8', 'bin', 'java.exe'),
    path.join(installRoot, 'runtime', 'jre8', 'jre', 'bin', 'java.exe')
  ];
}

function findExistingJava8(installRoot, extraCandidates = []) {
  const candidates = [
    ...getBundledJavaCandidates(installRoot),
    ...extraCandidates
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) return candidate;
  }
  return null;
}

function isJava8Executable(javaExe) {
  if (!javaExe || !fs.existsSync(javaExe)) return false;
  try {
    const result = childProcess.spawnSync(javaExe, ['-version'], {
      encoding: 'utf8',
      windowsHide: true,
      timeout: 8000
    });
    const text = `${result.stdout || ''}\n${result.stderr || ''}`;
    return /version "1\.8|version "8\./i.test(text);
  } catch (_) {
    return false;
  }
}

function listSystemJava8Installs() {
  const installs = [];
  const seen = new Set();

  const addInstall = (javaExe) => {
    if (!javaExe || seen.has(javaExe)) return;
    seen.add(javaExe);
    installs.push(javaExe);
  };

  const directCandidates = [
    'C:\\Program Files (x86)\\Common Files\\Oracle\\Java\\java8path\\java.exe',
    process.env.JAVA8_HOME && path.join(process.env.JAVA8_HOME, 'bin', 'java.exe'),
    process.env.JRE8_HOME && path.join(process.env.JRE8_HOME, 'bin', 'java.exe')
  ].filter(Boolean);

  for (const candidate of directCandidates) {
    if (fs.existsSync(candidate) && isJava8Executable(candidate)) {
      addInstall(candidate);
    }
  }

  const roots = [
    'C:\\Program Files\\Java',
    'C:\\Program Files (x86)\\Java',
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\BellSoft',
    process.env.LOCALAPPDATA && path.join(process.env.LOCALAPPDATA, 'Programs', 'Eclipse Adoptium'),
    process.env.USERPROFILE && path.join(process.env.USERPROFILE, '.gradle', 'jdks')
  ].filter(Boolean);

  for (const root of roots) {
    if (!fs.existsSync(root)) continue;
    let entries;
    try {
      entries = fs.readdirSync(root, { withFileTypes: true });
    } catch (_) {
      continue;
    }
    for (const entry of entries) {
      if (!entry.isDirectory()) continue;
      const name = entry.name.toLowerCase();
      if (!name.includes('1.8') && !name.startsWith('jdk-8') && !name.includes('jdk8') && !name.includes('jre-8')) {
        continue;
      }
      const java = path.join(root, entry.name, 'bin', 'java.exe');
      if (fs.existsSync(java) && isJava8Executable(java)) addInstall(java);
    }
  }
  return installs;
}

function downloadFile(url, destPath, onProgress) {
  return new Promise((resolve, reject) => {
    const request = (currentUrl) => {
      https.get(currentUrl, { timeout: 120000 }, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          res.resume();
          request(res.headers.location);
          return;
        }
        if (res.statusCode !== 200) {
          res.resume();
          reject(new Error('Download failed: HTTP ' + res.statusCode));
          return;
        }

        const total = parseInt(res.headers['content-length'] || '0', 10);
        let received = 0;
        const file = fs.createWriteStream(destPath);

        res.on('data', (chunk) => {
          received += chunk.length;
          if (onProgress && total > 0) {
            onProgress(Math.min(100, Math.round((received / total) * 100)));
          }
        });

        res.pipe(file);
        file.on('finish', () => file.close(() => resolve(destPath)));
        file.on('error', (err) => {
          fs.unlink(destPath, () => reject(err));
        });
      }).on('error', reject);
    };
    request(url);
  });
}

function findJavaExeInTree(rootDir, depth = 0) {
  if (!fs.existsSync(rootDir) || depth > 4) return null;
  const direct = path.join(rootDir, 'bin', 'java.exe');
  if (fs.existsSync(direct)) return direct;

  let entries;
  try {
    entries = fs.readdirSync(rootDir, { withFileTypes: true });
  } catch (_) {
    return null;
  }

  for (const entry of entries) {
    if (!entry.isDirectory()) continue;
    const found = findJavaExeInTree(path.join(rootDir, entry.name), depth + 1);
    if (found) return found;
  }
  return null;
}

function extractZip(zipPath, destDir) {
  fs.mkdirSync(destDir, { recursive: true });
  childProcess.execSync(
    `powershell.exe -NoProfile -Command "Expand-Archive -LiteralPath '${zipPath.replace(/'/g, "''")}' -DestinationPath '${destDir.replace(/'/g, "''")}' -Force"`,
    { windowsHide: true, stdio: 'pipe' }
  );
}

async function ensureBundledJava8(installRoot, onProgress) {
  const existing = findExistingJava8(installRoot, listSystemJava8Installs());
  if (existing) return existing;

  const runtimeDir = path.join(installRoot, 'runtime');
  const zipPath = path.join(runtimeDir, 'jre8-download.zip');
  const tempDir = path.join(runtimeDir, 'jre8-temp');
  const finalDir = path.join(runtimeDir, 'jre8');

  fs.mkdirSync(runtimeDir, { recursive: true });
  if (onProgress) onProgress(0, 'Downloading Java 8…');

  await downloadFile(ADOPTIUM_JRE8_URL, zipPath, (pct) => {
    if (onProgress) onProgress(pct, `Downloading Java 8… ${pct}%`);
  });

  if (onProgress) onProgress(100, 'Installing Java 8…');
  if (fs.existsSync(tempDir)) {
    fs.rmSync(tempDir, { recursive: true, force: true });
  }
  extractZip(zipPath, tempDir);

  const javaExe = findJavaExeInTree(tempDir);
  if (!javaExe) {
    throw new Error('Java 8 download finished but java.exe was not found.');
  }

  const extractedRoot = path.dirname(path.dirname(javaExe));
  if (fs.existsSync(finalDir)) {
    fs.rmSync(finalDir, { recursive: true, force: true });
  }
  fs.mkdirSync(path.dirname(finalDir), { recursive: true });
  fs.renameSync(extractedRoot, finalDir);

  try {
    fs.unlinkSync(zipPath);
    fs.rmSync(tempDir, { recursive: true, force: true });
  } catch (_) { /* ignore */ }

  const resolved = findExistingJava8(installRoot);
  if (!resolved) {
    throw new Error('Java 8 install failed.');
  }
  return resolved;
}

function inspectEnvironment(installRoot, clientRoot, listJava8Fn) {
  const bundledClient = isBundledClientRoot(clientRoot);
  const java8 = findExistingJava8(installRoot, listJava8Fn ? listJava8Fn() : listSystemJava8Installs());
  const gradleDev = fs.existsSync(path.join(clientRoot, 'gradlew.bat'));

  return {
    installRoot,
    clientRoot,
    bundledClient,
    gradleDev,
    java8Installed: !!java8,
    java8Path: java8 || null,
    readyToPlay: bundledClient ? !!java8 : (gradleDev && !!java8)
  };
}

function isBundledClientRoot(clientRoot) {
  if (!clientRoot) return false;
  return fs.existsSync(path.join(clientRoot, '.nitro-launch.json'))
    && fs.existsSync(path.join(clientRoot, 'libs'));
}

module.exports = {
  ADOPTIUM_JRE8_URL,
  ensureBundledJava8,
  findExistingJava8,
  listSystemJava8Installs,
  inspectEnvironment,
  isBundledClientRoot,
  getBundledJavaCandidates
};
