package dev.madusha.shangos;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * The one thing an app cannot do alone on Android TV is open the system's power dialog. An
 * accessibility service can ask for it (GLOBAL_ACTION_POWER_DIALOG). The user turns this service on
 * in Settings, Accessibility; it is declared with no window content, so it cannot read the screen,
 * and it ignores every event. Nothing else in shangOS uses it.
 */
public final class PowerService extends AccessibilityService {
    private static volatile PowerService running;

    /** True while the user has turned this service on. */
    static boolean enabled() {
        return running != null;
    }

    /** Asks Android to open its power dialog. False when the service is off or the TV refuses. */
    static boolean showPowerDialog() {
        PowerService s = running;
        return s != null && s.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }

    @Override
    protected void onServiceConnected() {
        running = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used: the service does not look at events.
    }

    @Override
    public void onInterrupt() {
        // Nothing to stop.
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        running = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        running = null;
        super.onDestroy();
    }
}
