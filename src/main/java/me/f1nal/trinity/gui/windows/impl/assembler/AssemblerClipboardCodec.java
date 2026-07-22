package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.AssemblerValueCodec;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.OpcodeClasses;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Editable, line-oriented text codec for assembler instructions. */
public final class AssemblerClipboardCodec {
    private AssemblerClipboardCodec() {
    }

    public static String format(List<AbstractInsnNode> instructions, Function<LabelNode, String> labelNamer) {
        LabelNames labels = new LabelNames(labelNamer);
        for (AbstractInsnNode instruction : instructions) {
            if (instruction instanceof LabelNode label) labels.name(label);
            collectReferencedLabels(instruction).forEach(labels::name);
        }

        List<String> lines = new ArrayList<>(instructions.size());
        for (AbstractInsnNode instruction : instructions) {
            lines.add(formatInstruction(instruction, labels));
        }
        return String.join("\n", lines);
    }

    /** Formats one instruction with the same canonical syntax used by assembler copy/paste. */
    public static String formatInstruction(AbstractInsnNode instruction,
                                           Function<LabelNode, String> labelNamer) {
        LabelNames labels = new LabelNames(labelNamer);
        if (instruction instanceof LabelNode label) labels.name(label);
        collectReferencedLabels(instruction).forEach(labels::name);
        return formatInstruction(instruction, labels);
    }

    public static ParsedInstructions parse(String input, Function<String, LabelNode> existingLabelResolver) {
        List<SourceLine> sourceLines = sourceLines(input);
        Map<String, LabelNode> declared = new LinkedHashMap<>();
        Map<LabelNode, String> labelNames = new IdentityHashMap<>();

        for (SourceLine sourceLine : sourceLines) {
            List<String> tokens = sourceLine.tokens();
            if (!tokens.get(0).equalsIgnoreCase("label")) continue;
            requireCount(tokens, 2, sourceLine.number());
            String name = parseString(tokens.get(1), sourceLine.number());
            LabelNode label = new LabelNode();
            if (declared.putIfAbsent(name, label) != null) {
                throw lineError(sourceLine.number(), "Duplicate label " + AssemblerValueCodec.quote(name));
            }
            labelNames.put(label, name);
        }

        ParseLabels labels = new ParseLabels(declared, existingLabelResolver, labelNames);
        List<AbstractInsnNode> instructions = new ArrayList<>();
        for (SourceLine sourceLine : sourceLines) {
            try {
                AbstractInsnNode instruction = parseInstruction(sourceLine.tokens(), labels);
                if (instruction != null) instructions.add(instruction);
            } catch (IllegalArgumentException exception) {
                if (exception.getMessage() != null && exception.getMessage().startsWith("Line ")) throw exception;
                throw lineError(sourceLine.number(), exception.getMessage());
            } catch (RuntimeException exception) {
                throw lineError(sourceLine.number(), exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage());
            }
        }
        instructions.addAll(0, labels.createdExternalLabels());
        return new ParsedInstructions(List.copyOf(instructions), labelNames);
    }

    private static String formatInstruction(AbstractInsnNode instruction, LabelNames labels) {
        if (instruction instanceof LabelNode label) {
            return "label " + quote(labels.name(label));
        }
        if (instruction instanceof FrameNode frame) {
            return "frame " + frameTypeName(frame.type) + " " + formatFrameValues(frame.local, labels)
                    + " " + formatFrameValues(frame.stack, labels);
        }
        if (instruction instanceof LineNumberNode line) {
            return "line " + line.line + " " + quote(labels.name(line.start));
        }

        String opcode = opcodeName(instruction.getOpcode());
        if (instruction instanceof InsnNode) return opcode;
        if (instruction instanceof IntInsnNode integer) return opcode + " " + integer.operand;
        if (instruction instanceof VarInsnNode variable) return opcode + " " + variable.var;
        if (instruction instanceof TypeInsnNode type) return opcode + " " + quote(type.desc);
        if (instruction instanceof FieldInsnNode field) {
            return opcode + " " + quote(field.owner) + " " + quote(field.name) + " " + quote(field.desc);
        }
        if (instruction instanceof MethodInsnNode method) {
            return opcode + " " + quote(method.owner) + " " + quote(method.name) + " "
                    + quote(method.desc) + " " + method.itf;
        }
        if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            return opcode + " " + quote(dynamic.name) + " " + quote(dynamic.desc) + " "
                    + AssemblerValueCodec.format(dynamic.bsm) + " "
                    + AssemblerValueCodec.formatList(dynamic.bsmArgs);
        }
        if (instruction instanceof JumpInsnNode jump) return opcode + " " + quote(labels.name(jump.label));
        if (instruction instanceof LdcInsnNode ldc) return opcode + " " + AssemblerValueCodec.format(ldc.cst);
        if (instruction instanceof IincInsnNode increment) {
            return opcode + " " + increment.var + " " + increment.incr;
        }
        if (instruction instanceof TableSwitchInsnNode table) {
            StringBuilder output = new StringBuilder(opcode).append(' ').append(table.min).append(' ')
                    .append(table.max).append(' ').append(quote(labels.name(table.dflt)));
            for (LabelNode label : table.labels) output.append(' ').append(quote(labels.name(label)));
            return output.toString();
        }
        if (instruction instanceof LookupSwitchInsnNode lookup) {
            StringBuilder output = new StringBuilder(opcode).append(' ').append(quote(labels.name(lookup.dflt)));
            for (int i = 0; i < lookup.keys.size(); i++) {
                output.append(' ').append(lookup.keys.get(i)).append(' ')
                        .append(quote(labels.name(lookup.labels.get(i))));
            }
            return output.toString();
        }
        if (instruction instanceof MultiANewArrayInsnNode array) {
            return opcode + " " + quote(array.desc) + " " + array.dims;
        }
        throw new IllegalArgumentException("Unsupported instruction node " + instruction.getClass().getName());
    }

    private static AbstractInsnNode parseInstruction(List<String> tokens, ParseLabels labels) {
        String name = tokens.get(0).toLowerCase(Locale.ROOT);
        if (name.equals("label")) {
            requireCount(tokens, 2, -1);
            return labels.declared(parseString(tokens.get(1), -1));
        }
        if (name.equals("frame")) {
            requireCount(tokens, 4, -1);
            List<Object> locals = parseFrameValues(tokens.get(2), labels);
            List<Object> stack = parseFrameValues(tokens.get(3), labels);
            return new FrameNode(parseFrameType(tokens.get(1)), size(locals), array(locals), size(stack), array(stack));
        }
        if (name.equals("line")) {
            requireCount(tokens, 3, -1);
            return new LineNumberNode(integer(tokens.get(1)), labels.resolve(parseString(tokens.get(2), -1)));
        }

        Class<?> instructionClass = OpcodeClasses.getOpcodeClass(name);
        if (instructionClass == null) throw new IllegalArgumentException("Unknown opcode '" + tokens.get(0) + "'");
        int opcode = OpcodeClasses.getOpcodeIndex(name);
        if (instructionClass == InsnNode.class) {
            requireCount(tokens, 1, -1);
            return new InsnNode(opcode);
        }
        if (instructionClass == IntInsnNode.class) {
            requireCount(tokens, 2, -1);
            return new IntInsnNode(opcode, integer(tokens.get(1)));
        }
        if (instructionClass == VarInsnNode.class) {
            requireCount(tokens, 2, -1);
            return new VarInsnNode(opcode, integer(tokens.get(1)));
        }
        if (instructionClass == TypeInsnNode.class) {
            requireCount(tokens, 2, -1);
            return new TypeInsnNode(opcode, parseString(tokens.get(1), -1));
        }
        if (instructionClass == FieldInsnNode.class) {
            requireCount(tokens, 4, -1);
            return new FieldInsnNode(opcode, parseString(tokens.get(1), -1),
                    parseString(tokens.get(2), -1), parseString(tokens.get(3), -1));
        }
        if (instructionClass == MethodInsnNode.class) {
            requireCount(tokens, 5, -1);
            return new MethodInsnNode(opcode, parseString(tokens.get(1), -1),
                    parseString(tokens.get(2), -1), parseString(tokens.get(3), -1), bool(tokens.get(4)));
        }
        if (instructionClass == InvokeDynamicInsnNode.class) {
            requireCount(tokens, 5, -1);
            Handle bootstrap = AssemblerValueCodec.parseHandle(tokens.get(3));
            return new InvokeDynamicInsnNode(parseString(tokens.get(1), -1), parseString(tokens.get(2), -1),
                    bootstrap, AssemblerValueCodec.parseList(tokens.get(4)));
        }
        if (instructionClass == JumpInsnNode.class) {
            requireCount(tokens, 2, -1);
            return new JumpInsnNode(opcode, labels.resolve(parseString(tokens.get(1), -1)));
        }
        if (instructionClass == LdcInsnNode.class) {
            requireCount(tokens, 2, -1);
            return new LdcInsnNode(AssemblerValueCodec.parse(tokens.get(1)));
        }
        if (instructionClass == IincInsnNode.class) {
            requireCount(tokens, 3, -1);
            return new IincInsnNode(integer(tokens.get(1)), integer(tokens.get(2)));
        }
        if (instructionClass == TableSwitchInsnNode.class) {
            if (tokens.size() < 5) throw new IllegalArgumentException("Expected min, max, default, and labels");
            int min = integer(tokens.get(1));
            int max = integer(tokens.get(2));
            if (max < min) throw new IllegalArgumentException("Table switch maximum is below its minimum");
            int labelCount = max - min + 1;
            if (tokens.size() != 4 + labelCount) {
                throw new IllegalArgumentException("Table switch range requires " + labelCount + " labels");
            }
            LabelNode[] targets = new LabelNode[labelCount];
            for (int i = 0; i < targets.length; i++) {
                targets[i] = labels.resolve(parseString(tokens.get(i + 4), -1));
            }
            return new TableSwitchInsnNode(min, max, labels.resolve(parseString(tokens.get(3), -1)), targets);
        }
        if (instructionClass == LookupSwitchInsnNode.class) {
            if (tokens.size() < 2 || (tokens.size() - 2) % 2 != 0) {
                throw new IllegalArgumentException("Expected default followed by key/label pairs");
            }
            int count = (tokens.size() - 2) / 2;
            int[] keys = new int[count];
            LabelNode[] targets = new LabelNode[count];
            for (int i = 0; i < count; i++) {
                keys[i] = integer(tokens.get(2 + i * 2));
                targets[i] = labels.resolve(parseString(tokens.get(3 + i * 2), -1));
                if (i > 0 && keys[i] <= keys[i - 1]) {
                    throw new IllegalArgumentException("Lookup switch keys must be unique and ascending");
                }
            }
            return new LookupSwitchInsnNode(labels.resolve(parseString(tokens.get(1), -1)), keys, targets);
        }
        if (instructionClass == MultiANewArrayInsnNode.class) {
            requireCount(tokens, 3, -1);
            return new MultiANewArrayInsnNode(parseString(tokens.get(1), -1), integer(tokens.get(2)));
        }
        throw new IllegalArgumentException("Unsupported opcode '" + tokens.get(0) + "'");
    }

    private static List<LabelNode> collectReferencedLabels(AbstractInsnNode instruction) {
        Set<LabelNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<LabelNode> labels = new ArrayList<>();
        if (instruction instanceof JumpInsnNode jump) addLabel(labels, seen, jump.label);
        if (instruction instanceof TableSwitchInsnNode table) {
            addLabel(labels, seen, table.dflt);
            table.labels.forEach(label -> addLabel(labels, seen, label));
        }
        if (instruction instanceof LookupSwitchInsnNode lookup) {
            addLabel(labels, seen, lookup.dflt);
            lookup.labels.forEach(label -> addLabel(labels, seen, label));
        }
        if (instruction instanceof LineNumberNode line) addLabel(labels, seen, line.start);
        if (instruction instanceof FrameNode frame) {
            collectFrameLabels(frame.local, labels, seen);
            collectFrameLabels(frame.stack, labels, seen);
        }
        return labels;
    }

    private static void collectFrameLabels(List<Object> values, List<LabelNode> labels, Set<LabelNode> seen) {
        if (values == null) return;
        for (Object value : values) if (value instanceof LabelNode label) addLabel(labels, seen, label);
    }

    private static void addLabel(List<LabelNode> labels, Set<LabelNode> seen, LabelNode label) {
        if (seen.add(label)) labels.add(label);
    }

    private static String formatFrameValues(List<Object> values, LabelNames labels) {
        if (values == null) return "null";
        return "[" + String.join(", ", values.stream().map(value -> formatFrameValue(value, labels)).toList()) + "]";
    }

    private static String formatFrameValue(Object value, LabelNames labels) {
        if (value == null) return "CHOP";
        if (value instanceof Integer kind) {
            if (kind.equals(Opcodes.TOP)) return "TOP";
            if (kind.equals(Opcodes.INTEGER)) return "INTEGER";
            if (kind.equals(Opcodes.FLOAT)) return "FLOAT";
            if (kind.equals(Opcodes.DOUBLE)) return "DOUBLE";
            if (kind.equals(Opcodes.LONG)) return "LONG";
            if (kind.equals(Opcodes.NULL)) return "NULL";
            if (kind.equals(Opcodes.UNINITIALIZED_THIS)) return "UNINITIALIZED_THIS";
        }
        if (value instanceof String type) return "object(" + quote(type) + ")";
        if (value instanceof LabelNode label) return "uninitialized(" + quote(labels.name(label)) + ")";
        throw new IllegalArgumentException("Unsupported frame value " + value);
    }

    private static List<Object> parseFrameValues(String input, ParseLabels labels) {
        if (input.equalsIgnoreCase("null")) return null;
        String body = listBody(input);
        if (body.isBlank()) return new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (String token : splitTopLevel(body, ',')) {
            String value = token.trim();
            switch (value.toUpperCase(Locale.ROOT)) {
                case "CHOP" -> values.add(null);
                case "TOP" -> values.add(Opcodes.TOP);
                case "INTEGER" -> values.add(Opcodes.INTEGER);
                case "FLOAT" -> values.add(Opcodes.FLOAT);
                case "DOUBLE" -> values.add(Opcodes.DOUBLE);
                case "LONG" -> values.add(Opcodes.LONG);
                case "NULL" -> values.add(Opcodes.NULL);
                case "UNINITIALIZED_THIS" -> values.add(Opcodes.UNINITIALIZED_THIS);
                default -> {
                    if (function(value, "object")) {
                        values.add(parseString(functionBody(value, "object"), -1));
                    } else if (function(value, "uninitialized")) {
                        values.add(labels.resolve(parseString(functionBody(value, "uninitialized"), -1)));
                    } else {
                        throw new IllegalArgumentException("Unknown frame value '" + value + "'");
                    }
                }
            }
        }
        return values;
    }

    private static List<SourceLine> sourceLines(String input) {
        List<SourceLine> output = new ArrayList<>();
        String[] lines = (input == null ? "" : input).split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            List<String> tokens;
            try {
                tokens = tokenize(line);
            } catch (IllegalArgumentException exception) {
                throw lineError(i + 1, exception.getMessage());
            }
            if (!tokens.isEmpty()) output.add(new SourceLine(i + 1, tokens));
        }
        return output;
    }

    /** Splits one assembler source line while preserving quoted and structured operands. */
    public static List<String> tokenize(String input) {
        List<String> output = new ArrayList<>();
        int start = -1;
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (start == -1) {
                if (Character.isWhitespace(character)) continue;
                start = i;
            }
            if (quoted) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') quoted = false;
                continue;
            }
            if (character == '"') quoted = true;
            else if (character == '(' || character == '[' || character == '{') depth++;
            else if (character == ')' || character == ']' || character == '}') {
                if (--depth < 0) throw new IllegalArgumentException("Unexpected closing delimiter");
            } else if (Character.isWhitespace(character) && depth == 0) {
                output.add(input.substring(start, i));
                start = -1;
            }
        }
        if (quoted) throw new IllegalArgumentException("Unterminated quoted string");
        if (depth != 0) throw new IllegalArgumentException("Unclosed delimiter");
        if (start != -1) output.add(input.substring(start));
        return output;
    }

    private static List<String> splitTopLevel(String input, char separator) {
        List<String> output = new ArrayList<>();
        int start = 0;
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') quoted = false;
                continue;
            }
            if (character == '"') quoted = true;
            else if (character == '(' || character == '[' || character == '{') depth++;
            else if (character == ')' || character == ']' || character == '}') depth--;
            else if (character == separator && depth == 0) {
                output.add(input.substring(start, i).trim());
                start = i + 1;
            }
        }
        output.add(input.substring(start).trim());
        return output;
    }

    private static String listBody(String input) {
        String value = input.trim();
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new IllegalArgumentException("Expected a bracketed list");
        }
        return value.substring(1, value.length() - 1);
    }

    private static boolean function(String input, String name) {
        return input.regionMatches(true, 0, name + "(", 0, name.length() + 1) && input.endsWith(")");
    }

    private static String functionBody(String input, String name) {
        if (!function(input, name)) throw new IllegalArgumentException("Expected " + name + "(...)");
        return input.substring(name.length() + 1, input.length() - 1).trim();
    }

    private static int size(List<Object> values) {
        return values == null ? 0 : values.size();
    }

    private static Object[] array(List<Object> values) {
        return values == null ? null : values.toArray();
    }

    private static int integer(String value) {
        try {
            return Integer.decode(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer '" + value + "'");
        }
    }

    private static boolean bool(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Expected true or false");
    }

    private static String parseString(String value, int line) {
        try {
            return AssemblerValueCodec.parseQuotedString(value);
        } catch (IllegalArgumentException exception) {
            if (line > 0) throw lineError(line, exception.getMessage());
            throw exception;
        }
    }

    private static String quote(String value) {
        return AssemblerValueCodec.quote(value);
    }

    private static String opcodeName(int opcode) {
        if (opcode < 0 || opcode >= Printer.OPCODES.length || Printer.OPCODES[opcode] == null) {
            throw new IllegalArgumentException("Unknown opcode " + opcode);
        }
        return Printer.OPCODES[opcode].toLowerCase(Locale.ROOT);
    }

    private static String frameTypeName(int type) {
        return switch (type) {
            case Opcodes.F_NEW -> "F_NEW";
            case Opcodes.F_FULL -> "F_FULL";
            case Opcodes.F_APPEND -> "F_APPEND";
            case Opcodes.F_CHOP -> "F_CHOP";
            case Opcodes.F_SAME -> "F_SAME";
            case Opcodes.F_SAME1 -> "F_SAME1";
            default -> throw new IllegalArgumentException("Unknown frame type " + type);
        };
    }

    private static int parseFrameType(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "F_NEW" -> Opcodes.F_NEW;
            case "F_FULL" -> Opcodes.F_FULL;
            case "F_APPEND" -> Opcodes.F_APPEND;
            case "F_CHOP" -> Opcodes.F_CHOP;
            case "F_SAME" -> Opcodes.F_SAME;
            case "F_SAME1" -> Opcodes.F_SAME1;
            default -> throw new IllegalArgumentException("Unknown frame type '" + type + "'");
        };
    }

    private static void requireCount(List<String> tokens, int expected, int line) {
        if (tokens.size() == expected) return;
        String message = "Expected " + (expected - 1) + " operand" + (expected == 2 ? "" : "s")
                + " for " + tokens.get(0) + ", found " + (tokens.size() - 1);
        if (line > 0) throw lineError(line, message);
        throw new IllegalArgumentException(message);
    }

    private static IllegalArgumentException lineError(int line, String message) {
        return new IllegalArgumentException("Line " + line + ": " + (message == null ? "Invalid instruction" : message));
    }

    public record ParsedInstructions(List<AbstractInsnNode> instructions, Map<LabelNode, String> labelNames) {
    }

    private record SourceLine(int number, List<String> tokens) {
    }

    private static final class LabelNames {
        private final Function<LabelNode, String> labelNamer;
        private final Map<LabelNode, String> names = new IdentityHashMap<>();
        private final Set<String> used = new LinkedHashSet<>();

        private LabelNames(Function<LabelNode, String> labelNamer) {
            this.labelNamer = labelNamer;
        }

        private String name(LabelNode label) {
            return names.computeIfAbsent(label, ignored -> {
                String requested = labelNamer == null ? null : labelNamer.apply(label);
                String base = requested == null || requested.isBlank() ? "L" + names.size() : requested;
                String name = base;
                int suffix = 2;
                while (!used.add(name)) name = base + "_" + suffix++;
                return name;
            });
        }
    }

    private static final class ParseLabels {
        private final Map<String, LabelNode> declared;
        private final Function<String, LabelNode> existingResolver;
        private final Map<LabelNode, String> labelNames;
        private final Map<String, LabelNode> external = new LinkedHashMap<>();
        private final Set<LabelNode> createdExternal = Collections.newSetFromMap(new IdentityHashMap<>());

        private ParseLabels(Map<String, LabelNode> declared, Function<String, LabelNode> existingResolver,
                            Map<LabelNode, String> labelNames) {
            this.declared = declared;
            this.existingResolver = existingResolver;
            this.labelNames = labelNames;
        }

        private LabelNode declared(String name) {
            LabelNode label = declared.get(name);
            if (label == null) throw new IllegalArgumentException("Unknown declared label " + quote(name));
            return label;
        }

        private LabelNode resolve(String name) {
            LabelNode label = declared.get(name);
            if (label != null) return label;
            return external.computeIfAbsent(name, ignored -> {
                LabelNode existing = existingResolver == null ? null : existingResolver.apply(name);
                if (existing != null) return existing;
                LabelNode created = new LabelNode();
                createdExternal.add(created);
                labelNames.put(created, name);
                return created;
            });
        }

        private List<LabelNode> createdExternalLabels() {
            return external.values().stream().filter(createdExternal::contains).toList();
        }
    }
}
