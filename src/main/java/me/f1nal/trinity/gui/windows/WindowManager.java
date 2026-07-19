package me.f1nal.trinity.gui.windows;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.gui.input.InputManager;
import me.f1nal.trinity.gui.windows.api.AbstractWindow;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WindowManager {
    private static final float DIALOG_DIM_ALPHA = 32.F;
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
    private ClosableWindow focusRequested;
    private int focusFramesRemaining;
    private final Animation dialogDimAnimation = new Animation(Easing.EASE_IN_OUT_QUAD, 220L);

    public WindowManager(DisplayManager displayManager) {
        this.displayManager = displayManager;
    }

    public void draw() {
        ClosableWindow[] windows = closableWindows.toArray(new ClosableWindow[0]);
        StaticWindow[] staticWindows = staticWindowMap.values().toArray(new StaticWindow[0]);
        List<AbstractWindow> dialogs = new ArrayList<>();
        Arrays.stream(windows).filter(window -> window.isVisible() && window.isDialog()).forEach(dialogs::add);
        Arrays.stream(staticWindows).filter(window -> window.isVisible() && window.isDialog()).forEach(dialogs::add);
        boolean dialogVisible = !dialogs.isEmpty();
        this.dialogDimAnimation.run(dialogVisible ? DIALOG_DIM_ALPHA : 0.F);

        for (ClosableWindow frame : windows) {
            if (!frame.isDialog()) frame.render();
        }

        for (StaticWindow staticWindow : staticWindows) {
            if (!staticWindow.isDialog()) staticWindow.render();
        }

        if (dialogVisible) {
            ImGui.pushStyleColor(ImGuiCol.ModalWindowDimBg,
                    ImColor.rgba(0, 0, 0, Math.round(this.dialogDimAnimation.getValue())));
            AbstractWindow popupHost = dialogs.get(dialogs.size() - 1);
            popupHost.setChildWindowRenderer(this::drawPopups);
            for (ClosableWindow frame : windows) {
                if (frame.isDialog()) frame.render();
            }
            for (StaticWindow staticWindow : staticWindows) {
                if (staticWindow.isDialog()) staticWindow.render();
            }
            popupHost.setChildWindowRenderer(null);
            ImGui.popStyleColor();
        } else {
            this.drawPopups();
        }

        if (!dialogVisible) this.drawDialogFadeOut();

        ClosableWindow requested = this.focusRequested;
        if (requested != null) {
            if (!requested.isVisible()) {
                this.focusRequested = null;
            } else if (requested.hasRendered()) {
                ImGui.setWindowFocus(requested.getImGuiWindowName());
                if (ImGui.isMouseDown(0)) {
                    this.focusFramesRemaining = 2;
                } else if (--this.focusFramesRemaining <= 0) {
                    this.focusRequested = null;
                }
            }
        }
    }

    private void drawDialogFadeOut() {
        int alpha = Math.round(this.dialogDimAnimation.getValue());
        if (alpha <= 0) return;

        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.getForegroundDrawList(viewport).addRectFilled(
                viewport.getPosX(), viewport.getPosY(),
                viewport.getPosX() + viewport.getSizeX(), viewport.getPosY() + viewport.getSizeY(),
                ImColor.rgba(0, 0, 0, alpha));
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
                if (popup == last && ImGui.isKeyPressed(ImGuiKey.Escape, false) && popup.canCloseOnEscapeNow()) {
                    popup.close();
                }
                if (!this.popups.contains(popup)) {
                    ImGui.closeCurrentPopup();
                }
                ++pops;
            }
        }

        while (pops-- != 0) ImGui.endPopup();
    }

    public void addClosableWindow(ClosableWindow window) {
        if (window.isCloseRequested()) {
            return;
        }

        final ClosableWindow windowAlreadyOpened = getClosableWindows().stream().filter(openWindow -> openWindow.isAlreadyOpen(window)).findFirst().orElse(null);

        if (windowAlreadyOpened != null) {
            windowAlreadyOpened.setVisible(true);
            return;
        }

        this.closableWindows.add(window);
        window.setVisible(true);
    }

    public void requestFocus(ClosableWindow window) {
        this.focusRequested = window;
        this.focusFramesRemaining = 2;
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
    
    public <T extends ClosableWindow> List<T> getWindowsOfType(Class<T> type) {
        //noinspection unchecked
        return (List<T>) getWindows(wnd -> type.isAssignableFrom(wnd.getClass()));
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
