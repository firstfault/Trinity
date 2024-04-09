package me.f1nal.trinity.gui.windows.impl.classstructure;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNodeClass;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNodeField;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNodeMethod;

import java.util.Objects;

public class ClassStructure {
    private final ClassInput classInput;
    private final ClassStructureNodeClass rootNode;

    public ClassStructure(ClassInput classInput) {
        this.classInput = Objects.requireNonNull(classInput, "Cannot initialize structure with a null class");
        this.rootNode = new ClassStructureNodeClass(classInput);
        this.rootNode.getBrowserViewerNode().setDefaultOpen(true);

        classInput.getFieldList().values().stream().map(ClassStructureNodeField::new).forEach(this.rootNode::addChild);
        classInput.getMethodList().values().stream().map(ClassStructureNodeMethod::new).forEach(this.rootNode::addChild);
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    public ClassStructureNodeClass getRootNode() {
        return rootNode;
    }
}
