package io.github.damienllado.snapkeymapper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After boot: reading the log requires the app in the foreground, so we only offer a restart notification. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && Prefs.active(c)) {
            ListenService.warnStopped(c);
        }
    }
}
