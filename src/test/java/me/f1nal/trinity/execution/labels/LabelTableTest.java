package me.f1nal.trinity.execution.labels;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Label;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelTableTest {
    @Test
    void resetRestartsGeneratedLabelNumbers() {
        LabelTable table = new LabelTable(null);
        assertEquals("L0", table.getLabel(new Label()).getName());
        assertEquals("L1", table.getLabel(new Label()).getName());

        table.reset();

        assertEquals("L0", table.getLabel(new Label()).getName());
    }
}
