// Official Java release versions from 1.8.9 through 1.21.11
const RELEASE_VERSIONS = [
  '1.8.9',
  '1.9', '1.9.1', '1.9.2', '1.9.3', '1.9.4',
  '1.10', '1.10.1', '1.10.2',
  '1.11', '1.11.1', '1.11.2',
  '1.12', '1.12.1', '1.12.2',
  '1.13', '1.13.1', '1.13.2',
  '1.14', '1.14.1', '1.14.2', '1.14.3', '1.14.4',
  '1.15', '1.15.1', '1.15.2',
  '1.16', '1.16.1', '1.16.2', '1.16.3', '1.16.4', '1.16.5',
  '1.17', '1.17.1',
  '1.18', '1.18.1', '1.18.2',
  '1.19', '1.19.1', '1.19.2', '1.19.3', '1.19.4',
  '1.20', '1.20.1', '1.20.2', '1.20.3', '1.20.4', '1.20.5', '1.20.6',
  '1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6',
  '1.21.7', '1.21.8', '1.21.9', '1.21.10', '1.21.11'
];

const POPULAR_VERSIONS = new Set([
  '1.8.9', '1.12.2', '1.16.5', '1.20.1', '1.21.1', '1.21.11'
]);

/**
 * Config-driven modern Nitro launcher profiles.
 * Add entries here for 1.20.x / 1.19.x — no renderer rewrite needed.
 */
const MODERN_PROFILES = [
  {
    mc: '1.21.11',
    id: 'nitro-modern-1.21.11',
    label: 'Nitro Modern 1.21.11',
    tag: 'RECOMMENDED',
    popular: true,
    recommended: true,
    desc: 'Latest Minecraft with Nitro launcher support — PvP-ready base, SMP join, and auto downloads.',
    features: [
      'Modern PvP-ready base',
      'Microsoft + offline login',
      'Quick SMP launch',
      'Auto downloads'
    ]
  }
];

const MODERN_MCS = new Set(MODERN_PROFILES.map((profile) => profile.mc));

function compareMcVersion(a, b) {
  const pa = a.split('.').map(Number);
  const pb = b.split('.').map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const da = pa[i] || 0;
    const db = pb[i] || 0;
    if (da !== db) return da - db;
  }
  return 0;
}

function getModernProfile(mc) {
  return MODERN_PROFILES.find((profile) => profile.mc === mc) || null;
}

function getRecommendedModernId() {
  const recommended = MODERN_PROFILES.find((profile) => profile.recommended);
  return recommended ? recommended.id : (MODERN_PROFILES[0]?.id || null);
}

function buildModernVersionEntry(profile) {
  return {
    id: profile.id,
    label: profile.label,
    mc: profile.mc,
    profile: 'nitro-modern',
    tag: profile.tag,
    popular: !!profile.popular,
    recommended: !!profile.recommended,
    desc: profile.desc,
    features: profile.features,
    thumb: profile.thumb || 'assets/bg_jungle_2.png'
  };
}

function versionThumb(mc, profile) {
  if (profile === 'nitro-full') return 'assets/bg_jungle_1.png';
  if (profile === 'nitro-modern') return 'assets/bg_jungle_2.png';
  const major = mc.split('.');
  const patch = parseInt(major[1] || '0', 10);
  if (patch >= 20) return 'assets/bg_jungle_2.png';
  if (patch >= 16) return 'assets/bg_jungle_1.png';
  return 'assets/bg_jungle_1.png';
}

function buildVersions() {
  const list = [{
    id: 'nitro-1.8.9',
    label: 'Nitro Client 1.8.9',
    mc: '1.8.9',
    profile: 'nitro-full',
    tag: 'NITRO',
    popular: true,
    desc: 'Mods, HUD, and the Nitro main menu',
    thumb: 'assets/bg_jungle_1.png'
  }];

  for (const modernProfile of MODERN_PROFILES) {
    list.push(buildModernVersionEntry(modernProfile));
  }

  const vanillaMcs = RELEASE_VERSIONS
    .filter((mc) => mc !== '1.8.9' && !MODERN_MCS.has(mc))
    .sort((a, b) => compareMcVersion(b, a));

  for (const mc of vanillaMcs) {
    const popular = POPULAR_VERSIONS.has(mc);
    list.push({
      id: `nitro-${mc}`,
      label: `Minecraft ${mc}`,
      mc,
      profile: 'nitro-vanilla',
      tag: popular ? 'POPULAR' : 'VANILLA',
      popular,
      desc: 'Vanilla Minecraft launched through Nitro',
      thumb: versionThumb(mc, 'nitro-vanilla')
    });
  }

  return list;
}

module.exports = {
  buildVersions,
  compareMcVersion,
  MODERN_PROFILES,
  getModernProfile,
  getRecommendedModernId
};
