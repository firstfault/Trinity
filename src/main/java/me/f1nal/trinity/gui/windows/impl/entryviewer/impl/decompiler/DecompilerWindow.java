package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseDecompiler;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.packages.other.ExtractArchiveEntryRunnable;
import me.f1nal.trinity.gui.components.popup.MenuBarProgress;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructure;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructureWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;
import me.f1nal.trinity.util.TimedStopwatch;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class DecompilerWindow extends ArchiveEntryViewerWindow<ClassTarget> implements IEventListener, IDatabaseSavable<DatabaseDecompiler> {
    private ClassInput selectedClass;
    /**
     * Notifies the selected class must be refreshed.
     */
    private TimedStopwatch forceRefresh;
    /**
     * Text component that is currently hovered.
     */
    private DecompilerComponent hoveredComponent;
    private boolean resetLines;
    /**
     * Selection cursor.
     */
    public final DecompilerCursor cursor = new DecompilerCursor(this);
    private DecompilerAutoScroll autoscrollTo;

    public DecompilerWindow(ClassTarget classTarget, Trinity trinity) {
        super(trinity, classTarget);
        trinity.getEventManager().registerListener(this);
        this.setDecompileTarget(Objects.requireNonNull(classTarget.getInput()));
        this.setMenuBar(new PopupMenuBar(PopupItemBuilder.create().
                menu("File", file -> {
                    file
                            .menuItem("Refresh", () -> this.forceRefresh = new TimedStopwatch(0L))
                            .predicate(() -> getDecompiledClass() != null, b -> b.separator()
                                    .menuItem("Copy", () -> SystemUtil.copyToClipboard(getDecompiledClass().getText()))
                                    .menuItem("Save", () -> new ExtractArchiveEntryRunnable(classTarget.getDisplaySimpleName() + ".java", getDecompiledClass().getText().getBytes()).run()))
                    ;
                }).
                menu("Find", find -> {})));
    }

    @Override
    public String getTitle() {
        return getArchiveEntry().getDisplayOrRealName() + ".java";
    }

    public void setDecompileTarget(ClassInput classInput) {
        if (classInput == selectedClass) {
            return;
        }
        selectedClass = classInput;
        if (classInput != null && !trinity.getDatabase().isLoading()) this.save();
        if (this.isFocusGained()) this.updateClassStructure();
    }

    @Override
    protected void onFocusGain() {
        this.updateClassStructure();
    }

    private void updateClassStructure() {
        if (this.selectedClass != null) {
            Main.getWindowManager().addStaticWindow(ClassStructureWindow.class).setClassStructure(new ClassStructure(this.selectedClass));
        }
    }

    public ClassInput getSelectedClass() {
        return selectedClass;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    protected void renderFrame() {
        if (selectedClass != null) {
            this.drawDecompileTab();
        } else {
            ImGui.text("No class selected");
        }
        DecompiledClass decompiledClass = this.getDecompiledClass();
        getMenuBar().setProgress(decompiledClass == null ? new MenuBarProgress("Decompiler", "Decompiling Class", -1) : null);
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        if (event.getClassInput() == this.selectedClass) {
            this.forceRefreshLines();
        }
    }

    public void forceRefreshLines() {
        this.forceRefresh = new TimedStopwatch(0L);
    }

    @Subscribe
    public void onRefreshDecompilerText(EventRefreshDecompilerText event) {
        DecompiledClass decompiledClass = getDecompiledClass();
        if (getDecompiledClass() != null && event.getPredicate().test(decompiledClass)) {
            this.resetLines = true;
        }
    }

    private ClassInput decompilingInput;

    private void drawDecompileTab() {
        this.runControls();

        ImGui.beginChild("DecompilerWindowChild", 0.F, 0.F, false, ImGuiWindowFlags.HorizontalScrollbar);

        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass == null) {
            ImGui.textUnformatted("...");
        } else {
            if (this.resetLines) {
                decompiledClass.resetLines();
                this.resetLines = false;
            }
            this.drawDecompiledOutput(decompiledClass);
        }

        ImGui.endChild();
    }

    private void runControls() {
        boolean refreshDecompiler = (this.forceRefresh != null && this.forceRefresh.hasPassed());
        if (refreshDecompiler) {
            this.forceRefresh = null;
            try {
                trinity.getDecompiler().decompile(selectedClass, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Main.getDisplayManager().getArchiveEntryViewerFacade().drawHistoryButtons(this.selectedClass.getClassTarget());

        if (trinity.getDecompiler().isDecompileFailed(selectedClass)) {
            ImGui.textColored(ImColor.rgb(245, 80, 80), "Decompilation failed");
        }

        if (decompilingInput != selectedClass) {
            if (trinity.getDecompiler().getFromCache(selectedClass) == null) {
                try {
                    trinity.getDecompiler().decompile(selectedClass, decompiledClass -> {
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            decompilingInput = selectedClass;
        }
    }

    public DecompiledClass getDecompiledClass() {
        return trinity.getDecompiler().getFromCache(selectedClass);
    }

    private void drawDecompiledOutput(DecompiledClass decompiledClass) {
        this.hoveredComponent = null;

        float mousePosY = ImGui.getMousePosY() + ImGui.getScrollY() - ImGui.getWindowPosY();
        float mousePosX = ImGui.getMousePosX() + ImGui.getScrollX();

        ImVec2 textSize = ImGui.calcTextSize(String.valueOf(decompiledClass.getLines().size() + 1));
        float lineNumberSpacing = 3.F + textSize.x;
        float cursorPosX = ImGui.getCursorPosX();

        cursor.handleInputs(mousePosX, mousePosY);

        for (DecompilerLine line : decompiledClass.getLines()) {
            final float cursorScreenPosX = ImGui.getCursorScreenPosX();

            int textOffset = 0;
            ImGui.setCursorPosX(cursorPosX + lineNumberSpacing);
            for (DecompilerLineText text : line.getComponents()) {
                if (!text.getComponent().render()) {
                    text.render(decompiledClass.isComponentHighlighted(text.getComponent()));
                    ImGui.sameLine(0.F, 0.F);
                }

                if (this.autoscrollTo != null && text.getComponent() == this.autoscrollTo.findComponent(decompiledClass)) {
                    cursor.setCoordinates(new DecompilerCoordinates(line, textOffset));
                    cursor.setScrollToCursor();
                    this.autoscrollTo = null;
                }

                if (this.hoveredComponent == null && ImGui.isItemHovered()) {
                    this.hoveredComponent = text.getComponent();
                }

                textOffset += text.getText().length();

            }

            float cursorPosY = ImGui.getCursorPosY();
            final boolean hovered = ImGui.isWindowHovered() && mousePosY >= cursorPosY && mousePosY < cursorPosY + textSize.y + ImGui.getStyle().getItemSpacingY();

            if (hovered)
                this.cursor.handleHoveredLineInputs(cursorScreenPosX, lineNumberSpacing, mousePosX, line);

            ImGui.setCursorPosX(cursorPosX);
            ImGui.textColored(CodeColorScheme.LINE_NUMBER, String.valueOf(line.getLineNumber()));
            ImGui.sameLine();

            this.cursor.handleLineDrawing(line, cursorScreenPosX, lineNumberSpacing, mousePosX, cursorPosY, textSize);

            ImGui.newLine();
        }

        boolean rightClick = ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Right);

        if (this.hoveredComponent != null) {
            List<ColoredString> tooltip = this.hoveredComponent.createTooltip();

            if (tooltip != null) {
                ImGui.beginTooltip();

                ColoredString.drawText(tooltip);

                ImGui.endTooltip();
            }

            if (rightClick) {
                PopupItemBuilder popup = this.hoveredComponent.createPopup();

                if (!popup.isEmpty()) {
                    Main.getDisplayManager().showPopup(popup);
                    rightClick = false;
                }
            }
        }

        if (rightClick) {
            Main.getDisplayManager().showPopup(PopupItemBuilder.create().disabled(() -> cursor.selectionEnd == null, items -> {
                items.menuItem("Copy", () -> {
                });
            }));
        }
    }

    private String formatClassName() {
        return null;
    }

    public void setDecompileTarget(Input<?> input) {
        this.autoscrollTo = new DecompilerAutoScroll(this, input);
        this.setDecompileTarget(input.getOwningClass());
    }

    @Override
    public DatabaseDecompiler createDatabaseObject() {
        return new DatabaseDecompiler(this.selectedClass.getFullName());
    }
}
