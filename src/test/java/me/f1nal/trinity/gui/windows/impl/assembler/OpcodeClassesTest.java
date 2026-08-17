package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.OpcodeClasses;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.*;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpcodeClassesTest {
    @Test
    void everyAdvertisedOpcodeHasInitializedDefaults() {
        for (String name : OpcodeClasses.getNamesToClasses().keySet()) {
            LabelNode target = new LabelNode();
            AbstractInsnNode instruction = OpcodeClasses.createDefault(name, "sample/Owner", target);
            assertNotNull(instruction, name);
            Map<LabelNode, LabelNode> labels = new IdentityHashMap<>();
            labels.put(target, target);
            if (instruction instanceof LabelNode label) labels.put(label, label);
            assertDoesNotThrow(() -> instruction.clone(labels), name);
            if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                assertNotNull(dynamic.bsm);
                assertNotNull(dynamic.bsmArgs);
            } else if (instruction instanceof TableSwitchInsnNode table) {
                assertNotNull(table.dflt);
                assertNotNull(table.labels);
            } else if (instruction instanceof LookupSwitchInsnNode lookup) {
                assertNotNull(lookup.dflt);
                assertNotNull(lookup.keys);
                assertNotNull(lookup.labels);
            } else if (instruction instanceof FrameNode frame) {
                org.junit.jupiter.api.Assertions.assertEquals(org.objectweb.asm.Opcodes.F_SAME, frame.type);
            }
        }
    }

    @Test
    void reportsInlineArgumentCounts() {
        assertEquals(0, OpcodeClasses.getArgumentCount("nop"));
        assertEquals(1, OpcodeClasses.getArgumentCount("aload"));
        assertEquals(2, OpcodeClasses.getArgumentCount("iinc"));
        assertEquals(3, OpcodeClasses.getArgumentCount("getfield"));
        assertEquals(4, OpcodeClasses.getArgumentCount("invokevirtual"));
        assertEquals(4, OpcodeClasses.getArgumentCount("invokedynamic"));
    }
}
