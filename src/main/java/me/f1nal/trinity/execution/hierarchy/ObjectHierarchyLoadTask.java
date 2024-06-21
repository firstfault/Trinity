package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;

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
        this.startWork(classList.size() * 2);

        for (ClassInput classInput : classList) {
            classInput.getClassHierarchy().buildHierarchy(execution);
            this.finishedWork();
        }

        for (ClassInput classInput : classList) {
            Set<ClassInput> inheritors = classInput.getClassHierarchy().getExtending();
            for (ClassInput inheritor : inheritors) {
                for (MethodInput superMethod : classInput.getMethodMap().values()) {
                    inheritor.addInheritedMethod(superMethod);
                }
            }
            this.finishedWork();
        }
    }
}
