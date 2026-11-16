package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Home: start and stop, what the key opens, the gesture. Setup, library and key finder have their own screens (menu). */
public class MainActivity extends Activity {
    private static final String[] GESTURES = Prefs.GESTURES;
    private static final int PICKED = 3;

    private TextView heroState;
    private LinearLayout stepsBox;
    private View addStep, addStepHelp;
    private View setupCard, heroCard, targetCard, gestureCard, shareCard, historyLink, finderLink;
    private TextView nativeWarning;
    private View restartHelp, restartButton;
    private PhoneView phone;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        // a failure notification opens the home screen on the gesture that failed
        showGesture(getIntent());
        LinearLayout root = Ui.page(this);
        root.addView(topBar());
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        content.setPadding(side, Ui.dp(this, 4), side, Ui.dp(this, 32));
        hero(content);
        setup(content);
        gesture(content);
        target(content);
        share(content);
        TextView history = Ui.text(this, getString(R.string.history_title), 14, Ui.accent);
        history.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12));
        history.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        historyLink = Ui.add(content, history, 6);
        TextView finder = Ui.text(this, getString(R.string.report_entry), 14, Ui.accent);
        finder.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12));
        finder.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        finderLink = Ui.add(content, finder, 0);
        // the listener sees the key only if it started while the app was on screen (Android 13+): restart it from here
        TextView help = Ui.body(this, getString(R.string.listen_restart_help));
        help.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        restartHelp = Ui.add(content, help, 10);
        restartButton = Ui.add(content, Ui.button(this, getString(R.string.listen_restart), false,
                v -> ListenService.restart(this, () -> heroState.postDelayed(this::refresh, 300))), 4);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
        // first launch, or setup not finished: the guided setup comes first
        if (SetupActivity.done(this) < SetupActivity.TOTAL) startActivity(new Intent(this, SetupActivity.class));
        else if (getIntent().getBooleanExtra("restart", false)) startListening();
    }

    /** The app was already open (or in the recent apps): the quick settings tile asks to start listening, a failure notification to show a gesture. */
    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        if (showGesture(i)) recreate();
        if (i.getBooleanExtra("restart", false) && SetupActivity.done(this) == SetupActivity.TOTAL) startListening();
    }

    /** True when the intent asks for a gesture (a failure notification): it becomes the one shown. */
    private boolean showGesture(Intent i) {
        String g = i.getStringExtra("gesture");
        if (g == null || !java.util.Arrays.asList(GESTURES).contains(g)) return false;
        Prefs.gesture(this, g);
        return true;
    }

    /** The notification shade or a tile changed the state while the app stayed open. */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) refresh();
    }

    /** Name, language and the menu that leads to the other screens. */
    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        TextView name = Ui.text(this, "SnapKey Mapper", 20, Ui.ink);
        name.setTypeface(Ui.display);
        bar.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView lang = Ui.text(this, Lang.label(this) + " ▾", 13, Ui.ink);
        lang.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        lang.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        Ui.ripple(lang, Ui.shape(this, Ui.soft, 14, Ui.line));
        lang.setContentDescription(getString(R.string.lang_title));
        lang.setOnClickListener(v -> chooseLanguage());
        bar.addView(lang);
        TextView menu = Ui.text(this, "☰", 22, Ui.ink);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription(getString(R.string.menu));
        Ui.ripple(menu, Ui.shape(this, 0, 20, 0));
        menu.setOnClickListener(this::showMenu);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48));
        mp.leftMargin = Ui.dp(this, 4);
        bar.addView(menu, mp);
        return bar;
    }

    private void showMenu(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        Menu items = m.getMenu();
        items.add(0, 1, 0, R.string.setup_title);
        items.add(0, 2, 1, R.string.library_title);
        items.add(0, 3, 2, R.string.finder_title);
        items.add(0, 4, 3, R.string.menu_help);
        items.add(0, 5, 4, R.string.menu_diag);
        m.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: startActivity(new Intent(this, SetupActivity.class)); break;
                case 2: startActivityForResult(new Intent(this, LibraryActivity.class), PICKED); break;
                case 3: startActivity(new Intent(this, FinderActivity.class)); break;
                case 5: Diagnostic.copy(this); break;
                default: startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.help_url))));
            }
            return true;
        });
        m.show();
    }

    /** Dark gradient card: state, the big start button and the tilted phone. */
    private void hero(LinearLayout top) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 20);
        card.setPadding(p, p, Ui.dp(this, 8), p);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0B6E50, 0xFF0C3328});
        g.setCornerRadius(Ui.dp(this, 28));
        card.setBackground(g);
        card.setElevation(Ui.dp(this, 4));
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        heroState = Ui.text(this, "", 16, 0xFFF0FFF7);
        heroState.setTypeface(Ui.display);
        left.addView(heroState);
        card.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        phone = new PhoneView(this);
        card.addView(phone, new LinearLayout.LayoutParams(Ui.dp(this, 110), Ui.dp(this, 150)));
        heroCard = Ui.add(top, card, 0);
    }

    /** Shown until every setup step is done. */
    private void setup(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.home_setup_title)));
        Ui.add(card, Ui.body(this, getString(R.string.home_setup_text)), 4);
        Ui.add(card, Ui.button(this, getString(R.string.setup_continue), true,
                v -> startActivity(new Intent(this, SetupActivity.class))), 14);
        setupCard = Ui.add(top, card, 0);
    }

    private void target(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.target_title)));
        stepsBox = new LinearLayout(this);
        stepsBox.setOrientation(LinearLayout.VERTICAL);
        Ui.add(card, stepsBox, 12);
        nativeWarning = Ui.body(this, getString(R.string.native_long_used));
        nativeWarning.setTextColor(0xFFB45309);
        Ui.add(card, nativeWarning, 8);
        addStep = Ui.add(card, Ui.button(this, getString(R.string.step_add), false, v -> addStep()), 14);
        addStepHelp = Ui.add(card, Ui.body(this, getString(R.string.step_add_help)), 6);
        Ui.add(card, Ui.button(this, getString(R.string.library_open), true,
                v -> startActivityForResult(new Intent(this, LibraryActivity.class), PICKED)), 8);
        Ui.add(card, Ui.button(this, getString(R.string.app_choose), false,
                v -> startActivityForResult(new Intent(this, AppPickerActivity.class), PICKED)), 8);
        Ui.add(card, Ui.button(this, getString(R.string.target_link_enter), false, v -> enterLink(1)), 8);
        Ui.add(card, Ui.button(this, getString(R.string.target_test), false, v -> {
            String failed = History.run(this, Prefs.gesture(this), false);
            if (failed != null) Toast.makeText(this, getString(R.string.toast_cannot_open, failed), Toast.LENGTH_LONG).show();
        }), 8);
        Ui.add(card, Ui.button(this, getString(R.string.target_clear), false, v -> {
            Prefs.clearTarget(this);
            refresh();
        }), 8);
        targetCard = Ui.add(top, card, 14);
    }

    /** Copy what the press does as a text, or paste one. */
    private void share(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.share_title)));
        Ui.add(card, Ui.body(this, getString(R.string.share_help)), 4);
        Ui.add(card, Ui.button(this, getString(R.string.share_copy), false, v -> copyShortcut()), 14);
        Ui.add(card, Ui.button(this, getString(R.string.share_paste), false, v -> pasteShortcut()), 8);
        shareCard = Ui.add(top, card, 14);
    }

    private void copyShortcut() {
        java.util.List<Prefs.Step> steps = Prefs.chain(this, Prefs.gesture(this));
        if (steps.isEmpty()) {
            Toast.makeText(this, R.string.share_nothing, Toast.LENGTH_SHORT).show();
            return;
        }
        getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("shortcut", Share.export(steps)));
        Toast.makeText(this, R.string.share_copied, Toast.LENGTH_LONG).show();
    }

    /** A field to paste in, filled with the clipboard when it holds a shortcut. */
    private void pasteShortcut() {
        ClipData clip = getSystemService(ClipboardManager.class).getPrimaryClip();
        String text = clip != null && clip.getItemCount() > 0 ? String.valueOf(clip.getItemAt(0).coerceToText(this)) : "";
        EditText field = Ui.field(this, text.contains("SKM") ? text.trim() : "", "SKM1 …");
        field.setSingleLine(false);
        FrameLayout box = new FrameLayout(this);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        box.addView(field);
        new AlertDialog.Builder(this).setTitle(R.string.share_paste).setMessage(R.string.share_paste_help).setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> confirmShortcut(field.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    /** Shows what the pasted shortcut does before it replaces the press; a refused one says why. */
    private void confirmShortcut(String text) {
        java.util.List<Prefs.Step> steps;
        try {
            steps = Share.parse(this, text);
        } catch (IllegalArgumentException e) {
            new AlertDialog.Builder(this).setMessage(e.getMessage()).setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            Prefs.Step s = steps.get(i);
            list.append(i + 1).append(". ").append(s.label(this));
            if (!s.app && !s.builtIn() && !s.label(this).equals(s.value)) list.append("\n    ").append(s.value.length() > 120 ? s.value.substring(0, 120) + "…" : s.value);
            list.append('\n');
        }
        String g = Prefs.gesture(this);
        new AlertDialog.Builder(this).setMessage(getString(R.string.share_confirm, Prefs.gestureLabel(this, g)) + "\n\n" + list + "\n" + getString(R.string.share_trust))
                .setPositiveButton(R.string.share_use, (d, w) -> {
                    Prefs.setChain(this, g, steps);
                    listenIfReady();
                    refresh();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    /** Another action after the last one (a built-in action can be followed by up to two more): the same three ways to choose it. */
    private void addStep() {
        int next = Prefs.chain(this, Prefs.gesture(this)).size() + 1;
        String[] ways = {getString(R.string.library_open), getString(R.string.app_choose), getString(R.string.target_link_enter)};
        new AlertDialog.Builder(this).setTitle(R.string.step_add).setItems(ways, (d, which) -> {
            Prefs.slot = next;
            if (which == 0) startActivityForResult(new Intent(this, LibraryActivity.class), PICKED);
            else if (which == 1) startActivityForResult(new Intent(this, AppPickerActivity.class), PICKED);
            else enterLink(next);
        }).show();
    }

    private void gesture(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.gesture_title)));
        Ui.add(card, Ui.body(this, getString(R.string.gesture_help)), 4);
        int current = java.util.Arrays.asList(GESTURES).indexOf(Prefs.gesture(this));
        Ui.add(card, new Ui.Segmented(this, new String[]{getString(R.string.gesture_single), getString(R.string.gesture_double), getString(R.string.gesture_triple), getString(R.string.gesture_long)}, Math.max(current, 0),
                i -> { Prefs.gesture(this, GESTURES[i]); refresh(); }), 12); // each press has its own target, shown below
        gestureCard = Ui.add(top, card, 14);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // the "Pairing..." notification of the setup service dies with the app when the permission is granted, and stays on screen
        if (checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            getSystemService(android.app.NotificationManager.class).cancel(2);
        }
        Prefs.slot = 1;
        listenIfReady();
        refresh();
    }

    private void refresh() {
        int done = SetupActivity.done(this), total = SetupActivity.TOTAL;
        boolean on = ListenService.running;
        heroState.setText(on ? getString(R.string.state_listening) : Prefs.paused(this) ? getString(R.string.state_paused) : done == total ? getString(R.string.state_choose) : getString(R.string.state_setup, done, total));
        phone.setListening(on);
        boolean ready = done == total;
        setupCard.setVisibility(ready ? View.GONE : View.VISIBLE);
        for (View v : new View[]{heroCard, targetCard, gestureCard, shareCard, historyLink, finderLink, restartHelp, restartButton}) v.setVisibility(ready ? View.VISIBLE : View.GONE);
        nativeWarning.setVisibility(Prefs.gesture(this).equals("long") && Prefs.nativeLongPress(this) ? View.VISIBLE : View.GONE);
        steps();
    }

    /** The actions of the gesture shown, in order, each with its real icon; a ✕ on each one when there are several. */
    private void steps() {
        String g = Prefs.gesture(this);
        java.util.List<Prefs.Step> chain = Prefs.chain(this, g);
        stepsBox.removeAllViews();
        if (chain.isEmpty()) Ui.add(stepsBox, Ui.text(this, getString(R.string.target_none), 15, Ui.ink), 0);
        for (int i = 0; i < chain.size(); i++) {
            final int n = i + 1;
            Prefs.Step s = chain.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(Ui.stepIcon(this, s, 44), new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
            TextView name = Ui.text(this, (chain.size() > 1 ? n + ". " : "") + s.label(this), 15, Ui.ink);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tp.leftMargin = Ui.dp(this, 12);
            row.addView(name, tp);
            if (chain.size() > 1) {
                TextView x = Ui.text(this, "✕", 18, Ui.muted);
                x.setGravity(Gravity.CENTER);
                x.setContentDescription(getString(R.string.step_remove));
                Ui.ripple(x, Ui.shape(this, 0, 20, 0));
                x.setOnClickListener(v -> {
                    Prefs.removeStep(this, g, n);
                    refresh();
                });
                row.addView(x, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
            }
            Ui.add(stepsBox, row, i == 0 ? 0 : 8);
        }
        // another action can follow a built-in one, up to 3
        int more = !chain.isEmpty() && chain.size() < Prefs.MAX_STEPS && chain.get(chain.size() - 1).builtIn() ? View.VISIBLE : View.GONE;
        addStep.setVisibility(more);
        addStepHelp.setVisibility(more);
    }

    /** An app or a link ends the series: whatever was kept after it goes (it would come back if that step later became a built-in action). */
    private void trimChain() {
        String g = Prefs.gesture(this);
        Prefs.setChain(this, g, Prefs.chain(this, g));
    }

    /** Link typed by hand: web address, deep link or intent: link. */
    private void enterLink(int step) {
        Prefs.slot = step;
        EditText field = Ui.field(this, Prefs.link(this), "https://…");
        FrameLayout box = new FrameLayout(this);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        box.addView(field);
        new AlertDialog.Builder(this).setTitle(R.string.target_link_enter).setMessage(R.string.target_link_help).setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    Prefs.save(this, field.getText().toString().trim(), Prefs.pattern(this));
                    Prefs.targetType(this, "link");
                    trimChain();
                    listenIfReady();
                    refresh();
                }).setNegativeButton(android.R.string.cancel, null).setOnDismissListener(d -> Prefs.slot = 1).show();
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

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        if (code == PICKED) { // a shortcut, an app or a link was chosen
            trimChain();
            refresh();
        }
    }

    /** Set up and something chosen to open: listen, with nothing to press (the grant of the permission closes the app, which is then not listening). */
    private void listenIfReady() {
        if (SetupActivity.done(this) == SetupActivity.TOTAL && Prefs.hasAnyTarget(this) && !ListenService.running && !Prefs.paused(this)) startListening();
    }

    private void startListening() {
        if (Prefs.pattern(this).isEmpty()) return;
        startForegroundService(new Intent(this, ListenService.class));
        Prefs.active(this, true);
        heroState.postDelayed(this::refresh, 300);
    }
}
