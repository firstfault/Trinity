package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.asm.AsmValueWalker;
import me.f1nal.trinity.logging.Logging;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Extracts class and member references from every standard ASM tree location.
 */
final class AsmReferenceScanner {
    private AsmReferenceScanner() {
    }

    static ScanResult scanClass(ClassNode node) {
        Builder builder = new Builder();
        Source classSource = Source.classSource();

        builder.addInternalName(node.superName, classSource, XrefKind.INHERIT, "Extends");
        if (node.interfaces != null) {
            for (String interfaceName : node.interfaces) {
                builder.addInternalName(interfaceName, classSource, XrefKind.INHERIT, "Implements");
            }
        }
        builder.addSignature(
                node.signature, false, classSource, XrefKind.METADATA, "Class signature");
        builder.addAnnotations(node.visibleAnnotations, classSource);
        builder.addAnnotations(node.invisibleAnnotations, classSource);
        builder.addAnnotations(node.visibleTypeAnnotations, classSource);
        builder.addAnnotations(node.invisibleTypeAnnotations, classSource);

        builder.addInternalName(
                node.outerClass, classSource, XrefKind.METADATA, "Enclosing class");
        if (node.outerClass != null && node.outerMethod != null && node.outerMethodDesc != null) {
            builder.addMember(node.outerClass, node.outerMethod, node.outerMethodDesc, classSource,
                    XrefKind.METADATA, XrefAccessType.READ, "Enclosing method",
                    -1, false, false);
            builder.addDescriptor(node.outerMethodDesc, classSource, XrefKind.METADATA,
                    "Enclosing method descriptor");
        }
        builder.addInternalName(
                node.nestHostClass, classSource, XrefKind.METADATA, "Nest host");
        builder.addInternalNames(
                node.nestMembers, classSource, XrefKind.METADATA, "Nest member");
        builder.addInternalNames(node.permittedSubclasses, classSource,
                XrefKind.METADATA, "Permitted subclass");

        if (node.innerClasses != null) {
            for (InnerClassNode innerClass : node.innerClasses) {
                builder.addInternalName(innerClass.name, classSource, XrefKind.METADATA,
                        "Inner class metadata");
                builder.addInternalName(innerClass.outerName, classSource, XrefKind.METADATA,
                        "Inner class owner");
            }
        }
        builder.addModuleReferences(node, classSource);

        if (node.recordComponents != null) {
            for (RecordComponentNode component : node.recordComponents) {
                builder.addDescriptor(component.descriptor, classSource, XrefKind.METADATA,
                        "Record component type");
                builder.addSignature(component.signature, true, classSource,
                        XrefKind.METADATA, "Record component signature");
                builder.addAnnotations(component.visibleAnnotations, classSource);
                builder.addAnnotations(component.invisibleAnnotations, classSource);
                builder.addAnnotations(component.visibleTypeAnnotations, classSource);
                builder.addAnnotations(component.invisibleTypeAnnotations, classSource);
            }
        }
        if (node.fields != null) {
            for (FieldNode field : node.fields) builder.scanField(field);
        }
        if (node.methods != null) {
            for (MethodNode method : node.methods) builder.scanMethod(method);
        }
        return builder.result();
    }

    static ScanResult scanMethod(MethodNode method) {
        Builder builder = new Builder();
        builder.scanMethod(method);
        return builder.result();
    }

    record Source(FieldNode field, MethodNode method, AbstractInsnNode instruction) {
        static Source classSource() {
            return new Source(null, null, null);
        }

        static Source fieldSource(FieldNode field) {
            return new Source(field, null, null);
        }

        static Source methodSource(MethodNode method) {
            return new Source(null, method, null);
        }

        static Source instructionSource(MethodNode method, AbstractInsnNode instruction) {
            return new Source(null, method, instruction);
        }
    }

    record ClassReference(String owner, Source source, XrefKind kind,
                          XrefAccessType access, String invocation) {
    }

    record MemberReference(MemberDetails details, Source source, XrefKind kind,
                           XrefAccessType access, String invocation, int opcode,
                           boolean interfaceOwner, boolean directInstruction) {
    }

    record ScanResult(List<ClassReference> classReferences,
                      List<MemberReference> memberReferences) {
    }

    private static final class Builder {
        private final List<ClassReference> classReferences = new ArrayList<>();
        private final Set<ClassReferenceKey> classReferenceKeys = new LinkedHashSet<>();
        private final List<MemberReference> memberReferences = new ArrayList<>();
        private final Set<MemberReferenceKey> memberReferenceKeys = new LinkedHashSet<>();

        private ScanResult result() {
            return new ScanResult(List.copyOf(classReferences), List.copyOf(memberReferences));
        }

        private void scanField(FieldNode field) {
            Source source = Source.fieldSource(field);
            addDescriptor(field.desc, source, XrefKind.TYPE, "Field type");
            addSignature(
                    field.signature, true, source, XrefKind.METADATA, "Field signature");
            addConstant(field.value, source, "Field constant");
            addAnnotations(field.visibleAnnotations, source);
            addAnnotations(field.invisibleAnnotations, source);
            addAnnotations(field.visibleTypeAnnotations, source);
            addAnnotations(field.invisibleTypeAnnotations, source);
        }

        private void scanMethod(MethodNode method) {
            Source source = Source.methodSource(method);
            addMethodDeclarationDescriptor(method.desc, source);
            addSignature(
                    method.signature, false, source, XrefKind.METADATA, "Method signature");
            if (method.exceptions != null) {
                for (String exception : method.exceptions) {
                    addInternalName(exception, source, XrefKind.EXCEPTION, "Throws");
                }
            }
            addConstant(method.annotationDefault, source, "Annotation default");
            addAnnotations(method.visibleAnnotations, source);
            addAnnotations(method.invisibleAnnotations, source);
            addAnnotations(method.visibleTypeAnnotations, source);
            addAnnotations(method.invisibleTypeAnnotations, source);
            addParameterAnnotations(method.visibleParameterAnnotations, source);
            addParameterAnnotations(method.invisibleParameterAnnotations, source);
            addAnnotations(method.visibleLocalVariableAnnotations, source);
            addAnnotations(method.invisibleLocalVariableAnnotations, source);

            if (method.localVariables != null) {
                for (LocalVariableNode local : method.localVariables) {
                    addDescriptor(local.desc, source, XrefKind.VARIABLE, "Variable");
                    addSignature(local.signature, true, source, XrefKind.VARIABLE, "Variable");
                }
            }
            if (method.tryCatchBlocks != null) {
                for (TryCatchBlockNode block : method.tryCatchBlocks) {
                    addInternalName(block.type, source, XrefKind.EXCEPTION, "Catch");
                    addAnnotations(block.visibleTypeAnnotations, source);
                    addAnnotations(block.invisibleTypeAnnotations, source);
                }
            }
            if (method.instructions == null) return;
            for (AbstractInsnNode instruction : method.instructions) {
                scanInstruction(method, instruction);
            }
        }

        private void scanInstruction(MethodNode method, AbstractInsnNode instruction) {
            Source source = Source.instructionSource(method, instruction);
            addAnnotations(instruction.visibleTypeAnnotations, source);
            addAnnotations(instruction.invisibleTypeAnnotations, source);

            if (instruction instanceof MethodInsnNode methodInsn) {
                String invocation = XrefInvocationFormatter.instruction(methodInsn.getOpcode());
                addInternalName(methodInsn.owner, source, XrefKind.INVOKE, invocation);
                addDescriptor(
                        methodInsn.desc, source, XrefKind.DESCRIPTOR, "Invocation descriptor");
                addMember(methodInsn.owner, methodInsn.name, methodInsn.desc, source,
                        XrefKind.INVOKE, XrefAccessType.EXECUTE, invocation,
                        methodInsn.getOpcode(), methodInsn.itf, true);
            } else if (instruction instanceof FieldInsnNode fieldInsn) {
                String invocation = XrefInvocationFormatter.instruction(fieldInsn.getOpcode());
                XrefAccessType access = isFieldWrite(fieldInsn.getOpcode())
                        ? XrefAccessType.WRITE : XrefAccessType.READ;
                addInternalName(fieldInsn.owner, source, XrefKind.FIELD, invocation, access);
                addDescriptor(fieldInsn.desc, source, XrefKind.DESCRIPTOR, "Field descriptor");
                addMember(fieldInsn.owner, fieldInsn.name, fieldInsn.desc, source,
                        XrefKind.FIELD, access, invocation, fieldInsn.getOpcode(), false, true);
            } else if (instruction instanceof TypeInsnNode typeInsn) {
                addInternalNameOrDescriptor(typeInsn.desc, source, XrefKind.TYPE,
                        XrefInvocationFormatter.instruction(typeInsn.getOpcode()));
            } else if (instruction instanceof MultiANewArrayInsnNode arrayInsn) {
                addDescriptor(arrayInsn.desc, source, XrefKind.TYPE,
                        XrefInvocationFormatter.instruction(arrayInsn.getOpcode()));
            } else if (instruction instanceof LdcInsnNode ldc) {
                addConstant(ldc.cst, source, "LDC");
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                String invocation =
                        XrefInvocationFormatter.instruction(Opcodes.INVOKEDYNAMIC);
                addDescriptor(dynamic.desc, source, XrefKind.TYPE, invocation);
                addConstant(dynamic.bsm, source, invocation + " bootstrap");
                if (dynamic.bsmArgs != null) {
                    for (Object argument : dynamic.bsmArgs) {
                        addConstant(argument, source, invocation + " bootstrap argument");
                    }
                }
            } else if (instruction instanceof FrameNode frame) {
                addFrameTypes(frame.local, source);
                addFrameTypes(frame.stack, source);
            }
        }

        private void addMethodDeclarationDescriptor(String descriptor, Source source) {
            if (descriptor == null) return;
            try {
                for (Type argument : Type.getArgumentTypes(descriptor)) {
                    addType(argument, source, XrefKind.PARAMETER, "Parameter");
                }
                addType(Type.getReturnType(descriptor), source, XrefKind.RETURN, "Returns");
            } catch (RuntimeException exception) {
                warnMalformed("method descriptor", descriptor, exception);
            }
        }

        private void addFrameTypes(List<Object> values, Source source) {
            if (values == null) return;
            for (Object value : values) {
                if (value instanceof String type) {
                    addInternalNameOrDescriptor(
                            type, source, XrefKind.STACK_FRAME, "Stack frame");
                }
            }
        }

        private void addModuleReferences(ClassNode node, Source source) {
            if (node.module == null) return;
            addInternalName(
                    node.module.mainClass, source, XrefKind.METADATA, "Module main class");
            addInternalNames(
                    node.module.uses, source, XrefKind.METADATA, "Module service use");
            if (node.module.provides != null) {
                for (ModuleProvideNode provide : node.module.provides) {
                    addInternalName(provide.service, source, XrefKind.METADATA,
                            "Module service");
                    addInternalNames(provide.providers, source,
                            XrefKind.METADATA, "Module service provider");
                }
            }
        }

        private void addInternalNames(List<String> names, Source source, XrefKind kind,
                                      String invocation) {
            if (names == null) return;
            for (String name : names) {
                addInternalName(name, source, kind, invocation);
            }
        }

        private void addParameterAnnotations(List<AnnotationNode>[] annotations, Source source) {
            if (annotations == null) return;
            for (List<AnnotationNode> parameter : annotations) addAnnotations(parameter, source);
        }

        private void addAnnotations(List<? extends AnnotationNode> annotations, Source source) {
            if (annotations == null) return;
            for (AnnotationNode annotation : annotations) {
                addConstant(annotation, source, "Annotation value");
            }
        }

        private void addConstant(Object root, Source source, String context) {
            AsmValueWalker.walk(root, value -> {
                if (value instanceof Type type) {
                    addType(type, source, XrefKind.LITERAL, ".class");
                } else if (value instanceof Handle handle) {
                    addHandle(handle, source, context);
                } else if (value instanceof ConstantDynamic dynamic) {
                    addDescriptor(dynamic.getDescriptor(), source, XrefKind.TYPE,
                            "ConstantDynamic type");
                } else if (value instanceof AnnotationNode annotation) {
                    addDescriptor(annotation.desc, source, XrefKind.ANNOTATION, "Annotation");
                } else if (value instanceof String[] enumValue) {
                    addAnnotationEnum(enumValue, source);
                }
            });
        }

        private void addAnnotationEnum(String[] enumValue, Source source) {
            if (enumValue.length != 2 || enumValue[0] == null || enumValue[1] == null) return;
            try {
                Type enumType = Type.getType(enumValue[0]);
                if (enumType.getSort() != Type.OBJECT) return;
                String owner = enumType.getInternalName();
                addInternalName(owner, source, XrefKind.ANNOTATION, "Annotation enum");
                addMember(owner, enumValue[1], enumValue[0], source,
                        XrefKind.ANNOTATION, XrefAccessType.READ, enumValue[1],
                        -1, false, false);
            } catch (RuntimeException exception) {
                warnMalformed("annotation enum descriptor", enumValue[0], exception);
            }
        }

        private void addHandle(Handle handle, Source source, String context) {
            int opcode = handleOpcode(handle.getTag());
            XrefKind kind = isFieldHandle(handle.getTag()) ? XrefKind.FIELD : XrefKind.INVOKE;
            XrefAccessType access = handleAccess(handle.getTag());
            String invocation =
                    context + " handle " + XrefInvocationFormatter.handle(handle.getTag());
            addInternalName(handle.getOwner(), source, kind, invocation, access);
            addDescriptor(handle.getDesc(), source, XrefKind.TYPE, context + " handle descriptor");
            addMember(handle.getOwner(), handle.getName(), handle.getDesc(), source,
                    kind, access, invocation, opcode, handle.isInterface(), false);
        }

        private void addDescriptor(String descriptor, Source source, XrefKind kind,
                                   String invocation) {
            if (descriptor == null) return;
            try {
                addType(Type.getType(descriptor), source, kind, invocation);
            } catch (RuntimeException exception) {
                warnMalformed("descriptor", descriptor, exception);
            }
        }

        private void addType(Type type, Source source, XrefKind kind, String invocation) {
            if (type == null) return;
            switch (type.getSort()) {
                case Type.OBJECT ->
                        addInternalName(type.getInternalName(), source, kind, invocation);
                case Type.ARRAY ->
                        addType(type.getElementType(), source, kind, invocation);
                case Type.METHOD -> {
                    for (Type argument : type.getArgumentTypes()) {
                        addType(argument, source, kind, invocation);
                    }
                    addType(type.getReturnType(), source, kind, invocation);
                }
                default -> {
                }
            }
        }

        private void addSignature(String signature, boolean typeSignature, Source source,
                                  XrefKind kind, String invocation) {
            if (signature == null) return;
            Consumer<String> consumer = name ->
                    addInternalName(name, source, kind, invocation);
            try {
                SignatureReader reader = new SignatureReader(signature);
                SignatureVisitor visitor = new TypeCollectingSignatureVisitor(consumer);
                if (typeSignature) reader.acceptType(visitor);
                else reader.accept(visitor);
            } catch (RuntimeException exception) {
                warnMalformed("signature", signature, exception);
            }
        }

        private void addInternalNameOrDescriptor(String value, Source source, XrefKind kind,
                                                 String invocation) {
            if (value == null) return;
            if (value.startsWith("[") || value.startsWith("(")
                    || value.startsWith("L") && value.endsWith(";")) {
                addDescriptor(value, source, kind, invocation);
            } else {
                addInternalName(value, source, kind, invocation);
            }
        }

        private void addInternalName(String owner, Source source, XrefKind kind,
                                     String invocation) {
            addInternalName(owner, source, kind, invocation, XrefAccessType.READ);
        }

        private void addInternalName(String owner, Source source, XrefKind kind,
                                     String invocation, XrefAccessType access) {
            if (owner == null || owner.isBlank()) return;
            String normalizedOwner = owner.replace('.', '/');
            if (classReferenceKeys.add(new ClassReferenceKey(
                    normalizedOwner, source, kind, access, invocation))) {
                classReferences.add(new ClassReference(
                        normalizedOwner, source, kind, access, invocation));
            }
        }

        private void addMember(String owner, String name, String descriptor, Source source,
                               XrefKind kind, XrefAccessType access, String invocation,
                               int opcode, boolean interfaceOwner, boolean directInstruction) {
            if (owner == null || name == null || descriptor == null) return;
            MemberDetails details =
                    new MemberDetails(owner.replace('.', '/'), name, descriptor);
            if (memberReferenceKeys.add(new MemberReferenceKey(
                    details, source, kind, access, invocation, directInstruction))) {
                memberReferences.add(new MemberReference(
                        details, source, kind, access, invocation, opcode,
                        interfaceOwner, directInstruction));
            }
        }

        private static void warnMalformed(String kind, String value, RuntimeException exception) {
            Logging.warn("Ignoring malformed ASM {} '{}': {}", kind, value, exception.getMessage());
        }
    }

    private record ClassReferenceKey(String owner, Source source, XrefKind kind,
                                     XrefAccessType access, String invocation) {
    }

    private record MemberReferenceKey(MemberDetails details, Source source, XrefKind kind,
                                      XrefAccessType access, String invocation,
                                      boolean directInstruction) {
    }

    private static final class TypeCollectingSignatureVisitor extends SignatureVisitor {
        private final Consumer<String> consumer;
        private String currentClass;

        private TypeCollectingSignatureVisitor(Consumer<String> consumer) {
            super(Opcodes.ASM9);
            this.consumer = consumer;
        }

        private SignatureVisitor child() {
            return new TypeCollectingSignatureVisitor(consumer);
        }

        @Override
        public SignatureVisitor visitClassBound() {
            return child();
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return child();
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            return child();
        }

        @Override
        public SignatureVisitor visitInterface() {
            return child();
        }

        @Override
        public SignatureVisitor visitParameterType() {
            return child();
        }

        @Override
        public SignatureVisitor visitReturnType() {
            return child();
        }

        @Override
        public SignatureVisitor visitExceptionType() {
            return child();
        }

        @Override
        public SignatureVisitor visitArrayType() {
            return child();
        }

        @Override
        public void visitClassType(String name) {
            currentClass = name;
            consumer.accept(name);
        }

        @Override
        public void visitInnerClassType(String name) {
            currentClass = currentClass == null ? name : currentClass + '$' + name;
            consumer.accept(currentClass);
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            return child();
        }
    }

    private static boolean isFieldWrite(int opcode) {
        return opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC;
    }

    private static boolean isFieldHandle(int tag) {
        return tag >= Opcodes.H_GETFIELD && tag <= Opcodes.H_PUTSTATIC;
    }

    private static XrefAccessType handleAccess(int tag) {
        if (tag == Opcodes.H_PUTFIELD || tag == Opcodes.H_PUTSTATIC) return XrefAccessType.WRITE;
        if (tag == Opcodes.H_GETFIELD || tag == Opcodes.H_GETSTATIC) return XrefAccessType.READ;
        return XrefAccessType.EXECUTE;
    }

    private static int handleOpcode(int tag) {
        return switch (tag) {
            case Opcodes.H_GETFIELD -> Opcodes.GETFIELD;
            case Opcodes.H_GETSTATIC -> Opcodes.GETSTATIC;
            case Opcodes.H_PUTFIELD -> Opcodes.PUTFIELD;
            case Opcodes.H_PUTSTATIC -> Opcodes.PUTSTATIC;
            case Opcodes.H_INVOKEVIRTUAL -> Opcodes.INVOKEVIRTUAL;
            case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
            case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> Opcodes.INVOKESPECIAL;
            case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
            default -> -1;
        };
    }

}
