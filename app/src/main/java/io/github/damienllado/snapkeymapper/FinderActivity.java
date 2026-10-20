package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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

/** For phones other than the Find X9: records what the system log says when the key is pressed, and keeps the line picked. */
public class FinderActivity extends Activity {
    // Lines likely to be about a key
    private static final Pattern HINT = Pattern.compile(
            "key|snap|button|gpio|trigger|shortcut|quick|hotkey|keycode|key_", Pattern.CASE_INSENSITIVE);
    private TextView capture;
    private CheckBox all;
    private EditText pattern;
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
        LinearLayout root = Ui.page(this);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        top.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 12));
        top.addView(Ui.title(this, getString(R.string.finder_title)));
        Ui.add(top, Ui.body(this, getString(R.string.finder_intro)), 4);
        String[] steps = getResources().getStringArray(R.array.steps_finder);
        for (int k = 0; k < steps.length; k++) Ui.add(top, Ui.body(this, (k + 1) + ".  " + steps[k]), k == 0 ? 12 : 6);
        capture = Ui.button(this, getString(R.string.capture_start), true, v -> toggleCapture());
        Ui.add(top, capture, 14);
        all = new CheckBox(this);
        all.setText(getString(R.string.finder_all));
        all.setTextColor(Ui.muted);
        all.setOnCheckedChangeListener((c, checked) -> showCapture());
        Ui.add(top, all, 4);
        Ui.add(top, Ui.body(this, getString(R.string.finder_pattern)), 10);
        pattern = Ui.add(top, Ui.field(this, Prefs.pattern(this), "KEYCODE_…"), 6);
        Ui.add(top, Ui.button(this, getString(R.string.finder_save), false, v -> saveCapture()), 10);
        Ui.add(top, Ui.body(this, getString(R.string.finder_privacy)), 6);

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
                t.setPadding(side, Ui.dp(FinderActivity.this, 6), side, Ui.dp(FinderActivity.this, 6));
                return t;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            pattern.setText(suggestion(adapter.getItem(pos - list.getHeaderViewsCount())));
            save();
            Toast.makeText(this, getString(R.string.toast_pattern_saved), Toast.LENGTH_LONG).show();
        });
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
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

    private void save() {
        Prefs.save(this, Prefs.link(this), pattern.getText().toString().trim());
    }

    private void toggleCapture() {
        if (log != null) {
            log.destroy();
            log = null;
            capture.setText(getString(R.string.capture_start));
            showCapture();
            return;
        }
        if (checkSelfPermission(Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.finder_need_log), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SetupActivity.class));
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
