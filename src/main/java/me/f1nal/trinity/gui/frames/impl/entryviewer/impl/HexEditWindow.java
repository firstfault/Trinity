package me.f1nal.trinity.gui.frames.impl.entryviewer.impl;

import imgui.ImGui;
import imgui.extension.memedit.MemoryEditor;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.frames.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class HexEditWindow extends ArchiveEntryViewerWindow<ResourceArchiveEntry> implements ICaption {
    private final MemoryEditor memoryEditor = new MemoryEditor();
    private ByteBuffer byteBuffer;

    public HexEditWindow(Trinity trinity, ResourceArchiveEntry archiveEntry) {
        super(trinity, archiveEntry);
        this.byteBuffer = MemoryUtil.memASCII(new String(this.getArchiveEntry().getBytes()));
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible && this.byteBuffer != null) {
            MemoryUtil.memFree(this.byteBuffer);
            this.byteBuffer = null;
        }
        super.setVisible(visible);
    }

    @Override
    protected void renderFrame() {
        ImGui.beginMenuBar();
        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save")) {
                byte[] dst = new byte[byteBuffer.capacity()];
                byteBuffer.position(0);
                byteBuffer.get(dst);
                this.saveBytes(dst, this);
                byteBuffer.position(0);
            }
            ImGui.endMenu();
        }
        ImGui.endMenuBar();
        memoryEditor.drawContents(MemoryUtil.memAddress(this.byteBuffer), this.byteBuffer.capacity());
    }

    @Override
    public String getCaption() {
        return "Hex Editor";
    }
}
