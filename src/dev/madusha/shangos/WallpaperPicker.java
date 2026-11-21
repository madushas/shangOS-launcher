package dev.madusha.shangos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** Full-screen wallpaper picker: moving between thumbnails previews each wallpaper live. */
final class WallpaperPicker {
    /** Request code for the system image picker launched by {@link #browseForPhoto}. */
    static final int REQUEST_BROWSE_PHOTO = 7823;

    /** What the picker needs from the launcher. */
    interface Host {
        /** The page and the bars that fade out while the picker is open. */
        View content();

        View nav();

        View shade();

        Wallpaper wallpaper();

        /** Show Settings again with row {@code row} selected. */
        void reload(int row);

        /** The activity, used to start the system image picker. */
        Activity activity();
    }

    private final Context context;
    private final Ui ui;
    private final Prefs prefs;
    private final Overlays overlays;
    private final ScreenHost screens;
    private final Host host;

    WallpaperPicker(
            Context context,
            Ui ui,
            Prefs prefs,
            Overlays overlays,
            ScreenHost screens,
            Host host) {
        this.context = context;
        this.ui = ui;
        this.prefs = prefs;
        this.overlays = overlays;
        this.screens = screens;
        this.host = host;
    }

    private void fadeBackIn() {
        host.content().animate().alpha(1).setDuration(260).start();
        host.nav().animate().alpha(1).setDuration(260).start();
        host.shade().animate().alpha(1).setDuration(260).start();
    }

    /**
     * Launches the system image picker so the user can pick a local photo as the wallpaper.
     * The result is delivered to {@code MainActivity.onActivityResult} with
     * {@link #REQUEST_BROWSE_PHOTO}.
     */
    void browseForPhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        host.activity().startActivityForResult(intent, REQUEST_BROWSE_PHOTO);
    }

    /** Opens the picker; choosing returns to Settings with {@code settingsRow} selected. */
    void show(int settingsRow) {
        final int saved = Wallpaper.choice(prefs);
        final Wallpaper wallpaper = host.wallpaper();
        FrameLayout o = overlays.open(Color.TRANSPARENT);
        overlays.onCancel(
                () -> {
                    wallpaper.show(saved);
                    fadeBackIn();
                });
        host.content().animate().alpha(0).setDuration(240).start();
        host.nav().animate().alpha(0).setDuration(240).start();
        host.shade().animate().alpha(0).setDuration(300).start();
        View fade = new View(context);
        fade.setBackground(
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {0x00000000, 0x99000000, 0xE6000000}));
        o.addView(fade, ui.pos(960, 260, 0, 280));
        TextView name = ui.text(Wallpaper.NAMES[saved], 34, Color.WHITE, "sans-serif-medium");
        name.setLetterSpacing(-.02f);
        name.setShadowLayer(ui.px(10), 0, ui.px(1), 0x66000000);
        o.addView(name, ui.pos(700, 44, 48, 318));
        TextView credit =
                ui.text(Wallpaper.AUTHORS[saved], 14, 0xCCFFFFFF, "sans-serif");
        credit.setShadowLayer(ui.px(6), 0, ui.px(1), 0x66000000);
        o.addView(credit, ui.pos(700, 22, 49, 362));
        TextView hint =
                ui.text(
                        "OK to choose  ·  Back to keep the current one",
                        12,
                        0x80FFFFFF,
                        "sans-serif");
        o.addView(hint, ui.pos(600, 18, 49, 392));
        android.widget.HorizontalScrollView scroller = new android.widget.HorizontalScrollView(context);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        o.addView(scroller, ui.pos(864, 90, 48, 420));
        LinearLayout row = new LinearLayout(context);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        scroller.addView(row, new FrameLayout.LayoutParams(-2, -1));
        ArrayList<View> cells = new ArrayList<>();
        final int count = Wallpaper.count(prefs);
        final int cellWidth = 92;
        final int cellHeight = 56;
        for (int i = 0; i < count; i++) {
            final int index = i;
            FrameLayout cell = new FrameLayout(context);
            cell.setFocusable(true);
            cell.setFocusableInTouchMode(true);
            cell.setSoundEffectsEnabled(false);
            cell.setClipToOutline(true);
            cell.setBackground(ui.shape(0xFF101521, 12));
            ImageView img = new ImageView(context);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (Wallpaper.THUMBS[i] == Wallpaper.PHOTO_THUMB)
                img.setImageBitmap(PhotoStore.thumbnail(context));
            else img.setImageResource(Wallpaper.THUMBS[i]);
            cell.addView(img, new FrameLayout.LayoutParams(-1, -1));
            GradientDrawable ring = ui.shape(Color.TRANSPARENT, 12);
            ring.setStroke(ui.px(1.5f), i == saved ? 0xCCFFFFFF : 0x33FFFFFF);
            cell.setForeground(ring);
            cell.setOnFocusChangeListener(
                    (v, f) -> {
                        GradientDrawable r = ui.shape(Color.TRANSPARENT, 12);
                        r.setStroke(
                                ui.px(f ? 2 : 1.5f),
                                f ? Color.WHITE : index == saved ? 0xCCFFFFFF : 0x33FFFFFF);
                        cell.setForeground(r);
                        lift(cell, f);
                        if (f) {
                            name.setText(Wallpaper.NAMES[index]);
                            credit.setText(Wallpaper.AUTHORS[index]);
                            wallpaper.show(index);
                            screens.step(index);
                        }
                    });
            cell.setOnClickListener(
                    v -> {
                        screens.click();
                        prefs.setWallpaper(index);
                        overlays.onCancel(null);
                        overlays.close(false);
                        fadeBackIn();
                        host.reload(settingsRow);
                    });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.px(cellWidth), ui.px(cellHeight));
            if (i > 0) lp.leftMargin = ui.px(9);
            row.addView(cell, lp);
            cells.add(cell);
        }

        // "Browse…" tile — opens the system image picker for a custom picture
        FrameLayout browseCell = new FrameLayout(context);
        browseCell.setFocusable(true);
        browseCell.setFocusableInTouchMode(true);
        browseCell.setSoundEffectsEnabled(false);
        browseCell.setClipToOutline(true);
        browseCell.setBackground(ui.shape(0x26FFFFFF, 12));
        LinearLayout browseContent = new LinearLayout(context);
        browseContent.setOrientation(LinearLayout.VERTICAL);
        browseContent.setGravity(android.view.Gravity.CENTER);
        TextView browseIcon = ui.text("＋", 16, 0xEEFFFFFF, "sans-serif-medium");
        browseIcon.setGravity(android.view.Gravity.CENTER);
        TextView browseLabel = ui.text("Browse", 11, 0xCCFFFFFF, "sans-serif-medium");
        browseLabel.setGravity(android.view.Gravity.CENTER);
        browseContent.addView(browseIcon);
        browseContent.addView(browseLabel);
        browseCell.addView(browseContent, new FrameLayout.LayoutParams(-1, -1));
        GradientDrawable browseRing = ui.shape(Color.TRANSPARENT, 12);
        browseRing.setStroke(ui.px(1.5f), 0x33FFFFFF);
        browseCell.setForeground(browseRing);
        browseCell.setOnFocusChangeListener(
                (v, f) -> {
                    GradientDrawable r = ui.shape(Color.TRANSPARENT, 12);
                    r.setStroke(ui.px(f ? 2 : 1.5f), f ? Color.WHITE : 0x33FFFFFF);
                    browseCell.setForeground(r);
                    lift(browseCell, f);
                    if (f) {
                        name.setText("Custom photo");
                        credit.setText("Browse device files or USB storage");
                    }
                });
        browseCell.setOnClickListener(
                v -> {
                    screens.click();
                    overlays.onCancel(null);
                    overlays.close(false);
                    fadeBackIn();
                    browseForPhoto();
                });
        LinearLayout.LayoutParams browseLp =
                new LinearLayout.LayoutParams(ui.px(cellWidth), ui.px(cellHeight));
        browseLp.leftMargin = ui.px(9);
        row.addView(browseCell, browseLp);

        screens.post(() -> cells.get(saved).requestFocus());
    }

    /**
     * How a picture answers focus, the same for every cell: it rises and grows like an app tile,
     * and its shadow goes with the tile shadows (see {@link Layers#elevation}).
     */
    private void lift(android.view.View cell, boolean focused) {
        cell.setElevation(Layers.elevation(ui.px(focused ? 18 : 2)));
        cell.animate().cancel();
        float s = focused ? 1.16f : 1;
        if (screens.motion())
            cell.animate()
                    .scaleX(s)
                    .scaleY(s)
                    .translationY(focused ? -ui.px(4) : 0)
                    .setDuration(420)
                    .setInterpolator(Motion.TILE)
                    .start();
        else {
            cell.setScaleX(s);
            cell.setScaleY(s);
        }
    }
}
