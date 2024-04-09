package me.f1nal.trinity.gui.windows.impl.entryviewer;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.execution.packages.ArchiveEntry;

import java.util.ArrayList;
import java.util.List;

@Deprecated(forRemoval = true)
public class ArchiveEntryViewerFacade {
    private final List<ArchiveEntry> historyBackward = new ArrayList<>();
    private final List<ArchiveEntry> historyForward = new ArrayList<>();
    private ArchiveEntry skipNextBackwardHistory;
    private ArchiveEntry backwardHistory;

    public void resetDecompilerComponents() {
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
    }

    public void addBackwardHistory(ArchiveEntry archiveEntry) {
        if (archiveEntry != null) {

            if (this.skipNextBackwardHistory == archiveEntry) {
                this.skipNextBackwardHistory = null;
                return;
            }
            if (this.backwardHistory != null) {
                this.historyBackward.add(backwardHistory);
                while (this.historyBackward.size() > 30) this.historyBackward.remove(0);
                this.historyForward.clear();
                this.backwardHistory = null;
            }
            if (!this.historyBackward.isEmpty() && this.historyBackward.get(this.historyBackward.size() - 1) == archiveEntry) {
                return;
            }
            this.backwardHistory = archiveEntry;
        }
    }

    public void drawHistoryButtons(ArchiveEntry selectedClass) {
        this.historyButton("<", 3, "Back", historyBackward.isEmpty(), () -> {
            this.historyForward.add(selectedClass);
            this.openViewer(historyBackward.remove(historyBackward.size() - 1));
        });
        this.historyButton(">", 4, "Forward", historyForward.isEmpty(), () -> {
            this.historyBackward.add(selectedClass);
            this.openViewer(this.historyForward.remove(this.historyForward.size() - 1));
        });
    }

    private void openViewer(ArchiveEntry archiveEntry) {
        this.skipNextBackwardHistory = archiveEntry;
        Main.getWindowManager().addClosableWindow(archiveEntry.getDefaultViewer());
    }

    private void historyButton(String label, int mouseButton, String tooltip, boolean disabled, Runnable runnable) {
        if (!disabled && ImGui.isMouseClicked(mouseButton)) {
            runnable.run();
        }
    }
}
