package me.f1nal.trinity.theme;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiDir;

import java.awt.Color;

public final class TrinityStyle {
    private static boolean initialized;

    private TrinityStyle() {
    }

    public static void initialize(AccentColor accentColor) {
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowPadding(9.F, 8.F);
        style.setWindowRounding(0.F);
        style.setWindowBorderSize(1.F);
        style.setWindowTitleAlign(0.F, 0.5F);
        style.setWindowMenuButtonPosition(ImGuiDir.Right);
        style.setChildRounding(5.F);
        style.setChildBorderSize(1.F);
        style.setPopupRounding(6.F);
        style.setPopupBorderSize(1.F);
        style.setFramePadding(4.F, 4.F);
        style.setFrameRounding(4.F);
        style.setFrameBorderSize(0.F);
        style.setItemSpacing(6.F, 6.F);
        style.setItemInnerSpacing(4.F, 4.F);
        style.setCellPadding(6.F, 4.F);
        style.setIndentSpacing(12.F);
        style.setScrollbarSize(11.F);
        style.setScrollbarRounding(8.F);
        style.setGrabMinSize(10.F);
        style.setGrabRounding(4.F);
        style.setTabRounding(0.F);
        style.setTabBorderSize(0.F);
        style.setTabCloseButtonMinWidthUnselected(0.F);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.F, 0.5F);
        style.setSeparatorTextBorderSize(1.F);
        style.setSeparatorTextPadding(14.F, 4.F);
        style.setDockingSeparatorSize(2.F);
        style.setHoverDelayShort(0.15F);
        style.setHoverDelayNormal(0.45F);

        initialized = true;
        refresh(accentColor);
    }

    public static void refresh(AccentColor accentColor) {
        if (!initialized) return;

        ImGuiStyle style = ImGui.getStyle();
        int background = opaque(CodeColorScheme.BACKGROUND);
        int popup = opaque(CodeColorScheme.POPUP_BACKGROUND);
        int widget = opaque(CodeColorScheme.WIDGET_BACKGROUND);
        int text = opaque(CodeColorScheme.TEXT);
        int disabled = opaque(CodeColorScheme.DISABLED);
        int elevated = blend(background, text, 0.055F, 255);
        int border = blend(background, text, 0.18F, 150);

        style.setColor(ImGuiCol.Text, text);
        style.setColor(ImGuiCol.TextDisabled, disabled);
        style.setColor(ImGuiCol.WindowBg, background);
        style.setColor(ImGuiCol.ChildBg, ImColor.rgba(0, 0, 0, 0));
        style.setColor(ImGuiCol.PopupBg, popup);
        style.setColor(ImGuiCol.Border, border);
        style.setColor(ImGuiCol.BorderShadow, ImColor.rgba(0, 0, 0, 0));
        style.setColor(ImGuiCol.FrameBg, widget);
        style.setColor(ImGuiCol.FrameBgHovered, blend(widget, text, 0.08F, 255));
        style.setColor(ImGuiCol.FrameBgActive, blend(widget, text, 0.13F, 255));
        style.setColor(ImGuiCol.TitleBg, blend(background, ImColor.rgb(0, 0, 0), 0.14F, 255));
        style.setColor(ImGuiCol.TitleBgActive, elevated);
        style.setColor(ImGuiCol.TitleBgCollapsed, blend(background, ImColor.rgb(0, 0, 0), 0.18F, 255));
        style.setColor(ImGuiCol.MenuBarBg, blend(background, ImColor.rgb(0, 0, 0), 0.12F, 255));
        style.setColor(ImGuiCol.ScrollbarBg, blend(background, ImColor.rgb(0, 0, 0), 0.10F, 150));
        style.setColor(ImGuiCol.ScrollbarGrab, blend(background, text, 0.18F, 210));
        style.setColor(ImGuiCol.ScrollbarGrabHovered, blend(background, text, 0.28F, 230));
        style.setColor(ImGuiCol.ScrollbarGrabActive, blend(background, text, 0.38F, 255));
        style.setColor(ImGuiCol.Separator, border);
        style.setColor(ImGuiCol.TableHeaderBg, elevated);
        style.setColor(ImGuiCol.TableBorderStrong, border);
        style.setColor(ImGuiCol.TableBorderLight, blend(background, text, 0.12F, 105));
        style.setColor(ImGuiCol.TableRowBg, ImColor.rgba(0, 0, 0, 0));
        style.setColor(ImGuiCol.TableRowBgAlt, blend(background, text, 0.035F, 100));
        style.setColor(ImGuiCol.TreeLines, blend(background, text, 0.20F, 145));
        style.setColor(ImGuiCol.DockingEmptyBg, blend(background, ImColor.rgb(0, 0, 0), 0.10F, 255));
        style.setColor(ImGuiCol.ModalWindowDimBg, ImColor.rgba(7, 8, 12, 165));
        style.setColor(ImGuiCol.NavWindowingDimBg, ImColor.rgba(7, 8, 12, 120));
        style.setColor(ImGuiCol.NavWindowingHighlight, blend(background, text, 0.58F, 180));
        style.setColor(ImGuiCol.UnsavedMarker, CodeColorScheme.NOTIFY_WARN);

        applyAccent(accentColor);
    }

    public static void applyAccent(AccentColor accentColor) {
        if (!initialized) return;

        AccentColor selected = accentColor == null ? AccentColor.SAPPHIRE : accentColor;
        ImGuiStyle style = ImGui.getStyle();
        int accent = selected.getColor();
        int background = opaque(CodeColorScheme.BACKGROUND);
        int widget = opaque(CodeColorScheme.WIDGET_BACKGROUND);
        int elevated = blend(background, opaque(CodeColorScheme.TEXT), 0.055F, 255);

        style.setColor(ImGuiCol.CheckMark, accent);
        style.setColor(ImGuiCol.SliderGrab, blend(widget, accent, 0.72F, 255));
        style.setColor(ImGuiCol.SliderGrabActive, accent);
        style.setColor(ImGuiCol.Button, blend(widget, accent, 0.12F, 255));
        style.setColor(ImGuiCol.ButtonHovered, blend(widget, accent, 0.34F, 255));
        style.setColor(ImGuiCol.ButtonActive, blend(widget, accent, 0.48F, 255));
        style.setColor(ImGuiCol.Header, CodeColorScheme.setAlpha(accent, 42));
        style.setColor(ImGuiCol.HeaderHovered, CodeColorScheme.setAlpha(accent, 78));
        style.setColor(ImGuiCol.HeaderActive, CodeColorScheme.setAlpha(accent, 112));
        style.setColor(ImGuiCol.SeparatorHovered, CodeColorScheme.setAlpha(accent, 180));
        style.setColor(ImGuiCol.SeparatorActive, accent);
        style.setColor(ImGuiCol.ResizeGrip, CodeColorScheme.setAlpha(accent, 28));
        style.setColor(ImGuiCol.ResizeGripHovered, CodeColorScheme.setAlpha(accent, 115));
        style.setColor(ImGuiCol.ResizeGripActive, CodeColorScheme.setAlpha(accent, 190));
        style.setColor(ImGuiCol.InputTextCursor, accent);
        style.setColor(ImGuiCol.Tab, elevated);
        style.setColor(ImGuiCol.TabHovered, blend(elevated, accent, 0.25F, 255));
        style.setColor(ImGuiCol.TabSelected, blend(elevated, accent, 0.15F, 255));
        style.setColor(ImGuiCol.TabSelectedOverline, accent);
        style.setColor(ImGuiCol.TabDimmed, blend(background, ImColor.rgb(0, 0, 0), 0.08F, 255));
        style.setColor(ImGuiCol.TabDimmedSelected, blend(background, accent, 0.09F, 255));
        style.setColor(ImGuiCol.TabDimmedSelectedOverline, CodeColorScheme.setAlpha(accent, 145));
        style.setColor(ImGuiCol.DockingPreview, CodeColorScheme.setAlpha(accent, 115));
        style.setColor(ImGuiCol.TextLink, accent);
        style.setColor(ImGuiCol.TextSelectedBg, CodeColorScheme.setAlpha(accent, 82));
        style.setColor(ImGuiCol.DragDropTarget, accent);
        style.setColor(ImGuiCol.DragDropTargetBg, CodeColorScheme.setAlpha(accent, 45));
        style.setColor(ImGuiCol.NavCursor, accent);
    }

    private static int opaque(int color) {
        return CodeColorScheme.setAlpha(color, 255);
    }

    private static int blend(int first, int second, float amount, int alpha) {
        Color a = CodeColorScheme.toColor(first);
        Color b = CodeColorScheme.toColor(second);
        float bounded = Math.max(0.F, Math.min(1.F, amount));
        int red = Math.round(a.getRed() + (b.getRed() - a.getRed()) * bounded);
        int green = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * bounded);
        int blue = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * bounded);
        return ImColor.rgba(red, green, blue, alpha);
    }
}
