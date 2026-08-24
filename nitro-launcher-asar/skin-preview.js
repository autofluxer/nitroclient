(() => {
  function createSkinPreview({ hostId, canvasId, bodyId, fallbackId, zoom = 0.56, playerY = -2.5, lookY = 14.5 }) {
    let viewer = null;
    let resizeObserver = null;
    let currentKey = '';
    let use2d = false;
    let lastGoodSize = { w: 280, h: 360 };

    function host() {
      return document.getElementById(hostId);
    }

    function canvasEl() {
      return document.getElementById(canvasId);
    }

    function bodyImg() {
      return bodyId ? document.getElementById(bodyId) : null;
    }

    function stageSize() {
      const stage = host();
      if (!stage) return null;
      const w = Math.floor(stage.clientWidth);
      const h = Math.floor(stage.clientHeight);
      if (w < 32 || h < 32) return null;
      return { w, h };
    }

    function setFallback(visible, initial) {
      const fallback = fallbackId ? document.getElementById(fallbackId) : null;
      const stage = host();
      if (fallback && initial) fallback.textContent = initial;
      if (fallback) fallback.hidden = !visible;
      stage?.classList.toggle('has-skin', !visible);
    }

    function show2d(name) {
      const c = canvasEl();
      const body = bodyImg();
      use2d = true;
      if (c) c.hidden = true;
      if (body) {
        body.src = `https://mc-heads.net/body/${encodeURIComponent(name || 'Steve')}/512`;
        body.hidden = false;
      }
      setFallback(false);
      host()?.classList.add('mode-2d');
    }

    function show3d() {
      const c = canvasEl();
      const body = bodyImg();
      use2d = false;
      if (c) c.hidden = false;
      if (body) body.hidden = true;
      host()?.classList.remove('mode-2d');
    }

    function ensureLayersVisible() {
      if (!viewer?.playerObject?.skin) return;
      try {
        const skin = viewer.playerObject.skin;
        skin.visible = true;
        skin.setInnerLayerVisible?.(true);
        skin.setOuterLayerVisible?.(true);
        skin.resetJoints?.();
      } catch (_) { /* ignore */ }
    }

    function applyIdle() {
      if (!viewer || use2d) return;
      try {
        if (!viewer.animation && typeof skinview3d !== 'undefined' && skinview3d.IdleAnimation) {
          viewer.animation = new skinview3d.IdleAnimation();
          viewer.animation.speed = 0.55;
        }
      } catch (_) { /* ignore */ }
    }

    function framePlayer() {
      if (!viewer || use2d) return;
      try { viewer.resetCameraPose?.(); } catch (_) {}

      viewer.fov = 40;
      viewer.zoom = zoom;

      try {
        viewer.playerObject.position.set(0, playerY, 0);
        viewer.playerObject.rotation.set(0, Math.PI * 0.16, 0);
        viewer.playerWrapper.rotation.set(0, 0, 0);
      } catch (_) {}

      try {
        if (viewer.controls) {
          viewer.controls.target.set(0, lookY, 0);
          viewer.controls.enableDamping = true;
          viewer.controls.dampingFactor = 0.12;
          viewer.controls.enableZoom = false;
          viewer.controls.enablePan = false;
          viewer.controls.enableRotate = true;
          viewer.controls.update();
        }
        viewer.adjustCameraDistance?.();
      } catch (_) {}
    }

    function sizeViewer() {
      if (!viewer || use2d) return;
      const size = stageSize();
      if (!size) return;
      lastGoodSize = size;
      const { w, h } = size;
      if (viewer.width !== w || viewer.height !== h) {
        try {
          viewer.width = w;
          viewer.height = h;
        } catch (_) { /* ignore */ }
      }
      framePlayer();
    }

    function setVisible(visible) {
      if (!viewer || use2d) return;
      try { viewer.renderPaused = !visible; } catch (_) {}
      if (viewer.animation) viewer.animation.paused = !visible;
      if (viewer.controls) viewer.controls.enabled = !!visible;
      if (visible) {
        requestAnimationFrame(() => {
          sizeViewer();
          applyIdle();
        });
      }
    }

    function ensureViewer() {
      if (viewer) return viewer;
      if (typeof skinview3d === 'undefined') return null;
      const c = canvasEl();
      const stage = host();
      if (!c || !stage) return null;

      const size = stageSize() || lastGoodSize;

      viewer = new skinview3d.SkinViewer({
        canvas: c,
        width: size.w,
        height: size.h,
        skin: null,
        preserveDrawingBuffer: true
      });

      try { viewer.globalLight.intensity = 1.35; } catch (_) {}
      try { viewer.cameraLight.intensity = 0.85; } catch (_) {}
      viewer.background = null;
      viewer.autoRotate = false;
      viewer.autoRotateSpeed = 0;
      viewer.animation = null;

      if (viewer.controls) {
        viewer.controls.enableZoom = false;
        viewer.controls.enablePan = false;
        viewer.controls.enableDamping = true;
        viewer.controls.dampingFactor = 0.12;
        viewer.controls.enableRotate = true;
        viewer.controls.rotateSpeed = 0.7;
      }

      applyIdle();
      framePlayer();
      resizeObserver = new ResizeObserver(() => sizeViewer());
      resizeObserver.observe(stage);
      window.addEventListener('resize', sizeViewer);
      document.addEventListener('visibilitychange', () => {
        setVisible(!document.hidden);
      });
      window.addEventListener('focus', () => setVisible(true));
      show3d();
      sizeViewer();
      return viewer;
    }

    function modelOpt(model) {
      if (model === 'slim') return 'slim';
      if (model === 'classic' || model === 'default') return 'default';
      return 'auto-detect';
    }

    async function loadSkinUrl(v, url, model) {
      await v.loadSkin(url, { model: modelOpt(model) });
      try { v.nameTag = null; } catch (_) {}
      try { v.resetCape?.(); } catch (_) {}
      ensureLayersVisible();
      show3d();
      setFallback(false);
      sizeViewer();
      applyIdle();
      framePlayer();
    }

    async function setPlayer(name) {
      const safe = (name || 'Steve').trim() || 'Steve';
      const key = 'user:' + safe;
      if (key === currentKey && viewer && !use2d) return;
      currentKey = key;

      const v = ensureViewer();
      if (!v) {
        show2d(safe);
        return;
      }

      const urls = [
        `https://mc-heads.net/skin/${encodeURIComponent(safe)}`,
        `https://minotar.net/skin/${encodeURIComponent(safe)}`,
        `https://crafatar.com/skins/${encodeURIComponent(safe)}`
      ];

      for (const url of urls) {
        try {
          await loadSkinUrl(v, url, 'auto-detect');
          return;
        } catch (_) { /* try next */ }
      }

      show2d(safe);
    }

    async function setSkin(url, model) {
      const key = 'src:' + String(url || '').slice(0, 96) + ':' + (model || 'auto');
      if (key === currentKey && viewer && !use2d) {
        sizeViewer();
        return;
      }
      currentKey = key;
      const v = ensureViewer();
      if (!v || !url) return;
      try {
        await loadSkinUrl(v, url, model);
      } catch (_) {
        show2d('Steve');
      }
    }

    function pause(paused) {
      if (use2d || !viewer) return;
      viewer.autoRotate = false;
      try { viewer.renderPaused = !!paused; } catch (_) {}
      if (viewer.animation) viewer.animation.paused = !!paused;
      if (viewer.controls) viewer.controls.enabled = !paused;
      if (!paused) sizeViewer();
    }

    function setLaunchMode(on) {
      if (!viewer || use2d) return;
      viewer.autoRotate = false;
      applyIdle();
      try {
        viewer.globalLight.intensity = on ? 1.5 : 1.35;
        viewer.cameraLight.intensity = on ? 1.0 : 0.85;
      } catch (_) {}
    }

    function recover() {
      setVisible(true);
      sizeViewer();
    }

    return {
      setPlayer,
      setSkin,
      pause,
      resize: sizeViewer,
      recover,
      setLaunchMode,
      pickSplash: () => {}
    };
  }

  window.nitroSkin = createSkinPreview({
    hostId: 'skinStage',
    canvasId: 'skinCanvas',
    bodyId: 'skinBody',
    fallbackId: 'skinStageAvatar',
    zoom: 0.56
  });

  window.nitroSkinPage = createSkinPreview({
    hostId: 'skinsPreviewStage',
    canvasId: 'skinsPreviewCanvas',
    bodyId: 'skinsPreviewBody',
    fallbackId: 'skinsPreviewFallback',
    zoom: 0.78,
    playerY: 3.6,
    lookY: 17.2
  });
})();
