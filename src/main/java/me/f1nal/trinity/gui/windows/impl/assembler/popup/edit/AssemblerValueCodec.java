package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Canonical text codec for constants used by LDC and invokedynamic. */
public final class AssemblerValueCodec {
    private AssemblerValueCodec() {
    }

    public static String format(Object value) {
        if (value instanceof Integer number) return "int(" + number + ")";
        if (value instanceof Long number) return "long(" + number + ")";
        if (value instanceof Float number) return "float(" + number + ")";
        if (value instanceof Double number) return "double(" + number + ")";
        if (value instanceof String string) return "string(" + quote(string) + ")";
        if (value instanceof Type type) return "type(" + quote(type.getDescriptor()) + ")";
        if (value instanceof Handle handle) {
            return "handle(" + handleTagName(handle.getTag()) + ", " + quote(handle.getOwner()) + ", "
                    + quote(handle.getName()) + ", " + quote(handle.getDesc()) + ", " + handle.isInterface() + ")";
        }
        if (value instanceof ConstantDynamic dynamic) {
            List<String> arguments = new ArrayList<>();
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) {
                arguments.add(format(dynamic.getBootstrapMethodArgument(i)));
            }
            return "condy(" + quote(dynamic.getName()) + ", " + quote(dynamic.getDescriptor()) + ", "
                    + format(dynamic.getBootstrapMethod()) + ", [" + String.join(", ", arguments) + "])";
        }
        throw new IllegalArgumentException("Unsupported ASM constant type: "
                + (value == null ? "null" : value.getClass().getName()));
    }

    public static String formatList(Object[] values) {
        List<String> output = new ArrayList<>();
        for (Object value : values) output.add(format(value));
        return "[" + String.join(", ", output) + "]";
    }

    public static Object parse(String input) {
        Parser parser = new Parser(input);
        Object value = parser.parseValue();
        parser.requireEnd();
        return value;
    }

    public static Object[] parseList(String input) {
        Parser parser = new Parser(input);
        List<Object> values = parser.parseList();
        parser.requireEnd();
        return values.toArray();
    }

    public static Handle parseHandle(String input) {
        Object value = parse(input);
        if (!(value instanceof Handle handle)) throw new IllegalArgumentException("Expected handle(...)");
        return handle;
    }

    public static String quote(String value) {
        StringBuilder output = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> output.append("\\\\");
                case '"' -> output.append("\\\"");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                default -> {
                    if (c < 0x20) output.append(String.format("\\u%04x", (int) c));
                    else output.append(c);
                }
            }
        }
        return output.append('"').toString();
    }

    public static String parseQuotedString(String input) {
        Parser parser = new Parser(input);
        String value = parser.string();
        parser.requireEnd();
        return value;
    }

    private static String handleTagName(int tag) {
        return switch (tag) {
            case Opcodes.H_GETFIELD -> "H_GETFIELD";
            case Opcodes.H_GETSTATIC -> "H_GETSTATIC";
            case Opcodes.H_PUTFIELD -> "H_PUTFIELD";
            case Opcodes.H_PUTSTATIC -> "H_PUTSTATIC";
            case Opcodes.H_INVOKEVIRTUAL -> "H_INVOKEVIRTUAL";
            case Opcodes.H_INVOKESTATIC -> "H_INVOKESTATIC";
            case Opcodes.H_INVOKESPECIAL -> "H_INVOKESPECIAL";
            case Opcodes.H_NEWINVOKESPECIAL -> "H_NEWINVOKESPECIAL";
            case Opcodes.H_INVOKEINTERFACE -> "H_INVOKEINTERFACE";
            default -> throw new IllegalArgumentException("Invalid handle tag: " + tag);
        };
    }

    private static int parseHandleTag(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "H_GETFIELD" -> Opcodes.H_GETFIELD;
            case "H_GETSTATIC" -> Opcodes.H_GETSTATIC;
            case "H_PUTFIELD" -> Opcodes.H_PUTFIELD;
            case "H_PUTSTATIC" -> Opcodes.H_PUTSTATIC;
            case "H_INVOKEVIRTUAL" -> Opcodes.H_INVOKEVIRTUAL;
            case "H_INVOKESTATIC" -> Opcodes.H_INVOKESTATIC;
            case "H_INVOKESPECIAL" -> Opcodes.H_INVOKESPECIAL;
            case "H_NEWINVOKESPECIAL" -> Opcodes.H_NEWINVOKESPECIAL;
            case "H_INVOKEINTERFACE" -> Opcodes.H_INVOKEINTERFACE;
            default -> throw new IllegalArgumentException("Unknown handle tag: " + name);
        };
    }

    private static final class Parser {
        private final String input;
        private int offset;

        private Parser(String input) {
            this.input = input == null ? "" : input;
        }

        private Object parseValue() {
            String function = identifier();
            expect('(');
            return switch (function.toLowerCase(Locale.ROOT)) {
                case "int" -> {
                    String number = scalar();
                    expect(')');
                    yield Integer.decode(number);
                }
                case "long" -> {
                    String number = scalar();
                    expect(')');
                    yield Long.decode(stripSuffix(number, 'l'));
                }
                case "float" -> {
                    String number = scalar();
                    expect(')');
                    yield Float.valueOf(stripSuffix(number, 'f'));
                }
                case "double" -> {
                    String number = scalar();
                    expect(')');
                    yield Double.valueOf(stripSuffix(number, 'd'));
                }
                case "string" -> {
                    String string = string();
                    expect(')');
                    yield string;
                }
                case "type" -> {
                    String descriptor = string();
                    expect(')');
                    try {
                        yield Type.getType(descriptor);
                    } catch (RuntimeException exception) {
                        throw error("Invalid type descriptor");
                    }
                }
                case "handle" -> parseHandleBody();
                case "condy" -> parseCondyBody();
                default -> throw error("Unknown value function '" + function + "'");
            };
        }

        private Handle parseHandleBody() {
            int tag;
            try {
                tag = parseHandleTag(identifier());
            } catch (IllegalArgumentException exception) {
                throw error(exception.getMessage());
            }
            comma();
            String owner = string();
            comma();
            String name = string();
            comma();
            String descriptor = string();
            comma();
            String interfaceText = identifier();
            if (!interfaceText.equals("true") && !interfaceText.equals("false")) {
                throw error("Expected true or false");
            }
            expect(')');
            return new Handle(tag, owner, name, descriptor, Boolean.parseBoolean(interfaceText));
        }

        private ConstantDynamic parseCondyBody() {
            String name = string();
            comma();
            String descriptor = string();
            comma();
            Object bootstrap = parseValue();
            if (!(bootstrap instanceof Handle handle)) throw error("ConstantDynamic bootstrap must be a handle");
            comma();
            List<Object> arguments = parseList();
            expect(')');
            return new ConstantDynamic(name, descriptor, handle, arguments.toArray());
        }

        private List<Object> parseList() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                offset++;
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    offset++;
                    return values;
                }
                expect(',');
            }
        }

        private String identifier() {
            skipWhitespace();
            int start = offset;
            while (offset < input.length()) {
                char c = input.charAt(offset);
                if (!Character.isJavaIdentifierPart(c)) break;
                offset++;
            }
            if (start == offset) throw error("Expected identifier");
            return input.substring(start, offset);
        }

        private String scalar() {
            skipWhitespace();
            int start = offset;
            while (offset < input.length() && input.charAt(offset) != ')'
                    && input.charAt(offset) != ',' && !Character.isWhitespace(input.charAt(offset))) offset++;
            if (start == offset) throw error("Expected number");
            return input.substring(start, offset);
        }

        private String string() {
            skipWhitespace();
            expectRaw('"');
            StringBuilder output = new StringBuilder();
            while (offset < input.length()) {
                char c = input.charAt(offset++);
                if (c == '"') return output.toString();
                if (c != '\\') {
                    output.append(c);
                    continue;
                }
                if (offset >= input.length()) throw error("Incomplete escape sequence");
                char escape = input.charAt(offset++);
                switch (escape) {
                    case '\\', '"' -> output.append(escape);
                    case 'n' -> output.append('\n');
                    case 'r' -> output.append('\r');
                    case 't' -> output.append('\t');
                    case 'b' -> output.append('\b');
                    case 'f' -> output.append('\f');
                    case 'u' -> {
                        if (offset + 4 > input.length()) throw error("Incomplete Unicode escape");
                        try {
                            output.append((char) Integer.parseInt(input.substring(offset, offset + 4), 16));
                        } catch (NumberFormatException exception) {
                            throw error("Invalid Unicode escape");
                        }
                        offset += 4;
                    }
                    default -> throw error("Unknown escape sequence \\" + escape + "'");
                }
            }
            throw error("Unterminated string");
        }

        private void comma() {
            expect(',');
        }

        private void expect(char expected) {
            skipWhitespace();
            expectRaw(expected);
        }

        private void expectRaw(char expected) {
            if (offset >= input.length() || input.charAt(offset) != expected) {
                throw error("Expected '" + expected + "'");
            }
            offset++;
        }

        private boolean peek(char expected) {
            return offset < input.length() && input.charAt(offset) == expected;
        }

        private void skipWhitespace() {
            while (offset < input.length() && Character.isWhitespace(input.charAt(offset))) offset++;
        }

        private void requireEnd() {
            skipWhitespace();
            if (offset != input.length()) throw error("Unexpected trailing input");
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + offset);
        }

        private static String stripSuffix(String value, char suffix) {
            if (!value.isEmpty() && Character.toLowerCase(value.charAt(value.length() - 1)) == suffix) {
                return value.substring(0, value.length() - 1);
            }
            return value;
        }
    }
}
