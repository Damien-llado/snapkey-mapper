package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Installed apps with their icon and name, and a search field: a tap makes the key open that app. */
public class AppPickerActivity extends Activity {
    private final List<ResolveInfo> all = new ArrayList<>(), shown = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        PackageManager pm = getPackageManager();
        all.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0));
        all.removeIf(r -> r.activityInfo.packageName.equals(getPackageName()));
        all.sort((x, y) -> x.loadLabel(pm).toString().compareToIgnoreCase(y.loadLabel(pm).toString()));
        for (ResolveInfo r : all) labels.add(r.loadLabel(pm).toString());
        shown.addAll(all);

        LinearLayout root = Ui.page(this);
        int side = Ui.dp(this, 16);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 8));
        head.addView(Ui.title(this, getString(R.string.app_choose)));
        EditText search = Ui.field(this, "", getString(R.string.app_search));
        search.setTypeface(android.graphics.Typeface.DEFAULT);
        search.setSingleLine();
        Ui.add(head, search, 12);
        root.addView(head);

        ListView list = new ListView(this);
        list.setDivider(null);
        BaseAdapter adapter = new BaseAdapter() {
            public int getCount() { return shown.size(); }
            public Object getItem(int i) { return shown.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View convert, ViewGroup parent) {
                LinearLayout row = (LinearLayout) convert;
                if (row == null) {
                    row = new LinearLayout(AppPickerActivity.this);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(side, Ui.dp(AppPickerActivity.this, 10), side, Ui.dp(AppPickerActivity.this, 10));
                    row.addView(new ImageView(AppPickerActivity.this), new LinearLayout.LayoutParams(Ui.dp(AppPickerActivity.this, 40), Ui.dp(AppPickerActivity.this, 40)));
                    TextView t = Ui.text(AppPickerActivity.this, "", 16, Ui.ink);
                    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    tp.leftMargin = Ui.dp(AppPickerActivity.this, 14);
                    row.addView(t, tp);
                }
                ResolveInfo r = shown.get(i);
                ((ImageView) row.getChildAt(0)).setImageDrawable(r.loadIcon(pm));
                ((TextView) row.getChildAt(1)).setText(labels.get(all.indexOf(r)));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, i, id) -> {
            Prefs.appPackage(this, shown.get(i).activityInfo.packageName);
            Prefs.targetType(this, "app");
            setResult(RESULT_OK);
            finish();
        });
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            public void onTextChanged(CharSequence s, int a, int c, int d) { }
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                for (int i = 0; i < all.size(); i++) if (labels.get(i).toLowerCase(Locale.ROOT).contains(q)) shown.add(all.get(i));
                adapter.notifyDataSetChanged();
            }
        });
        setContentView(root);
        Ui.barsAppearance(this);
        root.setFocusableInTouchMode(true); // no keyboard on opening
        root.requestFocus();
    }
}
