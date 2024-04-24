package me.f1nal.trinity.gui.viewport;

import com.google.common.io.Resources;
import imgui.*;
import imgui.gl3.ImGuiImplGl3;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.DecompilerFontEnum;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;

import java.io.IOException;

public class FontManager {
    public static final float DEFAULT_SIZE = 14.F;
    private float currentFontSize;

    public void setupFonts() {
        this.buildFonts(this.getFontSize());
    }

    public void resetFontsIfNeeded(ImGuiImplGl3 imGuiGlfw) {
        final float fontSize = this.getFontSize();
        if (this.currentFontSize != fontSize) {
        }
    }

    public float getCurrentFontSize() {
        return currentFontSize;
    }

    public float getFontSize() {
        return Main.getPreferences().getFontSize();
    }

    public float getGlobalScale() {
        return getFontSize() / getCurrentFontSize();
    }

    private void buildFonts(final float size) {
        ImGuiIO io = ImGui.getIO();

        DecompilerFontEnum.INTER.setFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fonts/inter-regular.ttf"), size));

        final ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setMergeMode(true);

        final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());
        rangesBuilder.addRanges(FontAwesomeIcons._IconRange);

        final short[] glyphRanges = rangesBuilder.buildRanges();

        io.getFonts().addFontFromMemoryTTF(loadFromResources("fonts/fa-solid-900.ttf"), size, fontConfig, glyphRanges);
        DecompilerFontEnum.JETBRAINS_MONO.setFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fonts/JetBrainsMonoNL-Regular.ttf"), size + 1.F));
        DecompilerFontEnum.ZED_MONO.setFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fonts/zed-mono-regular.ttf"), size + 1.F));
        this.currentFontSize = size;
    }

    private static byte[] loadFromResources(String name) {
        try {
            return Resources.toByteArray(Resources.getResource(name));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Loading resource '%s'", name), e);
        }
    }
}
