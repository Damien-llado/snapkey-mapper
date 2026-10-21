package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Home: start and stop, what the key opens, the gesture. Setup, library and key finder have their own screens (menu). */
public class MainActivity extends Activity {
    private static final String[] GESTURES = {"single", "double", "long"};
    private static final int PICKED = 3;

    private TextView heroState, listen, targetName;
    private ImageView targetIcon;
    private View setupCard, heroCard, targetCard, gestureCard, finderLink;
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
        LinearLayout root = Ui.page(this);
        root.addView(topBar());
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 16);
        content.setPadding(side, Ui.dp(this, 4), side, Ui.dp(this, 32));
        hero(content);
        setup(content);
        target(content);
        gesture(content);
        TextView finder = Ui.text(this, getString(R.string.home_finder), 14, Ui.accent);
        finder.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12));
        finder.setOnClickListener(v -> startActivity(new Intent(this, FinderActivity.class)));
        finderLink = Ui.add(content, finder, 10);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        Ui.barsAppearance(this);
        if (getIntent().getBooleanExtra("restart", false)) startListening();
        // first launch, or setup not finished: the guided setup comes first
        else if (SetupActivity.done(this) < SetupActivity.TOTAL) startActivity(new Intent(this, SetupActivity.class));
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
        m.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: startActivity(new Intent(this, SetupActivity.class)); break;
                case 2: startActivityForResult(new Intent(this, LibraryActivity.class), PICKED); break;
                case 3: startActivity(new Intent(this, FinderActivity.class)); break;
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
        listen = Ui.button(this, "", true, v -> toggleListen());
        listen.setTextColor(0xFF04130C);
        Ui.ripple(listen, Ui.shape(this, 0xFF3BD99A, 16, 0));
        Ui.add(left, listen, 16);
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
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        targetIcon = new ImageView(this);
        row.addView(targetIcon, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        targetName = Ui.text(this, "", 15, Ui.ink);
        targetName.setMaxLines(2);
        targetName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tp.leftMargin = Ui.dp(this, 12);
        row.addView(targetName, tp);
        Ui.add(card, row, 12);
        Ui.add(card, Ui.button(this, getString(R.string.library_open), true,
                v -> startActivityForResult(new Intent(this, LibraryActivity.class), PICKED)), 14);
        Ui.add(card, Ui.button(this, getString(R.string.app_choose), false,
                v -> startActivityForResult(new Intent(this, AppPickerActivity.class), PICKED)), 8);
        Ui.add(card, Ui.button(this, getString(R.string.target_link_enter), false, v -> enterLink()), 8);
        Ui.add(card, Ui.button(this, getString(R.string.target_test), false, v -> {
            try {
                Prefs.openTarget(this);
            } catch (RuntimeException e) {
                Toast.makeText(this, getString(R.string.toast_cannot_open, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }), 8);
        targetCard = Ui.add(top, card, 14);
    }

    private void gesture(LinearLayout top) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, getString(R.string.gesture_title)));
        Ui.add(card, Ui.body(this, getString(R.string.gesture_help)), 4);
        int current = java.util.Arrays.asList(GESTURES).indexOf(Prefs.gesture(this));
        Ui.add(card, new Ui.Segmented(this, new String[]{getString(R.string.gesture_single), getString(R.string.gesture_double), getString(R.string.gesture_long)}, Math.max(current, 0),
                i -> Prefs.gesture(this, GESTURES[i])), 12);
        gestureCard = Ui.add(top, card, 14);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // set up and not stopped by the user: listen right away (the grant of the permission restarts the app, which is not listening then)
        // the "Pairing..." notification of the setup service dies with the app when the permission is granted, and stays on screen
        if (checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            getSystemService(android.app.NotificationManager.class).cancel(2);
        }
        if (SetupActivity.done(this) == SetupActivity.TOTAL && !ListenService.running && !Prefs.stopped(this)) startListening();
        refresh();
    }

    private void refresh() {
        int done = SetupActivity.done(this), total = SetupActivity.TOTAL;
        boolean on = ListenService.running;
        heroState.setText(on ? getString(R.string.state_listening) : done == total ? getString(R.string.state_ready) : getString(R.string.state_setup, done, total));
        listen.setText(on ? getString(R.string.btn_stop) : getString(R.string.btn_start));
        phone.setListening(on);
        boolean ready = done == total;
        setupCard.setVisibility(ready ? View.GONE : View.VISIBLE);
        for (View v : new View[]{heroCard, targetCard, gestureCard, finderLink}) v.setVisibility(ready ? View.VISIBLE : View.GONE);
        if (Prefs.targetType(this).equals("app") && !Prefs.appPackage(this).isEmpty()) {
            String pkg = Prefs.appPackage(this);
            try {
                PackageManager pm = getPackageManager();
                targetName.setText(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
                targetIcon.setImageDrawable(pm.getApplicationIcon(pkg));
            } catch (PackageManager.NameNotFoundException e) {
                targetName.setText(pkg);
                targetIcon.setImageDrawable(null);
            }
        } else {
            String l = Prefs.link(this);
            targetName.setText(l.isEmpty() ? getString(R.string.target_none) : l);
            targetIcon.setImageResource(android.R.drawable.ic_menu_share);
        }
    }

    /** Link typed by hand: web address, deep link or intent: link. */
    private void enterLink() {
        EditText field = Ui.field(this, Prefs.link(this), "https://…");
        FrameLayout box = new FrameLayout(this);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        box.addView(field);
        new AlertDialog.Builder(this).setTitle(R.string.target_link_enter).setMessage(R.string.target_link_help).setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    Prefs.save(this, field.getText().toString().trim(), Prefs.pattern(this));
                    Prefs.targetType(this, "link");
                    refresh();
                }).setNegativeButton(android.R.string.cancel, null).show();
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
        if (code == PICKED) refresh(); // a shortcut, an app or a link was chosen
    }

    private void toggleListen() {
        if (ListenService.running) {
            stopService(new Intent(this, ListenService.class));
            Prefs.active(this, false);
            Prefs.stopped(this, true);
            heroState.postDelayed(this::refresh, 300);
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (Prefs.pattern(this).isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_choose_pattern), Toast.LENGTH_LONG).show();
            return;
        }
        startForegroundService(new Intent(this, ListenService.class));
        Prefs.active(this, true);
        Prefs.stopped(this, false);
        heroState.postDelayed(this::refresh, 300);
    }
}
