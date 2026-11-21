package dev.madusha.shangos;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * The rows of the Settings groups: Appearance, Motion and sound, Home and System. {@link
 * SettingsScreen} owns the pages, the list and the preview card; this builds what goes in them.
 */
final class SettingsPages {
    /** What a group needs from the page it is built into. */
    interface Kit {
        /** A row: a value with a chevron, or (with {@code toggle}) a switch. */
        View option(
                LinearLayout parent,
                String title,
                java.util.function.Supplier<String> value,
                String toggle,
                Runnable action,
                Runnable onFocus);

        /** Shows a thumbnail (0 for none), a title and some words in the card on the right. */
        void preview(int thumb, String title, String body);

        /** The wallpaper being shown. */
        int style();
    }

    // Positions in the Appearance group: the dialogs return to them.
    private static final int ROW_WALLPAPER = 0, ROW_NAME = 1;

    private final Activity activity;
    private final Ui ui;
    private final Prefs prefs;
    private final Overlays overlays;
    private final ScreenHost screens;
    private final SettingsScreen.Host host;
    private final SetupGuide setup;
    private final AboutScreen about;
    private final WallpaperPicker wallpaperPicker;
    private final NameEditor nameEditor;
    private final HomeScreen homeScreen;
    private final WeatherSettings weather;
    private final ScreenSaverSettings screenSaver;
    private final AppsSettings apps;
    private final DisplaySettings display;
    private int widgetsRow, aboutRowIndex;

    SettingsPages(
            Activity activity,
            Ui ui,
            Prefs prefs,
            Overlays overlays,
            ScreenHost screens,
            SettingsScreen.Host host,
            SetupGuide setup,
            AboutScreen about,
            WallpaperPicker wallpaperPicker,
            NameEditor nameEditor,
            HomeScreen homeScreen,
            WeatherSettings weather,
            ScreenSaverSettings screenSaver,
            AppsSettings apps,
            DisplaySettings display) {
        this.activity = activity;
        this.ui = ui;
        this.prefs = prefs;
        this.overlays = overlays;
        this.screens = screens;
        this.host = host;
        this.setup = setup;
        this.about = about;
        this.wallpaperPicker = wallpaperPicker;
        this.nameEditor = nameEditor;
        this.homeScreen = homeScreen;
        this.weather = weather;
        this.screenSaver = screenSaver;
        this.apps = apps;
        this.display = display;
    }

    /** The row of About in the System group (where its dialogs return to). */
    int aboutRow() {
        return aboutRowIndex;
    }

    /** Settings, Appearance. */
    void appearance(Kit kit, LinearLayout list, ArrayList<View> controls) {
        controls.add(
                kit.option(
                        list,
                        "Wallpaper",
                        () -> Wallpaper.NAMES[kit.style()],
                        null,
                        () -> wallpaperPicker.show(ROW_WALLPAPER),
                        () -> {
                            int cur = kit.style();
                            String desc =
                                    Wallpaper.AUTHORS[cur]
                                            + "\n\n"
                                            + "Eleven wallpapers from around the world — and your"
                                            + " own photo. OK opens the full-screen picker.";
                            kit.preview(Wallpaper.THUMBS[cur], Wallpaper.NAMES[cur], desc);
                        }));
        controls.add(
                kit.option(
                        list,
                        "Your name",
                        () -> {
                            String n = prefs.name();
                            return n.isEmpty() ? "Add" : n;
                        },
                        null,
                        () -> nameEditor.show(ROW_NAME),
                        () ->
                                kit.preview(
                                        0,
                                        "Your name",
                                        "Home greets you by name. It is saved only on this TV.")));
        controls.add(
                kit.option(
                        list,
                        "Clock",
                        null,
                        Prefs.CLOCK,
                        () -> {},
                        () ->
                                kit.preview(
                                        0,
                                        "Clock",
                                        "Shows the time in the top-right corner of every page.")));
        final int sizeRow = controls.size();
        controls.add(
                kit.option(
                        list,
                        "Layout size",
                        display::sizeLabel,
                        null,
                        () -> display.sizeMenu(sizeRow),
                        () -> kit.preview(0, "Layout size", display.sizeAbout())));
        controls.add(
                kit.option(
                        list,
                        "Status icons",
                        display::statusLabel,
                        null,
                        () -> display.statusMenu(sizeRow + 1),
                        () -> kit.preview(0, "Status icons", display.statusAbout())));
        if (Flavor.DEBUG)
            controls.add(
                    kit.option(
                            list,
                            "Preview size (debug)",
                            display::previewLabel,
                            null,
                            display::previewMenu,
                            () -> kit.preview(0, "Preview size", display.previewAbout())));
    }

    /** Settings, Motion and sound. */
    void motion(Kit kit, LinearLayout list, ArrayList<View> controls) {
        controls.add(
                kit.option(
                        list,
                        "Fluid motion",
                        null,
                        Prefs.MOTION,
                        () -> {},
                        () ->
                                kit.preview(
                                        0,
                                        "Fluid motion",
                                        "Springy focus, depth and the 3D flight into your TV. Turn"
                                                + " off for a calmer, simpler screen.")));
        // Wallpaper effects need a TV that can spare the GPU: on Light they are off whatever is
        // chosen, so their switches are not offered there.
        if (host.performance().tier().animates()) {
            controls.add(
                    kit.option(
                            list,
                            "Living wallpaper",
                            null,
                            Prefs.AMBIENT,
                            host::ambientChanged,
                            () ->
                                    kit.preview(
                                            Wallpaper.THUMBS[kit.style()],
                                            "Living wallpaper",
                                            "Clouds drift and water moves very slowly. Pauses"
                                                + " whenever an app is open.")));
            controls.add(
                    kit.option(
                            list,
                            "Room lighting",
                            null,
                            Prefs.STAGE,
                            () -> {},
                            () ->
                                    kit.preview(
                                            Wallpaper.THUMBS[kit.style()],
                                            "Room lighting",
                                            "The light behind your apps takes the colour of the one"
                                                + " you are on, and the wallpaper dims a"
                                                + " little.")));
            controls.add(
                    kit.option(
                            list,
                            "Frosted glass",
                            null,
                            Prefs.GLASS,
                            host::glassChanged,
                            () ->
                                    kit.preview(
                                            Wallpaper.THUMBS[kit.style()],
                                            "Frosted glass",
                                            "Bars, cards and menus show a blurred copy of the"
                                                    + " wallpaper behind them.")));
            final int strengthRow = controls.size();
            controls.add(
                    kit.option(
                            list,
                            "Glass strength",
                            () -> Frost.NAMES[prefs.glassStrength()],
                            null,
                            () ->
                                    overlays.menu(
                                            "Glass strength",
                                            Frost.NAMES,
                                            prefs.glassStrength(),
                                            choice -> {
                                                prefs.setGlassStrength(choice);
                                                host.glassChanged();
                                                host.reload(strengthRow);
                                            }),
                            () ->
                                    kit.preview(
                                            Wallpaper.THUMBS[kit.style()],
                                            "Glass strength",
                                            "How much of the wallpaper shows through the glass:"
                                                    + " Soft shows more, Deep less. Needs Frosted"
                                                    + " glass to be on.")));
        }
        controls.add(
                kit.option(
                        list,
                        "Calm mode",
                        null,
                        Prefs.CALM,
                        () -> {},
                        () ->
                                kit.preview(
                                        Wallpaper.THUMBS[kit.style()],
                                        "Calm mode",
                                        "After a quiet moment the bar and clock fade back so"
                                                + " the wallpaper has the room. Any key brings"
                                                + " them back.")));
        final int performanceRow = controls.size();
        controls.add(
                kit.option(
                        list,
                        "Performance",
                        () -> host.performance().label(),
                        null,
                        () ->
                                overlays.menu(
                                        "Performance",
                                        Prefs.PERF_NAMES,
                                        prefs.perf(),
                                        choice -> {
                                            prefs.setPerf(choice);
                                            host.reload(performanceRow);
                                            host.ambientChanged();
                                        }),
                        () ->
                                kit.preview(
                                        0,
                                        "Performance",
                                        "Auto chooses for this TV ("
                                                + Hardware.describe(activity)
                                                + "). Light keeps the wallpaper still, the lightest"
                                                + " and coolest, and hides Living wallpaper, Room"
                                                + " lighting and Frosted glass, which it would"
                                                + " switch off anyway. Balanced and Quality add"
                                                + " them, with slow gliding light that pauses"
                                                + " while you use the remote and after a long"
                                                + " time without a key press.")));
        controls.add(
                kit.option(
                        list,
                        "Navigation sounds",
                        null,
                        Prefs.SOUND,
                        () -> {},
                        () ->
                                kit.preview(
                                        0,
                                        "Navigation sounds",
                                        "Soft, quiet feedback as you move and open apps.")));
    }

    /** Settings, Home. */
    void home(Kit kit, LinearLayout list, ArrayList<View> controls) {
        widgetsRow = controls.size();
        controls.add(
                kit.option(
                        list,
                        "Home widgets",
                        this::widgetSummary,
                        null,
                        this::widgetEditor,
                        () ->
                                kit.preview(
                                        0,
                                        "Home widgets",
                                        "Choose what Home shows above your apps: Up Next,"
                                                + " Weather (off until you turn it on), "
                                                + (InputsWidget.count > 0 ? "Inputs, " : "")
                                                + "Control, Now Playing, Sun and Moon and System,"
                                                + " and their order. Left and Right turn the"
                                                + " pages.")));
        final int locationRow = controls.size();
        controls.add(
                kit.option(
                        list,
                        "City",
                        weather::cityLabel,
                        null,
                        () -> weather.pickCity(locationRow),
                        () -> kit.preview(0, "City", weather.cityAbout())));
        controls.add(
                kit.option(
                        list,
                        "Temperature",
                        weather::unitLabel,
                        null,
                        () -> weather.unitMenu(locationRow + 1),
                        () -> kit.preview(0, "Temperature", weather.unitAbout())));
        controls.add(
                kit.option(
                        list,
                        "Home screen",
                        () -> homeScreen.isDefault() ? "shangOS" : "TV default",
                        null,
                        homeScreen::show,
                        () ->
                                kit.preview(
                                        0,
                                        "Home screen",
                                        homeScreen.isDefault()
                                                ? "shangOS opens when you press Home. Choose to"
                                                        + " return to your TV's own home screen."
                                                : "Your TV's own home screen opens when you press"
                                                        + " Home.")));
        final int appsRow = controls.size();
        controls.add(
                kit.option(
                        list,
                        "Apps order",
                        apps::sortLabel,
                        null,
                        () -> apps.sortMenu(appsRow),
                        () -> kit.preview(0, "Apps order", apps.sortAbout())));
        controls.add(
                kit.option(
                        list,
                        "Hidden apps",
                        apps::hiddenLabel,
                        null,
                        () -> apps.showHidden(appsRow + 1),
                        () -> kit.preview(0, "Hidden apps", apps.hiddenAbout())));
    }

    /** Settings, System. */
    void system(Kit kit, LinearLayout list, ArrayList<View> controls) {
        controls.add(
                kit.option(
                        list,
                        "Setup guide",
                        setup::progress,
                        null,
                        () -> setup.show(setup.resumeStep()),
                        () ->
                                kit.preview(
                                        0,
                                        "Setup guide",
                                        "Privacy choices, optional permissions and the home screen,"
                                                + " one step at a time.")));
        controls.add(
                kit.option(
                        list,
                        "Screen saver",
                        screenSaver::label,
                        null,
                        screenSaver::menu,
                        () ->
                                kit.preview(
                                        0,
                                        "Screen saver",
                                        "shangOS Ambient: a quiet clock over your wallpaper that"
                                                + " moves every minute. No network, nothing to"
                                                + " allow.")));
        controls.add(
                kit.option(
                        list,
                        "Device settings",
                        () -> "",
                        null,
                        () -> activity.startActivity(new Intent(Settings.ACTION_SETTINGS)),
                        () ->
                                kit.preview(
                                        0,
                                        "Device settings",
                                        "Network, picture, sound and accounts.")));
        aboutRowIndex = controls.size();
        controls.add(
                kit.option(
                        list,
                        "About " + activity.getString(R.string.app_name),
                        () -> "",
                        null,
                        about::show,
                        () ->
                                kit.preview(
                                        0,
                                        "About " + activity.getString(R.string.app_name),
                                        "Version "
                                                + AboutScreen.version(activity)
                                                + " · based on Luma TV (MIT)")));
    }

    private String widgetSummary() {
        boolean inputs = !InputsWidget.list(activity).isEmpty();
        int n = 0;
        for (String id : WidgetBoard.IDS)
            if (WidgetBoard.on(prefs, id)
                    && (inputs || !id.equals("inputs"))
                    && (Net.enabled(prefs) || !id.equals("weather"))) n++;
        return n == 0 ? "Off" : n + " on";
    }

    private void widgetEditor() {
        new WidgetEditor(activity, ui, prefs, overlays, screens, widgetsRow);
    }
}
