package me.f1nal.trinity.gui.windows.impl.refactor;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.gui.components.general.ListBoxComponent;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.remap.NameHeuristics;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public class GlobalRenameWindow extends StaticWindow implements ICaption {
    private final ListBoxComponent<GlobalRenameType> listBox;

    public GlobalRenameWindow(Trinity trinity) {
        super("Global Rename", 0, 0, trinity);
        this.windowFlags |= ImGuiWindowFlags.NoResize;
        this.listBox = new ListBoxComponent<>(trinity.getRefactorManager().getGlobalRenameTypes());
    }

    @Override
    protected void renderFrame() {
        this.listBox.draw(160.F, 300.F);
        ImGui.sameLine();
        ImGui.beginGroup();
        if (ImGui.beginChild("GlobalRenameTypeChild", 500.F, 270.F)) {
            GlobalRenameType type = this.listBox.getSelection();

            ImGui.text(type.getName());

            ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.TEXT);
            ImGui.textWrapped(type.getDescription());
            ImGui.popStyleColor();

            ImGui.separator();

            ImGui.beginChild("GlobalRenameTypeInputsChild");
            type.drawInputs();
            ImGui.endChild();
        }
        ImGui.endChild();
        if (ImGui.beginChild("GlobalRenameTypeControlsChild", 0.F, 0.F)) {
            ImGui.separator();
            if (ImGui.button("Start Refactor")) {
                this.runRefactor();
            }
            ImGui.sameLine();
            ImGui.progressBar(0.F);
        }
        ImGui.endChild();
        ImGui.endGroup();
    }

    private void runRefactor() {
        GlobalRenameType type = this.listBox.getSelection();
        NameHeuristics nameHeuristics = trinity.getRemapper().getNameHeuristics();
        List<Rename> renames = new ArrayList<>();
        GlobalRenameContext context = new GlobalRenameContext(trinity.getExecution(), renames, nameHeuristics);
        type.refactor(context);
        renames.forEach(rename -> rename.rename(trinity.getRemapper()));
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
        Main.getDisplayManager().addNotification(new Notification(NotificationType.INFO, this, ColoredStringBuilder.create().fmt("Issued a global rename on {} objects", renames.size()).get()));
    }

    @Override
    public String getCaption() {
        return "Global Rename";
    }
}
