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

    @Test
    public void cardsAndTilesShareOneCornerRadius() {
        // Up Next shows app tiles on a card: a smaller radius of its own made them look odd.
        assertEquals(Metrics.RADIUS, Metrics.CARD, 0f);
        assertTrue("a control sits inside a card", Metrics.CONTROL < Metrics.CARD);
    }

    @Test
    public void textKeepsItsSizeOnAnAndroidTvAndGrowsOnlyWhenAskedTo() {
        // A TV draws 960 dp across, so one design unit is one dp: nothing changes.
        assertEquals(1f, Metrics.textScale(2f, 2f), 0f); // 1080p, 320 dpi
        assertEquals(1f, Metrics.textScale(4f, 4f), 0f); // 4K, 640 dpi
        assertEquals(1f, Metrics.textScale(1.3333f, 1.33125f), 0f); // 720p stick, 213 dpi: 1.0
        // A bigger Display size means fewer dp across: text grows, but only so far.
        assertEquals(1.1f, Metrics.textScale(2f, 2.2f), 1e-4f);
        assertEquals(Metrics.MAX_TEXT_SCALE, Metrics.textScale(2f, 4f), 0f);
        // A screen with more dp than the design is never shrunk below it.
        assertEquals(1f, Metrics.textScale(2f, 1f), 0f);
        assertEquals(1f, Metrics.textScale(0f, 2f), 0f);
    }

    @Test
    public void textNeverGoesBelowTheFloorAndIsUnchangedFromSeventyTwentyUp() {
        float unit720 = 1280 / 960f;
        assertEquals(14, Metrics.textPx(Metrics.MICRO - .5f, unit720, 1f)); // 10.5 units
        assertEquals(Math.round(Metrics.BODY * unit720), Metrics.textPx(Metrics.BODY, unit720, 1f));
        assertEquals(30, Metrics.textPx(Metrics.BODY, 2f, 1f)); // 1080p
        assertEquals(60, Metrics.textPx(Metrics.BODY, 4f, 1f)); // 4K
        // A small picture (854 px wide, unit 0.89): the caption stays readable.
        assertEquals(Metrics.MIN_TEXT_PX, Metrics.textPx(Metrics.CAPTION, 854 / 960f, 1f));
        assertEquals(33, Metrics.textPx(Metrics.BODY, 2f, 1.1f)); // 15 * 1.1 * 2
    }

    @Test
    public void theTypeScaleOnlyGoesDown() {
        float[] scale = {
            Metrics.DISPLAY,
            Metrics.TITLE,
            Metrics.HEADING,
            Metrics.BODY,
            Metrics.CAPTION,
            Metrics.MICRO
        };
        for (int i = 1; i < scale.length; i++) assertTrue(scale[i] < scale[i - 1]);
    }

    @Test
    public void tileNamesFollowTheTileWidthWithinReadableBounds() {
        Metrics.apply(Metrics.COMFORTABLE);
        assertEquals(13.5f, Metrics.labelSize(), 0f);
        Metrics.apply(Metrics.COMPACT);
        assertEquals(11.5f, Metrics.labelSize(), 0f);
        Metrics.apply(Metrics.COZY);
        assertEquals(15f, Metrics.labelSize(), 0f);
        Metrics.apply(Metrics.COMFORTABLE);
    }
}
