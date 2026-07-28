package me.f1nal.trinity.gui.windows.impl.entryviewer.impl;

import imgui.ImGui;
import imgui.ImDrawList;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class ImageViewerWindow extends ArchiveEntryViewerWindow<ResourceArchiveEntry> {
    private static final float MIN_ZOOM = 0.01F;
    private static final float MAX_ZOOM = 4096.F;
    private static final float ZOOM_STEP = 1.15F;
    private static final float CHECKER_SIZE = 16.F;
    private static final double ZOOM_INDICATOR_DURATION = 1.D;
    private static final double ZOOM_INDICATOR_FADE_DURATION = 0.3D;

    private int textureId;
    private int imageWidth;
    private int imageHeight;
    private boolean loadAttempted;
    private String loadError;
    private float zoom = 1.F;
    private boolean zoomInitialized;
    private double zoomIndicatorUntil;
    private boolean panning;
    private float panX;
    private float panY;

    public ImageViewerWindow(Trinity trinity, ResourceArchiveEntry archiveEntry) {
        super(trinity, archiveEntry);
    }

    @Override
    protected void renderFrame() {
        this.loadTexture();
        if (this.loadError != null) {
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, this.loadError);
            return;
        }

        ImVec2 contentMin = ImGui.getWindowContentRegionMin();
        ImVec2 contentMax = ImGui.getWindowContentRegionMax();
        float availableWidth = contentMax.x - contentMin.x;
        float availableHeight = contentMax.y - contentMin.y;
        float canvasX = ImGui.getWindowPosX() + contentMin.x;
        float canvasY = ImGui.getWindowPosY() + contentMin.y;
        float fitScale = Math.max(MIN_ZOOM, Math.min(
                availableWidth / this.imageWidth,
                availableHeight / this.imageHeight));
        if (!this.zoomInitialized) {
            this.zoom = Math.min(1.F, fitScale);
            this.zoomInitialized = true;
        }
        this.updateZoom();
        this.updateKeyboardZoom();
        float scale = this.zoom;
        float displayWidth = Math.max(1.F, this.imageWidth * scale);
        float displayHeight = Math.max(1.F, this.imageHeight * scale);

        ImVec2 canvasPosition = new ImVec2(canvasX, canvasY);
        ImGui.setCursorScreenPos(canvasPosition);
        ImGui.invisibleButton("##imageCanvas", availableWidth, availableHeight);
        boolean canvasHovered = ImGui.isItemHovered();
        this.drawCheckerboard(canvasX, canvasY, availableWidth, availableHeight);
        this.drawContextMenu(fitScale);
        float imageX = canvasPosition.x + (availableWidth - displayWidth) * 0.5F
                + this.panX;
        float imageY = canvasPosition.y + (availableHeight - displayHeight) * 0.5F
                + this.panY;
        boolean imageHovered = canvasHovered
                && ImGui.getMousePosX() >= imageX
                && ImGui.getMousePosX() <= imageX + displayWidth
                && ImGui.getMousePosY() >= imageY
                && ImGui.getMousePosY() <= imageY + displayHeight;
        float maximumPanX = Math.abs(displayWidth - availableWidth) * 0.5F;
        float maximumPanY = Math.abs(displayHeight - availableHeight) * 0.5F;
        this.updatePanning(imageHovered, maximumPanX, maximumPanY);

        imageX = canvasPosition.x + (availableWidth - displayWidth) * 0.5F
                + this.panX;
        imageY = canvasPosition.y + (availableHeight - displayHeight) * 0.5F
                + this.panY;
        ImGui.getWindowDrawList().addImage(this.textureId, imageX, imageY,
                imageX + displayWidth, imageY + displayHeight);

        if (imageHovered && !this.panning) {
            ImGui.setTooltip(this.imageWidth + " \u00d7 " + this.imageHeight);
        }
        this.drawZoomIndicator(scale);
    }

    private void drawCheckerboard(float x, float y, float width, float height) {
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(x, y, x + width, y + height,
                CodeColorScheme.setAlpha(CodeColorScheme.BACKGROUND, 255));

        int alternateColor = CodeColorScheme.setAlpha(
                CodeColorScheme.WIDGET_BACKGROUND, 65);
        int columns = (int) Math.ceil(width / CHECKER_SIZE);
        int rows = (int) Math.ceil(height / CHECKER_SIZE);
        for (int row = 0; row < rows; row++) {
            for (int column = row & 1; column < columns; column += 2) {
                float tileX = x + column * CHECKER_SIZE;
                float tileY = y + row * CHECKER_SIZE;
                drawList.addRectFilled(tileX, tileY,
                        Math.min(tileX + CHECKER_SIZE, x + width),
                        Math.min(tileY + CHECKER_SIZE, y + height),
                        alternateColor);
            }
        }
    }

    private void updateZoom() {
        float mouseWheel = ImGui.getIO().getMouseWheel();
        if (!ImGui.isWindowHovered() || !ImGui.getIO().getKeyCtrl()
                || mouseWheel == 0.F) {
            return;
        }

        this.setZoom(this.zoom * (float) Math.pow(ZOOM_STEP, mouseWheel));
        ImGui.getIO().setMouseWheel(0.F);
    }

    private void updateKeyboardZoom() {
        if (!this.isWindowFocused()) return;

        boolean zoomIn = ImGui.isKeyChordPressed(
                ImGuiKey.ImGuiMod_Ctrl | ImGuiKey.Equal)
                || ImGui.isKeyChordPressed(ImGuiKey.ImGuiMod_Ctrl
                | ImGuiKey.ImGuiMod_Shift | ImGuiKey.Equal)
                || ImGui.isKeyChordPressed(
                ImGuiKey.ImGuiMod_Ctrl | ImGuiKey.KeypadAdd);
        boolean zoomOut = ImGui.isKeyChordPressed(
                ImGuiKey.ImGuiMod_Ctrl | ImGuiKey.Minus)
                || ImGui.isKeyChordPressed(
                ImGuiKey.ImGuiMod_Ctrl | ImGuiKey.KeypadSubtract);
        if (zoomIn) {
            this.setZoom(this.zoom * ZOOM_STEP);
        } else if (zoomOut) {
            this.setZoom(this.zoom / ZOOM_STEP);
        }
    }

    private void drawContextMenu(float fitScale) {
        if (!ImGui.beginPopupContextItem("##imageViewerContext")) return;

        if (ImGui.menuItem("Zoom In", "Ctrl + +")) {
            this.setZoom(this.zoom * ZOOM_STEP);
        }
        if (ImGui.menuItem("Zoom Out", "Ctrl + -")) {
            this.setZoom(this.zoom / ZOOM_STEP);
        }
        ImGui.separator();
        if (ImGui.menuItem("Fit to Window")) {
            this.zoom = fitScale;
            this.centerImage();
            this.showZoomIndicator();
        }
        if (ImGui.menuItem("Reset Zoom")) {
            this.zoom = 1.F;
            this.centerImage();
            this.showZoomIndicator();
        }
        if (ImGui.menuItem("Center Image")) {
            this.centerImage();
        }
        ImGui.endPopup();
    }

    private void setZoom(float zoom) {
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        this.showZoomIndicator();
    }

    private void centerImage() {
        this.panX = 0.F;
        this.panY = 0.F;
    }

    private void showZoomIndicator() {
        this.zoomIndicatorUntil = ImGui.getTime() + ZOOM_INDICATOR_DURATION;
    }

    private void updatePanning(boolean imageHovered, float maximumPanX,
                               float maximumPanY) {
        boolean canPan = maximumPanX > 0.F || maximumPanY > 0.F;
        if (canPan && imageHovered && ImGui.isItemActive()
                && ImGui.isMouseDown(0)) {
            this.panning = true;
        }
        if (!ImGui.isMouseDown(0) || !canPan) {
            this.panning = false;
        }
        if (!canPan) {
            this.panX = 0.F;
            this.panY = 0.F;
            return;
        }

        if (imageHovered || this.panning) {
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll);
        }
        if (this.panning) {
            this.panX += ImGui.getIO().getMouseDeltaX();
            this.panY += ImGui.getIO().getMouseDeltaY();
        }
        this.panX = Math.max(-maximumPanX, Math.min(maximumPanX, this.panX));
        this.panY = Math.max(-maximumPanY, Math.min(maximumPanY, this.panY));
    }

    private void drawZoomIndicator(float scale) {
        double remaining = this.zoomIndicatorUntil - ImGui.getTime();
        if (remaining <= 0.D) return;

        float alpha = remaining >= ZOOM_INDICATOR_FADE_DURATION
                ? 1.F
                : (float) (remaining / ZOOM_INDICATOR_FADE_DURATION);
        String label = Math.round(scale * 100.F) + "%";
        ImVec2 textSize = ImGui.calcTextSize(label);
        ImVec2 contentMin = ImGui.getWindowContentRegionMin();
        ImVec2 contentMax = ImGui.getWindowContentRegionMax();
        float paddingX = 7.F;
        float paddingY = 4.F;
        float margin = 8.F;
        float right = ImGui.getWindowPosX() + contentMax.x - margin;
        float top = ImGui.getWindowPosY() + contentMin.y + margin;
        float left = right - textSize.x - paddingX * 2.F;
        float bottom = top + textSize.y + paddingY * 2.F;

        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(left, top, right, bottom,
                CodeColorScheme.setAlpha(CodeColorScheme.BACKGROUND,
                        Math.round(220.F * alpha)), 4.F);
        drawList.addText(left + paddingX, top + paddingY,
                CodeColorScheme.setAlpha(CodeColorScheme.TEXT,
                        Math.round(255.F * alpha)), label);
    }

    private void loadTexture() {
        if (this.loadAttempted) return;
        this.loadAttempted = true;

        BufferedImage image;
        try (ByteArrayInputStream input =
                     new ByteArrayInputStream(this.getArchiveEntry().getBytes())) {
            image = ImageIO.read(input);
        } catch (IOException exception) {
            this.loadError = "Unable to decode image: " + exception.getMessage();
            return;
        }
        if (image == null) {
            this.loadError = "Unable to decode this image.";
            return;
        }

        this.imageWidth = image.getWidth();
        this.imageHeight = image.getHeight();
        int pixelBufferSize;
        try {
            pixelBufferSize = Math.multiplyExact(
                    Math.multiplyExact(this.imageWidth, this.imageHeight), 4);
        } catch (ArithmeticException exception) {
            this.loadError = "Image dimensions are too large.";
            return;
        }

        ByteBuffer pixels = MemoryUtil.memAlloc(pixelBufferSize);
        try {
            int[] row = new int[this.imageWidth];
            for (int y = 0; y < this.imageHeight; y++) {
                image.getRGB(0, y, this.imageWidth, 1, row, 0, this.imageWidth);
                for (int argb : row) {
                    pixels.put((byte) (argb >> 16 & 0xFF));
                    pixels.put((byte) (argb >> 8 & 0xFF));
                    pixels.put((byte) (argb & 0xFF));
                    pixels.put((byte) (argb >> 24 & 0xFF));
                }
            }
            pixels.flip();

            int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int previousUnpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
            this.textureId = GL11.glGenTextures();
            try {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                        GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                        GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
                        GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
                        GL12.GL_CLAMP_TO_EDGE);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                        this.imageWidth, this.imageHeight, 0, GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE, pixels);
            } finally {
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousUnpackAlignment);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            }
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible && this.textureId != 0) {
            GL11.glDeleteTextures(this.textureId);
            this.textureId = 0;
        }
        super.setVisible(visible);
    }
}
