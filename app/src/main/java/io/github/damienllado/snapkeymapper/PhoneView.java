package io.github.damienllado.snapkeymapper;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** A small tilted phone with app tiles; the Snap Key on its left edge glows while the app listens. */
final class PhoneView extends View {
    private static final int[] TILES = {0xFF1DB954, 0xFFFC4C02, 0xFF25D366, 0xFF4285F4, 0xFFD97757, 0xFFE3B23C,
            0xFFE2557A, 0xFF5B8DEF, 0xFF8E6BF0, 0xFF2FD39A, 0xFFF08A3E, 0xFF12A8A8};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private boolean listening;
    private float pulse;
    private final ValueAnimator glow = ValueAnimator.ofFloat(0, 1);

    PhoneView(Context c) {
        super(c);
        setRotationY(-24);
        setRotationX(8);
        setRotation(3);
        setCameraDistance(Ui.dp(c, 900));
        ObjectAnimator float1 = ObjectAnimator.ofFloat(this, "translationY", -Ui.dp(c, 5), Ui.dp(c, 5));
        float1.setDuration(2600);
        float1.setRepeatMode(ValueAnimator.REVERSE);
        float1.setRepeatCount(ValueAnimator.INFINITE);
        float1.start();
        glow.setDuration(1400);
        glow.setRepeatMode(ValueAnimator.REVERSE);
        glow.setRepeatCount(ValueAnimator.INFINITE);
        glow.setInterpolator(new LinearInterpolator());
        glow.addUpdateListener(a -> { pulse = (float) a.getAnimatedValue(); invalidate(); });
    }

    void setListening(boolean on) {
        listening = on;
        if (on && !glow.isStarted()) glow.start();
        if (!on) { glow.cancel(); pulse = 0; }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), d = Ui.dp(getContext(), 1);
        float left = w * .22f, right = w * .86f, top = h * .04f, bottom = h * .96f;
        // body
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF121815);
        r.set(left, top, right, bottom);
        canvas.drawRoundRect(r, 18 * d, 18 * d, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * d);
        paint.setColor(0xFF3A4C43);
        canvas.drawRoundRect(r, 18 * d, 18 * d, paint);
        // screen
        paint.setStyle(Paint.Style.FILL);
        r.set(left + 5 * d, top + 5 * d, right - 5 * d, bottom - 5 * d);
        paint.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0xFF1E8F6B, 0xFF106048, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, 14 * d, 14 * d, paint);
        paint.setShader(null);
        // app tiles
        float cell = (r.width() - 8 * d) / 3, tile = cell - 7 * d, y0 = r.top + 22 * d;
        for (int i = 0; i < TILES.length; i++) {
            float x = r.left + 4 * d + (i % 3) * cell + 3.5f * d, y = y0 + (i / 3) * cell;
            if (y + tile > r.bottom - 6 * d) break;
            paint.setColor(TILES[i]);
            paint.setAlpha(listening ? 255 : 170);
            canvas.drawRoundRect(x, y, x + tile, y + tile, 8 * d, 8 * d, paint);
        }
        paint.setAlpha(255);
        // the Snap Key, upper left edge
        float ky0 = h * .20f, ky1 = h * .36f, kx = left - 1 * d;
        if (listening) {
            float rad = (26 + 14 * pulse) * d;
            paint.setShader(new RadialGradient(kx, (ky0 + ky1) / 2, rad, 0x883BD99A, 0x003BD99A, Shader.TileMode.CLAMP));
            canvas.drawCircle(kx, (ky0 + ky1) / 2, rad, paint);
            paint.setShader(null);
        }
        paint.setColor(listening ? 0xFF3BD99A : 0xFF5A6A62);
        canvas.drawRoundRect(kx - 3 * d, ky0, kx + 2 * d, ky1, 2.5f * d, 2.5f * d, paint);
    }
}
