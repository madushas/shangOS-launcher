package dev.madusha.shangos;

/**
 * Tells quick repeats from single moves: {@link #rapid} is true when the previous call was less
 * than {@code windowMillis} ago. While the D-pad is being pressed in a row, the full-screen
 * wallpaper effects skip their long animations (a moving full-screen layer repaints the whole
 * screen every frame, which a weak TV GPU cannot keep up with), and play them for a single move.
 * Pure, so it is unit tested.
 */
final class Pace {
    private final long windowMillis;
    private long last = Long.MIN_VALUE / 2;

    Pace(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /** Records a move at {@code nowMillis}; true if it came quickly after the one before. */
    boolean rapid(long nowMillis) {
        boolean quick = nowMillis - last < windowMillis;
        last = nowMillis;
        return quick;
    }
}
