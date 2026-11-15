package dev.madusha.shangos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.view.View;
import android.view.View.MeasureSpec;

/**
 * The icons beside the clock: the network (Wi-Fi with its strength, or Ethernet; amber and crossed
 * when there is none), a lock while a VPN carries the traffic, and a crossed speaker while the
 * sound is muted. Settings, Status icons limits them to problems. Reads only whether a network
 * exists (the network-state permission the app already has); the offline build shows no network
 * icons.
 */
final class StatusIcons extends View {
    private static final int AMBER = 0xFFFFB84D;

    private final Handler handler;
    private final Prefs prefs;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private int ink = 0xE6FFFFFF;
    private boolean vpn, offline, wifi, ethernet, muted, known;
    private int bars = 4;
    private ConnectivityManager manager;

    private final ConnectivityManager.NetworkCallback callback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onCapabilitiesChanged(Network n, NetworkCapabilities caps) {
                    set(caps, false);
                }

                @Override
                public void onLost(Network n) {
                    set(null, true);
                }
            };

    StatusIcons(Context context, Handler handler, Prefs prefs) {
        super(context);
        this.handler = handler;
        this.prefs = prefs;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        fill.setStyle(Paint.Style.FILL);
    }

    /** The colour of the icons: dark on a pale wallpaper. */
    void setInk(int color) {
        ink = color;
        invalidate();
    }

    /** Watches the default network; call {@link #stop} when Home leaves the screen. */
    void start() {
        refresh();
        if (!Flavor.NETWORK) return;
        try {
            manager = getContext().getSystemService(ConnectivityManager.class);
            if (manager == null) return;
            Network active = manager.getActiveNetwork();
            set(active == null ? null : manager.getNetworkCapabilities(active), active == null);
            manager.registerDefaultNetworkCallback(callback, handler);
        } catch (RuntimeException e) {
            manager = null; // no permission (the offline build): no network icons
        }
    }

    void stop() {
        if (manager == null) return;
        try {
            manager.unregisterNetworkCallback(callback);
        } catch (RuntimeException ignored) {
        }
        manager = null;
    }

    /** Looks at the sound again (called with the clock). */
    void refresh() {
        AudioManager audio = getContext().getSystemService(AudioManager.class);
        boolean now =
                audio != null
                        && !audio.isVolumeFixed()
                        && (audio.isStreamMute(AudioManager.STREAM_MUSIC)
                                || audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0);
        if (now != muted) {
            muted = now;
            requestLayout();
            invalidate();
        }
    }

    private void set(NetworkCapabilities caps, boolean none) {
        known = true;
        offline = none;
        vpn = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        wifi = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        ethernet = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        int unknown = NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED;
        int rssi = caps == null ? unknown : caps.getSignalStrength();
        bars = rssi == unknown ? 4 : Metrics.bars(rssi);
        setContentDescription(offline ? "No network" : vpn ? "VPN on" : "");
        requestLayout();
        invalidate();
    }

    /** How many icons show now: the sound, the VPN, and one for the network when it matters. */
    int count() {
        boolean all = !prefs.statusMode().equals("alerts");
        int n = (muted ? 1 : 0) + (vpn ? 1 : 0);
        if (known && (offline || (all && (wifi || ethernet)))) n++;
        return n;
    }

    /** As wide as the icons that show, so the capsule beside it fits them and no more. */
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int h = MeasureSpec.getSize(heightSpec);
        int w = Math.round(count() * h * 28f / 36f);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight(), u = h / 36f, cy = h / 2f, x = getWidth();
        boolean all = !prefs.statusMode().equals("alerts");
        stroke.setStrokeWidth(2f * u);
        if (muted) {
            x -= 26 * u;
            speaker(canvas, x, cy, u);
        }
        if (vpn) {
            x -= 26 * u;
            lock(canvas, x, cy, u);
        }
        if (known && (offline || (all && (wifi || ethernet)))) {
            x -= 28 * u;
            if (offline) noWifi(canvas, x, cy, u);
            else if (ethernet && !wifi) ethernet(canvas, x, cy, u);
            else wifi(canvas, x, cy, u);
        }
    }

    /** A padlock, 20 units wide, left edge at {@code left}. */
    private void lock(Canvas c, float left, float cy, float u) {
        stroke.setColor(ink);
        fill.setColor(ink);
        rect.set(left + 2 * u, cy - 1 * u, left + 18 * u, cy + 9 * u);
        c.drawRoundRect(rect, 2.5f * u, 2.5f * u, fill);
        path.reset();
        path.moveTo(left + 5.5f * u, cy - 1 * u);
        path.lineTo(left + 5.5f * u, cy - 4 * u);
        path.cubicTo(left + 5.5f * u, cy - 11 * u, left + 14.5f * u, cy - 11 * u,
                left + 14.5f * u, cy - 4 * u);
        path.lineTo(left + 14.5f * u, cy - 1 * u);
        c.drawPath(path, stroke);
    }

    /** Wi-Fi: a dot and up to three arcs, the weak ones dimmed. */
    private void wifi(Canvas c, float left, float cy, float u) {
        float cx = left + 11 * u, base = cy + 8 * u;
        fill.setColor(ink);
        c.drawCircle(cx, base, 1.8f * u, fill);
        for (int i = 1; i <= 3; i++) {
            stroke.setColor(i < bars ? ink : (ink & 0x00FFFFFF) | 0x4D000000);
            float r = i * 5.5f * u;
            rect.set(cx - r, base - r, cx + r, base + r);
            c.drawArc(rect, -135, 90, false, stroke);
        }
    }

    /** Ethernet: a small socket. */
    private void ethernet(Canvas c, float left, float cy, float u) {
        stroke.setColor(ink);
        rect.set(left + 2 * u, cy - 7 * u, left + 20 * u, cy + 5 * u);
        c.drawRoundRect(rect, 3 * u, 3 * u, stroke);
        for (int i = 0; i < 3; i++)
            c.drawLine(
                    left + (7 + i * 4) * u, cy - 3 * u, left + (7 + i * 4) * u, cy + 1 * u, stroke);
        c.drawLine(left + 11 * u, cy + 5 * u, left + 11 * u, cy + 9 * u, stroke);
    }

    /** No network: Wi-Fi arcs with a slash, in amber. */
    private void noWifi(Canvas c, float left, float cy, float u) {
        stroke.setColor(AMBER);
        fill.setColor(AMBER);
        float cx = left + 11 * u, base = cy + 8 * u;
        for (int i = 1; i <= 3; i++) {
            float r = i * 5.5f * u;
            rect.set(cx - r, base - r, cx + r, base + r);
            c.drawArc(rect, -135, 90, false, stroke);
        }
        c.drawCircle(cx, base, 1.8f * u, fill);
        c.drawLine(left + 2 * u, cy - 8 * u, left + 20 * u, cy + 10 * u, stroke);
    }

    /** A speaker with a slash: the sound is muted. */
    private void speaker(Canvas c, float left, float cy, float u) {
        stroke.setColor(ink);
        fill.setColor(ink);
        path.reset();
        path.moveTo(left + 3 * u, cy - 3 * u);
        path.lineTo(left + 7 * u, cy - 3 * u);
        path.lineTo(left + 12 * u, cy - 8 * u);
        path.lineTo(left + 12 * u, cy + 8 * u);
        path.lineTo(left + 7 * u, cy + 3 * u);
        path.lineTo(left + 3 * u, cy + 3 * u);
        path.close();
        c.drawPath(path, fill);
        c.drawLine(left + 15 * u, cy - 4 * u, left + 21 * u, cy + 4 * u, stroke);
        c.drawLine(left + 21 * u, cy - 4 * u, left + 15 * u, cy + 4 * u, stroke);
    }
}
