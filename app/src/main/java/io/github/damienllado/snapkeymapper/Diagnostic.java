package io.github.damienllado.snapkeymapper;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Text a stuck user pastes in a GitHub issue: phone, system, state of each setup step. Never the link or app chosen. */
final class Diagnostic {
    private static final String NEW_ISSUE = "https://github.com/Damien-llado/snapkey-mapper/issues/new";
    /** Longest encoded body kept in the link: GitHub refuses very long addresses. */
    private static final int MAX_BODY = 6000;

    private Diagnostic() {}

    static void copy(Context c) {
        c.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("SnapKey Mapper", text(c)));
        Toast.makeText(c, c.getString(R.string.diag_copied), Toast.LENGTH_LONG).show();
    }

    static String text(Context c) {
        boolean[] s = SetupActivity.steps(c);
        return device(c)
                + "\nLanguage: " + java.util.Locale.getDefault().toLanguageTag()
                + "\nLog permission: " + s[0] + ", overlay: " + s[1] + ", battery: " + s[2] + ", native key off: " + s[3]
                + "\nSkipped checks: " + skipped(c)
                + "\nLast setup message: " + scrub(Prefs.setupResult(c)) + (Prefs.refused(c) ? " (refused by ColorOS)" : "")
                + "\nListening: " + ListenService.running + ", targets: single " + Prefs.hasTarget(c, "single") + ", double " + Prefs.hasTarget(c, "double") + ", triple " + Prefs.hasTarget(c, "triple") + ", long " + Prefs.hasTarget(c, "long")
                + "\nKey line: " + Prefs.pattern(c) + ", gesture shown: " + Prefs.gesture(c);
    }

    /** App version, phone and system: the top of every report. */
    static String device(Context c) {
        String version;
        try {
            version = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "?";
        }
        return "SnapKey Mapper " + version
                + "\nPhone: " + model() + " (" + Build.DEVICE + ")"
                + "\nSystem: Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + "), " + Build.DISPLAY
                + oplus();
    }

    /** Commercial name when the system gives it (OPPO and OnePlus: "OPPO Find X9 Pro"), with the model code. */
    static String model() {
        String name = prop("ro.vendor.oplus.market.name");
        if (name.equals("-")) name = prop("ro.product.marketname");
        return name.equals("-") ? Build.MANUFACTURER + " " + Build.MODEL : name + " (" + Build.MODEL + ")";
    }

    /** The key pattern the user found, for a new phone: only that line, never the capture (it holds other apps' logs). */
    static void shareKey(Context c, String pattern) {
        issue(c, "New key: " + model(), device(c) + "\n\nKey pattern:\n```\n" + pattern + "\n```");
    }

    static void askFix(Context c) {
        issue(c, "Doesn't work on " + model(), "What happens:\n\n\nDiagnostic:\n```\n" + text(c) + "\n```");
    }

    /** Opens a filled-in GitHub issue in the browser, which sends it (the lite edition has no INTERNET permission). No browser: copies the text. */
    private static void issue(Context c, String title, String body) {
        if (Uri.encode(body).length() > MAX_BODY) {
            String cut = "\n\n(Cut: too long for a link.)";
            while (Uri.encode(body + cut).length() > MAX_BODY) body = body.substring(0, body.length() - 100);
            body += cut;
        }
        Uri url = Uri.parse(NEW_ISSUE).buildUpon().appendQueryParameter("title", title)
                .appendQueryParameter("body", body).build();
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, url));
        } catch (ActivityNotFoundException e) {
            c.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("SnapKey Mapper", title + "\n\n" + body));
            Toast.makeText(c, c.getString(R.string.report_copied), Toast.LENGTH_LONG).show();
        }
    }

    /** The setup message can quote a socket error: no address, no port, no long hexadecimal string in a public issue. */
    static String scrub(String message) {
        String m = message.replaceAll("\\d{1,3}(\\.\\d{1,3}){3}", "<ip>").replaceAll("[0-9a-fA-F]*:[0-9a-fA-F:]{2,}", "<ip>")
                .replaceAll("(?i)port \\d+", "port <n>").replaceAll(":\\d{2,5}\\b", ":<n>");
        return m.length() > 160 ? m.substring(0, 160) + "..." : m;
    }

    /** OPPO, OnePlus and realme share one base: OxygenOS 13 shows "13.0" in the display property, OxygenOS 12 only "V12" in the other. */
    private static String oplus() {
        String v = prop("ro.build.version.oplusrom.display");
        if (v.equals("-")) v = prop("ro.build.version.oplusrom");
        if (v.equals("-")) return "";
        return Build.MANUFACTURER.equalsIgnoreCase("OnePlus") ? ", OxygenOS " + v : ", ColorOS " + v;
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
