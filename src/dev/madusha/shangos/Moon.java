package dev.madusha.shangos;

/**
 * The moon's phase for any date, from the mean synodic month (29.530588853 days) counted from a
 * known new moon. Accurate to about a day, which is plenty for a home screen. Pure, so unit tested.
 */
final class Moon {
    private Moon() {}

    static final double SYNODIC_MS = 29.530588853 * 86_400_000.0;

    /** A new moon: 2000-01-06 18:14 UT. */
    static final long REFERENCE_NEW_MOON_MS = 947_182_440_000L;

    private static final String[] NAMES = {
        "New moon",
        "Waxing crescent",
        "First quarter",
        "Waxing gibbous",
        "Full moon",
        "Waning gibbous",
        "Last quarter",
        "Waning crescent"
    };

    /** The phase from 0 (new) through 0.5 (full) back to 1: the fraction of a cycle so far. */
    static double phase(long millis) {
        double p = ((millis - REFERENCE_NEW_MOON_MS) / SYNODIC_MS) % 1.0;
        return p < 0 ? p + 1 : p;
    }

    /** The lit fraction of the disc: 0 at new, 1 at full. */
    static double lit(double phase) {
        return (1 - Math.cos(2 * Math.PI * phase)) / 2;
    }

    /** One of the eight names, each for an eighth of the cycle around its centre. */
    static String name(double phase) {
        return NAMES[(int) Math.floor(phase * 8 + 0.5) % 8];
    }

    /** True while the lit side is on the right (waxing), which is the northern-hemisphere view. */
    static boolean waxing(double phase) {
        return phase < .5;
    }
}
