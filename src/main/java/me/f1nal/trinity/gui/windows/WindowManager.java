package me.f1nal.trinity.gui.windows;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.gui.input.InputManager;
import me.f1nal.trinity.gui.windows.api.AbstractWindow;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.gui.windows.api.StaticWindow;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WindowManager {
    private final DisplayManager displayManager;
    /**
     * List of all open {@link ClosableWindow}.
     */
    private final List<ClosableWindow> closableWindows = Collections.synchronizedList(new ArrayList<>());
    /**
     * List of all active {@link PopupWindow}.
     */
    private final List<PopupWindow> popups = Collections.synchronizedList(new ArrayList<>());
    /**
     * Map of all active {@link StaticWindow}, retrievable by their class.
     */
    private final Map<Class<? extends StaticWindow>, StaticWindow> staticWindowMap = new HashMap<>();
    private final InputManager inputHandler = new InputManager();

    public WindowManager(DisplayManager displayManager) {
        this.displayManager = displayManager;
    }

    public void draw() {
        ClosableWindow[] windows = closableWindows.toArray(new ClosableWindow[0]);
        for (ClosableWindow frame : windows) {
            frame.render();
        }

        for (StaticWindow staticWindow : staticWindowMap.values()) {
            staticWindow.render();
        }

        this.drawPopups();
    }

    private List<AbstractWindow> getAllWindows() {
        List<AbstractWindow> windowList = new ArrayList<>();
        windowList.addAll(this.closableWindows);
        windowList.addAll(this.staticWindowMap.values());
        windowList.addAll(this.popups);
        return windowList;
    }

    /**
     * Removes every window.
     */
    public void resetAllWindows() {
        getAllWindows().stream().filter(abstractWindow -> !(abstractWindow instanceof PopupWindow)).forEach(AbstractWindow::close);

        closableWindows.clear();
        staticWindowMap.clear();
    }


    private void drawPopups() {
        if (this.popups.isEmpty()) {
            return;
        }

        int pops = 0;
        PopupWindow[] popups = this.popups.toArray(new PopupWindow[0]);
        for (PopupWindow popup : popups) {
            popup.render();
        }
        PopupWindow last = popups[popups.length - 1];

        for (PopupWindow popup : popups) {
            if (popup == last && !ImGui.isPopupOpen(popup.getPopupId())) {
                ImGui.openPopup(popup.getPopupId());
            }
            if (ImGui.beginPopupModal(popup.getPopupId(), ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings)) {
                popup.renderPopup();
                ++pops;
            }
        }

        while (pops-- != 0) ImGui.endPopup();

        if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Escape))) {
            if (last.canCloseOnEscapeNow()) {
                last.close();
            }
        }
    }

    public void addClosableWindow(ClosableWindow window) {
        final ClosableWindow windowAlreadyOpened = getClosableWindows().stream().filter(openWindow -> openWindow.isAlreadyOpen(window)).findFirst().orElse(null);

        if (windowAlreadyOpened != null) {
            windowAlreadyOpened.setVisible(true);
            return;
        }

        this.closableWindows.add(window);
        window.setVisible(true);
    }

    public void addPopup(PopupWindow popup) {
        this.popups.add(popup);
        popup.setCloseEvent(() -> this.popups.remove(popup));
    }

    public <T extends StaticWindow> T addStaticWindow(Class<T> type) {
        T wnd = getStaticWindow(type);
        wnd.setVisible(true);
        return wnd;
    }

    public <T extends StaticWindow> T getStaticWindow(Class<T> type) {
        //noinspection unchecked
        return (T) staticWindowMap.computeIfAbsent(type, c -> {
            try {
                Constructor<? extends StaticWindow> constructor = c.getDeclaredConstructor(Trinity.class);
                return constructor.newInstance(displayManager.getTrinity());
            } catch (Throwable throwable) {
                throw new RuntimeException("Creating static window instance", throwable);
            }
        });
    }

    public boolean isStaticWindowOpen(Class<? extends StaticWindow> type) {
        StaticWindow window = staticWindowMap.get(type);
        return window != null && window.isVisible();
    }

    public void closeAll(Predicate<ClosableWindow> predicate) {
        this.getWindows(predicate).forEach(ClosableWindow::close);
    }

    public List<ClosableWindow> getWindows(Predicate<ClosableWindow> predicate) {
        return this.closableWindows.stream().filter(predicate).collect(Collectors.toList());
    }

    public List<PopupWindow> getPopups() {
        return popups;
    }

    public List<ClosableWindow> getClosableWindows() {
        return closableWindows;
    }
}
