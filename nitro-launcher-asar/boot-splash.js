(() => {
  // 7x7 brick mask for a chunky "N"
  const MASK = [
    [1, 1, 0, 0, 0, 1, 1],
    [1, 1, 1, 0, 0, 1, 1],
    [1, 1, 1, 1, 0, 1, 1],
    [1, 1, 0, 1, 1, 1, 1],
    [1, 1, 0, 0, 1, 1, 1],
    [1, 1, 0, 0, 0, 1, 1],
    [1, 1, 0, 0, 0, 1, 1]
  ];

  function buildBrickLogo(root) {
    if (!root) return 0;
    root.innerHTML = '';
    const bricks = [];
    MASK.forEach((row, y) => {
      row.forEach((cell, x) => {
        if (!cell) return;
        const brick = document.createElement('span');
        brick.className = 'boot-brick';
        brick.style.setProperty('--bx', String(x));
        brick.style.setProperty('--by', String(y));
        brick.style.setProperty('--delay', `${(x + y) * 55 + Math.random() * 40}ms`);
        root.appendChild(brick);
        bricks.push(brick);
      });
    });
    requestAnimationFrame(() => root.classList.add('is-building'));
    return bricks.length;
  }

  function startParticles(canvas) {
    if (!canvas) return () => {};
    const ctx = canvas.getContext('2d');
    if (!ctx) return () => {};

    let raf = 0;
    let running = true;
    const particles = [];

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = Math.floor(window.innerWidth * dpr);
      canvas.height = Math.floor(window.innerHeight * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    const spawn = (count) => {
      for (let i = 0; i < count; i++) {
        particles.push({
          x: Math.random() * window.innerWidth,
          y: Math.random() * window.innerHeight,
          r: 0.6 + Math.random() * 1.8,
          a: 0.15 + Math.random() * 0.45,
          vx: -0.15 + Math.random() * 0.3,
          vy: -0.35 - Math.random() * 0.45,
          hue: 200 + Math.random() * 40
        });
      }
    };

    const tick = () => {
      if (!running) return;
      const w = window.innerWidth;
      const h = window.innerHeight;
      ctx.clearRect(0, 0, w, h);
      for (const p of particles) {
        p.x += p.vx;
        p.y += p.vy;
        if (p.y < -8) {
          p.y = h + 6;
          p.x = Math.random() * w;
        }
        ctx.beginPath();
        ctx.fillStyle = `hsla(${p.hue}, 80%, 78%, ${p.a})`;
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fill();
      }
      raf = requestAnimationFrame(tick);
    };

    resize();
    spawn(48);
    window.addEventListener('resize', resize);
    raf = requestAnimationFrame(tick);

    return () => {
      running = false;
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', resize);
    };
  }

  window.nitroBootSplash = {
    buildBrickLogo,
    startParticles
  };
})();
