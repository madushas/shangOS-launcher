package dev.madusha.shangos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

/**
 * Sun and Moon: where the sun is on its arc between today's sunrise and sunset (the city you chose
 * in Settings, else the time zone's), the two times, and the moon's phase with its lit part. All of
 * it is computed on the TV: no network, no permission.
 */
final class SunMoonWidget extends Widget {
    private static final long MINUTE = 60_000L;
    private static final int PAD = 18;

    private final Daylight daylight;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Arc arc;
    private final MoonDisc moon;
    private final TextView sunrise, sunset, phase;
    private final Runnable tick =
            new Runnable() {
                public void run() {
                    update();
                    handler.postDelayed(this, MINUTE);
                }
            };

    SunMoonWidget(Context context, WidgetBoard.Host host, float width) {
        super(context, host, width);
        // The card itself takes focus, so a page of cards without buttons can still be left.
        setFocusable(true);
        setFocusableInTouchMode(true);
        setSoundEffectsEnabled(false);
        setOnFocusChangeListener(
                (v, f) -> {
                    if (!f) {
                        setBackground(restBackground());
                        return;
                    }
                    // The same highlight as the weather card: translucent with an edge, so the text
                    // keeps its colour.
                    android.graphics.drawable.GradientDrawable g =
                            ui.shape(light() ? 0xF2FFFFFF : 0x38FFFFFF, Metrics.CARD);
                    g.setStroke(Math.max(1, ui.px(1.2f)), light() ? 0x33000000 : 0xB3FFFFFF);
                    setBackground(g);
                });
        daylight = new Daylight(context, host.prefs());
        TextView title = text("Sun and Moon", 13, ink(0xE6), "sans-serif-medium");
        addView(title, pos(w - 2 * PAD, 16, PAD, 14));
        arc = new Arc(context);
        addView(arc, pos(w - 2 * PAD, 58, PAD, 36));
        sunrise = text("", 12.5f, ink(0xB3), "sans-serif");
        addView(sunrise, pos((w - 2 * PAD) / 2, 16, PAD, 100));
        sunset = text("", 12.5f, ink(0xB3), "sans-serif");
        sunset.setGravity(android.view.Gravity.RIGHT);
        addView(sunset, pos((w - 2 * PAD) / 2, 16, w / 2, 100));
        moon = new MoonDisc(context);
        addView(moon, pos(40, 40, PAD, 124));
        phase = text("", 14, ink(0xFF), "sans-serif-medium");
        addView(phase, pos(w - 2 * PAD - 52, 20, PAD + 52, 128));
        update();
    }

    @Override
    void setActive(boolean value) {
        boolean was = active;
        super.setActive(value);
        handler.removeCallbacks(tick);
        if (value && !was) {
            update();
            handler.postDelayed(tick, MINUTE);
        }
    }

    @Override
    void refresh() {
        update();
    }

    private void update() {
        long now = System.currentTimeMillis();
        long[] sun = daylight.times(now);
        arc.set(sun[0], sun[1], now, daylight.isDay(now));
        sunrise.setText("Sunrise  " + Formats.time(getContext(), sun[0]));
        sunset.setText("Sunset  " + Formats.time(getContext(), sun[1]));
        double p = Moon.phase(now);
        moon.set(p);
        int percent = (int) Math.round(Moon.lit(p) * 100);
        phase.setText(Moon.name(p) + "\n" + percent + "% lit");
    }

    /** The sun on a half-circle from sunrise (left) to sunset (right). */
    private final class Arc extends View {
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sun = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private final Path path = new Path();
        private float t = -1;
        private boolean day;

        Arc(Context context) {
            super(context);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(ui.px(2));
            line.setStrokeCap(Paint.Cap.ROUND);
            sun.setStyle(Paint.Style.FILL);
        }

        void set(long rise, long set, long now, boolean isDay) {
            day = isDay;
            t = set > rise && now >= rise && now <= set ? (now - rise) / (float) (set - rise) : -1;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Math.min(w / 2 - ui.px(10), h - ui.px(6));
            float cx = w / 2, cy = h - ui.px(6);
            oval.set(cx - r, cy - r, cx + r, cy + r);
            line.setColor(ink(0x40));
            path.reset();
            path.addArc(oval, 180, 180);
            c.drawPath(path, line);
            // Where the sun is now: on the arc while the day runs, on the horizon at night.
            float angle = t >= 0 ? (float) Math.PI * (1 - t) : 0;
            float x = cx + r * (float) Math.cos(angle), y = cy - r * (float) Math.sin(angle);
            if (t < 0) {
                x = cx;
                y = cy;
            }
            sun.setColor(day ? 0xFFFFC857 : ink(0x66));
            c.drawCircle(x, y, ui.px(day ? 7 : 4), sun);
        }
    }

    /** The moon's disc: the lit part on one side, with the edge drawn as a soft half-ellipse. */
    private static final class MoonDisc extends View {
        private final Paint lit = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private final Path path = new Path();
        private double phase;
        private final int dim;

        MoonDisc(Context context) {
            super(context);
            lit.setColor(0xFFF2EEDD);
            dim = 0x33FFFFFF;
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        void set(double p) {
            phase = p;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f, cx = w / 2, cy = h / 2;
            dark.setColor(dim);
            c.drawCircle(cx, cy, r, dark);
            double k = Moon.lit(phase);
            boolean waxing = Moon.waxing(phase);
            float edge = (float) Math.abs(2 * k - 1) * r;
            oval.set(cx - r, cy - r, cx + r, cy + r);
            // The lit half-disc on the lit side, then the terminator: an ellipse that adds lit
            // area when past half (gibbous) and takes it away before half (crescent).
            path.reset();
            path.addArc(oval, waxing ? -90 : 90, 180);
            path.close();
            c.drawPath(path, lit);
            path.reset();
            boolean bulgeLit = k > .5;
            RectF e = new RectF(cx - edge, cy - r, cx + edge, cy + r);
            path.addArc(e, waxing ? (bulgeLit ? 90 : -90) : (bulgeLit ? -90 : 90), 180);
            path.close();
            c.drawPath(path, bulgeLit ? lit : dark);
        }
    }
}
