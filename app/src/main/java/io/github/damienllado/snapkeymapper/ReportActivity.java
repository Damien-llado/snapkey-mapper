package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** "Doesn't work on my phone?": find the key yourself, or open an issue with the diagnostic. */
public class ReportActivity extends Activity {
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
        page.addView(Ui.title(this, getString(R.string.report_entry)));
        // the usual fixes come first, a report last: most "it stopped working" need one tap, not an issue
        LinearLayout quick = Ui.card(this);
        quick.addView(Ui.body(this, getString(R.string.report_quick_text)));
        Ui.add(quick, Ui.button(this, getString(R.string.listen_restart), true, v -> restart()), 14);
        // a line picked in the key finder replaced the pattern that works on OPPO and OnePlus
        if (!Prefs.pattern(this).equals(Prefs.DEFAULT_PATTERN)) {
            Ui.add(quick, Ui.body(this, getString(R.string.report_reset_text, Prefs.pattern(this))), 16);
            Ui.add(quick, Ui.button(this, getString(R.string.report_reset), false, v -> {
                Prefs.save(this, Prefs.link(this), Prefs.DEFAULT_PATTERN);
                restart();
            }), 10);
        }
        Ui.add(page, quick, 14);
        LinearLayout find = Ui.card(this);
        find.addView(Ui.body(this, getString(R.string.report_find_text)));
        Ui.add(find, Ui.button(this, getString(R.string.report_find), true,
                v -> startActivity(new Intent(this, FinderActivity.class))), 14);
        Ui.add(page, find, 14);
        LinearLayout fix = Ui.card(this);
        TextView last = Ui.body(this, getString(R.string.report_fix_first));
        last.setTypeface(last.getTypeface(), android.graphics.Typeface.BOLD);
        fix.addView(last);
        Ui.add(fix, Ui.body(this, getString(R.string.report_fix_text)), 4);
        Ui.add(fix, Ui.button(this, getString(R.string.report_fix), false, v -> Diagnostic.askFix(this)), 14);
        Ui.add(page, fix, 14);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
    }

    /** Back to the home screen, which shows the listener running: the key can be tried at once. */
    private void restart() {
        ListenService.restart(this, () -> {
            Toast.makeText(this, getString(R.string.report_restarted), Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
