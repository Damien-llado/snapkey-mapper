package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** The four setup steps, one card each: what to do as a short numbered list, one button, and a state that checks itself. */
public class SetupActivity extends Activity {
    static final int TOTAL = 4;
    private static final String COMMAND = "adb shell pm grant io.github.damienllado.snapkeymapper android.permission.READ_LOGS";
    private final TextView[] state = new TextView[TOTAL];
    private final TextView[] mark = new TextView[TOTAL];
    private final LinearLayout[] cards = new LinearLayout[TOTAL];

    /** Which steps are done: log reading, display over other apps, battery, native Snap Key action off (ticked by the user). */
    static boolean[] steps(Context c) {
        return new boolean[]{
                c.checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED,
                Settings.canDrawOverlays(c),
                c.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(c.getPackageName()),
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
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        top.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 32));
        top.addView(Ui.title(this, getString(R.string.setup_title)));
        Ui.add(top, Ui.body(this, getString(R.string.setup_intro)), 4);

        String pkg = "package:" + getPackageName();
        LinearLayout log = step(top, 0, R.string.perm_log, AdbSetup.AVAILABLE ? R.array.steps_log_auto : R.array.steps_log_computer);
        if (AdbSetup.AVAILABLE) {
            Ui.add(log, Ui.button(this, getString(R.string.perm_auto_btn), true, v -> AdbSetup.start(this)), 12);
            TextView other = Ui.button(this, getString(R.string.setup_other_way), false, null);
            View box = commandBox();
            box.setVisibility(View.GONE);
            other.setOnClickListener(v -> box.setVisibility(box.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            Ui.add(log, other, 8);
            Ui.add(log, box, 8);
        } else {
            Ui.add(log, commandBox(), 10);
        }

        LinearLayout overlay = step(top, 1, R.string.perm_overlay, R.array.steps_overlay);
        Ui.add(overlay, Ui.button(this, getString(R.string.setup_open_setting), true, v -> startActivity(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(pkg)))), 12);
        Ui.add(overlay, Ui.button(this, getString(R.string.perm_app_settings), false, v -> appInfo()), 8);

        LinearLayout battery = step(top, 2, R.string.perm_battery, R.array.steps_battery);
        Ui.add(battery, Ui.button(this, getString(R.string.perm_allow), true, v -> startActivity(
                new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse(pkg)))), 12);
        Ui.add(battery, Ui.button(this, getString(R.string.perm_app_settings), false, v -> appInfo()), 8);

        LinearLayout key = step(top, 3, R.string.setup_native_title, R.array.steps_native);
        Ui.add(key, Ui.button(this, getString(R.string.setup_open_phone_settings), false,
                v -> startActivity(new Intent(Settings.ACTION_SETTINGS))), 12);
        CheckBox ok = new CheckBox(this);
        ok.setText(getString(R.string.setup_native_done));
        ok.setTextColor(Ui.ink);
        ok.setChecked(Prefs.nativeKeyOff(this));
        ok.setOnCheckedChangeListener((c, checked) -> { Prefs.nativeKeyOff(this, checked); refresh(); });
        Ui.add(key, ok, 8);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(top);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
    }

    /** One card: done mark and number, title, state, then the numbered instructions. */
    private LinearLayout step(LinearLayout top, int i, int title, int lines) {
        LinearLayout card = Ui.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView n = Ui.text(this, String.valueOf(i + 1), 15, Ui.accentInk);
        n.setGravity(Gravity.CENTER);
        n.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mark[i] = n;
        head.addView(n, new LinearLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.text(this, getString(title), 16, Ui.ink);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t);
        state[i] = Ui.text(this, "", 13, Ui.muted);
        texts.addView(state[i]);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tp.leftMargin = Ui.dp(this, 12);
        head.addView(texts, tp);
        card.addView(head);
        String[] items = getResources().getStringArray(lines);
        for (int k = 0; k < items.length; k++) {
            TextView line = Ui.body(this, (k + 1) + ".  " + items[k]);
            line.setPadding(Ui.dp(this, 42), 0, 0, 0);
            Ui.add(card, line, k == 0 ? 12 : 6);
        }
        Ui.add(top, card, 14);
        cards[i] = card;
        return card;
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
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean[] ok = steps(this);
        for (int i = 0; i < TOTAL; i++) {
            mark[i].setText(ok[i] ? "✓" : String.valueOf(i + 1));
            mark[i].setBackground(Ui.shape(this, ok[i] ? Ui.accent : 0xFFE0A030, 15, 0));
            state[i].setText(ok[i] ? getString(R.string.perm_granted) : getString(R.string.perm_needed));
            // a done step folds its instructions away (the last step keeps its box, to untick it)
            int keep = i == TOTAL - 1 ? cards[i].getChildCount() - 1 : cards[i].getChildCount();
            for (int k = 1; k < keep; k++) cards[i].getChildAt(k).setVisibility(ok[i] ? View.GONE : View.VISIBLE);
        }
    }
}
