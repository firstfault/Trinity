package me.f1nal.trinity.refactor.identity;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.f1nal.trinity.refactor.identity.IdentityRefactorChange.Category;

/** In-place remapping of every standard reference-bearing ASM tree location. */
final class IdentityClassTransformer {
    private final IdentityAsmRemapper remapper;
    private final List<IdentityRefactorChange> changes;
    private String className;

    IdentityClassTransformer(IdentityAsmRemapper remapper, List<IdentityRefactorChange> changes) {
        this.remapper = remapper;
        this.changes = changes;
    }

    void transform(ClassNode node) {
        String originalOwner = node.name;
        this.className = originalOwner;

        node.name = mapType(node.name, Category.DECLARATION, "Class declaration");
        node.signature = mapSignature(node.signature, false, "Class signature");
        node.superName = mapType(node.superName, Category.METADATA, "Superclass");
        mapTypeList(node.interfaces, Category.METADATA, "Interface");
        mapAnnotations(node.visibleAnnotations, "Visible class annotation");
        mapAnnotations(node.invisibleAnnotations, "Invisible class annotation");
        mapAnnotations(node.visibleTypeAnnotations, "Visible class type annotation");
        mapAnnotations(node.invisibleTypeAnnotations, "Invisible class type annotation");

        String oldOuterClass = node.outerClass;
        String oldOuterMethod = node.outerMethod;
        String oldOuterDescriptor = node.outerMethodDesc;
        if (oldOuterClass != null && oldOuterMethod != null && oldOuterDescriptor != null) {
            node.outerMethod = change(Category.METADATA, "Enclosing method",
                    oldOuterMethod, remapper.mapMethodName(
                            oldOuterClass, oldOuterMethod, oldOuterDescriptor));
            node.outerMethodDesc = mapMethodDescriptor(
                    oldOuterDescriptor, "Enclosing method descriptor");
        }
        node.outerClass = mapType(oldOuterClass, Category.METADATA, "Enclosing class");
        node.nestHostClass = mapType(node.nestHostClass, Category.METADATA, "Nest host");
        mapTypeList(node.nestMembers, Category.METADATA, "Nest member");
        mapTypeList(node.permittedSubclasses, Category.METADATA, "Permitted subclass");

        if (node.innerClasses != null) {
            for (InnerClassNode inner : node.innerClasses) {
                String oldName = inner.name;
                String oldOuter = inner.outerName;
                String oldInner = inner.innerName;
                inner.innerName = change(Category.METADATA, "Inner simple name", oldInner,
                        remapper.mapInnerClassName(oldName, oldOuter, oldInner));
                inner.name = mapType(oldName, Category.METADATA, "Inner class");
                inner.outerName = mapType(oldOuter, Category.METADATA, "Inner class owner");
            }
        }

        mapModule(node);
        mapRecordComponents(node, originalOwner);

        if (node.fields != null) {
            for (FieldNode field : node.fields) mapField(field, originalOwner);
        }
        if (node.methods != null) {
            for (MethodNode method : node.methods) mapMethod(method, originalOwner);
        }
    }

    private void mapModule(ClassNode node) {
        if (node.module == null) return;
        node.module.mainClass = mapType(
                node.module.mainClass, Category.MODULE, "Module main class");
        mapTypeList(node.module.uses, Category.MODULE, "Module service use");
        if (node.module.provides != null) {
            for (ModuleProvideNode provide : node.module.provides) {
                provide.service = mapType(
                        provide.service, Category.MODULE, "Module service");
                mapTypeList(provide.providers, Category.MODULE, "Module service provider");
            }
        }
    }

    private void mapRecordComponents(ClassNode node, String originalOwner) {
        if (node.recordComponents == null) return;
        for (RecordComponentNode component : node.recordComponents) {
            String oldName = component.name;
            String oldDescriptor = component.descriptor;
            component.name = change(Category.DECLARATION, "Record component",
                    oldName, remapper.mapRecordComponentName(
                            originalOwner, oldName, oldDescriptor));
            component.descriptor = mapDescriptor(
                    oldDescriptor, "Record component descriptor");
            component.signature = mapSignature(
                    component.signature, true, "Record component signature");
            mapAnnotations(component.visibleAnnotations, "Visible record annotation");
            mapAnnotations(component.invisibleAnnotations, "Invisible record annotation");
            mapAnnotations(component.visibleTypeAnnotations, "Visible record type annotation");
            mapAnnotations(component.invisibleTypeAnnotations, "Invisible record type annotation");
        }
    }

    private void mapField(FieldNode field, String originalOwner) {
        String oldName = field.name;
        String oldDescriptor = field.desc;
        field.name = change(Category.DECLARATION, "Field declaration " + oldName,
                oldName, remapper.mapFieldName(originalOwner, oldName, oldDescriptor));
        field.desc = mapDescriptor(oldDescriptor, "Field descriptor " + oldName);
        field.signature = mapSignature(
                field.signature, true, "Field signature " + oldName);
        field.value = mapValue(field.value, Category.CONSTANT, "Field constant " + oldName);
        mapAnnotations(field.visibleAnnotations, "Visible field annotation " + oldName);
        mapAnnotations(field.invisibleAnnotations, "Invisible field annotation " + oldName);
        mapAnnotations(field.visibleTypeAnnotations, "Visible field type annotation " + oldName);
        mapAnnotations(field.invisibleTypeAnnotations, "Invisible field type annotation " + oldName);
    }

    private void mapMethod(MethodNode method, String originalOwner) {
        String oldName = method.name;
        String oldDescriptor = method.desc;
        String methodLocation = oldName + oldDescriptor;
        method.name = change(Category.DECLARATION, "Method declaration " + methodLocation,
                oldName, remapper.mapMethodName(originalOwner, oldName, oldDescriptor));
        method.desc = mapMethodDescriptor(oldDescriptor, "Method descriptor " + methodLocation);
        method.signature = mapSignature(
                method.signature, false, "Method signature " + methodLocation);
        mapTypeList(method.exceptions, Category.METADATA, "Declared exception " + methodLocation);
        method.annotationDefault = mapAnnotationValue(
                method.annotationDefault, "Annotation default " + methodLocation);
        mapAnnotations(method.visibleAnnotations, "Visible method annotation " + methodLocation);
        mapAnnotations(method.invisibleAnnotations, "Invisible method annotation " + methodLocation);
        mapAnnotations(method.visibleTypeAnnotations, "Visible method type annotation " + methodLocation);
        mapAnnotations(method.invisibleTypeAnnotations, "Invisible method type annotation " + methodLocation);
        mapAnnotationArray(method.visibleParameterAnnotations,
                "Visible parameter annotation " + methodLocation);
        mapAnnotationArray(method.invisibleParameterAnnotations,
                "Invisible parameter annotation " + methodLocation);
        mapLocalVariableAnnotations(method.visibleLocalVariableAnnotations,
                "Visible variable annotation " + methodLocation);
        mapLocalVariableAnnotations(method.invisibleLocalVariableAnnotations,
                "Invisible variable annotation " + methodLocation);

        if (method.localVariables != null) {
            for (LocalVariableNode local : method.localVariables) {
                local.desc = mapDescriptor(local.desc,
                        "Variable descriptor " + methodLocation + ':' + local.name);
                local.signature = mapSignature(local.signature, true,
                        "Variable signature " + methodLocation + ':' + local.name);
            }
        }
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                block.type = mapType(block.type, Category.METADATA,
                        "Catch type " + methodLocation);
                mapAnnotations(block.visibleTypeAnnotations,
                        "Visible catch annotation " + methodLocation);
                mapAnnotations(block.invisibleTypeAnnotations,
                        "Invisible catch annotation " + methodLocation);
            }
        }
        if (method.instructions == null) return;
        int instructionIndex = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            mapInstruction(instruction, methodLocation, instructionIndex++);
        }
    }

    private void mapInstruction(AbstractInsnNode instruction, String method, int index) {
        String location = method + " instruction " + index;
        mapAnnotations(instruction.visibleTypeAnnotations, "Visible instruction annotation " + location);
        mapAnnotations(instruction.invisibleTypeAnnotations, "Invisible instruction annotation " + location);

        if (instruction instanceof MethodInsnNode call) {
            String oldOwner = call.owner;
            String oldName = call.name;
            String oldDescriptor = call.desc;
            call.name = change(Category.BYTECODE, "Invocation name " + location, oldName,
                    remapper.mapMethodName(oldOwner, oldName, oldDescriptor));
            call.owner = mapType(oldOwner, Category.BYTECODE, "Invocation owner " + location);
            call.desc = mapMethodDescriptor(oldDescriptor, "Invocation descriptor " + location);
        } else if (instruction instanceof FieldInsnNode access) {
            String oldOwner = access.owner;
            String oldName = access.name;
            String oldDescriptor = access.desc;
            access.name = change(Category.BYTECODE, "Field access name " + location, oldName,
                    remapper.mapFieldName(oldOwner, oldName, oldDescriptor));
            access.owner = mapType(oldOwner, Category.BYTECODE, "Field access owner " + location);
            access.desc = mapDescriptor(oldDescriptor, "Field access descriptor " + location);
        } else if (instruction instanceof TypeInsnNode type) {
            type.desc = mapType(type.desc, Category.BYTECODE, "Type instruction " + location);
        } else if (instruction instanceof MultiANewArrayInsnNode array) {
            array.desc = mapDescriptor(array.desc, "Multi-array descriptor " + location);
        } else if (instruction instanceof LdcInsnNode ldc) {
            ldc.cst = mapValue(ldc.cst, Category.CONSTANT, "LDC " + location);
        } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            String oldName = dynamic.name;
            String oldDescriptor = dynamic.desc;
            Handle oldBootstrap = dynamic.bsm;
            Object[] oldArguments = dynamic.bsmArgs == null
                    ? new Object[0] : dynamic.bsmArgs.clone();
            dynamic.name = change(Category.BYTECODE, "InvokeDynamic name " + location,
                    oldName, remapper.mapInvokeDynamicMethodName(
                            oldName, oldDescriptor, oldBootstrap, oldArguments));
            dynamic.desc = mapMethodDescriptor(oldDescriptor,
                    "InvokeDynamic descriptor " + location);
            dynamic.bsm = (Handle) mapValue(oldBootstrap, Category.CONSTANT,
                    "InvokeDynamic bootstrap " + location);
            if (dynamic.bsmArgs != null) {
                for (int argument = 0; argument < dynamic.bsmArgs.length; argument++) {
                    Object before = dynamic.bsmArgs[argument];
                    Object semantic = remapper.mapBootstrapArgument(oldBootstrap,
                            oldDescriptor, oldArguments, argument, before);
                    Object after = remapper.mapValue(semantic);
                    if (!Objects.deepEquals(before, after)) {
                        add(Category.CONSTANT,
                                "InvokeDynamic argument " + argument + ' ' + location,
                                displayValue(before), displayValue(after));
                    }
                    dynamic.bsmArgs[argument] = after;
                }
            }
        } else if (instruction instanceof FrameNode frame) {
            mapFrameValues(frame.local, "Stack frame local " + location);
            mapFrameValues(frame.stack, "Stack frame value " + location);
        }
    }

    private void mapFrameValues(List<Object> values, String location) {
        if (values == null) return;
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (value instanceof String type) {
                values.set(index, mapType(type, Category.METADATA, location));
            }
        }
    }

    private void mapLocalVariableAnnotations(
            List<LocalVariableAnnotationNode> annotations, String location) {
        mapAnnotations(annotations, location);
    }

    private void mapAnnotationArray(List<AnnotationNode>[] annotations, String location) {
        if (annotations == null) return;
        for (List<AnnotationNode> parameter : annotations) mapAnnotations(parameter, location);
    }

    private void mapAnnotations(List<? extends AnnotationNode> annotations, String location) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) mapAnnotation(annotation, location);
    }

    private void mapAnnotation(AnnotationNode annotation, String location) {
        if (annotation == null) return;
        String oldDescriptor = annotation.desc;
        annotation.desc = mapDescriptor(oldDescriptor, location + " type");
        if (annotation.values == null) return;
        for (int index = 0; index + 1 < annotation.values.size(); index += 2) {
            Object name = annotation.values.get(index);
            if (name instanceof String attributeName) {
                annotation.values.set(index, change(Category.ANNOTATION,
                        location + " element", attributeName,
                        remapper.mapAnnotationAttributeName(oldDescriptor, attributeName)));
            }
            annotation.values.set(index + 1,
                    mapAnnotationValue(annotation.values.get(index + 1), location + " value"));
        }
    }

    private Object mapAnnotationValue(Object value, String location) {
        if (value instanceof AnnotationNode annotation) {
            mapAnnotation(annotation, location + " annotation");
            return annotation;
        }
        if (value instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> mutable = (List<Object>) list;
            for (int index = 0; index < mutable.size(); index++) {
                mutable.set(index, mapAnnotationValue(mutable.get(index), location));
            }
            return mutable;
        }
        if (value instanceof String[] enumValue && enumValue.length == 2) {
            String oldDescriptor = enumValue[0];
            String oldName = enumValue[1];
            try {
                String owner = Type.getType(oldDescriptor).getInternalName();
                enumValue[1] = change(Category.ANNOTATION, location + " enum constant",
                        oldName, remapper.mapFieldName(owner, oldName, oldDescriptor));
                enumValue[0] = mapDescriptor(oldDescriptor, location + " enum type");
            } catch (RuntimeException ignored) {
                // Preserve malformed annotation data exactly; validation will report other failures.
            }
            return enumValue;
        }
        return mapValue(value, Category.ANNOTATION, location);
    }

    private Object mapValue(Object value, Category category, String location) {
        if (value == null) return null;
        Object mapped = remapper.mapValue(value);
        if (!Objects.deepEquals(value, mapped)) {
            add(category, location, displayValue(value), displayValue(mapped));
        }
        return mapped;
    }

    private String mapType(String value, Category category, String location) {
        return value == null ? null : change(category, location, value, remapper.mapType(value));
    }

    private void mapTypeList(List<String> values, Category category, String location) {
        if (values == null) return;
        for (int index = 0; index < values.size(); index++) {
            values.set(index, mapType(values.get(index), category, location));
        }
    }

    private String mapDescriptor(String descriptor, String location) {
        return descriptor == null ? null : change(Category.DESCRIPTOR, location,
                descriptor, remapper.mapDesc(descriptor));
    }

    private String mapMethodDescriptor(String descriptor, String location) {
        return descriptor == null ? null : change(Category.DESCRIPTOR, location,
                descriptor, remapper.mapMethodDesc(descriptor));
    }

    private String mapSignature(String signature, boolean typeSignature, String location) {
        return signature == null ? null : change(Category.SIGNATURE, location,
                signature, remapper.mapSignature(signature, typeSignature));
    }

    private String change(Category category, String location, String before, String after) {
        if (!Objects.equals(before, after)) add(category, location, before, after);
        return after;
    }

    private void add(Category category, String location, String before, String after) {
        if (changes != null) {
            changes.add(new IdentityRefactorChange(
                    category, className, location, before, after));
        }
    }

    private static String displayValue(Object value) {
        if (value instanceof Object[] array) return Arrays.deepToString(array);
        return String.valueOf(value);
    }
}
