const fs = require('fs');
const path = require('path');

const THEMES = [
  { id: 'nitro', name: 'Nitro', accent: '#3db8ff', group: 'core' },
  { id: 'crimson', name: 'Crimson', accent: '#ff2d55', group: 'core' },
  { id: 'end', name: 'End Dimension', accent: '#b388ff', group: 'core' },
  { id: 'deep_dark', name: 'Deep Dark', accent: '#0ae8da', group: 'core' },
  { id: 'nether', name: 'Nether', accent: '#ff5722', group: 'core' },
  { id: 'cherry', name: 'Cherry Grove', accent: '#ff8cb4', group: 'core' },
  { id: 'aurora', name: 'Aurora', accent: '#5cffb1', group: 'core' },
  { id: 'cyber', name: 'Cyber Craft', accent: '#00f0ff', group: 'core' },
  { id: 'galaxy', name: 'Galaxy', accent: '#9d7bff', group: 'core' },
  { id: 'inferno', name: 'Inferno', accent: '#ff3d00', group: 'core' },
  { id: 'frost', name: 'Frost', accent: '#7dd3fc', group: 'core' },
  { id: 'summer_sunset', name: 'Summer Sunset', accent: '#ff6b35', group: 'summer' },
  { id: 'summer_ocean', name: 'Summer Ocean', accent: '#38bdf8', group: 'summer' },
  { id: 'summer_tropical', name: 'Tropical Grove', accent: '#34d399', group: 'summer' },
  { id: 'summer_fireflies', name: 'Firefly Night', accent: '#ffe566', group: 'summer' },
  { id: 'summer_golden', name: 'Golden Hour', accent: '#ffc857', group: 'summer' },
  { id: 'summer_beach', name: 'Beach Breeze', accent: '#7dd3fc', group: 'summer' }
];

const DEFAULT_THEME = 'nitro';
const MODERN_MC = '1.21.11';

function configPath(gameDir) {
  return path.join(gameDir, 'config', 'nitro-client.json');
}

function resolveModernGameDirs(minecraftRoot) {
  const appData = process.env.APPDATA || '';
  const root = minecraftRoot || path.join(appData, 'nitroclient', 'nitroclient');
  const candidates = [
    path.join(root, 'nitro-' + MODERN_MC),
    path.join(appData, 'nitroclient', 'nitroclient', 'nitro-' + MODERN_MC),
    path.join(appData, 'nitroclient', 'nitro-' + MODERN_MC)
  ];
  const seen = new Set();
  const dirs = [];
  for (const dir of candidates) {
    const resolved = path.resolve(dir);
    if (seen.has(resolved)) continue;
    seen.add(resolved);
    dirs.push(resolved);
  }
  return dirs;
}

function primaryModernGameDir(minecraftRoot) {
  const dirs = resolveModernGameDirs(minecraftRoot);
  for (const dir of dirs) {
    if (fs.existsSync(path.join(dir, 'mods'))) return dir;
  }
  return dirs[0];
}

function readConfig(gameDir) {
  const file = configPath(gameDir);
  const defaults = { fancyMainMenu: true, menuTheme: DEFAULT_THEME };
  try {
    if (fs.existsSync(file)) {
      return { ...defaults, ...JSON.parse(fs.readFileSync(file, 'utf8')) };
    }
  } catch (_) { /* ignore */ }
  return { ...defaults };
}

function writeThemeFields(gameDir, themeId) {
  const resolved = resolveThemeId(themeId);
  const file = configPath(gameDir);
  const config = readConfig(gameDir);
  config.menuTheme = resolved;
  config.fancyMainMenu = true;
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(config, null, 2));
  return { path: file, theme: resolved };
}

function resolveThemeId(id) {
  return THEMES.some((t) => t.id === id) ? id : DEFAULT_THEME;
}

function listThemes() {
  return THEMES.map((t) => ({ ...t }));
}

function themeName(id) {
  return THEMES.find((t) => t.id === id)?.name || id;
}

function getMenuThemeState(gameDir) {
  const config = readConfig(gameDir);
  const current = resolveThemeId(config.menuTheme);
  return {
    current,
    currentName: themeName(current),
    themes: listThemes()
  };
}

function setMenuTheme(gameDir, themeId) {
  const written = writeThemeFields(gameDir, themeId);
  return {
    ok: true,
    theme: written.theme,
    name: themeName(written.theme),
    path: written.path
  };
}

function setMenuThemeEverywhere(minecraftRoot, themeId) {
  const resolved = resolveThemeId(themeId);
  const results = [];
  for (const dir of resolveModernGameDirs(minecraftRoot)) {
    fs.mkdirSync(dir, { recursive: true });
    const written = writeThemeFields(dir, resolved);
    results.push(written.path);
  }
  return { ok: true, theme: resolved, name: themeName(resolved), paths: results };
}

function getMenuThemeStateEverywhere(minecraftRoot, launcherTheme) {
  const dir = primaryModernGameDir(minecraftRoot);
  const state = getMenuThemeState(dir);
  if (launcherTheme) {
    state.current = resolveThemeId(launcherTheme);
    state.currentName = themeName(state.current);
  }
  state.configPath = configPath(dir);
  state.gameDir = dir;
  return state;
}

function pickRandomTheme() {
  return THEMES[Math.floor(Math.random() * THEMES.length)].id;
}

module.exports = {
  THEMES,
  DEFAULT_THEME,
  MODERN_MC,
  listThemes,
  getMenuThemeState,
  getMenuThemeStateEverywhere,
  setMenuTheme,
  setMenuThemeEverywhere,
  pickRandomTheme,
  resolveThemeId,
  themeName,
  configPath,
  resolveModernGameDirs,
  primaryModernGameDir
};
