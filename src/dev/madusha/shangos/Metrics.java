package dev.madusha.shangos;

/**
 * The sizes of the app tiles, in the launcher's 960 x 540 units. One setting (Settings, Layout
 * size) picks how many tiles share a row; width, gap and corner follow, so a row is always 860
 * units wide inside 50-unit side margins, and a bigger TV can ask for fewer, larger tiles.
 */
final class Metrics {
    private Metrics() {}

    static final String COMPACT = "compact", COMFORTABLE = "comfortable", COZY = "cozy";

    /**
     * Corner radius of a tile, and of the pictures on cards (Up Next shows app tiles too, so its
     * cards use this, not their own number); generous, like tvOS.
     */
    static final float RADIUS = 24;

    /**
     * Corner radius of a glass surface: a widget, the tab bar, a panel or a card of text. The same
     * as a tile's, so surfaces and tiles read as one family.
     */
    static final float CARD = RADIUS;

    /** Corner radius of a row, a button or a picker cell inside a panel. */
    static final float CONTROL = 12;

    /**
     * The type scale, in design units: a big number, a title, a heading, body text, a caption and
     * the smallest label. New text picks one of these instead of a number of its own, so screens
     * agree with each other.
     */
    static final float DISPLAY = 34, TITLE = 22, HEADING = 18, BODY = 15, CAPTION = 13, MICRO = 11;

    /**
     * The smallest text, in pixels. At 720p the smallest label (10.5 units) is exactly this, so
     * nothing changes there; on a smaller picture, text stays readable instead of shrinking.
     */
    static final int MIN_TEXT_PX = 14;

    /** The most text is enlarged for a screen that asks for bigger things (see textScale). */
    static final float MAX_TEXT_SCALE = 1.15f;

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

    /**
     * How much to enlarge text for this screen. An Android TV draws 960 dp across, so a design unit
     * is one dp and this is 1. When the TV's own Display size is set larger, or the screen is a
     * smaller one, fewer dp fit across, a dp is wider than a unit, and text grows with it (to
     * {@link #MAX_TEXT_SCALE}) so it stays as easy to read as the user asked. It never shrinks.
     *
     * @param unit pixels in one design unit (screen width / 960)
     * @param density pixels in one dp, as the system reports it
     */
    static float textScale(float unit, float density) {
        if (unit <= 0 || density <= 0) return 1;
        return Math.max(1, Math.min(MAX_TEXT_SCALE, density / unit));
    }

    /** Text of {@code units} design units, in pixels: scaled, but never below the floor. */
    static int textPx(float units, float unit, float scale) {
        return Math.max(MIN_TEXT_PX, Math.round(units * scale * unit));
    }

    /**
     * The size of an app's name under its tile: it follows the tile's width (a cozy tile is
     * wider, a compact one narrower), in half units, between 11.5 and 15. The usual tile keeps
     * the 13.5 it always had.
     */
    static float labelSize() {
        return Math.max(11.5f, Math.min(15f, Math.round(tileW * 0.097f * 2) / 2f));
    }

    /** Wi-Fi strength (dBm) as 1 to 4 bars: the status icon's rule, here so it is unit tested. */
    static int bars(int rssi) {
        return rssi >= -55 ? 4 : rssi >= -67 ? 3 : rssi >= -78 ? 2 : 1;
    }

    /** Tiles per row: read by {@link AppTile} and {@link AppPages}. */
    static int columns = 5;

    /**
     * How far tile {@code index} is from the middle of its row, in tile steps: -2 and +2 at the
     * ends of a five-tile row, -2.5 and +2.5 of a six-tile row. The parallax and the tile's depth
     * follow the layout size through this, not a fixed row of six.
     */
    static float fromRowCentre(int index) {
        return index % columns - (columns - 1) / 2f;
    }

    /** {@link #fromRowCentre} scaled so the row's ends are -1 and +1 (0 for a one-tile row). */
    static float rowPosition(int index) {
        float half = (columns - 1) / 2f;
        return half == 0 ? 0 : fromRowCentre(index) / half;
    }

    private static void set(int columns, float width, float spacing) {
        Metrics.columns = columns;
        tileW = width;
        tileH = Math.round(width * 9 / 16f);
        gap = spacing;
    }
}
