package dev.madusha.shangos;

/**
 * The sizes of the app tiles, in the launcher's 960 x 540 units. One setting (Settings, Layout
 * size) picks how many tiles share a row; width, gap and corner follow, so a row is always 860
 * units wide inside 50-unit side margins, and a bigger TV can ask for fewer, larger tiles.
 */
final class Metrics {
    private Metrics() {}

    static final String COMPACT = "compact", COMFORTABLE = "comfortable", COZY = "cozy";

    /** Corner radius of a tile; generous, like tvOS. */
    static final float RADIUS = 24;

    static float tileW = 140, tileH = 79, gap = 40;

    /** Height of a tile row: the tile and the name under it. */
    static float rowH() {
        return tileH + 36;
    }

    /** Applies a layout size: compact is 6 tiles a row, comfortable 5, cozy 4. */
    static void apply(String size) {
        if (COMPACT.equals(size)) set(6, 120, 28);
        else if (COZY.equals(size)) set(4, 176, 52);
        else set(5, 140, 40);
    }

    /** Wi-Fi strength (dBm) as 1 to 4 bars: the status icon's rule, here so it is unit tested. */
    static int bars(int rssi) {
        return rssi >= -55 ? 4 : rssi >= -67 ? 3 : rssi >= -78 ? 2 : 1;
    }

    /** Tiles per row: read by {@link AppTile} and {@link AppPages}. */
    static int columns = 5;

    private static void set(int columns, float width, float spacing) {
        Metrics.columns = columns;
        tileW = width;
        tileH = Math.round(width * 9 / 16f);
        gap = spacing;
    }
}
