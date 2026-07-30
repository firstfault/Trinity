package me.f1nal.trinity.gui.windows.impl.constant.search;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.Type;

import java.util.List;

/** Searches class, array, primitive, and method-type constants. */
public final class ConstantSearchTypeType extends ConstantSearchType {
    private final ImString query = new ImString(512);

    public ConstantSearchTypeType(Trinity trinity) {
        super("Class Literal / Type", trinity);
    }

    ConstantSearchTypeType(Trinity trinity, String query) {
        this(trinity);
        this.query.set(query);
    }

    @Override
    public boolean draw() {
        ImGui.inputTextWithHint("Type", "java/lang/String, Ljava/lang/String;, or (I)V", query);
        boolean valid = query.get().isBlank() || normalizeQuery(query.get()) != null;
        if (!valid) {
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR,
                    "Enter a class name or valid JVM type descriptor.");
        }
        return valid;
    }

    @Override
    public String getSearchDescription() {
        return query.get().isBlank() ? "All Class and Method Types"
                : "Type " + query.get().trim();
    }

    @Override
    public void populate(List<ConstantViewCache> list) {
        String descriptor = normalizeQuery(query.get());
        if (descriptor == null && !query.get().isBlank()) return;
        new LdcConstantSearcher<Type>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof Type type
                        && (descriptor == null || descriptor.equals(type.getDescriptor()));
            }

            @Override
            protected String convertConstantToText(Type value) {
                return format(value);
            }
        }.populate(list, getTrinity().getExecution());
    }

    boolean matches(Type type) {
        String descriptor = normalizeQuery(query.get());
        return (descriptor == null && query.get().isBlank())
                || (descriptor != null && descriptor.equals(type.getDescriptor()));
    }

    static String normalizeQuery(String input) {
        if (input == null || input.isBlank()) return null;
        String value = input.trim();
        if (value.endsWith(".class")) {
            value = value.substring(0, value.length() - ".class".length()).trim();
        }
        String primitive = switch (value) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "float" -> "F";
            case "long" -> "J";
            case "double" -> "D";
            default -> null;
        };
        if (primitive != null) return primitive;

        int dimensions = 0;
        while (value.endsWith("[]")) {
            dimensions++;
            value = value.substring(0, value.length() - 2).trim();
        }
        if (dimensions > 0) {
            String element = normalizeQuery(value);
            if (element == null || element.equals("V") || element.startsWith("(")) return null;
            return "[".repeat(dimensions) + element;
        }

        String normalized = value.replace('.', '/');
        try {
            if (normalized.startsWith("(")) {
                if (!isValidMethodDescriptor(normalized)) return null;
                return Type.getMethodType(normalized).getDescriptor();
            }
            if (normalized.startsWith("[")
                    || normalized.length() == 1
                    || normalized.startsWith("L") && normalized.endsWith(";")) {
                if (typeDescriptorEnd(normalized, 0, true) != normalized.length()) return null;
                return Type.getType(normalized).getDescriptor();
            }
            if (normalized.isEmpty()
                    || normalized.indexOf('(') >= 0
                    || normalized.indexOf(')') >= 0
                    || normalized.indexOf(';') >= 0
                    || normalized.indexOf('[') >= 0) {
                return null;
            }
            return Type.getObjectType(normalized).getDescriptor();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isValidMethodDescriptor(String descriptor) {
        int index = 1;
        while (index < descriptor.length() && descriptor.charAt(index) != ')') {
            index = typeDescriptorEnd(descriptor, index, false);
            if (index == -1) return false;
        }
        if (index >= descriptor.length() || descriptor.charAt(index) != ')') return false;
        int returnTypeEnd = typeDescriptorEnd(descriptor, index + 1, true);
        return returnTypeEnd == descriptor.length();
    }

    private static int typeDescriptorEnd(String descriptor, int start, boolean allowVoid) {
        if (start >= descriptor.length()) return -1;
        int index = start;
        while (descriptor.charAt(index) == '[') {
            index++;
            if (index >= descriptor.length()) return -1;
            allowVoid = false;
        }
        char type = descriptor.charAt(index);
        if (type == 'V') return allowVoid && index == start ? index + 1 : -1;
        if ("ZBCSIFJD".indexOf(type) >= 0) return index + 1;
        if (type != 'L') return -1;
        int end = descriptor.indexOf(';', index + 1);
        if (end == -1 || end == index + 1) return -1;
        String internalName = descriptor.substring(index + 1, end);
        return internalName.indexOf('[') >= 0
                || internalName.indexOf('.') >= 0
                ? -1 : end + 1;
    }

    static String format(Type type) {
        return type.getSort() == Type.METHOD
                ? "method-type " + type.getDescriptor()
                : type.getClassName() + ".class";
    }
}
