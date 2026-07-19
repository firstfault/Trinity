package me.f1nal.trinity.gui.windows.impl.bytecode;

import imgui.ImGui;
import imgui.type.ImInt;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleOpenNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.ModuleRequireNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

public final class ClassEditorWindow extends AbstractBytecodeEditorWindow {
    private final ClassInput input;
    private final Package targetPackage;
    private final ClassNode node;
    private final ImInt version;
    private final BytecodeEditorSupport.AccessEditor access;
    private final ImString name;
    private final BytecodeEditorSupport.NullableText signature;
    private final BytecodeEditorSupport.NullableText superName;
    private final BytecodeEditorSupport.StringListEditor interfaces;
    private final BytecodeEditorSupport.NullableText sourceFile;
    private final BytecodeEditorSupport.NullableText sourceDebug;
    private final BytecodeEditorSupport.NullableText outerClass;
    private final BytecodeEditorSupport.NullableText outerMethod;
    private final BytecodeEditorSupport.NullableText outerMethodDescriptor;
    private final BytecodeEditorSupport.NullableText nestHost;
    private final BytecodeEditorSupport.StringListEditor nestMembers;
    private final BytecodeEditorSupport.StringListEditor permittedSubclasses;
    private final List<InnerClassState> innerClasses = new ArrayList<>();
    private final List<RecordComponentState> recordComponents = new ArrayList<>();
    private final BytecodeEditorSupport.AnnotationListEditor visibleAnnotations;
    private final BytecodeEditorSupport.AnnotationListEditor invisibleAnnotations;
    private final BytecodeEditorSupport.TypeAnnotationListEditor visibleTypeAnnotations;
    private final BytecodeEditorSupport.TypeAnnotationListEditor invisibleTypeAnnotations;
    private final BytecodeEditorSupport.AttributeListEditor attributes;
    private ModuleState module;

    public ClassEditorWindow(Trinity trinity, ClassInput input) {
        this(trinity, input, null, input.getNode());
    }

    public ClassEditorWindow(Trinity trinity, Package targetPackage) {
        this(trinity, null, targetPackage, defaultClass(targetPackage));
    }

    private ClassEditorWindow(Trinity trinity, ClassInput input, Package targetPackage, ClassNode node) {
        super(input == null ? "Add Class" : "Edit Class: " + input.getDisplaySimpleName(), trinity);
        this.input = input;
        this.targetPackage = targetPackage;
        this.node = node;
        this.version = new ImInt(node.version);
        this.access = new BytecodeEditorSupport.AccessEditor(node.access, AccessFlags.Flag::isClassFlag);
        this.name = BytecodeEditorSupport.text(node.name);
        this.signature = new BytecodeEditorSupport.NullableText(node.signature);
        this.superName = new BytecodeEditorSupport.NullableText(node.superName);
        this.interfaces = new BytecodeEditorSupport.StringListEditor("Interfaces", node.interfaces);
        this.sourceFile = new BytecodeEditorSupport.NullableText(node.sourceFile);
        this.sourceDebug = new BytecodeEditorSupport.NullableText(node.sourceDebug);
        this.outerClass = new BytecodeEditorSupport.NullableText(node.outerClass);
        this.outerMethod = new BytecodeEditorSupport.NullableText(node.outerMethod);
        this.outerMethodDescriptor = new BytecodeEditorSupport.NullableText(node.outerMethodDesc);
        this.nestHost = new BytecodeEditorSupport.NullableText(node.nestHostClass);
        this.nestMembers = new BytecodeEditorSupport.StringListEditor("Nest members", node.nestMembers);
        this.permittedSubclasses = new BytecodeEditorSupport.StringListEditor("Permitted subclasses", node.permittedSubclasses);
        if (node.innerClasses != null) {
            node.innerClasses.forEach(innerClass -> innerClasses.add(new InnerClassState(innerClass)));
        }
        if (node.recordComponents != null) {
            node.recordComponents.forEach(component -> recordComponents.add(new RecordComponentState(component)));
        }
        this.visibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Visible annotations", node.visibleAnnotations);
        this.invisibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Invisible annotations", node.invisibleAnnotations);
        this.visibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Visible type annotations", node.visibleTypeAnnotations);
        this.invisibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Invisible type annotations", node.invisibleTypeAnnotations);
        this.attributes = new BytecodeEditorSupport.AttributeListEditor("Class attributes", node.attrs);
        this.module = node.module == null ? null : new ModuleState(node.module);
    }

    @Override
    protected void drawEditor() {
        if (ImGui.beginTabBar(getId("ClassTabs"))) {
            if (ImGui.beginTabItem("General")) {
                ImGui.inputText("Internal name", name);
                ImGui.inputInt("Classfile version", version);
                signature.draw("Generic signature");
                superName.draw("Superclass");
                ImGui.separator();
                ImGui.text("Access flags");
                access.draw();
                interfaces.draw();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Source & owner")) {
                sourceFile.draw("Source file");
                sourceDebug.drawMultiline("Source debug extension", 150.F);
                outerClass.draw("Outer class");
                outerMethod.draw("Outer method");
                outerMethodDescriptor.draw("Outer method descriptor");
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Nest & sealing")) {
                nestHost.draw("Nest host");
                nestMembers.draw();
                permittedSubclasses.draw();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Inner classes")) {
                drawInnerClasses();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Records")) {
                drawRecordComponents();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Module")) {
                if (module == null) {
                    ImGui.textDisabled("No Module attribute");
                    if (ImGui.button(FontAwesomeIcons.Plus + " Add Module attribute")) {
                        module = new ModuleState(new ModuleNode("new.module", 0, null));
                    }
                } else {
                    module.draw();
                    if (ImGui.button(FontAwesomeIcons.Times + " Remove Module attribute")) {
                        module = null;
                    }
                }
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Annotations")) {
                visibleAnnotations.draw();
                invisibleAnnotations.draw();
                visibleTypeAnnotations.draw();
                invisibleTypeAnnotations.draw();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Attributes")) {
                attributes.draw();
                ImGui.text("Fields: " + node.fields.size());
                ImGui.text("Methods: " + node.methods.size());
                if (input != null) {
                    if (ImGui.button("Add Field")) {
                        BytecodeEditorLauncher.addField(input);
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Add Method")) {
                        BytecodeEditorLauncher.addMethod(input);
                    }
                }
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
    }

    @Override
    protected String stateFingerprint() {
        return BytecodeEditorSupport.stateFingerprint(
                version, access, name, signature, superName, interfaces,
                sourceFile, sourceDebug, outerClass, outerMethod, outerMethodDescriptor,
                nestHost, nestMembers, permittedSubclasses, innerClasses, recordComponents,
                visibleAnnotations, invisibleAnnotations, visibleTypeAnnotations,
                invisibleTypeAnnotations, attributes, module);
    }

    private void drawInnerClasses() {
        for (int i = 0; i < innerClasses.size(); i++) {
            InnerClassState innerClass = innerClasses.get(i);
            ImGui.pushID("InnerClass" + i);
            ImGui.inputText("Internal name", innerClass.name);
            innerClass.outerName.draw("Outer name");
            innerClass.innerName.draw("Inner simple name");
            ImGui.inputInt("Access mask", innerClass.access);
            if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove inner-class entry")) {
                innerClasses.remove(i--);
            }
            ImGui.separator();
            ImGui.popID();
        }
        if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add inner-class entry")) {
            innerClasses.add(new InnerClassState(new InnerClassNode("pkg/Outer$Inner", "pkg/Outer", "Inner", Opcodes.ACC_PUBLIC)));
        }
    }

    private void drawRecordComponents() {
        for (int i = 0; i < recordComponents.size(); i++) {
            RecordComponentState component = recordComponents.get(i);
            ImGui.pushID("RecordComponent" + i);
            ImGui.inputText("Name", component.name);
            ImGui.inputText("Descriptor", component.descriptor);
            component.signature.draw("Generic signature");
            component.visibleAnnotations.draw();
            component.invisibleAnnotations.draw();
            component.visibleTypeAnnotations.draw();
            component.invisibleTypeAnnotations.draw();
            component.attributes.draw();
            if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove record component")) {
                recordComponents.remove(i--);
            }
            ImGui.separator();
            ImGui.popID();
        }
        if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add record component")) {
            recordComponents.add(new RecordComponentState(new RecordComponentNode("component", "Ljava/lang/Object;", null)));
        }
    }

    @Override
    protected void saveChanges() {
        String newName = name.get().trim();
        BytecodeEditorSupport.requireInternalName(newName, "Class name");
        int majorVersion = version.get() & 0xFFFF;
        if (majorVersion < (Opcodes.V1_1 & 0xFFFF) || majorVersion > (Opcodes.V23 & 0xFFFF)) {
            throw new IllegalArgumentException("Unsupported classfile version: " + version.get());
        }
        if (superName.get() != null) {
            BytecodeEditorSupport.requireInternalName(superName.get(), "Superclass");
        }
        if (outerMethodDescriptor.get() != null) {
            BytecodeEditorSupport.requireMethodDescriptor(outerMethodDescriptor.get());
        }
        for (String interfaceName : interfaces.get()) {
            BytecodeEditorSupport.requireInternalName(interfaceName, "Interface");
        }

        node.version = version.get();
        node.access = access.get();
        node.signature = signature.get();
        node.superName = superName.get();
        node.interfaces = new ArrayList<>(interfaces.get());
        node.sourceFile = sourceFile.get();
        node.sourceDebug = sourceDebug.get();
        node.outerClass = outerClass.get();
        node.outerMethod = outerMethod.get();
        node.outerMethodDesc = outerMethodDescriptor.get();
        node.nestHostClass = nestHost.get();
        node.nestMembers = emptyToNull(nestMembers.get());
        node.permittedSubclasses = emptyToNull(permittedSubclasses.get());
        node.innerClasses = buildInnerClasses();
        node.recordComponents = buildRecordComponents();
        node.module = module == null ? null : module.build();
        node.visibleAnnotations = visibleAnnotations.get();
        node.invisibleAnnotations = invisibleAnnotations.get();
        node.visibleTypeAnnotations = visibleTypeAnnotations.get();
        node.invisibleTypeAnnotations = invisibleTypeAnnotations.get();
        node.attrs = attributes.get();

        if (input == null) {
            node.name = newName;
            trinity.getExecution().createClass(targetPackage, node);
        } else {
            trinity.getExecution().reindexClass(input, newName);
            input.getClassTarget().resetKind();
            trinity.getEventManager().postEvent(new EventClassModified(input));
        }
    }

    private List<InnerClassNode> buildInnerClasses() {
        List<InnerClassNode> output = new ArrayList<>();
        for (InnerClassState state : innerClasses) {
            BytecodeEditorSupport.requireInternalName(state.name.get(), "Inner class name");
            output.add(new InnerClassNode(state.name.get(), state.outerName.get(), state.innerName.get(), state.access.get()));
        }
        return output;
    }

    private List<RecordComponentNode> buildRecordComponents() {
        if (recordComponents.isEmpty()) {
            return null;
        }
        List<RecordComponentNode> output = new ArrayList<>();
        for (RecordComponentState state : recordComponents) {
            BytecodeEditorSupport.requireName(state.name.get(), "Record component name");
            BytecodeEditorSupport.requireFieldDescriptor(state.descriptor.get());
            state.node.name = state.name.get();
            state.node.descriptor = state.descriptor.get();
            state.node.signature = state.signature.get();
            state.node.visibleAnnotations = state.visibleAnnotations.get();
            state.node.invisibleAnnotations = state.invisibleAnnotations.get();
            state.node.visibleTypeAnnotations = state.visibleTypeAnnotations.get();
            state.node.invisibleTypeAnnotations = state.invisibleTypeAnnotations.get();
            state.node.attrs = state.attributes.get();
            output.add(state.node);
        }
        return output;
    }

    private static <T> List<T> emptyToNull(List<T> values) {
        return values.isEmpty() ? null : new ArrayList<>(values);
    }

    private static ClassNode defaultClass(Package targetPackage) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        node.name = targetPackage.getChildrenPath("NewClass");
        node.superName = "java/lang/Object";

        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 1;
        constructor.maxLocals = 1;
        node.methods.add(constructor);
        return node;
    }

    private static final class InnerClassState {
        private final ImString name;
        private final BytecodeEditorSupport.NullableText outerName;
        private final BytecodeEditorSupport.NullableText innerName;
        private final ImInt access;

        private InnerClassState(InnerClassNode node) {
            this.name = BytecodeEditorSupport.text(node.name);
            this.outerName = new BytecodeEditorSupport.NullableText(node.outerName);
            this.innerName = new BytecodeEditorSupport.NullableText(node.innerName);
            this.access = new ImInt(node.access);
        }
    }

    private static final class RecordComponentState {
        private final RecordComponentNode node;
        private final ImString name;
        private final ImString descriptor;
        private final BytecodeEditorSupport.NullableText signature;
        private final BytecodeEditorSupport.AnnotationListEditor visibleAnnotations;
        private final BytecodeEditorSupport.AnnotationListEditor invisibleAnnotations;
        private final BytecodeEditorSupport.TypeAnnotationListEditor visibleTypeAnnotations;
        private final BytecodeEditorSupport.TypeAnnotationListEditor invisibleTypeAnnotations;
        private final BytecodeEditorSupport.AttributeListEditor attributes;

        private RecordComponentState(RecordComponentNode node) {
            this.node = node;
            this.name = BytecodeEditorSupport.text(node.name);
            this.descriptor = BytecodeEditorSupport.text(node.descriptor);
            this.signature = new BytecodeEditorSupport.NullableText(node.signature);
            this.visibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Visible annotations", node.visibleAnnotations);
            this.invisibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Invisible annotations", node.invisibleAnnotations);
            this.visibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Visible type annotations", node.visibleTypeAnnotations);
            this.invisibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Invisible type annotations", node.invisibleTypeAnnotations);
            this.attributes = new BytecodeEditorSupport.AttributeListEditor("Record component attributes", node.attrs);
        }
    }

    private static final class ModuleState {
        private final ImString name;
        private final ImInt access;
        private final BytecodeEditorSupport.NullableText version;
        private final BytecodeEditorSupport.NullableText mainClass;
        private final BytecodeEditorSupport.StringListEditor packages;
        private final BytecodeEditorSupport.StringListEditor uses;
        private final List<RequireState> requires = new ArrayList<>();
        private final List<ExportState> exports = new ArrayList<>();
        private final List<ExportState> opens = new ArrayList<>();
        private final List<ProvideState> provides = new ArrayList<>();

        private ModuleState(ModuleNode node) {
            this.name = BytecodeEditorSupport.text(node.name);
            this.access = new ImInt(node.access);
            this.version = new BytecodeEditorSupport.NullableText(node.version);
            this.mainClass = new BytecodeEditorSupport.NullableText(node.mainClass);
            this.packages = new BytecodeEditorSupport.StringListEditor("Module packages", node.packages);
            this.uses = new BytecodeEditorSupport.StringListEditor("Used services", node.uses);
            if (node.requires != null) node.requires.forEach(require -> requires.add(new RequireState(require)));
            if (node.exports != null) node.exports.forEach(export -> exports.add(new ExportState(export.packaze, export.access, export.modules)));
            if (node.opens != null) node.opens.forEach(open -> opens.add(new ExportState(open.packaze, open.access, open.modules)));
            if (node.provides != null) node.provides.forEach(provide -> provides.add(new ProvideState(provide)));
        }

        private void draw() {
            ImGui.inputText("Module name", name);
            ImGui.inputInt("Module access mask", access);
            version.draw("Module version");
            mainClass.draw("Main class");
            packages.draw();
            uses.draw();
            drawRequires();
            drawExportEntries("Exports", exports);
            drawExportEntries("Opens", opens);
            drawProvides();
        }

        private void drawRequires() {
            if (!ImGui.collapsingHeader("Requires (" + requires.size() + ")###Requires")) return;
            for (int i = 0; i < requires.size(); i++) {
                RequireState state = requires.get(i);
                ImGui.pushID("Require" + i);
                ImGui.inputText("Module", state.module);
                ImGui.inputInt("Access", state.access);
                state.version.draw("Version");
                if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove")) requires.remove(i--);
                ImGui.separator();
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add require")) {
                requires.add(new RequireState(new ModuleRequireNode("module.name", 0, null)));
            }
        }

        private void drawExportEntries(String label, List<ExportState> values) {
            if (!ImGui.collapsingHeader(label + " (" + values.size() + ")###" + label)) return;
            for (int i = 0; i < values.size(); i++) {
                ExportState state = values.get(i);
                ImGui.pushID(label + i);
                ImGui.inputText("Package", state.packaze);
                ImGui.inputInt("Access", state.access);
                state.modules.draw();
                if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove")) values.remove(i--);
                ImGui.separator();
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add " + label.toLowerCase())) {
                values.add(new ExportState("package/name", 0, null));
            }
        }

        private void drawProvides() {
            if (!ImGui.collapsingHeader("Provides (" + provides.size() + ")###Provides")) return;
            for (int i = 0; i < provides.size(); i++) {
                ProvideState state = provides.get(i);
                ImGui.pushID("Provide" + i);
                ImGui.inputText("Service", state.service);
                state.providers.draw();
                if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove")) provides.remove(i--);
                ImGui.separator();
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add provide")) {
                provides.add(new ProvideState(new ModuleProvideNode("service/Type", List.of("provider/Type"))));
            }
        }

        private ModuleNode build() {
            BytecodeEditorSupport.requireName(name.get(), "Module name");
            ModuleNode output = new ModuleNode(name.get(), access.get(), version.get());
            output.mainClass = mainClass.get();
            output.packages = emptyToNull(packages.get());
            output.uses = emptyToNull(uses.get());
            output.requires = emptyToNull(requires.stream().map(RequireState::build).toList());
            output.exports = emptyToNull(exports.stream().map(ExportState::buildExport).toList());
            output.opens = emptyToNull(opens.stream().map(ExportState::buildOpen).toList());
            output.provides = emptyToNull(provides.stream().map(ProvideState::build).toList());
            return output;
        }
    }

    private static final class RequireState {
        private final ImString module;
        private final ImInt access;
        private final BytecodeEditorSupport.NullableText version;

        private RequireState(ModuleRequireNode node) {
            this.module = BytecodeEditorSupport.text(node.module);
            this.access = new ImInt(node.access);
            this.version = new BytecodeEditorSupport.NullableText(node.version);
        }

        private ModuleRequireNode build() {
            return new ModuleRequireNode(module.get(), access.get(), version.get());
        }
    }

    private static final class ExportState {
        private final ImString packaze;
        private final ImInt access;
        private final BytecodeEditorSupport.StringListEditor modules;

        private ExportState(String packaze, int access, List<String> modules) {
            this.packaze = BytecodeEditorSupport.text(packaze);
            this.access = new ImInt(access);
            this.modules = new BytecodeEditorSupport.StringListEditor("Target modules", modules);
        }

        private ModuleExportNode buildExport() {
            return new ModuleExportNode(packaze.get(), access.get(), emptyToNull(modules.get()));
        }

        private ModuleOpenNode buildOpen() {
            return new ModuleOpenNode(packaze.get(), access.get(), emptyToNull(modules.get()));
        }
    }

    private static final class ProvideState {
        private final ImString service;
        private final BytecodeEditorSupport.StringListEditor providers;

        private ProvideState(ModuleProvideNode node) {
            this.service = BytecodeEditorSupport.text(node.service);
            this.providers = new BytecodeEditorSupport.StringListEditor("Providers", node.providers);
        }

        private ModuleProvideNode build() {
            return new ModuleProvideNode(service.get(), providers.get());
        }
    }
}
