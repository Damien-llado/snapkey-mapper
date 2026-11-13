/* Façade vidéo de /setup/ : l'iframe youtube-nocookie ne se charge qu'au clic. Sans JavaScript, le lien ouvre la vidéo sur YouTube. */
(function () {
  var a = document.querySelector('.video > a[data-embed]');
  if (!a) return;
  a.addEventListener('click', function (e) {
    e.preventDefault();
    var f = document.createElement('iframe');
    f.src = a.getAttribute('data-embed');
    f.title = a.getAttribute('data-title');
    f.allow = 'accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; fullscreen';
    f.allowFullscreen = true;
    f.referrerPolicy = 'strict-origin-when-cross-origin';
    a.replaceWith(f);
    f.focus();
  });
})();
