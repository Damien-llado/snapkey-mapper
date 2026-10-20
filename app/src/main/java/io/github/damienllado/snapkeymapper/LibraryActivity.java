package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The recipe library: apps with their logo, a search field, and the recipes of each app one tap away. */
public class LibraryActivity extends Activity {
    private LinearLayout list;
    private final List<String> opened = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = Ui.page(this);
        int side = Ui.dp(this, 16);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 8));
        head.addView(Ui.title(this, getString(R.string.library_title)));
        Ui.add(head, Ui.body(this, getString(R.string.library_intro)), 4);
        EditText search = Ui.field(this, "", getString(R.string.library_search));
        search.setTypeface(Typeface.DEFAULT);
        search.setSingleLine();
        Ui.add(head, search, 12);
        root.addView(head);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(side, Ui.dp(this, 4), side, Ui.dp(this, 32));
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
        root.setFocusableInTouchMode(true); // the search field must not grab the focus (and the keyboard) on opening
        root.requestFocus();

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            public void onTextChanged(CharSequence s, int a, int c, int d) { }
            public void afterTextChanged(Editable s) { show(s.toString().trim().toLowerCase(Locale.ROOT)); }
        });
        show("");
    }

    private void show(String query) {
        list.removeAllViews();
        boolean any = false;
        for (Recipes.App app : Recipes.load(this)) {
            List<Recipes.Recipe> matches = new ArrayList<>();
            boolean appHit = app.name.toLowerCase(Locale.ROOT).contains(query);
            for (Recipes.Recipe r : app.recipes) {
                if (query.isEmpty() || appHit || r.title.toLowerCase(Locale.ROOT).contains(query) || r.desc.toLowerCase(Locale.ROOT).contains(query)) matches.add(r);
            }
            if (matches.isEmpty()) continue;
            any = true;
            Ui.add(list, group(app, matches, !query.isEmpty()), 12);
        }
        if (!any) Ui.add(list, Ui.body(this, getString(R.string.library_empty)), 24);
    }

    /** One app: logo, name and count; a tap unfolds its recipes (they are all unfolded while searching). */
    private LinearLayout group(Recipes.App app, List<Recipes.Recipe> recipes, boolean searching) {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.logo(this, app, 44), new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = Ui.text(this, app.name, 17, Ui.ink);
        name.setTypeface(Ui.display);
        texts.addView(name);
        boolean missing = !app.installed(this);
        TextView sub = Ui.text(this, getString(R.string.library_count, app.recipes.size())
                + (missing ? "  ·  " + getString(R.string.library_not_installed) : ""), 12, Ui.muted);
        texts.addView(sub);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tp.leftMargin = Ui.dp(this, 12);
        row.addView(texts, tp);
        TextView arrow = Ui.text(this, "▾", 18, Ui.muted);
        row.addView(arrow);
        card.addView(row);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        for (Recipes.Recipe r : recipes) body.addView(recipeRow(r));
        Ui.add(card, body, 8);
        boolean open = searching || opened.contains(app.id);
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        arrow.setText(open ? "▴" : "▾");
        row.setOnClickListener(v -> {
            boolean now = body.getVisibility() != View.VISIBLE;
            body.setVisibility(now ? View.VISIBLE : View.GONE);
            arrow.setText(now ? "▴" : "▾");
            if (now) opened.add(app.id); else opened.remove(app.id);
        });
        if (missing) {
            name.setTextColor(Ui.muted);
            row.getChildAt(0).setAlpha(0.6f);
        }
        return card;
    }

    private View recipeRow(Recipes.Recipe r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12));
        View line = new View(this);
        line.setBackgroundColor(Ui.line);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        TextView t = Ui.text(this, r.title, 15, Ui.ink);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        row.addView(t);
        Ui.add(row, Ui.body(this, r.desc), 2);
        Ui.ripple(row, Ui.shape(this, 0, 8, 0));
        row.setOnClickListener(v -> startActivityForResult(new Intent(this, RecipeActivity.class).putExtra("recipe", r.id), 4));
        wrap.addView(row);
        return wrap;
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        if (code == 4 && result == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
