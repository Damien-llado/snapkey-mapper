package io.github.damienllado.snapkeymapper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a press does as a short text to paste in a message or a comment, and the way back.
 * Format: "SKM1" then 1 to 3 steps separated by spaces, "a:" + package name (an app) or "l:" + the link with its special characters %-encoded.
 */
final class Share {
    private static final int VERSION = 1;
    // the text may sit inside a sentence or between quotes: take the first "SKM<n>" and the steps right after it
    private static final Pattern CODE = Pattern.compile("SKM(\\d+)((?:\\s+[al]:[^\\s`\"'<>]+)*)");

    private Share() {}

    static String export(List<Prefs.Step> steps) {
        StringBuilder sb = new StringBuilder("SKM" + VERSION);
        for (Prefs.Step s : steps) sb.append(' ').append(s.app ? "a:" + s.value : "l:" + Uri.encode(s.value, ":/").replace("'", "%27"));
        return sb.toString();
    }

    /** The steps in the text. IllegalArgumentException with a message to show when the text is not a shortcut, comes from a newer version, or aims at something this phone does not have. */
    static List<Prefs.Step> parse(Context c, String text) {
        Matcher m = CODE.matcher(text);
        if (!m.find()) throw new IllegalArgumentException(c.getString(R.string.share_invalid));
        if (Integer.parseInt(m.group(1)) > VERSION) throw new IllegalArgumentException(c.getString(R.string.share_newer));
        String[] parts = m.group(2).trim().split("\\s+");
        if (m.group(2).trim().isEmpty() || parts.length > Prefs.MAX_STEPS) throw new IllegalArgumentException(c.getString(R.string.share_invalid));
        List<Prefs.Step> steps = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            boolean app = parts[i].charAt(0) == 'a';
            String value = parts[i].substring(2);
            if (!app) value = Uri.decode(value);
            if (value.isEmpty() || value.length() > 2000 || app && !value.matches("[A-Za-z0-9._]+")) throw new IllegalArgumentException(c.getString(R.string.share_invalid));
            Prefs.Step s = new Prefs.Step(app, value);
            // only a built-in action can be followed by another step
            if (i < parts.length - 1 && !s.builtIn()) throw new IllegalArgumentException(c.getString(R.string.share_invalid));
            check(c, s);
            steps.add(s);
        }
        return steps;
    }

    /** Refuses a step this phone cannot run: an app that is not installed, a link nothing opens, an unknown built-in action. */
    private static void check(Context c, Prefs.Step s) {
        PackageManager pm = c.getPackageManager();
        if (s.app) {
            if (pm.getLaunchIntentForPackage(s.value) == null) throw new IllegalArgumentException(c.getString(R.string.fail_app_missing) + ": " + s.value);
        } else if (s.value.startsWith(Actions.SCHEME)) {
            if (!Actions.known(s.value)) throw new IllegalArgumentException(c.getString(R.string.share_newer));
        } else if (pm.resolveActivity(Prefs.intent(c, s.value), PackageManager.MATCH_DEFAULT_ONLY) == null) {
            throw new IllegalArgumentException(c.getString(R.string.fail_no_handler));
        }
    }
}
