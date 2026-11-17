// Download links of the site: note the click (path, UTM, referring page, user agent, country, network), then send the APK.
// The note never delays nor blocks the download: it runs after the response (waitUntil) and its errors are ignored.
// No IP address is stored. Read by the daily monitoring (D1 database snapkey-clics, table clics).
const APK = 'https://github.com/Damien-llado/snapkey-mapper/releases/latest/download/snapkey-mapper.apk';

export function clic(context, chemin) {
  try {
    const {request, env} = context, q = new URL(request.url).searchParams, h = request.headers, cf = request.cf || {};
    let ref = h.get('referer') || '';
    try { if (ref) { const r = new URL(ref); ref = r.host + r.pathname; } } catch (e) { ref = ''; }
    const court = (v, n) => (v ? String(v).slice(0, n) : null);
    if (env.snapkey_clics) {
      context.waitUntil(env.snapkey_clics.prepare('INSERT INTO clics VALUES (?,?,?,?,?,?,?,?,?,?,?)')
        .bind(new Date().toISOString(), chemin, court(q.get('utm_source'), 60), court(q.get('utm_medium'), 60),
          court(q.get('utm_campaign'), 100), court(ref, 300), court(h.get('user-agent'), 400), court(cf.country, 2),
          cf.asn || null, court(cf.asOrganization, 100), court(h.get('accept-language'), 40))
        .run().catch(() => {}));
    }
  } catch (e) { /* the download goes on */ }
  return new Response(null, {status: 302, headers: {Location: APK, 'Cache-Control': 'no-store'}});
}
