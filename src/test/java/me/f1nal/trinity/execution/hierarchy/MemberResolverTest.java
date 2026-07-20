package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberResolverTest {
    private final Map<String, ClassInput> classes = new LinkedHashMap<>();

    @BeforeEach
    void createHierarchy() {
        type("sample/RootInterface", null, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE,
                new String[0], method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "run", "()V"));
        type("sample/ChildInterface", null, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE,
                new String[]{"sample/RootInterface"});
        type("sample/Base", "java/lang/Object", Opcodes.ACC_PUBLIC,
                new String[]{"sample/ChildInterface"},
                method(Opcodes.ACC_PUBLIC, "<init>", "()V"),
                method(Opcodes.ACC_PUBLIC, "run", "()V"),
                method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "utility", "()V"),
                method(Opcodes.ACC_PRIVATE, "privateMethod", "()V"));
        type("sample/Left", "sample/Base", Opcodes.ACC_PUBLIC, new String[0],
                method(Opcodes.ACC_PUBLIC, "<init>", "()V"),
                method(Opcodes.ACC_PUBLIC, "run", "()V"),
                method(Opcodes.ACC_PUBLIC, "privateMethod", "()V"));
        type("sample/Leaf", "sample/Left", Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, new String[0]);
        type("sample/Right", "sample/Base", Opcodes.ACC_PUBLIC, new String[0],
                method(Opcodes.ACC_PUBLIC, "run", "()V"));

        ClassHierarchy.rebuildAll(classes.values(), classes::get);
    }

    @Test
    void hierarchyKeepsAncestorsAndDescendantsDirectionalAndTransitive() {
        ClassInput rootInterface = classes.get("sample/RootInterface");
        ClassInput base = classes.get("sample/Base");
        ClassInput left = classes.get("sample/Left");
        ClassInput leaf = classes.get("sample/Leaf");
        ClassInput right = classes.get("sample/Right");

        assertTrue(leaf.getClassHierarchy().getExtending().containsAll(
                Set.of(left, base, classes.get("sample/ChildInterface"), rootInterface)));
        assertFalse(left.getClassHierarchy().getExtending().contains(right));
        assertTrue(base.getClassHierarchy().getInheritors().containsAll(Set.of(left, leaf, right)));
        assertFalse(base.getClassHierarchy().getExtending().contains(left));
        assertTrue(rootInterface.getClassHierarchy().getInheritors().contains(leaf));
    }

    @Test
    void dynamicInvocationIncludesOnlyOverridesReachableFromSymbolicOwner() {
        assertEquals(Set.of("sample/Base", "sample/Left", "sample/Right"),
                invocationOwners(Opcodes.INVOKEVIRTUAL, "sample/Base", "run"));
        assertEquals(Set.of("sample/Left"),
                invocationOwners(Opcodes.INVOKEVIRTUAL, "sample/Left", "run"));
        assertEquals(Set.of("sample/Left"),
                invocationOwners(Opcodes.INVOKEVIRTUAL, "sample/Leaf", "run"));
        assertEquals(Set.of("sample/Base"),
                invocationOwners(Opcodes.INVOKESPECIAL, "sample/Base", "run"));
    }

    @Test
    void interfaceInvocationFindsImplementationsAcrossTransitiveInterfaces() {
        assertEquals(Set.of("sample/RootInterface", "sample/Base", "sample/Left", "sample/Right"),
                invocationOwners(Opcodes.INVOKEINTERFACE, "sample/RootInterface", "run"));
    }

    @Test
    void maximallySpecificInterfaceDeclarationsReplaceTheirParents() {
        ClassInput childInterface = classes.get("sample/ChildInterface");
        MethodInput childDeclaration = addMethod(childInterface,
                method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "run", "()V"));
        ClassInput interfaceOnly = type("sample/InterfaceOnly", "java/lang/Object",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                new String[]{childInterface.getRealName()});
        ClassHierarchy.rebuildAll(classes.values(), classes::get);

        assertEquals(Set.of(childDeclaration), Set.copyOf(MemberResolver.resolveMethods(
                classes::get, interfaceOnly, "run", "()V")));
    }

    @Test
    void unrelatedInterfaceDeclarationsBothReceiveAmbiguousReferences() {
        ClassInput first = type("sample/First", null, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE,
                new String[0], method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "accept", "()V"));
        ClassInput second = type("sample/Second", null, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE,
                new String[0], method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "accept", "()V"));
        type("sample/Ambiguous", "java/lang/Object", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                new String[]{first.getRealName(), second.getRealName()});
        ClassHierarchy.rebuildAll(classes.values(), classes::get);

        assertEquals(Set.of("sample/First", "sample/Second"),
                invocationOwners(Opcodes.INVOKEVIRTUAL, "sample/Ambiguous", "accept"));
    }

    @Test
    void staticPrivateAndConstructorLookalikesAreNotOverrideLinked() {
        MethodInput basePrivate = classes.get("sample/Base").getDeclaredMethod("privateMethod", "()V");
        MethodInput leftPrivate = classes.get("sample/Left").getDeclaredMethod("privateMethod", "()V");
        assertNull(basePrivate.getMethodHierarchy());
        assertFalse(leftPrivate.getMethodHierarchy().getLinkedMethods().contains(basePrivate));
        assertNull(classes.get("sample/Base").getDeclaredMethod("<init>", "()V").getMethodHierarchy());
        assertNull(classes.get("sample/Left").getDeclaredMethod("<init>", "()V").getMethodHierarchy());
        assertEquals(Set.of("sample/Base"),
                invocationOwners(Opcodes.INVOKESTATIC, "sample/Leaf", "utility"));
    }

    @Test
    void invokespecialUsesAccSuperSelectionFromTheDirectSuperclass() {
        MethodInsnNode instruction = new MethodInsnNode(
                Opcodes.INVOKESPECIAL, "sample/Base", "run", "()V", false);
        Set<String> owners = MemberResolver.resolveInvocationTargets(
                        classes::get, classes.get("sample/Leaf"), instruction)
                .stream().map(method -> method.getOwningClass().getRealName()).collect(Collectors.toSet());

        assertEquals(Set.of("sample/Left"), owners);
    }

    @Test
    void fieldResolutionHonorsJvmDeclarationOrderAndFieldHiding() {
        ClassInput rootInterface = classes.get("sample/RootInterface");
        FieldInput interfaceField = field(rootInterface, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "value", "I");
        FieldInput baseField = field(classes.get("sample/Base"), Opcodes.ACC_PUBLIC, "value", "I");
        ClassInput child = classes.get("sample/Leaf");
        ClassInput directInterfaceChild = type("sample/DirectInterfaceChild", "sample/Base", Opcodes.ACC_PUBLIC,
                new String[]{"sample/RootInterface"});

        assertSame(interfaceField, MemberResolver.resolveField(
                classes::get, directInterfaceChild, "value", "I"));
        assertSame(baseField, MemberResolver.resolveField(classes::get, child, "value", "I"));

        FieldInput childField = field(child, Opcodes.ACC_PUBLIC, "value", "I");
        assertSame(childField, MemberResolver.resolveField(classes::get, child, "value", "I"));
        assertSame(baseField, MemberResolver.resolveField(
                classes::get, classes.get("sample/Right"), "value", "I"));
    }

    @Test
    void packagePrivateMethodsOnlyLinkWithinTheirPackage() {
        ClassInput packageBase = type("one/PackageBase", "java/lang/Object", Opcodes.ACC_PUBLIC,
                new String[0], method(0, "work", "()V"));
        ClassInput samePackage = type("one/SamePackage", "one/PackageBase", Opcodes.ACC_PUBLIC,
                new String[0], method(Opcodes.ACC_PUBLIC, "work", "()V"));
        ClassInput otherPackage = type("two/OtherPackage", "one/PackageBase", Opcodes.ACC_PUBLIC,
                new String[0], method(Opcodes.ACC_PUBLIC, "work", "()V"));
        ClassHierarchy.rebuildAll(classes.values(), classes::get);

        assertSame(packageBase.getDeclaredMethod("work", "()V").getMethodHierarchy(),
                samePackage.getDeclaredMethod("work", "()V").getMethodHierarchy());
        assertFalse(otherPackage.getDeclaredMethod("work", "()V").getMethodHierarchy().getLinkedMethods()
                .contains(packageBase.getDeclaredMethod("work", "()V")));
    }

    private Set<String> invocationOwners(int opcode, String owner, String name) {
        return MemberResolver.resolveInvocationTargets(classes::get,
                        new MethodInsnNode(opcode, owner, name, "()V", opcode == Opcodes.INVOKEINTERFACE))
                .stream().map(method -> method.getOwningClass().getRealName()).collect(Collectors.toSet());
    }

    private ClassInput type(String name, String superName, int access, String[] interfaces,
                            MethodNode... methods) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.name = name;
        node.superName = superName;
        node.access = access;
        node.interfaces.addAll(java.util.List.of(interfaces));
        ClassTarget target = new ClassTarget(name, 0);
        ClassInput input = new ClassInput(null, node, target);
        target.setInput(input);
        for (MethodNode method : methods) {
            node.methods.add(method);
            input.addInput(new MethodInput(method, input));
        }
        classes.put(name, input);
        return input;
    }

    private static MethodNode method(int access, String name, String descriptor) {
        return new MethodNode(access, name, descriptor, null, null);
    }

    private static FieldInput field(ClassInput owner, int access, String name, String descriptor) {
        FieldNode node = new FieldNode(access, name, descriptor, null, null);
        owner.getNode().fields.add(node);
        FieldInput input = new FieldInput(node, owner);
        owner.addInput(input);
        return input;
    }

    private static MethodInput addMethod(ClassInput owner, MethodNode node) {
        owner.getNode().methods.add(node);
        MethodInput input = new MethodInput(node, owner);
        owner.addInput(input);
        return input;
    }
}
