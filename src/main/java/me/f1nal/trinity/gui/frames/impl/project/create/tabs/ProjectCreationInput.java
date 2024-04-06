package me.f1nal.trinity.gui.frames.impl.project.create.tabs;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.ProjectInputFileFactory;
import me.f1nal.trinity.gui.components.filelist.FileListBoxComponent;
import me.f1nal.trinity.gui.frames.Frame;
import me.f1nal.trinity.gui.frames.impl.project.create.AbstractProjectCreationTab;

import java.io.FilenameFilter;

public class ProjectCreationInput extends AbstractProjectCreationTab {
    private static final FilenameFilter INPUT_FILTER = (dir, name) -> {
        final String[] extensions = {
                "zip",
                "jar",
                "class"
        };
        final String lowerCase = name.toLowerCase();

        for (String extension : extensions) {
            if (lowerCase.endsWith(".".concat(extension))) {
                return true;
            }
        }

        return false;
    };

    private final FileListBoxComponent<AbstractProjectInputFile> fileListComponent;

    public ProjectCreationInput(Frame parentWindow) {
        this.fileListComponent = new FileListBoxComponent<>(parentWindow, "Input Class Path", INPUT_FILTER, new ProjectInputFileFactory());
    }

    public FileListBoxComponent<AbstractProjectInputFile> getFileListComponent() {
        return fileListComponent;
    }

    public ClassPath createClassPath() {
        ClassPath classPath = new ClassPath();
        for (AbstractProjectInputFile inputFile : this.fileListComponent.getListBoxComponent().getElementList()) {
            classPath.addClassPath(inputFile.getClassPath());
        }
        return classPath;
    }

    @Override
    public void drawTabContent() {
        fileListComponent.draw();
    }

    @Override
    public String getName() {
        return "Input";
    }

    @Override
    public String getDescription() {
        return "Target files to reverse engineer";
    }

    @Override
    protected boolean isInputValid() {
        return !fileListComponent.getListBoxComponent().getElementList().isEmpty();
    }
}
