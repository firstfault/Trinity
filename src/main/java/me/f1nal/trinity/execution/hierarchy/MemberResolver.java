package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Resolves symbolic JVM member references against the loaded class hierarchy. */
public final class MemberResolver {
    private MemberResolver() {
    }

    public static @Nullable MethodInput resolveMethod(ClassInput owner, String name, String descriptor) {
        Execution execution = owner.getExecution();
        if (execution == null) {
            return null;
        }
        return resolveMethods(execution::getClassInput, owner, name, descriptor).stream().findFirst().orElse(null);
    }

    public static @Nullable FieldInput resolveField(ClassInput owner, String name, String descriptor) {
        Execution execution = owner.getExecution();
        if (execution == null) {
            return null;
        }
        return resolveField(execution::getClassInput, owner, name, descriptor, new HashSet<>());
    }

    public static Collection<MethodInput> resolveInvocationTargets(Execution execution, MethodInsnNode instruction) {
        return resolveInvocationTargets(execution::getClassInput, null, instruction);
    }

    public static Collection<MethodInput> resolveInvocationTargets(
            Execution execution, ClassInput caller, MethodInsnNode instruction) {
        return resolveInvocationTargets(execution::getClassInput, caller, instruction);
    }

    static Collection<MethodInput> resolveInvocationTargets(
            Function<String, @Nullable ClassInput> classLookup, MethodInsnNode instruction) {
        return resolveInvocationTargets(classLookup, null, instruction);
    }

    static Collection<MethodInput> resolveInvocationTargets(
            Function<String, @Nullable ClassInput> classLookup, @Nullable ClassInput caller,
            MethodInsnNode instruction) {
        ClassInput symbolicOwner = classLookup.apply(instruction.owner);
        if (symbolicOwner == null) {
            return Collections.emptyList();
        }
        Collection<MethodInput> declarations = resolveMethods(
                classLookup, symbolicOwner, instruction.name, instruction.desc);
        if (selectsFromDirectSuperclass(classLookup, caller, symbolicOwner, instruction)) {
            ClassInput directSuperclass = classLookup.apply(caller.getSuperName());
            Collection<MethodInput> selected = directSuperclass == null ? Collections.emptyList()
                    : resolveMethods(classLookup, directSuperclass, instruction.name, instruction.desc);
            if (!selected.isEmpty()) {
                declarations = selected;
            }
        }
        if (declarations.isEmpty() || (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL
                && instruction.getOpcode() != Opcodes.INVOKEINTERFACE)) {
            return declarations;
        }

        Set<MethodInput> targets = new LinkedHashSet<>(declarations);
        for (MethodInput declaration : declarations) {
            MethodHierarchy hierarchy = declaration.getMethodHierarchy();
            if (hierarchy == null) {
                continue;
            }
            for (MethodInput linkedMethod : hierarchy.getLinkedMethods()) {
                ClassInput linkedOwner = linkedMethod.getOwningClass();
                if (linkedOwner == symbolicOwner
                        || isSubtype(classLookup, linkedOwner, symbolicOwner, new HashSet<>())) {
                    targets.add(linkedMethod);
                }
            }
        }
        return targets;
    }

    private static boolean selectsFromDirectSuperclass(
            Function<String, @Nullable ClassInput> classLookup, @Nullable ClassInput caller,
            ClassInput symbolicOwner, MethodInsnNode instruction) {
        return caller != null
                && instruction.getOpcode() == Opcodes.INVOKESPECIAL
                && !"<init>".equals(instruction.name)
                && !symbolicOwner.isInterface()
                && (caller.getAccessFlagsMask() & Opcodes.ACC_SUPER) != 0
                && caller != symbolicOwner
                && isSubtype(classLookup, caller, symbolicOwner, new HashSet<>());
    }

    public static @Nullable FieldInput resolveField(Execution execution, String owner,
                                                     String name, String descriptor) {
        ClassInput symbolicOwner = execution.getClassInput(owner);
        return symbolicOwner == null ? null : resolveField(symbolicOwner, name, descriptor);
    }

    static @Nullable FieldInput resolveField(Function<String, @Nullable ClassInput> classLookup,
                                             ClassInput owner, String name, String descriptor) {
        return resolveField(classLookup, owner, name, descriptor, new HashSet<>());
    }

    static Collection<MethodInput> resolveMethods(Function<String, @Nullable ClassInput> classLookup,
                                                  ClassInput owner, String name, String descriptor) {
        MethodInput declared = owner.getDeclaredMethod(name, descriptor);
        if (declared != null) {
            return List.of(declared);
        }

        Set<ClassInput> visited = new HashSet<>();
        visited.add(owner);
        ClassInput superClass = classLookup.apply(owner.getSuperName());
        while (superClass != null && visited.add(superClass)) {
            MethodInput superMethod = superClass.getDeclaredMethod(name, descriptor);
            if (superMethod != null) {
                return List.of(superMethod);
            }
            superClass = classLookup.apply(superClass.getSuperName());
        }

        LinkedHashSet<MethodInput> interfaceMethods = new LinkedHashSet<>();
        collectInterfaceMethods(classLookup, owner, name, descriptor, interfaceMethods, new HashSet<>());
        if (interfaceMethods.size() < 2) {
            return interfaceMethods;
        }

        List<MethodInput> maximallySpecific = new ArrayList<>(interfaceMethods);
        maximallySpecific.removeIf(candidate -> interfaceMethods.stream().anyMatch(other ->
                other != candidate && isSubtype(classLookup, other.getOwningClass(),
                        candidate.getOwningClass(), new HashSet<>())));
        return maximallySpecific;
    }

    private static void collectInterfaceMethods(Function<String, @Nullable ClassInput> classLookup,
                                                ClassInput type, String name, String descriptor,
                                                Set<MethodInput> output, Set<ClassInput> visited) {
        if (!visited.add(type)) {
            return;
        }
        for (String interfaceName : type.getInterfaces()) {
            ClassInput interfaceInput = classLookup.apply(interfaceName);
            if (interfaceInput == null) {
                continue;
            }
            MethodInput method = interfaceInput.getDeclaredMethod(name, descriptor);
            if (method != null && (method.getAccessFlagsMask()
                    & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0) {
                output.add(method);
            }
            collectInterfaceMethods(classLookup, interfaceInput, name, descriptor, output, visited);
        }
        ClassInput superClass = classLookup.apply(type.getSuperName());
        if (superClass != null) {
            collectInterfaceMethods(classLookup, superClass, name, descriptor, output, visited);
        }
    }

    private static @Nullable FieldInput resolveField(Function<String, @Nullable ClassInput> classLookup,
                                                     ClassInput owner, String name, String descriptor,
                                                     Set<ClassInput> visited) {
        if (!visited.add(owner)) {
            return null;
        }
        FieldInput declared = owner.getDeclaredField(name, descriptor);
        if (declared != null) {
            return declared;
        }
        for (String interfaceName : owner.getInterfaces()) {
            ClassInput interfaceInput = classLookup.apply(interfaceName);
            if (interfaceInput == null) {
                continue;
            }
            FieldInput interfaceField = resolveField(classLookup, interfaceInput, name, descriptor, visited);
            if (interfaceField != null) {
                return interfaceField;
            }
        }
        ClassInput superClass = classLookup.apply(owner.getSuperName());
        return superClass == null ? null
                : resolveField(classLookup, superClass, name, descriptor, visited);
    }

    private static boolean isSubtype(Function<String, @Nullable ClassInput> classLookup,
                                     ClassInput type, ClassInput possibleAncestor,
                                     Set<ClassInput> visited) {
        if (type == possibleAncestor) {
            return true;
        }
        if (!visited.add(type)) {
            return false;
        }
        ClassInput superClass = classLookup.apply(type.getSuperName());
        if (superClass != null && isSubtype(classLookup, superClass, possibleAncestor, visited)) {
            return true;
        }
        for (String interfaceName : type.getInterfaces()) {
            ClassInput interfaceInput = classLookup.apply(interfaceName);
            if (interfaceInput != null
                    && isSubtype(classLookup, interfaceInput, possibleAncestor, visited)) {
                return true;
            }
        }
        return false;
    }
}
