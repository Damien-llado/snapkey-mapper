package io.github.damienllado.snapkeymapper;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** "full" edition: grants READ_LOGS by itself through this phone's wireless debugging (see SetupService). */
final class AdbSetup {
    static final boolean AVAILABLE = true;

    static void start(Context c) {
        c.startForegroundService(new Intent(c, SetupService.class));
        c.startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
