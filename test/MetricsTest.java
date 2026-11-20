package dev.madusha.shangos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MetricsTest {
    private static float rowWidth() {
        return Metrics.columns * Metrics.tileW + (Metrics.columns - 1) * Metrics.gap;
    }

    @Test
    public void everyLayoutSizeFitsTheRowInsideTheMargins() {
        for (String size : new String[] {Metrics.COMPACT, Metrics.COMFORTABLE, Metrics.COZY}) {
            Metrics.apply(size);
            assertTrue(size + " too wide: " + rowWidth(), rowWidth() <= 860.5f);
        }
    }

    @Test
    public void tilesKeepSixteenByNineAndCountMatchesTheSize() {
        Metrics.apply(Metrics.COZY);
        assertEquals(4, Metrics.columns);
        assertEquals(Math.round(Metrics.tileW * 9 / 16f), Metrics.tileH, 0.01f);
        Metrics.apply(Metrics.COMPACT);
        assertEquals(6, Metrics.columns);
        Metrics.apply(Metrics.COMFORTABLE);
        assertEquals(5, Metrics.columns);
    }

    @Test
    public void rowPositionRunsFromMinusOneToPlusOneForEveryLayoutSize() {
        for (String size : new String[] {Metrics.COMPACT, Metrics.COMFORTABLE, Metrics.COZY}) {
            Metrics.apply(size);
            assertEquals(size, -1f, Metrics.rowPosition(0), 1e-6f);
            assertEquals(size, 1f, Metrics.rowPosition(Metrics.columns - 1), 1e-6f);
            assertEquals(
                    size + " second row starts at the left again",
                    -1f,
                    Metrics.rowPosition(Metrics.columns),
                    1e-6f);
        }
        Metrics.apply(Metrics.COMFORTABLE);
        assertEquals("the middle tile of five is centred", 0f, Metrics.rowPosition(2), 1e-6f);
        assertEquals(-2f, Metrics.fromRowCentre(0), 1e-6f);
    }

    @Test
    public void barsFollowSignalStrength() {
        assertEquals(4, Metrics.bars(-40));
        assertEquals(3, Metrics.bars(-60));
        assertEquals(2, Metrics.bars(-75));
        assertEquals(1, Metrics.bars(-90));
    }
}
