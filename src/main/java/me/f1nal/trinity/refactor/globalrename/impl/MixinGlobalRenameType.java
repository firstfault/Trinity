package me.f1nal.trinity.refactor.globalrename.impl;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.PackageSelectComponent;
import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.util.AnnotationUtil;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.NameUtil;
import me.f1nal.trinity.util.annotations.AnnotationDescriptor;
import imgui.ImGui;
import org.objectweb.asm.Type;

import java.util.List;

public class MixinGlobalRenameType extends GlobalRenameType {
    private final PackageSelectComponent packageSelect;

    public MixinGlobalRenameType() {
        super("Spongepowered Mixins", "Renames classes, methods and fields that use Mixin features by deduction from available injection type information inside annotations.");
        this.packageSelect = new PackageSelectComponent("com/example/mixins/");
    }

    @Override
    public void drawInputs() {
        ImGui.text("New Mixins Package");
        GuiUtil.informationTooltip("Package for the newly renamed classes to be put into.");
        packageSelect.draw();
    }

    @Override
    public void runRefactor(Execution execution, List<Rename> renames) {
        for (ClassInput classInput : execution.getClassList()) {
            this.renameClass(classInput, renames);

            for (MethodInput methodInput : classInput.getMethodMap().values()) {
                this.renameMethod(methodInput, renames);
            }
        }
    }

    private void renameMethod(MethodInput methodInput, List<Rename> renames) {
        AnnotationDescriptor annotation = AnnotationUtil.getAnnotation(methodInput.getNode().visibleAnnotations, "org/spongepowered/asm/mixin/injection/Inject");

        if (annotation == null) {
            return;
        }

        Object value = annotation.getValues().get("method");
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            Object descObj = ((List<?>) value).get(0);
            if (descObj instanceof String) {
                String desc = (String) descObj;
                int indexOf = Math.max(desc.indexOf('('), desc.indexOf('*'));
                if (indexOf != -1) {
                    desc = desc.substring(0, indexOf);
                }
                renames.add(new Rename(methodInput, desc));
            }
        }
    }

    private void renameClass(ClassInput classInput, List<Rename> renames) {
        AnnotationDescriptor annotation = AnnotationUtil.getAnnotation(classInput.getNode().invisibleAnnotations, "org/spongepowered/asm/mixin/Mixin");

        if (annotation == null) {
            return;
        }

        Object value = annotation.getValues().get("value");
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            Object typeObj = ((List<?>) value).get(0);
            if (typeObj instanceof Type type) {
                final String targetClassName = NameUtil.getSimpleName(NameUtil.internalToNormal(type.getInternalName()));
                final String newName = this.packageSelect.getClassInPackage(targetClassName + "Mixin");

                renames.add(new Rename(classInput, newName));
            }
        }
    }
}
