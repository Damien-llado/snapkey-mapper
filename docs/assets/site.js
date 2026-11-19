// Événements Clarity (téléchargement, GitHub, histoire partagée) : envoyés seulement si Clarity a été chargé après consentement (voir consent.js)
(function () {
  function track(n) { if (window.clarity) window.clarity('event', n); }
  document.addEventListener('click', function (e) {
    var a = e.target.closest('a'); if (!a) return;
    if (a.href.indexOf('/releases') > -1 || a.pathname === '/download') track('download');
    else if (a.href.indexOf('github.com') > -1) track('github');
  });
  var story = document.getElementById('story');
  if (story) story.addEventListener('submit', function () { track('story'); });
})();

// Étoiles et téléchargements GitHub en direct (affichés même à 0), en cache une heure pour rester sous la limite de l'API
(function () {
  var key = 'skm-counts', now = Date.now(), api = 'https://api.github.com/repos/Damien-llado/snapkey-mapper';
  function show(c) {
    document.querySelectorAll('[data-stars]').forEach(function (e) { e.textContent = c.stars; });
    // le nombre de téléchargements n'apparaît sur le bouton que s'il a été lu (pas de « 0 » quand GitHub ne répond pas)
    document.querySelectorAll('[data-downloads]').forEach(function (e) { e.textContent = c.downloads; if (c.downloads > 0) e.parentNode.hidden = false; });
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
var storyForm = document.getElementById('story');
if (storyForm) storyForm.addEventListener('submit', function (e) {
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

// Icône de l'appli dans le mot qui tourne (décor : la page reste juste sans script)
(function () {
  var rot = document.getElementById('rot'); if (!rot) return;
  var im = document.createElement('img');
  im.id = 'rot-i'; im.alt = ''; im.width = im.height = 40; im.src = '/img/apps/claude.webp';
  rot.insertBefore(im, rot.firstChild);
})();

// Quatre gestes : vidéo de chaque appui (sujets anim-<geste>), chargée à l'approche de la section, en pause hors de l'écran.
// Elle part de l'image 60 (appli ouverte), celle de l'image fixe ; les points sous le téléphone suivent ses appuis (images 18 + début..fin).
(function () {
  var cards = document.querySelectorAll('.trio li[id^="g-"]'), v0 = document.createElement('video');
  if (!cards.length || !window.IntersectionObserver || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  if (/^((?!chrome|chromium|android).)*safari/i.test(navigator.userAgent) || (navigator.connection && navigator.connection.saveData) || !v0.canPlayType('video/webm; codecs="vp09.00.10.08"')) return;
  var PR = {single: [[0, 6]], double: [[0, 5], [9, 14]], triple: [[0, 5], [8, 13], [16, 21]], long: [[0, 20]]};
  var io = new IntersectionObserver(function (es) {
    es.forEach(function (e) {
      var li = e.target, v = li.querySelector('video');
      if (!e.isIntersecting) { if (v) v.pause(); return; }
      if (!v) {
        var g = li.id.slice(2), dots = li.querySelectorAll('.g i'), stage = li.querySelector('.stage');
        v = document.createElement('video');
        v.muted = true; v.loop = true; v.playsInline = true; v.width = 380; v.height = 535; v.setAttribute('aria-hidden', 'true');
        v.src = '/img/3d/x9-trio-' + g + '.webm';
        v.addEventListener('loadedmetadata', function () { v.currentTime = 2; }, {once: true});
        v.addEventListener('playing', function () { li.classList.add('vid'); }, {once: true});
        var tick = function () {
          var f = v.currentTime * 30 - 18;
          if (g === 'long') dots[0].style.setProperty('--p', f < 0 || f > 92 ? 0 : Math.min(1, f / 20));
          else PR[g].forEach(function (p, k) { if (dots[k]) dots[k].classList.toggle('on', f >= p[0] && f <= p[1] + 3); });
          if (v.requestVideoFrameCallback) v.requestVideoFrameCallback(tick);
        };
        if (v.requestVideoFrameCallback) v.requestVideoFrameCallback(tick); else v.addEventListener('timeupdate', tick);
        stage.appendChild(v);
      }
      v.play().catch(function () {});
    });
  }, {rootMargin: '200px 0px'});
  cards.forEach(function (li) { io.observe(li); });
})();

// Animations : vidéo du téléphone 3D (appui sur la Snap Key, l'appli s'ouvre, comme dans les Shorts) ; inclinaison au pointeur.
// La vidéo ne se charge qu'après la page, si le mouvement est permis et sans économie de données ; Safari n'a pas la transparence
// du VP9 : il garde l'image fixe. La phrase « land in … » suit l'appli ouverte (images 29 + 84 n à 30 i/s, sujet anim-heros).
(function () {
  var stage = document.querySelector('.hero .stage.s3d'); // accueil et pages OnePlus, Snap Key, Find X9 Pro (data-v : leur vidéo)
  if (matchMedia('(prefers-reduced-motion: reduce)').matches || !stage) return;
  var T = [ // noms d'applis seulement : la phrase autour est traduite, pas ces mots
    ['Claude Code', 'claude'], ['Spotify', 'spotify'], ['Google Maps', 'maps'], ['Strava', 'strava'],
    ['Google Docs', 'gdocs'], ['Home Assistant', 'homeassistant'], ['WhatsApp', 'whatsapp']
  ];
  var rot = document.getElementById('rot'), v = document.createElement('video'), cur = 0;
  var safari = /^((?!chrome|chromium|android).)*safari/i.test(navigator.userAgent);
  if (safari || (navigator.connection && navigator.connection.saveData) || !v.canPlayType('video/webm; codecs="vp09.00.10.08"')) return;
  function say(i) {
    if (i === cur || !rot) return; cur = i;
    rot.lastChild.textContent = T[i][0];
    document.getElementById('rot-i').src = '/img/apps/' + T[i][1] + '.webp';
    rot.classList.remove('go'); void rot.offsetWidth; rot.classList.add('go');
  }
  function tick() {
    var n = Math.floor((v.currentTime * 30 - 29) / 84);
    if (n >= 0 && n < T.length) say(n);
    if (v.requestVideoFrameCallback) v.requestVideoFrameCallback(tick);
  }
  addEventListener('load', function () {
    v.muted = true; v.loop = true; v.playsInline = true; v.setAttribute('aria-hidden', 'true');
    v.className = 's3d-v'; v.width = 760; v.height = 720; v.src = stage.dataset.v || '/img/3d/x9-heros.webm';
    v.addEventListener('playing', function () { stage.classList.add('playing'); }, {once: true});
    if (v.requestVideoFrameCallback) v.requestVideoFrameCallback(tick); else v.addEventListener('timeupdate', tick);
    stage.appendChild(v);
    new IntersectionObserver(function (e) { if (e[0].isIntersecting) v.play().catch(function () {}); else v.pause(); }).observe(stage);
  });

  if (!matchMedia('(pointer: fine)').matches) return;
  var hero = document.querySelector('.hero'), raf = 0, mx = 0, my = 0;
  hero.addEventListener('pointermove', function (e) {
    var r = hero.getBoundingClientRect();
    mx = (e.clientX - r.left) / r.width * 2 - 1; my = (e.clientY - r.top) / r.height * 2 - 1;
    if (!raf) raf = requestAnimationFrame(function () { raf = 0; hero.style.setProperty('--mx', mx.toFixed(3)); hero.style.setProperty('--my', my.toFixed(3)); });
  });
  hero.addEventListener('pointerleave', function () { hero.style.setProperty('--mx', 0); hero.style.setProperty('--my', 0); });

  // Halo qui suit le pointeur sur les tuiles
  var bento = document.getElementById('bento'); if (!bento) return;
  bento.addEventListener('pointermove', function (e) {
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
  if (!steps) return; // pages sans frise (changelog)
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
  ['/assets/vendor/cookieconsent.umd.js', '/assets/consent.js?v=2'].forEach(function (src) {
    var s = document.createElement('script'); s.src = src; s.async = false; document.body.appendChild(s);
  });
})();

// Provenance du visiteur (moteur, YouTube, Reddit, UTM), gardée pour l'onglet seulement (sessionStorage, rien d'autre) et
// ajoutée au lien Télécharger : la fonction /download la note avec le clic. Relie « arrivé par Google » à « a téléchargé ».
(function () {
  var cle = 'skm-from', from = null;
  try {
    from = sessionStorage.getItem(cle);
    if (!from) {
      var q = new URLSearchParams(location.search), ref = '';
      try { ref = document.referrer ? new URL(document.referrer).hostname : ''; } catch (e) {}
      from = q.get('utm_source') || (ref && ref !== location.hostname ? ref : 'direct');
      sessionStorage.setItem(cle, from.slice(0, 60));
    }
  } catch (e) { return; }
  document.addEventListener('click', function (e) {
    var a = e.target.closest('a');
    if (a && a.hostname === location.hostname && a.pathname === '/download' && from) a.search = '?utm_source=' + encodeURIComponent(from) + '&utm_medium=site';
  }, true);
})();
