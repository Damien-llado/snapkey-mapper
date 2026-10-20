package io.github.damienllado.snapkeymapper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

/**
 * Debug builds only, for the emulator.
 * --es b64 <base64 of a link>: opens the link like the Snap Key would;
 * --es screen library: opens the recipe library; --es screen setup: starts the automatic setup (full edition);
 * --es screen <recipe id>: opens that recipe. No coordinates to tap: the home screen animation blocks uiautomator.
 */
public class DebugFire extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String screen = getIntent().getStringExtra("screen");
        if (screen != null && screen.equals("setup")) {
            AdbSetup.start(this);
        } else if (screen != null) {
            Intent i = screen.equals("library") ? new Intent(this, LibraryActivity.class) : new Intent(this, RecipeActivity.class).putExtra("recipe", screen);
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            try {
                Prefs.open(this, new String(Base64.decode(getIntent().getStringExtra("b64"), Base64.DEFAULT)));
                Log.i("DebugFire", "opened");
            } catch (RuntimeException e) {
                Log.w("DebugFire", "failed: " + e);
            }
        }
        finish();
    }
}
