package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.component.ClassComponent;
import me.f1nal.trinity.gui.frames.impl.annotation.AnnotationInsertPopup;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.List;

public class AnnotationEditor {
    private final List<AnnotationNode> annotationNodes;
    private final Trinity trinity;
    private final String componentId = ComponentId.getId(this.getClass());
    private int id;

    public AnnotationEditor(List<AnnotationNode> annotationNodes, Trinity trinity) {
        this.annotationNodes = annotationNodes;
        this.trinity = trinity;
    }

    public void draw() {
        if (ImGui.smallButton("Insert###InsertBtn" + componentId)) {
            Main.getDisplayManager().addPopup(new AnnotationInsertPopup(trinity));
        }

        id = 0;
        AnnotationNode[] copy = annotationNodes.toArray(new AnnotationNode[0]);
        for (int i = 0, copyLength = copy.length; i < copyLength; i++) {
            AnnotationNode annotationNode = copy[i];
            ImGui.textDisabled(annotationNode.desc);
            ImGui.sameLine(0.F, 0.F);
            drawAnnotationArgs(annotationNode);
            if (ImGui.smallButton("Delete###Delete" + componentId + i)) {
                annotationNodes.remove(annotationNode);
            }
            ImGui.sameLine(0.F, 3.F);
            if (ImGui.smallButton("Edit###Edit" + componentId + i)) {
//                delete = annotationNode;
            }
            ImGui.sameLine(0.F, 3.F);
            if (ImGui.smallButton("Duplicate###Duplicate" + componentId + i)) {
                annotationNodes.add(annotationNodes.indexOf(annotationNode), copyAnnotation(annotationNode));
            }
        }
    }

    private AnnotationNode copyAnnotation(AnnotationNode cpy) {
        AnnotationNode dst = new AnnotationNode(cpy.desc);
        ArrayList<Object> values = new ArrayList<>();
        if (cpy.values != null) for (Object value : cpy.values) {
            if (value instanceof AnnotationNode) {
                values.add(copyAnnotation((AnnotationNode) value));
            } else {
                values.add(value);
            }
        }
        dst.values = values;
        return dst;
    }

    private void drawAnnotationArgs(AnnotationNode annotationNode) {
        if (annotationNode.values == null || annotationNode.values.isEmpty()) {
            return;
        }
        ImGui.textDisabled("(");
        for (int i = 0; i < annotationNode.values.size(); i+=2) {
            String name = (String) annotationNode.values.get(i);
            Object value = annotationNode.values.get(i + 1);
            ImGui.sameLine(0.F, 0.F);
            ImGui.textDisabled(name + "=");
            ImGui.sameLine(0.F, 0.F);
            this.drawAnnotationValue(value);
            if (i != annotationNode.values.size() - 2) {
                ImGui.sameLine(0.F, 0.F);
                ImGui.textDisabled(", ");
            }
        }
        ImGui.sameLine(0.F, 0.F);
        ImGui.textDisabled(")");
    }

    private void drawAnnotationValue(Object value) {
        ++id;
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            ImGui.textDisabled("[");
            ImGui.sameLine(0.F, 0.F);
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                Object o = list.get(i);
                this.drawAnnotationValue(o);
                if (i != listSize - 1) {
                    ImGui.sameLine(0.F, 0.F);
                    ImGui.textDisabled(", ");
                }
            }
            ImGui.sameLine(0.F, 0.F);
            ImGui.textDisabled("]");
            ImGui.sameLine(0.F, 0.F);
            return;
        }

        if (value instanceof Type) {
            String className = ((Type) value).getClassName().replace('.', '/');
            ClassComponent comp = new ClassComponent(className, className, false, trinity.getExecution().getClassInput(className), trinity);
            ImGui.textColored(comp.getTextColor(), comp.getText());
            comp.setId(id);
            if (ImGui.isItemHovered()) {
                comp.handleItemHover();
            }
            comp.handleAfterDrawing();
            return;
        }

        ImGui.text(value == null ? "null" : value.toString());
    }
}
