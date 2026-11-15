package dev.madusha.shangos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MoonTest {
    private static final long DAY = 86_400_000L;

    @Test
    public void theReferenceDateIsANewMoon() {
        double p = Moon.phase(Moon.REFERENCE_NEW_MOON_MS);
        assertEquals("New moon", Moon.name(p));
        assertTrue(Moon.lit(p) < .01);
    }

    @Test
    public void halfACycleLaterIsFull() {
        long full = Moon.REFERENCE_NEW_MOON_MS + Math.round(Moon.SYNODIC_MS / 2);
        assertEquals("Full moon", Moon.name(Moon.phase(full)));
        assertTrue(Moon.lit(Moon.phase(full)) > .99);
    }

    @Test
    public void aQuarterCycleIsAFirstQuarter() {
        long quarter = Moon.REFERENCE_NEW_MOON_MS + Math.round(Moon.SYNODIC_MS / 4);
        assertEquals("First quarter", Moon.name(Moon.phase(quarter)));
        assertEquals(.5, Moon.lit(Moon.phase(quarter)), .02);
    }

    @Test
    public void datesBeforeTheReferenceStillWork() {
        double p = Moon.phase(Moon.REFERENCE_NEW_MOON_MS - 3 * DAY);
        assertTrue(p > 0 && p < 1);
        assertTrue(Moon.waxing(Moon.phase(Moon.REFERENCE_NEW_MOON_MS + 3 * DAY)));
        assertTrue(!Moon.waxing(Moon.phase(Moon.REFERENCE_NEW_MOON_MS + 20 * DAY)));
    }
}
