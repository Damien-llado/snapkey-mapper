package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private Ui.Segmented targetChoice;
    private CheckBox all;
    private ArrayAdapter<String> adapter;
    private final List<String> captured = new ArrayList<>();
    private Process log;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Ui.init(this);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.bg));

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
            Toast.makeText(this, getString(R.string.toast_pattern_saved), Toast.LENGTH_LONG).show();
        });
        // Android 15+ draws the app under the status bar: shift the content by its height
        list.setClipToPadding(true);
        list.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sys = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(0, sys.top, 0, sys.bottom);
            return insets;
        });
        setContentView(list);
        int bars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        getWindow().getInsetsController().setSystemBarsAppearance(Ui.night ? 0 : bars, bars);

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
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(Ui.dp(this, 120), Ui.dp(this, 170));
        pp.topMargin = Ui.dp(this, 26);
        card.addView(phone, pp);
        FrameLayout frame = new FrameLayout(this);
        frame.addView(card);
        TextView lang = Ui.text(this, Lang.label(this) + " \u25BE", 13, 0xFFF0FFF7);
        lang.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        lang.setPadding(Ui.dp(this, 12), Ui.dp(this, 7), Ui.dp(this, 12), Ui.dp(this, 7));
        Ui.ripple(lang, Ui.shape(this, 0x33FFFFFF, 14, 0x44FFFFFF));
        lang.setElevation(Ui.dp(this, 8));
        lang.setContentDescription(getString(R.string.lang_title));
        lang.setOnClickListener(v -> chooseLanguage());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lp.setMargins(0, Ui.dp(this, 14), Ui.dp(this, 14), 0);
        frame.addView(lang, lp);
        Ui.add(top, frame, 0);
    }

    private void permissions(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.perm_title)));
        Ui.add(card, Ui.body(this, getString(R.string.perm_sub)), 4);
        if (AdbSetup.AVAILABLE) {
            autoSetup = Ui.button(this, getString(R.string.perm_auto_btn), true, v -> AdbSetup.start(this));
            Ui.add(card, autoSetup, 14);
            Ui.add(card, Ui.body(this, getString(R.string.perm_auto_help)), 8);
        }
        permissionRow(card, 0, getString(R.string.perm_log), null, null);
        permissionRow(card, 1, getString(R.string.perm_overlay), getString(R.string.perm_allow), () -> startActivity(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        permissionRow(card, 2, getString(R.string.perm_battery), getString(R.string.perm_allow), () -> startActivity(
                new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))));
        Ui.add(card, Ui.body(this, getString(R.string.perm_coloros)), 10);
        Ui.add(card, Ui.button(this, getString(R.string.perm_app_settings), false, v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))), 8);
        Ui.add(card, Ui.body(this, getString(R.string.perm_by_hand)), 14);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.shape(this, Ui.soft, 12, Ui.line));
        TextView cmd = Ui.text(this, COMMAND, 11, Ui.ink);
        cmd.setTypeface(Ui.mono);
        cmd.setTextIsSelectable(true);
        cmd.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 48), Ui.dp(this, 12));
        box.addView(cmd);
        box.addView(Ui.copyIcon(this, () -> COMMAND), Ui.copyIconParams(this));
        Ui.add(card, box, 6);
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
        card.addView(Ui.title(this, getString(R.string.target_title)));
        boolean isApp = Prefs.targetType(this).equals("app");
        Ui.add(card, Ui.button(this, getString(R.string.library_open), false,
                v -> startActivityForResult(new Intent(this, LibraryActivity.class), 3)), 12);
        targetChoice = new Ui.Segmented(this, new String[]{getString(R.string.target_link), getString(R.string.target_app)}, isApp ? 1 : 0, i -> {
            Prefs.targetType(this, i == 1 ? "app" : "link");
            linkBox.setVisibility(i == 1 ? View.GONE : View.VISIBLE);
            appBox.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        });
        Ui.add(card, targetChoice, 12);

        linkBox = new LinearLayout(this);
        ((LinearLayout) linkBox).setOrientation(LinearLayout.VERTICAL);
        Ui.add((LinearLayout) linkBox, Ui.body(this, getString(R.string.target_link_help)), 0);
        link = Ui.add((LinearLayout) linkBox, Ui.field(this, Prefs.link(this), "https://…"), 8);
        Ui.add(card, linkBox, 12);
        linkBox.setVisibility(isApp ? View.GONE : View.VISIBLE);

        appBox = new LinearLayout(this);
        ((LinearLayout) appBox).setOrientation(LinearLayout.VERTICAL);
        appName = Ui.add((LinearLayout) appBox, Ui.text(this, "", 16, Ui.ink), 0);
        Ui.add((LinearLayout) appBox, Ui.button(this, getString(R.string.app_choose), false, v -> chooseApp()), 8);
        Ui.add(card, appBox, 12);
        appBox.setVisibility(isApp ? View.VISIBLE : View.GONE);

        Ui.add(card, Ui.button(this, getString(R.string.target_test), true, v -> {
            save();
            try {
                Prefs.openTarget(this);
            } catch (RuntimeException e) {
                Toast.makeText(this, getString(R.string.toast_cannot_open, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }), 14);
        Ui.add(top, card, 14);
    }

    private void gesture(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.gesture_title)));
        Ui.add(card, Ui.body(this, getString(R.string.gesture_help)), 4);
        int current = java.util.Arrays.asList(GESTURES).indexOf(Prefs.gesture(this));
        Ui.add(card, new Ui.Segmented(this, new String[]{getString(R.string.gesture_single), getString(R.string.gesture_double), getString(R.string.gesture_long)}, Math.max(current, 0),
                i -> Prefs.gesture(this, GESTURES[i])), 12);
        Ui.add(top, card, 14);
    }

    /** For phones other than the Find X9: shows what the system log says when the key is pressed. */
    private void finder(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.finder_title)));
        Ui.add(card, Ui.body(this, getString(R.string.finder_help)), 4);
        capture = Ui.button(this, getString(R.string.capture_start), true, v -> toggleCapture());
        Ui.add(card, capture, 12);
        all = new CheckBox(this);
        all.setText(getString(R.string.finder_all));
        all.setTextColor(Ui.muted);
        all.setOnCheckedChangeListener((c, checked) -> showCapture());
        Ui.add(card, all, 4);
        Ui.add(card, Ui.button(this, getString(R.string.finder_save), false, v -> saveCapture()), 4);
        Ui.add(card, Ui.body(this, getString(R.string.finder_pattern)), 14);
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
            permState[i].setText(ok[i] ? getString(R.string.perm_granted) : getString(R.string.perm_needed));
            if (permAction[i] != null) permAction[i].setVisibility(ok[i] ? View.GONE : View.VISIBLE);
        }
        if (autoSetup != null) autoSetup.setVisibility(ok[0] ? View.GONE : View.VISIBLE);
        boolean on = ListenService.running;
        heroState.setText(on ? getString(R.string.state_listening) : done == 3 ? getString(R.string.state_ready) : getString(R.string.state_setup, done));
        listen.setText(on ? getString(R.string.btn_stop) : getString(R.string.btn_start));
        phone.setListening(on);
        String pkg = Prefs.appPackage(this);
        appName.setText(pkg.isEmpty() ? getString(R.string.app_none) : label(pkg));
    }

    private String label(String pkg) {
        try {
            return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /** List of languages with their code and their name in that language. */
    private void chooseLanguage() {
        String[] names = new String[Lang.ALL.length];
        for (int i = 0; i < names.length; i++) names[i] = Lang.ALL[i][1] + "   " + Lang.ALL[i][2];
        new AlertDialog.Builder(this).setTitle(R.string.lang_title).setItems(names, (d, which) -> {
            Lang.set(this, Lang.ALL[which][0]);
            recreate();
        }).show();
    }

    private void chooseApp() {
        List<ResolveInfo> found = getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        found.sort((a, b) -> a.loadLabel(getPackageManager()).toString()
                .compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        String[] names = new String[found.size()];
        for (int i = 0; i < names.length; i++) names[i] = found.get(i).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle(getString(R.string.app_choose)).setItems(names, (d, which) -> {
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
            capture.setText(getString(R.string.capture_start));
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
            capture.setText(getString(R.string.capture_stop));
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.toast_cannot_read, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void showCapture() {
        List<String> lines = new ArrayList<>();
        synchronized (captured) {
            for (String l : captured) if (all.isChecked() || HINT.matcher(l).find()) lines.add(l);
        }
        adapter.clear();
        adapter.addAll(lines);
        if (lines.isEmpty()) Toast.makeText(this, getString(R.string.toast_no_lines), Toast.LENGTH_LONG).show();
    }

    /** Saves the capture to a file picked by the user (cloud storage works): the share sheet crashes above ~500 KB. */
    private void saveCapture() {
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain").putExtra(Intent.EXTRA_TITLE, "snapkey-capture.txt"), 2);
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        if (code == 3 && result == RESULT_OK) {
            // a recipe was chosen in the library: show it as the link target
            link.setText(Prefs.link(this));
            targetChoice.select(0);
            linkBox.setVisibility(View.VISIBLE);
            appBox.setVisibility(View.GONE);
            return;
        }
        if (code != 2 || result != RESULT_OK || data == null) return;
        try (java.io.OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            synchronized (captured) {
                for (String l : captured) out.write((l + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            Toast.makeText(this, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.toast_failed, e.getMessage()), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, getString(R.string.toast_choose_pattern), Toast.LENGTH_LONG).show();
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
