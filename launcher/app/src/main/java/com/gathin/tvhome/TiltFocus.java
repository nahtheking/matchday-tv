package com.gathin.tvhome;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * tvOS style focus (config.xml, tilt_focus): the focused tile leans toward the D-pad move that reached it and
 * swings back flat, with a glass rim and a specular highlight on the side it leaned to.
 * Only rotation and a foreground drawable change, so the GPU moves a cached layer instead of redrawing.
 */
final class TiltFocus {

    private static final float TILT_DEG = 12f;
    /** Flat at rest: a rotated, outline-clipped tile is drawn without anti-aliasing and its edges stair-step. */
    private static final float REST_DEG = 0f;
    /** Focus zoom with tilt: the lean does part of the lift, so the tile grows less and stays clear of the band. */
    static final float SCALE = 1.07f;

    /** The last D-pad key a tile handled: which way the move that focused the next tile went. */
    private static int lastKey;

    private TiltFocus() {}

    static void record(int keyCode) {
        lastKey = keyCode;
    }

    static void apply(View card, boolean focused, boolean reducedMotion, float radius, float density) {
        if (!focused) {
            card.setForeground(null);
            card.animate().rotationX(0f).rotationY(0f).setDuration(150).start();
            return;
        }
        float rx = 0f, ry = 0f, cx = 0.5f, cy = 0.15f;
        switch (lastKey) {
            case KeyEvent.KEYCODE_DPAD_RIGHT: ry = -TILT_DEG; cx = 0.85f; cy = 0.35f; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: ry = TILT_DEG; cx = 0.15f; cy = 0.35f; break;
            case KeyEvent.KEYCODE_DPAD_UP: rx = -TILT_DEG; cx = 0.5f; cy = 0.05f; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: rx = TILT_DEG; cx = 0.5f; cy = 0.95f; break;
            default: break;
        }
        card.setCameraDistance(3500f * density);
        growInward(card);
        card.setForeground(glassFace(card.getWidth(), radius, cx, cy, density));
        float settleX = rx * REST_DEG / TILT_DEG, settleY = ry * REST_DEG / TILT_DEG;
        if (reducedMotion) {
            card.animate().rotationX(settleX).rotationY(settleY).setDuration(120).start();
            return;
        }
        card.animate().rotationX(rx).rotationY(ry).setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    if (card.isFocused()) {
                        card.animate().rotationX(settleX).rotationY(settleY).setDuration(450)
                                .setInterpolator(new DecelerateInterpolator()).start();
                    }
                })
                .start();
    }

    /** A tile in the first or last column grows toward the middle, so its zoom and lean stay off the screen edge. */
    private static void growInward(View card) {
        View item = (View) card.getParent();
        if (item == null || !(item.getParent() instanceof View)) return;
        View list = (View) item.getParent();
        int w = card.getWidth();
        float left = item.getLeft() + card.getLeft();
        float right = list.getWidth() - (left + w);
        if (left < w / 2f) card.setPivotX(0f);
        else if (right < w / 2f) card.setPivotX(w);
        else card.setPivotX(w / 2f);
    }

    static void clear(View card) {
        card.setForeground(null);
        card.setRotationX(0f);
        card.setRotationY(0f);
    }

    private static Drawable glassFace(int width, float radius, float cx, float cy, float density) {
        GradientDrawable shine = new GradientDrawable();
        shine.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        shine.setColors(new int[] {0x4DFFFFFF, 0x00FFFFFF});
        shine.setGradientCenter(cx, cy);
        shine.setGradientRadius(Math.max(1f, width * 0.7f));
        shine.setCornerRadius(radius);
        GradientDrawable rim = new GradientDrawable();
        rim.setColor(0x00000000);
        rim.setStroke(Math.max(1, Math.round(2f * density)), 0xB3FFFFFF);
        rim.setCornerRadius(radius);
        return new LayerDrawable(new Drawable[] {shine, rim});
    }
}
