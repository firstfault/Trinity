package me.f1nal.trinity.execution.pattern;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.MethodInput;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstructionPatternMatcherTest {
    @Test
    void matchesExactInstructionsAndTypedOperandWildcards() {
        MethodInput method = method(new VarInsnNode(Opcodes.ALOAD, 0),
                new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "sample/Owner", "run", "()V", false),
                new InsnNode(Opcodes.RETURN));
        InstructionPattern pattern = compile("""
                aload *
                invokevirtual "sample/*" "run" * false
                """, false);

        List<InstructionPatternMatch> matches = InstructionPatternMatcher.findAll(method, pattern);

        assertEquals(1, matches.size());
        assertEquals(2, matches.get(0).instructions().size());
    }

    @Test
    void supportsHexIntegersAndEscapedQuotedWildcards() {
        MethodInput method = method(new IntInsnNode(Opcodes.BIPUSH, 16),
                new TypeInsnNode(Opcodes.NEW, "sample/*Literal"));
        InstructionPattern pattern = compile("""
                bipush 0x10
                new "sample/\\*Literal"
                """, false);

        assertEquals(1, InstructionPatternMatcher.findAll(method, pattern).size());
    }

    @Test
    void sequenceGapUsesTheShortestMatchAndIncludesOverlaps() {
        MethodInput method = method(new InsnNode(Opcodes.ICONST_0), new InsnNode(Opcodes.NOP),
                new InsnNode(Opcodes.IRETURN), new InsnNode(Opcodes.IRETURN));
        InstructionPattern pattern = compile("""
                iconst_0
                ...
                ireturn
                """, false);

        List<InstructionPatternMatch> matches = InstructionPatternMatcher.findAll(method, pattern);

        assertEquals(1, matches.size());
        assertEquals(3, matches.get(0).instructions().size());
    }

    @Test
    void repeatedPatternLabelsMustResolveToTheSameBytecodeLabel() {
        LabelNode first = new LabelNode();
        LabelNode second = new LabelNode();
        MethodInput method = method(new JumpInsnNode(Opcodes.IFEQ, first),
                new JumpInsnNode(Opcodes.GOTO, first),
                new JumpInsnNode(Opcodes.GOTO, second));
        InstructionPattern pattern = compile("""
                ifeq "target"
                goto "target"
                """, false);

        assertEquals(1, InstructionPatternMatcher.findAll(method, pattern).size());
    }

    @Test
    void metadataIsIgnoredUnlessEnabled() {
        LabelNode start = new LabelNode();
        MethodInput method = method(start, new LineNumberNode(12, start), new InsnNode(Opcodes.NOP));

        InstructionPatternCompiler.Compilation ignored = InstructionPatternCompiler.compile(
                "label \"x\"\n...\nnop", false);
        InstructionPatternCompiler.Compilation included = InstructionPatternCompiler.compile(
                "label \"x\"\n...\nnop", true);

        assertTrue(ignored.valid());
        assertTrue(ignored.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.severity() == PatternDiagnostic.Severity.WARNING));
        assertEquals(1, InstructionPatternMatcher.findAll(method, ignored.pattern()).size());
        assertEquals(1, InstructionPatternMatcher.findAll(method, included.pattern()).size());
    }

    @Test
    void reportsInvalidLinesWithoutProducingAPattern() {
        InstructionPatternCompiler.Compilation compilation =
                InstructionPatternCompiler.compile("aload\nnot_an_opcode", false);

        assertFalse(compilation.valid());
        assertTrue(compilation.diagnostics().stream().anyMatch(diagnostic -> diagnostic.line() == 1));
        assertTrue(compilation.diagnostics().stream().anyMatch(diagnostic -> diagnostic.line() == 2));
    }

    private static InstructionPattern compile(String source, boolean includeMetadata) {
        InstructionPatternCompiler.Compilation compilation =
                InstructionPatternCompiler.compile(source, includeMetadata);
        assertTrue(compilation.valid(), () -> String.valueOf(compilation.diagnostics()));
        return compilation.pattern();
    }

    private static MethodInput method(AbstractInsnNode... instructions) {
        ClassNode classNode = new ClassNode();
        classNode.name = "sample/Test";
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "test", "()V", null, null);
        for (AbstractInsnNode instruction : instructions) methodNode.instructions.add(instruction);
        classNode.methods.add(methodNode);
        ClassTarget target = new ClassTarget(classNode.name, 0);
        ClassInput classInput = new ClassInput(null, classNode, target);
        target.setInput(classInput);
        return new MethodInput(methodNode, classInput);
    }
}
