package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.MethodInput;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public final class ClassHierarchy {
    private final ClassInput currentClass;
    private @Nullable ClassInput superClass;
    private final Set<ClassInput> superClasses = new LinkedHashSet<>(1);
    private final Set<ClassInput> interfaces = new LinkedHashSet<>(1);
    /**
     * All classes that extend/implement us.
     */
    private final Set<ClassInput> inheritors = new LinkedHashSet<>(1);
    /**
     * All classes that we extend/implement.
     */
    private final Set<ClassInput> extending = new LinkedHashSet<>(1);
    private BuildState buildState = BuildState.UNBUILT;

    public ClassHierarchy(ClassInput currentClass) {
        this.currentClass = currentClass;
    }

    private void buildHierarchy(Function<String, @Nullable ClassInput> classLookup) {
        if (buildState != BuildState.UNBUILT) {
            return;
        }
        buildState = BuildState.BUILDING;

        for (String itf : this.currentClass.getInterfaces()) {
            final @Nullable ClassInput interfaceInput = classLookup.apply(itf);

            if (interfaceInput != null && interfaceInput != currentClass) {
                interfaceInput.getClassHierarchy().buildHierarchy(classLookup);
                this.extending.add(interfaceInput);
                this.interfaces.add(interfaceInput);
                this.extending.addAll(interfaceInput.getClassHierarchy().getExtending());
                this.interfaces.addAll(interfaceInput.getClassHierarchy().getInterfaces());
            }
        }

        final @Nullable ClassInput superClass = classLookup.apply(this.currentClass.getSuperName());

        if (superClass != null && superClass != this.currentClass) {
            this.superClass = superClass;
            superClass.getClassHierarchy().buildHierarchy(classLookup);
            this.extending.add(superClass);
            this.extending.addAll(superClass.getClassHierarchy().getExtending());
            this.superClasses.add(superClass);
            this.superClasses.addAll(superClass.getClassHierarchy().getSuperClasses());
            this.interfaces.addAll(superClass.getClassHierarchy().getInterfaces());
        }

        extending.remove(currentClass);
        superClasses.remove(currentClass);
        interfaces.remove(currentClass);
        buildState = BuildState.BUILT;
        for (ClassInput ancestor : this.extending) {
            ancestor.getClassHierarchy().inheritors.add(this.currentClass);
        }
    }

    public static void rebuildAll(Execution execution) {
        rebuildAll(execution.getClassList(), execution::getClassInput);
    }

    static void rebuildAll(Collection<ClassInput> classes,
                           Function<String, @Nullable ClassInput> classLookup) {
        for (ClassInput classInput : classes) {
            classInput.getClassHierarchy().reset();
            classInput.getMethodMap().values().forEach(method -> method.setMethodHierarchy(null));
        }
        for (ClassInput classInput : classes) {
            classInput.getClassHierarchy().buildHierarchy(classLookup);
        }
        for (ClassInput classInput : classes) {
            classInput.getClassHierarchy().linkDeclaredMethods();
        }
    }

    private void reset() {
        superClass = null;
        superClasses.clear();
        interfaces.clear();
        inheritors.clear();
        extending.clear();
        buildState = BuildState.UNBUILT;
    }

    private void linkDeclaredMethods() {
        for (MethodInput methodInput : currentClass.getMethodMap().values()) {
            if (!canParticipateInOverride(methodInput)) {
                continue;
            }
            MethodHierarchy methodHierarchy = methodInput.getMethodHierarchy();
            if (methodHierarchy == null) {
                methodHierarchy = new MethodHierarchy();
                methodHierarchy.linkMethod(methodInput);
            }
            for (ClassInput ancestor : extending) {
                MethodInput superMethod = ancestor.getDeclaredMethod(methodInput.getName(), methodInput.getDescriptor());
                if (superMethod != null && canOverride(methodInput, superMethod)) {
                    methodHierarchy.linkMethod(superMethod);
                }
            }
        }
    }

    private static boolean canOverride(MethodInput method, MethodInput superMethod) {
        if (!canParticipateInOverride(superMethod)
                || (superMethod.getAccessFlagsMask() & Opcodes.ACC_FINAL) != 0) {
            return false;
        }
        int access = superMethod.getAccessFlagsMask();
        return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                || packageName(method.getOwningClass()).equals(packageName(superMethod.getOwningClass()));
    }

    private static boolean canParticipateInOverride(MethodInput method) {
        int access = method.getAccessFlagsMask();
        return !method.isInitOrClinit()
                && (access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0;
    }

    private static String packageName(ClassInput classInput) {
        String name = classInput.getRealName();
        int separator = name.lastIndexOf('/');
        return separator < 0 ? "" : name.substring(0, separator);
    }

    public Set<ClassInput> getInheritors() {
        return inheritors;
    }

    public Set<ClassInput> getExtending() {
        return extending;
    }

    public Set<ClassInput> getSuperClasses() {
        return superClasses;
    }

    public Set<ClassInput> getInterfaces() {
        return interfaces;
    }

    public ClassInput getCurrentClass() {
        return currentClass;
    }

    public ClassInput getSuperClass() {
        return superClass;
    }

    private enum BuildState {
        UNBUILT,
        BUILDING,
        BUILT
    }
}
