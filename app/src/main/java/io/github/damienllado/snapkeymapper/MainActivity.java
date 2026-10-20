package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Single screen: permissions, target to open, side key detection in the log, start and stop. */
public class MainActivity extends Activity {
    private static final String COMMAND = "pm grant io.github.damienllado.snapkeymapper android.permission.READ_LOGS";
    // Lines likely to be about a key
    private static final Pattern HINT = Pattern.compile(
            "key|snap|button|gpio|trigger|shortcut|quick|hotkey|keycode|key_", Pattern.CASE_INSENSITIVE);

    private TextView status, appName;
    private EditText link, pattern;
    private Button capture, listen;
    private CheckBox all;
    private ArrayAdapter<String> adapter;
    private final List<String> captured = new ArrayList<>();
    private Process log;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        top.setPadding(margin, margin, margin, margin);

        title(top, "SnapKey Mapper");
        status = text(top, "");

        title(top, "1. Permissions (once)");
        if (AdbSetup.AVAILABLE) {
            text(top, "Log reading, automatic: join your Wi-Fi, tap the button, then in Developer options turn on Wireless debugging and tap \"Pair device with pairing code\". Pull the notification down and type the 6 digits. On ColorOS, turn on \"Disable system optimisation\" first.");
            button(top, "Set up log reading automatically", v -> AdbSetup.start(this));
            text(top, "Or by hand:");
        }
        text(top, "Log reading: run this command once (for example with Key Mapper: \"Shell command\" action, ADB execution mode, Test button), or from a computer with adb:");
        TextView cmd = text(top, COMMAND);
        cmd.setTextIsSelectable(true);
        button(top, "Copy the command", v -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("command", COMMAND));
            Toast.makeText(this, "Command copied", Toast.LENGTH_SHORT).show();
        });
        button(top, "Allow display over other apps", v -> startActivity(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        button(top, "Battery: do not restrict", v -> startActivity(
                new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))));
        text(top, "On some skins (ColorOS), also open the app settings, Battery usage, and choose \"No restrictions\" or \"Allow background activity\". Then lock the app in the recent apps list.");
        button(top, "Open the app settings", v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));

        title(top, "2. What the side key opens");
        RadioGroup targets = new RadioGroup(this);
        RadioButton asLink = new RadioButton(this), asApp = new RadioButton(this);
        asLink.setText("A link (web address, deep link or intent: link)");
        asApp.setText("An installed app");
        asLink.setChecked(!Prefs.targetType(this).equals("app"));
        asApp.setChecked(Prefs.targetType(this).equals("app"));
        asLink.setOnClickListener(v -> Prefs.targetType(this, "link"));
        asApp.setOnClickListener(v -> Prefs.targetType(this, "app"));
        targets.addView(asLink);
        targets.addView(asApp);
        top.addView(targets);
        text(top, "Link to open");
        link = field(top, Prefs.link(this));
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        appName = text(top, "");
        button(top, "Choose an app", v -> chooseApp());
        button(top, "Test the launch", v -> {
            save();
            try {
                Prefs.openTarget(this);
            } catch (RuntimeException e) {
                Toast.makeText(this, "Cannot open it: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        title(top, "3. Find the side key in the log");
        text(top, "Start the capture (accept the log access prompt if Android shows one), press the side key 2 or 3 times, stop. Then tap the line that comes back at every press.");
        capture = button(top, "Start capture", v -> toggleCapture());
        all = new CheckBox(this);
        all.setText("Show all lines (otherwise only lines that mention a key)");
        all.setOnCheckedChangeListener((c, checked) -> showCapture());
        top.addView(all);
        button(top, "Save the capture to a file", v -> saveCapture());
        text(top, "Pattern to recognise (part of the line, without numbers that change)");
        pattern = field(top, Prefs.pattern(this));

        text(top, "Gesture that opens the target (restart listening after a change)");
        RadioGroup gestures = new RadioGroup(this);
        String[][] choices = {{"single", "One press"}, {"double", "Two quick presses"}, {"long", "Long press"}};
        for (String[] c : choices) {
            RadioButton r = new RadioButton(this);
            r.setText(c[1]);
            r.setChecked(c[0].equals(Prefs.gesture(this)));
            r.setOnClickListener(v -> Prefs.gesture(this, c[0]));
            gestures.addView(r);
        }
        top.addView(gestures);

        title(top, "4. Listening");
        listen = button(top, "", v -> toggleListen());

        ListView list = new ListView(this);
        list.addHeaderView(top, null, false);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            pattern.setText(suggestion(adapter.getItem(pos - list.getHeaderViewsCount())));
            save();
            Toast.makeText(this, "Pattern saved, you can start listening", Toast.LENGTH_LONG).show();
        });
        // Android 15+ draws the app under the status bar: shift the content by its height
        list.setClipToPadding(false);
        list.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        setContentView(list);

        if (getIntent().getBooleanExtra("restart", false)) startListening();
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
        boolean logs = checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED;
        boolean overlay = Settings.canDrawOverlays(this);
        boolean battery = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        status.setText("Log reading: " + (logs ? "OK" : "NO, run the command")
                + "\nDisplay over other apps: " + (overlay ? "OK" : "NO")
                + "\nBattery unrestricted: " + (battery ? "OK" : "NO")
                + "\nListening: " + (ListenService.running ? "ACTIVE" : "stopped"));
        listen.setText(ListenService.running ? "Stop listening" : "Start listening");
        String pkg = Prefs.appPackage(this);
        appName.setText("App: " + (pkg.isEmpty() ? "none chosen" : label(pkg)));
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
            status.postDelayed(this::refresh, 300);
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
        status.postDelayed(this::refresh, 300);
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

    private TextView text(LinearLayout parent, String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setPadding(0, 8, 0, 8);
        parent.addView(v);
        return v;
    }

    private void title(LinearLayout parent, String t) {
        TextView v = text(parent, t);
        v.setTextSize(18);
        v.setPadding(0, 32, 0, 8);
    }

    private EditText field(LinearLayout parent, String value) {
        EditText e = new EditText(this);
        e.setText(value);
        parent.addView(e);
        return e;
    }

    private Button button(LinearLayout parent, String t, View.OnClickListener action) {
        Button b = new Button(this);
        b.setText(t);
        b.setOnClickListener(action);
        parent.addView(b);
        return b;
    }
}
