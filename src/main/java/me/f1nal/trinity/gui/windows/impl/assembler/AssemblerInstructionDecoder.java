package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.assembler.args.*;
import me.f1nal.trinity.gui.windows.impl.assembler.line.InstructionReferenceArrow;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AssemblerInstructionDecoder {
    private final AssemblerFrame assemblerFrame;
    private final MethodInput methodInput;

    public AssemblerInstructionDecoder(AssemblerFrame assemblerFrame, MethodInput methodInput) {
        this.assemblerFrame = assemblerFrame;
        this.methodInput = methodInput;
    }

    public InstructionList buildInstructions(InsnList il) {
        InstructionList instructions = new InstructionList();
        Map<InstructionComponent, AbstractInsnNode> mappedInstructions = new LinkedHashMap<>();
        for (AbstractInsnNode insnNode : il) {
            InstructionComponent component = this.translateInstruction(insnNode);
            instructions.add(component);
            mappedInstructions.put(component, insnNode);
        }
        AtomicReference<Float> hue = new AtomicReference<>(0.3F);
        List<InstructionReferenceArrow> arrowList = instructions.getInstructionReferenceArrowList();

        for (InstructionComponent instruction : instructions) {
            for (InstructionOperand argument : instruction.getOperands()) {
                if (argument instanceof LabelArgument) {
                    LabelNode label = ((LabelArgument) argument).getLabelNode();
                    InstructionComponent meaningfulInstruction = getNextMeaningfulInstruction(label, instructions);
                    if (meaningfulInstruction == null) {
                        continue;
                    }
                    InstructionComponent to = Objects.requireNonNull(meaningfulInstruction);
                    int rgb = CodeColorScheme.getRgb(new Color(Color.HSBtoRGB(hue.get(), 0.75F, 0.963F)), 100);
                    arrowList.add(new InstructionReferenceArrow(instruction, to, rgb, methodInput.getLabelTable().getLabel(label.getLabel()), ((LabelArgument) argument).getUpdateLabel()));
                    hue.updateAndGet(v -> v + 0.21F);
                }
            }
        }

        int maxDepth = 0;
        boolean[] intersects = new boolean[1];
        do {
            intersects[0] = false;
            for (InstructionReferenceArrow arrow : arrowList) {
                int from = instructions.indexOf(arrow.getFrom()), to = instructions.indexOf(arrow.getTo());
                arrow.setDepth(this.getArrowsInside(instructions, arrow, from, to, arrow.getDepth(), intersects));
                maxDepth = Math.max(maxDepth, arrow.getDepth());
            }
        } while (intersects[0]);
        instructions.setMaximumReferenceArrowDepth(maxDepth);
        instructions.removeIf(insn -> insn.getInstruction().getOpcode() == -1);
        return instructions;
    }

    private InstructionComponent getNextMeaningfulInstruction(AbstractInsnNode label, InstructionList instructions) {
        int index = instructions.indexOfInsn(label);
        if (index == -1) {
            return null;
        }
        InstructionComponent component;
        while (index != instructions.size() && (component = instructions.get(index)) != null) {
            if (component.getInstruction().getOpcode() != -1) {
                return component;
            }
            ++index;
        }
        return null;
    }

    private int getArrowsInside(InstructionList instructions, InstructionReferenceArrow exclude, int start, int end, int depth, boolean[] intersects) {
        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            if (arrow == exclude) continue;

            while (true) {
                final int from = instructions.indexOf(arrow.getFrom()), to = instructions.indexOf(arrow.getTo());
                final Line2D line = new Line2D.Double(start, depth, end, depth);
                final Line2D otherLine = new Line2D.Double(from, arrow.getDepth(), to, arrow.getDepth());
                if (line.intersectsLine(otherLine)) {
                    intersects[0] = true;
                    ++depth;
                    continue;
                }
                break;
            }
        }

        return depth;
    }

    public InstructionComponent translateInstruction(AbstractInsnNode insnNode) {
        final InstructionComponent component = new InstructionComponent(this.getOpcodeName(insnNode), insnNode);
        final List<InstructionOperand> arguments = component.getOperands();
        if (insnNode instanceof MethodInsnNode min) {
            arguments.add(new DetailsArgument(new MemberDetails(min.owner, min.name, min.desc), true));
        } else if (insnNode instanceof FieldInsnNode fin) {
            arguments.add(new DetailsArgument(new MemberDetails(fin.owner, fin.name, fin.desc), false));
        } else if (insnNode instanceof VarInsnNode vin) {
            arguments.add(new VariableArgument(this.methodInput.getVariableTable().getVariable(vin.var)));
        } else if (insnNode instanceof LabelNode ln) {
//            arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput, ln));
        } else if (insnNode instanceof JumpInsnNode jin) {
            arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput, jin.label, (newNode, labelNode) -> {
                ((JumpInsnNode) newNode).label = labelNode;
            }));
        } else if (insnNode instanceof LookupSwitchInsnNode lookupSwitch) {
            arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput, lookupSwitch.dflt, (newLookupSwitch, labelNode) -> {
                ((LookupSwitchInsnNode)newLookupSwitch).dflt = labelNode;
            }));

            for (Integer key : lookupSwitch.keys) {
                arguments.add(new NumberArgument(key));
            }

            List<LabelNode> labels = lookupSwitch.labels;
            for (int i = 0, labelsSize = labels.size(); i < labelsSize; i++) {
                LabelNode label = labels.get(i);

                final int index = i;
                arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput, label, (newLookupSwitch, labelNode) -> {
                    if (((LookupSwitchInsnNode) newLookupSwitch).labels == null) {
                        ((LookupSwitchInsnNode) newLookupSwitch).labels = new ArrayList<>();
                    }

                    // Should keep the same order
                    ((LookupSwitchInsnNode) newLookupSwitch).labels.set(index, labelNode);
                }));
            }
        } else if (insnNode instanceof TableSwitchInsnNode tableSwitch) {
            arguments.add(new NumberArgument(tableSwitch.min));
            arguments.add(new NumberArgument(tableSwitch.max));

            arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput, tableSwitch.dflt, (newTableSwitch, labelNode) -> {
                ((TableSwitchInsnNode)newTableSwitch).dflt = labelNode;
            }));

            List<LabelNode> labels = tableSwitch.labels;
            for (int i = 0, labelsSize = labels.size(); i < labelsSize; i++) {
                LabelNode label = labels.get(i);

                final int index = i;
                arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput, label, (newTableSwitch, labelNode) -> {
                    if (((TableSwitchInsnNode) newTableSwitch).labels == null) {
                        ((TableSwitchInsnNode) newTableSwitch).labels = new ArrayList<>();
                    }

                    // Should keep the same order
                    ((TableSwitchInsnNode) newTableSwitch).labels.set(index, labelNode);
                }));
            }
        } else if (insnNode instanceof LdcInsnNode) {
            final InstructionOperand cst = this.getCst(((LdcInsnNode)insnNode).cst);
            if (cst != null) arguments.add(cst);
        } else if (insnNode instanceof InvokeDynamicInsnNode indy) {
            arguments.add(new InvokeDynamicArgument(indy.name, indy.name));
            arguments.add(this.getCst(indy.bsm));
            for (Object arg : indy.bsmArgs) {
                InstructionOperand cst = getCst(arg);
                if (cst != null) arguments.add(cst);
            }
        } else if (insnNode instanceof TypeInsnNode) {
            arguments.add(new TypeArgument((((TypeInsnNode) insnNode).desc.replace('.', '/'))));
        } else {
            switch (insnNode.getOpcode()) {
                case Opcodes.SIPUSH:
                case Opcodes.BIPUSH:
                    arguments.add(new NumberArgument(((IntInsnNode) insnNode).operand));
                    break;
                case Opcodes.NEWARRAY:
                    arguments.add(new TypeArgument(this.getNewArrayTypeName(((IntInsnNode)insnNode).operand)));
                    break;
            }
        }
        return component;
    }

    private InstructionOperand getCst(Object cst) {
        if (cst instanceof Number) {
            return new NumberArgument((Number) cst);
        } else if (cst instanceof String) {
            return new StringArgument((String) cst);
        } else if (cst instanceof Type) {
            try {
                return new TypeArgument(((Type) cst).getClassName());
            } catch (Throwable throwable) {
                return null;
            }
        } else if (cst instanceof Handle) {
            return new HandleArgument((Handle) cst);
        }
        return null;
    }

    private static final String[] NEW_ARRAY_TYPES = new String[] {"BOOLEAN", "CHAR", "FLOAT", "DOUBLE", "BYTE", "SHORT", "INT", "LONG",};

    private String getNewArrayTypeName(int operand) {
        return NEW_ARRAY_TYPES[operand - Opcodes.T_BOOLEAN];
    }

    private String getOpcodeName(AbstractInsnNode insnNode) {
        final int opcode = insnNode.getOpcode();
        final String name = opcode != -1 ? Printer.OPCODES[opcode] : (insnNode instanceof LabelNode ? "label" : insnNode instanceof FrameNode ? "frame" : insnNode instanceof LineNumberNode ? "line" : "? " + insnNode.getClass().getSimpleName());

        return name.toLowerCase();
    }

}
