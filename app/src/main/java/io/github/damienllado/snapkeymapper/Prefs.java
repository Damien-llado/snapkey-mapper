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

    static final String[] GESTURES = {"single", "double", "triple", "long"};

    /** Each gesture has its own target, kept under "<gesture>.<name>"; the app, recipe and link screens set the gesture shown on the home screen. */
    private static String k(Context c, String gesture, String name) {
        migrate(c);
        return gesture + "." + name;
    }

    /** Up to 1.0.8 there was one target for one gesture: it becomes the target of that gesture. */
    private static void migrate(Context c) {
        SharedPreferences s = p(c);
        if (!s.contains("target_type") && !s.contains("link") && !s.contains("app_package")) return;
        String g = gesture(c);
        s.edit().putString(g + ".target_type", s.getString("target_type", "link")).putString(g + ".link", s.getString("link", ""))
                .putString(g + ".app_package", s.getString("app_package", "")).remove("target_type").remove("link").remove("app_package").apply();
    }

    /** "link" or "app". */
    static String targetType(Context c) { return targetTypeOf(c, gesture(c)); }
    static String targetTypeOf(Context c, String g) { return p(c).getString(k(c, g, "target_type"), "link"); }
    static void targetType(Context c, String type) { p(c).edit().putString(k(c, gesture(c), "target_type"), type).apply(); }
    static String link(Context c) { return link(c, gesture(c)); }
    static String link(Context c, String g) { return p(c).getString(k(c, g, "link"), ""); }
    static String appPackage(Context c) { return appPackageOf(c, gesture(c)); }
    static String appPackageOf(Context c, String g) { return p(c).getString(k(c, g, "app_package"), ""); }
    static void appPackage(Context c, String pkg) { p(c).edit().putString(k(c, gesture(c), "app_package"), pkg).apply(); }
    /** Nothing opens on this gesture any more. */
    static void clearTarget(Context c) {
        String g = gesture(c);
        p(c).edit().remove(k(c, g, "target_type")).remove(k(c, g, "link")).remove(k(c, g, "app_package")).apply();
    }
    /** The OPPO Snap Key and the OnePlus Plus Key; other phones find theirs with the capture. */
    static final String DEFAULT_PATTERN = "KEYCODE_ACTION_BUTTON_CLICK";
    static String pattern(Context c) { return p(c).getString("pattern", DEFAULT_PATTERN); }
    /** An app, a shortcut or a link is chosen: only then there is something to open. */
    static boolean hasTarget(Context c, String g) {
        return targetTypeOf(c, g).equals("app") ? !appPackageOf(c, g).isEmpty() : !link(c, g).isEmpty();
    }
    static boolean hasTarget(Context c) { return hasTarget(c, gesture(c)); }
    static boolean hasAnyTarget(Context c) {
        for (String g : GESTURES) if (hasTarget(c, g)) return true;
        return false;
    }
    static boolean active(Context c) { return p(c).getBoolean("active", false); }
    static void active(Context c, boolean active) { p(c).edit().putBoolean("active", active).apply(); }
    /** The gesture shown on the home screen: "single", "double", "triple" or "long". */
    static String gesture(Context c) { return p(c).getString("gesture", "double"); }
    static void gesture(Context c, String gesture) { p(c).edit().putString("gesture", gesture).apply(); }
    /** The long press of the Snap Key runs a ColorOS action (ring mode, flashlight...): read in the phone settings, false if unreadable. */
    static boolean nativeLongPress(Context c) {
        try {
            String v = android.provider.Settings.System.getString(c.getContentResolver(), "oplus_action_button_switch_state");
            return v != null && !v.isEmpty() && !v.equals("nothing");
        } catch (RuntimeException e) {
            return false;
        }
    }
    /** The user confirmed that the native Snap Key action is set to nothing (cannot be checked from an app). */
    static boolean nativeKeyOff(Context c) { return p(c).getBoolean("native_off", false); }
    static void nativeKeyOff(Context c, boolean off) { p(c).edit().putBoolean("native_off", off).apply(); }

    /** Outcome of the last automatic setup, shown on the setup screen. */
    /** A setup check the user said is done although the phone says otherwise (kept: a wrong check must never block). */
    static boolean skipped(Context c, String check) { return p(c).getBoolean("skip_" + check, false); }
    static void skip(Context c, String check) { p(c).edit().putBoolean("skip_" + check, true).apply(); }

    /** ColorOS refused the permission: the wizard asks for its hidden switch (a flag, not the translated message). */
    static boolean refused(Context c) { return p(c).getBoolean("setup_refused", false); }
    static void refused(Context c, boolean refused) { p(c).edit().putBoolean("setup_refused", refused).apply(); }

    /** After a failed setup, the automatic checks are asked again: an "Already done" tapped by mistake must not hide a real step. */
    static void clearSkips(Context c) {
        p(c).edit().remove("skip_wifi").remove("skip_dev").remove("skip_notif").apply();
    }

    static String setupResult(Context c) { return p(c).getString("setup_result", ""); }
    static void setupResult(Context c, String text) { p(c).edit().putString("setup_result", text).apply(); }

    static void save(Context c, String link, String pattern) {
        p(c).edit().putString(k(c, gesture(c), "link"), link.trim()).putString("pattern", pattern.trim()).apply();
    }

    /** Opens the chosen link (any app that handles it, or an intent: link) or launches the chosen app. */
    static void openTarget(Context c) { openTarget(c, gesture(c)); }
    static void openTarget(Context c, String g) {
        if (targetTypeOf(c, g).equals("app")) {
            Intent i = c.getPackageManager().getLaunchIntentForPackage(appPackageOf(c, g));
            if (i != null) c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (!link(c, g).isEmpty()) {
            open(c, link(c, g));
        }
    }

    /** Opens a link: "intent:" links describe a full Android intent (set a timer, open the camera...), "snapkey:" links are built-in actions; anything else is opened as a link. */
    static void open(Context c, String link) {
        if (link.startsWith(Actions.SCHEME)) {
            Actions.run(c, link); // flashlight, ring mode, music: nothing to open
            return;
        }
        Intent i;
        try {
            i = link.startsWith("intent:") ? Intent.parseUri(link, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(c.getString(R.string.err_invalid_intent), e);
        }
        i.setSelector(null);
        i.setFlags(0); // a pasted link must not carry flags of its own (URI permission grants, task flags)
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /** Runs logcat continuously (lines from now on); onEnd is called if the process stops. */
    static Process readLog(Line line, Runnable onEnd) throws IOException {
        Process p = new ProcessBuilder("logcat", "-v", "brief", "-v", "uid", "-T", "1", "-b", "main,system,events")
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
