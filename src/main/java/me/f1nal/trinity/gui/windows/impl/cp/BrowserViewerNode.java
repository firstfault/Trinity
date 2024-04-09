package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImColor;
import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.gui.components.events.MouseClickEventHandler;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class BrowserViewerNode {
    private String icon;
    private final Supplier<Integer> color;
    private final Supplier<String> label;
    private final RenameHandler rename;
    private Animation hoverAnimation;
    private final List<MouseClickEventHandler> mouseEventList = new ArrayList<>();
    private ImString renamingText = null;
    private String renamingId;
    private boolean renamingSetFocus;
    private List<ColoredString> prefix;
    private List<ColoredString> suffix;
    private boolean defaultOpen;

    public BrowserViewerNode(String icon, Supplier<Integer> color, Supplier<String> label, RenameHandler rename) {
        this.icon = icon;
        this.color = color;
        this.label = label;
        this.rename = rename;
    }

    public void setDefaultOpen(boolean defaultOpen) {
        this.defaultOpen = defaultOpen;
    }

    public boolean isDefaultOpen() {
        return defaultOpen;
    }

    public void setSuffix(List<ColoredString> suffix) {
        this.suffix = suffix;
    }

    public void setPrefix(List<ColoredString> prefix) {
        this.prefix = prefix;
    }

    public void addMouseClickHandler(MouseClickEventHandler handler) {
        this.mouseEventList.add(handler);
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Supplier<Integer> getColor() {
        return color;
    }

    public Supplier<String> getLabel() {
        return label;
    }

    public ImString getRenamingText() {
        return renamingText;
    }

    public boolean isRenameAvailable() {
        return this.rename != null;
    }

    public void beginRenaming() {
        if (!this.isRenameAvailable()) {
            return;
        }

        final String fullName = Objects.requireNonNullElse(this.rename.getFullName(), this.label.get());
        this.renamingText = new ImString(fullName, 0x200);
        this.renamingId = "Rename".concat(ComponentId.getId(this.getClass()));
        this.renamingSetFocus = false;
    }

    public void draw() {
        final boolean hovered = ImGui.isItemHovered();

        if (hovered) {
            if (this.hoverAnimation == null) this.hoverAnimation = new Animation(Easing.LINEAR, 40L, 100.F);

            final MouseClickType[] clickTypes = MouseClickType.values();
            for (int i = 0; i < clickTypes.length; i++) {
                if (ImGui.isMouseClicked(i)) {
                    for (MouseClickEventHandler e : mouseEventList) {
                        e.handleClick(clickTypes[i]);
                    }
                    break;
                }
            }
        }

        if (this.hoverAnimation != null) {
            ImGui.getWindowDrawList().addRectFilled(0, ImGui.getItemRectMinY() - 4, 0x10000, ImGui.getItemRectMaxY() + 4, ImColor.rgba(75, 75, 75, (int) this.hoverAnimation.getValue()));
            this.hoverAnimation.run(hovered ? this.hoverAnimation.getStartValue() : 0.F);
            if (this.hoverAnimation.getValue() == 0.F) this.hoverAnimation = null;
        }

        ImGui.textColored(getColor().get(), this.icon + " ");
        ImGui.sameLine(0.F, 0.F);

        if (this.prefix != null && !this.prefix.isEmpty()) {
            ColoredString.drawText(this.prefix);
            ImGui.sameLine(0.F, 0.F);
        }

        if (this.renamingText != null) {
            if (ImGui.isKeyDown(ImGui.getKeyIndex(ImGuiKey.Escape))) {
                this.renamingText = null;
            } else {
                GuiUtil.smallWidget(() -> {
                    ImGui.setKeyboardFocusHere();
                    ImGui.inputText("###" + this.renamingId, this.renamingText);
                    if ((this.renamingSetFocus && GuiUtil.isFocusLostOnItem()) || ImGui.isKeyDown(ImGui.getKeyIndex(ImGuiKey.Enter))) {
                        final String newName = this.renamingText.get();
                        this.renamingText = null;
                        Main.runLater(() -> this.rename.rename(newName));
                    }
                    this.renamingSetFocus = true;
                });
                return;
            }
        }
        ImGui.text(getLabel().get());

        if (this.suffix != null && !this.suffix.isEmpty()) {
            ImGui.sameLine(0.F, 0.F);
            ColoredString.drawText(this.suffix);
        }
    }
}
