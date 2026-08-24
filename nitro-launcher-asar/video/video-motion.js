(() => {
  let active = false;
  let orbitRaf = 0;

  function injectStylesheet() {
    if (document.getElementById('nitroVideoCss')) return;
    const link = document.createElement('link');
    link.id = 'nitroVideoCss';
    link.rel = 'stylesheet';
    link.href = 'video/video.css';
    document.head.appendChild(link);
  }

  function startSkinOrbit() {
    cancelAnimationFrame(orbitRaf);
    const tick = () => {
      if (!active || document.hidden) {
        orbitRaf = requestAnimationFrame(tick);
        return;
      }
      try {
        const stage = document.getElementById('skinStage');
        if (stage && !stage.classList.contains('mode-2d')) {
          const wrap = stage.closest('.dash-skin-wrap');
          if (wrap) {
            const t = performance.now() * 0.00035;
            wrap.style.transform = `translateY(${Math.sin(t) * 4}px)`;
          }
        }
      } catch (_) { /* ignore */ }
      orbitRaf = requestAnimationFrame(tick);
    };
    orbitRaf = requestAnimationFrame(tick);
  }

  async function enable() {
    if (active) return;
    active = true;
    injectStylesheet();
    document.body.classList.add('video-edition');
    // Keep stock Nitro Client branding / title.
    document.title = 'Nitro Client';
    startSkinOrbit();
  }

  window.nitroVideoShell = { enable };
})();
