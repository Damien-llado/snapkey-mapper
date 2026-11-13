package io.github.damienllado.snapkeymapper;

import android.app.NotificationManager;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;

/** Built-in actions ("snapkey:" links in the library): what people put on the side key that is not an app to open. */
final class Actions {
    static final String SCHEME = "snapkey:";
    private static volatile boolean torchOn;
    private static boolean watching;

    private Actions() {}

    static void run(Context c, String link) {
        switch (link.substring(SCHEME.length())) {
            case "torch": torch(c); break;
            case "ringer": ringer(c); break;
            case "play-pause": media(c, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); break;
            case "next": media(c, KeyEvent.KEYCODE_MEDIA_NEXT); break;
            case "previous": media(c, KeyEvent.KEYCODE_MEDIA_PREVIOUS); break;
            default: throw new IllegalArgumentException(c.getString(R.string.err_invalid_intent));
        }
    }

    /** Flashlight on, or off when it is on (also when turned on from the quick settings): no app opens. */
    private static void torch(Context c) {
        CameraManager cm = c.getSystemService(CameraManager.class);
        try {
            synchronized (Actions.class) {
                if (!watching) {
                    cm.registerTorchCallback(new CameraManager.TorchCallback() {
                        @Override
                        public void onTorchModeChanged(String id, boolean enabled) { torchOn = enabled; }
                    }, new Handler(Looper.getMainLooper()));
                    watching = true;
                }
            }
            for (String id : cm.getCameraIdList()) {
                if (Boolean.TRUE.equals(cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                    cm.setTorchMode(id, !torchOn);
                    return;
                }
            }
            throw new IllegalStateException("no flash");
        } catch (CameraAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** Ring, then vibrate, then silent; silent only once Do Not Disturb access is given (Android requires it), else ring again. */
    private static void ringer(Context c) {
        AudioManager am = c.getSystemService(AudioManager.class);
        int mode = am.getRingerMode();
        boolean silentAllowed = c.getSystemService(NotificationManager.class).isNotificationPolicyAccessGranted();
        int next = mode == AudioManager.RINGER_MODE_NORMAL ? AudioManager.RINGER_MODE_VIBRATE
                : mode == AudioManager.RINGER_MODE_VIBRATE && silentAllowed ? AudioManager.RINGER_MODE_SILENT
                : AudioManager.RINGER_MODE_NORMAL;
        am.setRingerMode(next);
    }

    /** The key a headset would send: the app playing music reacts to it. */
    private static void media(Context c, int code) {
        AudioManager am = c.getSystemService(AudioManager.class);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }
}
