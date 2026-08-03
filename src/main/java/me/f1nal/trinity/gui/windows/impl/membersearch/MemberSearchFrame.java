package me.f1nal.trinity.gui.windows.impl.membersearch;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventDependenciesChanged;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.membersearch.MemberSearchQuery;
import me.f1nal.trinity.execution.membersearch.MemberSearchSession;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.INameable;
import org.objectweb.asm.Opcodes;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Target-aware member query editor and time-sliced search controller. */
public final class MemberSearchFrame extends StaticWindow implements IEventListener {
    private static final long FRAME_BUDGET_NANOS = 4_000_000L;
    private static final float LABEL_WIDTH = 145.F;
    private static final MemorableCheckboxComponent CLOSE_AFTER_SEARCH =
            new MemorableCheckboxComponent("closeFrameAfterMemberSearch", "Close After Search", false);

    private final Map<MemberSearchQuery.Target, FormState> states =
            new EnumMap<>(MemberSearchQuery.Target.class);
    private MemberSearchQuery.Target target = MemberSearchQuery.Target.CLASS;
    private MemberSearchSession session;
    private String status;
    private boolean statusError;
    private boolean focusName;

    public MemberSearchFrame(Trinity trinity) {
        super("Member Search", 720.F, 650.F, trinity);
        this.setDialog(true);
        this.windowFlags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize;
        for (MemberSearchQuery.Target value : MemberSearchQuery.Target.values()) {
            states.put(value, new FormState(value));
        }
        trinity.getEventManager().registerListener(this);
    }

    public void applyQuery(MemberSearchQuery query) {
        this.target = query.target();
        this.states.get(target).apply(query);
        this.status = null;
        this.focusName = true;
    }

    public void applyAndRun(MemberSearchQuery query) {
        applyQuery(query);
        startSearch();
    }

    @Override
    protected void onOpen() {
        this.focusName = true;
    }

    @Override
    protected void renderFrame() {
        drawTargetHeader();
        ImGui.separator();

        float footerHeight = session == null
                ? ImGui.getFrameHeightWithSpacing() * 2.15F
                : ImGui.getFrameHeightWithSpacing() * 3.15F;
        if (ImGui.beginChild(getId("MemberSearchCriteria"), 0.F,
                Math.max(1.F, ImGui.getContentRegionAvailY() - footerHeight), false)) {
            FormState state = states.get(target);
            drawScope(state);
            section("Basic Filters");
            drawBasic(state);
            drawFlags(state);
            ImGui.spacing();
            if (ImGui.collapsingHeader("Advanced Filters")) {
                drawAdvanced(state);
            }
        }
        ImGui.endChild();

        ImGui.separator();
        drawFooter();
    }

    private void drawTargetHeader() {
        ImGui.text("Search for");
        ImGui.sameLine();
        ImGui.setNextItemWidth(180.F);
        target = enumCombo("###MemberSearchTarget", target, MemberSearchQuery.Target.values());
        ImGui.sameLine();
        ImGui.textColored(CodeColorScheme.DISABLED, switch (target) {
            case CLASS -> "Find declarations and type relationships";
            case FIELD -> "Find fields by owner, type, and usage";
            case METHOD -> "Find methods by signature and implementation";
        });
    }

    private void drawScope(FormState state) {
        section("Scope");
        label("Search in");
        ImGui.setNextItemWidth(210.F);
        state.scopeKind = enumCombo("###MemberSearchScope", state.scopeKind,
                MemberSearchQuery.ScopeKind.values());

        if (state.scopeKind == MemberSearchQuery.ScopeKind.INPUT) {
            label("Archive");
            state.container = containerCombo("###MemberSearchContainer" + target, state.container);
        } else if (state.scopeKind == MemberSearchQuery.ScopeKind.PACKAGE) {
            label("Package");
            state.pkg = packageCombo("###MemberSearchPackage" + target, state.pkg);
            ImGui.sameLine();
            ImGui.checkbox("Include subpackages###MemberSearchRecursive" + target,
                    state.includeSubpackages);
        }
    }

    private void drawBasic(FormState state) {
        label("Name");
        ImGui.setNextItemWidth(-1.F);
        if (focusName) {
            ImGui.setKeyboardFocusHere();
            focusName = false;
        }
        ImGui.inputTextWithHint("###MemberSearchName" + target,
                target == MemberSearchQuery.Target.CLASS ? "Class name" : "Member name", state.name);

        label("Name match");
        ImGui.setNextItemWidth(160.F);
        state.nameMode = enumCombo("###MemberSearchNameMode" + target, state.nameMode,
                MemberSearchQuery.TextMode.values());
        ImGui.sameLine();
        ImGui.checkbox("Case sensitive###MemberSearchCase" + target, state.caseSensitive);

        label("Visibility");
        ImGui.setNextItemWidth(210.F);
        state.visibility = enumCombo("###MemberSearchVisibility" + target, state.visibility,
                MemberSearchQuery.Visibility.values());

        switch (target) {
            case CLASS -> drawClassBasic(state);
            case FIELD -> drawFieldBasic(state);
            case METHOD -> drawMethodBasic(state);
        }
    }

    private void drawClassBasic(FormState state) {
        label("Class kind");
        ImGui.setNextItemWidth(210.F);
        state.classKind = enumCombo("###MemberSearchClassKind", state.classKind,
                MemberSearchQuery.ClassKind.values());

        label("Relationship");
        ImGui.setNextItemWidth(210.F);
        state.baseMode = hierarchyDirectionCombo("###MemberSearchHierarchyDirection", state.baseMode);
        label("Base type");
        ImGui.setNextItemWidth(-1.F);
        ImGui.inputTextWithHint("###MemberSearchBaseType", "Base class or interface", state.baseType);
        if (!state.baseType.get().isBlank()) {
            label("Hierarchy");
            ImGui.setNextItemWidth(210.F);
            state.hierarchyDepth = enumCombo("###MemberSearchHierarchyDepth", state.hierarchyDepth,
                    MemberSearchQuery.HierarchyDepth.values());
        }
    }

    private void drawFieldBasic(FormState state) {
        typeCriterion("Declared type", "java.lang.String or [I", state.declaredType,
                state.declaredTypeMode, value -> state.declaredTypeMode = value, "FieldType");
        drawOwnerFilters(state);
    }

    private void drawMethodBasic(FormState state) {
        label("Method kind");
        ImGui.setNextItemWidth(210.F);
        state.methodKind = enumCombo("###MemberSearchMethodKind", state.methodKind,
                MemberSearchQuery.MethodKind.values());

        typeCriterion("Return type", "void, int, or class name", state.returnType,
                state.returnTypeMode, value -> state.returnTypeMode = value, "ReturnType");
        typeCriterion("Has parameter", "Parameter type anywhere in signature", state.parameterType,
                state.parameterTypeMode, value -> state.parameterTypeMode = value, "ParameterType");

        label("Exact parameters");
        ImGui.setNextItemWidth(-1.F);
        ImGui.inputTextWithHint("###MemberSearchExactParameters",
                "java.lang.String, int[]   or   (Ljava/lang/String;[I)V", state.exactParameters);
        drawOwnerFilters(state);
    }

    private void drawOwnerFilters(FormState state) {
        label("Declaring class");
        float width = Math.max(180.F, ImGui.getContentRegionAvailX() - 175.F);
        ImGui.setNextItemWidth(width);
        ImGui.inputTextWithHint("###MemberSearchDeclaringClass" + target,
                "Owner class or interface", state.declaringClass);
        ImGui.sameLine();
        ImGui.setNextItemWidth(165.F);
        state.declaringClassMode = enumCombo("###MemberSearchDeclaringClassMode" + target,
                state.declaringClassMode, MemberSearchQuery.TypeMode.values());

        label("Owner kind");
        ImGui.setNextItemWidth(210.F);
        state.ownerKind = enumCombo("###MemberSearchOwnerKind" + target, state.ownerKind,
                MemberSearchQuery.ClassKind.values());
    }

    private void drawFlags(FormState state) {
        label("Access flags");
        List<FlagOption> options = flagOptions(target);
        for (int index = 0; index < options.size(); index++) {
            if (index > 0 && index % 4 == 0) ImGui.setCursorPosX(LABEL_WIDTH);
            FlagOption option = options.get(index);
            MemberSearchQuery.FlagMode mode = state.flags.getOrDefault(
                    option.mask(), MemberSearchQuery.FlagMode.IGNORE);
            pushFlagColor(mode);
            if (ImGui.smallButton(option.label() + "###MemberSearchFlag" + target + option.mask())) {
                state.flags.put(option.mask(), mode.next());
            }
            ImGui.popStyleColor(3);
            if (ImGui.isItemHovered()) {
                GuiUtil.tooltip(option.label() + ": " + mode.getName()
                        + "\nClick to cycle Ignore, Required, and Excluded");
            }
            if (index % 4 != 3 && index + 1 < options.size()) ImGui.sameLine();
        }
    }

    private void drawAdvanced(FormState state) {
        ImGui.spacing();
        if (target != MemberSearchQuery.Target.CLASS) {
            label("Descriptor");
            float width = Math.max(180.F, ImGui.getContentRegionAvailX() - 135.F);
            ImGui.setNextItemWidth(width);
            ImGui.inputTextWithHint("###MemberSearchDescriptor" + target,
                    target == MemberSearchQuery.Target.FIELD ? "Ljava/lang/String;" : "(I)Ljava/lang/String;",
                    state.descriptor);
            ImGui.sameLine();
            ImGui.setNextItemWidth(125.F);
            state.descriptorMode = enumCombo("###MemberSearchDescriptorMode" + target,
                    state.descriptorMode, MemberSearchQuery.DescriptorMode.values());
        }

        label("Generic signature");
        ImGui.setNextItemWidth(-1.F);
        ImGui.inputTextWithHint("###MemberSearchGeneric" + target,
                "Contains class or interface type", state.genericType);

        label("Annotation");
        float annotationWidth = target == MemberSearchQuery.Target.METHOD
                ? Math.max(180.F, ImGui.getContentRegionAvailX() - 225.F) : -1.F;
        ImGui.setNextItemWidth(annotationWidth);
        ImGui.inputTextWithHint("###MemberSearchAnnotation" + target,
                "Annotation class", state.annotationType);
        if (target == MemberSearchQuery.Target.METHOD) {
            ImGui.sameLine();
            ImGui.setNextItemWidth(215.F);
            state.annotationLocation = enumCombo("###MemberSearchAnnotationLocation",
                    state.annotationLocation, MemberSearchQuery.AnnotationLocation.values());
        }

        label("Name state");
        ImGui.setNextItemWidth(210.F);
        state.renameState = enumCombo("###MemberSearchRenameState" + target, state.renameState,
                MemberSearchQuery.RenameState.values());

        label("References");
        ImGui.setNextItemWidth(210.F);
        state.referenceState = enumCombo("###MemberSearchReferenceState" + target,
                state.referenceState, MemberSearchQuery.ReferenceState.values());
        drawRange("Reference count", state.referenceMinimum, state.referenceMaximum,
                "MemberSearchReferences" + target);
        ImGui.textColored(CodeColorScheme.DISABLED,
                "Reference counts omit metadata and stack-frame-only xrefs.");

        if (target == MemberSearchQuery.Target.METHOD) {
            ImGui.spacing();
            label("Implementation");
            ImGui.setNextItemWidth(210.F);
            state.bodyState = enumCombo("###MemberSearchBodyState", state.bodyState,
                    MemberSearchQuery.BodyState.values());
            drawRange("Parameter count", state.parameterMinimum, state.parameterMaximum,
                    "MemberSearchParameters");
            drawRange("Instruction count", state.instructionMinimum, state.instructionMaximum,
                    "MemberSearchInstructions");
        }
    }

    private void drawFooter() {
        if (session == null) {
            boolean loading = !trinity.getExecution().getAsynchronousLoad().isFinished();
            if (loading) ImGui.beginDisabled();
            if (ImGui.button("Search")) startSearch();
            if (loading) ImGui.endDisabled();
        } else {
            if (ImGui.button("Cancel")) cancelSearch(null);
        }
        ImGui.sameLine();
        CLOSE_AFTER_SEARCH.draw();
        ImGui.sameLine();
        if (status != null) {
            ImGui.textColored(statusError ? CodeColorScheme.NOTIFY_ERROR : CodeColorScheme.NOTIFY_WARN, status);
        } else {
            ImGui.textColored(CodeColorScheme.DISABLED,
                    !trinity.getExecution().getAsynchronousLoad().isFinished()
                            ? "Project is still loading."
                            : "Dependencies are used for type relationships, not returned as results.");
        }

        if (session == null) return;
        session.advance(FRAME_BUDGET_NANOS);
        ImGui.progressBar(session.progress(), -1.F, 0.F,
                session.searchedCount() + " / " + session.candidateCount() + " candidates");
        if (!session.isFinished() || session.isCancelled()) return;

        MemberSearchSession completed = session;
        session = null;
        Main.getWindowManager().addClosableWindow(new MemberSearchResultFrame(
                trinity, completed.query(), completed.results(),
                completed.unresolvedHierarchyComparisons()));
        if (CLOSE_AFTER_SEARCH.isChecked()) close();
    }

    public void startSearch() {
        if (!trinity.getExecution().getAsynchronousLoad().isFinished()) {
            this.status = "Project is still loading";
            this.statusError = false;
            return;
        }
        MemberSearchQuery query = states.get(target).toQuery();
        List<String> errors = new me.f1nal.trinity.execution.membersearch.MemberSearchEngine(trinity)
                .validate(query);
        if (!errors.isEmpty()) {
            this.status = errors.get(0);
            this.statusError = true;
            return;
        }
        this.status = null;
        this.statusError = false;
        this.session = new MemberSearchSession(trinity, query);
    }

    private void cancelSearch(String message) {
        if (session != null) session.cancel();
        session = null;
        status = message;
        statusError = false;
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

    @Subscribe
    public void onDependenciesChanged(EventDependenciesChanged event) {
        cancelSearch("Dependencies changed; run the search again");
    }

    @Subscribe
    public void onPackageStructureChanged(EventPackageStructureReload event) {
        cancelSearch("Project structure changed; run the search again");
    }

    @Override
    protected void onDispose() {
        trinity.getEventManager().unregisterListener(this);
    }

    private void typeCriterion(String label, String hint, ImString text,
                               MemberSearchQuery.TypeMode mode,
                               Consumer<MemberSearchQuery.TypeMode> update, String id) {
        label(label);
        float width = Math.max(180.F, ImGui.getContentRegionAvailX() - 175.F);
        ImGui.setNextItemWidth(width);
        ImGui.inputTextWithHint("###MemberSearch" + id + target, hint, text);
        ImGui.sameLine();
        ImGui.setNextItemWidth(165.F);
        update.accept(enumCombo("###MemberSearch" + id + "Mode" + target, mode,
                MemberSearchQuery.TypeMode.values()));
    }

    private void drawRange(String label, ImInt minimum, ImInt maximum, String id) {
        label(label);
        float width = Math.max(90.F, (ImGui.getContentRegionAvailX() - 52.F) * 0.5F);
        ImGui.setNextItemWidth(width);
        ImGui.inputInt("###" + id + "Minimum", minimum);
        ImGui.sameLine();
        ImGui.textColored(CodeColorScheme.DISABLED, "to");
        ImGui.sameLine();
        ImGui.setNextItemWidth(width);
        ImGui.inputInt("###" + id + "Maximum", maximum);
        if (ImGui.isItemHovered()) GuiUtil.tooltip("Use -1 for no limit");
    }

    private ProjectContainer containerCombo(String id, ProjectContainer selected) {
        List<ProjectContainer> containers = trinity.getExecution().getContainers().stream()
                .filter(ProjectContainer::isJar).toList();
        if (selected != null && !containers.contains(selected)) selected = null;
        String preview = selected == null ? "Choose archive..." : selected.getName();
        ImGui.setNextItemWidth(-1.F);
        if (ImGui.beginCombo(id, preview)) {
            for (ProjectContainer container : containers) {
                if (ImGui.selectable(container.getName(), container == selected)) selected = container;
            }
            ImGui.endCombo();
        }
        return selected;
    }

    private Package packageCombo(String id, Package selected) {
        List<Package> packages = trinity.getExecution().getAllPackages();
        if (selected != null && !packages.contains(selected)) selected = null;
        String preview = selected == null ? "Choose package..." : packageLabel(selected);
        ImGui.setNextItemWidth(Math.max(220.F, ImGui.getContentRegionAvailX() - 155.F));
        if (ImGui.beginCombo(id, preview)) {
            for (Package pkg : packages) {
                String label = packageLabel(pkg);
                if (ImGui.selectable(label, pkg == selected)) selected = pkg;
            }
            ImGui.endCombo();
        }
        return selected;
    }

    private static String packageLabel(Package pkg) {
        String archive = pkg.getContainer() == null ? "Loose Classes" : pkg.getContainer().getName();
        String path = pkg.getPrettyPath().isEmpty() ? "<root>" : pkg.getPrettyPath();
        return archive + "  /  " + path;
    }

    private static <T extends Enum<T> & INameable> T enumCombo(String id, T selected, T[] values) {
        if (ImGui.beginCombo(id, selected.getName())) {
            for (T value : values) {
                if (ImGui.selectable(value.getName(), value == selected)) selected = value;
            }
            ImGui.endCombo();
        }
        return selected;
    }

    private static MemberSearchQuery.TypeMode hierarchyDirectionCombo(
            String id, MemberSearchQuery.TypeMode selected) {
        if (selected == MemberSearchQuery.TypeMode.EXACT) {
            selected = MemberSearchQuery.TypeMode.ASSIGNABLE_TO;
        }
        String preview = selected == MemberSearchQuery.TypeMode.ASSIGNABLE_FROM
                ? "Supertypes Of" : "Subtypes Of";
        if (ImGui.beginCombo(id, preview)) {
            if (ImGui.selectable("Subtypes Of",
                    selected == MemberSearchQuery.TypeMode.ASSIGNABLE_TO)) {
                selected = MemberSearchQuery.TypeMode.ASSIGNABLE_TO;
            }
            if (ImGui.selectable("Supertypes Of",
                    selected == MemberSearchQuery.TypeMode.ASSIGNABLE_FROM)) {
                selected = MemberSearchQuery.TypeMode.ASSIGNABLE_FROM;
            }
            ImGui.endCombo();
        }
        return selected;
    }

    private static void section(String title) {
        ImGui.textColored(CodeColorScheme.TEXT, title);
        ImGui.separator();
    }

    private static void label(String text) {
        ImGui.alignTextToFramePadding();
        ImGui.textColored(CodeColorScheme.DISABLED, text);
        ImGui.sameLine(LABEL_WIDTH);
    }

    private static void pushFlagColor(MemberSearchQuery.FlagMode mode) {
        int base = switch (mode) {
            case IGNORE -> CodeColorScheme.setAlpha(CodeColorScheme.WIDGET_BACKGROUND, 225);
            case REQUIRE -> CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_SUCCESS, 150);
            case EXCLUDE -> CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_ERROR, 170);
        };
        int hover = switch (mode) {
            case IGNORE -> CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 75);
            case REQUIRE -> CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_SUCCESS, 205);
            case EXCLUDE -> CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_ERROR, 220);
        };
        ImGui.pushStyleColor(ImGuiCol.Button, base);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, hover);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, hover);
    }

    private static List<FlagOption> flagOptions(MemberSearchQuery.Target target) {
        return switch (target) {
            case CLASS -> List.of(
                    new FlagOption("Final", Opcodes.ACC_FINAL),
                    new FlagOption("Abstract", Opcodes.ACC_ABSTRACT),
                    new FlagOption("Synthetic", Opcodes.ACC_SYNTHETIC));
            case FIELD -> List.of(
                    new FlagOption("Static", Opcodes.ACC_STATIC),
                    new FlagOption("Final", Opcodes.ACC_FINAL),
                    new FlagOption("Volatile", Opcodes.ACC_VOLATILE),
                    new FlagOption("Transient", Opcodes.ACC_TRANSIENT),
                    new FlagOption("Synthetic", Opcodes.ACC_SYNTHETIC),
                    new FlagOption("Enum constant", Opcodes.ACC_ENUM));
            case METHOD -> List.of(
                    new FlagOption("Static", Opcodes.ACC_STATIC),
                    new FlagOption("Final", Opcodes.ACC_FINAL),
                    new FlagOption("Synchronized", Opcodes.ACC_SYNCHRONIZED),
                    new FlagOption("Bridge", Opcodes.ACC_BRIDGE),
                    new FlagOption("Varargs", Opcodes.ACC_VARARGS),
                    new FlagOption("Native", Opcodes.ACC_NATIVE),
                    new FlagOption("Abstract", Opcodes.ACC_ABSTRACT),
                    new FlagOption("Strict", Opcodes.ACC_STRICT),
                    new FlagOption("Synthetic", Opcodes.ACC_SYNTHETIC));
        };
    }

    private record FlagOption(String label, int mask) {
    }

    private static final class FormState {
        private final MemberSearchQuery.Target target;
        private MemberSearchQuery.ScopeKind scopeKind = MemberSearchQuery.ScopeKind.PROJECT;
        private ProjectContainer container;
        private Package pkg;
        private final ImBoolean includeSubpackages = new ImBoolean(true);
        private final ImString name = new ImString(256);
        private MemberSearchQuery.TextMode nameMode = MemberSearchQuery.TextMode.CONTAINS;
        private final ImBoolean caseSensitive = new ImBoolean(false);
        private MemberSearchQuery.Visibility visibility = MemberSearchQuery.Visibility.ANY;
        private final Map<Integer, MemberSearchQuery.FlagMode> flags = new LinkedHashMap<>();
        private MemberSearchQuery.ClassKind ownerKind = MemberSearchQuery.ClassKind.ANY;
        private final ImString declaringClass = new ImString(256);
        private MemberSearchQuery.TypeMode declaringClassMode = MemberSearchQuery.TypeMode.EXACT;

        private MemberSearchQuery.ClassKind classKind = MemberSearchQuery.ClassKind.ANY;
        private final ImString baseType = new ImString(256);
        private MemberSearchQuery.TypeMode baseMode = MemberSearchQuery.TypeMode.ASSIGNABLE_TO;
        private MemberSearchQuery.HierarchyDepth hierarchyDepth = MemberSearchQuery.HierarchyDepth.TRANSITIVE;

        private final ImString declaredType = new ImString(256);
        private MemberSearchQuery.TypeMode declaredTypeMode = MemberSearchQuery.TypeMode.EXACT;

        private MemberSearchQuery.MethodKind methodKind = MemberSearchQuery.MethodKind.ANY;
        private final ImString returnType = new ImString(256);
        private MemberSearchQuery.TypeMode returnTypeMode = MemberSearchQuery.TypeMode.EXACT;
        private final ImString parameterType = new ImString(256);
        private MemberSearchQuery.TypeMode parameterTypeMode = MemberSearchQuery.TypeMode.EXACT;
        private final ImString exactParameters = new ImString(512);
        private final ImInt parameterMinimum = new ImInt(-1);
        private final ImInt parameterMaximum = new ImInt(-1);
        private MemberSearchQuery.BodyState bodyState = MemberSearchQuery.BodyState.ANY;
        private final ImInt instructionMinimum = new ImInt(-1);
        private final ImInt instructionMaximum = new ImInt(-1);

        private final ImString descriptor = new ImString(512);
        private MemberSearchQuery.DescriptorMode descriptorMode = MemberSearchQuery.DescriptorMode.EXACT;
        private final ImString genericType = new ImString(256);
        private final ImString annotationType = new ImString(256);
        private MemberSearchQuery.AnnotationLocation annotationLocation =
                MemberSearchQuery.AnnotationLocation.DECLARATION_OR_PARAMETER;
        private MemberSearchQuery.RenameState renameState = MemberSearchQuery.RenameState.ANY;
        private MemberSearchQuery.ReferenceState referenceState = MemberSearchQuery.ReferenceState.ANY;
        private final ImInt referenceMinimum = new ImInt(-1);
        private final ImInt referenceMaximum = new ImInt(-1);

        private FormState(MemberSearchQuery.Target target) {
            this.target = target;
        }

        private MemberSearchQuery toQuery() {
            MemberSearchQuery.Scope scope = new MemberSearchQuery.Scope(
                    scopeKind, container, pkg, includeSubpackages.get());
            MemberSearchQuery.Common common = new MemberSearchQuery.Common(
                    new MemberSearchQuery.TextCriterion(name.get(), nameMode, caseSensitive.get()),
                    visibility, flags, ownerKind,
                    new MemberSearchQuery.TypeCriterion(declaringClass.get(), declaringClassMode),
                    descriptor.get(), descriptorMode, genericType.get(), annotationType.get(),
                    annotationLocation, renameState, referenceState,
                    new MemberSearchQuery.IntRange(referenceMinimum.get(), referenceMaximum.get()));
            MemberSearchQuery.ClassCriteria classes = new MemberSearchQuery.ClassCriteria(
                    classKind, new MemberSearchQuery.TypeCriterion(baseType.get(), baseMode), hierarchyDepth);
            MemberSearchQuery.FieldCriteria fields = new MemberSearchQuery.FieldCriteria(
                    new MemberSearchQuery.TypeCriterion(declaredType.get(), declaredTypeMode));
            MemberSearchQuery.MethodCriteria methods = new MemberSearchQuery.MethodCriteria(
                    methodKind, new MemberSearchQuery.TypeCriterion(returnType.get(), returnTypeMode),
                    new MemberSearchQuery.TypeCriterion(parameterType.get(), parameterTypeMode),
                    exactParameters.get(), new MemberSearchQuery.IntRange(parameterMinimum.get(), parameterMaximum.get()),
                    bodyState, new MemberSearchQuery.IntRange(instructionMinimum.get(), instructionMaximum.get()));
            return new MemberSearchQuery(target, scope, common, classes, fields, methods);
        }

        private void apply(MemberSearchQuery query) {
            MemberSearchQuery.Scope scope = query.scope();
            scopeKind = scope.kind();
            container = scope.container();
            pkg = scope.pkg();
            includeSubpackages.set(scope.includeSubpackages());
            MemberSearchQuery.Common common = query.common();
            name.set(common.name().text());
            nameMode = common.name().mode();
            caseSensitive.set(common.name().caseSensitive());
            visibility = common.visibility();
            flags.clear();
            flags.putAll(common.flags());
            ownerKind = common.ownerKind();
            declaringClass.set(common.declaringClass().text());
            declaringClassMode = common.declaringClass().mode();
            descriptor.set(common.descriptor());
            descriptorMode = common.descriptorMode();
            genericType.set(common.genericType());
            annotationType.set(common.annotationType());
            annotationLocation = common.annotationLocation();
            renameState = common.renameState();
            referenceState = common.referenceState();
            referenceMinimum.set(common.referenceRange().minimum());
            referenceMaximum.set(common.referenceRange().maximum());

            classKind = query.classCriteria().kind();
            baseType.set(query.classCriteria().baseType().text());
            baseMode = query.classCriteria().baseType().mode();
            hierarchyDepth = query.classCriteria().depth();
            declaredType.set(query.fieldCriteria().declaredType().text());
            declaredTypeMode = query.fieldCriteria().declaredType().mode();

            methodKind = query.methodCriteria().kind();
            returnType.set(query.methodCriteria().returnType().text());
            returnTypeMode = query.methodCriteria().returnType().mode();
            parameterType.set(query.methodCriteria().parameterType().text());
            parameterTypeMode = query.methodCriteria().parameterType().mode();
            exactParameters.set(query.methodCriteria().exactParameters());
            parameterMinimum.set(query.methodCriteria().parameterCount().minimum());
            parameterMaximum.set(query.methodCriteria().parameterCount().maximum());
            bodyState = query.methodCriteria().bodyState();
            instructionMinimum.set(query.methodCriteria().instructionCount().minimum());
            instructionMaximum.set(query.methodCriteria().instructionCount().maximum());
        }
    }
}
