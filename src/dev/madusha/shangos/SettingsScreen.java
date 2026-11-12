package dev.madusha.shangos;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * The Settings page, in the usual settings layout: the sections down the left, and on the right one
 * panel with the section's title, a sentence about it and its rows, with help for the row in focus
 * underneath. Moving down the sections shows each one at once; Right enters the rows, Left comes
 * back. A row is a value with a chevron or a switch bound to a preference; the dialogs they open
 * (wallpaper, name, city, ...) are separate classes, and {@link SettingsPages} builds the rows.
 */
final class SettingsScreen {
    /** What Settings needs from the launcher, besides the shared {@link ScreenHost}. */
    interface Host {
        /** True on a pale wallpaper (dark text). */
        boolean light();

        /** The frame the page is drawn into. */
        FrameLayout content();

        /** The Settings tab: focus moves there when going up from the first row. */
        View settingsTab();

        void clockVisible(boolean visible);

        /** The living wallpaper or the performance choice changed. */
        void ambientChanged();

        /** Frosted glass was switched on or off. */
        void glassChanged();

        /** Rebuild Settings and select row {@code row} of the section shown. */
        void reload(int row);

        /** "Auto (Light)": the performance choice and what Auto picked. */
        String performanceLabel();
    }

    // The sections, in the order of the list on the left.
    private static final int APPEARANCE = 0, MOTION = 1, HOME = 2, PRIVACY = 3, SYSTEM = 4;
    private static final String[] TITLES = {
        "Appearance", "Motion and sound", "Home", "Privacy", "System"
    };
    private static final String[] ABOUT = {
        "The wallpaper, glass and the greeting on Home.",
        "How things move and sound, and how hard the TV works.",
        "The weather, widgets and how your apps are arranged.",
        "What shangOS can reach, and what never leaves this TV.",
        "Screen saver, home screen, setup and about."
    };

    // Layout, in the launcher's 960 x 540 units, on an 8 unit grid.
    private static final int RAIL_X = 48, RAIL_W = 224, ITEM_H = 48, ITEM_GAP = 8, TOP = 112;
    private static final int PANEL_X = 304, PANEL_W = 608, PANEL_H = 384, ROW_H = 56;

    private final Activity activity;
    private final Ui ui;
    private final Prefs prefs;
    private final ScreenHost screens;
    private final Host host;
    private final PrivacyScreen privacy;
    private final SettingsPages pages;

    private int group = APPEARANCE;
    private final ArrayList<View> rail = new ArrayList<>();
    private ArrayList<View> rows = new ArrayList<>();
    private LinearLayout list;
    private ScrollView scroll;
    private TextView title, about, help;
    private ImageView helpImage;
    private ValueAnimator glide;
    private int lastRow;

    SettingsScreen(
            Activity activity,
            Ui ui,
            Prefs prefs,
            ScreenHost screens,
            Host host,
            PrivacyScreen privacy,
            SettingsPages pages) {
        this.activity = activity;
        this.ui = ui;
        this.prefs = prefs;
        this.screens = screens;
        this.host = host;
        this.privacy = privacy;
        this.pages = pages;
    }

    /** Another page is shown: forget the views. */
    void clear() {
        rail.clear();
        rows = new ArrayList<>();
    }

    /** The row of Privacy's one entry, where its screen returns to. */
    int privacyRow() {
        return 0;
    }

    int aboutRow() {
        return pages.aboutRow();
    }

    /** Focus goes to the list of sections (from the tab bar's Down key). */
    void focusSections() {
        if (group < rail.size()) rail.get(group).requestFocus();
    }

    /** Selects row {@code i} of the section shown once the page is laid out. */
    void focusRow(int i) {
        screens.post(
                () -> {
                    if (i < rows.size()) rows.get(i).requestFocus();
                    else focusSections();
                });
    }

    /**
     * Back from a row goes to its section in the list; true if it did. Back from the list leaves
     * Settings (the launcher does that).
     */
    boolean back() {
        View now = activity.getCurrentFocus();
        if (now == null || !rows.contains(now)) return false;
        focusSections();
        return true;
    }

    /** A switch row's state: "motion" also follows the system's animation setting. */
    private boolean isOn(String toggle) {
        return toggle.equals("motion") ? screens.motion() : prefs.flag(toggle);
    }

    private int style() {
        return Wallpaper.choice(prefs);
    }

    private int ink(int alpha) {
        return Theme.ink(host.light(), alpha);
    }

    /** The help line for the row in focus, with a picture for the wallpaper rows. */
    private void preview(int thumb, String heading, String body) {
        if (help == null) return;
        if (thumb != 0) {
            if (thumb == Wallpaper.PHOTO_THUMB)
                helpImage.setImageBitmap(PhotoStore.thumbnail(activity));
            else helpImage.setImageResource(thumb);
            helpImage.setVisibility(View.VISIBLE);
        } else helpImage.setVisibility(View.GONE);
        help.setText(body);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) help.getLayoutParams();
        lp.leftMargin = ui.px(thumb != 0 ? 104 : 24);
        lp.width = ui.px(thumb != 0 ? 480 : 560);
        help.setLayoutParams(lp);
    }

    /** Builds the Settings page into the launcher's content frame. */
    void show(boolean focus) {
        FrameLayout content = host.content();
        rail.clear();
        for (int g = 0; g < TITLES.length; g++) {
            View item = sectionItem(g);
            rail.add(item);
            content.addView(
                    item, ui.pos(RAIL_W, ITEM_H, RAIL_X, TOP + g * (ITEM_H + ITEM_GAP)));
        }
        FrameLayout panel = new FrameLayout(activity);
        panel.setBackground(ui.glass(panel, host.light(), 24));
        content.addView(panel, ui.pos(PANEL_W, PANEL_H, PANEL_X, TOP));
        title = ui.text("", 28, ink(0xFF), "sans-serif-medium");
        panel.addView(title, ui.pos(560, 36, 24, 24));
        about = ui.text("", 15, ink(0xB3), "sans-serif");
        panel.addView(about, ui.pos(560, 24, 24, 64));
        // The list scrolls itself (reveal) so a focused row is never cut at the edge.
        scroll =
                new ScrollView(activity) {
                    @Override
                    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
                        return 0;
                    }
                };
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalFadingEdgeEnabled(true);
        scroll.setFadingEdgeLength(ui.px(24));
        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        panel.addView(scroll, ui.pos(PANEL_W - 16, 232, 8, 104));
        View rule = new View(activity);
        rule.setBackgroundColor(ink(0x1F));
        panel.addView(rule, ui.pos(PANEL_W - 48, 1, 24, 344 - 1));
        helpImage = new ImageView(activity);
        helpImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        helpImage.setBackground(ui.shape(0x1FFFFFFF, 6));
        helpImage.setClipToOutline(true);
        helpImage.setVisibility(View.GONE);
        panel.addView(helpImage, ui.pos(64, 36, 24, 350));
        help = ui.text("", 14, ink(0xD9), "sans-serif");
        help.setMaxLines(2);
        panel.addView(help, ui.pos(560, 32, 24, 352));
        build();
        paintSections();
        if (focus) screens.post(this::focusSections);
    }

    /** Fills the panel with the rows of the section shown. */
    private void build() {
        title.setText(TITLES[group]);
        about.setText(ABOUT[group]);
        list.removeAllViews();
        rows = new ArrayList<>();
        switch (group) {
            case APPEARANCE:
                pages.appearance(kit, list, rows);
                break;
            case MOTION:
                pages.motion(kit, list, rows);
                break;
            case HOME:
                pages.home(kit, list, rows);
                break;
            case PRIVACY:
                privacyRows();
                break;
            default:
                pages.system(kit, list, rows);
        }
        scroll.scrollTo(0, 0);
        preview(0, "", "");
        lastRow = 0;
    }

    private void privacyRows() {
        rows.add(
                option(
                        list,
                        "Privacy and permissions",
                        () -> PrivacyScreen.summary(prefs),
                        null,
                        () -> privacy.show(0),
                        () ->
                                preview(
                                        0,
                                        "Privacy",
                                        "What "
                                                + activity.getString(R.string.app_name)
                                                + " can reach, which permissions it has, and the"
                                                + " switch that turns the network off.")));
    }

    /** Shows another section's rows at once, with a short fade. */
    private void select(int target) {
        if (target == group) return;
        group = target;
        paintSections();
        build();
        list.setAlpha(0);
        list.setTranslationY(ui.px(8));
        list.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(screens.motion() ? 200 : 0)
                .setInterpolator(Motion.EASE)
                .start();
    }

    private void paintSections() {
        for (View item : rail) item.getOnFocusChangeListener().onFocusChange(item, item.hasFocus());
    }

    /** One entry of the list of sections. */
    private View sectionItem(int g) {
        FrameLayout item = new FrameLayout(activity);
        item.setFocusable(true);
        item.setFocusableInTouchMode(true);
        item.setSoundEffectsEnabled(false);
        item.setContentDescription(TITLES[g]);
        TextView name = ui.text(TITLES[g], 17, ink(0xFF), "sans-serif-medium");
        item.addView(name, ui.pos(RAIL_W - 40, 24, 20, 12));
        item.setOnFocusChangeListener(
                (v, f) -> {
                    boolean current = g == group;
                    boolean pale = host.light();
                    int mark = pale ? 0x33000000 : 0x29FFFFFF;
                    item.setBackground(
                            f
                                    ? ui.shape(pale ? 0xFF1D1D1F : 0xF5FFFFFF, 12)
                                    : current ? ui.shape(mark, 12) : null);
                    name.setTextColor(
                            f ? (pale ? Color.WHITE : 0xFF17202B) : ink(current ? 0xFF : 0xB3));
                    if (f && g != group) screens.step(g);
                    if (f) select(g);
                });
        item.setOnClickListener(v -> rowToFocus().requestFocus());
        item.setOnKeyListener(
                (v, key, e) -> {
                    if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (key == KeyEvent.KEYCODE_DPAD_UP) {
                        (g == 0 ? host.settingsTab() : rail.get(g - 1)).requestFocus();
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (g < rail.size() - 1) rail.get(g + 1).requestFocus();
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        rowToFocus().requestFocus();
                        return true;
                    }
                    return key == KeyEvent.KEYCODE_DPAD_LEFT;
                });
        return item;
    }

    /** The row to enter when going right from the list of sections. */
    private View rowToFocus() {
        return rows.isEmpty() ? rail.get(group) : rows.get(Math.min(lastRow, rows.size() - 1));
    }

    private final SettingsPages.Kit kit =
            new SettingsPages.Kit() {
                public View option(
                        LinearLayout parent,
                        String title,
                        Supplier<String> value,
                        String toggle,
                        Runnable action,
                        Runnable onFocus) {
                    return SettingsScreen.this.option(
                            parent, title, value, toggle, action, onFocus);
                }

                public void preview(int thumb, String title, String body) {
                    SettingsScreen.this.preview(thumb, title, body);
                }

                public int style() {
                    return SettingsScreen.this.style();
                }
            };

    /** Scrolls the list so the focused row sits clear of the faded edges. */
    private void reveal(View row) {
        ScrollView sv = scroll;
        if (sv == null || row.getParent() == null || row.getParent().getParent() != sv) return;
        ViewGroup column = (ViewGroup) row.getParent();
        int top = row.getTop(), bottom = row.getBottom(), margin = ui.px(16);
        int h = sv.getHeight(), y = sv.getScrollY();
        int max = Math.max(0, column.getHeight() - h), target = y;
        if (top - margin < y) target = top - margin;
        else if (bottom + margin > y + h) target = bottom + margin - h;
        target = Math.max(0, Math.min(max, target));
        if (glide != null) glide.cancel();
        if (target == y) return;
        if (!screens.motion()) {
            sv.scrollTo(0, target);
            return;
        }
        glide = ValueAnimator.ofInt(y, target);
        glide.setDuration(200);
        glide.setInterpolator(Motion.EASE);
        glide.addUpdateListener(a -> sv.scrollTo(0, (int) a.getAnimatedValue()));
        glide.start();
    }

    /**
     * A row of the panel: its name on the left and, on the right, a value with a chevron or a
     * switch bound to a preference. Rows are one panel with hairlines between them, not a card
     * each; the row in focus fills with the same bright pill as the tab bar.
     */
    private View option(
            LinearLayout parent,
            String label,
            Supplier<String> value,
            String toggle,
            Runnable action,
            Runnable onFocus) {
        FrameLayout row = new FrameLayout(activity);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setSoundEffectsEnabled(false);
        TextView name = ui.text(label, 17, Color.WHITE, "sans-serif-medium");
        row.addView(name, ui.pos(320, 24, 24, 16));
        TextView detail = null;
        if (value != null) {
            String text = value.get();
            detail = ui.text(text + (text.isEmpty() ? "›" : "   ›"), 15, 0xB3FFFFFF, "sans-serif");
            detail.setGravity(Gravity.RIGHT);
            row.addView(detail, ui.pos(240, 24, 312, 16));
        }
        final View track = new View(activity), knob = new View(activity);
        if (toggle != null) {
            row.addView(track, ui.pos(44, 26, 524, 15));
            row.addView(knob, ui.pos(22, 22, 526, 17));
        }
        final View rule = new View(activity);
        row.addView(rule, ui.pos(PANEL_W - 48 - 16, 1, 16, ROW_H - 1));
        final TextView valueView = detail;
        Runnable paint =
                () -> {
                    boolean f = row.hasFocus(), pale = host.light();
                    row.setBackground(f ? ui.shape(pale ? 0xFF1D1D1F : 0xF5FFFFFF, 12) : null);
                    int on = pale ? Color.WHITE : 0xFF17202B;
                    name.setTextColor(f ? on : ink(0xFF));
                    if (valueView != null)
                        valueView.setTextColor(f ? (pale ? 0xB3FFFFFF : 0xFF5B6472) : ink(0xA6));
                    rule.setBackgroundColor(ink(0x1F));
                    rule.setVisibility(f ? View.INVISIBLE : View.VISIBLE);
                    if (toggle != null) {
                        boolean enabled = isOn(toggle);
                        track.setBackground(
                                ui.shape(enabled ? 0xFF34C759 : f ? 0x66808A99 : ink(0x33), 13));
                        knob.setBackground(ui.shape(Color.WHITE, 11));
                        knob.setElevation(ui.px(1.5f));
                        float x = enabled ? ui.px(18) : 0;
                        knob.animate().cancel();
                        if (screens.motion())
                            knob.animate()
                                    .translationX(x)
                                    .setDuration(200)
                                    .setInterpolator(Motion.EASE)
                                    .start();
                        else knob.setTranslationX(x);
                    }
                };
        paint.run();
        row.setOnFocusChangeListener(
                (v, f) -> {
                    paint.run();
                    if (!f) return;
                    lastRow = parent.indexOfChild(row);
                    onFocus.run();
                    screens.step(lastRow);
                    reveal(row);
                });
        row.setOnClickListener(
                v -> {
                    screens.click();
                    if (toggle != null) {
                        boolean enabled = isOn(toggle);
                        prefs.setFlag(toggle, !enabled);
                        if (toggle.equals("clock")) host.clockVisible(!enabled);
                        paint.run();
                    }
                    action.run();
                });
        row.setContentDescription(label);
        row.setOnKeyListener(
                (v, key, e) -> {
                    if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int at = parent.indexOfChild(row);
                    if (key == KeyEvent.KEYCODE_DPAD_UP) {
                        (at == 0 ? host.settingsTab() : parent.getChildAt(at - 1)).requestFocus();
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (at < parent.getChildCount() - 1)
                            parent.getChildAt(at + 1).requestFocus();
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                        focusSections();
                        return true;
                    }
                    return key == KeyEvent.KEYCODE_DPAD_RIGHT;
                });
        parent.addView(row, new LinearLayout.LayoutParams(-1, ui.px(ROW_H)));
        return row;
    }
}
