package dev.madusha.shangos;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * The parts that stay while pages change: the tab bar with its sliding pill, the clock, and the
 * shade that darkens the wallpaper behind Settings.
 */
final class Chrome {
    /** The pages, in tab order. */
    static final String[] PAGES = {"home", "apps", "settings"};

    private static final String[] TITLES = {"Home", "Apps", "Settings"};

    /** What the chrome needs from the launcher. */
    interface Host {
        boolean motion();

        /** True on a pale wallpaper (dark text). */
        boolean light();

        /** The sound of choosing something. */
        void click();

        /** The page being shown: one of {@link #PAGES}. */
        String page();

        /** Shows {@code page}; {@code focus}: its content takes focus, else the bar keeps it. */
        void showPage(String page, boolean focus);

        /** Home's first widget, or null when Home has none. */
        View firstWidget();

        /** Focus the first (or the selected) app tile. */
        void focusFirst();

        /** The power button was chosen: restart or shut down, after a confirmation. */
        void power();
    }

    private final Context context;
    private final Ui ui;
    private final Handler handler;
    private final Host host;
    private final ArrayList<TextView> tabs = new ArrayList<>();
    private final FrameLayout bar;
    private final View indicator;
    private final GradientDrawable indicatorShape;
    private final TextView clock;
    private final StatusIcons status;
    private final LinearLayout capsule;
    private PowerButton power;
    private final View shade;
    private ValueAnimator indicatorMove;
    private int indicatorColor = 0x24FFFFFF;
    private boolean clockRunning, calm;

    private final Runnable clockTick =
            new Runnable() {
                public void run() {
                    if (!clockRunning) return;
                    clock.setText(Formats.time(context, System.currentTimeMillis()));
                    status.refresh();
                    handler.postDelayed(this, 15000);
                }
            };

    Chrome(Context context, Ui ui, Handler handler, Host host) {
        this.context = context;
        this.ui = ui;
        this.handler = handler;
        this.host = host;
        shade = new View(context);
        shade.setBackgroundColor(0x80000000);
        shade.setAlpha(0);
        LinearLayout navigation = new LinearLayout(context);
        navigation.setGravity(Gravity.CENTER);
        for (int i = 0; i < PAGES.length; i++) {
            TextView tab = newTab(i);
            tabs.add(tab);
            navigation.addView(tab, new LinearLayout.LayoutParams(-2, ui.px(40)));
        }
        bar = new FrameLayout(context);
        bar.setClipChildren(false);
        indicatorShape = ui.shape(0x24FFFFFF, 18);
        indicator = new View(context);
        indicator.setBackground(indicatorShape);
        bar.addView(indicator, new FrameLayout.LayoutParams(0, ui.px(40), Gravity.CENTER_VERTICAL));
        bar.addView(navigation, new FrameLayout.LayoutParams(-2, -1));
        bar.setPadding(ui.px(4), ui.px(2), ui.px(4), ui.px(2));
        bar.setBackground(ui.glass(bar, false, Metrics.CARD));
        status = new StatusIcons(context, handler, Prefs.of(context));
        capsule = new LinearLayout(context);
        capsule.setOrientation(LinearLayout.HORIZONTAL);
        capsule.setGravity(Gravity.CENTER_VERTICAL);
        capsule.setPadding(ui.px(12), 0, ui.px(8), 0);
        capsule.setClipChildren(false);
        clock = ui.text("", 16, 0xF2FFFFFF, "sans-serif-medium");
        clock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        clock.setLetterSpacing(.01f);
    }

    private TextView newTab(int index) {
        TextView tab = ui.text(TITLES[index], 16, 0xB3FFFFFF, "sans-serif-medium");
        tab.setPadding(ui.px(20), ui.px(10), ui.px(20), ui.px(10));
        tab.setGravity(Gravity.CENTER);
        tab.setFocusable(true);
        tab.setFocusableInTouchMode(true);
        tab.setSoundEffectsEnabled(false);
        tab.setOnClickListener(
                v -> {
                    host.click();
                    host.showPage(PAGES[index], true);
                });
        // One pill slides between tabs; leaving the bar it settles, translucent, on the current
        // page.
        tab.setOnFocusChangeListener(
                (v, focus) -> {
                    if (focus) moveIndicator(true);
                    else
                        tab.post(
                                () -> {
                                    if (focusedTab() < 0) moveIndicator(true);
                                });
                });
        tab.setOnKeyListener(
                (v, key, e) -> {
                    if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
                    // Like tabs: Left and Right show the neighbouring page at once, the focus stays
                    // on the bar; Down goes into the page.
                    if (key == KeyEvent.KEYCODE_DPAD_RIGHT && index == PAGES.length - 1) {
                        power.requestFocus();
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        int to = index + (key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                        if (to >= 0 && to < PAGES.length && e.getRepeatCount() == 0) {
                            tabs.get(to).requestFocus();
                            host.showPage(PAGES[to], false);
                        }
                        return true;
                    }
                    if (key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
                    View widget = index == 0 ? host.firstWidget() : null;
                    if (!host.page().equals(PAGES[index])) host.showPage(PAGES[index], true);
                    else if (widget != null) widget.requestFocus();
                    else host.focusFirst();
                    return true;
                });
        return tab;
    }

    /** The darkening layer that goes between the wallpaper and the page. */
    View shade() {
        return shade;
    }

    /** The tab bar, for the parts that fade it with the page. */
    FrameLayout bar() {
        return bar;
    }

    View tab(int index) {
        return tabs.get(index);
    }

    /** The tab of the page being shown. */
    View pageTab() {
        return tabs.get(pageTabIndex());
    }

    /** Puts the tab bar (centred at the top) and the clock (top right) over {@code root}. */
    void addTo(FrameLayout root) {
        root.addView(bar, ui.pos(-2, 48, 0, 24));
        bar.post(
                () -> {
                    bar.setTranslationX(
                            (Screen.width(context) - bar.getWidth())
                                    / 2f);
                    moveIndicator(false);
                });
        // The clock and its status icons share one glass capsule, the same height as the tab bar,
        // at the top right: the icons sit just before the time.
        capsule.addView(status, new LinearLayout.LayoutParams(-2, -1));
        clock.setSingleLine(true);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-2, -1);
        cl.leftMargin = ui.px(6);
        capsule.addView(clock, cl);
        power = new PowerButton(context);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ui.px(28), ui.px(28));
        pl.leftMargin = ui.px(10);
        capsule.addView(power, pl);
        FrameLayout.LayoutParams fp =
                new FrameLayout.LayoutParams(-2, ui.px(36), Gravity.TOP | Gravity.RIGHT);
        fp.rightMargin = ui.px(48);
        fp.topMargin = ui.px(30);
        root.addView(capsule, fp);
    }

    /** Colours of the tab bar, clock and shade for the current wallpaper. */
    void paint() {
        boolean light = host.light();
        bar.setBackground(ui.glass(bar, light, Metrics.CARD));
        capsule.setBackground(ui.glass(capsule, light, Metrics.CARD));
        clock.setTextColor(Theme.ink(light, 0xF2));
        status.setInk(Theme.ink(light, 0xE6));
        ui.shadow(clock, 6, light);
        shade.setBackgroundColor(light ? 0x73F2F2F5 : 0x80000000);
    }

    /**
     * Calm: the tab bar and clock fade back (and the bar drifts up a little); false brings them
     * back. Quick to return, slow to leave.
     */
    void setCalm(boolean value) {
        if (calm == value) return;
        calm = value;
        long ms = value ? 1400 : 180;
        bar.animate()
                .alpha(value ? .22f : 1)
                .translationY(value ? -ui.px(4) : 0)
                .setDuration(host.motion() ? ms : 0)
                .setInterpolator(Motion.EASE)
                .start();
        capsule.animate()
                .alpha(value ? .45f : 1)
                .setDuration(host.motion() ? ms : 0)
                .setInterpolator(Motion.EASE)
                .start();
    }

    void setClockVisible(boolean visible) {
        clock.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Shows the time now and every 15 seconds until {@link #stopClock}. */
    void startClock() {
        handler.removeCallbacks(clockTick);
        clockRunning = true;
        handler.post(clockTick);
        status.start();
    }

    void stopClock() {
        clockRunning = false;
        handler.removeCallbacks(clockTick);
        status.stop();
    }

    private int focusedTab() {
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).hasFocus()) return i;
        return -1;
    }

    private int pageTabIndex() {
        String page = host.page();
        return page.equals("apps") ? 1 : page.equals("settings") ? 2 : 0;
    }

    /**
     * Slides the tab pill under the focused tab (white), or the current page's tab (glass) when
     * focus is elsewhere.
     */
    void moveIndicator(boolean animate) {
        if (tabs.isEmpty()) return;
        boolean light = host.light();
        int focused = focusedTab(), current = pageTabIndex();
        int index = focused >= 0 ? focused : current;
        View target = tabs.get(index), row = (View) target.getParent();
        if (target.getWidth() == 0) {
            target.post(() -> moveIndicator(false));
            return;
        }
        final float fromX = indicator.getTranslationX(),
                toX = row.getLeft() + target.getLeft() - ((View) row.getParent()).getPaddingLeft();
        final int fromW = indicator.getLayoutParams().width,
                toW = target.getWidth(),
                fromColor = indicatorColor,
                toColor =
                        focused >= 0
                                ? (light ? 0xFF1D1D1F : 0xF5FFFFFF)
                                : (light ? 0xE6FFFFFF : 0x24FFFFFF);
        final int[] fromText = new int[tabs.size()], toText = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            fromText[i] = tabs.get(i).getCurrentTextColor();
            toText[i] =
                    i == focused
                            ? (light ? Color.WHITE : 0xFF17202B)
                            : Theme.ink(light, i == current ? 0xFF : 0xA6);
        }
        if (indicatorMove != null) indicatorMove.cancel();
        ArgbEvaluator argb = new ArgbEvaluator();
        ValueAnimator.AnimatorUpdateListener step =
                a -> {
                    float f = (float) a.getAnimatedValue();
                    indicator.setTranslationX(fromX + (toX - fromX) * f);
                    ViewGroup.LayoutParams lp = indicator.getLayoutParams();
                    lp.width = Math.round(fromW + (toW - fromW) * f);
                    indicator.setLayoutParams(lp);
                    indicatorColor = (int) argb.evaluate(f, fromColor, toColor);
                    indicatorShape.setColor(indicatorColor);
                    // The text flips quickly, so it never matches the pill mid-way.
                    float t = Math.max(0, Math.min(1, (f - .12f) / .3f));
                    for (int i = 0; i < tabs.size(); i++)
                        tabs.get(i)
                                .setTextColor((int) argb.evaluate(t, fromText[i], toText[i]));
                };
        indicatorMove = ValueAnimator.ofFloat(0, 1);
        indicatorMove.addUpdateListener(step);
        if (!animate || !host.motion() || fromW == 0) {
            indicatorMove.setDuration(0);
        } else {
            indicatorMove.setDuration(280);
            indicatorMove.setInterpolator(Motion.EASE);
        }
        indicatorMove.start();
    }

    /** The power button at the end of the status capsule: a circle with a gap and a stroke. */
    private final class PowerButton extends View {
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();

        PowerButton(Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setSoundEffectsEnabled(false);
            setContentDescription("Power");
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeCap(Paint.Cap.ROUND);
            setOnFocusChangeListener((v, f) -> invalidate());
            setOnClickListener(v -> host.power());
            setOnKeyListener(
                    (v, key, e) -> {
                        if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
                        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                            tabs.get(PAGES.length - 1).requestFocus();
                            return true;
                        }
                        return key == KeyEvent.KEYCODE_DPAD_RIGHT;
                    });
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), u = Math.min(w, h) / 28f;
            if (hasFocus()) {
                ring.setStyle(Paint.Style.FILL);
                ring.setColor(host.light() ? 0xFF1D1D1F : 0xF5FFFFFF);
                c.drawCircle(w / 2, h / 2, Math.min(w, h) / 2, ring);
                ring.setColor(host.light() ? Color.WHITE : 0xFF17202B);
            } else {
                ring.setColor(Theme.ink(host.light(), 0xE6));
            }
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(2.2f * u);
            oval.set(w / 2 - 8 * u, h / 2 - 7 * u, w / 2 + 8 * u, h / 2 + 9 * u);
            c.drawArc(oval, -60, 300, false, ring);
            c.drawLine(w / 2, h / 2 - 10 * u, w / 2, h / 2 - 1 * u, ring);
        }
    }
}
