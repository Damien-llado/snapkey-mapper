package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** The last 20 presses: when, which gesture, what ran, and why it failed. Kept on the phone, nothing is sent. */
public class HistoryActivity extends Activity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = Ui.page(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        page.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 32));
        page.addView(Ui.title(this, getString(R.string.history_title)));
        Ui.add(page, Ui.body(this, getString(R.string.history_intro)), 6);
        List<History.Entry> all = History.list(this);
        if (all.isEmpty()) Ui.add(page, Ui.body(this, getString(R.string.history_empty)), 20);
        for (History.Entry e : all) Ui.add(page, row(e), 10);
        if (!all.isEmpty()) {
            Ui.add(page, Ui.button(this, getString(R.string.history_clear), false, v -> {
                History.clear(this);
                recreate();
            }), 20);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
    }

    private LinearLayout row(History.Entry e) {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(Ui.stepIcon(this, e.step, 40), new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView what = Ui.text(this, e.step.label(this), 15, Ui.ink);
        what.setMaxLines(2);
        what.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(what);
        String when = DateUtils.formatDateTime(this, e.time, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
        texts.addView(Ui.text(this, when + "  ·  " + Prefs.gestureLabel(this, e.gesture), 12, Ui.muted));
        if (e.error != null) {
            texts.addView(Ui.text(this, "✗ " + e.error, 13, Ui.night ? 0xFFF2B8B5 : 0xFFB3261E));
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tp.leftMargin = Ui.dp(this, 12);
        line.addView(texts, tp);
        if (e.error == null) line.addView(Ui.text(this, "✓", 18, Ui.accent));
        card.addView(line);
        return card;
    }
}
