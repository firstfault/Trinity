package me.f1nal.trinity.gui.frames.impl.classes.classhierarchy;

import imgui.ImGui;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.NodeEditorConfig;
import imgui.extension.nodeditor.NodeEditorContext;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.frames.ClosableWindow;

public class ClassHierarchyWindow extends ClosableWindow {
    private final ClassHierarchyNode root;
    private final NodeEditorContext nodeContext;
    private final ClassInput classInput;

    public ClassHierarchyWindow(Trinity trinity, ClassInput classInput) {
        super("Class Hierarchy", 650, 660, trinity);
        this.classInput = classInput;

        NodeEditorConfig editorConfig = new NodeEditorConfig();
        editorConfig.setSettingsFile(null);
        this.nodeContext = new NodeEditorContext(editorConfig);

        this.root = this.buildClassHierarchy();
    }

    @Override
    protected void renderFrame() {
        NodeEditor.setCurrentEditor(this.nodeContext);
        NodeEditor.begin(this.getTitle());
        this.drawNode(this.root);
        NodeEditor.end();
    }

    private void drawNode(ClassHierarchyNode root) {
        NodeEditor.beginNode(0);
        ImGui.text(root.getClassInput().getDisplayFullName());

        NodeEditor.beginPin(1, NodeEditorPinKind.Input);
        ImGui.text("extends");
        NodeEditor.endPin();

        NodeEditor.beginPin(2, NodeEditorPinKind.Output);
        ImGui.text("inherits");
        NodeEditor.endPin();

        NodeEditor.endNode();
    }

    @Override
    public String getTitle() {
        return super.getTitle() + ": " + classInput.getDisplayFullName();
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    private ClassHierarchyNode buildClassHierarchy() {
        ClassHierarchyNode root = new ClassHierarchyNode(ClassHierarchyNodeType.EXTENDS, classInput);
        return root;
    }
}
