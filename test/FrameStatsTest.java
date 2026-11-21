package dev.madusha.shangos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FrameStatsTest {
    private static final long MS = 1_000_000L, VSYNC = 16_666_667L;

    @Test
    public void noFramesIsNotAnError() {
        FrameStats s = new FrameStats();
        assertEquals(0f, s.jankPercent(), 0f);
        assertEquals("no frames", s.summary());
    }

    @Test
    public void countsLateFramesAndSaysWhoWasLate() {
        FrameStats s = new FrameStats();
        for (int i = 0; i < 6; i++) s.add(8 * MS, MS, MS, MS, 2 * MS, MS, 4 * MS, VSYNC);
        s.add(30 * MS, MS, 2 * MS, 12 * MS, 12 * MS, 2 * MS, 5 * MS, VSYNC); // the main thread
        s.add(25 * MS, MS, MS, MS, 2 * MS, MS, 24 * MS, VSYNC); // the GPU
        s.add(18 * MS, MS, MS, MS, 2 * MS, MS, 6 * MS, VSYNC); // neither alone
        assertEquals(9, s.frames());
        assertEquals(3, s.janky());
        assertEquals(100f / 3, s.jankPercent(), .01f);
        String line = s.summary();
        assertTrue(line, line.contains("main thread 1, gpu 1, other 1"));
        assertTrue(line, line.contains("worst 30.0"));
    }

    @Test
    public void resetStartsAgain() {
        FrameStats s = new FrameStats();
        s.add(30 * MS, MS, MS, MS, MS, MS, MS, VSYNC);
        s.reset();
        assertEquals(0, s.frames());
        assertEquals("no frames", s.summary());
    }
}
