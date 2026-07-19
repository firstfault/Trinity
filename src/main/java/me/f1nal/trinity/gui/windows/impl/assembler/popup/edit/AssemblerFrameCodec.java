package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.execution.labels.LabelTable;
import me.f1nal.trinity.execution.labels.MethodLabel;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class AssemblerFrameCodec {
    private AssemblerFrameCodec() {
    }

    static String format(FrameNode frame, LabelTable labels) {
        return "type=" + typeName(frame.type) + ", locals=" + formatValues(frame.local, labels)
                + ", stack=" + formatValues(frame.stack, labels);
    }

    static FrameData parse(String input, LabelTable table, Function<MethodLabel, LabelNode> resolver) {
        List<String> fields = splitTopLevel(input, ',');
        if (fields.size() != 3) throw new IllegalArgumentException("Expected type=..., locals=[...], stack=[...]");
        String typeText = field(fields.get(0), "type");
        String localsText = field(fields.get(1), "locals");
        String stackText = field(fields.get(2), "stack");
        return new FrameData(parseType(typeText), parseValues(localsText, table, resolver),
                parseValues(stackText, table, resolver));
    }

    private static String field(String input, String name) {
        int equals = input.indexOf('=');
        if (equals < 0 || !input.substring(0, equals).trim().equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("Expected " + name + "=...");
        }
        return input.substring(equals + 1).trim();
    }

    private static String formatValues(List<Object> values, LabelTable labels) {
        if (values == null || values.isEmpty()) return "[]";
        return "[" + String.join(", ", values.stream().map(value -> formatValue(value, labels)).toList()) + "]";
    }

    private static String formatValue(Object value, LabelTable labels) {
        if (value instanceof Integer kind) {
            if (kind.equals(Opcodes.TOP)) return "TOP";
            if (kind.equals(Opcodes.INTEGER)) return "INTEGER";
            if (kind.equals(Opcodes.FLOAT)) return "FLOAT";
            if (kind.equals(Opcodes.DOUBLE)) return "DOUBLE";
            if (kind.equals(Opcodes.LONG)) return "LONG";
            if (kind.equals(Opcodes.NULL)) return "NULL";
            if (kind.equals(Opcodes.UNINITIALIZED_THIS)) return "UNINITIALIZED_THIS";
        }
        if (value instanceof String type) return "object(\"" + type.replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
        if (value instanceof LabelNode label) return "uninitialized(" + labels.getLabel(label.getLabel()).getName() + ")";
        throw new IllegalArgumentException("Unsupported frame value: " + value);
    }

    private static List<Object> parseValues(String input, LabelTable table, Function<MethodLabel, LabelNode> resolver) {
        String text = input.trim();
        if (!text.startsWith("[") || !text.endsWith("]")) throw new IllegalArgumentException("Expected frame value list");
        text = text.substring(1, text.length() - 1).trim();
        List<Object> values = new ArrayList<>();
        if (text.isEmpty()) return values;
        for (String token : splitTopLevel(text, ',')) {
            String value = token.trim();
            switch (value.toUpperCase(Locale.ROOT)) {
                case "TOP" -> values.add(Opcodes.TOP);
                case "INTEGER" -> values.add(Opcodes.INTEGER);
                case "FLOAT" -> values.add(Opcodes.FLOAT);
                case "DOUBLE" -> values.add(Opcodes.DOUBLE);
                case "LONG" -> values.add(Opcodes.LONG);
                case "NULL" -> values.add(Opcodes.NULL);
                case "UNINITIALIZED_THIS" -> values.add(Opcodes.UNINITIALIZED_THIS);
                default -> {
                    if (value.startsWith("object(\"") && value.endsWith("\")")) {
                        values.add(value.substring(8, value.length() - 2).replace("\\\"", "\"").replace("\\\\", "\\"));
                    } else if (value.startsWith("uninitialized(") && value.endsWith(")")) {
                        String name = value.substring(14, value.length() - 1).trim();
                        MethodLabel label = table.getLabel(name);
                        LabelNode node = label == null ? null : resolver.apply(label);
                        if (node == null) throw new IllegalArgumentException("Unknown label " + name);
                        values.add(node);
                    } else {
                        throw new IllegalArgumentException("Unknown frame value " + value);
                    }
                }
            }
        }
        return values;
    }

    private static List<String> splitTopLevel(String input, char separator) {
        List<String> output = new ArrayList<>();
        int start = 0, depth = 0;
        boolean quoted = false, escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
                continue;
            }
            if (c == '"') quoted = true;
            else if (c == '[' || c == '(' || c == '{') depth++;
            else if (c == ']' || c == ')' || c == '}') depth--;
            else if (c == separator && depth == 0) {
                output.add(input.substring(start, i).trim());
                start = i + 1;
            }
        }
        output.add(input.substring(start).trim());
        return output;
    }

    private static String typeName(int type) {
        return switch (type) {
            case Opcodes.F_NEW -> "F_NEW";
            case Opcodes.F_FULL -> "F_FULL";
            case Opcodes.F_APPEND -> "F_APPEND";
            case Opcodes.F_CHOP -> "F_CHOP";
            case Opcodes.F_SAME -> "F_SAME";
            case Opcodes.F_SAME1 -> "F_SAME1";
            default -> throw new IllegalArgumentException("Unknown frame type: " + type);
        };
    }

    private static int parseType(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "F_NEW" -> Opcodes.F_NEW;
            case "F_FULL" -> Opcodes.F_FULL;
            case "F_APPEND" -> Opcodes.F_APPEND;
            case "F_CHOP" -> Opcodes.F_CHOP;
            case "F_SAME" -> Opcodes.F_SAME;
            case "F_SAME1" -> Opcodes.F_SAME1;
            default -> throw new IllegalArgumentException("Unknown frame type " + type);
        };
    }

    record FrameData(int type, List<Object> locals, List<Object> stack) {
    }
}
