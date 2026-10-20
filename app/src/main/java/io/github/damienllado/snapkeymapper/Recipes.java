package io.github.damienllado.snapkeymapper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The recipe library: ready-made links grouped by app, read from assets/recipes (texts translated per language). */
final class Recipes {
    static final class Var {
        String name, label, where, example, kind;
    }

    static final class Recipe {
        String id, title, desc, link, note, source, tested, level;
        List<Var> vars = new ArrayList<>();
        App app;
    }

    static final class App {
        String id, name, pkg, logo;
        List<String> also = new ArrayList<>(); // other builds of the same app (website build, beta...)
        boolean system;
        List<Recipe> recipes = new ArrayList<>();

        boolean installed(Context c) {
            if (system || pkg.isEmpty()) return true;
            List<String> all = new ArrayList<>(also);
            all.add(pkg);
            for (String p : all) {
                try {
                    c.getPackageManager().getPackageInfo(p, 0);
                    return true;
                } catch (PackageManager.NameNotFoundException e) {
                    // try the next build
                }
            }
            return false;
        }
    }

    private static List<App> cache;
    private static String cachedLang;

    /** Apps in display order: groups of Android functions first, then installed apps, then the others. */
    static synchronized List<App> load(Context c) {
        String lang = Lang.current(c);
        if (cache == null || !lang.equals(cachedLang)) {
            cache = parse(read(c, "recipes/recipes.json"), lang.equals("en") ? null : read(c, "recipes/i18n/" + lang + ".json"));
            cachedLang = lang;
        }
        List<App> sorted = new ArrayList<>();
        for (App a : cache) if (a.system) sorted.add(a);
        for (App a : cache) if (!a.system && a.installed(c)) sorted.add(a);
        for (App a : cache) if (!a.system && !a.installed(c)) sorted.add(a);
        return sorted;
    }

    static Recipe find(Context c, String id) {
        for (App a : load(c)) for (Recipe r : a.recipes) if (r.id.equals(id)) return r;
        return null;
    }

    static int count(Context c) {
        int n = 0;
        for (App a : load(c)) n += a.recipes.size();
        return n;
    }

    private static String read(Context c, String path) {
        try (InputStream in = c.getAssets().open(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static List<App> parse(String base, String texts) {
        List<App> apps = new ArrayList<>();
        try {
            JSONObject tr = texts == null ? new JSONObject() : new JSONObject(texts);
            JSONObject trApps = tr.optJSONObject("apps"), trRecipes = tr.optJSONObject("recipes");
            JSONArray list = new JSONObject(base).getJSONArray("apps");
            for (int i = 0; i < list.length(); i++) {
                JSONObject o = list.getJSONObject(i);
                App a = new App();
                a.id = o.getString("id");
                a.name = text(trApps, a.id, "name", o.getString("name"));
                a.pkg = o.optString("package", "");
                JSONArray others = o.optJSONArray("also");
                for (int k = 0; others != null && k < others.length(); k++) a.also.add(others.getString(k));
                a.logo = "logo_" + a.id.replace('-', '_');
                a.system = o.optString("kind").equals("system");
                JSONArray rs = o.getJSONArray("recipes");
                for (int j = 0; j < rs.length(); j++) {
                    JSONObject ro = rs.getJSONObject(j);
                    Recipe r = new Recipe();
                    r.id = ro.getString("id");
                    r.app = a;
                    r.title = text(trRecipes, r.id, "title", ro.getString("title"));
                    r.desc = text(trRecipes, r.id, "desc", ro.getString("desc"));
                    r.link = ro.getString("link");
                    r.note = text(trRecipes, r.id, "note", ro.optString("note", ""));
                    r.source = ro.optString("source", "");
                    r.tested = ro.optString("tested", "");
                    r.level = ro.optString("level", "screen");
                    JSONObject trVars = trRecipes == null || trRecipes.optJSONObject(r.id) == null ? null : trRecipes.getJSONObject(r.id).optJSONObject("vars");
                    JSONArray vs = ro.getJSONArray("vars");
                    for (int k = 0; k < vs.length(); k++) {
                        JSONObject vo = vs.getJSONObject(k);
                        Var v = new Var();
                        v.name = vo.getString("name");
                        v.label = text(trVars, v.name, "label", vo.getString("label"));
                        v.where = text(trVars, v.name, "where", vo.getString("where"));
                        v.example = vo.getString("example");
                        v.kind = vo.optString("kind", "text");
                        r.vars.add(v);
                    }
                    a.recipes.add(r);
                }
                apps.add(a);
            }
        } catch (JSONException e) {
            throw new IllegalStateException("recipes.json is not valid", e);
        }
        return apps;
    }

    /** Translated text if there is one, otherwise the English one. */
    private static String text(JSONObject group, String key, String field, String fallback) {
        if (group == null) return fallback;
        JSONObject o = group.optJSONObject(key);
        return o == null ? fallback : o.optString(field, fallback);
    }

    /** The link with every [NAME] replaced by the value typed for it, or null while a value is missing. */
    static String fill(Recipe r, List<String> values) {
        for (String v : values) if (v.trim().isEmpty()) return null;
        return preview(r, values);
    }

    /** Same, but a missing value leaves its [NAME] visible: for the live preview. */
    static String preview(Recipe r, List<String> values) {
        String link = r.link;
        for (int i = 0; i < r.vars.size(); i++) {
            String value = values.get(i).trim();
            if (!value.isEmpty()) link = link.replace("[" + r.vars.get(i).name + "]", encoded(r.vars.get(i), value));
        }
        return link;
    }

    private static String encoded(Var v, String value) {
        switch (v.kind) {
            case "raw": return value;                                // used as typed
            case "phone": return value.replaceAll("[^0-9+]", "");     // "+33 1 23" becomes "+331 23"
            case "digits": return value.replaceAll("[^0-9]", "");     // wa.me wants the number without + or spaces
            default: return Uri.encode(value);
        }
    }
}
