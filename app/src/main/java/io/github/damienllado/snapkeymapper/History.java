package io.github.damienllado.snapkeymapper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Runs what a gesture does and keeps the last 20 presses on the phone: when, which gesture, what, and why it failed. */
final class History {
    static final int MAX = 20;

    static final class Entry {
        long time;
        String gesture, error; // error: null when it worked
        Prefs.Step step;
    }

    private History() {}

    /** Runs the steps of a gesture. A press (not a test) is recorded, and its first failure shows a notification. Returns the first failure, or null. */
    static String run(Context c, String gesture, boolean press) {
        String first = null;
        for (Prefs.Step s : Prefs.chain(c, gesture)) {
            String why = null;
            try {
                Prefs.run(c, s);
            } catch (RuntimeException e) {
                why = reason(c, s, e);
                if (first == null) {
                    first = why;
                    if (press) notifyFailure(c, gesture, s, e, why);
                }
            }
            if (press) add(c, gesture, s, why);
        }
        return first;
    }

    private static String reason(Context c, Prefs.Step s, RuntimeException e) {
        if (e instanceof Actions.Failed) return c.getString(((Actions.Failed) e).reason);
        if (e instanceof ActivityNotFoundException) return s.app ? c.getString(R.string.fail_app_missing) : c.getString(R.string.fail_no_handler);
        if (e instanceof IllegalArgumentException) return c.getString(R.string.err_invalid_intent);
        if (e instanceof SecurityException) return c.getString(R.string.fail_denied);
        return c.getString(R.string.fail_other, e.getClass().getSimpleName());
    }

    /** Says why, and opens the screen that fixes it: the Android setting when there is one, else the home screen on that gesture. */
    private static void notifyFailure(Context c, String gesture, Prefs.Step s, RuntimeException e, String why) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("failed", c.getString(R.string.channel_failed), NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = e instanceof Actions.Failed && ((Actions.Failed) e).screen != null
                ? new Intent(((Actions.Failed) e).screen) : new Intent(c, MainActivity.class).putExtra("gesture", gesture).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        nm.notify(3, new android.app.Notification.Builder(c, "failed")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(c.getString(R.string.fail_title))
                .setContentText(s.label(c) + ": " + why)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(s.label(c) + ": " + why))
                .setContentIntent(PendingIntent.getActivity(c, 3, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setGroup("failed")
                .build());
    }

    private static android.content.SharedPreferences p(Context c) { return c.getSharedPreferences("settings", Context.MODE_PRIVATE); }

    private static synchronized void add(Context c, String gesture, Prefs.Step s, String error) {
        try {
            JSONArray old = new JSONArray(p(c).getString("history", "[]")), all = new JSONArray();
            JSONObject o = new JSONObject().put("t", System.currentTimeMillis()).put("g", gesture).put("a", s.app).put("v", s.value);
            if (error != null) o.put("e", error);
            all.put(o);
            for (int i = 0; i < old.length() && all.length() < MAX; i++) all.put(old.get(i));
            p(c).edit().putString("history", all.toString()).apply();
        } catch (JSONException e) {
            // a damaged list is dropped at the next press
            p(c).edit().remove("history").apply();
        }
    }

    /** Newest first. */
    static synchronized java.util.List<Entry> list(Context c) {
        java.util.List<Entry> out = new java.util.ArrayList<>();
        try {
            JSONArray all = new JSONArray(p(c).getString("history", "[]"));
            for (int i = 0; i < all.length(); i++) {
                JSONObject o = all.getJSONObject(i);
                Entry e = new Entry();
                e.time = o.getLong("t");
                e.gesture = o.getString("g");
                e.step = new Prefs.Step(o.getBoolean("a"), o.getString("v"));
                e.error = o.optString("e", null);
                out.add(e);
            }
        } catch (JSONException e) {
            // unreadable: shown as empty
        }
        return out;
    }

    static synchronized void clear(Context c) { p(c).edit().remove("history").apply(); }
}
