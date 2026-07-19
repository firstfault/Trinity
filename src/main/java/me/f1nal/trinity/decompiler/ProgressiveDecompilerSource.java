package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.decompiler.output.impl.ClassOutputMember;
import me.f1nal.trinity.decompiler.output.impl.FieldDeclarationOutputMember;
import me.f1nal.trinity.decompiler.output.impl.FieldOutputMember;
import me.f1nal.trinity.decompiler.output.impl.FieldStartEndOutputMember;
import me.f1nal.trinity.decompiler.output.impl.KindOutputMember;
import me.f1nal.trinity.decompiler.output.impl.MethodOutputMember;
import me.f1nal.trinity.decompiler.output.impl.MethodStartEndOutputMember;
import me.f1nal.trinity.decompiler.output.impl.NumberOutputMember;
import me.f1nal.trinity.decompiler.output.impl.PackageOutputMember;
import me.f1nal.trinity.decompiler.output.impl.StringOutputMember;
import me.f1nal.trinity.decompiler.output.impl.VariableOutputMember;
import me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.List;

final class ProgressiveDecompilerSource {
    static final String IN_PROGRESS = "// -- // trinity: decompilation in progress... // -- //";

    private ProgressiveDecompilerSource() {
    }

    static Result build(ClassInput classInput) {
        ClassNode node = classInput.getNode();
        StringBuilder output = new StringBuilder();

        output.append(OutputMemberSerializer.serializeTags(new PackageOutputMember(0, null, true)));
        output.append(OutputMemberSerializer.serializeTags(new PackageOutputMember(0, node.name, false)));
        appendClassHeader(output, classInput);

        boolean hasContent = false;
        for (FieldNode fieldNode : node.fields) {
            appendField(output, classInput, fieldNode);
            hasContent = true;
        }

        for (MethodNode methodNode : node.methods) {
            MethodInput methodInput = classInput.getMethod(methodNode.name, methodNode.desc);
            if (methodInput == null) {
                continue;
            }
            if (hasContent) {
                output.append('\n');
            }
            appendMethod(output, classInput, methodNode, methodInput);
            hasContent = true;
        }

        output.append("}\n");
        return new Result(output.toString());
    }

    private static void appendClassHeader(StringBuilder output, ClassInput classInput) {
        ClassNode node = classInput.getNode();
        appendVisibility(output, node.access);
        appendModifier(output, node.access, Opcodes.ACC_STATIC, "static");
        appendModifier(output, node.access, Opcodes.ACC_ABSTRACT, "abstract",
                (node.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION)) == 0);
        appendModifier(output, node.access, Opcodes.ACC_FINAL, "final",
                (node.access & (Opcodes.ACC_ENUM | Opcodes.ACC_RECORD)) == 0);

        boolean annotation = (node.access & Opcodes.ACC_ANNOTATION) != 0;
        boolean enumClass = (node.access & Opcodes.ACC_ENUM) != 0;
        boolean interfaceClass = (node.access & Opcodes.ACC_INTERFACE) != 0;
        boolean recordClass = (node.access & Opcodes.ACC_RECORD) != 0 && node.recordComponents != null;
        if (node.permittedSubclasses != null && !node.permittedSubclasses.isEmpty() && !enumClass) {
            appendKeyword(output, "sealed");
            output.append(' ');
        }

        if (annotation) {
            output.append(OutputMemberSerializer.kind("@interface", KindOutputMember.KindType.CLASS_ANNOTATION));
        } else if (enumClass) {
            output.append(OutputMemberSerializer.kind("enum", KindOutputMember.KindType.CLASS_ENUM));
        } else if (interfaceClass) {
            output.append(OutputMemberSerializer.kind("interface", KindOutputMember.KindType.CLASS_INTERFACE));
        } else if (recordClass) {
            appendKeyword(output, "record");
        } else {
            output.append(OutputMemberSerializer.kind("class", (node.access & Opcodes.ACC_ABSTRACT) != 0
                    ? KindOutputMember.KindType.CLASS_ABSTRACT : KindOutputMember.KindType.CLASS_CLASSES));
        }
        output.append(' ');
        appendClass(output, classInput.getDisplaySimpleName(), node.name);

        if (recordClass) {
            output.append('(');
            for (int i = 0; i < node.recordComponents.size(); i++) {
                if (i > 0) {
                    output.append(", ");
                }
                RecordComponentNode component = node.recordComponents.get(i);
                appendType(output, classInput, Type.getType(component.descriptor));
                output.append(' ');
                output.append(OutputMemberSerializer.tag(component.name,
                        length -> new FieldOutputMember(length, node.name, component.name, component.descriptor)));
            }
            output.append(')');
        }

        if (!interfaceClass && !enumClass && !recordClass && node.superName != null
                && !"java/lang/Object".equals(node.superName)) {
            output.append(' ');
            appendKeyword(output, "extends");
            output.append(' ');
            appendClass(output, displayInternalName(classInput, node.superName), node.superName);
        }
        if (node.interfaces != null && !node.interfaces.isEmpty() && !annotation) {
            output.append(' ');
            appendKeyword(output, interfaceClass ? "extends" : "implements");
            output.append(' ');
            appendClasses(output, classInput, node.interfaces);
        }
        if (node.permittedSubclasses != null && !node.permittedSubclasses.isEmpty() && !enumClass) {
            output.append(' ');
            appendKeyword(output, "permits");
            output.append(' ');
            appendClasses(output, classInput, node.permittedSubclasses);
        }
        output.append(" {\n");
    }

    private static void appendField(StringBuilder output, ClassInput owner, FieldNode field) {
        output.append(OutputMemberSerializer.serializeTags(new FieldStartEndOutputMember(
                0, owner.getRealName(), field.desc, field.name)));
        FieldInput fieldInput = owner.getField(field.name, field.desc);
        output.append("    ");
        output.append(OutputMemberSerializer.serializeTags(
                new FieldDeclarationOutputMember(0, owner.getRealName(), field.desc, field.name)));
        appendVisibility(output, field.access);
        appendModifier(output, field.access, Opcodes.ACC_STATIC, "static");
        appendModifier(output, field.access, Opcodes.ACC_FINAL, "final");
        appendModifier(output, field.access, Opcodes.ACC_TRANSIENT, "transient");
        appendModifier(output, field.access, Opcodes.ACC_VOLATILE, "volatile");
        appendType(output, owner, Type.getType(field.desc));
        output.append(' ');

        String displayName = fieldInput == null ? field.name : fieldInput.getDisplayName().getName();
        output.append(OutputMemberSerializer.tag(displayName,
                length -> new FieldOutputMember(length, owner.getRealName(), field.name, field.desc)));
        if (field.value != null) {
            output.append(" = ");
            appendConstant(output, field);
        }
        output.append(";\n");
        output.append(OutputMemberSerializer.serializeTags(new FieldStartEndOutputMember(0)));
    }

    private static void appendMethod(StringBuilder output, ClassInput owner, MethodNode method, MethodInput methodInput) {
        output.append(OutputMemberSerializer.serializeTags(new MethodStartEndOutputMember(
                0, owner.getRealName(), method.desc, method.name)));
        output.append("    ");

        if (methodInput.isClinit()) {
            appendKeyword(output, "static");
            output.append(' ');
            output.append(OutputMemberSerializer.tag("{",
                    length -> new MethodOutputMember(length, owner.getRealName(), method.name, method.desc)));
            appendProgressBody(output);
            output.append(OutputMemberSerializer.serializeTags(new MethodStartEndOutputMember(0)));
            return;
        }

        appendVisibility(output, method.access);
        appendModifier(output, method.access, Opcodes.ACC_STATIC, "static");
        appendModifier(output, method.access, Opcodes.ACC_FINAL, "final");
        appendModifier(output, method.access, Opcodes.ACC_SYNCHRONIZED, "synchronized");
        appendModifier(output, method.access, Opcodes.ACC_NATIVE, "native");
        appendModifier(output, method.access, Opcodes.ACC_ABSTRACT, "abstract");
        appendModifier(output, method.access, Opcodes.ACC_STRICT, "strictfp");

        boolean constructor = methodInput.isInit();
        Type methodType = Type.getMethodType(method.desc);
        if (!constructor) {
            appendType(output, owner, methodType.getReturnType());
            output.append(' ');
        }

        String methodName = constructor ? owner.getDisplaySimpleName() : methodInput.getDisplayName().getName();
        output.append(OutputMemberSerializer.tag(methodName,
                length -> new MethodOutputMember(length, owner.getRealName(), method.name, method.desc)));
        output.append('(');

        Type[] argumentTypes = methodType.getArgumentTypes();
        int localIndex = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        for (int i = 0; i < argumentTypes.length; i++) {
            if (i > 0) {
                output.append(", ");
            }
            Type argumentType = argumentTypes[i];
            boolean varargs = i == argumentTypes.length - 1
                    && (method.access & Opcodes.ACC_VARARGS) != 0
                    && argumentType.getSort() == Type.ARRAY;
            appendType(output, owner, varargs ? decreaseArray(argumentType) : argumentType);
            if (varargs) {
                output.append("...");
            }
            output.append(' ');

            String parameterName = methodInput.getVariableTable().getVariable(localIndex).getName();
            int parameterIndex = localIndex;
            output.append(OutputMemberSerializer.tag(parameterName,
                    length -> new VariableOutputMember(length, parameterIndex, typeValue(argumentType))));
            localIndex += argumentType.getSize();
        }
        output.append(')');

        if (method.exceptions != null && !method.exceptions.isEmpty()) {
            output.append(' ');
            appendKeyword(output, "throws");
            output.append(' ');
            appendClasses(output, owner, method.exceptions);
        }

        boolean noBody = (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0;
        if (noBody) {
            output.append(";\n");
        } else {
            output.append(" {");
            appendProgressBody(output);
        }
        output.append(OutputMemberSerializer.serializeTags(new MethodStartEndOutputMember(0)));
    }

    private static void appendProgressBody(StringBuilder output) {
        output.append("\n        ").append(IN_PROGRESS).append("\n    }\n");
    }

    private static void appendType(StringBuilder output, ClassInput context, Type type) {
        Type elementType = type;
        int dimensions = 0;
        if (type.getSort() == Type.ARRAY) {
            dimensions = type.getDimensions();
            elementType = type.getElementType();
        }

        String dimensionsText = "[]".repeat(dimensions);
        if (elementType.getSort() == Type.OBJECT) {
            appendClass(output, displayInternalName(context, elementType.getInternalName()) + dimensionsText,
                    elementType.getInternalName());
        } else {
            appendKeyword(output, elementType.getClassName() + dimensionsText);
        }
    }

    private static void appendClasses(StringBuilder output, ClassInput context, List<String> classes) {
        for (int i = 0; i < classes.size(); i++) {
            if (i > 0) {
                output.append(", ");
            }
            String internalName = classes.get(i);
            appendClass(output, displayInternalName(context, internalName), internalName);
        }
    }

    private static void appendClass(StringBuilder output, String text, String internalName) {
        output.append(OutputMemberSerializer.tag(text, length -> new ClassOutputMember(length, internalName)));
    }

    private static void appendConstant(StringBuilder output, FieldNode field) {
        Object value = field.value;
        Type type = Type.getType(field.desc);
        if (value instanceof String string) {
            String text = '"' + escapeString(string) + '"';
            output.append(OutputMemberSerializer.tag(text, length -> new StringOutputMember(length, string)));
            return;
        }
        if (value instanceof Type classType) {
            appendType(output, null, classType);
            output.append('.');
            appendKeyword(output, "class");
            return;
        }
        if (!(value instanceof Number number)) {
            output.append(String.valueOf(value));
            return;
        }

        switch (type.getSort()) {
            case Type.BOOLEAN -> appendKeyword(output, number.intValue() == 0 ? "false" : "true");
            case Type.CHAR -> output.append('\'')
                    .append(NumberOutputMember.getConst(escapeChar(number.intValue()),
                            NumberOutputMember.ConstType.CHAR, number.intValue()))
                    .append('\'');
            case Type.BYTE, Type.SHORT -> output.append(NumberOutputMember.getConst(
                    number.toString(), NumberOutputMember.ConstType.SHORT, number));
            case Type.INT -> output.append(NumberOutputMember.getConst(
                    number.toString(), NumberOutputMember.ConstType.INTEGER, number));
            case Type.LONG -> output.append(NumberOutputMember.getConst(
                    number.toString(), NumberOutputMember.ConstType.LONG, number, 'L'));
            case Type.FLOAT -> output.append(NumberOutputMember.getConst(
                    number.toString(), NumberOutputMember.ConstType.FLOAT, number, 'F'));
            case Type.DOUBLE -> output.append(NumberOutputMember.getConst(
                    number.toString(), NumberOutputMember.ConstType.DOUBLE, number, 'D'));
            default -> output.append(number);
        }
    }

    private static String escapeString(String value) {
        StringBuilder output = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            output.append(switch (character) {
                case '\\' -> "\\\\";
                case '"' -> "\\\"";
                case '\b' -> "\\b";
                case '\t' -> "\\t";
                case '\n' -> "\\n";
                case '\f' -> "\\f";
                case '\r' -> "\\r";
                default -> String.valueOf(character);
            });
        }
        return output.toString();
    }

    private static String escapeChar(int value) {
        return switch ((char) value) {
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            case '\b' -> "\\b";
            case '\t' -> "\\t";
            case '\n' -> "\\n";
            case '\f' -> "\\f";
            case '\r' -> "\\r";
            default -> String.valueOf((char) value);
        };
    }

    private static Type decreaseArray(Type type) {
        return Type.getType(type.getDescriptor().substring(1));
    }

    private static String typeValue(Type type) {
        Type elementType = type.getSort() == Type.ARRAY ? type.getElementType() : type;
        return elementType.getSort() == Type.OBJECT ? elementType.getInternalName() : elementType.getDescriptor();
    }

    private static String displayInternalName(ClassInput context, String internalName) {
        if (context == null) {
            return internalName.replace('/', '.');
        }
        ClassTarget target = context.getExecution().getClassTarget(internalName);
        return (target == null ? internalName : target.getDisplayOrRealName()).replace('/', '.');
    }

    private static void appendVisibility(StringBuilder output, int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            appendKeyword(output, "public");
            output.append(' ');
        } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
            appendKeyword(output, "protected");
            output.append(' ');
        } else if ((access & Opcodes.ACC_PRIVATE) != 0) {
            appendKeyword(output, "private");
            output.append(' ');
        }
    }

    private static void appendModifier(StringBuilder output, int access, int flag, String text) {
        appendModifier(output, access, flag, text, true);
    }

    private static void appendModifier(StringBuilder output, int access, int flag, String text, boolean condition) {
        if (condition && (access & flag) != 0) {
            appendKeyword(output, text);
            output.append(' ');
        }
    }

    private static void appendKeyword(StringBuilder output, String text) {
        output.append(OutputMemberSerializer.keyword(text));
    }

    record Result(String rawOutput) {
    }
}
