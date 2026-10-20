package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Single scrolling screen: start and stop, permissions, target to open, gesture, key finder. */
public class MainActivity extends Activity {
    private static final String COMMAND = "pm grant io.github.damienllado.snapkeymapper android.permission.READ_LOGS";
    // Lines likely to be about a key
    private static final Pattern HINT = Pattern.compile(
            "key|snap|button|gpio|trigger|shortcut|quick|hotkey|keycode|key_", Pattern.CASE_INSENSITIVE);
    private static final String[] GESTURES = {"single", "double", "long"};

    private TextView heroState, appName, listen, capture;
    private PhoneView phone;
    private final TextView[] permState = new TextView[3];
    private final View[] permDot = new View[3];
    private final View[] permAction = new View[3];
    private View autoSetup;
    private EditText link, pattern;
    private View linkBox, appBox;
    private CheckBox all;
    private ArrayAdapter<String> adapter;
    private final List<String> captured = new ArrayList<>();
    private Process log;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Ui.init(this);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.bg));
        int bars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        getWindow().getInsetsController().setSystemBarsAppearance(Ui.night ? 0 : bars, bars);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        top.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 32));

        hero(top);
        permissions(top);
        target(top);
        gesture(top);
        finder(top);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.addHeaderView(top, null, false);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convert, android.view.ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convert, parent);
                t.setTextColor(Ui.ink);
                t.setTypeface(Ui.mono);
                t.setTextSize(11);
                t.setPadding(side, Ui.dp(MainActivity.this, 6), side, Ui.dp(MainActivity.this, 6));
                return t;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            pattern.setText(suggestion(adapter.getItem(pos - list.getHeaderViewsCount())));
            save();
            Toast.makeText(this, "Pattern saved, you can start listening", Toast.LENGTH_LONG).show();
        });
        // Android 15+ draws the app under the status bar: shift the content by its height
        list.setClipToPadding(false);
        list.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sys = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(0, sys.top, 0, sys.bottom);
            return insets;
        });
        setContentView(list);

        if (getIntent().getBooleanExtra("restart", false)) startListening();
    }

    /** Dark gradient card: name, state, the big start button and the tilted phone. */
    private void hero(LinearLayout top) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 20);
        card.setPadding(p, p, Ui.dp(this, 8), p);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0B6E50, 0xFF0C3328});
        g.setCornerRadius(Ui.dp(this, 28));
        card.setBackground(g);
        card.setElevation(Ui.dp(this, 4));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        TextView name = Ui.text(this, "SnapKey Mapper", 26, 0xFFF0FFF7);
        name.setTypeface(Ui.display);
        left.addView(name);
        heroState = Ui.text(this, "", 14, 0xFFB6E8D2);
        Ui.add(left, heroState, 4);
        listen = Ui.button(this, "", true, v -> toggleListen());
        listen.setTextColor(0xFF04130C);
        Ui.ripple(listen, Ui.shape(this, 0xFF3BD99A, 16, 0));
        Ui.add(left, listen, 16);
        card.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        phone = new PhoneView(this);
        card.addView(phone, new LinearLayout.LayoutParams(Ui.dp(this, 130), Ui.dp(this, 190)));
        Ui.add(top, card, 0);
    }

    private void permissions(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, "Permissions"));
        Ui.add(card, Ui.body(this, "Three switches, once. The key itself needs nothing else."), 4);
        if (AdbSetup.AVAILABLE) {
            autoSetup = Ui.button(this, "Set up log reading automatically", true, v -> AdbSetup.start(this));
            Ui.add(card, autoSetup, 14);
            Ui.add(card, Ui.body(this, "Join your Wi-Fi, tap the button, then in Developer options turn on Wireless debugging, tap \"Pair device with pairing code\" and type the 6 digits in the notification. On ColorOS, turn on \"Disable system optimisation\" first."), 8);
        }
        permissionRow(card, 0, "Log reading", null, null);
        permissionRow(card, 1, "Display over other apps", "Allow", () -> startActivity(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        permissionRow(card, 2, "Battery unrestricted", "Allow", () -> startActivity(
                new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))));
        Ui.add(card, Ui.body(this, "On ColorOS, also set Battery usage to \"No restrictions\" in the app settings and lock the app in the recent apps."), 10);
        Ui.add(card, Ui.button(this, "Open the app settings", false, v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))), 8);
        Ui.add(card, Ui.body(this, "Log reading by hand, from a computer:"), 14);
        TextView cmd = Ui.text(this, COMMAND, 11, Ui.ink);
        cmd.setTypeface(Ui.mono);
        cmd.setTextIsSelectable(true);
        cmd.setBackground(Ui.shape(this, Ui.soft, 12, Ui.line));
        cmd.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        Ui.add(card, cmd, 6);
        Ui.add(card, Ui.button(this, "Copy the command", false, v -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("command", COMMAND));
            Toast.makeText(this, "Command copied", Toast.LENGTH_SHORT).show();
        }), 8);
        Ui.add(top, card, 14);
    }

    /** One line: coloured dot, name, state, and a button while it is not granted. */
    private void permissionRow(LinearLayout card, int i, String name, String actionLabel, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        permDot[i] = dot;
        row.addView(dot, new LinearLayout.LayoutParams(Ui.dp(this, 12), Ui.dp(this, 12)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView n = Ui.text(this, name, 15, Ui.ink);
        n.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(n);
        permState[i] = Ui.text(this, "", 12, Ui.muted);
        texts.addView(permState[i]);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tp.leftMargin = Ui.dp(this, 12);
        row.addView(texts, tp);
        if (action != null) {
            TextView b = Ui.button(this, actionLabel, false, v -> action.run());
            b.setMinHeight(Ui.dp(this, 38));
            permAction[i] = b;
            row.addView(b);
        }
        Ui.add(card, row, 14);
    }

    private void target(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, "What the key opens"));
        boolean isApp = Prefs.targetType(this).equals("app");
        Ui.add(card, new Ui.Segmented(this, new String[]{"A link", "An app"}, isApp ? 1 : 0, i -> {
            Prefs.targetType(this, i == 1 ? "app" : "link");
            linkBox.setVisibility(i == 1 ? View.GONE : View.VISIBLE);
            appBox.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        }), 12);

        linkBox = new LinearLayout(this);
        ((LinearLayout) linkBox).setOrientation(LinearLayout.VERTICAL);
        Ui.add((LinearLayout) linkBox, Ui.body(this, "A web address, a deep link or an intent: link"), 0);
        link = Ui.add((LinearLayout) linkBox, Ui.field(this, Prefs.link(this), "https://…"), 8);
        Ui.add(card, linkBox, 12);
        linkBox.setVisibility(isApp ? View.GONE : View.VISIBLE);

        appBox = new LinearLayout(this);
        ((LinearLayout) appBox).setOrientation(LinearLayout.VERTICAL);
        appName = Ui.add((LinearLayout) appBox, Ui.text(this, "", 16, Ui.ink), 0);
        Ui.add((LinearLayout) appBox, Ui.button(this, "Choose an app", false, v -> chooseApp()), 8);
        Ui.add(card, appBox, 12);
        appBox.setVisibility(isApp ? View.VISIBLE : View.GONE);

        Ui.add(card, Ui.button(this, "Test the launch", true, v -> {
            save();
            try {
                Prefs.openTarget(this);
            } catch (RuntimeException e) {
                Toast.makeText(this, "Cannot open it: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }), 14);
        Ui.add(top, card, 14);
    }

    private void gesture(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, "Gesture"));
        Ui.add(card, Ui.body(this, "Restart listening after a change. Single press is the fastest; a double press never fires from a press in your pocket."), 4);
        int current = java.util.Arrays.asList(GESTURES).indexOf(Prefs.gesture(this));
        Ui.add(card, new Ui.Segmented(this, new String[]{"One press", "Two presses", "Long press"}, Math.max(current, 0),
                i -> Prefs.gesture(this, GESTURES[i])), 12);
        Ui.add(top, card, 14);
    }

    /** For phones other than the Find X9: shows what the system log says when the key is pressed. */
    private void finder(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, "Another phone? Find your key"));
        Ui.add(card, Ui.body(this, "The Find X9 key is already known. On another phone: start the capture, press the side key 2 or 3 times, stop, then tap the line that comes back at every press."), 4);
        capture = Ui.button(this, "Start capture", true, v -> toggleCapture());
        Ui.add(card, capture, 12);
        all = new CheckBox(this);
        all.setText("Show all lines");
        all.setTextColor(Ui.muted);
        all.setOnCheckedChangeListener((c, checked) -> showCapture());
        Ui.add(card, all, 4);
        Ui.add(card, Ui.button(this, "Save the capture to a file", false, v -> saveCapture()), 4);
        Ui.add(card, Ui.body(this, "Pattern to recognise (part of the line, without numbers that change)"), 14);
        pattern = Ui.add(card, Ui.field(this, Prefs.pattern(this), "KEYCODE_…"), 6);
        Ui.add(top, card, 14);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    @Override
    protected void onDestroy() {
        if (log != null) log.destroy();
        super.onDestroy();
    }

    private void refresh() {
        boolean[] ok = {
                checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED,
                Settings.canDrawOverlays(this),
                getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName())};
        int done = 0;
        for (int i = 0; i < 3; i++) {
            if (ok[i]) done++;
            permDot[i].setBackground(Ui.shape(this, ok[i] ? Ui.accent : 0xFFE0A030, 6, 0));
            permState[i].setText(ok[i] ? "Granted" : "Needed");
            if (permAction[i] != null) permAction[i].setVisibility(ok[i] ? View.GONE : View.VISIBLE);
        }
        if (autoSetup != null) autoSetup.setVisibility(ok[0] ? View.GONE : View.VISIBLE);
        boolean on = ListenService.running;
        heroState.setText(on ? "Listening: press the key" : done == 3 ? "Ready" : "Setup: " + done + " of 3 permissions");
        listen.setText(on ? "Stop listening" : "Start listening");
        phone.setListening(on);
        String pkg = Prefs.appPackage(this);
        appName.setText(pkg.isEmpty() ? "No app chosen" : label(pkg));
    }

    private String label(String pkg) {
        try {
            return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private void chooseApp() {
        List<ResolveInfo> found = getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        found.sort((a, b) -> a.loadLabel(getPackageManager()).toString()
                .compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        String[] names = new String[found.size()];
        for (int i = 0; i < names.length; i++) names[i] = found.get(i).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle("Choose an app").setItems(names, (d, which) -> {
            Prefs.appPackage(this, found.get(which).activityInfo.packageName);
            Prefs.targetType(this, "app");
            recreate();
        }).show();
    }

    private void save() {
        Prefs.save(this, link.getText().toString(), pattern.getText().toString());
    }

    private void toggleCapture() {
        if (log != null) {
            log.destroy();
            log = null;
            capture.setText("Start capture");
            showCapture();
            return;
        }
        synchronized (captured) { captured.clear(); }
        try {
            log = Prefs.readLog(l -> {
                synchronized (captured) {
                    captured.add(l);
                    if (captured.size() > 3000) captured.remove(0);
                }
            }, null);
            capture.setText("Stop capture");
        } catch (IOException e) {
            Toast.makeText(this, "Cannot read the log: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCapture() {
        List<String> lines = new ArrayList<>();
        synchronized (captured) {
            for (String l : captured) if (all.isChecked() || HINT.matcher(l).find()) lines.add(l);
        }
        adapter.clear();
        adapter.addAll(lines);
        if (lines.isEmpty()) Toast.makeText(this, "No line: tick \"Show all lines\" or check the permission", Toast.LENGTH_LONG).show();
    }

    /** Saves the capture to a file picked by the user (cloud storage works): the share sheet crashes above ~500 KB. */
    private void saveCapture() {
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain").putExtra(Intent.EXTRA_TITLE, "snapkey-capture.txt"), 2);
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        if (code != 2 || result != RESULT_OK || data == null) return;
        try (java.io.OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            synchronized (captured) {
                for (String l : captured) out.write((l + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            Toast.makeText(this, "Capture saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleListen() {
        save();
        if (ListenService.running) {
            stopService(new Intent(this, ListenService.class));
            Prefs.active(this, false);
            heroState.postDelayed(this::refresh, 300);
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (Prefs.pattern(this).isEmpty()) {
            Toast.makeText(this, "Choose the pattern first (step 3)", Toast.LENGTH_LONG).show();
            return;
        }
        startForegroundService(new Intent(this, ListenService.class));
        Prefs.active(this, true);
        heroState.postDelayed(this::refresh, 300);
    }

    /** Message of the line (after "): "), cut before the first number that may change between presses. */
    static String suggestion(String line) {
        int i = line.indexOf("): ");
        String m = i >= 0 ? line.substring(i + 3) : line;
        for (int k = 6; k < m.length(); k++) {
            if (Character.isDigit(m.charAt(k))) return m.substring(0, k);
        }
        return m;
    }
}
