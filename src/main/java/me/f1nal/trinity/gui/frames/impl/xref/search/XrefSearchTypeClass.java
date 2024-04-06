package me.f1nal.trinity.gui.frames.impl.xref.search;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.ClassSelectComponent;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilderClassRef;

public class XrefSearchTypeClass extends XrefSearchType {
    private final ClassSelectComponent classSelectComponent = new ClassSelectComponent(trinity, (target) -> true);

    protected XrefSearchTypeClass(Trinity trinity) {
        super("Class", trinity);
    }

    @Override
    public boolean draw() {
        classSelectComponent.draw();
        return !this.classSelectComponent.getClassName().isEmpty();
    }

    @Override
    public XrefBuilder search() {
        return new XrefBuilderClassRef(trinity.getExecution().getXrefMap(), this.classSelectComponent.getClassName());
    }

    public ClassSelectComponent getClassSelectComponent() {
        return classSelectComponent;
    }
}
