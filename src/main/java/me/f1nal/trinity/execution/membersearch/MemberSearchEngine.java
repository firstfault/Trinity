package me.f1nal.trinity.execution.membersearch;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.remap.RenameType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Evaluates immutable member-search queries against the render-thread project model. */
public final class MemberSearchEngine {
    private final Execution execution;
    private final TypeHierarchyResolver hierarchy;

    public MemberSearchEngine(Trinity trinity) {
        this.execution = Objects.requireNonNull(trinity).getExecution();
        this.hierarchy = new TypeHierarchyResolver(execution);
    }

    public List<Input<?>> candidates(MemberSearchQuery.Target target) {
        List<Input<?>> candidates = new ArrayList<>();
        for (ClassInput classInput : execution.getClassList()) {
            switch (target) {
                case CLASS -> candidates.add(classInput);
                case FIELD -> candidates.addAll(classInput.getFieldMap().values());
                case METHOD -> candidates.addAll(classInput.getMethodMap().values());
            }
        }
        return candidates;
    }

    public List<String> validate(MemberSearchQuery query) {
        List<String> errors = new ArrayList<>();
        validateText(query.common().name(), "Name", errors);
        if (!query.common().descriptor().isEmpty()
                && query.common().descriptorMode() == MemberSearchQuery.DescriptorMode.REGEX) {
            validateRegex(query.common().descriptor(), "Descriptor", errors);
        } else if (!query.common().descriptor().isEmpty()) {
            validateDescriptor(query.target(), query.common().descriptor(), errors);
        }
        validateRange(query.common().referenceRange(), "Reference count", errors);
        validateType(query.common().declaringClass(), false, "Declaring class", errors);
        validateOptionalObjectType(query.common().genericType(), "Generic signature type", errors);
        validateOptionalObjectType(query.common().annotationType(), "Annotation type", errors);

        switch (query.target()) {
            case CLASS -> validateType(query.classCriteria().baseType(), false, "Base type", errors);
            case FIELD -> validateType(query.fieldCriteria().declaredType(), false, "Declared type", errors);
            case METHOD -> {
                validateType(query.methodCriteria().returnType(), true, "Return type", errors);
                validateType(query.methodCriteria().parameterType(), false, "Parameter type", errors);
                validateRange(query.methodCriteria().parameterCount(), "Parameter count", errors);
                validateRange(query.methodCriteria().instructionCount(), "Instruction count", errors);
                if (!query.methodCriteria().exactParameters().isEmpty()) {
                    try {
                        MemberSearchTypeUtil.parseParameterList(query.methodCriteria().exactParameters());
                    } catch (IllegalArgumentException exception) {
                        errors.add("Parameters: " + exception.getMessage());
                    }
                }
            }
        }

        MemberSearchQuery.Scope scope = query.scope();
        if (scope.kind() == MemberSearchQuery.ScopeKind.INPUT && scope.container() == null) {
            errors.add("Choose an imported archive");
        }
        if (scope.kind() == MemberSearchQuery.ScopeKind.PACKAGE && scope.pkg() == null) {
            errors.add("Choose a package");
        }
        return List.copyOf(errors);
    }

    public List<MemberSearchResult> search(MemberSearchQuery query) {
        List<String> errors = validate(query);
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));
        return candidates(query.target()).stream()
                .map(input -> evaluate(query, input))
                .filter(Objects::nonNull)
                .toList();
    }

    public MemberSearchResult evaluate(MemberSearchQuery query, Input<?> input) {
        if (!matchesTarget(query.target(), input) || !matchesScope(query.scope(), input)) return null;
        if (!matchesCommon(query, input)) return null;

        boolean matches = switch (query.target()) {
            case CLASS -> matchesClass(query.classCriteria(), (ClassInput) input);
            case FIELD -> matchesField(query.fieldCriteria(), (FieldInput) input);
            case METHOD -> matchesMethod(query.methodCriteria(), (MethodInput) input);
        };
        return matches ? createResult(query.target(), input) : null;
    }

    public int unresolvedHierarchyComparisons() {
        return hierarchy.getUnresolvedComparisons();
    }

    private boolean matchesCommon(MemberSearchQuery query, Input<?> input) {
        MemberSearchQuery.Common common = query.common();
        int access = input.getAccessFlagsMask();
        if (!matchesVisibility(common.visibility(), access)) return false;
        if (!matchesFlags(common.flags(), access)) return false;
        if (!matchesName(common.name(), input)) return false;

        if (input instanceof MemberInput<?> member) {
            if (common.ownerKind() != MemberSearchQuery.ClassKind.ANY
                    && classKind(member.getOwningClass().getNode()) != common.ownerKind()) return false;
            if (common.declaringClass().active()
                    && !matchesType(Type.getObjectType(member.getOwningClass().getRealName()),
                    common.declaringClass(), false)) return false;
            if (!common.descriptor().isEmpty()
                    && !matchesDescriptor(member.getDescriptor(), common.descriptor(),
                    common.descriptorMode())) return false;
        }

        String signature = signature(input);
        if (!common.genericType().isEmpty()) {
            Type genericType = MemberSearchTypeUtil.parseType(common.genericType(), false);
            if (genericType.getSort() != Type.OBJECT
                    || !MemberSearchTypeUtil.signatureContains(signature, genericType.getInternalName())) return false;
        }
        if (!common.annotationType().isEmpty() && !matchesAnnotation(input, common)) return false;

        boolean renamed = input.getDisplayName().getType() != RenameType.NONE;
        if (common.renameState() == MemberSearchQuery.RenameState.RENAMED && !renamed) return false;
        if (common.renameState() == MemberSearchQuery.RenameState.ORIGINAL && renamed) return false;

        if (common.referenceState() != MemberSearchQuery.ReferenceState.ANY
                || common.referenceRange().active()) {
            int count = referenceCount(input);
            if (common.referenceState() == MemberSearchQuery.ReferenceState.REFERENCED && count == 0) return false;
            if (common.referenceState() == MemberSearchQuery.ReferenceState.UNREFERENCED && count != 0) return false;
            if (!common.referenceRange().contains(count)) return false;
        }
        return true;
    }

    private boolean matchesClass(MemberSearchQuery.ClassCriteria criteria, ClassInput input) {
        if (criteria.kind() != MemberSearchQuery.ClassKind.ANY
                && classKind(input.getNode()) != criteria.kind()) return false;
        if (!criteria.baseType().active()) return true;

        Type base = MemberSearchTypeUtil.parseType(criteria.baseType().text(), false);
        if (base.getSort() != Type.OBJECT) return false;
        if (input.getRealName().equals(base.getInternalName())) return false;
        boolean direct = criteria.depth() == MemberSearchQuery.HierarchyDepth.DIRECT;

        if (criteria.baseType().mode() == MemberSearchQuery.TypeMode.ASSIGNABLE_FROM) {
            return hierarchy.isSubtype(base.getInternalName(), input.getRealName(), direct)
                    == TypeHierarchyResolver.Result.MATCH;
        }
        return hierarchy.isSubtype(input.getRealName(), base.getInternalName(), direct)
                == TypeHierarchyResolver.Result.MATCH;
    }

    private boolean matchesField(MemberSearchQuery.FieldCriteria criteria, FieldInput input) {
        return !criteria.declaredType().active()
                || matchesType(Type.getType(input.getDescriptor()), criteria.declaredType(), false);
    }

    private boolean matchesMethod(MemberSearchQuery.MethodCriteria criteria, MethodInput input) {
        MethodNode node = input.getNode();
        boolean constructor = input.isInit();
        boolean staticInitializer = input.isClinit();
        if (criteria.kind() == MemberSearchQuery.MethodKind.REGULAR && (constructor || staticInitializer)) return false;
        if (criteria.kind() == MemberSearchQuery.MethodKind.CONSTRUCTOR && !constructor) return false;
        if (criteria.kind() == MemberSearchQuery.MethodKind.STATIC_INITIALIZER && !staticInitializer) return false;

        Type methodType = Type.getMethodType(node.desc);
        if (criteria.returnType().active()
                && !matchesType(methodType.getReturnType(), criteria.returnType(), true)) return false;
        if (criteria.parameterType().active()) {
            boolean found = false;
            for (Type argument : methodType.getArgumentTypes()) {
                if (matchesType(argument, criteria.parameterType(), false)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        if (!criteria.exactParameters().isEmpty()) {
            List<Type> requested = MemberSearchTypeUtil.parseParameterList(criteria.exactParameters());
            Type[] actual = methodType.getArgumentTypes();
            if (requested.size() != actual.length) return false;
            for (int index = 0; index < actual.length; index++) {
                if (!actual[index].equals(requested.get(index))) return false;
            }
        }
        if (!criteria.parameterCount().contains(methodType.getArgumentTypes().length)) return false;

        int instructions = instructionCount(node);
        if (criteria.bodyState() == MemberSearchQuery.BodyState.HAS_BODY && instructions == 0) return false;
        if (criteria.bodyState() == MemberSearchQuery.BodyState.NO_BODY && instructions != 0) return false;
        return criteria.instructionCount().contains(instructions);
    }

    private boolean matchesType(Type actual, MemberSearchQuery.TypeCriterion criterion, boolean allowVoid) {
        Type requested = MemberSearchTypeUtil.parseType(criterion.text(), allowVoid);
        return MemberSearchTypeUtil.matches(actual, requested, criterion.mode(), hierarchy);
    }

    private boolean matchesAnnotation(Input<?> input, MemberSearchQuery.Common common) {
        Type requested = MemberSearchTypeUtil.parseType(common.annotationType(), false);
        if (requested.getSort() != Type.OBJECT) return false;
        String descriptor = requested.getDescriptor();
        boolean declaration;
        if (input instanceof ClassInput classInput) {
            declaration = hasAnnotation(classInput.getNode().visibleAnnotations, descriptor)
                    || hasAnnotation(classInput.getNode().invisibleAnnotations, descriptor);
        } else if (input instanceof FieldInput fieldInput) {
            declaration = hasAnnotation(fieldInput.getNode().visibleAnnotations, descriptor)
                    || hasAnnotation(fieldInput.getNode().invisibleAnnotations, descriptor);
        } else if (input instanceof MethodInput methodInput) {
            declaration = hasAnnotation(methodInput.getNode().visibleAnnotations, descriptor)
                    || hasAnnotation(methodInput.getNode().invisibleAnnotations, descriptor);
        } else {
            declaration = false;
        }
        if (!(input instanceof MethodInput methodInput)) return declaration;

        boolean parameter = hasParameterAnnotation(methodInput.getNode().visibleParameterAnnotations, descriptor)
                || hasParameterAnnotation(methodInput.getNode().invisibleParameterAnnotations, descriptor);
        return switch (common.annotationLocation()) {
            case DECLARATION_OR_PARAMETER -> declaration || parameter;
            case DECLARATION -> declaration;
            case PARAMETER -> parameter;
        };
    }

    private MemberSearchResult createResult(MemberSearchQuery.Target target, Input<?> input) {
        ClassInput owner = input.getOwningClass();
        String ownerName = owner.getDisplayName().getName().replace('/', '.');
        String name;
        String type = "";
        String descriptor = "";
        int instructions = -1;
        if (input instanceof ClassInput classInput) {
            name = classInput.getDisplayName().getName().replace('/', '.');
            type = inheritanceSummary(classInput.getNode());
        } else if (input instanceof FieldInput fieldInput) {
            name = fieldInput.getDisplayName().getName();
            descriptor = fieldInput.getDescriptor();
            type = MemberSearchTypeUtil.readable(Type.getType(descriptor));
        } else {
            MethodInput methodInput = (MethodInput) input;
            name = methodInput.getDisplayName().getName();
            descriptor = methodInput.getDescriptor();
            type = methodSummary(descriptor);
            instructions = instructionCount(methodInput.getNode());
        }
        Package pkg = owner.getClassTarget().getPackage();
        String packageName = pkg == null || pkg.getPrettyPath().isEmpty()
                ? "<default>" : pkg.getPrettyPath();
        String container = owner.getClassTarget().getContainer() == null
                ? "Loose Classes" : owner.getClassTarget().getContainer().getName();
        return new MemberSearchResult(target, input, name, ownerName, classKind(owner.getNode()).getName(),
                type, descriptor, accessText(input), container, packageName,
                referenceCount(input), instructions);
    }

    private int referenceCount(Input<?> input) {
        if (input instanceof ClassInput classInput) {
            return countSemantic(execution.getXrefMap().queryClassReferences(classInput.getClassTarget()));
        }
        MemberInput<?> member = (MemberInput<?>) input;
        return countSemantic(execution.getXrefMap().queryMemberReferences(member.getDetails()));
    }

    private static int countSemantic(Collection<? extends AbstractXref> references) {
        int count = 0;
        for (AbstractXref reference : references) {
            if (reference.getKind() != XrefKind.METADATA
                    && reference.getKind() != XrefKind.STACK_FRAME) count++;
        }
        return count;
    }

    private static boolean matchesTarget(MemberSearchQuery.Target target, Input<?> input) {
        return switch (target) {
            case CLASS -> input instanceof ClassInput;
            case FIELD -> input instanceof FieldInput;
            case METHOD -> input instanceof MethodInput;
        };
    }

    private static boolean matchesScope(MemberSearchQuery.Scope scope, Input<?> input) {
        ClassInput owner = input.getOwningClass();
        return switch (scope.kind()) {
            case PROJECT -> true;
            case INPUT -> owner.getClassTarget().getContainer() == scope.container();
            case PACKAGE -> matchesPackage(owner.getClassTarget().getPackage(), scope.pkg(),
                    scope.includeSubpackages());
        };
    }

    private static boolean matchesPackage(Package candidate, Package requested, boolean recursive) {
        if (candidate == null || requested == null || candidate.getContainer() != requested.getContainer()) return false;
        if (!recursive) return candidate == requested;
        for (Package current = candidate; current != null; current = current.getParent()) {
            if (current == requested) return true;
        }
        return false;
    }

    private static boolean matchesVisibility(MemberSearchQuery.Visibility visibility, int access) {
        return switch (visibility) {
            case ANY -> true;
            case PUBLIC -> (access & Opcodes.ACC_PUBLIC) != 0;
            case PROTECTED -> (access & Opcodes.ACC_PROTECTED) != 0;
            case PRIVATE -> (access & Opcodes.ACC_PRIVATE) != 0;
            case PACKAGE_PRIVATE -> (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED
                    | Opcodes.ACC_PRIVATE)) == 0;
        };
    }

    private static boolean matchesFlags(Map<Integer, MemberSearchQuery.FlagMode> flags, int access) {
        for (Map.Entry<Integer, MemberSearchQuery.FlagMode> entry : flags.entrySet()) {
            boolean set = (access & entry.getKey()) != 0;
            if (entry.getValue() == MemberSearchQuery.FlagMode.REQUIRE && !set) return false;
            if (entry.getValue() == MemberSearchQuery.FlagMode.EXCLUDE && set) return false;
        }
        return true;
    }

    private static boolean matchesName(MemberSearchQuery.TextCriterion criterion, Input<?> input) {
        if (!criterion.active()) return true;
        List<String> names = new ArrayList<>();
        if (input instanceof ClassInput classInput) {
            addClassNameVariants(names, classInput.getRealName());
            addClassNameVariants(names, classInput.getDisplayName().getName());
        } else {
            MemberInput<?> member = (MemberInput<?>) input;
            names.add(member.getDetails().getName());
            names.add(member.getDisplayName().getName());
        }

        String needle = criterion.caseSensitive() ? criterion.text()
                : criterion.text().toLowerCase(Locale.ROOT);
        Predicate<String> match = switch (criterion.mode()) {
            case CONTAINS -> value -> normalized(value, criterion.caseSensitive()).contains(needle);
            case EXACT -> value -> normalized(value, criterion.caseSensitive()).equals(needle);
            case REGEX -> {
                int flags = criterion.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                Pattern pattern = Pattern.compile(criterion.text(), flags);
                yield value -> pattern.matcher(value).find();
            }
        };
        return names.stream().distinct().anyMatch(match);
    }

    private static void addClassNameVariants(List<String> names, String name) {
        names.add(name);
        names.add(name.replace('/', '.'));
        int slash = name.lastIndexOf('/');
        names.add(slash < 0 ? name : name.substring(slash + 1));
    }

    private static String normalized(String value, boolean caseSensitive) {
        return caseSensitive ? value : value.toLowerCase(Locale.ROOT);
    }

    private static boolean matchesDescriptor(String actual, String requested,
                                             MemberSearchQuery.DescriptorMode mode) {
        return mode == MemberSearchQuery.DescriptorMode.EXACT
                ? actual.equals(requested) : Pattern.compile(requested).matcher(actual).find();
    }

    private static MemberSearchQuery.ClassKind classKind(ClassNode node) {
        if ((node.access & Opcodes.ACC_ANNOTATION) != 0) return MemberSearchQuery.ClassKind.ANNOTATION;
        if ((node.access & Opcodes.ACC_ENUM) != 0) return MemberSearchQuery.ClassKind.ENUM;
        if ((node.access & Opcodes.ACC_RECORD) != 0 || node.recordComponents != null && !node.recordComponents.isEmpty()) {
            return MemberSearchQuery.ClassKind.RECORD;
        }
        if ((node.access & Opcodes.ACC_INTERFACE) != 0) return MemberSearchQuery.ClassKind.INTERFACE;
        return MemberSearchQuery.ClassKind.CLASS;
    }

    private static String signature(Input<?> input) {
        if (input instanceof ClassInput classInput) return classInput.getNode().signature;
        if (input instanceof FieldInput fieldInput) return fieldInput.getNode().signature;
        if (input instanceof MethodInput methodInput) return methodInput.getNode().signature;
        return null;
    }

    private static boolean hasAnnotation(List<AnnotationNode> annotations, String descriptor) {
        return annotations != null && annotations.stream().anyMatch(annotation -> descriptor.equals(annotation.desc));
    }

    private static boolean hasParameterAnnotation(List<AnnotationNode>[] annotations, String descriptor) {
        if (annotations == null) return false;
        for (List<AnnotationNode> parameter : annotations) {
            if (hasAnnotation(parameter, descriptor)) return true;
        }
        return false;
    }

    private static int instructionCount(MethodNode node) {
        int count = 0;
        for (AbstractInsnNode instruction : node.instructions) {
            if (instruction.getOpcode() >= 0) count++;
        }
        return count;
    }

    private static String inheritanceSummary(ClassNode node) {
        List<String> parents = new ArrayList<>();
        if (node.superName != null && !node.superName.equals("java/lang/Object")) {
            parents.add(node.superName.replace('/', '.'));
        }
        if (node.interfaces != null) node.interfaces.stream()
                .map(name -> name.replace('/', '.')).forEach(parents::add);
        return parents.isEmpty() ? "java.lang.Object" : String.join(", ", parents);
    }

    private static String methodSummary(String descriptor) {
        Type method = Type.getMethodType(descriptor);
        String parameters = java.util.Arrays.stream(method.getArgumentTypes())
                .map(MemberSearchTypeUtil::readable).reduce((left, right) -> left + ", " + right).orElse("");
        return "(" + parameters + ") -> " + MemberSearchTypeUtil.readable(method.getReturnType());
    }

    private static String accessText(Input<?> input) {
        int access = input.getAccessFlagsMask();
        List<String> parts = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0) parts.add("public");
        else if ((access & Opcodes.ACC_PROTECTED) != 0) parts.add("protected");
        else if ((access & Opcodes.ACC_PRIVATE) != 0) parts.add("private");
        else parts.add("package-private");
        if (input instanceof ClassInput) {
            if ((access & Opcodes.ACC_FINAL) != 0) parts.add("final");
            if ((access & Opcodes.ACC_ABSTRACT) != 0) parts.add("abstract");
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) parts.add("synthetic");
        } else if (input instanceof FieldInput) {
            if ((access & Opcodes.ACC_STATIC) != 0) parts.add("static");
            if ((access & Opcodes.ACC_FINAL) != 0) parts.add("final");
            if ((access & Opcodes.ACC_VOLATILE) != 0) parts.add("volatile");
            if ((access & Opcodes.ACC_TRANSIENT) != 0) parts.add("transient");
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) parts.add("synthetic");
        } else if (input instanceof MethodInput) {
            if ((access & Opcodes.ACC_STATIC) != 0) parts.add("static");
            if ((access & Opcodes.ACC_FINAL) != 0) parts.add("final");
            if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) parts.add("synchronized");
            if ((access & Opcodes.ACC_BRIDGE) != 0) parts.add("bridge");
            if ((access & Opcodes.ACC_VARARGS) != 0) parts.add("varargs");
            if ((access & Opcodes.ACC_NATIVE) != 0) parts.add("native");
            if ((access & Opcodes.ACC_ABSTRACT) != 0) parts.add("abstract");
            if ((access & Opcodes.ACC_STRICT) != 0) parts.add("strict");
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) parts.add("synthetic");
        }
        return String.join(" ", parts);
    }

    private static void validateDescriptor(MemberSearchQuery.Target target, String descriptor,
                                           List<String> errors) {
        if (target == MemberSearchQuery.Target.CLASS) return;
        try {
            if (target == MemberSearchQuery.Target.METHOD) {
                if (!descriptor.startsWith("(")) throw new IllegalArgumentException();
                Type.getMethodType(descriptor);
            } else {
                Type type = Type.getType(descriptor);
                if (type.getSort() == Type.METHOD || type.getSort() == Type.VOID) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (IllegalArgumentException exception) {
            errors.add("Descriptor is not a valid " + target.getName().toLowerCase(Locale.ROOT)
                    + " descriptor");
        }
    }

    private static void validateText(MemberSearchQuery.TextCriterion criterion, String label,
                                     List<String> errors) {
        if (criterion.active() && criterion.mode() == MemberSearchQuery.TextMode.REGEX) {
            validateRegex(criterion.text(), label, errors);
        }
    }

    private static void validateRegex(String regex, String label, List<String> errors) {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException exception) {
            errors.add(label + ": " + exception.getDescription());
        }
    }

    private static void validateType(MemberSearchQuery.TypeCriterion criterion, boolean allowVoid,
                                     String label, List<String> errors) {
        if (!criterion.active()) return;
        try {
            MemberSearchTypeUtil.parseType(criterion.text(), allowVoid);
        } catch (IllegalArgumentException exception) {
            errors.add(label + ": " + exception.getMessage());
        }
    }

    private static void validateOptionalObjectType(String value, String label, List<String> errors) {
        if (value == null || value.isBlank()) return;
        try {
            if (MemberSearchTypeUtil.parseType(value, false).getSort() != Type.OBJECT) {
                errors.add(label + " must be a class or interface");
            }
        } catch (IllegalArgumentException exception) {
            errors.add(label + ": " + exception.getMessage());
        }
    }

    private static void validateRange(MemberSearchQuery.IntRange range, String label,
                                      List<String> errors) {
        if (range.minimum() < -1 || range.maximum() < -1) {
            errors.add(label + " cannot be less than zero");
        } else if (range.minimum() >= 0 && range.maximum() >= 0
                && range.minimum() > range.maximum()) {
            errors.add(label + " minimum cannot exceed maximum");
        }
    }
}
