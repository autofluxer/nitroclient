const fs = require('fs');
const path = require('path');
const { getGameDataDir } = require('./client-presets');

const LAUNCHER_TOGGLES = [
  { id: 'fps', mod: 'fps', label: 'FPS counter', desc: 'Show frames per second on HUD' },
  { id: 'keystrokes', mod: 'keystrokes', label: 'Keystrokes', desc: 'Display pressed keys' },
  { id: 'cps', mod: 'cps', label: 'CPS', desc: 'Clicks per second counter' },
  { id: 'ping', mod: 'ping', label: 'Ping', desc: 'Network latency display' },
  { id: 'coordinates', mod: 'coordinates', label: 'Coordinates', desc: 'XYZ position overlay' },
  { id: 'armour', mod: 'armour', label: 'Armour HUD', desc: 'Armour durability display' },
  { id: 'tab_list', mod: 'tab_list', label: 'Tab list tweaks', desc: 'Custom tab list styling' },
  { id: 'zoom', mod: 'zoom', label: 'Zoom', desc: 'Optifine-style zoom key' },
  { id: 'toggle_sprint', mod: 'toggle_sprint', label: 'Toggle sprint', desc: 'Sprint without holding key' },
  { id: 'freelook', mod: 'freelook', label: 'Freelook', desc: 'Look around without turning body' }
];

function configPaths(clientRoot, runDir) {
  const gameDir = getGameDataDir(clientRoot, runDir);
  return [
    path.join(gameDir, 'config', 'nitro-client', 'mods.json'),
    path.join(gameDir, 'config', 'sol-client', 'mods.json'),
    path.join(clientRoot, 'config', 'nitro-client', 'mods.json'),
    path.join(clientRoot, 'config', 'sol-client', 'mods.json')
  ];
}

function readModsJson(clientRoot, runDir) {
  for (const filePath of configPaths(clientRoot, runDir)) {
    try {
      if (fs.existsSync(filePath)) {
        return { data: JSON.parse(fs.readFileSync(filePath, 'utf8')), path: filePath };
      }
    } catch (_) { /* ignore */ }
  }
  return { data: {}, path: configPaths(clientRoot, runDir)[0] };
}

function writeModsToAll(clientRoot, data, runDir) {
  const written = [];
  for (const filePath of configPaths(clientRoot, runDir)) {
    try {
      fs.mkdirSync(path.dirname(filePath), { recursive: true });
      fs.writeFileSync(filePath, JSON.stringify(data, null, 2));
      written.push(filePath);
    } catch (_) { /* ignore */ }
  }
  return written;
}

function isModEnabled(modsData, toggle) {
  const node = modsData[toggle.mod];
  if (!node) return true;
  if (toggle.option) {
    return node[toggle.option] !== false;
  }
  return node.enabled !== false;
}

function listModToggles(clientRoot, runDir) {
  const { data } = readModsJson(clientRoot, runDir);
  return LAUNCHER_TOGGLES.map((toggle) => ({
    ...toggle,
    enabled: isModEnabled(data, toggle)
  }));
}

function setModToggle(clientRoot, modId, enabled, runDir) {
  const toggle = LAUNCHER_TOGGLES.find((t) => t.id === modId || t.mod === modId);
  if (!toggle) {
    throw new Error('Unknown mod toggle: ' + modId);
  }

  const { data } = readModsJson(clientRoot, runDir);
  const node = { ...(data[toggle.mod] || {}) };

  if (toggle.option) {
    node[toggle.option] = !!enabled;
  } else {
    node.enabled = !!enabled;
  }

  data[toggle.mod] = node;
  writeModsToAll(clientRoot, data, runDir);
  return listModToggles(clientRoot, runDir);
}

module.exports = {
  LAUNCHER_TOGGLES,
  listModToggles,
  setModToggle,
  readModsJson,
  writeModsToAll
};
