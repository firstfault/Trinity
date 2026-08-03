package me.f1nal.trinity.execution.membersearch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MemberSearchTypeUtil {
    private MemberSearchTypeUtil() {
    }

    static Type parseType(String value, boolean allowVoid) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Type is empty");
        if (text.startsWith("(")) throw new IllegalArgumentException("Method descriptors are not types");

        int dimensions = 0;
        while (text.endsWith("[]")) {
            dimensions++;
            text = text.substring(0, text.length() - 2).trim();
        }

        Type base;
        String primitive = primitiveDescriptor(text);
        if (primitive != null) {
            base = Type.getType(primitive);
        } else if (dimensions == 0 && isDescriptor(text)) {
            base = Type.getType(text);
        } else {
            String internal = text;
            if (internal.startsWith("L") && internal.endsWith(";")) {
                internal = internal.substring(1, internal.length() - 1);
            }
            internal = internal.replace('.', '/');
            if (internal.isBlank() || internal.indexOf(' ') >= 0) {
                throw new IllegalArgumentException("Invalid type: " + value);
            }
            base = Type.getObjectType(internal);
        }

        if (!allowVoid && base.getSort() == Type.VOID) {
            throw new IllegalArgumentException("void is not valid here");
        }
        if (dimensions == 0) return base;
        if (base.getSort() == Type.VOID) throw new IllegalArgumentException("void[] is not valid");
        return Type.getType("[".repeat(dimensions) + base.getDescriptor());
    }

    static List<Type> parseParameterList(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return List.of();
        if (text.startsWith("(")) {
            try {
                return List.of(Type.getArgumentTypes(text));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid method descriptor", exception);
            }
        }

        List<Type> types = new ArrayList<>();
        for (String part : text.split(",", -1)) {
            if (part.isBlank()) throw new IllegalArgumentException("Parameter type is empty");
            types.add(parseType(part, false));
        }
        return List.copyOf(types);
    }

    static boolean matches(Type candidate, Type requested, MemberSearchQuery.TypeMode mode,
                           TypeHierarchyResolver hierarchy) {
        if (candidate.equals(requested)) return true;

        int candidateDimensions = candidate.getSort() == Type.ARRAY ? candidate.getDimensions() : 0;
        int requestedDimensions = requested.getSort() == Type.ARRAY ? requested.getDimensions() : 0;
        if (candidateDimensions != requestedDimensions) return false;

        Type candidateElement = candidateDimensions == 0 ? candidate : candidate.getElementType();
        Type requestedElement = requestedDimensions == 0 ? requested : requested.getElementType();
        if (candidateElement.getSort() != Type.OBJECT || requestedElement.getSort() != Type.OBJECT) {
            return false;
        }
        if (mode == MemberSearchQuery.TypeMode.EXACT) return false;

        String child = mode == MemberSearchQuery.TypeMode.ASSIGNABLE_TO
                ? candidateElement.getInternalName() : requestedElement.getInternalName();
        String parent = mode == MemberSearchQuery.TypeMode.ASSIGNABLE_TO
                ? requestedElement.getInternalName() : candidateElement.getInternalName();
        return hierarchy.isSubtype(child, parent, false) == TypeHierarchyResolver.Result.MATCH;
    }

    static boolean signatureContains(String signature, String requestedInternalName) {
        if (signature == null || signature.isBlank()) return false;
        List<String> types = new ArrayList<>();
        try {
            new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public void visitClassType(String name) {
                    types.add(name);
                }

                @Override
                public void visitInnerClassType(String name) {
                    if (!types.isEmpty()) {
                        String owner = types.get(types.size() - 1);
                        types.add(owner + "$" + name);
                    }
                }
            });
        } catch (IllegalArgumentException exception) {
            try {
                new SignatureReader(signature).acceptType(new SignatureVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitClassType(String name) {
                        types.add(name);
                    }
                });
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return types.contains(requestedInternalName);
    }

    static String readable(Type type) {
        return type.getClassName();
    }

    private static boolean isDescriptor(String text) {
        if (text.startsWith("[") || text.startsWith("L") && text.endsWith(";")) return true;
        return text.length() == 1 && "VZCBSIFJD".contains(text);
    }

    private static String primitiveDescriptor(String text) {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "char" -> "C";
            case "byte" -> "B";
            case "short" -> "S";
            case "int" -> "I";
            case "float" -> "F";
            case "long" -> "J";
            case "double" -> "D";
            default -> null;
        };
    }
}
