package me.f1nal.trinity.gui.windows.impl.methodgraph;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.graph.MethodGraph;
import me.f1nal.trinity.execution.graph.MethodGraph.MethodKey;
import me.f1nal.trinity.execution.graph.MethodGraphAnalyzer;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.SystemUtil;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Interactive call and control-flow graph rooted at one method. */
public final class MethodGraphWindow extends ClosableWindow implements IEventListener {
    private static final ExecutorService GRAPH_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2)),
            new GraphThreadFactory());
    private static final int[] DEPTHS = {1, 2, 3, 4, 5, MethodGraphAnalyzer.INFINITE_DEPTH};

    private volatile MethodInput rootMethod;
    private volatile MethodKey rootKey;
    private int depth = 2;
    private MethodGraph.Direction direction = MethodGraph.Direction.CALLS;
    private final ImBoolean includeExternal = new ImBoolean(true);
    private final ImBoolean collapseMethodContent = new ImBoolean(false);
    private final ImString search = new ImString(192);
    private final MethodGraphCanvas canvas;
    private final AtomicInteger analysisGeneration = new AtomicInteger();
    private CompletableFuture<?> analysisFuture;
    private volatile Completion pendingCompletion;
    private MethodGraph graph;
    private String analysisError;
    private volatile boolean analysisRequested = true;
    private volatile boolean stale;
    private boolean searchFocusRequested;
    private int searchIndex = -1;

    public MethodGraphWindow(Trinity trinity, MethodInput rootMethod) {
        super("Method Graph", 1080.F, 700.F, trinity);
        this.rootMethod = Objects.requireNonNull(rootMethod, "rootMethod");
        this.rootKey = MethodGraphAnalyzer.key(rootMethod);
        this.canvas = new MethodGraphCanvas(new CanvasActions());
        trinity.getEventManager().registerListener(this);
    }

    public static void open(MethodInput method) {
        Main.getWindowManager().addClosableWindow(
                new MethodGraphWindow(Main.getTrinity(), method));
    }

    @Override
    public String getTitle() {
        return "Graph: " + rootKey.displayOwner() + "."
                + rootMethod.getDisplayName().getName();
    }

    @Override
    protected void renderFrame() {
        applyCompletion();
        drawToolbar();
        ImGui.separator();

        if (analysisRequested) {
            analysisRequested = false;
            startAnalysis();
        }

        if (graph == null) {
            drawEmptyState();
            return;
        }
        canvas.setSearch(search.get());
        canvas.draw(Math.max(1.F, ImGui.getContentRegionAvailX()),
                Math.max(1.F, ImGui.getContentRegionAvailY()));
    }

    private void drawToolbar() {
        ImGui.alignTextToFramePadding();
        ImGui.textDisabled("Direction");
        ImGui.sameLine();
        ImGui.setNextItemWidth(92.F);
        if (ImGui.beginCombo("###" + getId("Direction"), direction.getLabel())) {
            for (MethodGraph.Direction value : MethodGraph.Direction.values()) {
                if (ImGui.selectable(value.getLabel(), value == direction)) {
                    direction = value;
                    requestAnalysis();
                }
            }
            ImGui.endCombo();
        }

        ImGui.sameLine(0.F, 12.F);
        ImGui.alignTextToFramePadding();
        ImGui.textDisabled("Depth");
        ImGui.sameLine();
        ImGui.setNextItemWidth(62.F);
        if (ImGui.beginCombo("###" + getId("Depth"), depthLabel(depth))) {
            for (int value : DEPTHS) {
                if (ImGui.selectable(depthLabel(value), value == depth)) {
                    depth = value;
                    requestAnalysis();
                }
            }
            ImGui.endCombo();
        }

        ImGui.sameLine(0.F, 12.F);
        if (GuiUtil.smallCheckbox("External", includeExternal)) requestAnalysis();
        GuiUtil.tooltip("Show unresolved and dependency methods as leaf nodes");

        ImGui.sameLine(0.F, 12.F);
        if (GuiUtil.smallCheckbox("Collapse content", collapseMethodContent)) requestAnalysis();
        GuiUtil.tooltip("Show only method headers and call relationships");

        ImGui.sameLine(0.F, 12.F);
        if (ImGui.button("Fit")) canvas.requestFit();
        GuiUtil.tooltip("Fit the entire graph (F)");
        ImGui.sameLine();
        if (ImGui.button("Root")) canvas.centerRoot();
        GuiUtil.tooltip("Center the root method");
        ImGui.sameLine();
        if (ImGui.button("Reset")) canvas.resetLayout();
        GuiUtil.tooltip("Restore automatic node positions");
        ImGui.sameLine();
        if (ImGui.button("Rebuild")) requestAnalysis();

        float searchWidth = Math.min(230.F, Math.max(120.F, ImGui.getContentRegionAvailX() - 205.F));
        ImGui.sameLine(0.F, 14.F);
        ImGui.setNextItemWidth(searchWidth);
        if (searchFocusRequested) {
            ImGui.setKeyboardFocusHere();
            searchFocusRequested = false;
        }
        boolean findNext = ImGui.inputTextWithHint("###" + getId("Search"), "Find method...", search,
                ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.EscapeClearsAll);
        if (findNext) selectNextSearchResult();

        if (ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl()
                && ImGui.isKeyPressed(ImGuiKey.F, false)) {
            searchFocusRequested = true;
        }
        if (canvas.isHovered() && !ImGui.isAnyItemActive()
                && ImGui.isKeyPressed(ImGuiKey.F, false)) {
            canvas.requestFit();
        }

        if (graph != null) {
            ImGui.sameLine(0.F, 12.F);
            ImGui.alignTextToFramePadding();
            String status;
            if (graph.methodContentCollapsed()) {
                int callCount = graph.calls().stream()
                        .mapToInt(MethodGraph.CallEdge::callSites).sum();
                status = graph.nodes().size() + " methods  " + callCount + " calls";
            } else {
                status = graph.nodes().size() + " methods  "
                        + graph.basicBlockCount() + " blocks  "
                        + graph.flowEdgeCount() + " paths";
            }
            ImGui.textColored(CodeColorScheme.DISABLED, status);
        }
        if (analysisFuture != null && !analysisFuture.isDone()) {
            ImGui.sameLine(0.F, 10.F);
            ImGui.textColored(CodeColorScheme.METHOD_REF, "Analyzing...");
        } else if (stale) {
            ImGui.sameLine(0.F, 10.F);
            ImGui.textColored(CodeColorScheme.NOTIFY_WARN, "Project changed");
        }
    }

    private void drawEmptyState() {
        float available = ImGui.getContentRegionAvailY();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + Math.max(0.F, available * 0.42F));
        if (analysisError != null) {
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR,
                    "Unable to build graph: " + analysisError);
            if (ImGui.button("Retry")) requestAnalysis();
        } else {
            ImGui.textColored(CodeColorScheme.DISABLED, "Building method graph...");
        }
    }

    private void selectNextSearchResult() {
        if (graph == null || search.get().isBlank()) return;
        String query = search.get().toLowerCase();
        List<MethodGraph.MethodNode> matches = graph.nodes().values().stream()
                .filter(node -> node.key().symbol().toLowerCase().contains(query)
                        || node.method() != null && node.method().getDisplayName().getName()
                        .toLowerCase().contains(query))
                .sorted(Comparator.comparing(node -> node.key().symbol()))
                .toList();
        if (matches.isEmpty()) return;
        searchIndex = (searchIndex + 1) % matches.size();
        canvas.selectAndCenter(matches.get(searchIndex).key());
    }

    private void requestAnalysis() {
        this.stale = false;
        this.analysisRequested = true;
    }

    private void startAnalysis() {
        int generation = analysisGeneration.incrementAndGet();
        if (analysisFuture != null) analysisFuture.cancel(true);
        MethodInput current = resolve(rootKey);
        if (current != null) rootMethod = current;
        float lineHeight = Math.max(12.F, ImGui.getTextLineHeight());
        float characterWidth = Math.max(5.F, ImGui.calcTextSize("M").x);
        MethodGraphAnalyzer.Request request = new MethodGraphAnalyzer.Request(
                depth, direction, includeExternal.get(), collapseMethodContent.get(),
                lineHeight, characterWidth);
        MethodInput rootSnapshot = rootMethod;
        analysisError = null;
        analysisFuture = CompletableFuture.supplyAsync(() ->
                        new MethodGraphAnalyzer(trinity.getExecution()).analyze(rootSnapshot, request,
                                () -> generation != analysisGeneration.get()), GRAPH_EXECUTOR)
                .whenComplete((result, throwable) -> {
                    if (generation != analysisGeneration.get()) return;
                    pendingCompletion = new Completion(generation, result, throwable);
                });
    }

    private void applyCompletion() {
        Completion completion = pendingCompletion;
        if (completion == null) return;
        pendingCompletion = null;
        if (completion.generation() != analysisGeneration.get()) return;
        Throwable throwable = unwrap(completion.throwable());
        if (throwable != null) {
            if (!(throwable instanceof java.util.concurrent.CancellationException)) {
                analysisError = throwable.getMessage() == null
                        ? throwable.getClass().getSimpleName() : throwable.getMessage();
            }
            return;
        }
        boolean layoutModeChanged = graph != null && graph.methodContentCollapsed()
                != completion.graph().methodContentCollapsed();
        graph = completion.graph();
        canvas.setGraph(graph);
        if (layoutModeChanged) canvas.requestFit();
        stale = false;
        searchIndex = -1;
    }

    private static Throwable unwrap(Throwable throwable) {
        while (throwable instanceof java.util.concurrent.CompletionException
                || throwable instanceof java.util.concurrent.ExecutionException) {
            if (throwable.getCause() == null) break;
            throwable = throwable.getCause();
        }
        return throwable;
    }

    private MethodInput resolve(MethodKey key) {
        return trinity.getExecution().getMethod(
                new MemberDetails(key.owner(), key.name(), key.descriptor()));
    }

    private void setRoot(MethodInput method) {
        rootMethod = method;
        rootKey = MethodGraphAnalyzer.key(method);
        requestAnalysis();
        canvas.requestFit();
    }

    private void showNodePopup(MethodGraph.MethodNode node) {
        PopupItemBuilder popup = PopupItemBuilder.create();
        if (node.method() != null) {
            popup.menuItem("Set as Graph Root", () -> setRoot(node.method()));
            popup.menuItem("Open in Decompiler", () -> navigate(node.method(), null));
            popup.separator();
            node.method().populatePopup(popup);
        } else {
            ClassInput owner = trinity.getExecution().getClassInput(node.key().owner());
            if (owner != null) {
                popup.menuItem("Open Owner Class", () -> Main.getDisplayManager()
                        .followDecompilerView(owner, NavigationAction.FOLLOW_GRAPH));
            }
            popup.menuItem("Copy Symbol", () -> SystemUtil.copyToClipboard(node.key().symbol()));
        }
        Main.getDisplayManager().getPopupMenu().show(popup);
    }

    private void navigate(MethodInput method, AbstractInsnNode instruction) {
        Main.getDisplayManager().followDecompilerView(
                method, instruction, NavigationAction.FOLLOW_GRAPH);
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        markProjectChanged();
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        markProjectChanged();
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        MemberDetails previous = event.getPreviousDetails();
        if (previous.getOwner().equals(rootKey.owner())
                && previous.getName().equals(rootKey.name())
                && previous.getDesc().equals(rootKey.descriptor())
                && event.getMemberInput() instanceof MethodInput method) {
            rootMethod = method;
            rootKey = MethodGraphAnalyzer.key(method);
        }
        markProjectChanged();
    }

    private void markProjectChanged() {
        stale = true;
        analysisRequested = true;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) Main.getWindowManager().requestFocus(this);
        super.setVisible(visible);
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof MethodGraphWindow other
                && other.rootKey.equals(this.rootKey);
    }

    @Override
    protected void onDispose() {
        analysisGeneration.incrementAndGet();
        if (analysisFuture != null) analysisFuture.cancel(true);
        trinity.getEventManager().unregisterListener(this);
    }

    private static String depthLabel(int depth) {
        return depth == MethodGraphAnalyzer.INFINITE_DEPTH ? "Infinity" : Integer.toString(depth);
    }

    private final class CanvasActions implements MethodGraphCanvas.Actions {
        @Override
        public void navigate(MethodGraph.MethodNode node, AbstractInsnNode instruction) {
            if (node.method() != null) MethodGraphWindow.this.navigate(node.method(), instruction);
        }

        @Override
        public void showContextMenu(MethodGraph.MethodNode node) {
            showNodePopup(node);
        }
    }

    private record Completion(int generation, MethodGraph graph, Throwable throwable) {
    }

    private static final class GraphThreadFactory implements ThreadFactory {
        private final AtomicInteger nextId = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable,
                    "Trinity Method Graph " + nextId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
