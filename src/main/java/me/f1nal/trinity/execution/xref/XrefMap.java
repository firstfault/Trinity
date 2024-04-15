package me.f1nal.trinity.execution.xref;

import com.google.common.collect.ListMultimap;
import me.f1nal.trinity.execution.*;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.execution.xref.where.XrefWhereClass;
import me.f1nal.trinity.execution.xref.where.XrefWhereField;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethod;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.NameUtil;
import com.google.common.collect.Multimaps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
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
    private final ListMultimap<MemberDetails, MemberXref> memberReferences = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
    private final Execution execution;

    /**
     * @param execution The execution context to analyze.
     */
    public XrefMap(Execution execution) {
        super("Building Cross-References");
        this.execution = execution;
    }

    /**
     * Builds the reference map by analyzing the execution context.
     */
    @Override
    public void runImpl() {
        List<ClassInput> classList = execution.getClassList();
        this.startWork(classList.size());
        for (ClassInput classInput : classList) {
            this.buildClassXrefs(classInput);
            this.finishedWork();
        }
    }

    private void buildClassXrefs(ClassInput classInput) {
        ClassNode node = classInput.getNode();
        XrefWhereClass whereClass = new XrefWhereClass(classInput);

        this.processClassSupers(whereClass, classInput);

        this.processAnnotations(whereClass, node.visibleAnnotations);
        this.processAnnotations(whereClass, node.invisibleAnnotations);

        for (MethodInput methodInput : classInput.getMethodMap().values()) {
            XrefWhereMethod whereMethod = new XrefWhereMethod(methodInput);

            this.processMethodInstructions(whereMethod, methodInput);

            try {
                this.processArgumentXrefs(methodInput, methodInput.getDescriptor());
            } catch (Throwable throwable) {
                Logging.warn("Failed to process argument references: {} in {}", throwable, methodInput);
            }

            this.processAnnotations(whereMethod, methodInput.getNode().visibleAnnotations);
            this.processAnnotations(whereMethod, methodInput.getNode().invisibleAnnotations);

            this.processThrows(whereMethod, methodInput.getNode().exceptions);
            this.processTryCatchHandlers(whereMethod, methodInput.getNode().tryCatchBlocks);
        }

        for (FieldInput fieldInput : classInput.getFieldMap().values()) {
            XrefWhereField whereField = new XrefWhereField(fieldInput);

            this.processAnnotations(whereField, fieldInput.getNode().visibleAnnotations);
            this.processAnnotations(whereField, fieldInput.getNode().invisibleAnnotations);
        }
    }

    private void processMethodInstructions(XrefWhereMethod whereMethod, MethodInput methodInput) {
        for (AbstractInsnNode instruction : methodInput.getInstructions()) {
            if (instruction.getOpcode() <= Opcodes.NOP) {
                continue;
            }

            if (instruction instanceof MethodInsnNode min) {
                this.addReference(min.owner, min.name, min.desc, new MemberXref(methodInput, instruction));
            } else if (instruction instanceof FieldInsnNode fin) {
                this.addReference(fin.owner, fin.name, fin.desc, new MemberXref(methodInput, instruction));
            } else if (instruction instanceof TypeInsnNode) {
                this.processTypeInstruction(whereMethod, (TypeInsnNode) instruction);
            } else if (instruction instanceof LdcInsnNode) {
                Object cst = ((LdcInsnNode) instruction).cst;

                if (cst instanceof Type) {
                    this.processClassLiteralLdc(whereMethod, (Type)cst);
                }
            }
        }
    }


    private void processClassLiteralLdc(XrefWhere where, Type elementType) {
        if (elementType.getSort() != Type.OBJECT) {
            if (elementType.getSort() == Type.ARRAY) this.processClassLiteralLdc(where, elementType.getElementType());
            return;
        }
        putClassReference(NameUtil.internalToNormal(elementType.getClassName()), ClassXref.classLiteral(where));
    }

    private void processTypeInstruction(XrefWhereMethod where, TypeInsnNode instruction) {
        putClassReference(NameUtil.internalToNormal(instruction.desc), ClassXref.typeInstruction(where, instruction.getOpcode()));
    }

    private void processTryCatchHandlers(XrefWhereMethod where, List<TryCatchBlockNode> tryCatchBlocks) {
        if (tryCatchBlocks != null) for (TryCatchBlockNode block : tryCatchBlocks) {
            if (block.type == null) continue;
            this.putClassReference(NameUtil.internalToNormal(block.type), new ClassXref(where, XrefAccessType.READ, "Catch", XrefKind.EXCEPTION));
        }
    }

    private void processThrows(XrefWhereMethod whereMethod, List<String> exceptions) {
        if (exceptions != null) for (String exception : exceptions) {
            putClassReference(exception, new ClassXref(whereMethod, XrefAccessType.READ, "Throws", XrefKind.EXCEPTION));
        }
    }

    private void processClassSupers(XrefWhereClass where, ClassInput classInput) {
        if (classInput.getSuperName() != null) putClassReference(classInput.getSuperName(), ClassXref.extendsClass(where, false));
        List<String> interfaces = classInput.getNode().interfaces;
        if (interfaces != null) for (String itf : interfaces) {
            if (itf != null) putClassReference(itf, ClassXref.extendsClass(where, true));
        }
    }

    private void processAnnotations(XrefWhere where, List<AnnotationNode> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (AnnotationNode node : list) {
            Type type;
            try {
                type = Type.getType(node.desc);
            } catch (Throwable throwable) {
                Logging.warn("Failed to process annotation descriptor: {} inside {}", node.desc, where);
                continue;
            }
            putClassReference(NameUtil.internalToNormal(type.getClassName()), new ClassXref(where, XrefAccessType.READ, "@" + where.getName(), XrefKind.ANNOTATION));
            this.processAnnotationArgs(where, node.values);
        }
    }

    private void processAnnotationArgs(XrefWhere where, List<Object> values) {
        if (values != null) for (Object value : values) {
            if (value instanceof Type) processClassLiteralLdc(where, (Type)value);
            if (value instanceof AnnotationNode) this.processAnnotations(where, List.of((AnnotationNode) value));
            if (value instanceof List<?>) this.processAnnotationArgs(where, (List<Object>) value);
        }
    }

    private void processArgumentXrefs(MethodInput methodInput, String descriptor) {
        Type returnType = Type.getReturnType(descriptor);
        if (returnType.getSort() == Type.OBJECT) {
            putClassReference(NameUtil.internalToNormal(returnType.getClassName()), ClassXref.returnsClass(new XrefWhereMethod(methodInput)));
        }
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        int index = 0;
        for (Type arg : argumentTypes) {
            if (arg.getSort() == Type.OBJECT) {
                this.addMethodArgumentXref(methodInput, index, arg);
            } else if (arg.getSort() == Type.ARRAY) {
                Type elementType = arg.getElementType();
                if (elementType.getSort() == Type.OBJECT) {
                    this.addMethodArgumentXref(methodInput, index, elementType);
                }
            }

            index += arg.getSize();
        }
    }

    private void addMethodArgumentXref(MethodInput methodInput, int index, Type elementType) {
        putClassReference(NameUtil.internalToNormal(elementType.getClassName()), ClassXref.parameter(new XrefWhereMethod(methodInput)));
    }

    /**
     * Adds a reference to the map.
     *
     * @param owner      The referenced owner.
     * @param name       The referenced target name.
     * @param desc       The referenced target descriptor.
     * @param referencer The method responsible for the reference.
     */
    private void addReference(String owner, String name, String desc, MemberXref referencer) {
        owner = clearDescriptorFromOwner(owner);

        this.memberReferences.put(new MemberDetails(owner, name, desc), referencer);
        putClassReference(owner, new ClassXref(referencer.getWhere(), referencer.getAccess(), referencer.getInvocation(), referencer.getKind()));
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

    /**
     * Queries a reference in the map.
     *
     * @param owner The referenced owner class.
     * @param name  The referenced name.
     * @param desc  The referenced descriptor.
     * @return A non-null list of references for this target.
     */
    public Collection<MemberXref> queryMemberReferences(String owner, String name, String desc) {
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

    private Collection<MemberXref> translateMemberReferences(MemberDetails memberDetails) {
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

    public Collection<MemberXref> queryMemberReferences(MemberDetails details) {
        return queryMemberReferences(details.getOwner(), details.getName(), details.getDesc());
    }

    public List<MemberXref> getMemberReferencesByPattern(Pattern pattern) {
        List<MemberXref> list = new ArrayList<>();

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