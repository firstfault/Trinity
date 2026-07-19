package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.MethodInput;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssemblerDocumentTest {
    @Test
    void completeMethodClonePreservesEveryNodeAndMetadataRelationship() {
        MethodNode source = completeMethod();
        MethodNode clone = AssemblerDocument.cloneMethod(source);

        assertEquals(AssemblerDocument.fingerprint(source), AssemblerDocument.fingerprint(clone));
        assertEquals(source.instructions.size(), clone.instructions.size());
        for (int i = 0; i < source.instructions.size(); i++) {
            assertEquals(source.instructions.get(i).getClass(), clone.instructions.get(i).getClass(), "node " + i);
        }

        LabelNode originalStart = (LabelNode) source.instructions.getFirst();
        LabelNode clonedStart = (LabelNode) clone.instructions.getFirst();
        assertNotSame(originalStart, clonedStart);
        assertSame(clonedStart, clone.tryCatchBlocks.get(0).start);
        assertSame(clonedStart, clone.localVariables.get(0).start);
        assertSame(clonedStart, clone.visibleLocalVariableAnnotations.get(0).start.get(0));

        TableSwitchInsnNode table = find(clone, TableSwitchInsnNode.class);
        assertSame(table.dflt, table.labels.get(0), "shared label identity must remain shared");
        LookupSwitchInsnNode lookup = find(clone, LookupSwitchInsnNode.class);
        assertEquals(lookup.keys.size(), lookup.labels.size());
    }

    @Test
    void cloningLargeMethodDoesNotDropInstructions() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "large", "()V", null, null);
        for (int i = 0; i < 50_000; i++) method.instructions.add(new InsnNode(Opcodes.NOP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 0;
        method.maxLocals = 0;
        MethodNode clone = AssemblerDocument.cloneMethod(method);
        assertEquals(50_001, clone.instructions.size());
        assertEquals(AssemblerDocument.fingerprint(method), AssemblerDocument.fingerprint(clone));
    }

    @Test
    void commitAtomicallyReplacesAllCodeMetadataAndTracksExternalChanges() {
        MethodNode live = completeMethod();
        ClassNode ownerNode = new ClassNode(Opcodes.ASM9);
        ownerNode.version = Opcodes.V17;
        ownerNode.name = "sample/Owner";
        ownerNode.superName = "java/lang/Object";
        ownerNode.methods.add(live);
        ClassTarget target = new ClassTarget(ownerNode.name, 0);
        ClassInput owner = new ClassInput(null, ownerNode, target);
        target.setInput(owner);
        MethodInput input = new MethodInput(live, owner);
        AssemblerDocument document = new AssemblerDocument(input);

        MethodNode candidate = document.buildCandidate(List.of(document.getInstructions().toArray()));
        candidate.maxStack = 9;
        document.replaceWorkingMethod(candidate);
        document.commit(candidate);

        assertEquals(9, live.maxStack);
        assertEquals(candidate.instructions.size(), live.instructions.size());
        assertFalse(document.hasExternalChanges());
        live.instructions.insert(new InsnNode(Opcodes.NOP));
        assertTrue(document.hasExternalChanges());
    }

    private static MethodNode completeMethod() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "all", "()V", null, null);
        LabelNode start = new LabelNode();
        LabelNode caseLabel = new LabelNode();
        LabelNode end = new LabelNode();
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, "sample/Owner", "bootstrap",
                "()Ljava/lang/invoke/CallSite;", false);
        ConstantDynamic dynamic = new ConstantDynamic("constant", "Ljava/lang/String;", bootstrap, "argument");

        method.instructions.add(start);
        method.instructions.add(new LineNumberNode(10, start));
        method.instructions.add(new FrameNode(Opcodes.F_FULL, 1, new Object[]{"java/lang/Object"}, 0, null));
        InsnNode nop = new InsnNode(Opcodes.NOP);
        TypeAnnotationNode annotation = new TypeAnnotationNode(
                TypeReference.newTypeReference(TypeReference.CAST).getValue(), null, "Lsample/Marker;");
        annotation.values = List.of("value", "marked");
        nop.visibleTypeAnnotations = List.of(annotation);
        method.instructions.add(nop);
        method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 7));
        method.instructions.add(new VarInsnNode(Opcodes.ISTORE, 0));
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Object"));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "sample/Owner", "field", "I"));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "sample/Owner", "method", "()V", false));
        method.instructions.add(new InvokeDynamicInsnNode("dynamic", "()V", bootstrap, dynamic, Type.getType("Ljava/lang/String;")));
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, end));
        method.instructions.add(new LdcInsnNode(dynamic));
        method.instructions.add(new IincInsnNode(0, 2));
        method.instructions.add(new TableSwitchInsnNode(0, 0, caseLabel, caseLabel));
        method.instructions.add(new LookupSwitchInsnNode(end, new int[]{-1, 5}, new LabelNode[]{caseLabel, end}));
        method.instructions.add(new MultiANewArrayInsnNode("[[Ljava/lang/Object;", 2));
        method.instructions.add(caseLabel);
        method.instructions.add(new InsnNode(Opcodes.NOP));
        method.instructions.add(end);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, caseLabel, "java/lang/Throwable"));
        method.localVariables = List.of(new LocalVariableNode("value", "I", null, start, end, 0));
        method.visibleLocalVariableAnnotations = List.of(new LocalVariableAnnotationNode(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE).getValue(), null,
                new LabelNode[]{start}, new LabelNode[]{end}, new int[]{0}, "Lsample/Marker;"));
        method.maxStack = 4;
        method.maxLocals = 1;
        return method;
    }

    private static <T> T find(MethodNode method, Class<T> type) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (type.isInstance(instruction)) return type.cast(instruction);
        }
        throw new AssertionError("Missing " + type.getSimpleName());
    }
}
