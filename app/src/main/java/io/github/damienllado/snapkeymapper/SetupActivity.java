package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Guided setup: one screen per step, a single button that opens the exact setting, and the next step as soon as the phone says it is done. */
public class SetupActivity extends Activity {
    static final int TOTAL = 4;
    private static final String COMMAND = "adb shell pm grant io.github.damienllado.snapkeymapper android.permission.READ_LOGS";
    private LinearLayout page;

    /** Which steps are done: log reading, display over other apps, battery, native Snap Key action off (ticked by the user). */
    static boolean[] steps(Context c) {
        return new boolean[]{
                c.checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED,
                Settings.canDrawOverlays(c),
                // ColorOS has its own battery manager and may keep saying "optimized": the user can say it is done
                c.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(c.getPackageName()) || Prefs.skipped(c, "battery"),
                Prefs.nativeKeyOff(c)};
    }

    static int done(Context c) {
        int n = 0;
        for (boolean b : steps(c)) if (b) n++;
        return n;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = Ui.page(this);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        page.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 32));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        show(); // back from a settings screen: the step may be done now
    }

    /** Draws the first step that is not done yet, or the end screen. */
    private void show() {
        page.removeAllViews();
        boolean[] ok = steps(this);
        int current = 0;
        while (current < TOTAL && ok[current]) current++;
        if (current == TOTAL) {
            page.addView(Ui.title(this, getString(R.string.wiz_done_title)));
            Ui.add(page, Ui.body(this, getString(R.string.wiz_done_text)), 6);
            Ui.add(page, Ui.button(this, getString(R.string.wiz_done_btn), true, v -> finish()), 20);
            return;
        }
        page.addView(Ui.text(this, getString(R.string.setup_progress, current + 1, TOTAL), 13, Ui.muted));
        String pkg = "package:" + getPackageName();
        switch (current) {
            case 0: logStep(); break;
            case 1:
                card(R.string.perm_overlay, getString(R.string.wiz_overlay_text),
                        getString(R.string.setup_open_setting), () -> {
                            // the switch of this app itself; since Android 11 the public action only shows the list of all apps
                            Ui.open(this, new Intent("android.settings.MANAGE_APP_OVERLAY_PERMISSION", Uri.parse(pkg)),
                                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(pkg)));
                        });
                Ui.add(page, Ui.body(this, getString(R.string.wiz_restricted)), 16);
                Ui.add(page, Ui.button(this, getString(R.string.perm_app_settings), false, v -> appInfo()), 8);
                break;
            case 2:
                card(R.string.perm_battery, getString(R.string.wiz_battery_text),
                        getString(R.string.perm_allow), () -> Ui.open(this, new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse(pkg)),
                                new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
                skip("battery");
                break;
            default:
                card(R.string.setup_native_title, getString(R.string.wiz_native_text),
                        getString(R.string.setup_open_phone_settings), () -> Ui.open(this));
                CheckBox box = new CheckBox(this);
                box.setText(getString(R.string.setup_native_done));
                box.setTextColor(Ui.ink);
                box.setOnCheckedChangeListener((c, checked) -> { Prefs.nativeKeyOff(this, checked); show(); });
                Ui.add(page, box, 14);
        }
    }

    /** Step 1, in small parts: Wi-Fi, Developer options, then pairing (full edition); a command for a computer (lite). */
    private void logStep() {
        if (!AdbSetup.AVAILABLE) {
            card(R.string.perm_log, getString(R.string.wiz_computer_text), null, null);
            Ui.add(page, commandBox(), 12);
            return;
        }
        String last = Prefs.setupResult(this);
        if (!wifi() && !skipped("wifi")) {
            card(R.string.wiz_wifi_title, getString(R.string.wiz_wifi_text),
                    getString(R.string.wiz_wifi_btn), () -> Ui.open(this, new Intent(Settings.Panel.ACTION_WIFI), new Intent(Settings.ACTION_WIFI_SETTINGS)));
            skip("wifi");
        } else if (!devOptions() && !skipped("dev")) {
            card(R.string.wiz_dev_title, getString(R.string.wiz_dev_text),
                    getString(R.string.wiz_dev_btn), () -> Ui.openSetting(this, Settings.ACTION_DEVICE_INFO_SETTINGS, "build_number"));
            skip("dev");
        } else if (!notifications() && !skipped("notif")) {
            // the pairing code is typed in a notification: without notifications, nowhere to type it
            card(R.string.wiz_notif_title, getString(R.string.wiz_notif_text), getString(R.string.wiz_notif_btn),
                    () -> Ui.open(this, new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()),
                            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
            skip("notif");
        } else if (Prefs.refused(this) || (oplus() && !skipped("sysopt"))) {
            // ColorOS refuses the grant until a hidden switch (only shown in English) is on
            card(R.string.wiz_sysopt_title, getString(R.string.wiz_sysopt_text),
                    getString(R.string.wiz_sysopt_btn), () -> Ui.open(this, new Intent(Settings.ACTION_LOCALE_SETTINGS)));
            Ui.add(page, Ui.button(this, getString(R.string.wiz_sysopt_btn2), false, v -> Ui.open(this)), 8);
            Ui.add(page, Ui.button(this, getString(R.string.wiz_sysopt_done), false, v -> {
                Prefs.skip(this, "sysopt");
                Prefs.refused(this, false);
                Prefs.setupResult(this, "");
                show();
            }), 8);
        } else {
            card(R.string.wiz_pair_title, getString(R.string.wiz_pair_text),
                    getString(R.string.perm_auto_btn), () -> {
                        Prefs.setupResult(this, ""); // a new attempt: the previous message no longer applies
                        AdbSetup.start(this);
                    });
            if (!last.isEmpty() && !last.equals(getString(R.string.setup_done))) {
                Ui.add(page, Ui.body(this, getString(R.string.wiz_last, last)), 14);
            }
        }
        TextView other = Ui.button(this, getString(R.string.setup_other_way), false, null);
        View box = commandBox();
        box.setVisibility(View.GONE);
        other.setOnClickListener(v -> box.setVisibility(box.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        Ui.add(page, other, 24);
        Ui.add(page, box, 8);
    }

    /** A check can be wrong on some phones (VPN, ROM that hides a setting): "Already done" moves on, and stays chosen. */
    private boolean skipped(String check) { return Prefs.skipped(this, check); }

    private void skip(String check) {
        Ui.add(page, Ui.button(this, getString(R.string.wiz_skip), false, v -> { Prefs.skip(this, check); show(); }), 8);
    }

    /** OPPO, OnePlus and realme phones run ColorOS. */
    private static boolean oplus() {
        String m = android.os.Build.MANUFACTURER.toLowerCase(java.util.Locale.ROOT);
        return m.contains("oppo") || m.contains("oneplus") || m.contains("realme");
    }

    /** Developer options on: the setting itself, or USB debugging on (only possible with them on). */
    private boolean devOptions() {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
                || Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
    }

    /** Notifications of the app on, and the channel of the pairing code not blocked. */
    private boolean notifications() {
        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        android.app.NotificationChannel ch = nm.getNotificationChannel("setup");
        return nm.areNotificationsEnabled() && (ch == null || ch.getImportance() != android.app.NotificationManager.IMPORTANCE_NONE);
    }

    /** Title, what to do, and the one button that opens the right screen. */
    private void card(int title, String text, String button, Runnable action) {
        Ui.add(page, Ui.title(this, getString(title)), 6);
        Ui.add(page, Ui.body(this, text), 8);
        if (button != null) Ui.add(page, Ui.button(this, button, true, v -> action.run()), 18);
    }

    /** Any network of the phone on Wi-Fi: with a VPN on, the active network is the VPN, not the Wi-Fi. */
    @SuppressWarnings("deprecation")
    private boolean wifi() {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        for (android.net.Network net : cm.getAllNetworks()) {
            NetworkCapabilities n = cm.getNetworkCapabilities(net);
            if (n != null && n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true;
        }
        return false;
    }

    /** The command for a computer (lite edition, or when the automatic setup cannot be used). */
    private View commandBox() {
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.shape(this, Ui.soft, 12, Ui.line));
        TextView cmd = Ui.text(this, COMMAND, 11, Ui.ink);
        cmd.setTypeface(Ui.mono);
        cmd.setTextIsSelectable(true);
        cmd.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 48), Ui.dp(this, 12));
        box.addView(cmd);
        box.addView(Ui.copyIcon(this, () -> COMMAND), Ui.copyIconParams(this));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(Ui.body(this, getString(R.string.perm_by_hand)));
        Ui.add(wrap, box, 6);
        return wrap;
    }

    private void appInfo() {
        Ui.open(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }
}
