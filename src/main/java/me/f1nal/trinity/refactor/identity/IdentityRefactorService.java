package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.AbstractDatabaseObject;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventIdentityRefactored;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.RenameType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Analyzes and commits atomic, project-wide JVM identity refactors. */
public final class IdentityRefactorService {
    private static final ExecutorService ANALYZER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Trinity identity refactor analyzer");
        thread.setDaemon(true);
        return thread;
    });

    private final Trinity trinity;

    public IdentityRefactorService(Trinity trinity) {
        this.trinity = Objects.requireNonNull(trinity, "trinity");
    }

    /** Captures a render-thread snapshot immediately and performs the expensive analysis off-thread. */
    public CompletableFuture<IdentityRefactorPlan> analyze(IdentityRefactorRequest request) {
        Main.assertRenderThread();
        IdentityRefactorSnapshot snapshot = IdentityRefactorSnapshot.capture(trinity);
        return CompletableFuture.supplyAsync(
                () -> new IdentityRefactorAnalyzer().analyze(snapshot, request), ANALYZER);
    }

    /** Synchronous entry point used by tests and headless callers that already own project access. */
    public IdentityRefactorPlan analyzeNow(IdentityRefactorRequest request) {
        return new IdentityRefactorAnalyzer().analyze(
                IdentityRefactorSnapshot.capture(trinity), request);
    }

    /** Applies a previously analyzed plan as one render-thread transaction. */
    public IdentityRefactorResult apply(IdentityRefactorPlan plan) {
        Main.assertRenderThread();
        Objects.requireNonNull(plan, "plan");
        if (!plan.belongsTo(trinity)) {
            throw new IllegalStateException("The refactor belongs to a different project");
        }
        if (plan.hasConflicts()) {
            throw new IllegalStateException("A refactor with conflicts cannot be applied");
        }
        if (!plan.isProjectStateCurrent()) {
            throw new IllegalStateException("The project changed while the refactor was being analyzed");
        }

        IdentityRefactorSnapshot liveSnapshot = IdentityRefactorSnapshot.capture(trinity);
        IdentityClassUniverse universe = new IdentityClassUniverse(
                liveSnapshot.projectClasses(), liveSnapshot.dependencyClasses());
        IdentityAsmRemapper remapper = new IdentityAsmRemapper(plan.mapping(), universe);
        Map<IdentityMemberKey, MemberInput<?>> liveMembers = liveMembers();
        Set<IDatabaseSavable<?>> persisted = persistedIdentityState(plan, liveMembers);
        Map<DisplayName, DisplayNameState> displayNames = captureDisplayNames(plan, liveMembers);

        Map<MemberDetails, MemberDetails> memberMappings = new LinkedHashMap<>();
        boolean transformed = false;
        try {
            persisted.forEach(trinity.getDatabase()::remove);
            // Set this before the loop: even a transformer failure on the first class can
            // leave that class partially changed and therefore requires inverse remapping.
            transformed = true;
            for (ClassInput input : trinity.getExecution().getClassList()) {
                new IdentityClassTransformer(remapper, null).transform(input.getNode());
            }
            synchronizeIdentityIndexes(memberMappings);
            normalizeDisplayNames(plan, liveMembers);

            for (ClassInput affected : plan.getAffectedClasses()) {
                affected.markRebuildRequired();
                trinity.getDecompiler().invalidateCache(affected);
            }
            trinity.getExecution().refreshStructuralIndexes();
            saveMigratedState(persisted);
            saveWindowAndNavigationState(plan.getAffectedClasses());

            trinity.getEventManager().postEvent(new EventIdentityRefactored(
                    plan.getRequest(), plan.mapping().classes(), memberMappings,
                    plan.getAffectedClasses()));
            trinity.getEventManager().postEvent(new EventClassesLoaded());
            for (ClassInput affected : plan.getAffectedClasses()) {
                trinity.getEventManager().postEvent(new EventClassModified(affected));
            }
            return new IdentityRefactorResult(
                    plan.getAffectedClasses().size(), plan.getChanges().size());
        } catch (Throwable failure) {
            Throwable recoveryFailure = null;
            if (transformed) {
                try {
                    rollback(plan);
                } catch (Throwable rollbackFailure) {
                    recoveryFailure = rollbackFailure;
                }
            }
            restoreDisplayNames(displayNames);
            restoreDisplayPackages(displayNames);
            for (IDatabaseSavable<?> savable : persisted) {
                try {
                    trinity.getDatabase().save(savable);
                } catch (Throwable saveFailure) {
                    if (recoveryFailure == null) recoveryFailure = saveFailure;
                    else recoveryFailure.addSuppressed(saveFailure);
                }
            }
            if (recoveryFailure != null) failure.addSuppressed(recoveryFailure);
            if (failure instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Unable to apply identity refactor", failure);
        }
    }

    private void rollback(IdentityRefactorPlan plan) {
        IdentityMapping inverse = plan.mapping().inverse(plan.getRequest().name());
        IdentityRefactorSnapshot current = IdentityRefactorSnapshot.capture(trinity);
        IdentityClassUniverse universe = new IdentityClassUniverse(
                current.projectClasses(), current.dependencyClasses());
        IdentityAsmRemapper remapper = new IdentityAsmRemapper(inverse, universe);
        for (ClassInput input : trinity.getExecution().getClassList()) {
            new IdentityClassTransformer(remapper, null).transform(input.getNode());
        }
        synchronizeIdentityIndexes(new LinkedHashMap<>());
        trinity.getExecution().refreshStructuralIndexes();
    }

    /** Aligns stable wrappers and lookup maps to their already-remapped ASM declarations. */
    private void synchronizeIdentityIndexes(Map<MemberDetails, MemberDetails> memberMappings) {
        for (ClassInput input : List.copyOf(trinity.getExecution().getClassList())) {
            String nodeName = input.getNode().name;
            if (requiresClassReindex(input)) {
                List<MemberDetails> previous = input.getMemberList().stream()
                        .map(MemberInput::getDetails).toList();
                trinity.getExecution().reindexClass(input, nodeName);
                for (MemberDetails before : previous) {
                    MemberDetails after = new MemberDetails(
                            nodeName, before.getName(), before.getDesc());
                    memberMappings.put(before, after);
                }
            }
        }

        for (ClassInput input : trinity.getExecution().getClassList()) {
            for (MemberInput<?> member : new ArrayList<>(input.getMemberList())) {
                String nodeName = member instanceof MethodInput method
                        ? method.getNode().name : ((FieldInput) member).getNode().name;
                String nodeDescriptor = member instanceof MethodInput method
                        ? method.getNode().desc : ((FieldInput) member).getNode().desc;
                MemberDetails before = member.getDetails();
                if (before.getOwner().equals(input.getRealName())
                        && before.getName().equals(nodeName)
                        && before.getDesc().equals(nodeDescriptor)) continue;
                input.rebindMemberIdentity(member);
                memberMappings.put(before, member.getDetails());
            }
        }
    }

    static boolean requiresClassReindex(ClassInput input) {
        // ClassInput#getRealName() reads ClassNode.name, which has already been changed by
        // the transformer. The ClassTarget retains the key currently used by Execution's
        // lookup map until reindexClass() commits the new identity.
        return !input.getClassTarget().getRealName().equals(input.getNode().name);
    }

    private Map<IdentityMemberKey, MemberInput<?>> liveMembers() {
        Map<IdentityMemberKey, MemberInput<?>> members = new LinkedHashMap<>();
        for (ClassInput owner : trinity.getExecution().getClassList()) {
            owner.getMemberList().forEach(member ->
                    members.put(IdentityMemberKey.of(member.getDetails()), member));
        }
        return members;
    }

    private Set<IDatabaseSavable<?>> persistedIdentityState(
            IdentityRefactorPlan plan,
            Map<IdentityMemberKey, MemberInput<?>> liveMembers) {
        Set<IDatabaseSavable<?>> candidates = java.util.Collections.newSetFromMap(
                new IdentityHashMap<>());
        if (plan.getRequest().kind() == IdentityRefactorKind.CLASS) {
            ClassInput input = trinity.getExecution().getClassInput(plan.getRequest().owner());
            if (input != null) {
                candidates.add(input.getClassTarget());
                for (MemberInput<?> member : input.getMemberList()) addMemberState(candidates, member);
            }
        }
        plan.mapping().methods().forEach(key -> addMemberState(candidates, liveMembers.get(key)));
        plan.mapping().fields().forEach(key -> addMemberState(candidates, liveMembers.get(key)));
        if (Main.getDisplayManager() != null && Main.getTrinity() == trinity) {
            Main.getWindowManager().getWindowsOfType(
                            me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow.class)
                    .stream().filter(window -> plan.getAffectedClasses()
                            .contains(window.getSelectedClass()))
                    .forEach(candidates::add);
            candidates.add(Main.getDisplayManager().getNavigationHistory());
        }

        Set<IDatabaseSavable<?>> persisted = java.util.Collections.newSetFromMap(
                new IdentityHashMap<>());
        for (IDatabaseSavable<?> candidate : candidates) {
            AbstractDatabaseObject object = candidate.createDatabaseObject();
            if (object != null && trinity.getDatabase().getObjects().contains(object)) {
                persisted.add(candidate);
            }
        }
        return persisted;
    }

    private static void addMemberState(Set<IDatabaseSavable<?>> candidates,
                                       MemberInput<?> member) {
        if (member == null) return;
        if (member instanceof IDatabaseSavable<?> savable) candidates.add(savable);
        if (member instanceof MethodInput method) {
            for (Variable variable : method.getVariableTable().getVariableMap()) {
                if (variable.isEditable()) candidates.add(variable);
            }
        }
    }

    private void normalizeDisplayNames(
            IdentityRefactorPlan plan,
            Map<IdentityMemberKey, MemberInput<?>> liveMembers) {
        if (plan.getRequest().kind() == IdentityRefactorKind.CLASS) {
            ClassInput input = trinity.getExecution().getClassInput(plan.getRequest().newName());
            if (input != null && input.getDisplayName().getName().equals(plan.getRequest().newName())) {
                input.getDisplayName().setName(plan.getRequest().newName(), RenameType.NONE);
            }
            return;
        }
        Set<IdentityMemberKey> keys = new java.util.LinkedHashSet<>();
        keys.addAll(plan.mapping().methods());
        keys.addAll(plan.mapping().fields());
        for (IdentityMemberKey key : keys) {
            MemberInput<?> input = liveMembers.get(key);
            if (input != null && input.getDisplayName().getName().equals(plan.getRequest().newName())) {
                input.getDisplayName().setName(plan.getRequest().newName(), RenameType.NONE);
            }
        }
    }

    private Map<DisplayName, DisplayNameState> captureDisplayNames(
            IdentityRefactorPlan plan,
            Map<IdentityMemberKey, MemberInput<?>> liveMembers) {
        Map<DisplayName, DisplayNameState> states = new IdentityHashMap<>();
        if (plan.getRequest().kind() == IdentityRefactorKind.CLASS) {
            ClassInput input = trinity.getExecution().getClassInput(plan.getRequest().owner());
            if (input != null) captureDisplayName(states, input.getDisplayName());
        }
        plan.mapping().methods().forEach(key -> {
            MemberInput<?> input = liveMembers.get(key);
            if (input != null) captureDisplayName(states, input.getDisplayName());
        });
        plan.mapping().fields().forEach(key -> {
            MemberInput<?> input = liveMembers.get(key);
            if (input != null) captureDisplayName(states, input.getDisplayName());
        });
        return states;
    }

    private static void captureDisplayName(Map<DisplayName, DisplayNameState> states,
                                           DisplayName displayName) {
        states.put(displayName, new DisplayNameState(displayName.getOriginalName(),
                displayName.getName(), displayName.getType()));
    }

    private static void restoreDisplayNames(Map<DisplayName, DisplayNameState> states) {
        states.forEach((displayName, state) -> {
            displayName.replaceOriginalName(state.originalName());
            displayName.setName(state.name(), state.type());
        });
    }

    private void restoreDisplayPackages(Map<DisplayName, DisplayNameState> states) {
        for (ClassInput input : trinity.getExecution().getClassList()) {
            if (!states.containsKey(input.getDisplayName())) continue;
            ClassTarget target = input.getClassTarget();
            me.f1nal.trinity.execution.packages.ProjectContainer container = target.getContainer();
            target.setPackage(container == null
                    ? trinity.getExecution().getRootPackage() : container.getRootPackage());
        }
    }

    private void saveMigratedState(Collection<IDatabaseSavable<?>> persisted) {
        for (IDatabaseSavable<?> savable : persisted) {
            if (savable instanceof ClassTarget target
                    && target.getDisplayName().getType() == RenameType.NONE) continue;
            if (savable instanceof MemberInput<?> member
                    && member.getDisplayName().getType() == RenameType.NONE) continue;
            trinity.getDatabase().save(savable);
        }
    }

    private void saveWindowAndNavigationState(Set<ClassInput> affectedClasses) {
        if (Main.getDisplayManager() == null || Main.getTrinity() != trinity) return;
        Main.getWindowManager().getWindowsOfType(
                        me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow.class)
                .stream().filter(window -> affectedClasses.contains(window.getSelectedClass()))
                .forEach(window -> trinity.getDatabase().save(window));
        trinity.getDatabase().save(Main.getDisplayManager().getNavigationHistory());
    }

    private record DisplayNameState(String originalName, String name, RenameType type) {
    }
}
