package me.f1nal.trinity.refactor.identity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

import java.util.Set;

/** Resolution-aware ASM remapper for one analyzed identity mapping. */
final class IdentityAsmRemapper extends Remapper {
    private final IdentityMapping mapping;
    private final IdentityClassUniverse universe;

    IdentityAsmRemapper(IdentityMapping mapping, IdentityClassUniverse universe) {
        super(Opcodes.ASM9);
        this.mapping = mapping;
        this.universe = universe;
    }

    @Override
    public String map(String internalName) {
        return mapping.classes().getOrDefault(internalName, internalName);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        IdentityMemberKey exact = new IdentityMemberKey(owner, name, descriptor);
        if (mapping.methods().contains(exact)) return mapping.newMemberName();

        Set<IdentityMemberKey> resolved = universe.resolveMethods(owner, name, descriptor);
        if (resolved.stream().anyMatch(mapping.methods()::contains)) {
            return mapping.newMemberName();
        }
        return name;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        IdentityMemberKey exact = new IdentityMemberKey(owner, name, descriptor);
        if (mapping.fields().contains(exact)) return mapping.newMemberName();

        IdentityMemberKey resolved = universe.resolveField(owner, name, descriptor);
        return resolved != null && mapping.fields().contains(resolved)
                ? mapping.newMemberName() : name;
    }

    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        return mapping.recordComponents().contains(new IdentityMemberKey(owner, name, descriptor))
                ? mapping.newMemberName() : name;
    }

    @Override
    public String mapAnnotationAttributeName(String descriptor, String name) {
        String owner;
        try {
            owner = org.objectweb.asm.Type.getType(descriptor).getInternalName();
        } catch (RuntimeException ignored) {
            return name;
        }
        return mapping.methods().stream().anyMatch(method ->
                method.owner().equals(owner) && method.name().equals(name))
                ? mapping.newMemberName() : name;
    }

    @Override
    public String mapBasicInvokeDynamicMethodName(
            String name, String descriptor, Handle bootstrapMethodHandle,
            Object... bootstrapMethodArguments) {
        if (!"java/lang/invoke/ConstantBootstraps".equals(
                bootstrapMethodHandle.getOwner())) return name;
        try {
            return switch (bootstrapMethodHandle.getName()) {
                case "enumConstant" -> {
                    Type enumType = Type.getType(descriptor);
                    yield mapFieldName(enumType.getInternalName(), name, descriptor);
                }
                case "getStaticFinal" -> {
                    Type owner = typeArgument(bootstrapMethodArguments, 0);
                    yield owner == null ? name
                            : mapFieldName(owner.getInternalName(), name, descriptor);
                }
                case "fieldVarHandle", "staticFieldVarHandle" -> {
                    Type owner = typeArgument(bootstrapMethodArguments, 0);
                    Type fieldType = typeArgument(bootstrapMethodArguments, 1);
                    yield owner == null || fieldType == null ? name
                            : mapFieldName(owner.getInternalName(), name,
                            fieldType.getDescriptor());
                }
                default -> name;
            };
        } catch (RuntimeException ignored) {
            return name;
        }
    }

    /** Maps identities encoded as well-known bootstrap String arguments. */
    Object mapBootstrapArgument(Handle bootstrap, String callSiteDescriptor,
                                Object[] arguments, int index, Object value) {
        if (!(value instanceof String text)) return value;
        if ("java/lang/runtime/ObjectMethods".equals(bootstrap.getOwner())
                && "bootstrap".equals(bootstrap.getName()) && index == 1) {
            Type owner = typeArgument(arguments, 0);
            return owner == null ? value : mapRecordComponentNames(owner.getInternalName(), text);
        }
        if ("java/lang/runtime/SwitchBootstraps".equals(bootstrap.getOwner())
                && "enumSwitch".equals(bootstrap.getName())) {
            Type[] parameters = Type.getArgumentTypes(callSiteDescriptor);
            if (parameters.length != 0 && parameters[0].getSort() == Type.OBJECT) {
                String owner = parameters[0].getInternalName();
                return mapFieldName(owner, text, 'L' + owner + ';');
            }
        }
        return value;
    }

    private String mapRecordComponentNames(String owner, String names) {
        String[] components = names.split(";", -1);
        boolean changed = false;
        for (int index = 0; index < components.length; index++) {
            String component = components[index];
            if (mapping.recordComponents().stream().anyMatch(key ->
                    key.owner().equals(owner) && key.name().equals(component))) {
                components[index] = mapping.newMemberName();
                changed = true;
            }
        }
        return changed ? String.join(";", components) : names;
    }

    private static Type typeArgument(Object[] arguments, int index) {
        return index >= 0 && index < arguments.length && arguments[index] instanceof Type type
                ? type : null;
    }
}
