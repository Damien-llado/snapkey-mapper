package io.github.damienllado.snapkeymapper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Text a stuck user pastes in a GitHub issue: phone, system, state of each setup step. Never the link or app chosen. */
final class Diagnostic {
    private Diagnostic() {}

    static void copy(Context c) {
        c.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("SnapKey Mapper", text(c)));
        Toast.makeText(c, c.getString(R.string.diag_copied), Toast.LENGTH_LONG).show();
    }

    static String text(Context c) {
        boolean[] s = SetupActivity.steps(c);
        String version;
        try {
            version = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "?";
        }
        return "SnapKey Mapper " + version
                + "\nPhone: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")"
                + "\nSystem: Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + "), " + Build.DISPLAY
                + oplus()
                + "\nLanguage: " + java.util.Locale.getDefault().toLanguageTag()
                + "\nLog permission: " + s[0] + ", overlay: " + s[1] + ", battery: " + s[2] + ", native key off: " + s[3]
                + "\nSkipped checks: " + skipped(c)
                + "\nLast setup message: " + scrub(Prefs.setupResult(c)) + (Prefs.refused(c) ? " (refused by ColorOS)" : "")
                + "\nListening: " + ListenService.running + ", target chosen: " + Prefs.hasTarget(c) + " (" + Prefs.targetType(c) + ")"
                + "\nKey line: " + Prefs.pattern(c) + ", gesture: " + Prefs.gesture(c);
    }

    /** The setup message can quote a socket error: no address, no port, no long hexadecimal string in a public issue. */
    static String scrub(String message) {
        String m = message.replaceAll("\\d{1,3}(\\.\\d{1,3}){3}", "<ip>").replaceAll("[0-9a-fA-F]*:[0-9a-fA-F:]{2,}", "<ip>")
                .replaceAll("(?i)port \\d+", "port <n>").replaceAll(":\\d{2,5}\\b", ":<n>");
        return m.length() > 160 ? m.substring(0, 160) + "..." : m;
    }

    private static String oplus() {
        String v = prop("ro.build.version.oplusrom.display");
        return v.equals("-") ? "" : ", ColorOS " + v;
    }

    private static String skipped(Context c) {
        StringBuilder b = new StringBuilder();
        for (String k : new String[]{"wifi", "dev", "notif", "sysopt", "battery"}) if (Prefs.skipped(c, k)) b.append(k).append(' ');
        return b.length() == 0 ? "none" : b.toString().trim();
    }

    /** System property readable by any app (getprop), empty when the phone does not have it. */
    private static String prop(String name) {
        try {
            Process p = new ProcessBuilder("getprop", name).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l = r.readLine();
                return l == null || l.isEmpty() ? "-" : l;
            }
        } catch (Exception e) {
            return "-";
        }
    }
}
