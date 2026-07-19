package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.execution.labels.LabelTable;
import me.f1nal.trinity.execution.labels.MethodLabel;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.ArrayList;
import java.util.function.Function;

final class EditFieldSwitch extends EditFieldText<Object> {
    private final Object instruction;
    private final LabelTable labels;
    private final Function<MethodLabel, LabelNode> resolver;

    EditFieldSwitch(TableSwitchInsnNode table, LabelTable labels, Function<MethodLabel, LabelNode> resolver) {
        super(8192, "Switch", "min=0, max=1, default=L0, labels=[L1, L2]", () -> table, value -> {
            AssemblerSwitchCodec.TableData data = (AssemblerSwitchCodec.TableData) value;
            table.min = data.min();
            table.max = data.max();
            table.dflt = data.dflt();
            table.labels = new ArrayList<>(data.labels());
        });
        this.instruction = table;
        this.labels = labels;
        this.resolver = resolver;
    }

    EditFieldSwitch(LookupSwitchInsnNode lookup, LabelTable labels, Function<MethodLabel, LabelNode> resolver) {
        super(8192, "Switch", "default=L0, cases={-1:L1, 5:L2}", () -> lookup, value -> {
            AssemblerSwitchCodec.LookupData data = (AssemblerSwitchCodec.LookupData) value;
            lookup.dflt = data.dflt();
            lookup.keys = new ArrayList<>(data.keys());
            lookup.labels = new ArrayList<>(data.labels());
        });
        this.instruction = lookup;
        this.labels = labels;
        this.resolver = resolver;
    }

    @Override
    protected Object parse(String input) throws InvalidEditInputException {
        try {
            if (instruction instanceof TableSwitchInsnNode) return AssemblerSwitchCodec.parseTable(input, labels, resolver);
            return AssemblerSwitchCodec.parseLookup(input, labels, resolver);
        } catch (IllegalArgumentException exception) {
            throw new InvalidEditInputException(exception.getMessage());
        }
    }

    @Override
    public void updateField() {
        text.set(instruction instanceof TableSwitchInsnNode table
                ? AssemblerSwitchCodec.format(table, labels)
                : AssemblerSwitchCodec.format((LookupSwitchInsnNode) instruction, labels));
    }
}
