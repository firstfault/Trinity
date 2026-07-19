package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.labels.MethodLabel;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AssemblerStructuredOperandCodecTest {
    @Test
    void parsesCanonicalSwitchAndFrameSyntax() {
        ClassNode ownerNode = new ClassNode(Opcodes.ASM9);
        ownerNode.name = "sample/Owner";
        ClassTarget target = new ClassTarget(ownerNode.name, 0);
        ClassInput owner = new ClassInput(null, ownerNode, target);
        target.setInput(owner);
        MethodNode method = new MethodNode(Opcodes.ACC_STATIC, "method", "()V", null, null);
        MethodInput input = new MethodInput(method, owner);
        LabelNode l0 = new LabelNode();
        LabelNode l1 = new LabelNode();
        LabelNode l2 = new LabelNode();
        method.instructions.add(l0);
        method.instructions.add(l1);
        method.instructions.add(l2);
        input.getLabelTable().getLabel(l0.getLabel()).getNameProperty().set("L0");
        input.getLabelTable().getLabel(l1.getLabel()).getNameProperty().set("L1");
        input.getLabelTable().getLabel(l2.getLabel()).getNameProperty().set("L2");
        Map<MethodLabel, LabelNode> nodes = new IdentityHashMap<>();
        nodes.put(input.getLabelTable().getLabel(l0.getLabel()), l0);
        nodes.put(input.getLabelTable().getLabel(l1.getLabel()), l1);
        nodes.put(input.getLabelTable().getLabel(l2.getLabel()), l2);
        Function<MethodLabel, LabelNode> resolver = nodes::get;

        AssemblerSwitchCodec.TableData table = AssemblerSwitchCodec.parseTable(
                "min=0, max=1, default=L0, labels=[L1, L2]", input.getLabelTable(), resolver);
        assertEquals(0, table.min());
        assertEquals(1, table.max());
        assertSame(l0, table.dflt());
        assertEquals(java.util.List.of(l1, l2), table.labels());

        AssemblerSwitchCodec.LookupData lookup = AssemblerSwitchCodec.parseLookup(
                "default=L0, cases={-1:L1, 5:L2}", input.getLabelTable(), resolver);
        assertEquals(java.util.List.of(-1, 5), lookup.keys());
        assertEquals(java.util.List.of(l1, l2), lookup.labels());

        AssemblerFrameCodec.FrameData frame = AssemblerFrameCodec.parse(
                "type=F_FULL, locals=[INTEGER, object(\"java/lang/String\")], stack=[uninitialized(L1)]",
                input.getLabelTable(), resolver);
        assertEquals(Opcodes.F_FULL, frame.type());
        assertEquals(java.util.List.of(Opcodes.INTEGER, "java/lang/String"), frame.locals());
        assertSame(l1, frame.stack().get(0));
    }
}
