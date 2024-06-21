package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.MethodInput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private boolean built;

    public ClassHierarchy(ClassInput currentClass) {
        this.currentClass = currentClass;
    }

    void buildHierarchy(Execution execution) {
        if (this.built) {
            return;
        }
        this.built = true;

        for (String itf : this.currentClass.getInterfaces()) {
            final @Nullable ClassInput interfaceInput = execution.getClassInput(itf);

            if (interfaceInput != null) {
                this.extending.add(interfaceInput);
                this.interfaces.add(interfaceInput);
            }
        }

        final @Nullable ClassInput superClass = execution.getClassInput(this.currentClass.getSuperName());

        if (superClass != null && superClass != this.currentClass) {
            this.superClass = superClass;

            this.addSuperElements(execution, superClass);
        }

        for (ClassInput extendingInput : this.extending) {
            extendingInput.getClassHierarchy().getExtending().add(this.getCurrentClass());
        }

        for (MethodInput methodInput : this.currentClass.getMethodMap().values()) {
            if (methodInput.getMethodHierarchy() != null || methodInput.getAccessFlags().isStatic()) {
                continue;
            }

            final MethodHierarchy methodHierarchy = new MethodHierarchy();
            methodHierarchy.linkMethod(methodInput);

            for (ClassInput extendingInput : this.extending) {
                final @Nullable MethodInput overridingMethod = extendingInput.getMethod(methodInput.getName(), methodInput.getDescriptor());

                if (overridingMethod != null && !overridingMethod.getAccessFlags().isStatic()) {
                    methodHierarchy.linkMethod(overridingMethod);
                }
            }
        }
    }

    private void addSuperElements(Execution execution, ClassInput superClass) {
        final List<ClassInput> processedSupers = new ArrayList<>(1);

        while (superClass != null) {
            ClassHierarchy classHierarchy = superClass.getClassHierarchy();
            classHierarchy.buildHierarchy(execution);

            extending.add(superClass);
            superClasses.add(superClass);
            interfaces.addAll(classHierarchy.getInterfaces());

            processedSupers.add(superClass);

            if (processedSupers.contains(classHierarchy.getSuperClass())) {
                break;
            }

            superClass = classHierarchy.getSuperClass();
        }
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
}
