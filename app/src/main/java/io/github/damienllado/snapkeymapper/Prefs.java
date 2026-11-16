package io.github.damienllado.snapkeymapper;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** App settings, launching the target and reading the system log. */
final class Prefs {
    interface Line { void received(String line); }

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static final String[] GESTURES = {"single", "double", "triple", "long"};

    static final int MAX_STEPS = 3;

    /** One thing a gesture does: launch an app, or open a link (which includes the built-in "snapkey:" actions). */
    static final class Step {
        final boolean app;
        final String value;

        Step(boolean app, String value) {
            this.app = app;
            this.value = value;
        }

        /** Built-in actions open nothing, so another step can follow; an app or a link ends the series. */
        boolean builtIn() { return !app && value.startsWith(Actions.SCHEME); }

        /** The app name, or the library title of the link; the link itself when it is not from the library. */
        String label(Context c) {
            if (app) {
                try {
                    PackageManager pm = c.getPackageManager();
                    return pm.getApplicationLabel(pm.getApplicationInfo(value, 0)).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    return value;
                }
            }
            Recipes.Recipe r = Recipes.forLink(c, value);
            return r != null ? r.title : value;
        }
    }

    /** The step edited by the library, app and link screens: 1 (the first, replaced) unless the home screen adds one after the last. */
    static volatile int slot = 1;

    /** Each gesture has its own targets, kept under "<gesture>.<name>" (first step) and "<gesture>.s2.<name>", "<gesture>.s3.<name>" (next steps); the app, recipe and link screens set the gesture shown on the home screen. */
    private static String k(Context c, String gesture, int step, String name) {
        migrate(c);
        return gesture + (step > 1 ? ".s" + step : "") + "." + name;
    }

    /** Up to 1.0.8 there was one target for one gesture: it becomes the target of that gesture. */
    private static void migrate(Context c) {
        SharedPreferences s = p(c);
        if (!s.contains("target_type") && !s.contains("link") && !s.contains("app_package")) return;
        String g = gesture(c);
        s.edit().putString(g + ".target_type", s.getString("target_type", "link")).putString(g + ".link", s.getString("link", ""))
                .putString(g + ".app_package", s.getString("app_package", "")).remove("target_type").remove("link").remove("app_package").apply();
    }

    /** Step n (from 1) of a gesture, or null when it has none. The first step keeps the keys of the single target of earlier versions. */
    static Step step(Context c, String g, int n) {
        SharedPreferences s = p(c);
        if (s.getString(k(c, g, n, "target_type"), "link").equals("app")) {
            String pkg = s.getString(k(c, g, n, "app_package"), "");
            return pkg.isEmpty() ? null : new Step(true, pkg);
        }
        String link = s.getString(k(c, g, n, "link"), "");
        return link.isEmpty() ? null : new Step(false, link);
    }

    /** What a gesture does, in order: up to 3 steps, ending at the first app or link. */
    static List<Step> chain(Context c, String g) {
        List<Step> out = new ArrayList<>();
        for (int n = 1; n <= MAX_STEPS; n++) {
            Step s = step(c, g, n);
            if (s == null) break;
            out.add(s);
            if (!s.builtIn()) break;
        }
        return out;
    }

    /** Replaces everything a gesture does by these steps. */
    static void setChain(Context c, String g, List<Step> steps) {
        SharedPreferences.Editor e = p(c).edit();
        for (int n = 1; n <= MAX_STEPS; n++) {
            e.remove(k(c, g, n, "target_type")).remove(k(c, g, n, "link")).remove(k(c, g, n, "app_package"));
            if (n > steps.size()) continue;
            Step s = steps.get(n - 1);
            e.putString(k(c, g, n, "target_type"), s.app ? "app" : "link").putString(k(c, g, n, s.app ? "app_package" : "link"), s.value);
        }
        e.apply();
    }

    /** The link typed or chosen for the step being edited. */
    static String link(Context c) { return p(c).getString(k(c, gesture(c), slot, "link"), ""); }
    static void targetType(Context c, String type) { p(c).edit().putString(k(c, gesture(c), slot, "target_type"), type).apply(); }
    static void appPackage(Context c, String pkg) { p(c).edit().putString(k(c, gesture(c), slot, "app_package"), pkg).apply(); }
    /** Nothing opens on this gesture any more. */
    static void clearTarget(Context c) { setChain(c, gesture(c), new ArrayList<>()); }
    /** Drops step n; the steps after it move up. */
    static void removeStep(Context c, String g, int n) {
        List<Step> all = new ArrayList<>();
        for (int i = 1; i <= MAX_STEPS; i++) {
            Step s = step(c, g, i);
            if (s != null && i != n) all.add(s);
        }
        setChain(c, g, all);
    }
    /** The OPPO Snap Key and the OnePlus Plus Key; other phones find theirs with the capture. */
    static final String DEFAULT_PATTERN = "KEYCODE_ACTION_BUTTON_CLICK";
    static String pattern(Context c) { return p(c).getString("pattern", DEFAULT_PATTERN); }
    /** An app, a shortcut or a link is chosen: only then there is something to open. */
    static boolean hasTarget(Context c, String g) { return step(c, g, 1) != null; }
    static boolean hasAnyTarget(Context c) {
        for (String g : GESTURES) if (hasTarget(c, g)) return true;
        return false;
    }
    static boolean active(Context c) { return p(c).getBoolean("active", false); }
    static void active(Context c, boolean active) { p(c).edit().putBoolean("active", active).putBoolean("paused", false).apply(); }
    /** Listening was turned off on purpose (quick settings tile): opening the app must not turn it back on. */
    static boolean paused(Context c) { return p(c).getBoolean("paused", false); }
    static void pause(Context c) { p(c).edit().putBoolean("active", false).putBoolean("paused", true).apply(); }
    /** The name of a gesture in the language of the app. */
    static String gestureLabel(Context c, String g) {
        switch (g) {
            case "single": return c.getString(R.string.gesture_single);
            case "double": return c.getString(R.string.gesture_double);
            case "triple": return c.getString(R.string.gesture_triple);
            default: return c.getString(R.string.gesture_long);
        }
    }
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
        p(c).edit().putString(k(c, gesture(c), slot, "link"), link.trim()).putString("pattern", pattern.trim()).apply();
    }

    /** Launches the app, or opens the link. */
    static void run(Context c, Step s) {
        if (s.app) {
            Intent i = c.getPackageManager().getLaunchIntentForPackage(s.value);
            if (i == null) throw new ActivityNotFoundException(s.value);
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            open(c, s.value);
        }
    }

    /** Opens a link: "intent:" links describe a full Android intent (set a timer, open the camera...), "snapkey:" links are built-in actions; anything else is opened as a link. */
    static void open(Context c, String link) {
        if (link.startsWith(Actions.SCHEME)) {
            Actions.run(c, link); // flashlight, ring mode, music: nothing to open
            return;
        }
        c.startActivity(intent(c, link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /** The intent a link stands for, without the flags and selector a pasted link could carry (URI permission grants, task flags). */
    static Intent intent(Context c, String link) {
        Intent i;
        try {
            i = link.startsWith("intent:") ? Intent.parseUri(link, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(c.getString(R.string.err_invalid_intent), e);
        }
        i.setSelector(null);
        i.setFlags(0);
        return i;
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
