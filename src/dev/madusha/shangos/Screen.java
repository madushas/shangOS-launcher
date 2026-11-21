package dev.madusha.shangos;

import android.content.Context;

/**
 * The size of the screen the layout is built for. Normally the real one; in a debug build the
 * preview setting pretends to be a larger one (1080p or 4K): the page is laid out and drawn at that
 * size and scaled down onto the real screen, so a 720p TV shows what a 4K TV would render.
 */
final class Screen {
    private Screen() {}

    /** Width in pixels of the screen to pretend to be; 0 for the real one. Debug builds only. */
    static int preview;

    static int width(Context context) {
        return preview > 0 ? preview : context.getResources().getDisplayMetrics().widthPixels;
    }

    static int height(Context context) {
        int real = context.getResources().getDisplayMetrics().heightPixels;
        return preview > 0 ? Math.round(real * scale(context)) : real;
    }

    /** Virtual pixels per real pixel (1 when not previewing). */
    static float scale(Context context) {
        return width(context) / (float) context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Puts {@code root} on screen. When previewing, it is laid out at the pretend size and scaled
     * down to fit the real screen.
     */
    static void show(android.app.Activity activity, android.view.View root) {
        Layers.atStart(activity, root); // debug builds only: QA switches given on a cold start
        if (preview <= 0) {
            activity.setContentView(root);
            return;
        }
        root.setPivotX(0);
        root.setPivotY(0);
        root.setScaleX(1 / scale(activity));
        root.setScaleY(1 / scale(activity));
        activity.setContentView(
                root, new android.view.ViewGroup.LayoutParams(width(activity), height(activity)));
    }
}
