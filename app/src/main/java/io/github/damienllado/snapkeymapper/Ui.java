package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Look of the app: the same palette, rounded cards and display font as the website. */
final class Ui {
    static boolean night;
    static int bg, card, ink, muted, line, accent, accentInk, soft;
    static Typeface display, mono;

    static void init(Context c) {
        night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        bg = night ? 0xFF090E0C : 0xFFF5F8F4;
        card = night ? 0xFF111915 : 0xFFFFFFFF;
        ink = night ? 0xFFE7F0EA : 0xFF0E1914;
        muted = night ? 0xFF97A99F : 0xFF56665E;
        line = night ? 0xFF212E28 : 0xFFDBE4DD;
        accent = night ? 0xFF3BD99A : 0xFF0A8A63;
        accentInk = night ? 0xFF04130C : 0xFFFFFFFF;
        soft = night ? 0xFF18221D : 0xFFEDF2EE;
        display = new Typeface.Builder(c.getAssets(), "bricolage.ttf").setFontVariationSettings("'wght' 700").build();
        mono = Typeface.MONOSPACE;
    }

    static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    static GradientDrawable shape(Context c, int fill, float radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(dp(c, 1), stroke);
        return g;
    }

    static void ripple(View v, GradientDrawable bgShape) {
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33808080), bgShape, null));
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 18);
        l.setPadding(p, p, p, p);
        l.setBackground(shape(c, card, 24, line));
        l.setElevation(dp(c, 2));
        return l;
    }

    static TextView title(Context c, String t) {
        TextView v = text(c, t, 20, ink);
        v.setTypeface(display);
        return v;
    }

    static TextView body(Context c, String t) {
        TextView v = text(c, t, 14, muted);
        v.setLineSpacing(0, 1.15f);
        return v;
    }

    static TextView text(Context c, String t, float sp, int color) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    static TextView button(Context c, String t, boolean filled, View.OnClickListener l) {
        TextView b = text(c, t, 15, filled ? accentInk : ink);
        b.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(c, 48));
        b.setPadding(dp(c, 18), 0, dp(c, 18), 0);
        ripple(b, shape(c, filled ? accent : 0, 16, filled ? 0 : line));
        b.setOnClickListener(l);
        return b;
    }

    static EditText field(Context c, String value, String hint) {
        EditText e = new EditText(c);
        e.setText(value);
        e.setHint(hint);
        e.setTextSize(14);
        e.setTextColor(ink);
        e.setHintTextColor(muted);
        e.setTypeface(mono);
        e.setBackground(shape(c, soft, 14, line));
        e.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        return e;
    }

    /** Gap above a child of a vertical layout. */
    static <T extends View> T add(LinearLayout parent, T child, float topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(parent.getContext(), topDp);
        parent.addView(child, lp);
        return child;
    }

    /** Lucide "copy" icon at the top right of a field that holds a command; turns into a check mark for a second. */
    static ImageView copyIcon(Context c, java.util.function.Supplier<String> text) {
        ImageView icon = new ImageView(c);
        icon.setImageResource(R.drawable.ic_copy);
        icon.setColorFilter(muted);
        icon.setPadding(dp(c, 7), dp(c, 7), dp(c, 7), dp(c, 7));
        icon.setContentDescription(c.getString(R.string.perm_copy));
        ripple(icon, shape(c, 0, 10, 0));
        icon.setOnClickListener(v -> {
            String t = text.get();
            if (t == null) return;
            c.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("command", t));
            icon.setImageResource(R.drawable.ic_check);
            icon.setColorFilter(accent);
            icon.postDelayed(() -> { icon.setImageResource(R.drawable.ic_copy); icon.setColorFilter(muted); }, 1500);
        });
        return icon;
    }

    static FrameLayout.LayoutParams copyIconParams(Context c) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(c, 36), dp(c, 36), Gravity.TOP | Gravity.END);
        lp.setMargins(0, dp(c, 4), dp(c, 4), 0);
        return lp;
    }

    /** Full-screen vertical page that keeps its content out of the system bars (Android 15 draws under them). */
    static LinearLayout page(Activity a) {
        init(a);
        a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bg));
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sys = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(0, sys.top, 0, sys.bottom);
            return insets;
        });
        return root;
    }

    /** Call after setContentView: dark icons in the status and navigation bars in the light theme. */
    static void barsAppearance(Activity a) {
        int bars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        a.getWindow().getInsetsController().setSystemBarsAppearance(night ? 0 : bars, bars);
    }

    /** Opens a settings screen; the Settings app highlights the line whose key is given (AOSP keys, ignored if unknown). */
    static void openSetting(Context c, String action, String highlight) {
        android.content.Intent i = new android.content.Intent(action).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        if (highlight != null) {
            i.putExtra(":settings:fragment_args_key", highlight);
            android.os.Bundle args = new android.os.Bundle();
            args.putString(":settings:fragment_args_key", highlight);
            i.putExtra(":settings:show_fragment_args", args);
        }
        open(c, i);
    }

    /** Opens the first screen this phone has; the main Settings screen if none (some ROMs remove or rename screens). */
    static void open(Context c, android.content.Intent... screens) {
        for (android.content.Intent i : screens) {
            try {
                c.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (RuntimeException e) { // ActivityNotFoundException, SecurityException
            }
        }
        try {
            c.startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(c, android.provider.Settings.ACTION_SETTINGS, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /** Rounded tile with the logo of an app or of an Android function. */
    static ImageView logo(Context c, Recipes.App app, int sizeDp) {
        ImageView v = new ImageView(c);
        int id = c.getResources().getIdentifier(app.logo, "drawable", c.getPackageName());
        if (id != 0) v.setImageResource(id);
        int pad = dp(c, sizeDp * 0.22f);
        v.setPadding(pad, pad, pad, pad);
        // system functions sit on a green tint, brand logos on a light tile so dark logos stay readable at night
        v.setBackground(shape(c, app.system ? (night ? 0x2A3BD99A : 0x1A0A8A63) : (night ? 0xFFF0F4F1 : 0xFFFFFFFF), sizeDp * 0.3f, app.system ? 0 : line));
        v.setContentDescription(app.name);
        return v;
    }

    /** Pill-shaped choice between a few options. */
    static final class Segmented extends LinearLayout {
        interface Choice { void picked(int index); }
        private final TextView[] items;

        Segmented(Context c, String[] labels, int selected, Choice onPick) {
            super(c);
            setBackground(shape(c, soft, 16, line));
            int pad = dp(c, 4);
            setPadding(pad, pad, pad, pad);
            items = new TextView[labels.length];
            for (int i = 0; i < labels.length; i++) {
                final int index = i;
                TextView t = text(c, labels[i], 14, ink);
                t.setGravity(Gravity.CENTER);
                t.setMinHeight(dp(c, 42));
                t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                t.setOnClickListener(v -> { select(index); onPick.picked(index); });
                addView(t, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                items[i] = t;
            }
            select(selected);
        }

        void select(int index) {
            for (int i = 0; i < items.length; i++) {
                items[i].setBackground(i == index ? shape(getContext(), accent, 12, 0) : null);
                items[i].setTextColor(i == index ? accentInk : ink);
            }
        }
    }
}
