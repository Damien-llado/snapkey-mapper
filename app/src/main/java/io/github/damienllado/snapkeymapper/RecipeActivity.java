package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** One recipe: a field per value to fill in, the final link as it builds up, and the buttons to test or use it. */
public class RecipeActivity extends Activity {
    private Recipes.Recipe recipe;
    private final List<EditText> fields = new ArrayList<>();
    private TextView preview;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        recipe = Recipes.find(this, getIntent().getStringExtra("recipe"));
        if (recipe == null) {
            finish();
            return;
        }
        LinearLayout root = Ui.page(this);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        top.setPadding(side, Ui.dp(this, 12), side, Ui.dp(this, 32));

        LinearLayout app = new LinearLayout(this);
        app.setGravity(Gravity.CENTER_VERTICAL);
        app.addView(Ui.logo(this, recipe.app, 36), new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
        TextView appName = Ui.text(this, recipe.app.name, 15, Ui.muted);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        np.leftMargin = Ui.dp(this, 10);
        app.addView(appName, np);
        top.addView(app);
        TextView title = Ui.text(this, recipe.title, 26, Ui.ink);
        title.setTypeface(Ui.display);
        Ui.add(top, title, 14);
        Ui.add(top, Ui.body(this, recipe.desc), 6);

        if (!recipe.vars.isEmpty()) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.title(this, getString(R.string.recipe_fields)));
            for (Recipes.Var v : recipe.vars) {
                TextView label = Ui.text(this, v.label, 14, Ui.ink);
                label.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                Ui.add(card, label, 14);
                Ui.add(card, Ui.body(this, v.where), 2);
                EditText f = Ui.field(this, "", v.example);
                f.setSingleLine();
                f.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
                    public void onTextChanged(CharSequence s, int a, int c, int d) { }
                    public void afterTextChanged(Editable s) { updatePreview(); }
                });
                fields.add(f);
                Ui.add(card, f, 6);
            }
            Ui.add(top, card, 16);
        }

        LinearLayout linkCard = Ui.card(this);
        linkCard.addView(Ui.title(this, getString(R.string.recipe_link)));
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.shape(this, Ui.soft, 12, Ui.line));
        preview = Ui.text(this, "", 11, Ui.ink);
        preview.setTypeface(Ui.mono);
        preview.setTextIsSelectable(true);
        preview.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 48), Ui.dp(this, 12));
        box.addView(preview);
        box.addView(Ui.copyIcon(this, this::filled), Ui.copyIconParams(this));
        Ui.add(linkCard, box, 10);
        Ui.add(top, linkCard, 14);

        Ui.add(top, Ui.button(this, getString(R.string.target_test), false, v -> test()), 16);
        Ui.add(top, Ui.button(this, getString(R.string.recipe_use), true, v -> use()), 10);
        if (!recipe.note.isEmpty()) Ui.add(top, Ui.body(this, recipe.note), 16);
        if (!recipe.source.isEmpty()) {
            TextView source = Ui.text(this, getString(R.string.recipe_source), 14, Ui.accent);
            source.setPaintFlags(source.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            source.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            source.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(recipe.source))));
            Ui.add(top, source, 8);
        }
        if (!recipe.tested.isEmpty()) Ui.add(top, Ui.body(this, getString(recipe.level.equals("dispatch") ? R.string.recipe_verified_partial : R.string.recipe_verified, recipe.tested)), recipe.note.isEmpty() ? 16 : 8);
        else Ui.add(top, Ui.body(this, getString(R.string.recipe_untested)), recipe.note.isEmpty() ? 16 : 8);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(top);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
        updatePreview();
    }

    private List<String> values() {
        List<String> out = new ArrayList<>();
        for (EditText f : fields) out.add(f.getText().toString());
        return out;
    }

    private void updatePreview() {
        preview.setText(Recipes.preview(recipe, values()));
    }

    /** The final link, or null (with a message and the cursor on the first empty field) while a value is missing. */
    private String filled() {
        String link = Recipes.fill(recipe, values());
        if (link == null) {
            Toast.makeText(this, getString(R.string.recipe_fill_all), Toast.LENGTH_SHORT).show();
            for (EditText f : fields) {
                if (f.getText().toString().trim().isEmpty()) { f.requestFocus(); break; }
            }
        }
        return link;
    }

    private void test() {
        String link = filled();
        if (link == null) return;
        try {
            Prefs.open(this, link);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.recipe_missing), Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, getString(R.string.toast_cannot_open, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void use() {
        String link = filled();
        if (link == null) return;
        Prefs.targetType(this, "link");
        Prefs.save(this, link, Prefs.pattern(this));
        Toast.makeText(this, getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
