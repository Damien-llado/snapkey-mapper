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
        return "SnapKey Mapper " + version + (AdbSetup.AVAILABLE ? "" : " (lite)")
                + "\nPhone: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")"
                + "\nSystem: Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + "), " + Build.DISPLAY
                + ", ColorOS " + prop("ro.build.version.oplusrom.display")
                + "\nLanguage: " + java.util.Locale.getDefault().toLanguageTag()
                + "\nLog permission: " + s[0] + ", overlay: " + s[1] + ", battery: " + s[2] + ", native key off: " + s[3]
                + "\nSkipped checks: " + skipped(c)
                + "\nLast setup message: " + Prefs.setupResult(c) + (Prefs.refused(c) ? " (refused by ColorOS)" : "")
                + "\nListening: " + ListenService.running + ", target chosen: " + Prefs.hasTarget(c) + " (" + Prefs.targetType(c) + ")"
                + "\nKey line: " + Prefs.pattern(c) + ", gesture: " + Prefs.gesture(c);
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
