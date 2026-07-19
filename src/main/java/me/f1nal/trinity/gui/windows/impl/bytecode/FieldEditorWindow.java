package me.f1nal.trinity.gui.windows.impl.bytecode;

import imgui.ImGui;
import imgui.type.ImInt;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MemberDetails;
import org.objectweb.asm.tree.FieldNode;

public final class FieldEditorWindow extends AbstractBytecodeEditorWindow {
    private static final String[] CONSTANT_TYPES = {"None", "Integer", "Long", "Float", "Double", "String"};

    private final ClassInput owner;
    private final FieldInput input;
    private final FieldNode node;
    private final String originalName;
    private final String originalDescriptor;
    private final BytecodeEditorSupport.AccessEditor access;
    private final ImString name;
    private final ImString descriptor;
    private final BytecodeEditorSupport.NullableText signature;
    private final ImInt constantType = new ImInt();
    private final ImString constantValue;
    private final BytecodeEditorSupport.AnnotationListEditor visibleAnnotations;
    private final BytecodeEditorSupport.AnnotationListEditor invisibleAnnotations;
    private final BytecodeEditorSupport.TypeAnnotationListEditor visibleTypeAnnotations;
    private final BytecodeEditorSupport.TypeAnnotationListEditor invisibleTypeAnnotations;
    private final BytecodeEditorSupport.AttributeListEditor attributes;

    public FieldEditorWindow(Trinity trinity, ClassInput owner) {
        this(trinity, owner, null, defaultField());
    }

    public FieldEditorWindow(Trinity trinity, FieldInput input) {
        this(trinity, input.getOwningClass(), input, input.getNode());
    }

    private FieldEditorWindow(Trinity trinity, ClassInput owner, FieldInput input, FieldNode node) {
        super(input == null ? "Add Field" : "Edit Field: " + input.getDisplayName().getName(), trinity);
        this.owner = owner;
        this.input = input;
        this.node = node;
        this.originalName = node.name;
        this.originalDescriptor = node.desc;
        this.access = new BytecodeEditorSupport.AccessEditor(node.access, AccessFlags.Flag::isFieldFlag);
        this.name = BytecodeEditorSupport.text(node.name);
        this.descriptor = BytecodeEditorSupport.text(node.desc);
        this.signature = new BytecodeEditorSupport.NullableText(node.signature);
        this.constantValue = BytecodeEditorSupport.text(node.value == null ? "" : String.valueOf(node.value));
        this.constantType.set(constantType(node.value));
        this.visibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Visible annotations", node.visibleAnnotations);
        this.invisibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Invisible annotations", node.invisibleAnnotations);
        this.visibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Visible type annotations", node.visibleTypeAnnotations);
        this.invisibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Invisible type annotations", node.invisibleTypeAnnotations);
        this.attributes = new BytecodeEditorSupport.AttributeListEditor("Field attributes", node.attrs);
    }

    @Override
    protected void drawEditor() {
        if (ImGui.beginTabBar(getId("FieldTabs"))) {
            if (ImGui.beginTabItem("General")) {
                ImGui.inputText("Name", name);
                ImGui.inputText("Descriptor", descriptor);
                signature.draw("Generic signature");
                ImGui.separator();
                ImGui.text("Access flags");
                access.draw();
                ImGui.separator();
                ImGui.combo("Constant type", constantType, CONSTANT_TYPES);
                if (constantType.get() == 0) {
                    ImGui.beginDisabled();
                }
                ImGui.inputText("Constant value", constantValue);
                if (constantType.get() == 0) {
                    ImGui.endDisabled();
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
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
    }

    @Override
    protected String stateFingerprint() {
        return BytecodeEditorSupport.stateFingerprint(
                access, name, descriptor, signature, constantType, constantValue,
                visibleAnnotations, invisibleAnnotations, visibleTypeAnnotations,
                invisibleTypeAnnotations, attributes);
    }

    @Override
    protected void saveChanges() {
        String newName = name.get().trim();
        String newDescriptor = descriptor.get().trim();
        BytecodeEditorSupport.requireMemberName(newName, "Field name", false);
        BytecodeEditorSupport.requireFieldDescriptor(newDescriptor);

        FieldInput conflict = owner.getField(newName, newDescriptor);
        if (conflict != null && conflict != input) {
            throw new IllegalArgumentException("A field with this name and descriptor already exists");
        }

        node.access = access.get();
        node.name = newName;
        node.desc = newDescriptor;
        node.signature = signature.get();
        node.value = parseConstant(newDescriptor);
        node.visibleAnnotations = visibleAnnotations.get();
        node.invisibleAnnotations = invisibleAnnotations.get();
        node.visibleTypeAnnotations = visibleTypeAnnotations.get();
        node.invisibleTypeAnnotations = invisibleTypeAnnotations.get();
        node.attrs = attributes.get();

        if (input == null) {
            owner.addField(node);
            trinity.getEventManager().postEvent(new EventClassModified(owner));
        } else {
            MemberDetails previousDetails = input.getDetails();
            FieldInput savedInput = input;
            if (!originalName.equals(newName) || !originalDescriptor.equals(newDescriptor)) {
                savedInput = owner.reindexField(input);
            }
            trinity.getEventManager().postEvent(new EventMemberModified(savedInput, previousDetails));
        }
    }

    private Object parseConstant(String fieldDescriptor) {
        String value = constantValue.get();
        try {
            Object constant = switch (constantType.get()) {
                case 0 -> null;
                case 1 -> Integer.decode(value);
                case 2 -> Long.decode(value);
                case 3 -> Float.parseFloat(value);
                case 4 -> Double.parseDouble(value);
                case 5 -> value;
                default -> throw new IllegalArgumentException("Unknown constant type");
            };
            if (constant != null && !constantMatches(fieldDescriptor, constant)) {
                throw new IllegalArgumentException("Constant type does not match field descriptor " + fieldDescriptor);
            }
            return constant;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + CONSTANT_TYPES[constantType.get()].toLowerCase() + " constant");
        }
    }

    private static boolean constantMatches(String descriptor, Object value) {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> value instanceof Integer;
            case "J" -> value instanceof Long;
            case "F" -> value instanceof Float;
            case "D" -> value instanceof Double;
            case "Ljava/lang/String;" -> value instanceof String;
            default -> false;
        };
    }

    private static int constantType(Object value) {
        if (value instanceof Integer) return 1;
        if (value instanceof Long) return 2;
        if (value instanceof Float) return 3;
        if (value instanceof Double) return 4;
        if (value instanceof String) return 5;
        return 0;
    }

    private static FieldNode defaultField() {
        return new FieldNode(org.objectweb.asm.Opcodes.ACC_PUBLIC, "newField", "I", null, null);
    }
}
