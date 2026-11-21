package dev.madusha.shangos;

import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

/**
 * Debug builds only: switches single layers of the screen off, to find out which one makes frames
 * late ({@code tools/jank-layers.sh} drives it). The names listed are turned OFF; {@code none} puts
 * everything back:
 *
 * <pre>
 * adb shell am start -n dev.madusha.shangos/.MainActivity --es shangos.layers wallpaper,shadow
 * </pre>
 *
 * {@code wallpaper}: the picture and lights; {@code tiles}: every app tile (focus still moves);
 * {@code shadow}: the tiles' elevation shadows; {@code clip}: the rounded clipping of each tile's
 * picture. Release and offline builds never call {@link #apply}, so nothing changes there.
 */
final class Layers {
    private Layers() {}

    private static boolean shadow = true, clip = true;

    /** The elevation a tile asks for; 0 while the shadows are switched off. */
    static int elevation(int px) {
        return Flavor.DEBUG && !shadow ? 0 : px;
    }

    static boolean clip() {
        return !Flavor.DEBUG || clip;
    }

    static void apply(String names, View root) {
        if (!Flavor.DEBUG || names == null) return;
        List<String> off = Arrays.asList(names.split(","));
        shadow = !off.contains("shadow");
        clip = !off.contains("clip");
        walk(root, !off.contains("wallpaper"), !off.contains("tiles"));
        Dbg.d("layers", "off: " + names);
    }

    // Alpha 0, not INVISIBLE: an invisible tile cannot take focus, and the keys must still move it.
    private static void walk(View view, boolean wallpaper, boolean tiles) {
        if (view instanceof Wallpaper) view.setAlpha(wallpaper ? 1 : 0);
        if (view instanceof AppTile) {
            view.setAlpha(tiles ? 1 : 0);
            ((AppTile) view).layers();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                walk(group.getChildAt(i), wallpaper, tiles);
        }
    }
}
