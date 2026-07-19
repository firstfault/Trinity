package me.f1nal.trinity.gui.windows.impl.bytecode;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Predicate;

final class BytecodeEditorSupport {
    private BytecodeEditorSupport() {
    }

    static ImString text(String value) {
        String safeValue = value == null ? "" : value;
        return new ImString(safeValue, Math.max(256, safeValue.length() + 1024));
    }

    static String nullable(ImString value) {
        return value.get().isBlank() ? null : value.get();
    }

    static String stateFingerprint(Object... values) {
        StringBuilder output = new StringBuilder();
        IdentityHashMap<Object, Integer> visited = new IdentityHashMap<>();
        appendState(output, values, visited);
        return output.toString();
    }

    private static void appendState(StringBuilder output, Object value,
                                    IdentityHashMap<Object, Integer> visited) {
        if (value == null) {
            output.append('N');
            return;
        }
        if (value instanceof ImString string) {
            appendText(output, 'S', string.get());
            return;
        }
        if (value instanceof ImInt integer) {
            output.append('I').append(integer.get()).append(';');
            return;
        }
        if (value instanceof ImBoolean bool) {
            output.append(bool.get() ? "B1;" : "B0;");
            return;
        }
        if (value instanceof CharSequence text) {
            appendText(output, 'T', text.toString());
            return;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof Enum<?>) {
            appendText(output, 'V', value.getClass().getName() + ':' + value);
            return;
        }

        Integer reference = visited.get(value);
        if (reference != null) {
            output.append('R').append(reference).append(';');
            return;
        }
        visited.put(value, visited.size());

        if (value instanceof Collection<?> collection) {
            output.append('C').append(collection.size()).append('[');
            collection.forEach(item -> appendState(output, item, visited));
            output.append(']');
            return;
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            output.append('A').append(length).append('[');
            for (int i = 0; i < length; i++) {
                appendState(output, Array.get(value, i), visited);
            }
            output.append(']');
            return;
        }
        if (type.getPackageName().equals(BytecodeEditorSupport.class.getPackageName())) {
            output.append('O');
            appendText(output, 'K', type.getName());
            Field[] fields = type.getDeclaredFields();
            Arrays.sort(fields, Comparator.comparing(Field::getName));
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                appendText(output, 'F', field.getName());
                try {
                    field.setAccessible(true);
                    appendState(output, field.get(value), visited);
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    output.append('X');
                }
            }
            output.append(';');
            return;
        }
        output.append('Q').append(System.identityHashCode(value)).append(';');
    }

    private static void appendText(StringBuilder output, char kind, String value) {
        output.append(kind).append(value.length()).append(':').append(value).append(';');
    }

    static void requireName(String name, String label) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be empty");
        }
    }

    static void requireInternalName(String name, String label) {
        requireName(name, label);
        if (name.indexOf('.') != -1 || name.indexOf(';') != -1 || name.indexOf('[') != -1
                || name.startsWith("/") || name.endsWith("/") || name.contains("//")) {
            throw new IllegalArgumentException(label + " must be a slash-separated JVM internal name");
        }
    }

    static void requireMemberName(String name, String label, boolean method) {
        requireName(name, label);
        if (name.indexOf('.') != -1 || name.indexOf(';') != -1 || name.indexOf('[') != -1 || name.indexOf('/') != -1) {
            throw new IllegalArgumentException(label + " contains a character forbidden by the classfile format");
        }
        if (method && (name.indexOf('<') != -1 || name.indexOf('>') != -1)
                && !"<init>".equals(name) && !"<clinit>".equals(name)) {
            throw new IllegalArgumentException("Only <init> and <clinit> may use angle brackets in a method name");
        }
    }

    static void requireFieldDescriptor(String descriptor) {
        try {
            Type type = Type.getType(descriptor);
            if (type.getSort() == Type.METHOD || type.getSort() == Type.VOID) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid field descriptor: " + descriptor);
        }
    }

    static void requireMethodDescriptor(String descriptor) {
        try {
            Type.getMethodType(descriptor);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid method descriptor: " + descriptor);
        }
    }

    static final class AccessEditor {
        private final ImInt mask;
        private final Predicate<AccessFlags.Flag> validFlag;

        AccessEditor(int mask, Predicate<AccessFlags.Flag> validFlag) {
            this.mask = new ImInt(mask);
            this.validFlag = validFlag;
        }

        int get() {
            return mask.get();
        }

        void draw() {
            ImGui.inputInt("Raw access mask", mask, 1, 16);
            ImGui.sameLine();
            ImGui.textDisabled(String.format("0x%04X", mask.get()));

            int column = 0;
            for (AccessFlags.Flag flag : AccessFlags.getFlags()) {
                if (!validFlag.test(flag)) {
                    continue;
                }
                boolean set = (mask.get() & flag.getMask()) != 0;
                ImGui.pushStyleColor(ImGuiCol.Button, set ? 0xFF55AF46 : 0xFF3C3C3C);
                if (ImGui.smallButton(flag.getName() + "###Access" + flag.getName())) {
                    mask.set(set ? mask.get() & ~flag.getMask() : mask.get() | flag.getMask());
                }
                ImGui.popStyleColor();
                if (++column % 4 != 0) {
                    ImGui.sameLine();
                }
            }
        }
    }

    static final class NullableText {
        private final ImBoolean present;
        private final ImString value;

        NullableText(String value) {
            this.present = new ImBoolean(value != null);
            this.value = text(value);
        }

        String get() {
            return present.get() ? value.get() : null;
        }

        void draw(String label) {
            ImGui.checkbox("###Present" + label, present);
            ImGui.sameLine();
            if (!present.get()) {
                ImGui.beginDisabled();
            }
            ImGui.inputText(label, value);
            if (!present.get()) {
                ImGui.endDisabled();
            }
        }

        void drawMultiline(String label, float height) {
            ImGui.checkbox("###Present" + label, present);
            ImGui.sameLine();
            ImGui.text(label);
            if (!present.get()) ImGui.beginDisabled();
            ImGui.inputTextMultiline("###" + label, value, -1.F, height);
            if (!present.get()) ImGui.endDisabled();
        }
    }

    static final class StringListEditor {
        private final String label;
        private final List<ImString> values = new ArrayList<>();

        StringListEditor(String label, List<String> values) {
            this.label = label;
            if (values != null) {
                values.forEach(value -> this.values.add(text(value)));
            }
        }

        List<String> get() {
            List<String> output = new ArrayList<>(values.size());
            for (ImString value : values) {
                if (!value.get().isBlank()) {
                    output.add(value.get());
                }
            }
            return output;
        }

        void draw() {
            ImGui.text(label);
            for (int i = 0; i < values.size(); i++) {
                ImGui.pushID(label + i);
                ImGui.setNextItemWidth(Math.max(120.F, ImGui.getContentRegionAvailX() - 30.F));
                ImGui.inputText("###Value", values.get(i));
                ImGui.sameLine();
                if (ImGui.smallButton(FontAwesomeIcons.Times + "###Remove")) {
                    values.remove(i--);
                }
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add " + singular(label))) {
                values.add(text(""));
            }
        }

        private static String singular(String label) {
            return label.endsWith("s") ? label.substring(0, label.length() - 1) : label;
        }
    }

    static final class AnnotationListEditor {
        private final String label;
        private final List<AnnotationData> entries = new ArrayList<>();

        AnnotationListEditor(String label, List<AnnotationNode> annotations) {
            this.label = label;
            if (annotations != null) {
                annotations.forEach(annotation -> entries.add(new AnnotationData(annotation)));
            }
        }

        List<AnnotationNode> get() {
            if (entries.isEmpty()) {
                return null;
            }
            List<AnnotationNode> output = new ArrayList<>(entries.size());
            for (AnnotationData entry : entries) {
                if (!entry.descriptor.get().isBlank()) {
                    output.add(entry.build());
                }
            }
            return output.isEmpty() ? null : output;
        }

        void draw() {
            if (!ImGui.collapsingHeader(label + " (" + entries.size() + ")###AnnotationHeader" + label)) {
                return;
            }
            for (int i = 0; i < entries.size(); i++) {
                AnnotationData entry = entries.get(i);
                ImGui.pushID(label + i);
                ImGui.setNextItemWidth(Math.max(120.F, ImGui.getContentRegionAvailX() - 30.F));
                ImGui.inputText("###Descriptor", entry.descriptor);
                ImGui.sameLine();
                if (ImGui.smallButton(FontAwesomeIcons.Times + "###Remove")) {
                    entries.remove(i--);
                    ImGui.popID();
                    continue;
                }
                entry.drawValues();
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add annotation###AddAnnotation" + label)) {
                entries.add(new AnnotationData(new AnnotationNode("Lannotation/Type;")));
            }
        }
    }

    static final class TypeAnnotationListEditor {
        private final String label;
        private final List<TypeAnnotationEntry> entries = new ArrayList<>();

        TypeAnnotationListEditor(String label, List<TypeAnnotationNode> annotations) {
            this.label = label;
            if (annotations != null) {
                annotations.forEach(annotation -> entries.add(new TypeAnnotationEntry(annotation)));
            }
        }

        List<TypeAnnotationNode> get() {
            if (entries.isEmpty()) {
                return null;
            }
            List<TypeAnnotationNode> output = new ArrayList<>();
            for (TypeAnnotationEntry entry : entries) {
                if (entry.annotation.descriptor.get().isBlank()) {
                    continue;
                }
                TypePath path;
                try {
                    path = entry.path.get().isBlank() ? null : TypePath.fromString(entry.path.get());
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException("Invalid type path in " + label + ": " + entry.path.get());
                }
                TypeAnnotationNode annotation = new TypeAnnotationNode(entry.typeRef.get(), path, entry.annotation.descriptor.get());
                annotation.values = entry.annotation.buildValues();
                output.add(annotation);
            }
            return output.isEmpty() ? null : output;
        }

        void draw() {
            if (!ImGui.collapsingHeader(label + " (" + entries.size() + ")###TypeAnnotationHeader" + label)) {
                return;
            }
            for (int i = 0; i < entries.size(); i++) {
                TypeAnnotationEntry entry = entries.get(i);
                ImGui.pushID(label + i);
                ImGui.inputText("Descriptor", entry.annotation.descriptor);
                ImGui.inputInt("Type reference", entry.typeRef);
                ImGui.inputText("Type path", entry.path);
                entry.annotation.drawValues();
                if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove")) {
                    entries.remove(i--);
                }
                ImGui.separator();
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add type annotation###AddTypeAnnotation" + label)) {
                entries.add(new TypeAnnotationEntry(new TypeAnnotationNode(0, null, "Lannotation/Type;")));
            }
        }

        private static final class TypeAnnotationEntry {
            private final ImInt typeRef;
            private final ImString path;
            private final AnnotationData annotation;

            private TypeAnnotationEntry(TypeAnnotationNode annotation) {
                this.typeRef = new ImInt(annotation.typeRef);
                this.path = text(annotation.typePath == null ? "" : annotation.typePath.toString());
                this.annotation = new AnnotationData(annotation);
            }
        }
    }

    static final class ParameterAnnotationEditor {
        private final String label;
        private final List<AnnotationListEditor> parameters = new ArrayList<>();

        ParameterAnnotationEditor(String label, List<AnnotationNode>[] annotations) {
            this.label = label;
            if (annotations != null) {
                for (int i = 0; i < annotations.length; i++) {
                    parameters.add(new AnnotationListEditor("Parameter " + i, annotations[i]));
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<AnnotationNode>[] get() {
            if (parameters.isEmpty()) return null;
            List<AnnotationNode>[] output = (List<AnnotationNode>[]) new List<?>[parameters.size()];
            boolean any = false;
            for (int i = 0; i < parameters.size(); i++) {
                output[i] = parameters.get(i).get();
                any |= output[i] != null;
            }
            return any ? output : null;
        }

        int size() {
            return parameters.size();
        }

        void draw() {
            if (!ImGui.collapsingHeader(label + " (" + parameters.size() + " parameter slots)###ParameterAnnotationHeader" + label)) return;
            for (int i = 0; i < parameters.size(); i++) {
                ImGui.pushID(label + i);
                parameters.get(i).draw();
                if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove parameter annotation slot")) {
                    parameters.remove(i--);
                }
                ImGui.separator();
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add parameter annotation slot###AddParameterAnnotationSlot" + label)) {
                parameters.add(new AnnotationListEditor("Parameter " + parameters.size(), null));
            }
        }
    }

    static final class OptionalAnnotationValueEditor {
        private final ImBoolean present;
        private final AnnotationValue value;

        OptionalAnnotationValueEditor(Object value) {
            this.present = new ImBoolean(value != null);
            this.value = new AnnotationValue(value == null ? "" : value);
        }

        Object get() {
            return present.get() ? value.build() : null;
        }

        void draw(String label) {
            ImGui.checkbox("Has " + label.toLowerCase(), present);
            if (present.get()) value.draw();
        }
    }

    static final class AttributeListEditor {
        private final String label;
        private final List<AttributeData> attributes = new ArrayList<>();

        AttributeListEditor(String label, List<Attribute> attributes) {
            this.label = label;
            if (attributes != null) attributes.forEach(attribute -> this.attributes.add(new AttributeData(attribute)));
        }

        List<Attribute> get() {
            if (attributes.isEmpty()) return null;
            List<Attribute> output = new ArrayList<>(attributes.size());
            for (AttributeData attribute : attributes) {
                requireName(attribute.type.get(), "Attribute type");
                output.add(new RawAttribute(attribute.type.get(), parseHex(attribute.content.get()), attribute.code.get()));
            }
            return output;
        }

        void draw() {
            ImGui.textWrapped("Unknown/custom attribute payloads are editable as raw hexadecimal bytes.");
            for (int i = 0; i < attributes.size(); i++) {
                AttributeData attribute = attributes.get(i);
                ImGui.pushID(label + i);
                ImGui.inputText("Type", attribute.type);
                ImGui.checkbox("Code attribute", attribute.code);
                ImGui.inputTextMultiline("Payload (hex)", attribute.content, -1.F, 90.F);
                if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove attribute")) attributes.remove(i--);
                ImGui.separator();
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add attribute###" + label)) {
                attributes.add(new AttributeData("Attribute", new byte[0], false));
            }
        }

        private static byte[] parseHex(String text) {
            String normalized = text.replaceAll("[^0-9A-Fa-f]", "");
            if ((normalized.length() & 1) != 0) throw new IllegalArgumentException("Attribute hex payload has an odd number of digits");
            byte[] output = new byte[normalized.length() / 2];
            for (int i = 0; i < output.length; i++) {
                output[i] = (byte) Integer.parseInt(normalized.substring(i * 2, i * 2 + 2), 16);
            }
            return output;
        }

        private static String toHex(byte[] bytes) {
            StringBuilder output = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) output.append(String.format("%02X", value & 0xFF));
            return output.toString();
        }

        private static byte[] content(Attribute attribute) {
            try {
                java.lang.reflect.Field field = Attribute.class.getDeclaredField("content");
                field.setAccessible(true);
                byte[] content = (byte[]) field.get(attribute);
                return content == null ? new byte[0] : content.clone();
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return new byte[0];
            }
        }

        private static final class AttributeData {
            private final ImString type;
            private final ImString content;
            private final ImBoolean code;

            private AttributeData(Attribute attribute) {
                this(attribute.type, content(attribute), attribute.isCodeAttribute());
            }

            private AttributeData(String type, byte[] content, boolean code) {
                this.type = text(type);
                this.content = text(toHex(content));
                this.code = new ImBoolean(code);
            }
        }

        private static final class RawAttribute extends Attribute {
            private final byte[] content;
            private final boolean code;

            private RawAttribute(String type, byte[] content, boolean code) {
                super(type);
                this.content = content;
                this.code = code;
            }

            @Override
            public boolean isCodeAttribute() {
                return code;
            }

            @Override
            protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength,
                                       int maxStack, int maxLocals) {
                return new ByteVector(content.length).putByteArray(content, 0, content.length);
            }
        }
    }

    private static final class AnnotationData {
        private final ImString descriptor;
        private final List<NamedAnnotationValue> values = new ArrayList<>();

        private AnnotationData(AnnotationNode annotation) {
            this.descriptor = text(annotation.desc);
            if (annotation.values != null) {
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    values.add(new NamedAnnotationValue(String.valueOf(annotation.values.get(i)), annotation.values.get(i + 1)));
                }
            }
        }

        private AnnotationNode build() {
            AnnotationNode output = new AnnotationNode(descriptor.get());
            output.values = buildValues();
            return output;
        }

        private List<Object> buildValues() {
            if (values.isEmpty()) {
                return null;
            }
            List<Object> output = new ArrayList<>(values.size() * 2);
            for (NamedAnnotationValue value : values) {
                requireName(value.name.get(), "Annotation value name");
                output.add(value.name.get());
                output.add(value.value.build());
            }
            return output;
        }

        private void drawValues() {
            if (ImGui.treeNode("Values (" + values.size() + ")###Values")) {
                for (int i = 0; i < values.size(); i++) {
                    NamedAnnotationValue value = values.get(i);
                    ImGui.pushID("AnnotationValue" + i);
                    ImGui.inputText("Name", value.name);
                    value.value.draw();
                    if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove value")) {
                        values.remove(i--);
                    }
                    ImGui.separator();
                    ImGui.popID();
                }
                if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add value")) {
                    values.add(new NamedAnnotationValue("value", ""));
                }
                ImGui.treePop();
            }
        }
    }

    private static final class NamedAnnotationValue {
        private final ImString name;
        private final AnnotationValue value;

        private NamedAnnotationValue(String name, Object value) {
            this.name = text(name);
            this.value = new AnnotationValue(value);
        }
    }

    private static final class AnnotationValue {
        private static final String[] TYPES = {
                "String", "Byte", "Boolean", "Character", "Short", "Integer", "Long", "Float", "Double",
                "Type", "Enum", "Annotation", "Array"
        };

        private final ImInt type = new ImInt();
        private final ImString value;
        private final ImString secondary;
        private AnnotationData annotation;
        private final List<AnnotationValue> array = new ArrayList<>();

        private AnnotationValue(Object original) {
            int detectedType = detectType(original);
            this.type.set(detectedType);
            if (original instanceof Type asmType) {
                this.value = text(asmType.getDescriptor());
                this.secondary = text("");
            } else if (original instanceof String[] enumValue && enumValue.length >= 2) {
                this.value = text(enumValue[0]);
                this.secondary = text(enumValue[1]);
            } else if (original instanceof AnnotationNode annotationNode) {
                this.value = text("");
                this.secondary = text("");
                this.annotation = new AnnotationData(annotationNode);
            } else if (original instanceof List<?> list) {
                this.value = text("");
                this.secondary = text("");
                list.forEach(item -> array.add(new AnnotationValue(item)));
            } else if (original != null && original.getClass().isArray()) {
                this.value = text("");
                this.secondary = text("");
                int length = java.lang.reflect.Array.getLength(original);
                for (int i = 0; i < length; i++) {
                    array.add(new AnnotationValue(java.lang.reflect.Array.get(original, i)));
                }
            } else {
                this.value = text(original == null ? "" : String.valueOf(original));
                this.secondary = text("");
            }
        }

        private void draw() {
            ImGui.combo("Type", type, TYPES);
            switch (type.get()) {
                case 10 -> {
                    ImGui.inputText("Enum descriptor", value);
                    ImGui.inputText("Enum value", secondary);
                }
                case 11 -> {
                    if (annotation == null) {
                        annotation = new AnnotationData(new AnnotationNode("Lannotation/Nested;"));
                    }
                    ImGui.inputText("Annotation descriptor", annotation.descriptor);
                    annotation.drawValues();
                }
                case 12 -> drawArray();
                default -> ImGui.inputText("Value", value);
            }
        }

        private void drawArray() {
            if (ImGui.treeNode("Array elements (" + array.size() + ")###ArrayElements")) {
                for (int i = 0; i < array.size(); i++) {
                    ImGui.pushID("ArrayValue" + i);
                    array.get(i).draw();
                    if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove element")) {
                        array.remove(i--);
                    }
                    ImGui.separator();
                    ImGui.popID();
                }
                if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add element")) {
                    array.add(new AnnotationValue(""));
                }
                ImGui.treePop();
            }
        }

        private Object build() {
            try {
                return switch (type.get()) {
                    case 0 -> value.get();
                    case 1 -> Byte.valueOf(value.get());
                    case 2 -> Boolean.valueOf(value.get());
                    case 3 -> parseCharacter(value.get());
                    case 4 -> Short.valueOf(value.get());
                    case 5 -> Integer.decode(value.get());
                    case 6 -> Long.decode(value.get());
                    case 7 -> Float.valueOf(value.get());
                    case 8 -> Double.valueOf(value.get());
                    case 9 -> Type.getType(value.get());
                    case 10 -> new String[]{value.get(), secondary.get()};
                    case 11 -> {
                        if (annotation == null) annotation = new AnnotationData(new AnnotationNode("Lannotation/Nested;"));
                        yield annotation.build();
                    }
                    case 12 -> array.stream().map(AnnotationValue::build).toList();
                    default -> throw new IllegalArgumentException("Unknown annotation value type");
                };
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid " + TYPES[type.get()].toLowerCase() + " annotation value: " + value.get());
            }
        }

        private static Character parseCharacter(String value) {
            if (value.length() == 1) return value.charAt(0);
            return switch (value) {
                case "\\n" -> '\n';
                case "\\r" -> '\r';
                case "\\t" -> '\t';
                case "\\b" -> '\b';
                case "\\f" -> '\f';
                case "\\\\" -> '\\';
                default -> throw new IllegalArgumentException();
            };
        }

        private static int detectType(Object value) {
            if (value instanceof Byte) return 1;
            if (value instanceof Boolean) return 2;
            if (value instanceof Character) return 3;
            if (value instanceof Short) return 4;
            if (value instanceof Integer) return 5;
            if (value instanceof Long) return 6;
            if (value instanceof Float) return 7;
            if (value instanceof Double) return 8;
            if (value instanceof Type) return 9;
            if (value instanceof String[]) return 10;
            if (value instanceof AnnotationNode) return 11;
            if (value instanceof List<?> || value != null && value.getClass().isArray()) return 12;
            return 0;
        }
    }

}
