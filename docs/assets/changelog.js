// Page /changelog/ : la version visible est surlignée dans la colonne (et dans le select sur téléphone) ; le select saute à la version choisie
(function () {
  var nav = document.querySelector('.nav'), root = document.documentElement;
  function navh() { if (nav) root.style.setProperty('--navh', nav.offsetHeight + 'px'); }
  navh(); addEventListener('resize', navh);
  var arts = document.querySelectorAll('.cl-ver'), links = document.querySelectorAll('.cl-side a'), sel = document.getElementById('cl-jump');
  function mark(id) {
    links.forEach(function (a) { if (a.getAttribute('href') === '#' + id) a.setAttribute('aria-current', 'true'); else a.removeAttribute('aria-current'); });
    if (sel) sel.value = id;
  }
  if (sel) sel.addEventListener('change', function () {
    var t = document.getElementById(sel.value);
    if (t) { t.scrollIntoView(); history.replaceState(null, '', '#' + sel.value); }
  });
  if (!arts.length) return;
  mark(arts[0].id);
  if (!('IntersectionObserver' in window)) return;
  var seen = {};
  var io = new IntersectionObserver(function (entries) {
    entries.forEach(function (e) { seen[e.target.id] = e.isIntersecting; });
    for (var i = 0; i < arts.length; i++) if (seen[arts[i].id]) { mark(arts[i].id); return; }
  }, { rootMargin: '-25% 0px -60% 0px' });
  arts.forEach(function (a) { io.observe(a); });
})();
