package me.f1nal.trinity.gui.windows.impl.pattern;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.pattern.InstructionPatternCompiler;
import me.f1nal.trinity.execution.pattern.PatternDiagnostic;
import me.f1nal.trinity.execution.pattern.PatternSearchSession;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.pattern.PatternReplaceFrame;
import me.f1nal.trinity.theme.CodeColorScheme;

public final class PatternSearchFrame extends StaticWindow implements IEventListener {
    private static final long FRAME_BUDGET_NANOS = 4_000_000L;
    private final AssemblerPatternEditor editor;
    private final MemorableCheckboxComponent includeMetadata = new MemorableCheckboxComponent(
            "patternSearchIncludeMetadata", "Include Metadata", false);
    private InstructionPatternCompiler.Compilation compilation;
    private PatternSearchSession session;
    private boolean resultsOpened;
    private String staleMessage;

    public PatternSearchFrame(Trinity trinity) {
        super("Pattern Search", 680, 390, trinity);
        this.setDialog(true);
        this.windowFlags = ImGuiWindowFlags.NoCollapse;
        this.editor = new AssemblerPatternEditor(trinity);
        this.compilation = InstructionPatternCompiler.compile("", includeMetadata.isChecked());
        trinity.getEventManager().registerListener(this);
    }

    @Override
    protected void onOpen() {
        this.editor.requestFocus();
    }

    @Override
    protected void renderFrame() {
        ImGui.textColored(CodeColorScheme.DISABLED,
                "Assembler instruction pattern  |  * operand/instruction  |  ... instruction gap");
        boolean progressVisible = session != null;
        float reservedHeight = ImGui.getFrameHeightWithSpacing()
                * (progressVisible ? 2.F : 1.F);
        AssemblerPatternEditor.EditorState state = editor.draw(Math.max(125.F,
                ImGui.getContentRegionAvailY() - reservedHeight));

        if (state.changed()) {
            compilation = InstructionPatternCompiler.compile(state.text(), includeMetadata.isChecked());
            cancelSearch(null);
        }

        boolean search = false;
        boolean cancel = false;
        if (session == null) {
            boolean valid = compilation.valid();
            if (!valid) ImGui.beginDisabled();
            search = ImGui.button("Search") || state.searchShortcut();
            if (!valid) ImGui.endDisabled();
        } else {
            cancel = ImGui.button("Cancel");
        }

        ImGui.sameLine();
        if (ImGui.button("Search & Replace")) {
            Main.getWindowManager().addStaticWindow(PatternReplaceFrame.class);
        }
        ImGui.sameLine();
        boolean metadataBefore = includeMetadata.isChecked();
        includeMetadata.draw();
        if (metadataBefore != includeMetadata.isChecked()) {
            compilation = InstructionPatternCompiler.compile(state.text(), includeMetadata.isChecked());
            cancelSearch(null);
        }

        ImGui.sameLine();
        drawDiagnostic();
        if (cancel) cancelSearch(null);
        if (search && compilation.valid()) startSearch();
        if (session != null && progressVisible) drawSearchProgress();
    }

    private void drawDiagnostic() {
        if (staleMessage != null) {
            ImGui.textColored(CodeColorScheme.NOTIFY_WARN, staleMessage);
            return;
        }
        PatternDiagnostic diagnostic = compilation.primaryDiagnostic();
        if (diagnostic == null) {
            ImGui.textColored(CodeColorScheme.NOTIFY_SUCCESS, "Pattern is valid");
            return;
        }
        int color = diagnostic.severity() == PatternDiagnostic.Severity.ERROR
                ? CodeColorScheme.NOTIFY_ERROR : CodeColorScheme.DISABLED;
        ImGui.textColored(color, "Line " + diagnostic.line() + ", column " + diagnostic.column()
                + ": " + diagnostic.message());
    }

    private void startSearch() {
        this.staleMessage = null;
        this.resultsOpened = false;
        this.session = new PatternSearchSession(trinity, compilation.pattern(),
                Main.getPreferences().getSearchMaxDisplay().getMax());
    }

    private void drawSearchProgress() {
        session.advance(FRAME_BUDGET_NANOS);
        ImGui.progressBar(session.progress(), -1.F, 0.F,
                session.methodsSearched() + " / " + session.methodCount() + " methods");
        if (!session.isFinished() || session.isCancelled() || resultsOpened) return;

        resultsOpened = true;
        Main.getWindowManager().addClosableWindow(new PatternSearchResultFrame(
                trinity, session.results(), session.matchCount(), session.pattern().source()));
        session = null;
    }

    private void cancelSearch(String message) {
        if (session != null) session.cancel();
        session = null;
        resultsOpened = false;
        staleMessage = message;
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        cancelSearch("Project changed; run the search again");
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        cancelSearch("Project changed; run the search again");
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        cancelSearch("Project changed; run the search again");
    }
}
