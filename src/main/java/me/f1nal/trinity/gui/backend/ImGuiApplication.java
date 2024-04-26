package me.f1nal.trinity.gui.backend;

import imgui.app.Configuration;

public abstract class ImGuiApplication extends ImGuiWindow {
    /**
     * Method called before window creation. Could be used to provide basic window information, like title name etc.
     *
     * @param config configuration object with basic window information
     */
    protected void configure(final Configuration config) {
    }

    /**
     * Method called once, before application run loop.
     */
    protected void preRun() {
    }

    /**
     * Method called once, after application run loop.
     */
    protected void postRun() {
    }

    /**
     * Entry point of any ImGui application. Use it to start the application loop.
     *
     * @param app application instance to run
     */
    public static void launch(final ImGuiApplication app) {
        initialize(app);
        app.preRun();
        app.run();
        app.postRun();
        app.dispose();
    }

    private static void initialize(final ImGuiApplication app) {
        final Configuration config = new Configuration();
        app.configure(config);
        app.init(config);
    }
}
