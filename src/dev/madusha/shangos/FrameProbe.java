package dev.madusha.shangos;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.FrameMetrics;
import android.view.Window;

/**
 * Debug builds only: logs once a second how many frames were late and whether the main thread or
 * the GPU was the cause ({@code adb logcat -s shangOS/frames}). It uses the platform's frame
 * metrics, so it costs almost nothing and runs on its own thread. Call {@link #attach} and {@link
 * #detach} from the window's life; both do nothing in release and offline builds.
 */
final class FrameProbe implements Window.OnFrameMetricsAvailableListener {
    private static final long REPORT_NANOS = 1_000_000_000L;

    private final FrameStats stats = new FrameStats();
    private long interval = 16_666_667L;
    private long since;
    private HandlerThread thread;
    private Window window;

    void attach(Window target, float refreshRate) {
        if (!Flavor.DEBUG || window != null) return;
        window = target;
        interval = (long) (1e9 / Math.max(24f, refreshRate));
        thread = new HandlerThread("frame-probe");
        thread.start();
        target.addOnFrameMetricsAvailableListener(this, new Handler(thread.getLooper()));
    }

    void detach() {
        if (window == null) return;
        window.removeOnFrameMetricsAvailableListener(this);
        window = null;
        thread.quit();
        thread = null;
    }

    @Override
    public void onFrameMetricsAvailable(Window w, FrameMetrics m, int dropped) {
        if (m.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1) return; // starting up, not scrolling
        stats.add(
                m.getMetric(FrameMetrics.TOTAL_DURATION),
                m.getMetric(FrameMetrics.INPUT_HANDLING_DURATION),
                m.getMetric(FrameMetrics.ANIMATION_DURATION),
                m.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION),
                m.getMetric(FrameMetrics.DRAW_DURATION),
                m.getMetric(FrameMetrics.SYNC_DURATION),
                m.getMetric(FrameMetrics.GPU_DURATION),
                interval);
        long now = m.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP);
        if (since == 0) since = now;
        if (now - since >= REPORT_NANOS) {
            Dbg.d("frames", stats.summary());
            stats.reset();
            since = now;
        }
    }
}
