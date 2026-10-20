package io.github.damienllado.snapkeymapper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Foreground service: reads the system log continuously and opens the target when the side key pattern shows up.
 * Android 13+ only allows log reading if it starts while the app is in the foreground:
 * the service is therefore started from the app screen and never restarted by the system.
 */
public class ListenService extends Service {
    static volatile boolean running;
    private Process log;
    private static final Pattern RELEASE = Pattern.compile("eventTime=(\\d+)");
    private static final Pattern PRESS = Pattern.compile("downTime=(\\d+)");
    // "logcat -v uid" writes "I/Tag(  1000: 1234): ..." (or "system:" on some builds): the writer of the line
    private static final Pattern SYSTEM = Pattern.compile("^./[^(]*\\(\\s*(1000|system)\\s*:");
    private volatile long lastRelease, alreadySeen;
    private volatile int presses;
    private volatile boolean stopRequested;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, notification(this, getString(R.string.notif_listening_title), getString(R.string.notif_listening_text), 0),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        if (log == null) {
            String pattern = Prefs.pattern(this);
            String gesture = Prefs.gesture(this);
            try {
                log = Prefs.readLog(l -> {
                    // Diagnostic: Android logs the launches it blocks
                    if (l.contains(getPackageName()) && l.toLowerCase().contains("background activity")) {
                        diagnostic(getString(R.string.diag_blocked));
                        return;
                    }
                    // Only the release line is read: it carries press and release times (in nanoseconds)
                    if (pattern.isEmpty() || !l.contains(pattern) || !l.contains("ACTION_UP")) return;
                    // Any app can write to the log: only lines written by Android itself (uid 1000, "system") count,
                    // so an app cannot fire the target by writing a fake key line
                    if (!SYSTEM.matcher(l).find()) return;
                    Matcher end = RELEASE.matcher(l), start = PRESS.matcher(l);
                    if (!end.find() || !start.find()) return;
                    long release = Long.parseLong(end.group(1)) / 1_000_000, press = Long.parseLong(start.group(1)) / 1_000_000;
                    if (release == alreadySeen) return; // same press logged on several lines
                    alreadySeen = release;
                    long previous = lastRelease;
                    lastRelease = release;
                    boolean isLong = release - press >= 500;
                    switch (gesture) {
                        case "long": if (!isLong) return; break;
                        case "double":
                            if (isLong || press - previous > 600) return; // first press: wait for the second
                            lastRelease = 0; // a third press starts over
                            break;
                        default: if (isLong) return;
                    }
                    presses++;
                    try {
                        Prefs.openTarget(this);
                        diagnostic(getString(R.string.diag_requested));
                    } catch (RuntimeException e) {
                        diagnostic(getString(R.string.diag_error, e.getClass().getSimpleName()));
                    }
                }, () -> {
                    if (stopRequested) return;
                    warnStopped(this);
                    stopSelf();
                });
            } catch (IOException e) {
                warnStopped(this);
                stopSelf();
            }
        }
        running = true;
        return START_NOT_STICKY;
    }

    /** Shows in the notification how many presses were seen and what happened at the last one. */
    private void diagnostic(String lastState) {
        getSystemService(NotificationManager.class).notify(1, notification(this,
                getString(R.string.notif_count, presses), lastState, 0));
    }

    @Override
    public void onDestroy() {
        running = false;
        stopRequested = true;
        if (log != null) log.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    /** "Tap to restart" notification: after a reboot or if the system stopped the listener. */
    static void warnStopped(Context base) {
        Context c = Lang.wrap(base);
        c.getSystemService(NotificationManager.class)
                .notify(2, notification(c, c.getString(R.string.notif_stopped_title), c.getString(R.string.notif_stopped_text), Notification.FLAG_AUTO_CANCEL));
    }

    static Notification notification(Context c, String title, String text, int flags) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("listen", c.getString(R.string.channel_listen), NotificationManager.IMPORTANCE_LOW));
        Intent open = new Intent(c, MainActivity.class).putExtra("restart", flags != 0);
        Notification n = new Notification.Builder(c, "listen")
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(PendingIntent.getActivity(c, flags, open,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(flags != 0)
                .setOngoing(flags == 0)
                .build();
        return n;
    }
}
