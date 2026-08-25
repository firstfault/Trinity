package me.f1nal.trinity.gui.windows.impl.pattern;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.pattern.InstructionPatternCompiler;
import me.f1nal.trinity.execution.pattern.PatternDiagnostic;
import me.f1nal.trinity.execution.pattern.PatternReplaceSession;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.theme.CodeColorScheme;

public final class PatternReplaceFrame extends StaticWindow implements IEventListener {

    private static final long FRAME_BUDGET_NANOS = 4_000_000L;

    private final AssemblerPatternEditor searchEditor;
    private final AssemblerPatternEditor replaceEditor;
    private final MemorableCheckboxComponent includeMetadata = new MemorableCheckboxComponent(
            "patternReplaceIncludeMetadata", "Include Metadata", false);

    private InstructionPatternCompiler.Compilation searchCompilation;
    private String currentSearchText = "";
    private String currentReplaceText = "";
    private PatternReplaceSession session;
    private String staleMessage;
    private String lastError;

    public PatternReplaceFrame(Trinity trinity) {
        super("Bytecode Search & Replace", 700, 530, trinity);
        this.setDialog(true);
        this.windowFlags = ImGuiWindowFlags.NoCollapse;
        this.searchEditor = new AssemblerPatternEditor(trinity);
        this.replaceEditor = new AssemblerPatternEditor(trinity);
        this.searchCompilation = InstructionPatternCompiler.compile("", false);
        trinity.getEventManager().registerListener(this);
    }

    @Override
    protected void onOpen() {
        searchEditor.requestFocus();
    }

    @Override
    protected void renderFrame() {
        ImGui.textColored(CodeColorScheme.DISABLED,
                "Find an instruction pattern across all methods and replace every match.");

        float availableHeight = ImGui.getContentRegionAvailY();
        boolean sessionActive = session != null;
        float controlsHeight = ImGui.getFrameHeightWithSpacing() * (sessionActive ? 3.5f : 2.5f);
        float splitHeight = Math.max(80f, (availableHeight - controlsHeight) * 0.5f - 4f);

        drawSectionHeader("Search Pattern", CodeColorScheme.KEYWORD_DATA);
        AssemblerPatternEditor.EditorState searchState = searchEditor.draw(splitHeight);
        currentSearchText = searchState.text();
        if (searchState.changed()) {
            searchCompilation = InstructionPatternCompiler.compile(currentSearchText, includeMetadata.isChecked());
            invalidateSession("Pattern changed");
        }

        ImGui.spacing();
        drawSectionHeader("Replacement", CodeColorScheme.NOTIFY_SUCCESS);
        AssemblerPatternEditor.EditorState replaceState = replaceEditor.draw(splitHeight);
        currentReplaceText = replaceState.text();

        ImGui.spacing();
        drawDiagnostic();
        ImGui.spacing();

        boolean metaBefore = includeMetadata.isChecked();
        includeMetadata.draw();
        if (metaBefore != includeMetadata.isChecked()) {
            searchCompilation = InstructionPatternCompiler.compile(currentSearchText, includeMetadata.isChecked());
            invalidateSession("Metadata setting changed");
        }

        ImGui.sameLine();

        if (sessionActive) {
            session.advance(FRAME_BUDGET_NANOS);
            float barWidth = Math.max(1f, ImGui.getContentRegionAvailX() - 80f);
            ImGui.progressBar(session.progress(), barWidth, 0f,
                    session.methodsProcessed() + " / " + session.methodCount() + " methods");
            ImGui.sameLine();
            if (ImGui.button("Cancel")) {
                cancelSession();
            }
            if (session != null && session.isFinished()) {
                onSessionFinished();
            }
        } else {
            boolean canReplace = searchCompilation.valid();
            if (!canReplace) ImGui.beginDisabled();
            boolean clicked = ImGui.button("Replace All");
            if (!canReplace) ImGui.endDisabled();
            if (clicked && canReplace) startSession();

            if (lastError != null) {
                ImGui.sameLine();
                ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, lastError);
            }
        }
    }

    private void drawSectionHeader(String label, int color) {
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float w = ImGui.getContentRegionAvailX();
        float h = ImGui.getTextLineHeight() + 4f;
        ImGui.getWindowDrawList().addRectFilled(x, y, x + w, y + h,
                CodeColorScheme.setAlpha(color, 35), 2f);
        ImGui.setCursorPosX(ImGui.getCursorPosX() + 4f);
        ImGui.textColored(color, label);
        ImGui.spacing();
    }

    private void drawDiagnostic() {
        if (staleMessage != null) {
            ImGui.textColored(CodeColorScheme.NOTIFY_WARN, staleMessage);
            return;
        }
        PatternDiagnostic diagnostic = searchCompilation.primaryDiagnostic();
        if (diagnostic == null) {
            ImGui.textColored(CodeColorScheme.NOTIFY_SUCCESS, "Search pattern valid \u2013 ready to replace");
            return;
        }
        int color = diagnostic.severity() == PatternDiagnostic.Severity.ERROR
                ? CodeColorScheme.NOTIFY_ERROR : CodeColorScheme.DISABLED;
        ImGui.textColored(color, "Line " + diagnostic.line() + ", col "
                + diagnostic.column() + ": " + diagnostic.message());
    }

    private void startSession() {
        lastError = null;
        staleMessage = null;
        session = new PatternReplaceSession(trinity, searchCompilation.pattern(), currentReplaceText);
    }

    private void onSessionFinished() {
        PatternReplaceSession done = session;
        session = null;
        staleMessage = null;

        if (!done.errors().isEmpty()) {
            lastError = done.errors().size() + " method(s) failed \u2013 see log";
            done.errors().forEach(Logging::warn);
        }

        int replaced = done.totalReplacements();
        int methods = done.methodsModified();
        NotificationType type = replaced > 0 ? NotificationType.SUCCESS : NotificationType.INFO;
        String msg = replaced > 0
                ? "Replaced " + replaced + " match(es) across " + methods + " method(s)."
                : "No matches found for the given pattern.";

        Main.getDisplayManager().addNotification(new Notification(
                type,
                new SimpleCaption("Search & Replace"),
                ColoredStringBuilder.create().fmt(msg).get()
        ));
    }

    private void cancelSession() {
        session = null;
        staleMessage = "Replace cancelled";
    }

    private void invalidateSession(String reason) {
        session = null;
        staleMessage = reason + " \u2013 run Replace All again";
        lastError = null;
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        invalidateSession("Project changed");
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        invalidateSession("Project changed");
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        invalidateSession("Project changed");
    }
}
