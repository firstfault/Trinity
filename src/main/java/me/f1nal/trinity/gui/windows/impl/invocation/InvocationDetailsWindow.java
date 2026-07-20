package me.f1nal.trinity.gui.windows.impl.invocation;

import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.impl.InvocationOutputMember;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.util.SystemUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InvocationDetailsWindow extends ClosableWindow {
    private final InvocationOutputMember invocation;
    private final MethodInput caller;
    private final MethodInput target;

    public InvocationDetailsWindow(Trinity trinity, InvocationOutputMember invocation, MethodInput caller, MethodInput target) {
        super("Invocation: " + invocation.getMethodName(), 720.F, 560.F, trinity);
        this.invocation = invocation;
        this.caller = caller;
        this.target = target;
        this.windowFlags |= ImGuiWindowFlags.HorizontalScrollbar;
        this.setCloseableByEscape(true);
    }

    @Override
    protected void renderFrame() {
        if (ImGui.button("Copy All Details")) {
            SystemUtil.copyToClipboard(buildCopyText());
        }
        if (target != null) {
            ImGui.sameLine();
            if (ImGui.button("View Target Method")) {
                Main.getDisplayManager().followDecompilerView(target, NavigationAction.FOLLOW_MEMBER);
            }
        }

        ImGui.separator();
        if (ImGui.beginChild(getId("InvocationDetails"), 0.F, 0.F, false, ImGuiWindowFlags.HorizontalScrollbar)) {
            drawSection("Call Site", getCallSiteDetails(), 1);
            drawSection("Target", getTargetDetails(), 2);
            drawSection("Receiver", getReceiverDetails(), 3);

            List<InvocationOutputMember.Argument> arguments = invocation.getArguments();
            if (arguments.isEmpty()) {
                drawSection("Arguments", List.of(new Detail("Count", "0")), 4);
            } else {
                for (int i = 0; i < arguments.size(); i++) {
                    InvocationOutputMember.Argument argument = arguments.get(i);
                    drawSection("Argument " + i, List.of(
                            new Detail("Expression", argument.getExpression()),
                            new Detail("Expression kind", argument.getExpressionKind()),
                            new Detail("Inferred type", formatType(argument.getInferredType())),
                            new Detail("Declared type", formatType(argument.getDeclaredType()))
                    ), 10 + i);
                }
            }

            List<String> bootstrapArguments = invocation.getBootstrapArguments();
            List<Detail> bootstrapDetails = new ArrayList<>();
            bootstrapDetails.add(new Detail("Count", String.valueOf(bootstrapArguments.size())));
            for (int i = 0; i < bootstrapArguments.size(); i++) {
                bootstrapDetails.add(new Detail("#" + i, bootstrapArguments.get(i)));
            }
            drawSection("Bootstrap Arguments", bootstrapDetails, 1000);
        }
        ImGui.endChild();
    }

    private void drawSection(String title, List<Detail> details, int id) {
        ImGui.text(title);
        if (ImGui.beginTable(getId("DetailsTable" + id), 2, ImGuiTableFlags.Borders | ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.Resizable)) {
            ImGui.tableSetupColumn("Field");
            ImGui.tableSetupColumn("Value");
            ImGui.tableHeadersRow();

            for (Detail detail : details) {
                ImGui.tableNextRow();
                ImGui.tableNextColumn();
                ImGui.textDisabled(detail.label());
                boolean copyRequested = ImGui.isItemClicked(ImGuiMouseButton.Right);
                ImGui.tableNextColumn();
                ImGui.textWrapped(detail.value());
                copyRequested |= ImGui.isItemClicked(ImGuiMouseButton.Right);
                if (copyRequested) {
                    showCopyPopup(detail);
                }
            }
            ImGui.endTable();
        }
        ImGui.spacing();
    }

    private void showCopyPopup(Detail detail) {
        Main.getDisplayManager().showPopup(PopupItemBuilder.create()
                .menuItem("Copy Name", () -> SystemUtil.copyToClipboard(detail.label()))
                .menuItem("Copy Value", () -> SystemUtil.copyToClipboard(detail.value())));
    }

    private List<Detail> getCallSiteDetails() {
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Caller", caller == null ? "<outside a decoded method>" : formatMethod(caller)));
        details.add(new Detail("Invocation opcode", invocation.getInvocationType()));
        details.add(new Detail("Function kind", invocation.getFunctionType()));
        details.add(new Detail("Static invocation", yesNo(invocation.isStaticInvocation())));
        details.add(new Detail("Expression ID", String.valueOf(invocation.getExpressionId())));
        details.add(new Detail("Bytecode offsets", invocation.getBytecodeOffsets().isEmpty()
                ? "<unavailable>"
                : invocation.getBytecodeOffsets().stream().map(String::valueOf).collect(Collectors.joining(", "))));
        details.add(new Detail("Dynamic class suffix", Objects.toString(invocation.getDynamicClassSuffix(), "<not dynamic>")));
        details.add(new Detail("Decompiler boxing call", yesNo(invocation.isBoxingCall())));
        details.add(new Detail("Decompiler unboxing call", yesNo(invocation.isUnboxingCall())));
        details.add(new Detail("Varargs target", yesNo(invocation.isVarArgsCall())));
        return details;
    }

    private List<Detail> getTargetDetails() {
        String descriptor = invocation.getMethodDescriptor();
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Owner (internal)", invocation.getOwnerName()));
        details.add(new Detail("Owner (Java)", invocation.getOwnerName().replace('/', '.')));
        details.add(new Detail("Name", invocation.getMethodName()));
        details.add(new Detail("JVM symbol", invocation.getOwnerName() + "." + invocation.getMethodName() + descriptor));
        details.add(new Detail("Descriptor", descriptor));
        details.add(new Detail("Return type", formatType(Type.getReturnType(descriptor).getDescriptor())));

        Type[] parameterTypes = Type.getArgumentTypes(descriptor);
        details.add(new Detail("Parameter count", String.valueOf(parameterTypes.length)));
        for (int i = 0; i < parameterTypes.length; i++) {
            details.add(new Detail("Parameter " + i, formatType(parameterTypes[i].getDescriptor())));
        }

        details.add(new Detail("Resolved in project", yesNo(target != null)));
        if (target == null) {
            return details;
        }

        MethodNode method = target.getNode();
        ClassInput owner = target.getOwningClass();
        ClassNode ownerNode = owner.getNode();
        details.add(new Detail("Resolved display name", target.getDisplayName().getName()));
        details.add(new Detail("Resolved owner", owner.getRealName()));
        details.add(new Detail("Access", formatAccess(target)));
        details.add(new Detail("Access mask", String.format("0x%04X", target.getAccessFlagsMask())));
        details.add(new Detail("Generic signature", Objects.toString(method.signature, "<none>")));
        details.add(new Detail("Declared exceptions", joinOrNone(method.exceptions)));
        details.add(new Detail("Instruction nodes", String.valueOf(method.instructions.size())));
        details.add(new Detail("Max stack", String.valueOf(method.maxStack)));
        details.add(new Detail("Max locals", String.valueOf(method.maxLocals)));
        details.add(new Detail("Try/catch blocks", String.valueOf(size(method.tryCatchBlocks))));
        details.add(new Detail("Local variable entries", String.valueOf(size(method.localVariables))));
        details.add(new Detail("Method parameters", String.valueOf(size(method.parameters))));
        details.add(new Detail("Visible annotations", annotationNames(method.visibleAnnotations)));
        details.add(new Detail("Invisible annotations", annotationNames(method.invisibleAnnotations)));
        details.add(new Detail("Visible type annotations", String.valueOf(size(method.visibleTypeAnnotations))));
        details.add(new Detail("Invisible type annotations", String.valueOf(size(method.invisibleTypeAnnotations))));
        details.add(new Detail("Custom attributes", String.valueOf(size(method.attrs))));
        details.add(new Detail("Annotation default", Objects.toString(method.annotationDefault, "<none>")));
        details.add(new Detail("Owner class access", formatAccess(owner)));
        details.add(new Detail("Owner class version", String.valueOf(ownerNode.version)));
        details.add(new Detail("Owner superclass", Objects.toString(ownerNode.superName, "<none>")));
        details.add(new Detail("Owner interfaces", joinOrNone(ownerNode.interfaces)));
        details.add(new Detail("Owner source file", Objects.toString(ownerNode.sourceFile, "<none>")));
        details.add(new Detail("Owner outer class", Objects.toString(ownerNode.outerClass, "<none>")));
        details.add(new Detail("Owner nest host", Objects.toString(ownerNode.nestHostClass, "<none>")));
        details.add(new Detail("Owner permitted subclasses", joinOrNone(ownerNode.permittedSubclasses)));
        return details;
    }

    private List<Detail> getReceiverDetails() {
        if (invocation.getReceiverExpression() == null) {
            return List.of(new Detail("Receiver", "<none; static or dynamic invocation>"));
        }
        return List.of(
                new Detail("Expression", invocation.getReceiverExpression()),
                new Detail("Expression kind", invocation.getReceiverKind()),
                new Detail("Inferred type", formatType(invocation.getReceiverType()))
        );
    }

    private String buildCopyText() {
        StringBuilder output = new StringBuilder();
        appendSection(output, "Call Site", getCallSiteDetails());
        appendSection(output, "Target", getTargetDetails());
        appendSection(output, "Receiver", getReceiverDetails());
        for (int i = 0; i < invocation.getArguments().size(); i++) {
            InvocationOutputMember.Argument argument = invocation.getArguments().get(i);
            appendSection(output, "Argument " + i, List.of(
                    new Detail("Expression", argument.getExpression()),
                    new Detail("Expression kind", argument.getExpressionKind()),
                    new Detail("Inferred type", formatType(argument.getInferredType())),
                    new Detail("Declared type", formatType(argument.getDeclaredType()))
            ));
        }
        List<Detail> bootstrap = new ArrayList<>();
        for (int i = 0; i < invocation.getBootstrapArguments().size(); i++) {
            bootstrap.add(new Detail("#" + i, invocation.getBootstrapArguments().get(i)));
        }
        appendSection(output, "Bootstrap Arguments", bootstrap);
        return output.toString().stripTrailing();
    }

    private static void appendSection(StringBuilder output, String title, List<Detail> details) {
        output.append(title).append('\n');
        for (Detail detail : details) {
            output.append(detail.label()).append(": ").append(detail.value()).append('\n');
        }
        output.append('\n');
    }

    private static String formatMethod(MethodInput method) {
        return method.getOwningClass().getRealName() + "." + method.getName() + method.getDescriptor();
    }

    private static String formatType(String descriptor) {
        if (descriptor == null) {
            return "<unknown>";
        }
        try {
            return Type.getType(descriptor).getClassName() + " (" + descriptor + ")";
        } catch (IllegalArgumentException ignored) {
            return descriptor;
        }
    }

    private static String formatAccess(me.f1nal.trinity.execution.Input<?> input) {
        List<String> flags = new ArrayList<>();
        for (AccessFlags.Flag flag : AccessFlags.getFlags()) {
            if (input.isAccessFlagValid(flag) && input.getAccessFlags().isFlag(flag)) {
                flags.add(flag.getName().toLowerCase());
            }
        }
        return flags.isEmpty() ? "<package-private/no flags>" : String.join(" ", flags);
    }

    private static String annotationNames(List<? extends AnnotationNode> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return "<none>";
        }
        return annotations.stream().map(annotation -> annotation.desc).collect(Collectors.joining(", "));
    }

    private static String joinOrNone(List<String> values) {
        return values == null || values.isEmpty() ? "<none>" : String.join(", ", values);
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private record Detail(String label, String value) {
    }
}
