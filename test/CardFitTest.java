package dev.madusha.shangos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class CardFitTest {
    private static final float ROW = 864, GAP = 16, MAX = 560;

    private static double pref(String id) {
        switch (id) {
            case "weather": return 288;
            case "upnext": return 560;
            case "sunmoon":
            case "glance": return 300;
            default: return 424;
        }
    }

    private static boolean grows(String id) {
        return id.equals("sunmoon") || id.equals("glance");
    }

    private static float total(float[] w) {
        float t = GAP * (w.length - 1);
        for (float v : w) t += v;
        return t;
    }

    @Test
    public void twoGrowableCardsFillTheRowWhenWeatherIsOff() {
        // The offline build: Sun and Moon and System alone on a page used to leave a gap.
        List<String> page = Arrays.asList("sunmoon", "glance");
        float[] w = CardFit.widths(page, CardFitTest::pref, CardFitTest::grows, ROW, GAP, MAX);
        assertEquals(ROW, total(w), 0.5f);
        assertEquals(w[0], w[1], 0.5f);
    }

    @Test
    public void aFixedCardIsNeverStretched() {
        List<String> page = Arrays.asList("weather", "sunmoon");
        float[] w = CardFit.widths(page, CardFitTest::pref, CardFitTest::grows, ROW, GAP, MAX);
        assertEquals(288, w[0], 0.01f);
        assertEquals(ROW - 288 - GAP, w[1], 0.5f);
    }

    @Test
    public void aLoneGrowableCardIsCappedAndCentred() {
        List<String> page = Arrays.asList("glance");
        float[] w = CardFit.widths(page, CardFitTest::pref, CardFitTest::grows, ROW, GAP, MAX);
        assertEquals(MAX, w[0], 0.01f);
        assertEquals((ROW - MAX) / 2, CardFit.centred(w, 0, ROW, GAP), 0.01f);
    }

    @Test
    public void nothingIsWiderThanTheRow() {
        List<String> page = Arrays.asList("upnext", "weather");
        float[] w = CardFit.widths(page, CardFitTest::pref, CardFitTest::grows, ROW, GAP, MAX);
        assertTrue(total(w) <= ROW + 0.01f);
    }
}
