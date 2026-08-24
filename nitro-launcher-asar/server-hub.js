const DEFAULT_SERVERS = [
  {
    id: 'nitro-smp',
    name: 'Nitro SMP',
    host: 'nitrosmp.lol',
    tag: 'OFFICIAL',
    featured: true,
    description: 'Official Nitro survival SMP',
    icon: 'assets/icon.png'
  },
  {
    id: 'hypixel',
    name: 'Hypixel',
    host: 'mc.hypixel.net',
    tag: 'PARTNER',
    description: 'The largest Minecraft server network',
    icon: 'https://api.mcsrvstat.us/icon/mc.hypixel.net'
  },
  {
    id: 'minehut',
    name: 'Minehut',
    host: 'minehut.com',
    tag: 'PARTNER',
    description: 'Create and join community servers',
    icon: 'https://api.mcsrvstat.us/icon/minehut.com'
  },
  {
    id: 'cubecraft',
    name: 'CubeCraft',
    host: 'play.cubecraft.net',
    tag: 'PARTNER',
    description: 'Minigames and skyblock',
    icon: 'https://api.mcsrvstat.us/icon/play.cubecraft.net'
  }
];

function normalizeServer(server) {
  return {
    id: server.id,
    name: server.name || server.host,
    host: server.host,
    tag: server.tag || 'SERVER',
    featured: !!server.featured,
    description: server.description || '',
    icon: server.icon || (server.host ? `https://api.mcsrvstat.us/icon/${server.host}` : '')
  };
}

function getDefaultServers() {
  return DEFAULT_SERVERS.map(normalizeServer);
}

function mergeFavoriteServers(settings) {
  const favorites = Array.isArray(settings.favoriteServers) ? settings.favoriteServers : [];
  const defaults = getDefaultServers();
  const byHost = new Map(defaults.map((s) => [s.host.toLowerCase(), s]));

  for (const fav of favorites) {
    if (!fav?.host) continue;
    const key = fav.host.toLowerCase();
    if (!byHost.has(key)) {
      byHost.set(key, normalizeServer({
        id: fav.id || key.replace(/\W+/g, '-'),
        name: fav.name || fav.host,
        host: fav.host,
        tag: fav.tag || 'FAVORITE',
        description: fav.description || 'Saved server'
      }));
    }
  }

  return [...byHost.values()];
}

function recordLastServer(settings, host, name) {
  if (!host) return settings;
  return {
    ...settings,
    lastServer: { host, name: name || host, at: Date.now() }
  };
}

function toggleFavorite(settings, server) {
  const favorites = Array.isArray(settings.favoriteServers) ? [...settings.favoriteServers] : [];
  const host = server.host.toLowerCase();
  const idx = favorites.findIndex((s) => s.host?.toLowerCase() === host);

  if (idx === -1) {
    favorites.push({
      id: server.id,
      name: server.name,
      host: server.host,
      tag: server.tag || 'FAVORITE'
    });
  } else {
    favorites.splice(idx, 1);
  }

  return { ...settings, favoriteServers: favorites };
}

function isFavorite(settings, host) {
  const favorites = settings.favoriteServers || [];
  return favorites.some((s) => s.host?.toLowerCase() === host?.toLowerCase());
}

module.exports = {
  getDefaultServers,
  mergeFavoriteServers,
  recordLastServer,
  toggleFavorite,
  isFavorite,
  normalizeServer
};
