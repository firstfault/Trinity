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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        Map<AbstractInsnNode, InstructionComponent> mappedInstructions = new LinkedHashMap<>();
        for (AbstractInsnNode insnNode : il) {
            InstructionComponent component = this.translateInstruction(insnNode);
            instructions.add(component);
            mappedInstructions.put(insnNode, component);
        }
        AtomicReference<Float> hue = new AtomicReference<>(0.3F);
        List<InstructionReferenceArrow> arrowList = instructions.getInstructionReferenceArrowList();
        mappedInstructions.forEach((insnNode, from) -> {
            if (insnNode instanceof JumpInsnNode) {
                LabelNode label = ((JumpInsnNode) insnNode).label;
                InstructionComponent meaningfulInstruction = getNextMeaningfulInstruction(mappedInstructions, label, instructions);
                if (meaningfulInstruction == null) {
                    return;
                }
                InstructionComponent to = Objects.requireNonNull(meaningfulInstruction);
                int rgb = CodeColorScheme.getRgb(new Color(Color.HSBtoRGB(hue.get(), 0.75F, 0.963F)), 100);
                arrowList.add(new InstructionReferenceArrow(from, to, rgb, methodInput.getLabelTable().getLabel(label.getLabel()), (newInsn, newLabel) -> {
                    ((JumpInsnNode)newInsn).label = newLabel;
                }));
                hue.updateAndGet(v -> v + 0.21F);
            }
        });
        int maxDepth = 0;
        for (InstructionReferenceArrow arrow : arrowList) {
            int from = instructions.indexOf(arrow.getFrom()), to = instructions.indexOf(arrow.getTo());
            arrow.setDepth(this.getArrowsInside(instructions, arrow, from, to));
            maxDepth = Math.max(maxDepth, arrow.getDepth());
        }
        instructions.setMaximumReferenceArrowDepth(maxDepth);
        instructions.removeIf(insn -> insn.getInstruction().getOpcode() == -1);
        return instructions;
    }

    private InstructionComponent getNextMeaningfulInstruction(Map<AbstractInsnNode, InstructionComponent> mappedInstructions, AbstractInsnNode label, InstructionList instructions) {
        int index = instructions.indexOfInsn(label);
        if (index == -1) {
            return null;
        }
        InstructionComponent component;
        while ((component = instructions.get(index)) != null) {
            if (component.getInstruction().getOpcode() != -1) {
                return component;
            }
            ++index;
        }
        return null;
    }

    private int getArrowsInside(InstructionList instructions, InstructionReferenceArrow exclude, int start, int end) {
        int count = 0;

        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            if (arrow == exclude) continue;
            int from = instructions.indexOf(arrow.getFrom()), to = instructions.indexOf(arrow.getTo());
            if ((from >= start && from <= end) || (to >= start && to <= end) || to == end) {
                ++count;
            }
        }

        return count;
    }

    public InstructionComponent translateInstruction(AbstractInsnNode insnNode) {
        final InstructionComponent component = new InstructionComponent(this.getOpcodeName(insnNode), insnNode);
        final List<AbstractInsnArgument> arguments = component.getArguments();
        if (insnNode instanceof MethodInsnNode) {
            final MethodInsnNode min = (MethodInsnNode) insnNode;
            arguments.add(new DetailsArgument(new MemberDetails(min.owner, min.name, min.desc), true));
        } else if (insnNode instanceof FieldInsnNode) {
            final FieldInsnNode fin = (FieldInsnNode) insnNode;
            arguments.add(new DetailsArgument(new MemberDetails(fin.owner, fin.name, fin.desc), false));
        } else if (insnNode instanceof VarInsnNode) {
            final VarInsnNode vin = (VarInsnNode) insnNode;
            arguments.add(new VariableArgument(this.methodInput.getVariableTable().getVariable(vin.var)));
        } else if (insnNode instanceof LabelNode) {
            final LabelNode ln = (LabelNode) insnNode;
            arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput.getLabelTable().getLabel(ln.getLabel())));
        } else if (insnNode instanceof JumpInsnNode) {
            final JumpInsnNode jin = (JumpInsnNode) insnNode;
            arguments.add(new LabelArgument(this.assemblerFrame, this.methodInput.getLabelTable().getLabel(jin.label.getLabel())));
        } else if (insnNode instanceof LdcInsnNode) {
            final AbstractInsnArgument cst = this.getCst(((LdcInsnNode)insnNode).cst);
            if (cst != null) arguments.add(cst);
        } else if (insnNode instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insnNode;
            arguments.add(new InvokeDynamicArgument(indy.name, indy.name));
            arguments.add(this.getCst(indy.bsm));
            for (Object arg : indy.bsmArgs) {
                AbstractInsnArgument cst = getCst(arg);
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

    private AbstractInsnArgument getCst(Object cst) {
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
