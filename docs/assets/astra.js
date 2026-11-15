/* shangOS website script. Every page is complete without it: this only enhances.
   No network calls, no cookies. Stores two keys in localStorage: shangos-theme, shangos-os. */
(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const get = k => { try { return localStorage.getItem(k); } catch { return null; } };
  const set = (k, v) => { try { v == null ? localStorage.removeItem(k) : localStorage.setItem(k, v); } catch { /* storage blocked: the choice just won't persist */ } };

  /* ---- Theme: system by default; one toggle flips what you currently see. ----
     State lives in <meta name="color-scheme"> ("light dark" | "light" | "dark"); CSS maps it to color-scheme. */
  const meta = $('meta[name="color-scheme"]');
  const sys = matchMedia('(prefers-color-scheme: dark)');
  const btn = $('#theme');
  const shown = () => (meta.content === 'dark' || (meta.content !== 'light' && sys.matches)) ? 'dark' : 'light';
  const label = () => btn?.setAttribute('aria-label', shown() === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
  const apply = v => { meta.content = v === 'light' || v === 'dark' ? v : 'light dark'; label(); };
  btn?.addEventListener('click', () => {
    const target = shown() === 'dark' ? 'light' : 'dark';
    const system = sys.matches ? 'dark' : 'light';
    set('shangos-theme', target === system ? null : target);   // matching the system = undo the override
    apply(target === system ? null : target);
  });
  sys.addEventListener('change', label);
  addEventListener('storage', e => { if (e.key === 'shangos-theme') apply(e.newValue); });
  label();

  /* ---- Header: inline links when the row fits, native popover menu when it does not. ---- */
  const header = $('.site-header'), inner = $('.header-inner'), menu = $('#site-menu');
  if (header && inner && menu) {
    const fit = () => {
      header.classList.add('is-measuring', 'is-wide');
      menu.removeAttribute('popover');
      const fits = inner.scrollWidth <= inner.clientWidth + 1;
      header.classList.toggle('is-wide', fits);
      if (!fits) menu.setAttribute('popover', '');
      requestAnimationFrame(() => header.classList.remove('is-measuring'));
    };
    new ResizeObserver(fit).observe(header);
    document.fonts?.ready.then(fit);
    fit();
    menu.addEventListener('click', e => { if (e.target.closest('a') && menu.matches(':popover-open')) menu.hidePopover(); });
  }

  /* ---- OS tabs (ARIA tabs: arrows, Home, End). One remembered choice across the page. ---- */
  const osNow = () => { const p = (navigator.userAgentData?.platform || navigator.platform || '').toLowerCase(); return p.includes('win') ? 'windows' : /mac|iphone|ipad/.test(p) ? 'mac' : 'linux'; };
  const groups = $$('[data-tabs]');
  groups.forEach(g => {
    const tabs = $$('[role=tab]', g), panels = tabs.map(t => document.getElementById(t.getAttribute('aria-controls')));
    const pick = (i, focus) => {
      tabs.forEach((t, n) => { t.setAttribute('aria-selected', n === i); t.tabIndex = n === i ? 0 : -1; panels[n].hidden = n !== i; });
      if (focus) tabs[i].focus();
    };
    pick(Math.max(0, tabs.findIndex(t => t.dataset.os === (get('shangos-os') || osNow()))));
    tabs.forEach((t, i) => {
      t.addEventListener('click', () => { set('shangos-os', t.dataset.os); pick(i); groups.forEach(o => o !== g && o.dispatchEvent(new CustomEvent('os', { detail: t.dataset.os }))); });
      t.addEventListener('keydown', e => {
        const k = { ArrowRight: i + 1, ArrowLeft: i - 1, Home: 0, End: tabs.length - 1 }[e.key];
        if (k !== undefined) { e.preventDefault(); pick((k + tabs.length) % tabs.length, true); }
      });
    });
    g.addEventListener('os', e => { const i = tabs.findIndex(t => t.dataset.os === e.detail); if (i >= 0) pick(i); });
  });

  /* ---- Copy: the visible command, without prompts (.p) and comments (.c, .o). ---- */
  const live = document.body.appendChild(Object.assign(document.createElement('div'), { className: 'vh' }));
  live.setAttribute('role', 'status');
  document.addEventListener('click', async e => {
    const b = e.target.closest('.copy'); if (!b) return;
    const term = b.closest('.term');
    const src = $('[role=tabpanel]:not([hidden]) code', term) || $('code', term);
    const code = src.cloneNode(true);
    $$('.p,.c,.o', code).forEach(n => n.remove());
    const text = code.textContent.split('\n').map(l => l.trimEnd()).join('\n').trim().replace(/\n{2,}/g, '\n');
    let ok = true;
    try { await navigator.clipboard.writeText(text); } catch { ok = false; getSelection().selectAllChildren(src); }
    const old = b.textContent;
    b.textContent = ok ? 'Copied' : 'Press Ctrl+C'; b.dataset.state = 'done';
    live.textContent = ok ? 'Command copied to clipboard' : 'Command selected. Press Control or Command C to copy.';
    setTimeout(() => { b.textContent = old; delete b.dataset.state; live.textContent = ''; }, 2200);
  });

  /* ---- Docs contents: mark the section being read; collapsed on small screens. ---- */
  const toc = $('.toc');
  if (toc && 'IntersectionObserver' in window) {
    const links = $$('a[href^="#"]', toc), seen = new Set();
    const io = new IntersectionObserver(es => {
      es.forEach(en => (en.isIntersecting ? seen.add(en.target.id) : seen.delete(en.target.id)));
      const first = links.find(a => seen.has(a.getAttribute('href').slice(1)));
      links.forEach(a => a.removeAttribute('aria-current'));
      first?.setAttribute('aria-current', 'true');
    }, { rootMargin: '-20% 0px -65% 0px' });
    links.forEach(a => { const s = document.getElementById(a.getAttribute('href').slice(1)); if (s) io.observe(s); });
    if (toc.tagName === 'DETAILS') { const m = matchMedia('(min-width:1021px)'); const f = () => { toc.open = m.matches; }; f(); m.addEventListener('change', f); }
  }
})();
