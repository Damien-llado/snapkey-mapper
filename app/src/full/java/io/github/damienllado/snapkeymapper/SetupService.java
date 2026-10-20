package io.github.damienllado.snapkeymapper;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.android.AdbMdns;

/**
 * Grants READ_LOGS to the app, once, through the phone's own wireless debugging:
 * finds the pairing service on the local network, takes the 6-digit code typed in the notification,
 * pairs, connects, forgets its key, runs "pm grant" (Android then closes the app).
 */
public class SetupService extends Service {
    private static final String CODE = "code";
    private volatile int pairingPort = -1;
    private volatile String pairingHost = "127.0.0.1";
    private AdbMdns mdns;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle reply = intent == null ? null : RemoteInput.getResultsFromIntent(intent);
        if (reply != null) {
            String code = String.valueOf(reply.getCharSequence(CODE)).trim();
            say(getString(R.string.setup_pairing), getString(R.string.setup_wait), false);
            new Thread(() -> pairAndGrant(code)).start();
            return START_NOT_STICKY;
        }
        startForeground(2, build(getString(R.string.setup_waiting_title),
                getString(R.string.setup_waiting_text), false),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        if (mdns == null) {
            mdns = new AdbMdns(this, AdbMdns.SERVICE_TYPE_TLS_PAIRING, (host, port) -> {
                if (port <= 0) return;
                pairingPort = port;
                if (host != null) pairingHost = host.getHostAddress();
                say(getString(R.string.setup_code_title), getString(R.string.setup_code_text), true);
            });
            mdns.start();
        }
        return START_NOT_STICKY;
    }

    private void pairAndGrant(String code) {
        String result;
        try {
            if (mdns != null) mdns.stop();
            AbsAdbConnectionManager adb = new Adb(this);
            boolean paired;
            try {
                paired = adb.pair(pairingHost, pairingPort, code);
            } catch (Exception e) {
                android.util.Log.w("SnapKeySetup", "pairing failed", e);
                paired = false; // wrong code: Android closes the pairing screen
            }
            if (!paired) throw new IllegalStateException(getString(R.string.setup_fail_pair));
            if (!adb.autoConnect(this, 10000)) throw new IllegalStateException(getString(R.string.setup_fail_connect));
            // The key is only needed to open the connection: forget it now, because Android kills this app
            // as soon as the permission is granted (the shell posts the final message itself).
            Adb.forget(this);
            StringBuilder out = new StringBuilder();
            String done = "cmd notification post -S bigtext -t 'SnapKey Mapper' setup '" + getString(R.string.setup_done).replace("'", "'\\''") + "'";
            try (BufferedReader r = new BufferedReader(new InputStreamReader(adb.openStream(
                    "shell:pm grant " + getPackageName() + " android.permission.READ_LOGS && " + done).openInputStream()))) {
                for (String l; (l = r.readLine()) != null; ) out.append(l).append('\n');
            }
            if (checkSelfPermission(Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException(out.toString().contains("GRANT_RUNTIME_PERMISSIONS")
                        ? getString(R.string.setup_refused)
                        : getString(R.string.setup_not_granted, out.toString().trim()));
            }
            result = getString(R.string.setup_done);
        } catch (Exception e) {
            result = String.valueOf(e.getMessage());
            Adb.forget(this);
        }
        say(getString(R.string.setup_notif_title), result, false);
        stopForeground(STOP_FOREGROUND_DETACH);
        stopSelf();
    }

    private void say(String title, String text, boolean input) {
        getSystemService(NotificationManager.class).notify(2, build(title, text, input));
    }

    private Notification build(String title, String text, boolean input) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("setup", getString(R.string.setup_channel), NotificationManager.IMPORTANCE_HIGH));
        Notification.Builder b = new Notification.Builder(this, "setup")
                .setSmallIcon(android.R.drawable.ic_menu_send).setContentTitle(title).setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text)).setOnlyAlertOnce(true);
        if (input) {
            PendingIntent pi = PendingIntent.getService(this, 0, new Intent(this, SetupService.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            b.addAction(new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_edit),
                    getString(R.string.setup_enter_code), pi).addRemoteInput(new RemoteInput.Builder(CODE).setLabel(getString(R.string.setup_code_label)).build()).build());
        }
        return b.build();
    }

    @Override
    public void onDestroy() {
        if (mdns != null) mdns.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
