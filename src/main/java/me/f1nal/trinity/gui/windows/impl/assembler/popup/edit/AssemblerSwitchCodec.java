package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.execution.labels.LabelTable;
import me.f1nal.trinity.execution.labels.MethodLabel;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class AssemblerSwitchCodec {
    private AssemblerSwitchCodec() {
    }

    static String format(TableSwitchInsnNode table, LabelTable labels) {
        return "min=" + table.min + ", max=" + table.max + ", default=" + name(table.dflt, labels)
                + ", labels=[" + String.join(", ", table.labels.stream().map(label -> name(label, labels)).toList()) + "]";
    }

    static String format(LookupSwitchInsnNode lookup, LabelTable labels) {
        List<String> cases = new ArrayList<>();
        for (int i = 0; i < Math.min(lookup.keys.size(), lookup.labels.size()); i++) {
            cases.add(lookup.keys.get(i) + ":" + name(lookup.labels.get(i), labels));
        }
        return "default=" + name(lookup.dflt, labels) + ", cases={" + String.join(", ", cases) + "}";
    }

    static TableData parseTable(String input, LabelTable labels, Function<MethodLabel, LabelNode> resolver) {
        List<String> fields = splitTopLevel(input, ',');
        if (fields.size() != 4) throw new IllegalArgumentException("Expected min=..., max=..., default=..., labels=[...]");
        int min = integer(field(fields.get(0), "min"));
        int max = integer(field(fields.get(1), "max"));
        LabelNode dflt = label(field(fields.get(2), "default"), labels, resolver);
        List<LabelNode> targets = labelList(field(fields.get(3), "labels"), labels, resolver);
        if (max < min || targets.size() != max - min + 1) {
            throw new IllegalArgumentException("Table range does not match label count");
        }
        return new TableData(min, max, dflt, targets);
    }

    static LookupData parseLookup(String input, LabelTable labels, Function<MethodLabel, LabelNode> resolver) {
        List<String> fields = splitTopLevel(input, ',');
        if (fields.size() != 2) throw new IllegalArgumentException("Expected default=..., cases={key:label, ...}");
        LabelNode dflt = label(field(fields.get(0), "default"), labels, resolver);
        String cases = field(fields.get(1), "cases").trim();
        if (!cases.startsWith("{") || !cases.endsWith("}")) throw new IllegalArgumentException("Expected cases={key:label, ...}");
        cases = cases.substring(1, cases.length() - 1).trim();
        List<Integer> keys = new ArrayList<>();
        List<LabelNode> targets = new ArrayList<>();
        if (!cases.isEmpty()) for (String entry : splitTopLevel(cases, ',')) {
            int colon = entry.indexOf(':');
            if (colon < 0) throw new IllegalArgumentException("Expected lookup entry key:label");
            keys.add(integer(entry.substring(0, colon).trim()));
            targets.add(label(entry.substring(colon + 1).trim(), labels, resolver));
        }
        for (int i = 1; i < keys.size(); i++) {
            if (keys.get(i) <= keys.get(i - 1)) throw new IllegalArgumentException("Lookup keys must be unique and ascending");
        }
        return new LookupData(dflt, keys, targets);
    }

    private static List<LabelNode> labelList(String input, LabelTable labels,
                                             Function<MethodLabel, LabelNode> resolver) {
        String value = input.trim();
        if (!value.startsWith("[") || !value.endsWith("]")) throw new IllegalArgumentException("Expected label list");
        value = value.substring(1, value.length() - 1).trim();
        List<LabelNode> output = new ArrayList<>();
        if (!value.isEmpty()) for (String name : splitTopLevel(value, ',')) output.add(label(name, labels, resolver));
        return output;
    }

    private static LabelNode label(String name, LabelTable labels, Function<MethodLabel, LabelNode> resolver) {
        MethodLabel label = labels.getLabel(name.trim());
        LabelNode node = label == null ? null : resolver.apply(label);
        if (node == null) throw new IllegalArgumentException("Unknown label " + name.trim());
        return node;
    }

    private static String name(LabelNode label, LabelTable labels) {
        return labels.getLabel(label.getLabel()).getName();
    }

    private static String field(String input, String name) {
        int equals = input.indexOf('=');
        if (equals < 0 || !input.substring(0, equals).trim().equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("Expected " + name + "=...");
        }
        return input.substring(equals + 1).trim();
    }

    private static int integer(String value) {
        try {
            return Integer.decode(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer " + value);
        }
    }

    private static List<String> splitTopLevel(String input, char separator) {
        List<String> output = new ArrayList<>();
        int start = 0, depth = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '[' || c == '{' || c == '(') depth++;
            else if (c == ']' || c == '}' || c == ')') depth--;
            else if (c == separator && depth == 0) {
                output.add(input.substring(start, i).trim());
                start = i + 1;
            }
        }
        output.add(input.substring(start).trim());
        return output;
    }

    record TableData(int min, int max, LabelNode dflt, List<LabelNode> labels) {
    }

    record LookupData(LabelNode dflt, List<Integer> keys, List<LabelNode> labels) {
    }
}
