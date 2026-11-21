package dev.madusha.shangos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FrostTest {
    private static int gray(int v) {
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    @Test
    public void aFlatPictureStaysFlat() {
        int[] px = new int[16 * 9];
        java.util.Arrays.fill(px, gray(120));
        Frost.blur(px, 16, 9, 3, 3);
        for (int p : px) assertEquals(gray(120), p);
    }

    @Test
    public void aCheckerboardBecomesSmooth() {
        int w = 24, h = 24;
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) px[i] = ((i % w + i / w) % 2 == 0) ? gray(0) : gray(255);
        Frost.blur(px, w, h, 2, 3);
        int min = 255, max = 0;
        for (int y = 6; y < h - 6; y++)
            for (int x = 6; x < w - 6; x++) {
                int v = px[y * w + x] & 255;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        assertTrue("spread " + (max - min), max - min < 40);
    }

    @Test
    public void lightIsKeptNotCreatedOrLost() {
        int w = 30, h = 20;
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) px[i] = gray(i % w < 15 ? 40 : 200);
        long before = 0, after = 0;
        for (int p : px) before += p & 255;
        Frost.blur(px, w, h, 2, 3);
        for (int p : px) after += p & 255;
        // Edges repeat, so the total changes only a little.
        assertTrue(Math.abs(after - before) < before / 50);
    }

    @Test
    public void channelsAreBlurredSeparately() {
        int[] px = new int[8 * 8];
        java.util.Arrays.fill(px, 0xFFFF0000);
        Frost.blur(px, 8, 8, 2, 2);
        assertEquals(0xFFFF0000, px[27]);
    }

    @Test
    public void theVeilKeepsItsColourAndOnlyChangesOpacity() {
        int veil = 0x80FF8040;
        assertEquals("medium is the look before 0.6.9", veil, Frost.veil(veil, Frost.MEDIUM));
        int soft = Frost.veil(veil, Frost.SOFT), deep = Frost.veil(veil, Frost.DEEP);
        assertEquals(0xFF8040, soft & 0xFFFFFF);
        assertEquals(0xFF8040, deep & 0xFFFFFF);
        assertTrue("soft shows more wallpaper", (soft >>> 24) < 0x80);
        assertTrue("deep shows less", (deep >>> 24) > 0x80);
    }

    @Test
    public void theVeilNeverOverflowsAndUnknownStrengthsAreMedium() {
        int dark = Frost.veil(0xD01C1C1E, Frost.DEEP);
        assertEquals("deep still lets some wallpaper through", 0xF0, dark >>> 24);
        int past = Frost.veil(0xF8102030, Frost.DEEP);
        assertEquals("a veil already past the cap keeps its own", 0xF8, past >>> 24);
        assertEquals(0xF8, Frost.veil(0xF8102030, Frost.MEDIUM) >>> 24);
        assertEquals(0x30, Frost.veil(0x30FFFFFF, 7) >>> 24);
        assertEquals(0x30, Frost.veil(0x30FFFFFF, -1) >>> 24);
        assertEquals("a clear veil stays clear", 0, Frost.veil(0x00FFFFFF, Frost.DEEP) >>> 24);
    }
}
