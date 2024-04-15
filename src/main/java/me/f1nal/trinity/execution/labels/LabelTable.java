package me.f1nal.trinity.execution.labels;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.Label;

import java.util.Set;

public class LabelTable {
    private final MethodInput methodInput;
    private final BiMap<Label, MethodLabel> labelMap = HashBiMap.create();

    public LabelTable(MethodInput methodInput) {
        this.methodInput = methodInput;
    }

    public MethodInput getMethodInput() {
        return methodInput;
    }

    public MethodLabel getLabel(Label label) {
        int size = labelMap.size();
        return labelMap.computeIfAbsent(label, i -> new MethodLabel(this, "L".concat(String.valueOf(size))));
    }

    public MethodLabel getLabel(String name) {
        for (MethodLabel label : labelMap.values()) {
            if (label.getName().equals(name)) {
                return label;
            }
        }
        return null;
    }

    public Set<MethodLabel> getLabels() {
        return labelMap.values();
    }

    public Label getOriginal(MethodLabel label) {
        return labelMap.inverse().get(label);
    }
}
