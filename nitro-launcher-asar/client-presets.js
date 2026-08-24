const fs = require('fs');
const path = require('path');

const PRESETS = {
  pvp: {
    label: 'PvP',
    desc: 'Best for Nitro SMP — responsive combat and clean particles.',
    mods: {
      sol_client: { fancyMainMenu: true, openAnimation: false },
      tweaks: {
        disableBlockParticles: true,
        minimalViewBobbing: true,
        minimalDamageShake: true
      },
      '1.7_visuals': { particles: false },
      motion_blur: { enabled: false },
      menu_blur: { enabled: false },
      chunk_animator: { enabled: false },
      particles: { enabled: false }
    },
    options: {
      renderDistance: 10,
      fancyGraphics: false,
      ao: 1,
      entityShadows: false,
      particles: 0,
      maxFps: 240,
      enableVsync: false
    }
  },
  balanced: {
    label: 'Balanced',
    desc: 'Good looks and solid FPS for everyday play.',
    mods: {
      sol_client: { fancyMainMenu: true, openAnimation: false },
      tweaks: { disableBlockParticles: true },
      '1.7_visuals': { particles: true },
      motion_blur: { enabled: false },
      menu_blur: { enabled: false }
    },
    options: {
      renderDistance: 12,
      fancyGraphics: true,
      ao: 1,
      entityShadows: true,
      particles: 0,
      maxFps: 120,
      enableVsync: false
    }
  },
  fps: {
    label: 'Max FPS',
    desc: 'Strips heavy effects for the highest frame rate.',
    mods: {
      sol_client: { fancyMainMenu: false, openAnimation: false },
      tweaks: {
        disableBlockParticles: true,
        minimalViewBobbing: true,
        minimalDamageShake: true
      },
      '1.7_visuals': { particles: false },
      motion_blur: { enabled: false },
      menu_blur: { enabled: false },
      chunk_animator: { enabled: false },
      particles: { enabled: false }
    },
    options: {
      renderDistance: 8,
      fancyGraphics: false,
      ao: 0,
      entityShadows: false,
      particles: 0,
      maxFps: 0,
      enableVsync: false,
      mipmapLevels: 0
    }
  }
};

const PERFORMANCE_OVERLAY = {
  mods: {
    sol_client: { openAnimation: false },
    motion_blur: { enabled: false },
    menu_blur: { enabled: false },
    chunk_animator: { enabled: false },
    particles: { enabled: false }
  },
  options: {
    renderDistance: 8,
    fancyGraphics: false,
    entityShadows: false,
    particles: 0,
    maxFps: 0,
    enableVsync: false,
    mipmapLevels: 0
  }
};

function getGameDataDir(clientRoot, runDirOverride) {
  if (runDirOverride) return runDirOverride;
  const runDir = path.join(clientRoot, 'run');
  if (fs.existsSync(runDir)) return runDir;
  return clientRoot;
}

function deepMerge(target, source) {
  const out = { ...target };
  for (const [key, value] of Object.entries(source || {})) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      out[key] = deepMerge(out[key] || {}, value);
    } else {
      out[key] = value;
    }
  }
  return out;
}

function readJson(filePath, fallback) {
  try {
    if (fs.existsSync(filePath)) {
      return JSON.parse(fs.readFileSync(filePath, 'utf8'));
    }
  } catch (_) { /* ignore */ }
  return fallback;
}

function writeJson(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2));
}

function patchOptionsFile(optionsPath, patch) {
  const lines = fs.existsSync(optionsPath)
    ? fs.readFileSync(optionsPath, 'utf8').split('\n')
    : [];
  const map = new Map();

  for (const line of lines) {
    const idx = line.indexOf(':');
    if (idx === -1) continue;
    map.set(line.slice(0, idx), line);
  }

  for (const [key, value] of Object.entries(patch)) {
    const formatted = typeof value === 'string' ? value : String(value);
    map.set(key, `${key}:${formatted}`);
  }

  const output = [...map.values()];
  if (!output.some((line) => line.startsWith('lang:'))) {
    output.push('lang:en_US');
  }
  fs.writeFileSync(optionsPath, output.join('\n') + '\n');
}

function applyPreset(clientRoot, presetName, performanceMode = false, runDirOverride) {
  const presetKey = PRESETS[presetName] ? presetName : 'pvp';
  const base = PRESETS[presetKey];
  const preset = {
    mods: performanceMode ? deepMerge(base.mods, PERFORMANCE_OVERLAY.mods) : base.mods,
    options: performanceMode ? { ...base.options, ...PERFORMANCE_OVERLAY.options } : base.options,
    label: base.label + (performanceMode ? ' + Performance' : '')
  };

  const gameDir = getGameDataDir(clientRoot, runDirOverride);
  const configDirs = [
    path.join(gameDir, 'config', 'nitro-client'),
    path.join(gameDir, 'config', 'sol-client'),
    path.join(clientRoot, 'config', 'nitro-client'),
    path.join(clientRoot, 'config', 'sol-client')
  ];

  for (const configDir of configDirs) {
    const modsPath = path.join(configDir, 'mods.json');
    const existing = readJson(modsPath, {});
    const merged = deepMerge(existing, preset.mods);
    writeJson(modsPath, merged);
  }

  const optionsPaths = [
    path.join(gameDir, 'options.txt'),
    path.join(clientRoot, 'options.txt')
  ];

  for (const optionsPath of optionsPaths) {
    if (fs.existsSync(path.dirname(optionsPath)) || optionsPath.includes('run')) {
      patchOptionsFile(optionsPath, preset.options);
    }
  }

  return { preset: presetKey, label: preset.label, gameDir, performanceMode: !!performanceMode };
}

function listPresets() {
  return Object.entries(PRESETS).map(([id, preset]) => ({
    id,
    label: preset.label,
    desc: preset.desc
  }));
}

module.exports = {
  PRESETS,
  applyPreset,
  listPresets,
  getGameDataDir
};
