package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.DecompilerMemberReader;
import me.f1nal.trinity.decompiler.output.component.AbstractTextComponent;
import me.f1nal.trinity.decompiler.output.lines.ComponentGroup;
import me.f1nal.trinity.decompiler.output.lines.LineText;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.util.StringUtil;

import java.io.IOException;
import java.util.*;

public final class DecompiledClass {
    private final Trinity trinity;
    private final ClassInput classInput;
    private final List<ComponentGroup> componentGroupList = new ArrayList<>();
    private final Map<MethodInput, DecompiledMethod> decompiledMethodMap = new HashMap<>();

    public DecompiledClass(Trinity trinity, ClassInput classInput, final String rawOutput) throws IOException {
        this.trinity = trinity;
        this.classInput = classInput;

        final DecompilerMemberReader reader = new DecompilerMemberReader(this, rawOutput);
        this.addLines(reader.getComponentList());

        System.out.println("Decompiling " + classInput.getFullName());
    }

    public void resetLines() {
        List<AbstractTextComponent> componentList = new ArrayList<>(getComponentGroupList().size());
        for (ComponentGroup group : getComponentGroupList()) {
            componentList.add(group.getComponent());
        }
        this.componentGroupList.clear();
        this.addLines(componentList);
    }

    private void addLines(List<AbstractTextComponent> componentList) {
        boolean normalizeText = Main.getPreferences().isDecompilerNormalizeText();

        for (int j = 0, textComponentListSize = componentList.size(); j < textComponentListSize; j++) {
            AbstractTextComponent textComponent = componentList.get(j);

//            if (textComponent.getText().isEmpty()) {
//                continue;
//            }

            ComponentGroup group = this.beginGroup(textComponent);
            textComponent.setId(j);

            String text = textComponent.getText();
            String[] split = text.split("\n");
            int lines = 0;

            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lines++;
                }
            }

            for (String line : split) {
                if (line == null || line.isEmpty()) {
                    group.addText(new LineText.LineTextNewline());
                } else {
                    group.addText(new LineText.LineTextComponent(normalizeText ? StringUtil.convertStringToJava(line, true) : line, textComponent::getTextColor));
                }
                --lines;
            }
            if (lines < 0) {
                group.addText(new LineText.LineTextSameLine());
            }
            for (int i = 0; i < lines; i++) {
                group.addText(new LineText.LineTextNewline());
            }
        }
    }

    private ComponentGroup beginGroup(AbstractTextComponent component) {
        ComponentGroup group = new ComponentGroup(component);
        this.componentGroupList.add(group);
        return group;
    }

    public DecompiledMethod getMethod(MethodInput methodInput) {
        return decompiledMethodMap.get(methodInput);
    }

    public DecompiledMethod createMethod(MethodInput methodInput) {
        return decompiledMethodMap.computeIfAbsent(methodInput, DecompiledMethod::new);
    }

    public Collection<DecompiledMethod> getMethods() {
        return decompiledMethodMap.values();
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    public Trinity getTrinity() {
        return trinity;
    }

    public List<ComponentGroup> getComponentGroupList() {
        return componentGroupList;
    }

    public boolean containsComponent(AbstractTextComponent component) {
        for (ComponentGroup group : componentGroupList) {
            if (group.getComponent().equals(component)) {
                return true;
            }
        }
        return false;
    }
}
