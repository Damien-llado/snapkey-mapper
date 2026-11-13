package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;

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
        LinearLayout find = Ui.card(this);
        find.addView(Ui.body(this, getString(R.string.report_find_text)));
        Ui.add(find, Ui.button(this, getString(R.string.report_find), true,
                v -> startActivity(new Intent(this, FinderActivity.class))), 14);
        Ui.add(page, find, 14);
        LinearLayout fix = Ui.card(this);
        fix.addView(Ui.body(this, getString(R.string.report_fix_text)));
        Ui.add(fix, Ui.button(this, getString(R.string.report_fix), false, v -> Diagnostic.askFix(this)), 14);
        Ui.add(page, fix, 14);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
    }
}
