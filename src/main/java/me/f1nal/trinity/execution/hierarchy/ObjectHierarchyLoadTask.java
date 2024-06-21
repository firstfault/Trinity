package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ObjectHierarchyLoadTask extends ProgressiveLoadTask {
    private final Execution execution;

    public ObjectHierarchyLoadTask(Execution execution) {
        super("Building Object Hierarchy");
        this.execution = execution;
    }

    @Override
    public void runImpl() {
        execution.setClassesLoaded();

        final List<ClassInput> classList = execution.getClassList();
        this.startWork((classList.size() * 2) + 1);

        for (ClassInput classInput : classList) {
            classInput.getClassHierarchy().buildHierarchy(execution);
            this.finishedWork();
        }

        List<ClassWithMethod> inheritorMethods = new ArrayList<>();
        for (ClassInput classInput : classList) {
            Set<ClassInput> inheritors = classInput.getClassHierarchy().getExtending();
            for (ClassInput inheritor : inheritors) {
                for (MethodInput superMethod : classInput.getMethodMap().values()) {
                    if (superMethod.getAccessFlags().isStatic()) {
                        continue;
                    }

                    inheritorMethods.add(new ClassWithMethod(inheritor, superMethod));
                }
            }
            this.finishedWork();
        }
        for (ClassWithMethod inheritorMethod : inheritorMethods) {
            inheritorMethod.getClassInput().addInheritedMethod(inheritorMethod.getMethodInput());
        }
        this.finishedWork();
    }

    private static class ClassWithMethod {
        private final ClassInput classInput;
        private final MethodInput methodInput;

        private ClassWithMethod(ClassInput classInput, MethodInput methodInput) {
            this.classInput = classInput;
            this.methodInput = methodInput;
        }

        public ClassInput getClassInput() {
            return classInput;
        }

        public MethodInput getMethodInput() {
            return methodInput;
        }
    }
}
