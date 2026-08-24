let versions = [];
let selectedId = 'nitro-1.8.9';
let launching = false;
let launchStartedAt = 0;
let launcherMeta = null;
let smpStatus = null;

const NITRO_SMP = 'nitrosmp.lol';
const DISCORD_URL = 'https://discord.gg/nitrosmp';
const STORE_URL = 'https://nitrosmp.lol';

let modPreset = 'pvp';
let performanceMode = false;
let loginMode = 'offline';
let rememberMicrosoftLogin = true;

const FEATURES = {
  'nitro-1.8.9': ['Module menu', 'Custom HUD', 'PvP tweaks']
};

const VANILLA_FEATURES = ['Vanilla gameplay', 'Offline login', 'Auto downloads'];

const LAUNCHER_THEMES = [
  { id: 'jungle', name: 'Jungle', thumb: 'assets/bg_jungle_2.png' },
  { id: 'classic', name: 'Classic', thumb: 'assets/bg_jungle_1.png' },
  { id: 'night', name: 'Night', thumb: 'assets/bg_jungle_2.png' },
  { id: 'summer', name: 'Summer', thumb: 'assets/bg_jungle_1.png' },
  { id: 'void', name: 'Void', thumb: '' }
];

let launcherThemeId = localStorage.getItem('nitro.launcherTheme') || 'jungle';

const els = {
  username: document.getElementById('username'),
  avatar: document.getElementById('avatar'),
  avatarLarge: document.getElementById('avatarLarge'),
  accountChipName: document.getElementById('accountChipName'),
  accountChipBtn: document.getElementById('accountChipBtn'),
  accountDropdown: document.getElementById('accountDropdown'),
  accountChipWrap: document.getElementById('accountChipWrap'),
  accountDropTitle: document.getElementById('accountDropTitle'),
  accountDropSub: document.getElementById('accountDropSub'),
  accountSettingsBtn: document.getElementById('accountSettingsBtn'),
  accountManageBtn: document.getElementById('accountManageBtn'),
  themeModal: document.getElementById('themeModal'),
  themeGrid: document.getElementById('themeGrid'),
  themeSearch: document.getElementById('themeSearch'),
  themeCount: document.getElementById('themeCount'),
  themeActiveLabel: document.getElementById('themeActiveLabel'),
  themeModalClose: document.getElementById('themeModalClose'),
  themeResetBtn: document.getElementById('themeResetBtn'),
  openThemeModal: document.getElementById('openThemeModal'),
  homeShell: document.getElementById('homeShell'),
  selectedLabel: document.getElementById('selectedLabel'),
  selectedHint: document.getElementById('selectedHint'),
  profileBadge: document.getElementById('profileBadge'),
  heroFeatures: document.getElementById('heroFeatures'),
  metaVersion: document.getElementById('metaVersion'),
  dockVerPill: document.getElementById('dockVerPill'),
  launchBtn: document.getElementById('launchBtn'),
  launchBtnLabel: document.getElementById('launchBtnLabel'),
  launchBtnSub: document.getElementById('launchBtnSub'),
  launchStatus: document.getElementById('launchStatus'),
  statusText: document.getElementById('statusText'),
  statusPct: document.getElementById('statusPct'),
  progressFill: document.getElementById('progressFill'),
  versionGrid: document.getElementById('versionGrid'),
  versionSearch: document.getElementById('versionSearch'),
  memoryInput: document.getElementById('memoryInput'),
  memorySlider: document.getElementById('memorySlider'),
  switchVersionBtn: document.getElementById('switchVersionBtn'),
  toast: document.getElementById('toast'),
  smpDot: document.getElementById('smpDot'),
  smpStatusText: document.getElementById('smpStatusText'),
  smpCredit: document.getElementById('smpCredit'),
  smpMotd: document.getElementById('smpMotd'),
  smpMeta: document.getElementById('smpMeta'),
  smpPing: document.getElementById('smpPing'),
  playSmpBtn: document.getElementById('playSmpBtn'),
  newsList: document.getElementById('newsList'),
  newsPanel: document.getElementById('newsPanel'),
  launcherVersionLabel: document.getElementById('launcherVersionLabel'),
  changelogList: document.getElementById('changelogList'),
  updateBanner: document.getElementById('updateBanner'),
  updateBannerText: document.getElementById('updateBannerText'),
  updateDownloadBtn: document.getElementById('updateDownloadBtn'),
  repairBtn: document.getElementById('repairBtn'),
  launchSoloBtn: document.getElementById('launchSoloBtn'),
  copyIpBtn: document.getElementById('copyIpBtn'),
  joinSmpBtn: document.getElementById('joinSmpBtn'),
  discordBtn: document.getElementById('discordBtn'),
  presetGrid: document.getElementById('presetGrid'),
  performanceModeInput: document.getElementById('performanceMode'),
  onboarding: document.getElementById('onboarding'),
  onboardingDone: document.getElementById('onboardingDone'),
  microsoftStatus: document.getElementById('microsoftStatus'),
  microsoftLoginBtn: document.getElementById('microsoftLoginBtn'),
  microsoftLogoutBtn: document.getElementById('microsoftLogoutBtn'),
  rememberMicrosoft: document.getElementById('rememberMicrosoft'),
  accountMode: document.getElementById('accountMode'),
  serverHub: document.getElementById('serverHub'),
  serverHubGrid: document.getElementById('serverHubGrid'),
  lastServerLabel: document.getElementById('lastServerLabel'),
  modToggleGrid: document.getElementById('modToggleGrid'),
  pickResourcePackBtn: document.getElementById('pickResourcePackBtn'),
  applyResourcePackBtn: document.getElementById('applyResourcePackBtn'),
  resourcePackLabel: document.getElementById('resourcePackLabel'),
  exportSettingsBtn: document.getElementById('exportSettingsBtn'),
  importSettingsBtn: document.getElementById('importSettingsBtn'),
  javaSetupStatus: document.getElementById('javaSetupStatus'),
  setupJavaBtn: document.getElementById('setupJavaBtn'),
  securityPanelList: document.getElementById('securityPanelList'),
  javaWizard: document.getElementById('javaWizard'),
  javaWizardInstall: document.getElementById('javaWizardInstall'),
  javaWizardSkip: document.getElementById('javaWizardSkip'),
  heroPlayerName: document.getElementById('heroPlayerName'),
  footerVersion: document.getElementById('footerVersion'),
  settingsRail: document.getElementById('settingsRail'),
  bgVideoToggle: document.getElementById('bgVideoToggle'),
  reduceMotionToggle: document.getElementById('reduceMotionToggle'),
  openThemeFromSettings: document.getElementById('openThemeFromSettings'),
  pillVersion: document.getElementById('pillVersion'),
  pillLoader: document.getElementById('pillLoader'),
  pillJava: document.getElementById('pillJava'),
  pillRam: document.getElementById('pillRam'),
  skinStageAvatar: document.getElementById('skinStageAvatar'),
  skinBody: document.getElementById('skinBody'),
  avatarImg: document.getElementById('avatarImg'),
  onlinePill: document.getElementById('onlinePill'),
  accountsAvatar: document.getElementById('accountsAvatar'),
  accountsName: document.getElementById('accountsName'),
  accountsModeBadge: document.getElementById('accountsModeBadge'),
  accountsModeHint: document.getElementById('accountsModeHint'),
  accountsGotoSettings: document.getElementById('accountsGotoSettings'),
  addAccountBtn: document.getElementById('addAccountBtn'),
  settingsOpenAccounts: document.getElementById('settingsOpenAccounts'),
  smpCard: document.getElementById('smpCard'),
  viewAllServersBtn: document.getElementById('viewAllServersBtn'),
  openClientFolderHome: document.getElementById('openClientFolderHome'),
  openLaunchLogHome: document.getElementById('openLaunchLogHome'),
  profilesFocusSearch: document.getElementById('profilesFocusSearch'),
  partnerList: document.getElementById('partnerList'),
  partnersPageList: document.getElementById('partnersPageList'),
  modSearch: document.getElementById('modSearch'),
  modGrid: document.getElementById('modGrid'),
  installedModList: document.getElementById('installedModList'),
  modsProfileHint: document.getElementById('modsProfileHint'),
  openModsFolderBtn: document.getElementById('openModsFolderBtn'),
  homePartnerList: document.getElementById('homePartnerList')
};

let PARTNER_SERVERS = [
  { name: 'Nitro SMP', host: 'nitrosmp.lol', icon: 'assets/icon.png', featured: true, description: 'Official Nitro survival SMP' },
  { name: 'Hypixel', host: 'mc.hypixel.net', icon: 'https://api.mcsrvstat.us/icon/mc.hypixel.net', description: 'The largest Minecraft server network' },
  { name: 'Minehut', host: 'minehut.com', icon: 'https://api.mcsrvstat.us/icon/minehut.com', description: 'Create and join community servers' },
  { name: 'CubeCraft', host: 'play.cubecraft.net', icon: 'https://api.mcsrvstat.us/icon/play.cubecraft.net', description: 'Minigames and skyblock' }
];

let remotePartnersActive = false;
let liveMetaPollTimer = null;
let ownerBuild = false;
let ownerUnlocked = false;
let ownerLiveConfig = null;
let videoBuild = false;

function showToast(msg) {
  if (!els.toast) return;
  els.toast.textContent = msg;
  els.toast.classList.add('show');
  setTimeout(() => els.toast.classList.remove('show'), 3200);
}
window.showToast = showToast;

function launchErrorHint(error) {
  const code = error?.code;
  if (code === 'JAVA8_MISSING') return 'Java 8 is required — open Settings → Support & Recovery.';
  if (code === 'JAVA21_MISSING') return 'Java 21 is required for Minecraft 1.21.11 — install JDK 21.';
  if (code === 'LAUNCH_BUSY') return 'Launch already running — wait a second.';
  if (code === 'LAUNCH_FAIL' || code === 'MOD121_MISSING') return (error.message || 'Launch failed') + ' Try Play again after setup finishes.';
  if (code === 'BUNDLE_INCOMPLETE' || code === 'GAME_CRASH' || code === 'EARLY_EXIT') {
    return (error.message || 'Launch failed') + ' Try Settings → Repair install.';
  }
  if (code === 'GRADLE_FAIL') return 'Try Repair install in Settings.';
  return error?.message || 'Launch failed';
}

function isNitroFull(id = selectedId) {
  const v = versions.find((x) => x.id === id);
  return v?.profile === 'nitro-full';
}

function isNitroModern(id = selectedId) {
  const v = versions.find((x) => x.id === id);
  return v?.profile === 'nitro-modern';
}

function getDefaultVersionId() {
  const recommended = versions.find((v) => v.recommended);
  return recommended?.id || 'nitro-1.8.9';
}

function resolveSelectedVersionId(savedId) {
  if (!savedId) return getDefaultVersionId();
  if (savedId === 'nitro-1.21.11') return 'nitro-modern-1.21.11';
  if (versions.some((v) => v.id === savedId)) return savedId;
  return getDefaultVersionId();
}

function updateNitroOnlySettings() {
  const showNitro189 = isNitroFull();
  document.querySelectorAll('[data-nitro-only]').forEach((el) => {
    el.hidden = !showNitro189;
  });
}

function applyHomeTheme(v) {
  if (!els.homeShell) return;
  els.homeShell.classList.toggle('theme-vanilla', v.profile === 'nitro-vanilla');
  els.homeShell.classList.toggle('theme-modern', v.profile === 'nitro-modern');
  if (els.profileBadge) {
    els.profileBadge.classList.toggle('badge-modern', v.profile === 'nitro-modern');
    els.profileBadge.classList.toggle('badge-nitro', v.profile === 'nitro-full');
  }
}

function formatMotdLine(motd) {
  if (!motd) return 'Nitro SMP';
  const line = motd.split('\n').map((s) => s.trim()).find(Boolean) || motd;
  return line.length > 72 ? line.slice(0, 69) + '…' : line;
}

function mapLaunchStage(line, percent, phase) {
  const text = String(line || '').toLowerCase();
  if (phase === 'done' || percent >= 100) return 'Launching Minecraft…';
  if (phase === 'error') return line || 'Launch failed';
  if (phase === 'cancelled') return 'Launch cancelled';
  if (/java|jdk/.test(text)) return 'Initializing Nitro Client…';
  if (/download|asset|librar|fabric|mod/.test(text)) return 'Loading assets…';
  if (/verif|check|repair|hash/.test(text)) return 'Verifying files…';
  if (/prepar|config|install/.test(text)) return 'Preparing Minecraft…';
  if (/launch|start|opening|spawn|waiting/.test(text)) return 'Launching Minecraft…';
  if (typeof percent === 'number') {
    if (percent < 20) return 'Initializing Nitro Client…';
    if (percent < 45) return 'Loading assets…';
    if (percent < 70) return 'Verifying files…';
    if (percent < 90) return 'Preparing Minecraft…';
    return 'Launching Minecraft…';
  }
  return line || 'Starting…';
}

function launchButtonLabel() {
  const v = versions.find((x) => x.id === selectedId);
  const loader = v?.profile === 'nitro-vanilla' ? 'VANILLA' : (v?.profile === 'nitro-full' ? 'LEGACY' : 'FABRIC');
  return 'LAUNCH ' + loader;
}

function updatePlayButtons() {
  if (!launching && els.launchBtnLabel) {
    els.launchBtnLabel.textContent = launchButtonLabel();
  }
  renderFriends();
  if (els.playSmpBtn) els.playSmpBtn.hidden = true;
  if (els.launchSoloBtn) els.launchSoloBtn.hidden = true;
}

let launchCancelHintTimer = null;

function setLaunching(active) {
  launching = active;
  document.body.classList.toggle('is-launching', active);
  els.launchBtn?.classList.toggle('is-loading', active);
  els.launchBtn?.classList.toggle('is-stopping', false);
  if (els.launchBtn) els.launchBtn.setAttribute('aria-busy', active ? 'true' : 'false');
  if (els.playSmpBtn) els.playSmpBtn.disabled = active;
  if (els.joinSmpBtn) els.joinSmpBtn.disabled = active;
  window.nitroSkin?.setLaunchMode?.(active);
  if (launchCancelHintTimer) {
    clearTimeout(launchCancelHintTimer);
    launchCancelHintTimer = null;
  }
  if (active) {
    if (els.launchBtnLabel) els.launchBtnLabel.textContent = 'Initializing Nitro Client…';
    launchCancelHintTimer = setTimeout(() => {
      if (!launching || !els.launchBtnLabel) return;
      els.launchBtnLabel.textContent = 'Cancel';
      els.launchBtn?.classList.add('is-stopping');
    }, 2500);
  } else {
    const fill = document.getElementById('playProgressFill');
    if (fill) fill.style.width = '0%';
    document.body.classList.remove('launch-fade');
  }
  updatePlayButtons();
}

function setLaunchUi({ line, percent, phase, idle = false }) {
  els.launchStatus?.classList.toggle('idle', idle);
  const stage = idle ? null : mapLaunchStage(line, percent, phase);
  if (line && els.statusText) els.statusText.textContent = line;
  if (launching && stage && els.launchBtnLabel) {
    const canCancel = Date.now() - launchStartedAt >= 2500;
    els.launchBtnLabel.textContent = canCancel ? 'Cancel' : stage;
    els.launchBtn?.classList.toggle('is-stopping', canCancel);
  }
  if (typeof percent === 'number') {
    if (els.progressFill) els.progressFill.style.width = percent + '%';
    if (els.statusPct) els.statusPct.textContent = percent + '%';
    const playFill = document.getElementById('playProgressFill');
    if (playFill) playFill.style.width = Math.max(0, Math.min(100, percent)) + '%';
  } else if (idle) {
    if (els.progressFill) els.progressFill.style.width = '0%';
    if (els.statusPct) els.statusPct.textContent = '';
    const playFill = document.getElementById('playProgressFill');
    if (playFill) playFill.style.width = '0%';
  }
  if (phase === 'done') {
    if (els.launchBtnLabel) els.launchBtnLabel.textContent = 'Launching Minecraft…';
    const playFill = document.getElementById('playProgressFill');
    if (playFill) playFill.style.width = '100%';
    document.body.classList.add('launch-fade');
    setTimeout(() => document.body.classList.remove('launch-fade'), 1200);
    setTimeout(() => { if (!launching) resetLaunchUi(); }, 3000);
  }
  if (phase === 'cancelled') {
    setTimeout(() => {
      if (!launching) resetLaunchUi();
    }, 1200);
  }
}

function resetLaunchUi() {
  setLaunchUi({ line: 'Ready to play', idle: true });
  updatePlayButtons();
}

function runBootSequence() {
  const splash = document.getElementById('bootSplash');
  const reduce = document.body.classList.contains('reduce-motion')
    || window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const splashMs = reduce ? 140 : 2800;
  const fadeMs = reduce ? 80 : 600;
  let stopParticles = null;

  if (splash && window.nitroBootSplash) {
    window.nitroBootSplash.buildBrickLogo(document.getElementById('bootNLogo'));
    if (!reduce) {
      stopParticles = window.nitroBootSplash.startParticles(document.getElementById('bootParticles'));
    }
  }

  const finish = () => {
    splash?.classList.add('is-done');
    document.body.classList.remove('is-booting');
    requestAnimationFrame(() => {
      document.body.classList.add('shell-ready');
      requestAnimationFrame(() => window.nitroSkin?.resize?.());
    });
    setTimeout(() => {
      try { stopParticles?.(); } catch (_) {}
      splash?.remove();
    }, fadeMs + 40);
  };

  if (!splash) {
    document.body.classList.add('shell-ready');
    document.body.classList.remove('is-booting');
    return;
  }

  setTimeout(finish, splashMs);
}

function setView(name) {
  const next = document.getElementById('view-' + name);
  if (!next) {
    showToast('That page is missing — restart Nitro Client');
    return;
  }
  document.querySelectorAll('.view').forEach((v) => v.classList.remove('active'));
  next.classList.add('active');
  document.querySelectorAll('[data-view]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.view === name);
  });
  document.querySelectorAll('.nav-tab').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.view === name);
  });
  document.querySelectorAll('.fc-nav[data-view]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.view === name);
  });
  syncLauncherVideo();
  const onHome = name === 'launchpad';
  window.nitroSkin?.pause?.(!onHome);
  window.nitroSkinPage?.pause?.(name !== 'skins');
  if (onHome) {
    requestAnimationFrame(() => window.nitroSkin?.resize?.());
  }
  if (name === 'mods') {
    refreshModsView();
  }
  if (name === 'friends') {
    refreshFriendsUi();
  }
  if (name === 'skins') {
    try { refreshSkinsPage(); } catch (_) { /* keep the page visible */ }
    requestAnimationFrame(() => window.nitroSkinPage?.resize?.());
  }
  if (name === 'host') {
    window.nitroHostUi?.show?.();
  }
}

function updateUserUi() {
  const name = (els.username?.value || '').trim() || 'Player';
  const initial = name.charAt(0).toUpperCase();
  if (els.avatar) els.avatar.textContent = initial;
  if (els.avatarLarge) els.avatarLarge.textContent = initial;
  if (els.accountChipName) els.accountChipName.textContent = name;
  if (els.accountDropTitle) els.accountDropTitle.textContent = name;
  if (els.heroPlayerName) els.heroPlayerName.textContent = name;
  if (els.skinStageAvatar) els.skinStageAvatar.textContent = initial;
  if (els.accountsAvatar) els.accountsAvatar.textContent = initial;
  if (els.accountsName) els.accountsName.textContent = name;
  if (els.accountsModeBadge) {
    els.accountsModeBadge.textContent = loginMode === 'microsoft' ? 'MICROSOFT' : 'OFFLINE';
  }
  if (els.accountsModeHint) {
    els.accountsModeHint.textContent = loginMode === 'microsoft' ? 'Premium login' : 'Local username';
  }
  if (els.accountDropSub) {
    els.accountDropSub.textContent = loginMode === 'microsoft' ? 'Microsoft account' : 'Offline account';
  }
  if (els.onlinePill) {
    els.onlinePill.textContent = loginMode === 'microsoft' ? 'Online' : 'Ready';
  }
  updateSkinPreview(name);
}

function updateSkinPreview(name) {
  const raw = (name || 'Steve').trim() || 'Steve';
  const safe = encodeURIComponent(raw);
  const headUrl = `https://mc-heads.net/avatar/${safe}/64`;

  if (els.avatarImg) {
    els.avatarImg.onload = () => {
      els.avatarImg.hidden = false;
      if (els.avatar) els.avatar.style.display = 'none';
      const dashImg = document.getElementById('dashAvatarImg');
      const dashAv = document.getElementById('dashAvatar');
      const acctImg = document.getElementById('accountsAvatarImg');
      const acctAv = document.getElementById('accountsAvatar');
      if (dashImg) {
        dashImg.src = els.avatarImg.src;
        dashImg.hidden = false;
      }
      if (dashAv) dashAv.style.display = 'none';
      if (acctImg) {
        acctImg.src = els.avatarImg.src;
        acctImg.hidden = false;
      }
      if (acctAv) acctAv.style.display = 'none';
    };
    els.avatarImg.onerror = () => {
      els.avatarImg.hidden = true;
      if (els.avatar) els.avatar.style.display = '';
      const acctImg = document.getElementById('accountsAvatarImg');
      const acctAv = document.getElementById('accountsAvatar');
      if (acctImg) acctImg.hidden = true;
      if (acctAv) acctAv.style.display = '';
    };
    els.avatarImg.src = headUrl;
  }

  if (appliedSkin?.dataUrl && window.nitroSkin?.setSkin) {
    window.nitroSkin.setSkin(appliedSkin.dataUrl, appliedSkin.model);
  } else if (window.nitroSkin?.setPlayer) {
    window.nitroSkin.setPlayer(raw);
  }
}

function updateMetaPills(v) {
  const memoryMb = parseInt(els.memoryInput?.value, 10) || 4096;
  const gb = (memoryMb / 1024).toFixed(1).replace(/\.0$/, '') + 'GB';
  const loader = v?.profile === 'nitro-vanilla' ? 'Vanilla' : (v?.profile === 'nitro-full' ? 'Legacy' : 'Fabric');
  const java = v?.profile === 'nitro-full' ? 'Java 8' : 'Java 21+';
  if (els.pillVersion) els.pillVersion.textContent = v?.mc || '—';
  if (els.pillLoader) els.pillLoader.textContent = loader;
  const heroVer = document.getElementById('heroVersionTitle');
  if (heroVer) heroVer.textContent = 'Minecraft ' + (v?.mc || '1.21.11');
  if (!launching && els.launchBtnLabel) els.launchBtnLabel.textContent = launchButtonLabel();
  if (els.pillJava) els.pillJava.textContent = java;
  if (els.pillRam) els.pillRam.textContent = gb;
  if (els.selectedHint) {
    els.selectedHint.textContent = v?.mc || '';
  }
  const footerProfile = document.getElementById('footerProfile');
  const footerMc = document.getElementById('footerMc');
  if (footerProfile) footerProfile.textContent = v?.mc || '';
  if (footerMc) footerMc.textContent = v?.mc || '—';
}

function setAccountDropdownOpen(open) {
  if (!els.accountDropdown || !els.accountChipBtn) return;
  els.accountDropdown.classList.toggle('hidden', !open);
  els.accountChipBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
}

function applyLauncherTheme(id) {
  const theme = LAUNCHER_THEMES.find((t) => t.id === id) || LAUNCHER_THEMES[0];
  launcherThemeId = theme.id;
  localStorage.setItem('nitro.launcherTheme', theme.id);
  if (els.themeActiveLabel) els.themeActiveLabel.textContent = theme.name;
  document.body.classList.remove('bg-theme-jungle', 'bg-theme-classic', 'bg-theme-night', 'bg-theme-summer', 'bg-theme-void', 'bg-theme-minecraft');
  applyMotionPref();
  syncLauncherVideo();
  renderThemeGrid(els.themeSearch?.value || '');
}

function isBgVideoEnabled() {
  // Forest glass shell uses a static blurred backdrop by default.
  if (document.body.classList.contains('fc-ui')) {
    return localStorage.getItem('nitro.bgVideo') === '1';
  }
  return localStorage.getItem('nitro.bgVideo') !== '0';
}

function isReduceMotionEnabled() {
  return localStorage.getItem('nitro.reduceMotion') === '1';
}

function applyMotionPref() {
  document.body.classList.toggle('reduce-motion', isReduceMotionEnabled());
}

function syncLauncherVideo() {
  const video = document.getElementById('launcherVideo');
  const onHome = document.getElementById('view-launchpad')?.classList.contains('active');
  const useVideo = onHome && !document.hidden && isBgVideoEnabled();
  document.body.classList.toggle('has-bg-video', useVideo);
  if (!video) return;
  if (useVideo) {
    video.muted = true;
    const play = video.play();
    if (play && typeof play.catch === 'function') play.catch(() => {});
  } else {
    video.pause();
  }
}

function setSettingsCategory(cat) {
  const target = cat || 'account';
  document.querySelectorAll('.settings-section[data-cat]').forEach((section) => {
    section.hidden = section.dataset.cat !== target;
  });
  els.settingsRail?.querySelectorAll('[data-cat]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.cat === target);
  });
}

function renderThemeGrid(filter = '') {
  if (!els.themeGrid) return;
  const q = filter.toLowerCase().trim();
  const list = LAUNCHER_THEMES.filter((t) => !q || t.name.toLowerCase().includes(q));
  if (els.themeCount) els.themeCount.textContent = `Themes (${list.length})`;
  els.themeGrid.innerHTML = '';
  list.forEach((theme) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'theme-card' + (theme.id === launcherThemeId ? ' active' : '');
    const thumbStyle = theme.thumb
      ? `background-image:linear-gradient(180deg,rgba(0,0,0,.15),rgba(0,0,0,.55)),url('${theme.thumb}')`
      : 'background:radial-gradient(ellipse at 50% 20%,#1a2433 0%,#07090d 55%,#030406 100%)';
    btn.innerHTML = `
      <div class="theme-thumb" style="${thumbStyle}"></div>
      <strong>${theme.name}</strong>
      <span class="theme-dot" aria-hidden="true"></span>
    `;
    btn.addEventListener('click', () => applyLauncherTheme(theme.id));
    els.themeGrid.appendChild(btn);
  });
}

function setThemeModalOpen(open) {
  if (!els.themeModal) return;
  els.themeModal.classList.toggle('hidden', !open);
  if (open) renderThemeGrid(els.themeSearch?.value || '');
}

function renderFeatures(id) {
  if (!els.heroFeatures) return;
  const v = versions.find((x) => x.id === id);
  const feats = v?.features
    || FEATURES[id]
    || (v?.profile === 'nitro-full' ? FEATURES['nitro-1.8.9'] : VANILLA_FEATURES);
  els.heroFeatures.innerHTML = feats.map((f) => `<span class="feat">${f}</span>`).join('');
}

function updateDockMeta() {
  // Home SMP line is driven by renderSmpStatus; keep metaVersion for compat only.
  if (!els.metaVersion) return;
  if (loginMode === 'microsoft') {
    els.metaVersion.textContent = 'Microsoft login';
  } else if (smpStatus?.online) {
    els.metaVersion.textContent = `${smpStatus.playersOnline}/${smpStatus.playersMax} online`;
  } else {
    els.metaVersion.textContent = 'Offline';
  }
}

function renderSmpStatus(status) {
  smpStatus = status;
  updateDockMeta();

  // Always point at the visible home SMP controls (not rebuilt partner rows)
  els.smpDot = document.getElementById('smpDot');
  els.smpMeta = document.getElementById('smpMeta');
  els.smpMotd = document.getElementById('smpMotd');
  els.smpStatusText = document.getElementById('smpStatusText');
  els.smpPing = document.getElementById('smpPing');
  els.smpCard = document.getElementById('smpCard');

  if (els.smpDot) {
    els.smpDot.hidden = false;
    els.smpDot.classList.toggle('online', !!status.online);
    els.smpDot.classList.toggle('offline', !status.online);
  }

  if (els.onlinePill) {
    els.onlinePill.textContent = status.online ? 'Online' : 'Offline';
    els.onlinePill.style.color = status.online ? '' : '#ff6b6b';
  }
  const smpLive = document.getElementById('smpLive');
  if (smpLive) smpLive.textContent = status.online ? 'Online' : 'Offline';

  if (status.online) {
    if (els.smpStatusText) els.smpStatusText.textContent = `${status.playersOnline}/${status.playersMax} players`;
    if (els.smpMotd) els.smpMotd.textContent = 'nitrosmp.lol';
    if (els.smpMeta) els.smpMeta.textContent = '';
    if (els.smpPing) els.smpPing.textContent = status.ping ? `${status.ping} ms` : '';
  } else {
    if (els.smpStatusText) els.smpStatusText.textContent = 'Server offline';
    if (els.smpMotd) els.smpMotd.textContent = 'nitrosmp.lol';
    if (els.smpMeta) els.smpMeta.textContent = '';
    if (els.smpPing) els.smpPing.textContent = '';
  }
}

async function refreshServerStatus() {
  try {
    const status = await window.nitro.getServerStatus();
    renderSmpStatus(status);
  } catch (_) {
    renderSmpStatus({ online: false, error: 'Status check failed' });
  }
}

function renderNews(meta) {
  const version = String(meta?.launcherVersion || '1.6.3').replace(/^v/, '');
  if (els.launcherVersionLabel) els.launcherVersionLabel.textContent = 'v' + version;
  if (els.footerVersion) els.footerVersion.textContent = version;

  const news = Array.isArray(meta?.news) ? meta.news : [];
  const fallback = [
    {
      title: 'Nitro launcher refresh',
      body: 'Unique Nitro home layout, working controls, and cleaner navigation.'
    },
    {
      title: 'Nitro SMP',
      body: 'Live player count, ping, and one-click join from the Servers tab.'
    }
  ];
  const entries = news.length ? news : fallback;
  if (els.newsList) {
    els.newsList.innerHTML = entries.slice(0, 5).map((item) => `
      <article class="news-item">
        <strong>${escHtml(item.title || item.headline || 'Update')}</strong>
        <p>${escHtml(item.body || item.summary || item.text || '')}</p>
      </article>
    `).join('');
  }

  const homeNews = document.getElementById('homeNews');
  if (homeNews) {
    homeNews.innerHTML = entries.slice(0, 3).map((item) => `
      <article class="home-news-banner" ${item.url ? `data-url="${escHtml(item.url)}"` : ''}>
        <strong>${escHtml(item.title || item.headline || 'Update')}</strong>
        <p>${escHtml(item.body || item.summary || item.text || '')}</p>
        ${item.date ? `<span class="nx-news-date">${escHtml(item.date)}</span>` : ''}
      </article>
    `).join('');
    homeNews.querySelectorAll('[data-url]').forEach((card) => {
      card.style.cursor = 'pointer';
      card.addEventListener('click', () => window.nitro.openExternal(card.dataset.url));
    });
  }

  if (els.newsPanel) {
    els.newsPanel.classList.remove('hidden');
    els.newsPanel.hidden = false;
    els.newsPanel.setAttribute('aria-hidden', 'false');
  }
}

function renderChangelog(meta) {
  if (!els.changelogList) return;
  const entries = meta?.changelog || [];
  els.changelogList.innerHTML = entries.map((entry) => `
    <div class="changelog-entry">
      <div class="changelog-head">
        <strong>v${entry.version}</strong>
        <span>${entry.date || ''}</span>
      </div>
      <ul>${(entry.items || []).map((item) => `<li>${item}</li>`).join('')}</ul>
    </div>
  `).join('') || '<p class="settings-note">No changelog entries.</p>';
}

function renderUpdateBanner(meta) {
  if (!els.updateBanner) return;
  if (meta?.updateAvailable) {
    els.updateBanner.classList.remove('hidden');
    els.updateBannerText.textContent = `Version ${meta.latestVersion} is available (you have v${meta.launcherVersion}).`;
    els.updateDownloadBtn.disabled = false;
    els.updateDownloadBtn.dataset.url = meta.downloadUrl || 'https://nitrosmp.lol/download';
  } else {
    els.updateBanner.classList.add('hidden');
  }
}

async function refreshSecurityPanel() {
  if (!els.securityPanelList) return;
  try {
    const info = await window.nitro.getAuthSecurity();
    const items = [
      'Sign-in opens the official Microsoft page — Nitro never sees your password',
      'Login tokens stay on your computer only (not sent to Nitro SMP or our servers)',
      'Tokens only launch Minecraft — they cannot change your email or Microsoft password',
      info.encryptedStorage
        ? 'Saved login is encrypted with Windows secure storage on this PC'
        : 'Saved login is encrypted locally on this PC',
      'Use Sign out anytime to remove saved login from this PC'
    ];
    els.securityPanelList.innerHTML = items.map((item) => `<li>${item}</li>`).join('');
  } catch (_) { /* keep static fallback */ }
}

function renderServerHub(hub) {
  if (!els.serverHubGrid) return;

  const others = (hub?.servers || []).filter((s) => !s.featured);

  if (!others.length) {
    els.serverHub?.classList.add('hidden');
    els.serverHubGrid.innerHTML = '';
    return;
  }

  els.serverHub?.classList.remove('hidden');

  if (els.lastServerLabel) {
    els.lastServerLabel.textContent = hub?.lastServer?.name
      ? `Also play on · last: ${hub.lastServer.name}`
      : 'Also play on';
  }

  els.serverHubGrid.innerHTML = others.map((server, i) => `
    <button type="button" class="partner-row server-play" data-host="${server.host}" title="${server.description || server.host}">
      <span class="partner-rank">#${i + 2}</span>
      <span class="partner-icon">${(server.name || '?').charAt(0)}</span>
      <span class="partner-info">
        <strong>${server.name} <i class="status-dot online"></i></strong>
        <span class="partner-host">${server.host}</span>
      </span>
      <span class="partner-count">Join</span>
    </button>
  `).join('');

  els.serverHubGrid.querySelectorAll('.server-play').forEach((btn) => {
    btn.addEventListener('click', () => launch({ joinServer: btn.dataset.host }));
  });
}

function mapPartnerEntry(s) {
  return {
    id: s.id || '',
    name: s.name || s.host,
    host: s.host,
    tag: s.tag || (s.featured ? 'OFFICIAL' : 'PARTNER'),
    icon: s.icon || '',
    description: s.description || s.host,
    featured: !!s.featured || String(s.tag || '').toUpperCase() === 'OFFICIAL'
  };
}

function applyPartnersFromList(list) {
  if (!Array.isArray(list) || !list.length) return false;
  PARTNER_SERVERS = list.map(mapPartnerEntry).filter((s) => s.host);
  renderPartnersHome();
  if (els.partnersPageList) renderPartnerRows(els.partnersPageList);
  return true;
}

function mergeFavoritePartners(base, hubServers) {
  const out = [...base];
  const seen = new Set(out.map((s) => String(s.host || '').toLowerCase()));
  for (const s of hubServers || []) {
    const host = String(s.host || '').toLowerCase();
    if (!host || seen.has(host)) continue;
    if (String(s.tag || '').toUpperCase() !== 'FAVORITE') continue;
    seen.add(host);
    out.push(mapPartnerEntry(s));
  }
  return out;
}

async function loadServerHub() {
  try {
    const hub = await window.nitro.getServerHub();
    const fromHub = Array.isArray(hub?.servers) ? hub.servers : [];
    if (remotePartnersActive && PARTNER_SERVERS.length) {
      PARTNER_SERVERS = mergeFavoritePartners(PARTNER_SERVERS, fromHub);
      renderPartnersHome();
      if (els.partnersPageList) renderPartnerRows(els.partnersPageList);
    } else if (fromHub.length) {
      applyPartnersFromList(fromHub);
    } else {
      renderPartnersHome();
    }
    renderServerHub(hub);
  } catch (_) {
    renderPartnersHome();
  }
}

async function renderModToggles() {
  if (!els.modToggleGrid) return;
  try {
    const toggles = await window.nitro.getModToggles();
    els.modToggleGrid.innerHTML = toggles.map((toggle) => `
      <label class="mod-toggle-row">
        <input type="checkbox" data-mod-id="${toggle.id}" ${toggle.enabled ? 'checked' : ''} />
        <span>
          <strong>${toggle.label}</strong>
          <small>${toggle.desc}</small>
        </span>
      </label>
    `).join('');

    els.modToggleGrid.querySelectorAll('input[data-mod-id]').forEach((input) => {
      input.addEventListener('change', async () => {
        try {
          await window.nitro.setModToggle({ id: input.dataset.modId, enabled: input.checked });
          showToast(input.checked ? 'Module enabled' : 'Module disabled');
        } catch (e) {
          showToast(e.message || 'Could not update module');
          input.checked = !input.checked;
        }
      });
    });
  } catch (_) {
    els.modToggleGrid.innerHTML = '<p class="settings-note">Launch Nitro once to configure modules.</p>';
  }
}

async function refreshJavaStatus(showWizard = true) {
  if (!els.javaSetupStatus) return null;
  try {
    const env = await window.nitro.getEnvironment();
    if (env.java8Installed) {
      els.javaSetupStatus.textContent = 'Java 8 is ready.';
      if (els.setupJavaBtn) els.setupJavaBtn.textContent = 'Reinstall Java 8';
      if (els.javaWizard) els.javaWizard.classList.add('hidden');
      return env;
    }
    els.javaSetupStatus.textContent = env.bundledClient
      ? 'Java 8 is missing — click Download to set up automatically.'
      : 'Java 8 not detected. Download it or install JDK 8 manually.';
    if (showWizard && els.javaWizard && isNitroFull()) {
      els.javaWizard.classList.remove('hidden');
    }
    return env;
  } catch (_) {
    els.javaSetupStatus.textContent = 'Could not check Java status.';
    return null;
  }
}

async function runJavaSetup(fromWizard = false) {
  if (launching) return;
  setLaunching(true);
  setLaunchUi({ line: 'Downloading Java 8…', percent: 5, phase: 'prepare' });
  try {
    await window.nitro.setupJava8();
    if (fromWizard && els.javaWizard) els.javaWizard.classList.add('hidden');
    showToast('Java 8 installed');
    await refreshJavaStatus(false);
    setLaunchUi({ line: 'Java 8 ready', percent: 100, phase: 'done' });
  } catch (e) {
    showToast(e.message || 'Java setup failed');
    setLaunchUi({ line: e.message || 'Java setup failed', percent: 0, phase: 'error' });
  } finally {
    setLaunching(false);
    setTimeout(() => { if (!launching) resetLaunchUi(); }, 2000);
  }
}

async function loadLauncherMeta() {
  try {
    launcherMeta = await window.nitro.getLauncherMeta();
    if (Array.isArray(launcherMeta?.partners) && launcherMeta.partners.length) {
      remotePartnersActive = true;
      applyPartnersFromList(launcherMeta.partners);
    }
    renderNews(launcherMeta);
    renderChangelog(launcherMeta);
    renderUpdateBanner(launcherMeta);
    if (els.launcherVersionLabel) {
      els.launcherVersionLabel.textContent = 'v' + (launcherMeta?.launcherVersion || '1.3.0');
    }
  } catch (_) {
    renderNews(null);
    renderChangelog(null);
  }
}

function startLiveMetaPolling() {
  if (liveMetaPollTimer) clearInterval(liveMetaPollTimer);
  liveMetaPollTimer = setInterval(() => {
    loadLauncherMeta();
  }, 180000);
}

function selectVersion(id) {
  selectedId = id;
  const v = versions.find((x) => x.id === id);
  if (!v) return;

  if (els.selectedLabel) els.selectedLabel.textContent = v.mc || v.label;
  if (els.launchBtnSub) els.launchBtnSub.textContent = v.mc || '';
  updateDockMeta();
  if (els.profileBadge) els.profileBadge.textContent = v.tag;
  if (els.dockVerPill) els.dockVerPill.textContent = v.mc;
  updateMetaPills(v);

  applyHomeTheme(v);
  renderFeatures(id);
  updatePlayButtons();
  updateNitroOnlySettings();

  document.querySelectorAll('.version-card, .ver-card').forEach((card) => {
    const on = card.dataset.id === id;
    card.classList.toggle('selected', on);
    card.classList.toggle('active', on);
  });

  window.nitro.saveSettings({ selectedVersion: id });
  renderLaunchProfiles();
  syncVersionControls();
  renderFriends();
}

function renderVersions(filter = '') {
  if (!els.versionGrid) return;
  const q = filter.toLowerCase();
  els.versionGrid.innerHTML = '';
  versions
    .filter((v) => v.label.toLowerCase().includes(q) || v.mc.includes(q) || (v.tag || '').toLowerCase().includes(q))
    .forEach((v) => {
      const card = document.createElement('button');
      card.type = 'button';
      const active = v.id === selectedId;
      card.className = 'ver-card' + (active ? ' active' : '');
      card.dataset.id = v.id;
      const loader = v.profile === 'nitro-vanilla' ? 'Vanilla' : (v.profile === 'nitro-full' ? 'Legacy' : 'Fabric');
      const thumb = v.thumb || 'assets/bg_jungle_2.png';
      card.innerHTML = `
        <div class="ver-card-bg" style="background-image:url('${thumb}')"></div>
        <div class="ver-card-overlay"></div>
        <div class="ver-card-content">
          <span class="ver-tag">${v.tag || 'VERSION'}</span>
          <h3>${v.mc}${active ? ' <span class="active-badge">ACTIVE</span>' : ''}</h3>
          <p class="ver-desc">${loader} version</p>
          <div class="ver-pills">
            <span>${v.mc}</span>
            <span>${loader}</span>
          </div>
        </div>
        <span class="ver-check">✓</span>
      `;
      card.addEventListener('click', () => {
        selectVersion(v.id);
        showToast('Selected ' + v.mc);
      });
      els.versionGrid.appendChild(card);
    });
  renderLaunchProfiles();
}

function ensureLaunchProfiles(settingsProfiles) {
  const list = Array.isArray(settingsProfiles) ? settingsProfiles.slice() : [];
  if (!list.length && versions.length) {
    versions.forEach((v) => {
      list.push({
        id: 'profile-' + v.id,
        name: (v.tag || 'Nitro') + ' ' + v.mc,
        versionId: v.id,
        ramGb: 4
      });
    });
  }
  return list;
}

let launchProfiles = [];
let activeLaunchProfileId = '';

function renderLaunchProfiles() {
  const grid = document.getElementById('profileGrid');
  if (!grid) return;
  if (!launchProfiles.length) {
    launchProfiles = ensureLaunchProfiles([]);
  }
  grid.innerHTML = '';
  launchProfiles.forEach((profile) => {
    const v = versions.find((x) => x.id === profile.versionId);
    const card = document.createElement('article');
    const active = profile.id === activeLaunchProfileId;
    card.className = 'profile-card' + (active ? ' active' : '');
    card.dataset.profileId = profile.id;
    card.innerHTML = `
      <span class="tag">${active ? 'ACTIVE' : 'PROFILE'}</span>
      <strong>${profile.name}</strong>
      <small>${v ? `${v.mc} · ${v.profile === 'nitro-full' ? 'Legacy' : 'Fabric'}` : 'Missing version'} · ${profile.ramGb || 4}GB</small>
      <div class="profile-actions">
        <button class="btn btn-sm btn-accent use-profile" type="button">Use</button>
        <button class="btn btn-sm btn-glass rename-profile" type="button">Rename</button>
        <button class="btn btn-sm btn-glass delete-profile" type="button">Delete</button>
      </div>
    `;
    card.querySelector('.use-profile')?.addEventListener('click', (e) => {
      e.stopPropagation();
      useLaunchProfile(profile.id);
    });
    card.querySelector('.rename-profile')?.addEventListener('click', (e) => {
      e.stopPropagation();
      const next = window.prompt('Profile name', profile.name);
      if (!next || !next.trim()) return;
      profile.name = next.trim();
      persistLaunchProfiles();
      renderLaunchProfiles();
    });
    card.querySelector('.delete-profile')?.addEventListener('click', (e) => {
      e.stopPropagation();
      if (launchProfiles.length <= 1) {
        showToast('Keep at least one profile');
        return;
      }
      launchProfiles = launchProfiles.filter((p) => p.id !== profile.id);
      if (activeLaunchProfileId === profile.id) {
        activeLaunchProfileId = launchProfiles[0]?.id || '';
      }
      persistLaunchProfiles();
      renderLaunchProfiles();
    });
    card.addEventListener('click', () => useLaunchProfile(profile.id));
    grid.appendChild(card);
  });
}

function useLaunchProfile(profileId) {
  const profile = launchProfiles.find((p) => p.id === profileId);
  if (!profile) return;
  activeLaunchProfileId = profile.id;
  if (profile.versionId) selectVersion(profile.versionId);
  if (profile.ramGb) {
    const mb = Math.round(Number(profile.ramGb) * 1024);
    if (els.memoryInput) els.memoryInput.value = String(mb);
    if (els.memorySlider) els.memorySlider.value = String(Math.min(8192, mb));
    const ramSelect = document.getElementById('ramSelect');
    if (ramSelect) ramSelect.value = String(profile.ramGb);
    window.nitro.saveSettings({ memory: mb });
    if (els.pillRam) els.pillRam.textContent = profile.ramGb + 'GB';
  }
  persistLaunchProfiles();
  renderLaunchProfiles();
  showToast('Using profile ' + profile.name);
}

function persistLaunchProfiles() {
  window.nitro.saveSettings({
    launchProfiles,
    activeLaunchProfileId
  });
}

function createLaunchProfile() {
  const v = versions.find((x) => x.id === selectedId) || versions[0];
  if (!v) return;
  const name = window.prompt('New profile name', 'My ' + v.mc + ' profile');
  if (!name || !name.trim()) return;
  const profile = {
    id: 'profile-' + Date.now().toString(36),
    name: name.trim(),
    versionId: v.id,
    ramGb: Number(els.memoryInput?.value ? Number(els.memoryInput.value) / 1024 : (document.getElementById('ramSelect')?.value || 4))
  };
  launchProfiles.push(profile);
  activeLaunchProfileId = profile.id;
  persistLaunchProfiles();
  renderLaunchProfiles();
  showToast('Profile created');
}

let friendsState = { me: 'Player', friends: [], incoming: [], outgoing: [], chats: {}, onlineCount: 0 };
let activeFriend = '';
let friendsTab = 'all';

function escHtml(value) {
  return String(value || '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

function headUrl(name) {
  return `https://mc-heads.net/avatar/${encodeURIComponent(name || 'Steve')}/64`;
}

function extractServerHost(text) {
  const url = String(text || '').match(/https?:\/\/([a-z0-9.-]+(?::\d{1,5})?)/i);
  if (url) return url[1];
  const host = String(text || '').match(/\b(?:[a-z0-9-]+\.)+[a-z]{2,}(?::\d{1,5})?\b/i);
  return host ? host[0] : '';
}

function linkify(text) {
  return escHtml(text).replace(
    /(https?:\/\/[^\s]+)|(\b(?:[a-z0-9-]+\.)+[a-z]{2,}(?::\d{1,5})?\b)/gi,
    (m) => `<a class="friends-link" href="#" data-open="${escHtml(m)}">${escHtml(m)}</a>`
  );
}

function applyFriendsState(state) {
  if (!state) return;
  friendsState = state;
  window.__nitroFriendsState = state;
  renderFriends();
  renderFriendsPage();
  window.nitroHostUi?.syncFriends?.();
}

function friendDotClass(f) {
  if (f.self || f.online) return (f.status === 'ingame' || /in game/i.test(f.statusText || '')) ? 'game' : 'on';
  return '';
}

function lastSeenLabel(f) {
  if (f.online) return f.statusText || 'Online';
  if (!f.lastSeen) return 'Offline';
  const mins = Math.max(1, Math.round((Date.now() - f.lastSeen) / 60000));
  if (mins < 2) return 'Last seen just now';
  if (mins < 60) return `Last seen ${mins}m ago`;
  const hrs = Math.round(mins / 60);
  if (hrs < 24) return `Last seen ${hrs}h ago`;
  return 'Offline';
}

function lastChatPreview(name) {
  const messages = (friendsState.chats && friendsState.chats[keyName(name)]) || [];
  const last = messages[messages.length - 1];
  return last ? last.text : '';
}

function renderFriends() {
  const list = document.getElementById('homeFriendsList');
  if (!list) return;
  const friends = (friendsState.friends || []).slice(0, 6);
  if (!friends.length) {
    list.innerHTML = `<p class="friends-empty">No friends yet. Open Friends to search and add people.</p>`;
    return;
  }
  list.innerHTML = friends.map((f) => `
    <article class="nx-friend-row" data-open-chat="${escHtml(f.name)}">
      <div class="friends-avatar sm">
        <img src="${headUrl(f.name)}" alt="" loading="lazy" onerror="this.style.display='none'" />
        <i class="dot ${friendDotClass(f)}"></i>
      </div>
      <div>
        <strong>${escHtml(f.name)}</strong>
        <small>${escHtml(lastSeenLabel(f))}</small>
      </div>
    </article>
  `).join('');
}

function renderFriendsYou() {
  const card = document.getElementById('friendsYouCard');
  if (!card) return;
  const me = friendsState.me || (els.username?.value || 'Player');
  card.innerHTML = `
    <div class="friends-avatar sm">
      <img src="${headUrl(me)}" alt="" loading="lazy" onerror="this.style.display='none'" />
      <i class="dot on"></i>
    </div>
    <div>
      <strong>${escHtml(me)}</strong>
      <small>This is you</small>
    </div>`;
}

function renderFriendsPage() {
  const pill = document.getElementById('friendsOnlinePill');
  if (pill) pill.innerHTML = `<i class="dot on"></i>${friendsState.onlineCount || 0} online`;
  renderFriendsYou();

  document.querySelectorAll('#friendsTabs [data-friends-tab]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.friendsTab === friendsTab);
  });
  const count = document.getElementById('friendsRequestCount');
  const incoming = friendsState.incoming || [];
  if (count) {
    count.hidden = incoming.length === 0;
    count.textContent = String(incoming.length);
  }

  const req = document.getElementById('friendRequests');
  if (req) {
    req.hidden = incoming.length === 0 || friendsTab === 'requests';
    req.innerHTML = incoming.map((r) => `
      <article class="friends-request">
        <div class="friends-avatar">
          <img src="${headUrl(r.from)}" alt="" />
          <i class="dot warn"></i>
        </div>
        <div><strong>${escHtml(r.from)}</strong><small>wants to be friends</small></div>
        <div class="row-actions">
          <button class="btn btn-sm btn-accent" data-accept="${escHtml(r.from)}" type="button">Accept</button>
          <button class="btn btn-sm" data-decline="${escHtml(r.from)}" type="button">Decline</button>
        </div>
      </article>
    `).join('');
  }

  const list = document.getElementById('friendsList');
  if (list) {
    if (friendsTab === 'requests') {
      list.innerHTML = incoming.length ? incoming.map((r) => `
        <article class="friends-request">
          <div class="friends-avatar">
            <img src="${headUrl(r.from)}" alt="" />
            <i class="dot warn"></i>
          </div>
          <div><strong>${escHtml(r.from)}</strong><small>wants to be friends</small></div>
          <div class="row-actions">
            <button class="btn btn-sm btn-accent" data-accept="${escHtml(r.from)}" type="button">Accept</button>
            <button class="btn btn-sm" data-decline="${escHtml(r.from)}" type="button">Decline</button>
          </div>
        </article>
      `).join('') : `
        <div class="friends-empty-state">
          <strong>No pending requests</strong>
          <p>When someone adds you, it shows up here.</p>
        </div>`;
    } else {
      const friends = (friendsState.friends || []).filter((f) => friendsTab !== 'online' || f.online);
      list.innerHTML = friends.length ? friends.map((f) => {
        const preview = lastChatPreview(f.name);
        const status = lastSeenLabel(f);
        const join = f.online && f.server
          ? `<button class="friends-join-chip" data-join="${escHtml(f.server)}" type="button">Join</button>`
          : '';
        return `
        <article class="friends-row ${keyName(f.name) === keyName(activeFriend) ? 'active' : ''}" data-open-chat="${escHtml(f.name)}">
          <div class="friends-avatar">
            <img src="${headUrl(f.name)}" alt="" />
            <i class="dot ${friendDotClass(f)}"></i>
          </div>
          <div class="friends-row-copy">
            <strong>${escHtml(f.name)}</strong>
            <small>${escHtml(preview || status)}</small>
          </div>
          <div class="friends-row-actions">
            ${join}
            <button class="friends-icon-btn" data-remove="${escHtml(f.name)}" type="button" title="Remove">×</button>
          </div>
        </article>`;
      }).join('') : `
        <div class="friends-empty-state">
          <strong>${friendsTab === 'online' ? 'Nobody is online' : 'No friends yet'}</strong>
          <p>${friendsTab === 'online'
            ? 'Friends show as online when they have Nitro open.'
            : 'Search a Nitro username above to send your first request.'}</p>
        </div>`;
    }
  }

  renderFriendsChat();
}

function keyName(name) {
  return String(name || '').trim().toLowerCase();
}

function renderFriendsChat() {
  const head = document.getElementById('friendsChatHead');
  const log = document.getElementById('friendsChatLog');
  const input = document.getElementById('friendsChatInput');
  const send = document.getElementById('friendsChatSend');
  const friend = (friendsState.friends || []).find((f) => keyName(f.name) === keyName(activeFriend));
  if (!friend) {
    if (head) {
      head.innerHTML = `
        <div class="friends-chat-head-copy">
          <strong>Select a friend</strong>
          <small>Add someone, then open a chat to share servers.</small>
        </div>`;
    }
    if (log) {
      log.innerHTML = `
        <div class="friends-chat-empty">
          <strong>Your Nitro party starts here</strong>
          <p>Search a Nitro username, add them, then paste a server IP so they can hop in with one click.</p>
          <div class="friends-tips">
            <span>Search</span>
            <span>Add</span>
            <span>Share a server</span>
          </div>
        </div>`;
    }
    if (input) input.disabled = true;
    if (send) send.disabled = true;
    return;
  }
  const joinBtn = friend.online && friend.server
    ? `<button class="friends-join-chip" data-join="${escHtml(friend.server)}" type="button">Join ${escHtml(friend.server)}</button>`
    : '';
  if (head) {
    head.innerHTML = `
      <div class="friends-avatar lg">
        <img src="${headUrl(friend.name)}" alt="" />
        <i class="dot ${friendDotClass(friend)}"></i>
      </div>
      <div class="friends-chat-head-copy">
        <strong>${escHtml(friend.name)}</strong>
        <small><i class="dot ${friend.online ? 'on' : ''}"></i>${escHtml(lastSeenLabel(friend))}</small>
      </div>
      ${joinBtn}`;
  }
  if (input) input.disabled = false;
  if (send) send.disabled = false;
  const messages = (friendsState.chats && friendsState.chats[keyName(friend.name)]) || [];
  if (log) {
    if (!messages.length) {
      log.innerHTML = `
        <div class="friends-chat-empty">
          <strong>Say hey to ${escHtml(friend.name)}</strong>
          <p>Paste a server IP or link and they’ll get a Join button right in chat.</p>
        </div>`;
    } else {
      log.innerHTML = messages.map((m) => {
        const mine = keyName(m.from) === keyName(friendsState.me);
        const host = extractServerHost(m.text);
        const join = host ? `<div class="friends-join"><button class="friends-join-card" data-join="${escHtml(host)}" type="button">Join ${escHtml(host)}</button></div>` : '';
        return `<article class="friends-msg ${mine ? 'mine' : ''}">${linkify(m.text)}${join}<time>${new Date(m.at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</time></article>`;
      }).join('');
      log.scrollTop = log.scrollHeight;
    }
  }
}

async function refreshFriendsUi() {
  try {
    applyFriendsState(await window.nitro.friendsState());
  } catch (_) {
    renderFriends();
  }
}

function friendsError(err) {
  return String(err?.message || err || 'Search failed')
    .replace(/^Error invoking remote method '[^']+':\s*(?:Error:\s*)?/i, '');
}

async function searchFriend() {
  const input = document.getElementById('friendSearch');
  const box = document.getElementById('friendSearchResult');
  const name = (input?.value || '').trim();
  if (!box) return;
  box.hidden = false;
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) {
    box.innerHTML = '<p class="friends-empty">Enter a username (3–16 letters).</p>';
    return;
  }
  box.innerHTML = '<p class="friends-empty">Searching…</p>';
  try {
    const hit = await window.nitro.friendsSearch(name);
    if (!hit.found && !hit.alreadyFriend) {
      box.innerHTML = `<p class="friends-empty">No user named ${escHtml(name)}.</p>`;
      return;
    }
    const status = hit.online
      ? (hit.server ? `Online · ${hit.server}` : 'Online')
      : (hit.lastSeen ? 'Offline' : 'Offline account');
    const action = hit.alreadyFriend
      ? '<span class="dim">Already friends</span>'
      : hit.pending
        ? '<span class="dim">Request pending</span>'
        : `<button class="btn btn-sm btn-accent" data-add="${escHtml(hit.name)}" type="button">Add friend</button>`;
    box.innerHTML = `
      <article class="friends-search-hit">
        <div class="friends-avatar">
          <img src="${headUrl(hit.name)}" alt="" />
          <i class="dot ${hit.online ? (hit.status === 'ingame' ? 'game' : 'on') : ''}"></i>
        </div>
        <div>
          <strong>${escHtml(hit.name)}</strong>
          <small>${escHtml(status)}</small>
        </div>
        <div class="friends-hit-actions">${action}</div>
      </article>`;
  } catch (e) {
    box.innerHTML = `<p class="friends-empty">${escHtml(friendsError(e))}</p>`;
  }
}

let friendsBound = false;
function bindFriendsUi() {
  if (friendsBound) return;
  friendsBound = true;
  document.getElementById('friendSearchBtn')?.addEventListener('click', searchFriend);
  document.getElementById('friendSearch')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      searchFriend();
    }
  });
  document.getElementById('friendsTabs')?.addEventListener('click', (e) => {
    const tab = e.target?.closest('[data-friends-tab]')?.dataset?.friendsTab;
    if (!tab) return;
    friendsTab = tab;
    renderFriendsPage();
  });
  document.getElementById('friendSearchResult')?.addEventListener('click', async (e) => {
    const name = e.target?.dataset?.add;
    if (!name) return;
    try {
      applyFriendsState(await window.nitro.friendsAdd(name));
      showToast('Friend request sent to ' + name);
    } catch (err) {
      showToast(friendsError(err) || 'Could not add friend');
    }
  });
  async function handleFriendRequestClick(e) {
    const accept = e.target?.dataset?.accept;
    const decline = e.target?.dataset?.decline;
    try {
      if (accept) applyFriendsState(await window.nitro.friendsAccept(accept));
      if (decline) applyFriendsState(await window.nitro.friendsDecline(decline));
    } catch (err) {
      showToast(err.message || 'Request failed');
    }
  }
  document.getElementById('friendRequests')?.addEventListener('click', handleFriendRequestClick);
  document.getElementById('friendsList')?.addEventListener('click', async (e) => {
    if (e.target?.dataset?.accept || e.target?.dataset?.decline) {
      await handleFriendRequestClick(e);
      return;
    }
    const join = e.target?.dataset?.join;
    if (join) {
      e.stopPropagation();
      launch({ joinServer: join });
      return;
    }
    const remove = e.target?.dataset?.remove;
    const open = e.target?.closest('[data-open-chat]')?.dataset?.openChat;
    if (remove) {
      e.stopPropagation();
      applyFriendsState(await window.nitro.friendsRemove(remove));
      if (keyName(activeFriend) === keyName(remove)) activeFriend = '';
      return;
    }
    if (open) {
      activeFriend = open;
      applyFriendsState(await window.nitro.friendsOpenChat(open));
    }
  });
  document.getElementById('homeFriendsList')?.addEventListener('click', async (e) => {
    const open = e.target?.closest('[data-open-chat]')?.dataset?.openChat;
    if (!open) return;
    activeFriend = open;
    setView('friends');
    try {
      applyFriendsState(await window.nitro.friendsOpenChat(open));
    } catch (_) { /* ignore */ }
  });
  document.getElementById('friendsChatHead')?.addEventListener('click', (e) => {
    const join = e.target?.dataset?.join;
    if (join) launch({ joinServer: join });
  });
  document.getElementById('friendsChatLog')?.addEventListener('click', (e) => {
    const join = e.target?.dataset?.join;
    const open = e.target?.dataset?.open;
    if (join) {
      launch({ joinServer: join });
      return;
    }
    if (open) {
      if (/^https?:\/\//i.test(open)) window.nitro.openExternal(open);
      else launch({ joinServer: open });
    }
  });
  document.getElementById('friendsChatForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const input = document.getElementById('friendsChatInput');
    const text = (input?.value || '').trim();
    if (!text || !activeFriend) return;
    try {
      applyFriendsState(await window.nitro.friendsSend({ name: activeFriend, text }));
      if (input) input.value = '';
    } catch (err) {
      showToast(err.message || 'Message failed');
    }
  });
  window.nitro.onFriendsUpdated?.((state) => applyFriendsState(state));
}

let skinsState = { activeId: '', items: [] };
let selectedSkinId = '';
let appliedSkin = null;
let skinsBound = false;

function selectedSkin() {
  return (skinsState.items || []).find((s) => s.id === selectedSkinId) || null;
}

function applySkinsState(state) {
  if (!state || state.cancelled) return;
  skinsState = state;
  if (!selectedSkinId || !state.items.some((s) => s.id === selectedSkinId)) {
    selectedSkinId = state.activeId || state.items[0]?.id || '';
  }
  appliedSkin = state.items.find((s) => s.id === state.activeId) || null;
  renderSkinsPage();
}

function drawPaperDoll(canvas, src) {
  if (!canvas || !src) return;
  const img = new Image();
  img.onload = () => {
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;
    ctx.clearRect(0, 0, w, h);
    ctx.imageSmoothingEnabled = false;
    const unit = Math.floor(Math.min(w / 16, h / 32));
    const x = Math.floor((w - 16 * unit) / 2);
    const y = Math.floor((h - 32 * unit) / 2);
    const hd = img.height >= 64;
    ctx.drawImage(img, 8, 8, 8, 8, x + 4 * unit, y, 8 * unit, 8 * unit);
    ctx.drawImage(img, 40, 8, 8, 8, x + 4 * unit, y, 8 * unit, 8 * unit);
    ctx.drawImage(img, 20, 20, 8, 12, x + 4 * unit, y + 8 * unit, 8 * unit, 12 * unit);
    ctx.drawImage(img, 44, 20, 4, 12, x, y + 8 * unit, 4 * unit, 12 * unit);
    ctx.drawImage(img, hd ? 36 : 44, hd ? 52 : 20, 4, 12, x + 12 * unit, y + 8 * unit, 4 * unit, 12 * unit);
    ctx.drawImage(img, 4, 20, 4, 12, x + 4 * unit, y + 20 * unit, 4 * unit, 12 * unit);
    ctx.drawImage(img, hd ? 20 : 4, hd ? 52 : 20, 4, 12, x + 8 * unit, y + 20 * unit, 4 * unit, 12 * unit);
  };
  img.src = src;
}

function renderSkinsPage() {
  const grid = document.getElementById('skinsGrid');
  if (grid) {
    const items = skinsState.items || [];
    grid.innerHTML = items.length ? items.map((s) => `
      <article class="skins-card ${s.id === selectedSkinId ? 'active' : ''}" data-skin-id="${escHtml(s.id)}">
        <button class="skins-card-del" data-skin-del="${escHtml(s.id)}" type="button" title="Delete">×</button>
        <div class="skins-card-preview"><canvas width="84" height="140" data-skin-doll="${escHtml(s.id)}"></canvas></div>
        <strong>${escHtml(s.name)}</strong>
        <small>Skin Type: ${s.model === 'slim' ? 'Slim' : 'Classic'}</small>
      </article>
    `).join('') : '<p class="skins-empty">Add a username, paste a skin URL, or choose a PNG file.</p>';
    grid.querySelectorAll('canvas[data-skin-doll]').forEach((c) => {
      const item = items.find((s) => s.id === c.dataset.skinDoll);
      if (item?.dataUrl) drawPaperDoll(c, item.dataUrl);
    });
  }

  const skin = selectedSkin();
  const nameInput = document.getElementById('skinsNameInput');
  const current = document.getElementById('skinsCurrentLabel');
  if (nameInput) nameInput.value = skin?.name || '';
  if (current) {
    current.textContent = skin
      ? (skin.id === skinsState.activeId ? 'Current Skin' : 'Preview')
      : 'No skin selected';
  }
  document.querySelectorAll('#skinsTypeToggle [data-skin-model]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.skinModel === (skin?.model || 'classic'));
  });
  if (skin?.dataUrl) {
    window.nitroSkinPage?.setSkin?.(skin.dataUrl, skin.model);
    requestAnimationFrame(() => window.nitroSkinPage?.resize?.());
  }
}

async function refreshSkinsPage() {
  try {
    applySkinsState(await window.nitro.skinsState());
  } catch (_) {
    renderSkinsPage();
  }
  requestAnimationFrame(() => window.nitroSkinPage?.resize?.());
}

function bindSkinsUi() {
  if (skinsBound) return;
  skinsBound = true;
  const query = document.getElementById('skinsQuery');
  const addFromQuery = async () => {
    const value = (query?.value || '').trim();
    if (!value) {
      showToast('Enter a username or skin URL');
      return;
    }
    try {
      applySkinsState(await window.nitro.skinsAdd(value));
      if (query) query.value = '';
      selectedSkinId = skinsState.items[0]?.id || selectedSkinId;
      renderSkinsPage();
      showToast('Skin added');
    } catch (err) {
      showToast(friendsError(err) || 'Could not add skin');
    }
  };
  query?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addFromQuery();
    }
  });
  document.getElementById('skinsChooseFile')?.addEventListener('click', async () => {
    try {
      const state = await window.nitro.skinsAddFile();
      if (state?.cancelled) return;
      applySkinsState(state);
      selectedSkinId = skinsState.items[0]?.id || selectedSkinId;
      renderSkinsPage();
      showToast('Skin added');
    } catch (err) {
      showToast(friendsError(err) || 'Could not add skin');
    }
  });
  document.getElementById('skinsGrid')?.addEventListener('click', async (e) => {
    const del = e.target?.dataset?.skinDel;
    if (del) {
      e.stopPropagation();
      try {
        applySkinsState(await window.nitro.skinsRemove(del));
        updateUserUi();
      } catch (err) {
        showToast(friendsError(err) || 'Could not remove skin');
      }
      return;
    }
    const id = e.target?.closest('[data-skin-id]')?.dataset?.skinId;
    if (!id) return;
    selectedSkinId = id;
    renderSkinsPage();
  });
  document.getElementById('skinsNameInput')?.addEventListener('change', async (e) => {
    if (!selectedSkinId) return;
    try {
      applySkinsState(await window.nitro.skinsRename({ id: selectedSkinId, name: e.target.value }));
    } catch (_) { /* ignore */ }
  });
  document.getElementById('skinsTypeToggle')?.addEventListener('click', async (e) => {
    const model = e.target?.dataset?.skinModel;
    if (!model || !selectedSkinId) return;
    try {
      applySkinsState(await window.nitro.skinsModel({ id: selectedSkinId, model }));
    } catch (_) { /* ignore */ }
  });
  document.getElementById('skinsEnvSelect')?.addEventListener('change', (e) => {
    const stage = document.getElementById('skinsPreviewStage');
    if (!stage) return;
    stage.classList.remove('env-panorama', 'env-void', 'env-studio');
    stage.classList.add('env-' + (e.target.value || 'panorama'));
  });
  document.getElementById('skinsApplyBtn')?.addEventListener('click', async () => {
    if (!selectedSkinId) {
      showToast('Select a skin first');
      return;
    }
    try {
      applySkinsState(await window.nitro.skinsApply(selectedSkinId));
      updateUserUi();
      showToast('Skin applied');
    } catch (err) {
      showToast(friendsError(err) || 'Could not apply skin');
    }
  });
}

function syncVersionControls() {
  const versionSelect = document.getElementById('versionSelect');
  const loaderSelect = document.getElementById('loaderSelect');
  const ramSelect = document.getElementById('ramSelect');
  const javaSelect = document.getElementById('javaSelect');
  if (versionSelect) {
    versionSelect.innerHTML = versions.map((v) =>
      `<option value="${v.id}" ${v.id === selectedId ? 'selected' : ''}>${v.mc} (${v.tag || v.label})</option>`
    ).join('');
  }
  const v = versions.find((x) => x.id === selectedId);
  if (loaderSelect && v) {
    loaderSelect.value = v.profile === 'nitro-vanilla' ? 'vanilla' : (v.profile === 'nitro-full' ? 'legacy' : 'fabric');
  }
  if (javaSelect) {
    javaSelect.value = v?.profile === 'nitro-full' ? '8' : '21';
  }
  if (ramSelect && els.memoryInput) {
    const gb = Math.round((Number(els.memoryInput.value) || 4096) / 1024);
    const opt = Array.from(ramSelect.options).find((o) => Number(o.value) === gb);
    if (opt) ramSelect.value = String(gb);
  }
  if (els.modsProfileHint && v) {
    const loader = v.profile === 'nitro-vanilla' ? 'Vanilla' : (v.profile === 'nitro-full' ? 'Legacy' : 'Fabric');
    els.modsProfileHint.textContent = `${loader} · ${v.mc}`;
  }
}

function bindVersionControls() {
  document.getElementById('versionSelect')?.addEventListener('change', (e) => {
    selectVersion(e.target.value);
    syncVersionControls();
  });
  document.getElementById('loaderSelect')?.addEventListener('change', (e) => {
    const want = e.target.value;
    const match = versions.find((v) => {
      if (want === 'vanilla') return v.profile === 'nitro-vanilla';
      if (want === 'legacy') return v.profile === 'nitro-full';
      return v.profile !== 'nitro-vanilla' && v.profile !== 'nitro-full';
    });
    if (match) selectVersion(match.id);
    syncVersionControls();
  });
  document.getElementById('ramSelect')?.addEventListener('change', (e) => {
    const mb = Number(e.target.value) * 1024;
    if (els.memoryInput) els.memoryInput.value = String(mb);
    if (els.memorySlider) els.memorySlider.value = String(Math.min(8192, mb));
    window.nitro.saveSettings({ memory: mb });
    if (els.pillRam) els.pillRam.textContent = e.target.value + 'GB';
  });
  document.getElementById('modLoaderFilter')?.addEventListener('change', () => {
    searchMods(els.modSearch?.value || '');
  });
  document.getElementById('modVersionFilter')?.addEventListener('change', () => {
    searchMods(els.modSearch?.value || '');
  });
  bindSpotifyControls();
}

let modSearchTimer = null;
let modInstallLock = false;

async function refreshModsView() {
  const v = versions.find((x) => x.id === selectedId);
  if (els.modsProfileHint && v) {
    const loader = v.profile === 'nitro-vanilla' ? 'Vanilla' : (v.profile === 'nitro-full' ? 'Legacy' : 'Fabric');
    els.modsProfileHint.textContent = `${loader} · ${v.mc}`;
  }
  await renderInstalledMods();
  await searchMods(els.modSearch?.value || '');
}

let installedModNames = new Set();

async function renderInstalledMods() {
  if (!els.installedModList) return;
  try {
    const list = await window.nitro.listInstalledMods({ versionId: selectedId });
    installedModNames = new Set(list.map((m) => String(m.name || '').toLowerCase()));
    if (!list.length) {
      els.installedModList.innerHTML = '<li class="dim">No mods installed yet.</li>';
      return;
    }
    els.installedModList.innerHTML = list.map((mod) => `
      <li class="nx-installed-mod">
        <div>
          <strong>${mod.name}</strong>
          <span class="dim">${(mod.size / 1024 / 1024).toFixed(1)} MB</span>
        </div>
        <button class="btn btn-sm" type="button" data-uninstall="${mod.name}">Uninstall</button>
      </li>
    `).join('');
    els.installedModList.querySelectorAll('[data-uninstall]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        try {
          await window.nitro.uninstallMod({ versionId: selectedId, fileName: btn.dataset.uninstall });
          showToast('Uninstalled ' + btn.dataset.uninstall);
          await refreshModsView();
        } catch (err) {
          showToast(err.message || 'Uninstall failed');
        }
      });
    });
  } catch (_) {
    installedModNames = new Set();
    els.installedModList.innerHTML = '<li class="dim">Could not read mods folder.</li>';
  }
}

function modLooksInstalled(mod) {
  const slug = String(mod.slug || '').toLowerCase();
  const title = String(mod.title || '').toLowerCase().replace(/\s+/g, '');
  for (const name of installedModNames) {
    if (slug && name.includes(slug)) return true;
    if (title && name.replace(/[^a-z0-9]/g, '').includes(title.replace(/[^a-z0-9]/g, ''))) return true;
  }
  return false;
}

async function searchMods(query) {
  if (!els.modGrid) return;
  const v = versions.find((x) => x.id === selectedId);
  if (v?.profile === 'nitro-full') {
    els.modGrid.innerHTML = '<p class="note">Switch to a Fabric version (1.21.11) to browse Modrinth mods.</p>';
    return;
  }
  const q = (query || '').trim();
  const loader = document.getElementById('modLoaderFilter')?.value || 'fabric';
  const mcVersion = document.getElementById('modVersionFilter')?.value || v?.mc || '1.21.11';
  els.modGrid.innerHTML = `<p class="note">${q ? 'Searching Modrinth…' : 'Loading popular mods…'}</p>`;
  try {
    const hits = await window.nitro.searchMods({
      query: q,
      mcVersion,
      loader,
      limit: 40,
      index: q ? 'relevance' : 'downloads'
    });
    if (!hits.length) {
      els.modGrid.innerHTML = '<p class="note">No mods found. Try another search.</p>';
      return;
    }
    els.modGrid.innerHTML = '';
    hits.forEach((mod) => {
      const card = document.createElement('article');
      const installed = modLooksInstalled(mod);
      card.className = 'mod-card glass' + (installed ? ' is-installed' : '');
      const icon = mod.iconUrl
        ? `<img class="mod-icon" src="${mod.iconUrl}" alt="" loading="lazy" />`
        : '<div class="mod-icon mod-icon-fallback">?</div>';
      card.innerHTML = `
        ${icon}
        <div class="mod-card-body">
          <strong>${mod.title}</strong>
          <span class="mod-author">by ${mod.author || 'Unknown'}</span>
          <p>${(mod.description || '').slice(0, 120)}</p>
          <span class="dim">${formatDownloads(mod.downloads)} downloads${installed ? ' · Installed' : ''}</span>
        </div>
        <button class="btn btn-sm ${installed ? '' : 'btn-accent'} mod-install-btn" type="button">${installed ? 'Installed' : 'Install'}</button>
      `;
      const btn = card.querySelector('.mod-install-btn');
      if (installed) {
        btn.disabled = true;
      } else {
        btn?.addEventListener('click', async (e) => {
          e.stopPropagation();
          await installModFromModrinth(mod);
        });
      }
      els.modGrid.appendChild(card);
    });
  } catch (err) {
    const msg = err?.message || 'Modrinth search failed';
    const hint = /ENOTFOUND|ECONN|network|fetch/i.test(msg)
      ? ' Check your internet connection — Modrinth could not be reached.'
      : '';
    els.modGrid.innerHTML = `<p class="note">${msg}${hint}</p>`;
  }
}

function formatDownloads(n) {
  if (!n) return '0';
  if (n >= 1e6) return (n / 1e6).toFixed(1) + 'M';
  if (n >= 1e3) return (n / 1e3).toFixed(1) + 'K';
  return String(n);
}

async function installModFromModrinth(mod) {
  if (modInstallLock) return;
  modInstallLock = true;
  const v = versions.find((x) => x.id === selectedId);
  const loader = document.getElementById('modLoaderFilter')?.value || 'fabric';
  const mcVersion = document.getElementById('modVersionFilter')?.value || v?.mc;
  try {
    showToast('Installing ' + mod.title + '…');
    const result = await window.nitro.installMod({
      projectId: mod.id,
      mcVersion,
      loader
    });
    showToast(result.alreadyInstalled ? mod.title + ' already installed' : mod.title + ' installed');
    await refreshModsView();
  } catch (err) {
    showToast(err.message || 'Install failed');
  } finally {
    modInstallLock = false;
  }
}

async function refreshSpotifyUi() {
  const statusEl = document.getElementById('spotifyStatusText');
  const now = document.getElementById('spotifyNowPlaying');
  const controls = document.getElementById('spotifyControls');
  const disconnectBtn = document.getElementById('spotifyDisconnectBtn');
  const connectBtn = document.getElementById('spotifyConnectBtn');
  try {
    const status = await window.nitro.spotifyStatus();
    const input = document.getElementById('spotifyClientIdInput');
    if (input && status.clientId && !input.value) input.value = status.clientId;
    if (status.connected) {
      if (statusEl) {
        statusEl.textContent = status.displayName
          ? `Spotify · Connected as ${status.displayName}`
          : 'Spotify · Connected';
      }
      if (disconnectBtn) disconnectBtn.hidden = false;
      if (connectBtn) connectBtn.textContent = 'Reconnect Spotify';
      if (controls) controls.hidden = false;
      if (now && status.track) {
        now.hidden = false;
        const art = document.getElementById('spotifyAlbumArt');
        const title = document.getElementById('spotifyTrackName');
        const artist = document.getElementById('spotifyArtistName');
        if (art) art.src = status.track.albumArt || '';
        if (title) title.textContent = status.track.name || '—';
        if (artist) artist.textContent = status.track.artist || '—';
        const pp = document.getElementById('spotifyPlayPauseBtn');
        if (pp) pp.textContent = status.track.isPlaying ? 'Pause' : 'Play';
      } else if (now) {
        now.hidden = true;
      }
    } else {
      if (statusEl) statusEl.textContent = 'Not connected. Authorize through Spotify’s official login page.';
      if (disconnectBtn) disconnectBtn.hidden = true;
      if (connectBtn) connectBtn.textContent = 'Connect Spotify';
      if (controls) controls.hidden = true;
      if (now) now.hidden = true;
    }
  } catch (_) {
    if (statusEl) statusEl.textContent = 'Spotify status unavailable.';
  }
}

function bindSpotifyControls() {
  document.getElementById('spotifySaveIdBtn')?.addEventListener('click', async () => {
    const id = document.getElementById('spotifyClientIdInput')?.value?.trim() || '';
    localStorage.setItem('nitro.spotifyClientId', id);
    try {
      await window.nitro.spotifySaveClientId(id);
      showToast(id ? 'Spotify Client ID saved' : 'Cleared Spotify Client ID');
    } catch (err) {
      showToast(err.message || 'Could not save Client ID');
    }
  });
  document.getElementById('spotifyOpenDashboardBtn')?.addEventListener('click', () => {
    window.nitro.openExternal('https://developer.spotify.com/dashboard');
  });
  document.getElementById('spotifyConnectBtn')?.addEventListener('click', async () => {
    const id = document.getElementById('spotifyClientIdInput')?.value?.trim()
      || localStorage.getItem('nitro.spotifyClientId')
      || '';
    if (!id) {
      showToast('Paste your Spotify Client ID first');
      return;
    }
    showToast('Opening Spotify login…');
    try {
      await window.nitro.spotifySaveClientId(id);
      await window.nitro.spotifyConnect({ clientId: id });
      showToast('Spotify connected');
      await refreshSpotifyUi();
    } catch (err) {
      showToast(err.message || 'Spotify connect failed');
    }
  });
  document.getElementById('spotifyDisconnectBtn')?.addEventListener('click', async () => {
    await window.nitro.spotifyDisconnect();
    showToast('Spotify disconnected');
    await refreshSpotifyUi();
  });
  document.getElementById('spotifyPrevBtn')?.addEventListener('click', async () => {
    try { await window.nitro.spotifyPlayback({ action: 'previous' }); await refreshSpotifyUi(); }
    catch (err) { showToast(err.message || 'Spotify previous failed'); }
  });
  document.getElementById('spotifyNextBtn')?.addEventListener('click', async () => {
    try { await window.nitro.spotifyPlayback({ action: 'next' }); await refreshSpotifyUi(); }
    catch (err) { showToast(err.message || 'Spotify next failed'); }
  });
  document.getElementById('spotifyPlayPauseBtn')?.addEventListener('click', async () => {
    try {
      const status = await window.nitro.spotifyStatus();
      const action = status?.track?.isPlaying ? 'pause' : 'play';
      await window.nitro.spotifyPlayback({ action });
      await refreshSpotifyUi();
    } catch (err) {
      showToast(err.message || 'Spotify playback failed');
    }
  });
  const savedId = localStorage.getItem('nitro.spotifyClientId') || '';
  const spotifyInput = document.getElementById('spotifyClientIdInput');
  if (spotifyInput && savedId) spotifyInput.value = savedId;
}

const partnerStatusCache = new Map();

function partnerCountLabel(host) {
  const st = partnerStatusCache.get(String(host || '').toLowerCase());
  if (!st) return { text: '…', cls: 'is-loading' };
  if (st.online) return { text: `${st.playersOnline}/${st.playersMax}`, cls: 'is-online' };
  return { text: 'Offline', cls: 'is-offline' };
}

function partnerIconUrl(server) {
  if (server.icon) return server.icon;
  if (server.host) return `https://api.mcsrvstat.us/icon/${encodeURIComponent(server.host)}`;
  return '';
}

function partnerCardHtml(server, index = 0, rich = false) {
  const src = partnerIconUrl(server);
  const letter = (server.name || '?').charAt(0);
  const icon = src
    ? `<img class="partner-icon" src="${src}" alt="" data-fallback="${letter}" loading="eager" decoding="async" />`
    : `<span class="partner-fallback">${letter}</span>`;
  const count = partnerCountLabel(server.host);
  const rank = index + 1;
  const rankClass = rank === 1 ? 'gold' : '';
  const featured = (server.featured || rank === 1) ? ' featured' : '';
  if (rich) {
    return `
    <button class="server-tile${featured}" type="button" data-host="${server.host}" title="${server.description || server.host}">
      <div class="server-tile-top">
        ${icon}
        <span class="partner-info">
          <strong>${server.name}</strong>
          <em>${server.host}</em>
        </span>
      </div>
      <p>${server.description || 'Click to join this server'}</p>
      <div class="server-tile-foot">
        <span class="partner-join ${count.cls}" data-partner-count="${server.host}">${count.text}</span>
        <span class="server-join">Join</span>
      </div>
    </button>`;
  }
  return `
    <button class="partner-card${featured}" type="button" data-host="${server.host}" title="${server.description || server.host}">
      <span class="partner-rank ${rankClass}">#${rank}</span>
      ${icon}
      <span class="partner-info">
        <strong>${server.name}</strong>
        <em>${server.host}</em>
      </span>
      <span class="partner-join ${count.cls}" data-partner-count="${server.host}">${count.text}</span>
    </button>
  `;
}

function renderPartnerRows(target) {
  if (!target) return;
  if (!PARTNER_SERVERS.length) {
    target.innerHTML = '<p class="partner-empty">Partner servers will appear here.</p>';
    return;
  }
  const rich = target.id === 'partnersPageList';
  target.innerHTML = PARTNER_SERVERS.map((s, i) => partnerCardHtml(s, i, rich)).join('');
  target.querySelectorAll('img.partner-icon').forEach((img) => {
    img.addEventListener('error', () => {
      const letter = img.dataset.fallback || '?';
      const fallback = document.createElement('span');
      fallback.className = 'partner-fallback';
      fallback.textContent = letter;
      img.replaceWith(fallback);
    }, { once: true });
  });
}

function updatePartnerCountUi() {
  document.querySelectorAll('[data-partner-count]').forEach((el) => {
    const count = partnerCountLabel(el.dataset.partnerCount);
    el.textContent = count.text;
    el.classList.remove('is-loading', 'is-online', 'is-offline');
    el.classList.add(count.cls);
  });
}

function applyPartnerIconsFromStatus() {
  document.querySelectorAll('.partner-card[data-host], .server-tile[data-host]').forEach((card) => {
    const host = card.dataset.host;
    const st = partnerStatusCache.get(String(host || '').toLowerCase());
    if (!st?.favicon || !st.favicon.startsWith('data:image')) return;
    const img = card.querySelector('img.partner-icon');
    if (img && img.src !== st.favicon) img.src = st.favicon;
  });
}

async function refreshPartnerStatuses() {
  const hosts = [...new Set(PARTNER_SERVERS.map((s) => s.host).filter(Boolean))];
  await Promise.all(hosts.map(async (host) => {
    try {
      const status = await window.nitro.pingServer(host);
      partnerStatusCache.set(String(host).toLowerCase(), status);
      const idx = PARTNER_SERVERS.findIndex((s) => s.host?.toLowerCase() === String(host).toLowerCase());
      if (idx >= 0 && status.favicon) {
        PARTNER_SERVERS[idx].icon = status.favicon;
      }
    } catch (_) {
      partnerStatusCache.set(String(host).toLowerCase(), { online: false, host });
    }
  }));
  updatePartnerCountUi();
  applyPartnerIconsFromStatus();
}

function renderPartnersHome() {
  els.homePartnerList = document.getElementById('homePartnerList');
  els.partnersPageList = document.getElementById('partnersPageList');
  els.partnerList = document.getElementById('partnerList');
  renderPartnerRows(els.homePartnerList);
  renderPartnerRows(els.partnersPageList);
  renderPartnerRows(els.partnerList);
  els.smpCard = document.getElementById('smpCard');
  els.smpDot = document.getElementById('smpDot');
  els.smpMeta = document.getElementById('smpMeta');
  els.smpMotd = document.getElementById('smpMotd');
  refreshPartnerStatuses();
}

let consoleOpen = false;
let consolePollTimer = null;

function appendConsoleLine(line) {
  const el = document.getElementById('consoleLog');
  if (!el || !line) return;
  const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 24;
  el.textContent += (el.textContent.endsWith('\n') || !el.textContent ? '' : '\n') + line;
  if (atBottom) el.scrollTop = el.scrollHeight;
}

async function loadConsoleLog() {
  const el = document.getElementById('consoleLog');
  if (!el || !window.nitro?.readLaunchLog) return;
  try {
    const data = await window.nitro.readLaunchLog();
    el.textContent = data?.text || 'No launch log yet.';
    el.scrollTop = el.scrollHeight;
  } catch (_) {
    el.textContent = 'Could not load launch log.';
  }
}

function setConsoleOpen(open) {
  const modal = document.getElementById('consoleModal');
  if (!modal) return;
  consoleOpen = !!open;
  modal.classList.toggle('hidden', !open);
  if (open) {
    loadConsoleLog();
    if (consolePollTimer) clearInterval(consolePollTimer);
    consolePollTimer = setInterval(() => {
      if (consoleOpen) loadConsoleLog();
    }, 1500);
  } else if (consolePollTimer) {
    clearInterval(consolePollTimer);
    consolePollTimer = null;
  }
}

function syncMemory(fromSlider) {
  if (!els.memorySlider || !els.memoryInput) return;
  const val = fromSlider
    ? parseInt(els.memorySlider.value, 10)
    : parseInt(els.memoryInput.value, 10) || 4096;
  els.memoryInput.value = val;
  els.memorySlider.value = Math.min(8192, Math.max(2048, val));
  const v = versions.find((x) => x.id === selectedId);
  updateMetaPills(v);
  window.nitro.saveSettings({ memory: val });
}

function updateAccountUi(account) {
  const isMicrosoft = loginMode === 'microsoft';
  if (els.username) els.username.disabled = isMicrosoft && account && !account.expired;
  const profileUser = document.getElementById('profileUsername');
  const offlineField = document.getElementById('profileOfflineField');
  if (profileUser) {
    profileUser.disabled = isMicrosoft && account && !account.expired;
    if (els.username && profileUser.value !== els.username.value) {
      profileUser.value = els.username.value;
    }
  }
  if (offlineField) offlineField.hidden = isMicrosoft;

  if (!isMicrosoft) {
    if (els.microsoftStatus) els.microsoftStatus.textContent = 'Using offline username from the account menu.';
    if (els.microsoftLoginBtn) els.microsoftLoginBtn.hidden = true;
    if (els.microsoftLogoutBtn) els.microsoftLogoutBtn.hidden = true;
    updateUserUi();
    return;
  }

  if (els.microsoftLoginBtn) els.microsoftLoginBtn.hidden = false;

  if (account && !account.expired) {
    if (els.microsoftStatus) els.microsoftStatus.textContent = `Signed in as ${account.username}`;
    if (els.username) els.username.value = account.username;
    if (els.microsoftLogoutBtn) els.microsoftLogoutBtn.hidden = false;
    if (els.microsoftLoginBtn) els.microsoftLoginBtn.textContent = 'Switch account';
  } else if (account?.expired) {
    if (els.microsoftStatus) els.microsoftStatus.textContent = 'Session expired — sign in again.';
    if (els.microsoftLogoutBtn) els.microsoftLogoutBtn.hidden = true;
    if (els.microsoftLoginBtn) els.microsoftLoginBtn.textContent = 'Sign in with Microsoft';
  } else {
    if (els.microsoftStatus) els.microsoftStatus.textContent = 'Sign in with your Microsoft account to play as premium.';
    if (els.microsoftLogoutBtn) els.microsoftLogoutBtn.hidden = true;
    if (els.microsoftLoginBtn) els.microsoftLoginBtn.textContent = 'Sign in with Microsoft';
  }
  updateUserUi();
}

async function refreshMicrosoftAccount() {
  try {
    const account = await window.nitro.getMicrosoftAccount();
    updateAccountUi(account);
    return account;
  } catch (_) {
    updateAccountUi(null);
    return null;
  }
}

function bindAccountMode() {
  if (!els.accountMode) return;

  els.accountMode.querySelectorAll('input[name="loginMode"]').forEach((input) => {
    input.checked = input.value === loginMode;
    input.addEventListener('change', async () => {
      if (!input.checked) return;
      loginMode = input.value;
      await window.nitro.saveSettings({ loginMode });
      await refreshMicrosoftAccount();
      updateDockMeta();
      updatePlayButtons();
    });
  });
}

async function launch(opts = {}) {
  if (launching) {
    // Ignore accidental double-clicks while starting; only cancel after a real hold period.
    if (Date.now() - launchStartedAt < 2500) {
      showToast('Already starting…');
      return;
    }
    try {
      await window.nitro.cancelLaunch();
      showToast('Launch stopped');
    } catch (e) {
      showToast(e.message || 'Could not stop launch');
    } finally {
      setLaunching(false);
      resetLaunchUi();
    }
    return;
  }

  const username = (els.username?.value || '').trim();
  if (loginMode !== 'microsoft' && !username) {
    showToast('Enter a username');
    els.username?.focus();
    return;
  }

  if (loginMode === 'microsoft') {
    const account = await refreshMicrosoftAccount();
    if (!account || account.expired) {
      showToast('Sign in with Microsoft first (Settings → Account)');
      setView('settings');
      return;
    }
  }

  const joinServer = opts.joinServer !== undefined ? opts.joinServer : null;

  launchStartedAt = Date.now();
  setLaunching(true);
  setLaunchUi({ line: 'Initializing Nitro Client…', percent: 8, phase: 'prepare' });

  try {
    const memory = parseInt(els.memoryInput?.value, 10) || 4096;
    await window.nitro.saveSettings({
      username,
      selectedVersion: selectedId,
      memory,
      modPreset,
      performanceMode,
      loginMode,
      rememberMicrosoftLogin
    });
    await window.nitro.launchGame({
      versionId: selectedId,
      username,
      memory,
      joinServer,
      loginMode
    });
    showToast('Minecraft is opening');
  } catch (e) {
    if (e.code === 'CANCELLED' || e.message === 'Launch cancelled') {
      resetLaunchUi();
      return;
    }
    const hint = launchErrorHint(e);
    setLaunchUi({ line: hint, percent: 0, phase: 'error' });
    showToast(hint);
    if (e.code === 'JAVA8_MISSING') {
      setView('settings');
      await refreshJavaStatus(true);
    } else if (e.code === 'BUNDLE_INCOMPLETE' || e.code === 'GAME_CRASH' || e.code === 'EARLY_EXIT') {
      setView('settings');
    }
  } finally {
    setLaunching(false);
  }
}
window.launch = launch;

async function repairInstall() {
  if (launching) return;
  setLaunching(true);
  setLaunchUi({ line: 'Repairing install…', percent: 15, phase: 'prepare' });
  try {
    await window.nitro.repairInstall();
    showToast('Repair complete');
    setLaunchUi({ line: 'Repair complete — ready to play', percent: 100, phase: 'done' });
  } catch (e) {
    showToast(e.message || 'Repair failed');
    setLaunchUi({ line: e.message || 'Repair failed', percent: 0, phase: 'error' });
  } finally {
    setLaunching(false);
    setTimeout(() => { if (!launching) resetLaunchUi(); }, 2500);
  }
}

async function renderPresets() {
  if (!els.presetGrid) return;
  const presets = await window.nitro.getPresets();
  els.presetGrid.innerHTML = presets.map((preset) => `
    <button type="button" class="preset-card ${preset.id === modPreset ? 'selected' : ''}" data-preset="${preset.id}">
      <strong>${preset.label}</strong>
      <span>${preset.desc}</span>
    </button>
  `).join('');

  els.presetGrid.querySelectorAll('[data-preset]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      modPreset = btn.dataset.preset;
      els.presetGrid.querySelectorAll('.preset-card').forEach((card) => {
        card.classList.toggle('selected', card.dataset.preset === modPreset);
      });
      await window.nitro.saveSettings({ modPreset });
      try {
        await window.nitro.applyPreset({ preset: modPreset, performanceMode });
        showToast('Preset saved: ' + btn.querySelector('strong').textContent);
      } catch (e) {
        showToast(e.message || 'Could not apply preset');
      }
    });
  });
}

function showOnboardingIfNeeded(settings) {
  if (!els.onboarding || settings.onboardingComplete) return;
  const input = document.getElementById('onboardingUsername');
  if (input) input.value = settings.username || els.username?.value || 'Player';
  els.onboarding.classList.remove('hidden');
  setTimeout(() => input?.focus(), 50);
}

const UPDATE_UNLOCK_KEY = 'nitro.unlockedUpdates';
let updateUnlockTimer = null;

function getUnlockedUpdates() {
  try {
    const raw = localStorage.getItem(UPDATE_UNLOCK_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

function saveUnlockedUpdates(ids) {
  localStorage.setItem(UPDATE_UNLOCK_KEY, JSON.stringify(ids));
}

function showUpdateUnlockCelebration(title) {
  const overlay = document.getElementById('updateUnlock');
  const titleEl = document.getElementById('updateUnlockTitle');
  const textEl = document.getElementById('updateUnlockText');
  if (!overlay) return;
  if (titleEl) titleEl.textContent = 'Congrats!';
  if (textEl) textEl.textContent = title
    ? `You unlocked “${title}”`
    : 'You unlocked updates';
  overlay.classList.remove('hidden');
  const card = overlay.querySelector('.update-unlock-card');
  if (card) {
    card.style.animation = 'none';
    // reflow to replay animation
    void card.offsetWidth;
    card.style.animation = '';
  }
  clearTimeout(updateUnlockTimer);
  updateUnlockTimer = setTimeout(() => overlay.classList.add('hidden'), 2200);
}

function initUpdateUnlocks() {
  const root = document.getElementById('homeUpdates');
  if (!root) return;
  const unlocked = new Set(getUnlockedUpdates());
  root.querySelectorAll('.update-card[data-update-id]').forEach((card) => {
    const id = card.dataset.updateId;
    if (unlocked.has(id)) {
      card.classList.remove('locked');
      card.classList.add('unlocked');
    } else {
      card.classList.add('locked');
      card.classList.remove('unlocked');
    }
    card.addEventListener('click', () => {
      if (!card.classList.contains('locked')) return;
      const title = card.querySelector('strong')?.textContent?.trim() || 'updates';
      unlocked.add(id);
      saveUnlockedUpdates([...unlocked]);
      card.classList.remove('locked');
      card.classList.add('unlocked');
      showUpdateUnlockCelebration(title);
    });
  });
}

function bindUi() {
  bindFriendsUi();
  bindSkinsUi();
  const syncUsernameFields = (value, source) => {
    const name = (value || '').trim().slice(0, 16);
    if (source !== 'chip' && els.username && els.username.value !== name) els.username.value = name;
    const profileUser = document.getElementById('profileUsername');
    if (source !== 'profile' && profileUser && profileUser.value !== name) profileUser.value = name;
    updateUserUi();
    window.nitro.saveSettings({ username: name || 'Player' });
  };
  els.username?.addEventListener('input', () => syncUsernameFields(els.username.value, 'chip'));
  document.getElementById('profileUsername')?.addEventListener('input', (e) => {
    syncUsernameFields(e.target.value, 'profile');
  });
  els.launchBtn?.addEventListener('click', () => launch({ joinServer: null }));
  els.launchSoloBtn?.addEventListener('click', () => launch({ joinServer: null }));
  els.playSmpBtn?.addEventListener('click', () => launch({ joinServer: null }));
  els.copyIpBtn?.addEventListener('click', async () => {
    await window.nitro.copyText(NITRO_SMP);
    showToast('Copied ' + NITRO_SMP);
  });
  els.discordBtn?.addEventListener('click', () => window.nitro.openExternal(DISCORD_URL));
  document.getElementById('homeDiscordBtn')?.addEventListener('click', () => window.nitro.openExternal(DISCORD_URL));
  document.getElementById('homeStoreBtn')?.addEventListener('click', () => window.nitro.openExternal(STORE_URL));
  document.getElementById('createProfileBtn')?.addEventListener('click', createLaunchProfile);
  els.repairBtn?.addEventListener('click', repairInstall);

  els.accountChipBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    const open = els.accountDropdown?.classList.contains('hidden');
    setAccountDropdownOpen(!!open);
  });
  els.accountSettingsBtn?.addEventListener('click', () => {
    setAccountDropdownOpen(false);
    setView('accounts');
  });
  els.accountManageBtn?.addEventListener('click', () => {
    setAccountDropdownOpen(false);
    setView('accounts');
  });
  els.accountsGotoSettings?.addEventListener('click', () => setView('accounts'));
  els.settingsOpenAccounts?.addEventListener('click', () => setView('accounts'));
  els.addAccountBtn?.addEventListener('click', async () => {
    setView('accounts');
    if (loginMode !== 'microsoft') {
      const ms = els.accountMode?.querySelector('input[value="microsoft"]');
      if (ms) {
        ms.checked = true;
        ms.dispatchEvent(new Event('change'));
      }
    }
    try {
      showToast('Opening Microsoft login…');
      await window.nitro.microsoftLogin({ remember: !!els.rememberMicrosoft?.checked });
      await refreshMicrosoftAccount();
      updateUserUi();
      showToast('Microsoft account ready');
    } catch (err) {
      showToast(err?.message || 'Microsoft login cancelled');
      els.microsoftLoginBtn?.focus();
    }
  });
  document.getElementById('resetSettingsBtn')?.addEventListener('click', async () => {
    if (!window.confirm('Reset launcher settings to defaults?')) return;
    localStorage.removeItem('nitro.spotifyClientId');
    localStorage.removeItem('nitro.reduceMotion');
    localStorage.removeItem('nitro.bgVideo');
    await window.nitro.saveSettings({
      memory: 4096,
      performanceMode: false,
      startWithWindows: false,
      minimizeToTray: false,
      checkUpdates: true,
      uiAnimations: true
    });
    if (els.memoryInput) els.memoryInput.value = '4096';
    if (els.memorySlider) els.memorySlider.value = '4096';
    showToast('Settings reset');
    setSettingsCategory('account');
  });
  document.getElementById('focusSkinBtn')?.addEventListener('click', () => {
    setView('skins');
  });
  document.getElementById('navSkins')?.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    setView('skins');
  });
  els.viewAllServersBtn?.addEventListener('click', async () => {
    await window.nitro.copyText(NITRO_SMP);
    showToast('Copied ' + NITRO_SMP);
  });
  els.smpCard?.addEventListener('click', () => launch({ joinServer: NITRO_SMP }));
  els.openClientFolderHome?.addEventListener('click', () => window.nitro.openClientFolder());
  els.openLaunchLogHome?.addEventListener('click', () => setConsoleOpen(true));
  document.getElementById('consoleModalClose')?.addEventListener('click', () => setConsoleOpen(false));
  document.getElementById('consoleCloseBtn')?.addEventListener('click', () => setConsoleOpen(false));
  document.getElementById('consoleRefreshBtn')?.addEventListener('click', () => loadConsoleLog());
  document.getElementById('consoleClearBtn')?.addEventListener('click', async () => {
    await window.nitro.clearLaunchLog?.();
    const el = document.getElementById('consoleLog');
    if (el) el.textContent = 'Log cleared.\n';
  });
  document.getElementById('consoleOpenFileBtn')?.addEventListener('click', () => window.nitro.openLaunchLog());
  document.getElementById('consoleModal')?.addEventListener('click', (e) => {
    if (e.target?.id === 'consoleModal') setConsoleOpen(false);
  });
  els.profilesFocusSearch?.addEventListener('click', () => {
    els.versionSearch?.focus();
  });
  document.addEventListener('click', (e) => {
    if (!els.accountChipWrap?.contains(e.target)) setAccountDropdownOpen(false);
  });

  els.openThemeModal?.addEventListener('click', () => setThemeModalOpen(true));
  els.themeModalClose?.addEventListener('click', () => setThemeModalOpen(false));
  els.themeResetBtn?.addEventListener('click', () => applyLauncherTheme('jungle'));
  els.themeSearch?.addEventListener('input', (e) => renderThemeGrid(e.target.value));
  els.themeModal?.addEventListener('click', (e) => {
    if (e.target === els.themeModal) setThemeModalOpen(false);
  });
  els.performanceModeInput?.addEventListener('change', async (e) => {
    performanceMode = e.target.checked;
    await window.nitro.saveSettings({ performanceMode });
    try {
      await window.nitro.applyPreset({ preset: modPreset, performanceMode });
      showToast(performanceMode ? 'Performance mode on' : 'Performance mode off');
    } catch (err) {
      showToast(err.message || 'Could not apply performance mode');
    }
  });
  els.onboardingDone?.addEventListener('click', async () => {
    const input = document.getElementById('onboardingUsername');
    const name = (input?.value || els.username?.value || 'Player').trim().slice(0, 16) || 'Player';
    if (els.username) els.username.value = name;
    updateUserUi();
    els.onboarding?.classList.add('hidden');
    await window.nitro.saveSettings({ onboardingComplete: true, username: name });
    showToast('Ready — hit Play when you are');
  });

  document.addEventListener('visibilitychange', () => {
    syncLauncherVideo();
    if (!document.hidden) {
      requestAnimationFrame(() => {
        window.nitroSkin?.recover?.();
        window.nitroSkin?.resize?.();
      });
    } else {
      window.nitroSkin?.pause?.(true);
    }
  });

  window.nitro?.onWindowState?.((state) => {
    if (state?.minimized) {
      window.nitroSkin?.pause?.(true);
      return;
    }
    // Restore / focus / unmaximize — recover layout + WebGL preview.
    requestAnimationFrame(() => {
      window.nitroSkin?.recover?.();
      window.nitroSkin?.resize?.();
      const onHome = document.getElementById('view-launchpad')?.classList.contains('active');
      if (onHome) window.nitroSkin?.pause?.(false);
    });
  });

  els.microsoftLoginBtn?.addEventListener('click', async () => {
    try {
      setLaunchUi({ line: 'Opening Microsoft sign-in…', percent: 5, phase: 'prepare' });
      const remember = els.rememberMicrosoft?.checked !== false;
      await window.nitro.microsoftLogin({ remember });
      rememberMicrosoftLogin = remember;
      loginMode = 'microsoft';
      bindAccountMode();
      await refreshMicrosoftAccount();
      showToast('Signed in with Microsoft');
    } catch (e) {
      showToast(e.message || 'Microsoft sign-in cancelled');
    } finally {
      resetLaunchUi();
    }
  });

  els.rememberMicrosoft?.addEventListener('change', async (e) => {
    rememberMicrosoftLogin = e.target.checked;
    await window.nitro.saveSettings({ rememberMicrosoftLogin });
  });

  els.microsoftLogoutBtn?.addEventListener('click', async () => {
    await window.nitro.microsoftLogout();
    loginMode = 'offline';
    bindAccountMode();
    els.username.disabled = false;
    updateAccountUi(null);
    showToast('Signed out');
  });
  els.versionSearch?.addEventListener('input', (e) => renderVersions(e.target.value));
  els.modSearch?.addEventListener('input', (e) => {
    clearTimeout(modSearchTimer);
    modSearchTimer = setTimeout(() => searchMods(e.target.value), 350);
  });
  els.switchVersionBtn?.addEventListener('click', () => setView('versions'));
  els.memorySlider?.addEventListener('input', () => syncMemory(true));
  els.memoryInput?.addEventListener('change', () => syncMemory(false));

  document.querySelectorAll('[data-view]').forEach((btn) => {
    btn.addEventListener('click', () => setView(btn.dataset.view));
  });

  els.settingsRail?.querySelectorAll('[data-cat]').forEach((btn) => {
    btn.addEventListener('click', () => setSettingsCategory(btn.dataset.cat));
  });

  els.bgVideoToggle?.addEventListener('change', (e) => {
    localStorage.setItem('nitro.bgVideo', e.target.checked ? '1' : '0');
    syncLauncherVideo();
  });

  els.reduceMotionToggle?.addEventListener('change', (e) => {
    localStorage.setItem('nitro.reduceMotion', e.target.checked ? '1' : '0');
    applyMotionPref();
  });

  els.openThemeFromSettings?.addEventListener('click', () => setThemeModalOpen(true));

  document.getElementById('openMcFolder')?.addEventListener('click', () => window.nitro.openMinecraftFolder());
  els.openModsFolderBtn?.addEventListener('click', () => window.nitro.openModsFolder({ versionId: selectedId }));
  document.getElementById('openClientFolder')?.addEventListener('click', () => window.nitro.openClientFolder());
  document.getElementById('openLaunchLogBtn')?.addEventListener('click', () => setConsoleOpen(true));
  document.getElementById('winMin')?.addEventListener('click', () => window.nitro.minimize());
  document.getElementById('winMax')?.addEventListener('click', () => window.nitro.toggleMaximize());
  document.getElementById('winClose')?.addEventListener('click', () => window.nitro.close());

  els.updateDownloadBtn?.addEventListener('click', async () => {
    try {
      await window.nitro.downloadLauncherUpdate();
    } catch (_) {
      const url = els.updateDownloadBtn.dataset.url;
      if (url) window.nitro.openExternal(url);
    }
  });

  els.setupJavaBtn?.addEventListener('click', () => runJavaSetup(false));
  els.javaWizardInstall?.addEventListener('click', () => runJavaSetup(true));
  els.javaWizardSkip?.addEventListener('click', () => els.javaWizard?.classList.add('hidden'));

  els.pickResourcePackBtn?.addEventListener('click', async () => {
    const result = await window.nitro.pickResourcePack();
    if (result?.path && els.resourcePackLabel) {
      els.resourcePackLabel.textContent = 'Selected: ' + result.path.split(/[/\\]/).pop();
      showToast('Resource pack selected');
    }
  });

  els.applyResourcePackBtn?.addEventListener('click', async () => {
    try {
      await window.nitro.applyResourcePack();
      showToast('Resource pack installed');
    } catch (e) {
      showToast(e.message || 'Could not install resource pack');
    }
  });

  els.exportSettingsBtn?.addEventListener('click', async () => {
    const result = await window.nitro.exportSettings();
    if (result?.path) showToast('Settings exported');
  });

  els.importSettingsBtn?.addEventListener('click', async () => {
    const result = await window.nitro.importSettings();
    if (result?.ok) {
      showToast('Settings imported — restart launch profile if needed');
      renderModToggles();
    }
  });

  // Partner rows are re-rendered; use delegation so joins keep working
  const onPartnerClick = (e) => {
    const btn = e.target.closest('.partner-card[data-host], .server-tile[data-host], .partner[data-host]');
    if (btn?.dataset.host) launch({ joinServer: btn.dataset.host });
  };
  document.getElementById('homePartnerList')?.addEventListener('click', onPartnerClick);
  els.partnerList?.addEventListener('click', onPartnerClick);
  document.getElementById('partnersPageList')?.addEventListener('click', onPartnerClick);
  bindOwnerUi();
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function setOwnerStatus(text) {
  const el = document.getElementById('ownerStatus');
  if (el) el.textContent = text;
}

function collectOwnerConfigFromDom() {
  const partners = [...document.querySelectorAll('#ownerPartnerEditor .owner-row')].map((row) => ({
    id: row.querySelector('[data-f="id"]')?.value?.trim() || undefined,
    name: row.querySelector('[data-f="name"]')?.value?.trim() || '',
    host: row.querySelector('[data-f="host"]')?.value?.trim() || '',
    tag: row.querySelector('[data-f="tag"]')?.value?.trim() || 'PARTNER',
    description: row.querySelector('[data-f="description"]')?.value?.trim() || '',
    icon: row.querySelector('[data-f="icon"]')?.value?.trim() || ''
  })).filter((p) => p.host);

  const news = [...document.querySelectorAll('#ownerNewsEditor .owner-row')].map((row) => ({
    title: row.querySelector('[data-f="title"]')?.value?.trim() || 'Update',
    body: row.querySelector('[data-f="body"]')?.value?.trim() || ''
  })).filter((n) => n.title || n.body);

  const changelog = [...document.querySelectorAll('#ownerChangelogEditor .owner-row')].map((row) => ({
    version: row.querySelector('[data-f="version"]')?.value?.trim() || '1.0.0',
    date: row.querySelector('[data-f="date"]')?.value?.trim() || '',
    items: String(row.querySelector('[data-f="items"]')?.value || '')
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
  })).filter((c) => c.version);

  return {
    ...(ownerLiveConfig || {}),
    partners,
    news,
    changelog,
    updatedAt: Date.now()
  };
}

function renderOwnerPartners(partners) {
  const root = document.getElementById('ownerPartnerEditor');
  if (!root) return;
  const list = Array.isArray(partners) ? partners : [];
  root.innerHTML = list.map((p, i) => `
    <div class="owner-row" data-index="${i}">
      <div class="owner-row-grid">
        <label class="owner-field"><span>Name</span><input data-f="name" value="${escapeHtml(p.name || '')}" /></label>
        <label class="owner-field"><span>Host / IP</span><input data-f="host" value="${escapeHtml(p.host || '')}" /></label>
        <label class="owner-field"><span>Tag</span>
          <select data-f="tag">
            ${['OFFICIAL', 'PARTNER', 'FAVORITE', 'SERVER'].map((tag) => `
              <option value="${tag}" ${String(p.tag || 'PARTNER').toUpperCase() === tag ? 'selected' : ''}>${tag}</option>
            `).join('')}
          </select>
        </label>
        <label class="owner-field"><span>Icon URL</span><input data-f="icon" value="${escapeHtml(p.icon || '')}" /></label>
      </div>
      <label class="owner-field"><span>Description</span><input data-f="description" value="${escapeHtml(p.description || '')}" /></label>
      <input type="hidden" data-f="id" value="${escapeHtml(p.id || '')}" />
      <div class="owner-row-actions">
        <button class="btn btn-sm owner-remove-partner" type="button">Remove</button>
      </div>
    </div>
  `).join('') || '<p class="settings-note">No partners yet.</p>';
}

function renderOwnerNews(news) {
  const root = document.getElementById('ownerNewsEditor');
  if (!root) return;
  const list = Array.isArray(news) ? news : [];
  root.innerHTML = list.map((n, i) => `
    <div class="owner-row" data-index="${i}">
      <label class="owner-field"><span>Title</span><input data-f="title" value="${escapeHtml(n.title || '')}" /></label>
      <label class="owner-field"><span>Body</span><textarea data-f="body" rows="3">${escapeHtml(n.body || '')}</textarea></label>
      <div class="owner-row-actions">
        <button class="btn btn-sm owner-remove-news" type="button">Remove</button>
      </div>
    </div>
  `).join('') || '<p class="settings-note">No news yet.</p>';
}

function renderOwnerChangelog(changelog) {
  const root = document.getElementById('ownerChangelogEditor');
  if (!root) return;
  const list = Array.isArray(changelog) ? changelog : [];
  root.innerHTML = list.map((c, i) => `
    <div class="owner-row" data-index="${i}">
      <div class="owner-row-grid">
        <label class="owner-field"><span>Version</span><input data-f="version" value="${escapeHtml(c.version || '')}" /></label>
        <label class="owner-field"><span>Date</span><input data-f="date" value="${escapeHtml(c.date || '')}" placeholder="2026-08-04" /></label>
      </div>
      <label class="owner-field"><span>Items (one per line)</span><textarea data-f="items" rows="4">${escapeHtml((c.items || []).join('\n'))}</textarea></label>
      <div class="owner-row-actions">
        <button class="btn btn-sm owner-remove-changelog" type="button">Remove</button>
      </div>
    </div>
  `).join('') || '<p class="settings-note">No changelog entries yet.</p>';
}

async function loadOwnerEditors() {
  if (!ownerUnlocked) return;
  ownerLiveConfig = await window.nitro.ownerGetLiveConfig();
  const pub = await window.nitro.ownerGetPublishSettings();
  const urlInput = document.getElementById('ownerPublishUrl');
  const tokenInput = document.getElementById('ownerPublishToken');
  if (urlInput) urlInput.value = pub.publishUrl || '';
  if (tokenInput) tokenInput.value = pub.publishToken || '';
  renderOwnerPartners(ownerLiveConfig.partners);
  renderOwnerNews(ownerLiveConfig.news);
  renderOwnerChangelog(ownerLiveConfig.changelog);
  const when = pub.lastPublishAt ? new Date(pub.lastPublishAt).toLocaleString() : 'never';
  setOwnerStatus(pub.lastPublishError
    ? `Draft loaded. Last publish error: ${pub.lastPublishError}`
    : `Draft loaded. Last publish: ${when}`);
}

function revealOwnerUi() {
  ownerUnlocked = true;
  document.body.classList.add('is-owner-unlocked');
  document.getElementById('ownerGate')?.classList.add('hidden');
  document.getElementById('ownerGate')?.setAttribute('aria-hidden', 'true');
  document.querySelectorAll('.owner-only').forEach((el) => el.classList.remove('hidden'));
  const brand = document.querySelector('.fc-brand-name');
  if (brand) brand.textContent = 'NITRO OWNER';
  document.title = 'Nitro Owner';
}

async function setupVideoShell() {
  try {
    const mode = await window.nitro.getVideoMode?.();
    videoBuild = !!mode?.enabled;
  } catch (_) {
    videoBuild = false;
  }
  if (videoBuild) {
    try { await window.nitroVideoShell?.enable?.(); } catch (_) {}
    // Prefer cinematic backdrop for recording shots.
    try {
      localStorage.setItem('nitro.bgVideo', '1');
      if (els.bgVideoToggle) els.bgVideoToggle.checked = true;
      syncLauncherVideo?.();
    } catch (_) { /* ignore */ }
  }
}

async function setupOwnerGate() {
  try {
    const mode = await window.nitro.getOwnerMode();
    ownerBuild = !!mode?.enabled;
  } catch (_) {
    ownerBuild = false;
  }
  if (!ownerBuild) {
    document.getElementById('ownerGate')?.classList.add('hidden');
    return true;
  }

  document.body.classList.add('is-owner');
  const gate = document.getElementById('ownerGate');
  gate?.classList.remove('hidden');
  gate?.setAttribute('aria-hidden', 'false');
  document.getElementById('bootSplash')?.classList.add('is-done');

  return new Promise((resolve) => {
    const unlock = async () => {
      const password = document.getElementById('ownerPasswordInput')?.value || '';
      const err = document.getElementById('ownerGateError');
      try {
        const result = await window.nitro.ownerUnlock(password);
        if (!result?.ok) {
          if (err) {
            err.hidden = false;
            err.textContent = result?.error || 'Wrong password';
          }
          return;
        }
        revealOwnerUi();
        await loadOwnerEditors();
        resolve(true);
      } catch (e) {
        if (err) {
          err.hidden = false;
          err.textContent = e.message || 'Unlock failed';
        }
      }
    };
    document.getElementById('ownerUnlockBtn')?.addEventListener('click', unlock);
    document.getElementById('ownerPasswordInput')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') unlock();
    });
  });
}

function bindOwnerUi() {
  document.getElementById('ownerAddPartnerBtn')?.addEventListener('click', () => {
    const cfg = collectOwnerConfigFromDom();
    cfg.partners.push({
      id: '',
      name: 'New Partner',
      host: 'play.example.com',
      tag: 'PARTNER',
      description: '',
      icon: ''
    });
    renderOwnerPartners(cfg.partners);
  });
  document.getElementById('ownerAddNewsBtn')?.addEventListener('click', () => {
    const cfg = collectOwnerConfigFromDom();
    cfg.news.unshift({ title: 'New headline', body: '' });
    renderOwnerNews(cfg.news);
  });
  document.getElementById('ownerAddChangelogBtn')?.addEventListener('click', () => {
    const cfg = collectOwnerConfigFromDom();
    cfg.changelog.unshift({
      version: '2.5.0',
      date: new Date().toISOString().slice(0, 10),
      items: ['Describe the change']
    });
    renderOwnerChangelog(cfg.changelog);
  });

  document.getElementById('ownerPartnerEditor')?.addEventListener('click', (e) => {
    if (!e.target.classList.contains('owner-remove-partner')) return;
    e.target.closest('.owner-row')?.remove();
  });
  document.getElementById('ownerNewsEditor')?.addEventListener('click', (e) => {
    if (!e.target.classList.contains('owner-remove-news')) return;
    e.target.closest('.owner-row')?.remove();
  });
  document.getElementById('ownerChangelogEditor')?.addEventListener('click', (e) => {
    if (!e.target.classList.contains('owner-remove-changelog')) return;
    e.target.closest('.owner-row')?.remove();
  });

  document.getElementById('ownerReloadBtn')?.addEventListener('click', async () => {
    try {
      await loadOwnerEditors();
      showToast('Owner draft reloaded');
    } catch (e) {
      showToast(e.message || 'Reload failed');
    }
  });

  document.getElementById('ownerSaveBtn')?.addEventListener('click', async () => {
    try {
      const cfg = collectOwnerConfigFromDom();
      const pubUrl = document.getElementById('ownerPublishUrl')?.value || '';
      const pubToken = document.getElementById('ownerPublishToken')?.value || '';
      ownerLiveConfig = await window.nitro.ownerSaveLiveConfig(cfg);
      await window.nitro.ownerSavePublishSettings({
        publishUrl: pubUrl,
        publishToken: pubToken
      });
      setOwnerStatus('Draft saved locally on this PC.');
      showToast('Draft saved');
    } catch (e) {
      showToast(e.message || 'Save failed');
    }
  });

  document.getElementById('ownerExportBtn')?.addEventListener('click', async () => {
    try {
      const cfg = collectOwnerConfigFromDom();
      const result = await window.nitro.ownerExportLiveConfig(cfg);
      if (!result?.canceled) {
        setOwnerStatus(`Exported to ${result.path}`);
        showToast('JSON exported — upload it to your remoteMetaUrl host');
      }
    } catch (e) {
      showToast(e.message || 'Export failed');
    }
  });

  document.getElementById('ownerPublishBtn')?.addEventListener('click', async () => {
    try {
      const cfg = collectOwnerConfigFromDom();
      const publishUrl = document.getElementById('ownerPublishUrl')?.value || '';
      const publishToken = document.getElementById('ownerPublishToken')?.value || '';
      setOwnerStatus('Publishing…');
      const result = await window.nitro.ownerPublishLiveConfig({ config: cfg, publishUrl, publishToken });
      ownerLiveConfig = result.config;
      setOwnerStatus(`Published via ${result.method} at ${new Date(result.at).toLocaleString()}`);
      showToast('Live config published');
      loadLauncherMeta();
    } catch (e) {
      setOwnerStatus(`Publish failed: ${e.message || 'unknown error'} — use Export JSON if your host has no upload API`);
      showToast(e.message || 'Publish failed');
    }
  });
}

async function init() {
  try {
    if (!window.nitro) {
      showToast('Launcher bridge missing — restart Nitro Client');
      return;
    }

    await setupVideoShell();
    await setupOwnerGate();

    versions = await window.nitro.getVersions();
    const settings = await window.nitro.getSettings();

    if (els.username) els.username.value = settings.username || 'Player';
    const profileUser = document.getElementById('profileUsername');
    if (profileUser) profileUser.value = settings.username || els.username?.value || 'Player';
    if (els.memoryInput) els.memoryInput.value = settings.memory || 4096;
    if (els.memorySlider) els.memorySlider.value = Math.min(8192, settings.memory || 4096);
    selectedId = resolveSelectedVersionId(settings.selectedVersion);
    launchProfiles = ensureLaunchProfiles(settings.launchProfiles);
    activeLaunchProfileId = settings.activeLaunchProfileId
      || launchProfiles.find((p) => p.versionId === selectedId)?.id
      || launchProfiles[0]?.id
      || '';
    modPreset = settings.modPreset || 'pvp';
    performanceMode = !!settings.performanceMode;
    loginMode = settings.loginMode || 'offline';
    rememberMicrosoftLogin = settings.rememberMicrosoftLogin !== false;
    if (els.rememberMicrosoft) els.rememberMicrosoft.checked = rememberMicrosoftLogin;
    if (els.performanceModeInput) els.performanceModeInput.checked = performanceMode;

    bindAccountMode();
    await refreshMicrosoftAccount();
    await refreshSecurityPanel();

    applyLauncherTheme(launcherThemeId);
    applyMotionPref();
    if (els.bgVideoToggle) els.bgVideoToggle.checked = isBgVideoEnabled();
    if (els.reduceMotionToggle) els.reduceMotionToggle.checked = isReduceMotionEnabled();
    setSettingsCategory('account');
    initUpdateUnlocks();
    try { applySkinsState(await window.nitro.skinsState()); } catch (_) { /* ignore */ }
    updateUserUi();
    selectVersion(selectedId);
    renderVersions();
    syncVersionControls();
    bindVersionControls();
    renderPartnersHome();
    renderPresets();
    renderModToggles();
    refreshJavaStatus(true);
    const fv = document.getElementById('footerVersion');
    if (fv) fv.textContent = '2.6.7';
    if (settings.resourcePackPath && els.resourcePackLabel) {
      els.resourcePackLabel.textContent = 'Selected: ' + settings.resourcePackPath.split(/[/\\]/).pop();
    }
    resetLaunchUi();
    await loadLauncherMeta();
    await loadServerHub();
    startLiveMetaPolling();
    refreshServerStatus();
    setInterval(refreshServerStatus, 60000);
    setInterval(refreshPartnerStatuses, 60000);
    showOnboardingIfNeeded(settings);
    if (ownerUnlocked) {
      setView('owner');
    }

    window.nitro.onLaunchProgress((payload) => {
      if (payload?.line && (consoleOpen || payload.consoleOnly)) {
        appendConsoleLine(payload.line);
      }
      if (payload?.consoleOnly) return;
      if (!launching && payload.phase !== 'cancelled') return;
      setLaunchUi({
        line: payload.line,
        percent: payload.percent,
        phase: payload.phase,
        idle: false
      });
      if (payload.phase === 'cancelled') {
        setLaunching(false);
      }
    });

    bindUi();
    refreshFriendsUi();
    runBootSequence();
  } catch (err) {
    console.error(err);
    showToast(err?.message || 'Launcher failed to start');
    try { bindUi(); } catch (_) {}
    runBootSequence();
  }
}

init();
