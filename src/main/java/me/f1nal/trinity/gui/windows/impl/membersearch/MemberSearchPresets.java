package me.f1nal.trinity.gui.windows.impl.membersearch;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.membersearch.MemberSearchQuery;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;

import java.util.Map;

/** Context-sensitive launchers for common reverse-engineering member queries. */
public final class MemberSearchPresets {
    private MemberSearchPresets() {
    }

    public static void addContextAction(Input<?> input, PopupItemBuilder builder) {
        if (input instanceof ClassInput classInput) {
            builder.menuItem("Find Subtypes...", () -> findSubtypes(classInput));
        } else if (input instanceof FieldInput fieldInput) {
            builder.menuItem("Find Fields of This Type...", () -> findFieldsOfType(fieldInput));
        } else if (input instanceof MethodInput methodInput) {
            builder.menuItem("Find Methods with This Signature...",
                    () -> findMethodsWithSignature(methodInput));
        }
    }

    public static void findSubtypes(ClassInput input) {
        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.CLASS,
                MemberSearchQuery.Scope.project(), MemberSearchQuery.Common.defaults(),
                new MemberSearchQuery.ClassCriteria(MemberSearchQuery.ClassKind.ANY,
                        new MemberSearchQuery.TypeCriterion(input.getRealName(),
                                MemberSearchQuery.TypeMode.ASSIGNABLE_TO),
                        MemberSearchQuery.HierarchyDepth.TRANSITIVE),
                MemberSearchQuery.FieldCriteria.defaults(), MemberSearchQuery.MethodCriteria.defaults());
        open(query);
    }

    public static void findFieldsOfType(FieldInput input) {
        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.FIELD,
                MemberSearchQuery.Scope.project(), MemberSearchQuery.Common.defaults(),
                MemberSearchQuery.ClassCriteria.defaults(),
                new MemberSearchQuery.FieldCriteria(new MemberSearchQuery.TypeCriterion(
                        input.getDescriptor(), MemberSearchQuery.TypeMode.EXACT)),
                MemberSearchQuery.MethodCriteria.defaults());
        open(query);
    }

    public static void findMethodsWithSignature(MethodInput input) {
        MemberSearchQuery.Common defaults = MemberSearchQuery.Common.defaults();
        MemberSearchQuery.Common common = new MemberSearchQuery.Common(
                new MemberSearchQuery.TextCriterion(input.getDetails().getName(),
                        MemberSearchQuery.TextMode.EXACT, true),
                defaults.visibility(), Map.of(), defaults.ownerKind(), defaults.declaringClass(),
                input.getDescriptor(), MemberSearchQuery.DescriptorMode.EXACT,
                defaults.genericType(), defaults.annotationType(), defaults.annotationLocation(),
                defaults.renameState(), defaults.referenceState(), defaults.referenceRange());
        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.METHOD,
                MemberSearchQuery.Scope.project(), common,
                MemberSearchQuery.ClassCriteria.defaults(), MemberSearchQuery.FieldCriteria.defaults(),
                MemberSearchQuery.MethodCriteria.defaults());
        open(query);
    }

    private static void open(MemberSearchQuery query) {
        MemberSearchFrame frame = Main.getWindowManager().addStaticWindow(MemberSearchFrame.class);
        frame.applyQuery(query);
        Main.getWindowManager().requestFocus(frame);
    }
}
