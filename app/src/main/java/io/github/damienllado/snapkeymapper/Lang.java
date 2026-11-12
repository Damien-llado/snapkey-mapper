package io.github.damienllado.snapkeymapper;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/** Language of the app: the one picked in the app, otherwise the phone's when supported, otherwise English. */
final class Lang {
    /** code, short label shown on the button, name in its own language */
    static final String[][] ALL = {
            {"en", "EN", "English"}, {"fr", "FR", "Français"}, {"es", "ES", "Español"}, {"de", "DE", "Deutsch"},
            {"it", "IT", "Italiano"}, {"nl", "NL", "Nederlands"}, {"pt", "PT", "Português (Brasil)"},
            {"id", "ID", "Bahasa Indonesia"}, {"vi", "VI", "Tiếng Việt"}, {"hi", "HI", "हिन्दी"}, {"th", "TH", "ไทย"},
            {"ja", "JA", "日本語"}, {"zh", "ZH", "中文"}};

    static String current(Context c) {
        String saved = c.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("lang", "");
        if (!saved.isEmpty()) return saved;
        String system = Locale.getDefault().getLanguage();
        if (system.equals("in")) system = "id"; // Android names Indonesian "in" (old ISO code); its resources are in values-in
        for (String[] l : ALL) if (l[0].equals(system)) return system;
        return "en";
    }

    static void set(Context c, String code) {
        c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("lang", code).apply();
    }

    /** A context whose resources use the chosen language: for activities, services and receivers alike. */
    static Context wrap(Context base) {
        String code = current(base);
        Locale locale = code.equals("pt") ? new Locale("pt", "BR") : code.equals("zh") ? new Locale("zh", "CN") : new Locale(code);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(locale);
        return base.createConfigurationContext(cfg);
    }

    static String label(Context c) {
        String code = current(c);
        for (String[] l : ALL) if (l[0].equals(code)) return l[1];
        return "EN";
    }
}
