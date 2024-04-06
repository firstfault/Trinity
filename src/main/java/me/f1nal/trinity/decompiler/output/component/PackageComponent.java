package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.decompiler.output.effect.TooltipEffect;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.theme.CodeColorScheme;

public class PackageComponent extends AbstractTextComponent {
    private final ClassInput classInput;

    public PackageComponent(ClassInput classInput) {
        super("pkg");

        this.addEffect(new TooltipEffect(() -> {
            Package pkg = classInput.getClassTarget().getPackage();

            return ColoredStringBuilder.create().
                    text(CodeColorScheme.PACKAGE, pkg.getPrettyPath()).newline().
                    fmt(CodeColorScheme.TEXT, CodeColorScheme.STRING, "{} files, {} directories", pkg.getEntries().size(), pkg.getPackages().size()).
                    get();
        }));

        this.classInput = classInput;
    }

    @Override
    public boolean handleItemHover() {
        return false;
    }

    @Override
    public String getText() {
        if (classInput.getClassTarget().getPackage().getParent() == null) {
            return "";
        }
        return String.format("%s;\n\n", classInput.getClassTarget().getPackage().getPrettyPath());
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.PACKAGE;
    }
}
