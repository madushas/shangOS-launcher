package dev.madusha.shangos;

import java.util.Locale;

/**
 * Adds up what the frame timings of a window say, to answer one question: when frames are late, is
 * the main thread late (layout, draw, animation callbacks) or the GPU? Durations are in
 * nanoseconds. Pure, so it is unit tested; {@link FrameProbe} feeds it on debug builds.
 */
final class FrameStats {
    private int frames, janky, uiBound, gpuBound;
    private long total, input, animation, layout, draw, sync, gpu, worst;

    /**
     * One frame. {@code interval} is the display's frame time: a frame whose total is longer is
     * janky. {@code gpuNanos} is 0 when the GPU time is not known.
     */
    void add(
            long totalNanos,
            long inputNanos,
            long animationNanos,
            long layoutNanos,
            long drawNanos,
            long syncNanos,
            long gpuNanos,
            long interval) {
        frames++;
        total += totalNanos;
        input += inputNanos;
        animation += animationNanos;
        layout += layoutNanos;
        draw += drawNanos;
        sync += syncNanos;
        gpu += gpuNanos;
        worst = Math.max(worst, totalNanos);
        if (totalNanos <= interval) return;
        janky++;
        long ui = inputNanos + animationNanos + layoutNanos + drawNanos + syncNanos;
        if (ui > interval) uiBound++;
        else if (gpuNanos > interval) gpuBound++;
    }

    int frames() {
        return frames;
    }

    int janky() {
        return janky;
    }

    /** Percent of frames that were late, 0 with no frames. */
    float jankPercent() {
        return frames == 0 ? 0 : 100f * janky / frames;
    }

    void reset() {
        frames = janky = uiBound = gpuBound = 0;
        total = input = animation = layout = draw = sync = gpu = worst = 0;
    }

    /** One line for the log: the share of late frames, why, and where the time goes. */
    String summary() {
        if (frames == 0) return "no frames";
        return String.format(
                Locale.ROOT,
                "frames=%d janky=%.1f%% (main thread %d, gpu %d, other %d) avg ms: total %.1f"
                        + " input %.1f anim %.1f layout %.1f draw %.1f sync %.1f gpu %.1f worst"
                        + " %.1f",
                frames,
                jankPercent(),
                uiBound,
                gpuBound,
                janky - uiBound - gpuBound,
                ms(total),
                ms(input),
                ms(animation),
                ms(layout),
                ms(draw),
                ms(sync),
                ms(gpu),
                worst / 1e6f);
    }

    private float ms(long sum) {
        return sum / 1e6f / frames;
    }
}
