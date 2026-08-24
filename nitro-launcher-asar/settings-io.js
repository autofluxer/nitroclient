const fs = require('fs');
const path = require('path');
const { getGameDataDir } = require('./client-presets');

function collectExportPaths(clientRoot, userDataDir, runDir) {
  const gameDir = getGameDataDir(clientRoot, runDir);
  const paths = {
    launcherSettings: path.join(userDataDir, 'nitro-settings.json'),
    modsJson: path.join(gameDir, 'config', 'nitro-client', 'mods.json'),
    modsJsonLegacy: path.join(gameDir, 'config', 'sol-client', 'mods.json'),
    optionsTxt: path.join(gameDir, 'options.txt')
  };
  return paths;
}

function exportSettings(clientRoot, userDataDir, destFile, runDir) {
  const paths = collectExportPaths(clientRoot, userDataDir, runDir);
  const bundle = {
    exportedAt: new Date().toISOString(),
    version: 1,
    launcherSettings: readJsonSafe(paths.launcherSettings, {}),
    mods: readJsonSafe(paths.modsJson, readJsonSafe(paths.modsJsonLegacy, {})),
    options: readTextSafe(paths.optionsTxt, '')
  };

  fs.mkdirSync(path.dirname(destFile), { recursive: true });
  fs.writeFileSync(destFile, JSON.stringify(bundle, null, 2));
  return { ok: true, path: destFile };
}

function importSettings(clientRoot, userDataDir, srcFile, runDir) {
  const raw = JSON.parse(fs.readFileSync(srcFile, 'utf8'));
  const paths = collectExportPaths(clientRoot, userDataDir, runDir);

  if (raw.launcherSettings && typeof raw.launcherSettings === 'object') {
    writeJsonSafe(paths.launcherSettings, raw.launcherSettings);
  }

  if (raw.mods && typeof raw.mods === 'object') {
    writeJsonSafe(paths.modsJson, raw.mods);
    writeJsonSafe(paths.modsJsonLegacy, raw.mods);
  }

  if (typeof raw.options === 'string' && raw.options.trim()) {
    fs.mkdirSync(path.dirname(paths.optionsTxt), { recursive: true });
    fs.writeFileSync(paths.optionsTxt, raw.options.endsWith('\n') ? raw.options : raw.options + '\n');
  }

  return { ok: true };
}

function readJsonSafe(filePath, fallback) {
  try {
    if (fs.existsSync(filePath)) {
      return JSON.parse(fs.readFileSync(filePath, 'utf8'));
    }
  } catch (_) { /* ignore */ }
  return fallback;
}

function readTextSafe(filePath, fallback) {
  try {
    if (fs.existsSync(filePath)) {
      return fs.readFileSync(filePath, 'utf8');
    }
  } catch (_) { /* ignore */ }
  return fallback;
}

function writeJsonSafe(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2));
}

function applyResourcePack(clientRoot, packPath, runDir) {
  if (!packPath || !fs.existsSync(packPath)) {
    throw new Error('Resource pack file or folder not found.');
  }

  const gameDir = getGameDataDir(clientRoot, runDir);
  const packsDir = path.join(gameDir, 'resourcepacks');
  fs.mkdirSync(packsDir, { recursive: true });

  const baseName = path.basename(packPath);
  const dest = path.join(packsDir, baseName);

  if (fs.statSync(packPath).isDirectory()) {
    copyDirRecursive(packPath, dest);
  } else {
    fs.copyFileSync(packPath, dest);
  }

  return { ok: true, installedTo: dest };
}

function copyDirRecursive(src, dest) {
  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const from = path.join(src, entry.name);
    const to = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyDirRecursive(from, to);
    } else {
      fs.copyFileSync(from, to);
    }
  }
}

module.exports = {
  exportSettings,
  importSettings,
  applyResourcePack,
  collectExportPaths
};
