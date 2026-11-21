package dev.madusha.shangos;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * System glance: how the network is (Wi-Fi with its bars, Ethernet or none), how full the storage
 * is and how much memory is free. Reads the system's own numbers, which need no permission beyond
 * the network state the launcher already has. Refreshes every 30 seconds while Home is shown.
 */
final class GlanceWidget extends Widget {
    private static final long EVERY = 30_000L;
    private static final int PAD = 18;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView network, storage, memory;
    private final Bar storageBar, memoryBar;
    private final Runnable tick =
            new Runnable() {
                public void run() {
                    update();
                    handler.postDelayed(this, EVERY);
                }
            };

    GlanceWidget(Context context, WidgetBoard.Host host, float width) {
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
        int inner = Math.round(w) - 2 * PAD;
        addView(text("System", 13, ink(0xE6), "sans-serif-medium"), pos(inner, 16, PAD, 14));
        addView(label("Network"), pos(inner, 14, PAD, 42));
        network = text("", 15, ink(0xFF), "sans-serif-medium");
        addView(network, pos(inner, 20, PAD, 56));
        addView(label("Storage"), pos(inner, 14, PAD, 88));
        storage = text("", 13, ink(0xD9), "sans-serif");
        addView(storage, pos(inner, 16, PAD, 102));
        storageBar = new Bar(context);
        addView(storageBar, pos(inner, 5, PAD, 122));
        addView(label("Memory"), pos(inner, 14, PAD, 134));
        memory = text("", 13, ink(0xD9), "sans-serif");
        addView(memory, pos(inner, 16, PAD, 148));
        memoryBar = new Bar(context);
        addView(memoryBar, pos(inner, 5, PAD, 168));
        update();
    }

    private TextView label(String value) {
        TextView t =
                text(value.toUpperCase(Locale.ROOT), 10.5f, ink(0x8C), "sans-serif-medium");
        t.setLetterSpacing(.06f);
        return t;
    }

    @Override
    void setActive(boolean value) {
        boolean was = active;
        super.setActive(value);
        handler.removeCallbacks(tick);
        if (value && !was) {
            update();
            handler.postDelayed(tick, EVERY);
        }
    }

    @Override
    void refresh() {
        update();
    }

    private void update() {
        network.setText(networkLine());
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        double total = fs.getTotalBytes(), free = fs.getAvailableBytes();
        storage.setText(gb(free) + " free of " + gb(total));
        storageBar.set(total > 0 ? 1 - free / total : 0);
        ActivityManager am = getContext().getSystemService(ActivityManager.class);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) am.getMemoryInfo(mi);
        double totalMem = mi.totalMem, freeMem = mi.availMem;
        memory.setText(gb(freeMem) + " free of " + gb(totalMem));
        memoryBar.set(totalMem > 0 ? 1 - freeMem / totalMem : 0);
    }

    /** "Wi-Fi, strong", "Ethernet", or "Offline"; the offline build cannot read it. */
    private String networkLine() {
        if (!Flavor.NETWORK) return "Not available in this build";
        try {
            ConnectivityManager cm = getContext().getSystemService(ConnectivityManager.class);
            Network active = cm == null ? null : cm.getActiveNetwork();
            if (active == null) return "Offline";
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps == null) return "Connected";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                int rssi = caps.getSignalStrength();
                String strength =
                        rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED
                                ? ""
                                : ", " + Metrics.bars(rssi) + " of 4 bars";
                return "Wi-Fi" + strength;
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
            return "Connected";
        } catch (RuntimeException e) {
            return "Not available";
        }
    }

    private static String gb(double bytes) {
        return String.format(Locale.US, "%.1f GB", bytes / 1e9);
    }

    /** A thin rounded bar that is filled to a fraction (how much is used). */
    private final class Bar extends View {
        private final Paint back = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float used;

        Bar(Context context) {
            super(context);
            back.setColor(ink(0x26));
            fill.setColor(ink(0xD9));
        }

        void set(double fraction) {
            used = (float) Math.max(0, Math.min(1, fraction));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float h = getHeight(), w = getWidth(), r = h / 2;
            c.drawRoundRect(0, 0, w, h, r, r, back);
            if (used > 0) c.drawRoundRect(0, 0, Math.max(h, w * used), h, r, r, fill);
        }
    }
}
