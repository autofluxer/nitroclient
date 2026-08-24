(() => {
  const app = document.getElementById('hostApp');
  if (!app || !window.nitro?.hostingState) return;

  let state = null;
  let selectedId = '';
  let cheats = false;
  let confirmStop = false;
  let tickTimer = null;

  function cleanErr(err) {
    return String(err?.message || err || 'Failed')
      .replace(/^Error invoking remote method '[^']+':\s*(?:Error:\s*)?/i, '');
  }

  function esc(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/"/g, '&quot;');
  }

  function formatDate(ms) {
    if (!ms) return '';
    try {
      return new Date(ms).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (_) {
      return '';
    }
  }

  function formatBytes(n) {
    if (n == null || Number.isNaN(n)) return '—';
    if (n < 1024) return n + ' B';
    if (n < 1024 * 1024) return (n / 1024).toFixed(0) + ' KB';
    if (n < 1024 * 1024 * 1024) return (n / (1024 * 1024)).toFixed(1) + ' MB';
    return (n / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
  }

  function formatUptime(startedAt) {
    if (!startedAt) return '0:00';
    const sec = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    if (h) return h + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
    return m + ':' + String(s).padStart(2, '0');
  }

  function setBar(id, pct) {
    const el = document.getElementById(id);
    if (el) el.style.width = Math.max(0, Math.min(100, pct || 0)) + '%';
  }

  function headUrl(name) {
    return 'https://mc-heads.net/avatar/' + encodeURIComponent(name || 'Steve') + '/64';
  }

  function modeLabel(mode) {
    const key = String(mode || 'survival').toLowerCase();
    return key.charAt(0).toUpperCase() + key.slice(1);
  }

  function setMode(mode) {
    app.classList.remove('is-setup', 'is-live', 'is-starting', 'is-confirm');
    app.classList.add('is-' + mode);
    if (confirmStop && mode === 'live') app.classList.add('is-confirm');
  }

  function applyState(next) {
    if (!next) return;
    state = next;
    if (next.event === 'error') window.showToast?.(next.session?.error || 'Hosting failed');
    if (!selectedId || !next.worlds?.some((w) => w.id === selectedId)) {
      selectedId = next.selectedWorldId || next.worlds?.[0]?.id || '';
    }
    if (next.config) {
      const modeEl = document.getElementById('hostMode');
      if (modeEl && next.config.mode) modeEl.value = next.config.mode;
      cheats = !!next.config.cheats;
    }
    render();
  }

  function selectedWorld() {
    return (state?.worlds || []).find((w) => w.id === selectedId) || state?.worlds?.[0] || null;
  }

  function thumbHtml(world, letterSize) {
    if (world?.icon) return `<img src="${esc(world.icon)}" alt="" />`;
    return `<span style="font-size:${letterSize || 16}px">${esc((world?.name || 'W').charAt(0).toUpperCase())}</span>`;
  }

  function renderWorlds() {
    const list = document.getElementById('hostWorldList');
    const count = document.getElementById('hostWorldCount');
    const worlds = state?.worlds || [];
    if (count) count.textContent = String(worlds.length);
    if (!list) return;
    if (!worlds.length) {
      list.innerHTML = '<p class="host-world-empty">No worlds yet.<br>Create one, or play singleplayer so Nitro can find your saves.</p>';
      return;
    }
    list.innerHTML = worlds.map((w) => {
      const sub = [w.source === 'saves' ? 'Local save' : 'Nitro', formatDate(w.lastPlayed) || 'New'].join(' · ');
      return `
        <button type="button" class="host-world-card${w.id === selectedId ? ' is-selected' : ''}" data-world="${esc(w.id)}">
          <div class="host-world-thumb">${thumbHtml(w)}</div>
          <div>
            <strong>${esc(w.name)}<em class="host-world-mode">${esc(modeLabel(w.mode))}</em></strong>
            <small>${esc(sub)}</small>
          </div>
        </button>`;
    }).join('');
  }

  function renderSelected() {
    const world = selectedWorld();
    const session = state?.session;
    const live = session?.status === 'online';
    const name = document.getElementById('hostSelectedName');
    const meta = document.getElementById('hostSelectedMeta');
    const tag = document.getElementById('hostSelectedTag');
    const art = document.getElementById('hostSelectedThumb');
    const stage = document.getElementById('hostStage');
    const startBtn = document.getElementById('hostStartBtn');
    if (name) name.textContent = live ? (session.worldName || world?.name || 'Hosted world') : (world?.name || 'Pick a world');
    if (meta) {
      meta.textContent = world
        ? [(world.source === 'saves' ? 'Local save' : 'Nitro world'), modeLabel(live ? session.mode : world.mode), formatDate(world.lastPlayed)].filter(Boolean).join(' · ')
        : 'Create a world or play singleplayer so Nitro can find your saves.';
    }
    if (tag) tag.textContent = live ? 'Live session' : (world ? 'Ready to host' : 'No world selected');
    if (art) art.innerHTML = world ? thumbHtml(world, 72) : '';
    stage?.classList.toggle('is-live', live);
    if (startBtn) startBtn.disabled = !world;
  }

  function renderSteps() {
    const session = state?.session;
    const live = session?.status === 'online';
    const starting = session?.status === 'starting' || state?.starting;
    const hasWorld = !!selectedWorld();
    document.querySelectorAll('#hostSteps .host-step').forEach((el) => {
      const step = Number(el.dataset.step);
      el.classList.remove('is-on', 'is-done');
      if (live) {
        if (step < 3) el.classList.add('is-done');
        else el.classList.add('is-on');
      } else if (starting) {
        if (step === 1) el.classList.add('is-done');
        else if (step === 2) el.classList.add('is-on');
      } else if (hasWorld) {
        if (step === 1) el.classList.add('is-done');
        else if (step === 2) el.classList.add('is-on');
      } else if (step === 1) {
        el.classList.add('is-on');
      }
    });
  }

  function renderDash() {
    const session = state?.session;
    const live = session?.status === 'online';
    const pill = document.getElementById('hostStatusPill');
    if (pill) {
      pill.classList.toggle('is-off', !live);
      const label = pill.querySelector('span');
      if (label) label.textContent = live ? 'Online' : (session?.status === 'starting' ? 'Starting' : 'Offline');
    }
    const world = document.getElementById('hostStatWorld');
    if (world) world.textContent = session?.worldName || selectedWorld()?.name || '—';
    const maxPlayers = session?.playersMax || 8;
    const onlinePlayers = live ? (session.playersOnline || 0) : 0;
    const players = document.getElementById('hostStatPlayers');
    if (players) players.textContent = onlinePlayers + '/' + maxPlayers;
    setBar('hostStatPlayersBar', (onlinePlayers / maxPlayers) * 100);

    const m = (live && session.metrics) || state?.machine || {};
    const cpu = document.getElementById('hostStatCpu');
    if (cpu) cpu.textContent = m.cpu != null ? Number(m.cpu).toFixed(1) + '%' : '—';
    setBar('hostStatCpuBar', Number(m.cpu) || 0);
    const ram = document.getElementById('hostStatRam');
    if (ram) ram.textContent = m.ramUsed != null ? formatBytes(m.ramUsed) : '—';
    setBar('hostStatRamBar', m.ramMax ? (m.ramUsed / m.ramMax) * 100 : 0);
    const netIn = document.getElementById('hostStatNetIn');
    if (netIn) netIn.textContent = live && m.netIn != null ? formatBytes(m.netIn) + '/s' : '—';
    const netOut = document.getElementById('hostStatNetOut');
    if (netOut) netOut.textContent = live && m.netOut != null ? formatBytes(m.netOut) + '/s' : '—';

    const uptime = document.getElementById('hostUptime');
    if (uptime) uptime.textContent = live ? formatUptime(session.startedAt) : '0:00';

    const addr = document.getElementById('hostJoinAddress');
    const host = (session?.address || '').trim();
    const port = session?.port && session.port !== 25565 ? ':' + session.port : '';
    if (addr) addr.textContent = live ? (host ? host + port : 'Waiting for public IP') : 'Offline';
    const code = document.getElementById('hostJoinCode');
    if (code) code.textContent = live && session?.joinCode ? session.joinCode : '—';
    const note = document.getElementById('hostProviderNote');
    if (note) {
      note.textContent = !live
        ? 'Host a world to generate an invite code. Friends can join from Nitro.'
        : (session.publicJoin
          ? 'Share the address or invite a friend. They can join from Nitro.'
          : 'Invite sends your Nitro join code. A hosting provider unlocks a public IP.');
    }
    document.getElementById('hostCopyAddress')?.toggleAttribute('disabled', !live);
    document.getElementById('hostCopyCode')?.toggleAttribute('disabled', !live);
  }

  function renderFriends() {
    const box = document.getElementById('hostFriendsList');
    if (!box) return;
    const friends = (window.__nitroFriendsState?.friends || []).slice(0, 12);
    if (!friends.length) {
      box.innerHTML = '<p class="host-world-empty">No friends yet. Add someone in Friends, then invite them here.</p>';
      return;
    }
    const invited = state?.session?.invites || [];
    const live = state?.session?.status === 'online';
    box.innerHTML = friends.map((f) => `
      <div class="host-friend">
        <div class="host-friend-ava">
          <img src="${headUrl(f.name)}" alt="" loading="lazy" onerror="this.style.display='none'" />
          <i class="dot${f.online ? ' on' : ''}"></i>
        </div>
        <div>
          <strong>${esc(f.name)}</strong>
          <small>${esc(f.statusText || (f.online ? 'Online' : 'Offline'))}</small>
        </div>
        <button type="button" class="${invited.includes(f.name) ? 'is-sent' : ''}" data-invite="${esc(f.name)}" ${live ? '' : 'disabled'}>${invited.includes(f.name) ? 'Sent' : 'Invite'}</button>
      </div>
    `).join('');
  }

  function renderHomeChip() {
    const chip = document.getElementById('hostHomeChip');
    const text = document.getElementById('hostHomeChipText');
    const live = state?.session?.status === 'online';
    if (chip) chip.hidden = !live;
    if (text && live) text.textContent = 'Hosting · ' + (state.session.worldName || 'World');
  }

  function render() {
    const session = state?.session;
    document.getElementById('hostCheatsBox')?.classList.toggle('is-on', cheats);
    renderWorlds();
    renderSelected();
    renderSteps();
    renderDash();
    renderFriends();
    renderHomeChip();
    const startLine = document.getElementById('hostStartLine');
    const startBar = document.getElementById('hostStartBar');
    if (startLine) startLine.textContent = session?.progress?.line || session?.error || 'Preparing…';
    if (startBar) startBar.style.width = Math.max(4, session?.progress?.percent || 0) + '%';

    if (session?.status === 'starting' || state?.starting) setMode('starting');
    else if (session?.status === 'online') setMode('live');
    else setMode('setup');

    if (confirmStop && session?.status === 'online') app.classList.add('is-confirm');
  }

  async function refresh() {
    try { applyState(await window.nitro.hostingState()); } catch (_) { /* ignore */ }
  }

  function startTick() {
    if (tickTimer) return;
    tickTimer = setInterval(async () => {
      try {
        if (state?.session?.status === 'online') applyState(await window.nitro.hostingRefresh());
        else if (document.getElementById('view-host')?.classList.contains('active')) {
          applyState(await window.nitro.hostingState());
        }
      } catch (_) { /* ignore */ }
    }, 4000);
  }

  async function startHost() {
    const worldId = selectedId || state?.selectedWorldId;
    if (!worldId) {
      window.showToast?.('Select or create a world first');
      return;
    }
    setMode('starting');
    try {
      applyState(await window.nitro.hostingStart({
        worldId,
        mode: document.getElementById('hostMode')?.value || 'survival',
        cheats
      }));
    } catch (err) {
      setMode('setup');
      window.showToast?.(cleanErr(err) || 'Could not start hosting');
    }
  }

  function bind() {
    document.getElementById('hostWorldList')?.addEventListener('click', (e) => {
      const id = e.target.closest('[data-world]')?.dataset?.world;
      if (!id) return;
      selectedId = id;
      renderWorlds();
      renderSelected();
      renderSteps();
      window.nitro.hostingSelectWorld(id).then(applyState).catch(() => renderWorlds());
    });
    document.getElementById('hostCreateToggle')?.addEventListener('click', () => {
      document.getElementById('hostCreateForm')?.classList.toggle('is-open');
      document.getElementById('hostCreateName')?.focus();
    });
    document.getElementById('hostCreateForm')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('hostCreateName')?.value || '';
      try {
        applyState(await window.nitro.hostingCreateWorld({
          name,
          mode: document.getElementById('hostMode')?.value || 'survival',
          cheats
        }));
        document.getElementById('hostCreateForm')?.classList.remove('is-open');
        if (document.getElementById('hostCreateName')) document.getElementById('hostCreateName').value = '';
      } catch (err) {
        window.showToast?.(cleanErr(err) || 'Could not create world');
      }
    });
    document.getElementById('hostCheatsBtn')?.addEventListener('click', () => {
      cheats = !cheats;
      document.getElementById('hostCheatsBox')?.classList.toggle('is-on', cheats);
      window.nitro.hostingConfig({ cheats, mode: document.getElementById('hostMode')?.value }).catch(() => {});
    });
    document.getElementById('hostMode')?.addEventListener('change', (e) => {
      window.nitro.hostingConfig({ mode: e.target.value, cheats }).catch(() => {});
    });
    document.getElementById('hostStartBtn')?.addEventListener('click', startHost);
    document.getElementById('hostStopBtn')?.addEventListener('click', () => {
      confirmStop = true;
      app.classList.add('is-confirm');
    });
    document.getElementById('hostStopCancel')?.addEventListener('click', () => {
      confirmStop = false;
      app.classList.remove('is-confirm');
    });
    document.getElementById('hostStopConfirm')?.addEventListener('click', async () => {
      confirmStop = false;
      try { applyState(await window.nitro.hostingStop()); } catch (err) {
        window.showToast?.(cleanErr(err) || 'Could not stop hosting');
      }
    });
    async function copyJoin(kind) {
      try {
        const info = await window.nitro.hostingJoinInfo();
        const text = kind === 'address'
          ? (info.address || '')
          : (kind === 'code' ? (info.joinCode || '') : (info.line || info.joinCode || ''));
        if (!text) {
          window.showToast?.(kind === 'address' && info.joinCode ? 'No public address yet — copy the invite code' : 'Host a world first');
          return;
        }
        await window.nitro.copyText(text);
        window.showToast?.(kind === 'address' ? 'Address copied' : (kind === 'code' ? 'Invite code copied' : 'Invite copied'));
      } catch (err) {
        window.showToast?.(err.message || 'Copy failed');
      }
    }
    document.getElementById('hostCopyBtn')?.addEventListener('click', () => copyJoin('line'));
    document.getElementById('hostCopyAddress')?.addEventListener('click', () => copyJoin('address'));
    document.getElementById('hostCopyCode')?.addEventListener('click', () => copyJoin('code'));
    document.getElementById('hostPlayBtn')?.addEventListener('click', async () => {
      const info = await window.nitro.hostingJoinInfo().catch(() => ({}));
      if (typeof window.launch === 'function') {
        window.launch({ joinServer: info.address || null });
      } else {
        window.showToast?.('Open Play to launch Minecraft');
      }
    });
    document.getElementById('hostFriendsList')?.addEventListener('click', async (e) => {
      const name = e.target?.dataset?.invite;
      if (!name) return;
      try {
        applyState(await window.nitro.hostingInvite(name));
        window.showToast?.('Invite sent to ' + name);
      } catch (err) {
        window.showToast?.(cleanErr(err) || 'Could not invite');
      }
    });
    window.nitro.onHostingUpdated?.((payload) => applyState(payload));
  }

  window.nitroHostUi = {
    show: refresh,
    apply: applyState,
    syncFriends: renderFriends
  };

  bind();
  refresh();
  startTick();
})();
