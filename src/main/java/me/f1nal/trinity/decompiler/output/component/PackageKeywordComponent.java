package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.execution.ClassTarget;

public class PackageKeywordComponent extends KeywordComponent {
    private final ClassTarget target;

    public PackageKeywordComponent(ClassTarget target) {
        super("package ");
        this.target = target;
    }

    @Override
    public String getText() {
        return target.getPackage().getParent() == null ? "" : super.getText();
    }
}
