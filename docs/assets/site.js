// Événements Clarity (téléchargement, GitHub, histoire partagée) : envoyés seulement si Clarity a été chargé après consentement (voir consent.js)
(function () {
  function track(n) { if (window.clarity) window.clarity('event', n); }
  document.addEventListener('click', function (e) {
    var a = e.target.closest('a'); if (!a) return;
    if (a.href.indexOf('/releases') > -1) track('download');
    else if (a.href.indexOf('github.com') > -1) track('github');
  });
  document.getElementById('story').addEventListener('submit', function () { track('story'); });
})();

// Étoiles et téléchargements GitHub en direct (affichés même à 0), en cache une heure pour rester sous la limite de l'API
(function () {
  var key = 'skm-counts', now = Date.now(), api = 'https://api.github.com/repos/Damien-llado/snapkey-mapper';
  function show(c) {
    document.querySelectorAll('[data-stars]').forEach(function (e) { e.textContent = c.stars; });
    document.querySelectorAll('[data-downloads]').forEach(function (e) { e.textContent = c.downloads; });
  }
  try {
    var c = JSON.parse(localStorage.getItem(key) || 'null');
    if (c && now - c.t < 3600e3) return show(c);
  } catch (e) {}
  function get(u) { return fetch(u).then(function (r) { return r.ok ? r.json() : null; }).catch(function () { return null; }); }
  Promise.all([get(api), get(api + '/releases?per_page=100')]).then(function (r) {
    if (!r[0]) return;
    var d = 0;
    (r[1] || []).forEach(function (rel) { rel.assets.forEach(function (a) { d += a.download_count; }); });
    var c = { stars: r[0].stargazers_count, downloads: d, t: now };
    show(c);
    try { localStorage.setItem(key, JSON.stringify(c)); } catch (e) {}
  });
})();

// Histoires : ouvre une discussion GitHub préremplie, rien n'est envoyé par la page elle-même
document.getElementById('story').addEventListener('submit', function (e) {
  e.preventDefault();
  var f = e.target.elements, what = f.what.value.trim();
  var body = '**What the Snap Key opens:** ' + what + '\n**Phone:** ' + f.phone.value + '\n**Gesture:** ' + f.gesture.value +
    (f.link.value.trim() ? '\n**Link or app:** `' + f.link.value.trim().replace(/`/g, '') + '`' : '') +
    (f.why.value.trim() ? '\n\n' + f.why.value.trim() : '');
  location.href = 'https://github.com/Damien-llado/snapkey-mapper/discussions/new?category=show-and-tell&title=' +
    encodeURIComponent('My Snap Key: ' + what) + '&body=' + encodeURIComponent(body);
});

// Mobile : le bouton Download passe dans l'en-tête quand celui du héros sort de l'écran
(function () {
  var cta = document.querySelector('.hero .ctas'); if (!cta || !window.IntersectionObserver) return;
  new IntersectionObserver(function (e) { document.body.classList.toggle('past-hero', !e[0].isIntersecting && e[0].boundingClientRect.top < 0); }).observe(cta);
})();

// Animations : appui sur la Snap Key, l'écran ouvre la cible suivante ; inclinaison au pointeur
(function () {
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  var T = [
    ['Claude Code', 'claude.ai/code/…', '#D97757', 'b-claude', 'Claude Code'],
    ['your run playlist', 'spotify:playlist:…', '#1ED760', 'b-spotify', 'Run playlist'],
    ['navigation home', 'google.navigation:q=…', '#4285F4', 'b-maps', 'Maps'],
    ['Strava', 'App: Strava', '#FC4C02', 'b-strava', 'Strava'],
    ["today's notes", 'docs.google.com/…', '#4285F4', 'b-gdocs', 'Notes'],
    ['the garage dashboard', 'homeassistant://navigate/…', '#18BCF2', 'b-ha', 'Home Assistant'],
    ['a WhatsApp chat', 'wa.me/…', '#25D366', 'b-wa', 'WhatsApp']
  ];
  var i = 0, rot = document.getElementById('rot'), stage = document.getElementById('stage'),
      launch = document.getElementById('launch'), use = launch.querySelector('use');
  function cycle() {
    if (document.hidden) return;
    i = (i + 1) % T.length;
    var t = T[i];
    stage.classList.remove('pressed'); void stage.offsetWidth; stage.classList.add('pressed');
    setTimeout(function () {
      launch.style.setProperty('--h', t[2]);
      use.setAttribute('href', '#' + t[3]);
      document.getElementById('launch-t').textContent = t[4];
      document.getElementById('launch-s').textContent = t[1];
      launch.classList.remove('go'); void launch.offsetWidth; launch.classList.add('go');
      rot.textContent = t[0];
      rot.classList.remove('go'); void rot.offsetWidth; rot.classList.add('go');
    }, 260);
  }
  // La scène d'ouverture joue d'abord (icônes, appui, Claude Code), puis le cycle démarre
  setTimeout(function () { setInterval(cycle, 2800); }, 1800);

  if (!matchMedia('(pointer: fine)').matches) return;
  var hero = document.querySelector('.hero'), raf = 0, mx = 0, my = 0;
  hero.addEventListener('pointermove', function (e) {
    var r = hero.getBoundingClientRect();
    mx = (e.clientX - r.left) / r.width * 2 - 1; my = (e.clientY - r.top) / r.height * 2 - 1;
    if (!raf) raf = requestAnimationFrame(function () { raf = 0; hero.style.setProperty('--mx', mx.toFixed(3)); hero.style.setProperty('--my', my.toFixed(3)); });
  });
  hero.addEventListener('pointerleave', function () { hero.style.setProperty('--mx', 0); hero.style.setProperty('--my', 0); });

  // Halo qui suit le pointeur sur les tuiles
  document.getElementById('bento').addEventListener('pointermove', function (e) {
    var t = e.target.closest('.tile'); if (!t) return;
    var r = t.getBoundingClientRect();
    t.style.setProperty('--x', (e.clientX - r.left) + 'px'); t.style.setProperty('--y', (e.clientY - r.top) + 'px');
  });
})();
// Apparitions au défilement, façon Audit Greenwashing : léger glissement et fondu rapide, en cascade
(function () {
  var reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
  var els = document.querySelectorAll('main > section:not(.hero) > .kicker, main > section:not(.hero) > h2, main > section > .sub, .facts, .split-copy > *, .console, .narrow > p, .table-wrap, .faq details, .story, .checks > li, .cando > div, .final-card, .bento > li, .gestures > li, .cards > li, .steps > li, .note');
  var steps = document.querySelector('.steps');
  if (reduce || !('IntersectionObserver' in window)) {
    els.forEach(function (e) { e.classList.add('in'); });
    steps.style.setProperty('--p', 1);
    return;
  }
  els.forEach(function (e) { e.setAttribute('data-r', e.matches('.steps > li') ? 'side' : ''); });
  var io = new IntersectionObserver(function (entries) {
    var k = 0;
    entries.forEach(function (en) {
      if (!en.isIntersecting) return;
      en.target.style.setProperty('--i', Math.min(k++, 6));
      en.target.classList.add('in');
      io.unobserve(en.target);
    });
  }, { rootMargin: '0px 0px -8% 0px', threshold: 0.12 });
  els.forEach(function (e) { io.observe(e); });

  // La frise des étapes se remplit à mesure qu'on descend
  var raf = 0;
  function fill() {
    raf = 0;
    var r = steps.getBoundingClientRect(), p = (innerHeight * 0.65 - r.top) / r.height;
    steps.style.setProperty('--p', Math.max(0, Math.min(1, p)).toFixed(3));
  }
  addEventListener('scroll', function () { if (!raf) raf = requestAnimationFrame(fill); }, { passive: true });
  fill();
})();
document.querySelectorAll('[data-copy]').forEach(function (el) {
  var box = document.createElement('div');
  box.className = 'copybox';
  el.parentNode.insertBefore(box, el);
  box.appendChild(el);
  var ICON = '<svg viewBox="0 0 24 24" aria-hidden="true"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>';
  var DONE = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>';
  var b = document.createElement('button');
  b.type = 'button';
  b.className = 'copy-ico';
  b.setAttribute('aria-label', document.body.getAttribute('data-copy-label') || 'Copy');
  b.innerHTML = ICON;
  b.addEventListener('click', function () {
    var text = el.getAttribute('data-copy') || el.textContent.trim();
    function done() { b.innerHTML = DONE; b.classList.add('ok'); setTimeout(function () { b.innerHTML = ICON; b.classList.remove('ok'); }, 1500); }
    try { navigator.clipboard.writeText(text).then(done); } catch (e) {}
  });
  box.appendChild(b);
});

// Bandeau de consentement : librairie vanilla-cookieconsent hébergée ici, puis notre configuration
(function () {
  ['/assets/vendor/cookieconsent.umd.js', '/assets/consent.js'].forEach(function (src) {
    var s = document.createElement('script'); s.src = src; s.async = false; document.body.appendChild(s);
  });
})();
