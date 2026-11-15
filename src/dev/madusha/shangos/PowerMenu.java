package dev.madusha.shangos;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

/**
 * The power button: opens the TV's own power dialog (restart, power off, and whatever the TV
 * offers), which asks for its own confirmation. That needs the shangOS power service turned on in
 * Settings, Accessibility; until then the button explains how.
 */
final class PowerMenu {
    private final Activity activity;
    private final Overlays overlays;

    PowerMenu(Activity activity, Overlays overlays) {
        this.activity = activity;
        this.overlays = overlays;
    }

    void open() {
        if (PowerService.enabled() && PowerService.showPowerDialog()) return;
        if (!PowerService.enabled()) {
            overlays.menu(
                    "Power",
                    new String[] {"Turn on the power button", "Cancel"},
                    -1,
                    choice -> {
                        if (choice == 0) openAccessibility();
                    });
            return;
        }
        overlays.notice(
                "Power",
                "This TV did not open its power dialog. Use the remote's power button.");
    }

    private void openAccessibility() {
        overlays.notice(
                "Turn on the power button",
                "Open Accessibility, choose shangOS power button and turn it on. It only opens"
                        + " the TV's power dialog; it cannot read the screen. Then press the power"
                        + " button again.");
        try {
            activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (RuntimeException e) {
            // The TV has no accessibility settings page; the notice above says what to do.
        }
    }
}
