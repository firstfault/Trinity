package me.f1nal.trinity.gui.viewport;

import com.google.common.io.Resources;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;
import imgui.ImGuiIO;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.gui.components.CodiconIcons;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.FontSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FontManager {
    private static final List<String> FONT_RESOURCES = List.of(
            "inter-regular.ttf",
            "JetBrainsMonoNL-Regular.ttf",
            "zed-mono-regular.ttf",
            "smallest_pixel-7.ttf",
            "fa-solid-900.ttf",
            "codicon.ttf"
    );

    /** Font data must remain reachable for as long as ImGui retains its native pointers. */
    private final Map<String, byte[]> resourceCache = new HashMap<>();
    private final List<short[]> glyphRangeCache = new ArrayList<>();
    private ImFont codiconFont;

    public void setupFonts() {
        List<ImFontConfig> fontConfigs = new ArrayList<>();

        // Finish every potentially large Java allocation before giving ImGui pointers into
        // these arrays. The atlas is built synchronously immediately after registration.
        FONT_RESOURCES.forEach(this::loadFromResources);

        try {
            this.buildFonts(Main.getPreferences().getDefaultFont(), fontConfigs);
            this.buildFonts(Main.getPreferences().getDecompilerFont(), fontConfigs);

            ImFontConfig codiconConfig = createJavaOwnedFontConfig(fontConfigs);
            codiconConfig.setPixelSnapH(true);
            this.codiconFont = ImGui.getIO().getFonts().addFontFromMemoryTTF(
                    loadFromResources("codicon.ttf"),
                    Main.getPreferences().getDefaultFont().getSize(),
                    codiconConfig,
                    CodiconIcons.ICON_RANGE);

            ImFontAtlas fontAtlas = ImGui.getIO().getFonts();
            if (!fontAtlas.build()) {
                throw new IllegalStateException("Unable to build the ImGui font atlas");
            }
        } finally {
            fontConfigs.forEach(ImFontConfig::destroy);
        }
    }

    public ImFont getCodiconFont() {
        return codiconFont;
    }

    private void buildFonts(FontSettings fontSettings, List<ImFontConfig> fontConfigs) {
        final float size = fontSettings.getSize();
        fontSettings.setBuiltSize(size);

        final ImGuiIO io = ImGui.getIO();
        final ImFontConfig baseFontConfig = createJavaOwnedFontConfig(fontConfigs);
        final ImFontConfig iconFontConfig = createJavaOwnedFontConfig(fontConfigs);
        iconFontConfig.setMergeMode(true);
        final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());
        rangesBuilder.addRanges(FontAwesomeIcons._IconRange);
        final short[] glyphRanges = rangesBuilder.buildRanges();
        glyphRangeCache.add(glyphRanges);

        fontSettings.registerFont(FontEnum.INTER, io.getFonts().addFontFromMemoryTTF(
                loadFromResources("inter-regular.ttf"), size, baseFontConfig));
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(
                loadFromResources("fa-solid-900.ttf"), size, iconFontConfig, glyphRanges));

        fontSettings.registerFont(FontEnum.JETBRAINS_MONO, io.getFonts().addFontFromMemoryTTF(
                loadFromResources("JetBrainsMonoNL-Regular.ttf"), size, baseFontConfig));
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(
                loadFromResources("fa-solid-900.ttf"), size, iconFontConfig, glyphRanges));

        fontSettings.registerFont(FontEnum.ZED_MONO, io.getFonts().addFontFromMemoryTTF(
                loadFromResources("zed-mono-regular.ttf"), size, baseFontConfig));
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(
                loadFromResources("fa-solid-900.ttf"), size, iconFontConfig, glyphRanges));

        fontSettings.registerFont(FontEnum.SMALLEST_PIXEL, io.getFonts().addFontFromMemoryTTF(
                loadFromResources("smallest_pixel-7.ttf"), size, baseFontConfig));
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(
                loadFromResources("fa-solid-900.ttf"), size, iconFontConfig, glyphRanges));
    }

    private ImFontConfig createJavaOwnedFontConfig(List<ImFontConfig> fontConfigs) {
        ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setFontDataOwnedByAtlas(false);
        fontConfigs.add(fontConfig);
        return fontConfig;
    }

    private byte[] loadFromResources(String name) {
        return resourceCache.computeIfAbsent(name, key -> {
            try {
                return Resources.toByteArray(Resources.getResource("fonts/" + key));
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load font resource '" + key + "'", exception);
            }
        });
    }
}
