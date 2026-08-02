package me.f1nal.trinity.execution.xref;

import com.google.common.collect.ListMultimap;
import me.f1nal.trinity.execution.*;
import me.f1nal.trinity.execution.hierarchy.MemberResolver;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.execution.xref.where.XrefWhereClass;
import me.f1nal.trinity.execution.xref.where.XrefWhereField;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethod;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethodInsn;
import com.google.common.collect.Multimaps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The {@link XrefMap} class manages a mapping of references between class members and code within an execution context.
 * It provides methods for building and querying this reference map.
 */
public final class XrefMap extends ProgressiveLoadTask {
    /**
     * The internal data structure that stores references.
     */
    private final ListMultimap<MemberDetails, AbstractXref> memberReferences = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
    private final Execution execution;

    /**
     * @param execution The execution context to analyze.
     */
    public XrefMap(Execution execution) {
        super("Building Cross-References");
        this.setTrinity(execution.getTrinity());
        this.execution = execution;
    }

    /**
     * Builds the reference map by analyzing the execution context.
     */
    @Override
    public synchronized void runImpl() {
        List<ClassInput> classList = execution.getClassList();
        this.startWork(classList.size());
        clearReferences();
        for (ClassInput classInput : classList) {
            this.buildClassXrefs(classInput);
            this.finishedWork();
        }
    }

    /** Rebuilds target resolution after class or member structure changes. */
    public synchronized void rebuild() {
        clearReferences();
        for (ClassInput classInput : execution.getClassList()) {
            buildClassXrefs(classInput);
        }
    }

    private void clearReferences() {
        memberReferences.clear();
        for (ClassTarget target : new ArrayList<>(execution.getClassTargetMap().values())) {
            target.getReferences().clear();
        }
    }

    private void buildClassXrefs(ClassInput classInput) {
        indexScan(classInput, AsmReferenceScanner.scanClass(classInput.getNode()));
    }

    /** Replaces every reference originating from one edited method. */
    public synchronized void refreshMethod(MethodInput methodInput) {
        memberReferences.entries().removeIf(entry -> entry.getValue().getWhere().getInput() == methodInput);
        for (ClassTarget target : execution.getClassTargetMap().values()) {
            target.getReferences().removeIf(reference -> reference.getWhere().getInput() == methodInput);
        }
        indexScan(methodInput.getOwningClass(),
                AsmReferenceScanner.scanMethod(methodInput.getNode()));
    }

    private void indexScan(ClassInput classInput, AsmReferenceScanner.ScanResult scan) {
        Map<FieldNode, FieldInput> fields = new IdentityHashMap<>();
        classInput.getFieldMap().values().forEach(field -> fields.put(field.getNode(), field));
        Map<MethodNode, MethodInput> methods = new IdentityHashMap<>();
        classInput.getMethodMap().values().forEach(method -> methods.put(method.getNode(), method));

        for (AsmReferenceScanner.ClassReference reference : scan.classReferences()) {
            XrefWhere where = createWhere(classInput, reference.source(), fields, methods);
            putClassReference(reference.owner(), new ClassXref(
                    where, reference.access(), reference.invocation(), reference.kind()));
        }
        for (AsmReferenceScanner.MemberReference reference : scan.memberReferences()) {
            XrefWhere where = createWhere(classInput, reference.source(), fields, methods);
            MethodInput sourceMethod = reference.source().method() == null
                    ? null : methods.get(reference.source().method());
            AbstractXref xref = createMemberXref(reference, where, sourceMethod);
            indexMemberReference(classInput, reference, xref);
        }
    }

    private XrefWhere createWhere(ClassInput classInput, AsmReferenceScanner.Source source,
                                  Map<FieldNode, FieldInput> fields,
                                  Map<MethodNode, MethodInput> methods) {
        if (source.method() != null) {
            MethodInput method = methods.get(source.method());
            if (method != null) {
                return source.instruction() == null
                        ? new XrefWhereMethod(method)
                        : new XrefWhereMethodInsn(method, source.instruction());
            }
        }
        if (source.field() != null) {
            FieldInput field = fields.get(source.field());
            if (field != null) return new XrefWhereField(field);
        }
        return new XrefWhereClass(classInput);
    }

    private AbstractXref createMemberXref(AsmReferenceScanner.MemberReference reference,
                                          XrefWhere where, MethodInput sourceMethod) {
        if (sourceMethod != null && reference.directInstruction()) {
            return new MemberXref(sourceMethod, reference.source().instruction());
        }
        return new SymbolicMemberXref(
                where, reference.kind(), reference.access(), reference.invocation());
    }

    private void indexMemberReference(ClassInput caller,
                                      AsmReferenceScanner.MemberReference reference,
                                      AbstractXref xref) {
        MemberDetails details = reference.details();
        if (isMethodOpcode(reference.opcode())) {
            MethodInsnNode instruction = new MethodInsnNode(reference.opcode(),
                    details.getOwner(), details.getName(), details.getDesc(),
                    reference.interfaceOwner());
            Collection<MethodInput> targets =
                    MemberResolver.resolveInvocationTargets(execution, caller, instruction);
            if (!targets.isEmpty()) {
                targets.forEach(target -> putMemberReference(target.getDetails(), xref));
                return;
            }
        } else if (isFieldOpcode(reference.opcode())) {
            FieldInput target = MemberResolver.resolveField(
                    execution, details.getOwner(), details.getName(), details.getDesc());
            if (target != null) {
                putMemberReference(target.getDetails(), xref);
                return;
            }
        }
        putMemberReference(details, xref);
    }

    private void putMemberReference(MemberDetails details, AbstractXref referencer) {
        this.memberReferences.put(new MemberDetails(clearDescriptorFromOwner(details.getOwner()),
                details.getName(), details.getDesc()), referencer);
    }

    private void putClassReference(String owner, ClassXref ref) {
        final String className = clearDescriptorFromOwner(owner);
        final ClassTarget classTarget = execution.addClassTarget(className);

        classTarget.getReferences().add(ref);
    }

    private static String clearDescriptorFromOwner(String owner) {
        int index = 0;

        while (index != owner.length()) {
            if (owner.charAt(index) == '[') {
                ++index;
                continue;
            }
            if (owner.charAt(index) == 'L' && owner.charAt(owner.length() - 1) == ';') {
                return owner.substring(index + 1, owner.length() - 1);
            }
            break;
        }

        return owner;
    }

    private static boolean isMethodOpcode(int opcode) {
        return opcode == Opcodes.INVOKEVIRTUAL
                || opcode == Opcodes.INVOKESPECIAL
                || opcode == Opcodes.INVOKESTATIC
                || opcode == Opcodes.INVOKEINTERFACE;
    }

    private static boolean isFieldOpcode(int opcode) {
        return opcode == Opcodes.GETFIELD
                || opcode == Opcodes.GETSTATIC
                || opcode == Opcodes.PUTFIELD
                || opcode == Opcodes.PUTSTATIC;
    }

    /**
     * Queries a reference in the map.
     *
     * @param owner The referenced owner class.
     * @param name  The referenced name.
     * @param desc  The referenced descriptor.
     * @return A non-null list of references for this target.
     */
    public Collection<AbstractXref> queryMemberReferences(String owner, String name, String desc) {
        final MemberDetails memberDetails = new MemberDetails(owner, name, desc);
        if (!this.memberReferences.containsKey(memberDetails)) {
            return this.translateMemberReferences(memberDetails);
        }
        return this.memberReferences.get(memberDetails);
    }

    /**
     * Translates this members display name into the old name which can be queried.
     */
    private MemberInput<?> translateMemberDetails(MemberDetails memberDetails) {
        final @Nullable ClassTarget classTarget = execution.getClassTargetByDisplayName(memberDetails.getOwner());

        if (classTarget != null && classTarget.isInputAvailable()) {
            for (MemberInput<?> memberInput : classTarget.getInput().getMemberList()) {
                if (memberInput.getDescriptor().equals(memberDetails.getDesc()) && memberInput.getDisplayName().getName().equals(memberDetails.getName())) {
                    return memberInput;
                }
            }
        }

        return null;
    }

    private Collection<AbstractXref> translateMemberReferences(MemberDetails memberDetails) {
        final @Nullable MemberInput<?> translatedDetails = this.translateMemberDetails(memberDetails);

        if (translatedDetails == null) {
            return Collections.emptyList();
        }

        return memberReferences.get(translatedDetails.getDetails());
    }

    /**
     * Queries a reference to this class name.
     *
     * @param className Referenced class name.
     * @return List of references.
     */
    public Collection<ClassXref> queryClassReferences(String className) {
        final @Nullable ClassTarget classTarget = execution.getClassTarget(className);

        if (classTarget == null) {
            return Collections.emptyList();
        }

        return this.queryClassReferences(classTarget);
    }

    public Collection<ClassXref> queryClassReferences(ClassTarget classTarget) {
        return classTarget == null ? Collections.emptyList() : classTarget.getReferences();
    }

    public Collection<AbstractXref> queryMemberReferences(MemberDetails details) {
        return queryMemberReferences(details.getOwner(), details.getName(), details.getDesc());
    }

    public List<AbstractXref> getMemberReferencesByPattern(Pattern pattern) {
        List<AbstractXref> list = new ArrayList<>();

        this.memberReferences.asMap().forEach((memberDetails, xrefs) -> {
            if (pattern.matcher(this.translateMemberAsDisplayNames(memberDetails).getKey()).matches()) {
                list.addAll(this.memberReferences.get(memberDetails));
            }
        });

        return list;
    }

    private MemberDetails translateMemberAsDisplayNames(MemberDetails memberDetails) {
        // Could this be done better perhaps?
        final @Nullable ClassTarget classTarget = execution.getClassTargetByDisplayName(memberDetails.getOwner());

        if (classTarget != null && classTarget.isInputAvailable()) {
            final MemberInput<?> member = classTarget.getInput().getMember(memberDetails);

            if (member != null) {
                return new MemberDetails(member.getOwningClass().getDisplayName().getName(), member.getDisplayName().getName(), member.getDescriptor());
            }
        }

        return memberDetails;
    }
}
// com/paterva/maltego/licensing/serialize/I.<init>.(Ljava/lang/String;Ljava/lang/String;Z)V
