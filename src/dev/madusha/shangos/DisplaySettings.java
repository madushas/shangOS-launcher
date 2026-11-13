package dev.madusha.shangos;

import android.app.Activity;

import java.util.function.IntConsumer;

/** Settings, Appearance: layout size and status icons, and in debug builds the preview size. */
final class DisplaySettings {
    private static final String[] SIZES = {"Compact", "Comfortable", "Cozy"};
    private static final String[] SIZE_KEYS = {Metrics.COMPACT, Metrics.COMFORTABLE, Metrics.COZY};
    private static final String[] STATUS = {"Always", "Only problems"};
    private static final String[] PREVIEWS = {"Real screen", "1080p", "4K"};
    private static final int[] PREVIEW_WIDTHS = {0, 1920, 3840};

    private final Activity activity;
    private final Prefs prefs;
    private final Overlays overlays;
    private final IntConsumer reload;

    DisplaySettings(Activity activity, Prefs prefs, Overlays overlays, IntConsumer reload) {
        this.activity = activity;
        this.prefs = prefs;
        this.overlays = overlays;
        this.reload = reload;
    }

    private int sizeIndex() {
        return Math.max(0, java.util.Arrays.asList(SIZE_KEYS).indexOf(prefs.layoutSize()));
    }

    String sizeLabel() {
        return SIZES[sizeIndex()];
    }

    String sizeAbout() {
        return "How many apps share a row: Compact 6, Comfortable 5, Cozy 4, larger and more spaced"
                + " out, for big screens or sitting far away.";
    }

    void sizeMenu(int row) {
        overlays.menu(
                "Layout size",
                SIZES,
                sizeIndex(),
                choice -> {
                    prefs.setLayoutSize(SIZE_KEYS[choice]);
                    Metrics.apply(SIZE_KEYS[choice]);
                    reload.accept(row);
                });
    }

    String statusLabel() {
        return prefs.statusMode().equals("alerts") ? STATUS[1] : STATUS[0];
    }

    String statusAbout() {
        return "The icons beside the clock: network, VPN and sound muted. Only problems shows"
                + " them when something is wrong (no network, a VPN on, sound muted).";
    }

    void statusMenu(int row) {
        overlays.menu(
                "Status icons",
                STATUS,
                prefs.statusMode().equals("alerts") ? 1 : 0,
                choice -> {
                    prefs.setStatusMode(choice == 1 ? "alerts" : "all");
                    reload.accept(row);
                });
    }

    String previewLabel() {
        for (int i = 0; i < PREVIEW_WIDTHS.length; i++)
            if (PREVIEW_WIDTHS[i] == prefs.previewWidth()) return PREVIEWS[i];
        return PREVIEWS[0];
    }

    String previewAbout() {
        return "Debug builds only: lays the launcher out at 1080p or 4K and scales it down to this"
                + " screen, to see how a bigger TV would draw it.";
    }

    void previewMenu() {
        overlays.menu(
                "Preview size",
                PREVIEWS,
                java.util.Arrays.binarySearch(PREVIEW_WIDTHS, prefs.previewWidth()),
                choice -> {
                    prefs.setPreviewWidth(PREVIEW_WIDTHS[choice]);
                    activity.recreate();
                });
    }
}
