package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.execution.pattern.InstructionPatternCompiler;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.*;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssemblerClipboardCodecTest {
    @Test
    void roundTripsEveryInstructionNodeFamilyAsEditableText() {
        MethodNode source = completeInstructions();
        Map<LabelNode, String> names = labelNames(source);

        String text = AssemblerClipboardCodec.format(List.of(source.instructions.toArray()), names::get);
        AssemblerClipboardCodec.ParsedInstructions parsed = AssemblerClipboardCodec.parse(text, ignored -> null);
        MethodNode restored = method(parsed.instructions());

        assertEquals(AssemblerDocument.fingerprint(source), AssemblerDocument.fingerprint(restored));
        assertTrue(text.contains("invokevirtual \"sample/Owner\" \"method\" \"()V\" false"));
        assertTrue(text.contains("ldc condy("));
        assertTrue(text.contains("tableswitch 0 1"));
        assertFalse(text.toLowerCase().contains("version"));
        assertTrue(InstructionPatternCompiler.compile(text, true).valid());
    }

    @Test
    void parsesHandEditedAssemblyAndResolvesExistingExternalLabels() {
        LabelNode existing = new LabelNode();
        String text = """
                # This is ordinary editable assembler text.
                label "start"
                line 42 "start"
                iconst_1
                istore 0
                ldc string("edited text")
                goto "outside"
                return
                """;

        AssemblerClipboardCodec.ParsedInstructions parsed = AssemblerClipboardCodec.parse(text,
                name -> name.equals("outside") ? existing : null);

        assertEquals(7, parsed.instructions().size());
        assertEquals("edited text", ((LdcInsnNode) parsed.instructions().get(4)).cst);
        assertSame(existing, ((JumpInsnNode) parsed.instructions().get(5)).label);
        assertFalse(parsed.instructions().contains(existing));
    }

    @Test
    void createsADeclaredTargetWhenAnExternalLabelDoesNotExist() {
        AssemblerClipboardCodec.ParsedInstructions parsed = AssemblerClipboardCodec.parse(
                "goto \"missing\"", ignored -> null);

        assertEquals(2, parsed.instructions().size());
        LabelNode created = (LabelNode) parsed.instructions().get(0);
        assertSame(created, ((JumpInsnNode) parsed.instructions().get(1)).label);
        assertEquals("missing", parsed.labelNames().get(created));
    }

    @Test
    void intentionallyDoesNotCopyInstructionTypeAnnotations() {
        InsnNode instruction = new InsnNode(Opcodes.NOP);
        instruction.visibleTypeAnnotations = List.of(new TypeAnnotationNode(
                TypeReference.newTypeReference(TypeReference.CAST).getValue(), null, "Lsample/Marker;"));

        String text = AssemblerClipboardCodec.format(List.of(instruction), ignored -> null);
        AbstractInsnNode restored = AssemblerClipboardCodec.parse(text, ignored -> null).instructions().get(0);

        assertNull(restored.visibleTypeAnnotations);
        assertNull(restored.invisibleTypeAnnotations);
        assertEquals("nop", text);
    }

    @Test
    void preservesEveryCompressedFrameShape() {
        List<AbstractInsnNode> frames = List.of(
                new FrameNode(Opcodes.F_APPEND, 1, new Object[]{"java/lang/String"}, 0, null),
                new FrameNode(Opcodes.F_CHOP, 2, null, 0, null),
                new FrameNode(Opcodes.F_SAME, 0, null, 0, null),
                new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER})
        );

        String text = AssemblerClipboardCodec.format(frames, ignored -> null);
        List<AbstractInsnNode> restored = AssemblerClipboardCodec.parse(text, ignored -> null).instructions();

        assertEquals(frames.size(), restored.size());
        for (int i = 0; i < frames.size(); i++) {
            FrameNode expected = (FrameNode) frames.get(i);
            FrameNode actual = (FrameNode) restored.get(i);
            assertEquals(expected.type, actual.type);
            assertEquals(expected.local, actual.local);
            assertEquals(expected.stack, actual.stack);
        }
        assertTrue(text.contains("frame F_CHOP [CHOP, CHOP] null"));
    }

    @Test
    void reportsTheEditedLineContainingInvalidSyntax() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AssemblerClipboardCodec.parse("nop\nnot_an_opcode", ignored -> null));
        assertTrue(error.getMessage().startsWith("Line 2:"));
    }

    private static MethodNode completeInstructions() {
        MethodNode method = method(List.of());
        LabelNode start = new LabelNode();
        LabelNode firstCase = new LabelNode();
        LabelNode secondCase = new LabelNode();
        LabelNode end = new LabelNode();
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, "sample/Owner", "bootstrap",
                "()Ljava/lang/Object;", false);
        ConstantDynamic dynamic = new ConstantDynamic("dynamic", "Ljava/lang/String;", bootstrap,
                7, "argument", Type.getType("Ljava/lang/String;"));

        method.instructions.add(start);
        method.instructions.add(new LineNumberNode(10, start));
        method.instructions.add(new FrameNode(Opcodes.F_FULL, 2,
                new Object[]{Opcodes.INTEGER, "java/lang/String"}, 1, new Object[]{start}));
        method.instructions.add(new InsnNode(Opcodes.NOP));
        method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 7));
        method.instructions.add(new VarInsnNode(Opcodes.ISTORE, 0));
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Object"));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "sample/Owner", "field", "I"));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "sample/Owner", "method", "()V", false));
        method.instructions.add(new InvokeDynamicInsnNode("call", "()V", bootstrap,
                dynamic, Type.getMethodType("()V")));
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, end));
        method.instructions.add(new LdcInsnNode(dynamic));
        method.instructions.add(new IincInsnNode(0, -2));
        method.instructions.add(new TableSwitchInsnNode(0, 1, end, firstCase, secondCase));
        method.instructions.add(new LookupSwitchInsnNode(end,
                new int[]{-1, 5}, new LabelNode[]{firstCase, secondCase}));
        method.instructions.add(new MultiANewArrayInsnNode("[[Ljava/lang/Object;", 2));
        method.instructions.add(firstCase);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(secondCase);
        method.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
        method.instructions.add(end);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private static MethodNode method(List<AbstractInsnNode> instructions) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "method", "()V", null, null);
        instructions.forEach(method.instructions::add);
        method.maxStack = 0;
        method.maxLocals = 0;
        return method;
    }

    private static Map<LabelNode, String> labelNames(MethodNode method) {
        Map<LabelNode, String> names = new IdentityHashMap<>();
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label) names.put(label, "L" + index++);
        }
        return names;
    }
}
