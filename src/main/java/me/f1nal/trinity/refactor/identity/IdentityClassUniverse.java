package me.f1nal.trinity.refactor.identity;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency-aware symbolic JVM member resolver used by identity remapping. */
final class IdentityClassUniverse {
    private final Map<String, IdentityRefactorSnapshot.SnapshotClass> project;
    private final Map<String, byte[]> dependencies;
    private final Map<String, ClassNode> parsedDependencies = new java.util.HashMap<>();
    private final Set<String> unreadableDependencies = new HashSet<>();

    IdentityClassUniverse(Map<String, IdentityRefactorSnapshot.SnapshotClass> project,
                          Map<String, byte[]> dependencies) {
        this.project = project;
        this.dependencies = dependencies;
    }

    @Nullable ClassNode getClass(String name) {
        IdentityRefactorSnapshot.SnapshotClass snapshot = project.get(name);
        if (snapshot != null) return snapshot.node();
        byte[] bytes = dependencies.get(name);
        if (bytes == null || unreadableDependencies.contains(name)) return null;
        ClassNode parsed = parsedDependencies.get(name);
        if (parsed != null) return parsed;
        try {
            ClassNode node = new ClassNode(Opcodes.ASM9);
            new ClassReader(bytes).accept(node,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            parsedDependencies.put(name, node);
            return node;
        } catch (RuntimeException exception) {
            unreadableDependencies.add(name);
            return null;
        }
    }

    int unreadableDependencyCount() {
        return unreadableDependencies.size();
    }

    boolean isProjectClass(String name) {
        return project.containsKey(name);
    }

    Collection<IdentityMemberKey> projectMethodDeclarations(String name, String descriptor) {
        List<IdentityMemberKey> methods = new ArrayList<>();
        project.forEach((owner, snapshot) -> {
            if (declaredMethod(snapshot.node(), name, descriptor) != null) {
                methods.add(new IdentityMemberKey(owner, name, descriptor));
            }
        });
        return methods;
    }

    Set<IdentityMemberKey> projectOverrideFamily(IdentityMemberKey selected) {
        MethodNode selectedNode = declaredMethod(getClass(selected.owner()),
                selected.name(), selected.descriptor());
        if (selectedNode == null || !canParticipate(selectedNode)) return Set.of(selected);

        Set<IdentityMemberKey> declarations = new LinkedHashSet<>(
                projectMethodDeclarations(selected.name(), selected.descriptor()));
        Set<IdentityMemberKey> family = new LinkedHashSet<>();
        family.add(selected);
        boolean changed;
        do {
            changed = false;
            for (IdentityMemberKey candidate : declarations) {
                if (family.contains(candidate)) continue;
                for (IdentityMemberKey member : List.copyOf(family)) {
                    if (canOverrideEitherDirection(candidate, member)) {
                        changed |= family.add(candidate);
                        break;
                    }
                }
            }
        } while (changed);
        return family;
    }

    boolean hasDependencyContract(IdentityMemberKey method) {
        ClassNode owner = getClass(method.owner());
        if (owner == null) return false;
        return hasDependencyContract(owner, method.name(), method.descriptor(), new HashSet<>());
    }

    private boolean hasDependencyContract(ClassNode type, String name, String descriptor,
                                          Set<String> visited) {
        if (type == null || !visited.add(type.name)) return false;
        List<String> parents = new ArrayList<>();
        if (type.superName != null) parents.add(type.superName);
        if (type.interfaces != null) parents.addAll(type.interfaces);
        for (String parentName : parents) {
            ClassNode parent = getClass(parentName);
            if (parent == null) continue;
            MethodNode declaration = declaredMethod(parent, name, descriptor);
            if (!isProjectClass(parentName) && declaration != null && canParticipate(declaration)) {
                return true;
            }
            if (hasDependencyContract(parent, name, descriptor, visited)) return true;
        }
        return false;
    }

    Set<IdentityMemberKey> resolveMethods(String owner, String name, String descriptor) {
        ClassNode symbolicOwner = getClass(owner);
        if (symbolicOwner == null) return Set.of();
        return resolveMethods(symbolicOwner, name, descriptor);
    }

    private Set<IdentityMemberKey> resolveMethods(ClassNode owner, String name, String descriptor) {
        MethodNode declared = declaredMethod(owner, name, descriptor);
        if (declared != null) {
            return Set.of(new IdentityMemberKey(owner.name, name, descriptor));
        }

        Set<String> visited = new HashSet<>();
        visited.add(owner.name);
        ClassNode superClass = getClass(owner.superName);
        while (superClass != null && visited.add(superClass.name)) {
            MethodNode superMethod = declaredMethod(superClass, name, descriptor);
            if (superMethod != null) {
                return Set.of(new IdentityMemberKey(superClass.name, name, descriptor));
            }
            superClass = getClass(superClass.superName);
        }

        LinkedHashSet<IdentityMemberKey> interfaces = new LinkedHashSet<>();
        collectInterfaceMethods(owner, name, descriptor, interfaces, new HashSet<>());
        if (interfaces.size() < 2) return interfaces;

        List<IdentityMemberKey> specific = new ArrayList<>(interfaces);
        specific.removeIf(candidate -> interfaces.stream().anyMatch(other ->
                other != candidate && isSubtype(other.owner(), candidate.owner())));
        return new LinkedHashSet<>(specific);
    }

    @Nullable IdentityMemberKey resolveField(String owner, String name, String descriptor) {
        ClassNode symbolicOwner = getClass(owner);
        return symbolicOwner == null ? null
                : resolveField(symbolicOwner, name, descriptor, new HashSet<>());
    }

    private @Nullable IdentityMemberKey resolveField(ClassNode owner, String name,
                                                       String descriptor, Set<String> visited) {
        if (owner == null || !visited.add(owner.name)) return null;
        if (declaredField(owner, name, descriptor) != null) {
            return new IdentityMemberKey(owner.name, name, descriptor);
        }
        if (owner.interfaces != null) {
            for (String interfaceName : owner.interfaces) {
                IdentityMemberKey result = resolveField(getClass(interfaceName),
                        name, descriptor, visited);
                if (result != null) return result;
            }
        }
        return resolveField(getClass(owner.superName), name, descriptor, visited);
    }

    boolean isSubtype(String child, String possibleAncestor) {
        return isSubtype(child, possibleAncestor, new HashSet<>());
    }

    private boolean isSubtype(String child, String possibleAncestor, Set<String> visited) {
        if (child.equals(possibleAncestor)) return true;
        if (!visited.add(child)) return false;
        ClassNode node = getClass(child);
        if (node == null) return false;
        if (node.superName != null && isSubtype(node.superName, possibleAncestor, visited)) return true;
        if (node.interfaces != null) {
            for (String interfaceName : node.interfaces) {
                if (isSubtype(interfaceName, possibleAncestor, visited)) return true;
            }
        }
        return false;
    }

    private boolean canOverrideEitherDirection(IdentityMemberKey first, IdentityMemberKey second) {
        if (isSubtype(first.owner(), second.owner())) return canOverride(first, second);
        if (isSubtype(second.owner(), first.owner())) return canOverride(second, first);
        return false;
    }

    private boolean canOverride(IdentityMemberKey child, IdentityMemberKey parent) {
        MethodNode childNode = declaredMethod(getClass(child.owner()), child.name(), child.descriptor());
        MethodNode parentNode = declaredMethod(getClass(parent.owner()), parent.name(), parent.descriptor());
        if (childNode == null || parentNode == null || !canParticipate(childNode)
                || !canParticipate(parentNode) || (parentNode.access & Opcodes.ACC_FINAL) != 0) {
            return false;
        }
        int access = parentNode.access;
        return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                || packageName(child.owner()).equals(packageName(parent.owner()));
    }

    private void collectInterfaceMethods(ClassNode type, String name, String descriptor,
                                         Set<IdentityMemberKey> output, Set<String> visited) {
        if (type == null || !visited.add(type.name)) return;
        if (type.interfaces != null) {
            for (String interfaceName : type.interfaces) {
                ClassNode interfaceNode = getClass(interfaceName);
                if (interfaceNode == null) continue;
                MethodNode method = declaredMethod(interfaceNode, name, descriptor);
                if (method != null && (method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0) {
                    output.add(new IdentityMemberKey(interfaceName, name, descriptor));
                }
                collectInterfaceMethods(interfaceNode, name, descriptor, output, visited);
            }
        }
        collectInterfaceMethods(getClass(type.superName), name, descriptor, output, visited);
    }

    static @Nullable MethodNode declaredMethod(@Nullable ClassNode node,
                                                String name, String descriptor) {
        if (node == null || node.methods == null) return null;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        return null;
    }

    static @Nullable FieldNode declaredField(@Nullable ClassNode node,
                                              String name, String descriptor) {
        if (node == null || node.fields == null) return null;
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(descriptor)) return field;
        }
        return null;
    }

    private static boolean canParticipate(MethodNode method) {
        return !method.name.equals("<init>") && !method.name.equals("<clinit>")
                && (method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0;
    }

    private static String packageName(String owner) {
        int separator = owner.lastIndexOf('/');
        return separator == -1 ? "" : owner.substring(0, separator);
    }
}
