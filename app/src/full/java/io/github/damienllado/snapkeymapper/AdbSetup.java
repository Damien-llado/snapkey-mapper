package io.github.damienllado.snapkeymapper;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.TileService;

/** "full" edition: grants READ_LOGS by itself through this phone's wireless debugging (see SetupService). */
final class AdbSetup {
    static final boolean AVAILABLE = true;

    static void start(Context c) {
        c.startForegroundService(new Intent(c, SetupService.class));
        // The Wireless debugging screen itself, through the preferences of its quick settings tile (the way Key Mapper
        // does it); if the phone refuses, Developer options with the Wireless debugging line highlighted.
        Intent tile = new Intent(TileService.ACTION_QS_TILE_PREFERENCES).setPackage("com.android.settings")
                .putExtra(Intent.EXTRA_COMPONENT_NAME, new ComponentName("com.android.settings",
                        "com.android.settings.development.qstile.DevelopmentTiles$WirelessDebugging"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(tile);
        } catch (SecurityException | ActivityNotFoundException e) {
            Ui.openSetting(c, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "toggle_adb_wireless");
        }
    }
}
