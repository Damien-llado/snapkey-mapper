package io.github.damienllado.snapkeymapper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick settings tile: listening on or off. Off stops the listener at once. On opens the app, which starts listening:
 * Android gives the log access only to a listener started with the app on screen.
 */
public class ListenTile extends TileService {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    public void onStartListening() {
        show(ListenService.running);
    }

    private void show(boolean on) {
        Tile t = getQsTile();
        if (t == null) return;
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setSubtitle(getString(on ? R.string.tile_on : R.string.tile_off));
        t.updateTile();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick() {
        if (ListenService.running) {
            Prefs.pause(this); // the app must not turn it back on when it opens
            stopService(new Intent(this, ListenService.class));
            show(false);
            return;
        }
        Intent open = new Intent(this, MainActivity.class).putExtra("restart", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // an open app gets the request too
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            startActivityAndCollapse(open);
        }
    }
}
