package dev.madusha.shangos;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaceTest {
    @Test
    public void theFirstMoveIsNeverRapid() {
        assertFalse(new Pace(450).rapid(0));
        assertFalse(new Pace(450).rapid(1_000_000));
    }

    @Test
    public void movesCloserThanTheWindowAreRapidAndTheNextOneMeasuresFromThem() {
        Pace pace = new Pace(450);
        assertFalse(pace.rapid(10_000));
        assertTrue(pace.rapid(10_200));
        assertTrue("measured from the last move, not the first", pace.rapid(10_600));
        assertFalse("a pause longer than the window ends it", pace.rapid(11_100));
        assertTrue(pace.rapid(11_100));
    }
}
