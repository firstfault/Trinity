package me.f1nal.trinity.gui.components.filelist;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.MoveButtonEnum;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.components.general.ListBoxComponent;
import me.f1nal.trinity.gui.windows.api.AbstractWindow;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.util.GuiUtil;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class FileListBoxComponent<T extends ListedFile> implements ICaption {
    private final AbstractWindow parentWindow;
    private final String label;
    private final ListBoxComponent<T> listBoxComponent = new ListBoxComponent<>(new ArrayList<>());
    private final FileSelectorComponent fileSelectorComponent;
    private final ListedFileFactory<T> listedFileFactory;
    private Consumer<T> elementAddEvent;

    public FileListBoxComponent(AbstractWindow parentWindow, String label, FilenameFilter filter, ListedFileFactory<T> listedFileFactory) {
        this.parentWindow = parentWindow;
        this.label = label;
        this.listedFileFactory = listedFileFactory;
        this.fileSelectorComponent = new FileSelectorComponent("Add Class Path", new File("").getAbsolutePath(), filter, FileDialog.LOAD);
    }

    public void setElementAddEvent(Consumer<T> elementAddEvent) {
        this.elementAddEvent = elementAddEvent;
    }

    public ListBoxComponent<T> getListBoxComponent() {
        return listBoxComponent;
    }

    public void draw() {
        ImGui.text(this.label);
        listBoxComponent.draw(ImGui.getContentRegionAvailX(), 80.F);
        if (ImGui.button(FontAwesomeIcons.Plus + " Add")) {
            this.addFile(fileSelectorComponent.openFileChooser());
        }
        ImGui.sameLine();
        if (GuiUtil.disabledWidget(listBoxComponent.getSelection() == null, () -> ImGui.button(FontAwesomeIcons.Trash + " Remove"))) {
            listBoxComponent.removeElement(listBoxComponent.getSelection());
        }
        Arrays.stream(MoveButtonEnum.values()).forEach(m -> {
            ImGui.sameLine();
            m.draw(listBoxComponent.getSelection(), listBoxComponent.getElementList());
        });
        ImGui.sameLine();
        GuiUtil.disabledWidget(listBoxComponent.getSelection() == null, () -> {
            if (ImGui.button(FontAwesomeIcons.Eye + " View File")) {
                listedFileFactory.view(listBoxComponent.getSelection());
            }
        });
    }

    public void addFile(File file) {
        if (file == null) {
            return;
        }

        T listedFile = this.listedFileFactory.create(file);

        if (listedFile == null) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.INFO, this,
                    ColoredStringBuilder.create().fmt("File {} is invalid or could not be read", file.getName()).get()));
            return;
        }

        listBoxComponent.addElement(listedFile);

        if (this.elementAddEvent != null) this.elementAddEvent.accept(listedFile);
    }

    @Override
    public String getCaption() {
        return this.label;
    }
}
