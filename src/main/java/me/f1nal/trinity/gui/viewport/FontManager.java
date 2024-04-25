package me.f1nal.trinity.gui.viewport;

import com.google.common.io.Resources;
import imgui.*;
import imgui.gl3.ImGuiImplGl3;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.FontSettings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private Map<String, byte[]> resourceCache;

    public void setupFonts() {
        this.resourceCache = new HashMap<>();

        this.buildFonts(Main.getPreferences().getDefaultFont());
        this.buildFonts(Main.getPreferences().getDecompilerFont());

        this.resourceCache = null;
    }

    private void buildFonts(FontSettings fontSettings) {
        final float size = fontSettings.getSize();
        fontSettings.setBuiltSize(size);

        final ImGuiIO io = ImGui.getIO();
        final ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setMergeMode(true);
        final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());
        rangesBuilder.addRanges(FontAwesomeIcons._IconRange);
        final short[] glyphRanges = rangesBuilder.buildRanges();

        fontSettings.registerFont(FontEnum.INTER, io.getFonts().addFontFromMemoryTTF(loadFromResources("inter-regular.ttf"), size));
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fa-solid-900.ttf"), size, fontConfig, glyphRanges));

        fontSettings.registerFont(FontEnum.JETBRAINS_MONO, io.getFonts().addFontFromMemoryTTF(loadFromResources("JetBrainsMonoNL-Regular.ttf"), size));
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fa-solid-900.ttf"), size, fontConfig, glyphRanges));

        fontSettings.registerFont(FontEnum.ZED_MONO, io.getFonts().addFontFromMemoryTTF(loadFromResources("zed-mono-regular.ttf"), size));
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fa-solid-900.ttf"), size, fontConfig, glyphRanges));
    }

    private byte[] loadFromResources(String name) {
        return resourceCache.computeIfAbsent(name, (k) -> {
            try {
                return Resources.toByteArray(Resources.getResource("fonts/".concat(k)));
            } catch (IOException e) {
                throw new RuntimeException(String.format("Loading font resource '%s'", name), e);
            }
        });
    }
}
