package io.github.damienllado.snapkeymapper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/** App settings, launching the target and reading the system log. */
final class Prefs {
    interface Line { void received(String line); }

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    /** "link" or "app". */
    static String targetType(Context c) { return p(c).getString("target_type", "link"); }
    static void targetType(Context c, String type) { p(c).edit().putString("target_type", type).apply(); }
    static String link(Context c) { return p(c).getString("link", ""); }
    static String appPackage(Context c) { return p(c).getString("app_package", ""); }
    static void appPackage(Context c, String pkg) { p(c).edit().putString("app_package", pkg).apply(); }
    static String pattern(Context c) { return p(c).getString("pattern", ""); }
    static boolean active(Context c) { return p(c).getBoolean("active", false); }
    static void active(Context c, boolean active) { p(c).edit().putBoolean("active", active).apply(); }
    /** "single", "double" or "long". */
    static String gesture(Context c) { return p(c).getString("gesture", "double"); }
    static void gesture(Context c, String gesture) { p(c).edit().putString("gesture", gesture).apply(); }

    static void save(Context c, String link, String pattern) {
        p(c).edit().putString("link", link.trim()).putString("pattern", pattern.trim()).apply();
    }

    /** Opens the chosen link (any app that handles it, or an intent: link) or launches the chosen app. */
    static void openTarget(Context c) {
        Intent i;
        if (targetType(c).equals("app")) {
            i = c.getPackageManager().getLaunchIntentForPackage(appPackage(c));
        } else {
            String link = link(c);
            if (link.isEmpty()) return;
            try {
                // "intent:" links describe a full Android intent (set a timer, open the camera...); anything else is opened as a link
                i = link.startsWith("intent:") ? Intent.parseUri(link, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid intent link", e);
            }
            i.setSelector(null);
        }
        if (i == null) return;
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /** Runs logcat continuously (lines from now on); onEnd is called if the process stops. */
    static Process readLog(Line line, Runnable onEnd) throws IOException {
        Process p = new ProcessBuilder("logcat", "-v", "brief", "-T", "1", "-b", "main,system,events")
                .redirectErrorStream(true).start();
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) line.received(l);
            } catch (IOException ignore) {
                // process destroyed
            }
            if (onEnd != null) onEnd.run();
        }, "log-reader");
        t.setDaemon(true);
        t.start();
        return p;
    }
}
