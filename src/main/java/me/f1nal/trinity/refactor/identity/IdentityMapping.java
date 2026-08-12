package me.f1nal.trinity.refactor.identity;

import java.util.Map;
import java.util.Set;

final class IdentityMapping {
    private final Map<String, String> classes;
    private final Set<IdentityMemberKey> methods;
    private final Set<IdentityMemberKey> fields;
    private final Set<IdentityMemberKey> recordComponents;
    private final String newMemberName;

    IdentityMapping(Map<String, String> classes,
                    Set<IdentityMemberKey> methods,
                    Set<IdentityMemberKey> fields,
                    Set<IdentityMemberKey> recordComponents,
                    String newMemberName) {
        this.classes = Map.copyOf(classes);
        this.methods = Set.copyOf(methods);
        this.fields = Set.copyOf(fields);
        this.recordComponents = Set.copyOf(recordComponents);
        this.newMemberName = newMemberName;
    }

    Map<String, String> classes() {
        return classes;
    }

    Set<IdentityMemberKey> methods() {
        return methods;
    }

    Set<IdentityMemberKey> fields() {
        return fields;
    }

    Set<IdentityMemberKey> recordComponents() {
        return recordComponents;
    }

    String newMemberName() {
        return newMemberName;
    }

    IdentityMapping inverse(String originalMemberName) {
        Map<String, String> inverseClasses = new java.util.LinkedHashMap<>();
        classes.forEach((before, after) -> inverseClasses.put(after, before));
        Set<IdentityMemberKey> inverseMethods = methods.stream()
                .map(method -> new IdentityMemberKey(
                        classes.getOrDefault(method.owner(), method.owner()),
                        newMemberName, remapDescriptor(method.descriptor())))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<IdentityMemberKey> inverseFields = fields.stream()
                .map(field -> new IdentityMemberKey(
                        classes.getOrDefault(field.owner(), field.owner()),
                        newMemberName, remapDescriptor(field.descriptor())))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<IdentityMemberKey> inverseRecords = recordComponents.stream()
                .map(record -> new IdentityMemberKey(
                        classes.getOrDefault(record.owner(), record.owner()),
                        newMemberName, remapDescriptor(record.descriptor())))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return new IdentityMapping(inverseClasses, inverseMethods, inverseFields,
                inverseRecords, originalMemberName);
    }

    private String remapDescriptor(String descriptor) {
        org.objectweb.asm.commons.Remapper remapper = new org.objectweb.asm.commons.Remapper() {
            @Override
            public String map(String internalName) {
                return classes.getOrDefault(internalName, internalName);
            }
        };
        return descriptor.startsWith("(")
                ? remapper.mapMethodDesc(descriptor) : remapper.mapDesc(descriptor);
    }
}
