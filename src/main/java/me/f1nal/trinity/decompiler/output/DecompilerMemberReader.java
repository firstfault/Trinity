package me.f1nal.trinity.decompiler.output;

import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.DecompiledMethod;
import me.f1nal.trinity.decompiler.output.impl.*;
import me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.assembler.line.MethodOpcodeSource;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerComponent;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.Stopwatch;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer.TAG_END;
import static me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer.TAG_START;

public class DecompilerMemberReader {
    private final DecompiledClass decompiledClass;
    private DecompiledMethod currentMethod;
    private final List<DecompilerComponent> componentList = new ArrayList<>();
    private List<AbstractInsnNode> instructionsLinkedToComponent;
    private List<AbstractInsnNode> lastInstructionsLinkedToComponent;

    public DecompilerMemberReader(DecompiledClass decompiledClass, String rawOutput) throws IOException {
        this.decompiledClass = decompiledClass;
        Stopwatch stopwatch = new Stopwatch();
        this.decode(rawOutput);
        if (stopwatch.hasPassed(100L)) {
            Logging.warn("Decoding decompiler output took way too long! {}ms", stopwatch.getDifference());
        }
    }/*

    private DecompilerComponent convertToComponent(String text, OutputMember outputMember) throws IOException {
        final Trinity trinity = decompiledClass.getTrinity();

        if (outputMember instanceof MethodOutputMember) {
            MethodOutputMember member = (MethodOutputMember) outputMember;
            MemberDetails details = new MemberDetails(member);
            return new MethodComponent(text, trinity.getExecution().getMethod(details), details, decompiledClass);
        } else if (outputMember instanceof FieldOutputMember) {
            FieldOutputMember member = (FieldOutputMember) outputMember;
            MemberDetails details = new MemberDetails(member);
            return new FieldComponent(text, trinity.getExecution().getField(details), details);
        } else if (outputMember instanceof ClassOutputMember) {
            String className = ((ClassOutputMember) outputMember).getClassName();
            ClassInput classInput = trinity.getExecution().getClassInput(className);
            return new ClassComponent(text, className, ((ClassOutputMember) outputMember).isImport(), classInput, trinity);
        } else if (outputMember instanceof StringOutputMember) {
            return new StringComponent(text);
        } else if (outputMember instanceof PackageOutputMember) {
            if (((PackageOutputMember) outputMember).isParent()) {
                return new PackageKeywordComponent(decompiledClass.getClassInput().getClassTarget());
            }
            return new PackageComponent(decompiledClass.getClassInput());
        } else if (outputMember instanceof VariableOutputMember) {
            VariableOutputMember member = (VariableOutputMember) outputMember;
            Variable variable = member.getVar() == 0 || this.currentMethod == null ? null : this.currentMethod.getMethodInput().getVariableTable().getVariable(member.getVar());
            return new VariableComponent(text, variable, member.getVar(), member.getType());
        } else if (outputMember instanceof KeywordOutputMember) {
            return new KeywordComponent(text);
        } else if (outputMember instanceof ConstOutputMember) {
            return new NumberComponent(text, ((ConstOutputMember) outputMember).getNumber());
        } else if (outputMember instanceof CommentOutputMember) {
            return new CommentComponent(text);
        } else if (outputMember instanceof FieldDeclarationOutputMember) {
            FieldInput field = trinity.getExecution().getField(new MemberDetails((FieldDeclarationOutputMember) outputMember));
            return field == null ? null : new InputStartComponent(field);
        } else if (outputMember instanceof MethodStartEndOutputMember) {
            MethodStartEndOutputMember mse = (MethodStartEndOutputMember) outputMember;
            if (mse.isStart()) {
                ClassInput classInput = trinity.getExecution().getClassInput(mse.getOwner());
                if (classInput == null) {
                    throw new NullPointerException(String.format("Class input is null for %s.", mse.getOwner()));
                }
                this.currentMethod = decompiledClass.createMethod(Objects.requireNonNull(classInput).createMethod(mse.getName(), mse.getDesc()));
                if (currentMethod != null) {
                    return new InputStartComponent(this.currentMethod.getMethodInput());
                }
            } else {
                if (this.currentMethod == null) {
                    throw new RuntimeException();
                }
                this.currentMethod = null;
            }
            return null;
        }
        throw new IOException("Don't know how to handle member type " + outputMember.getClass().getSimpleName());
    }*/

    private void handleBytecodeMarker(BytecodeMarkerOutputMember marker) {
        ClassInput classInput = decompiledClass.getClassInput();
        for (MethodInput methodInput : classInput.getMethodMap().values()) {
            if (marker.getMethod() != BytecodeMarkerOutputMember.getHashcode(classInput.getFullName(), methodInput.getDescriptor(), methodInput.getName())) {
                continue;
            }
            DecompiledMethod method = decompiledClass.createMethod(methodInput);
            if (method.getOpcodeMap().containsKey(marker.getOffsetFromStart())) {
                new IllegalStateException(String.format("Already have this bytecode at %d!", marker.getOffsetFromStart())).printStackTrace();
                continue;
            }
            if (this.instructionsLinkedToComponent == null) {
                this.instructionsLinkedToComponent = new ArrayList<>();
            }
            method.getOpcodeMap().put(marker.getOffsetFromStart(), new MethodOpcodeSource(this.instructionsLinkedToComponent, marker.getOpcode()));
            return;
        }
        Logging.warn("Cannot find method input by hash {}", marker.getMethod());
    }

    private void setBytecodeMarkers() {
        for (DecompiledMethod method : this.decompiledClass.getMethods()) {
            this.setBytecodeMarkers(method);
        }
    }

    private void setBytecodeMarkers(DecompiledMethod method) {
        int index = 0;
        for (AbstractInsnNode instruction : method.getMethodInput().getInstructions()) {
            if (instruction.getOpcode() == -1) {
                continue;
            }
            MethodOpcodeSource opcodeSource = method.getOpcodeMap().get(index);
            this.parseOpcode(method, opcodeSource, index, instruction);
            ++index;
        }
    }

    private void parseOpcode(DecompiledMethod method, MethodOpcodeSource opcodeSource, int index, AbstractInsnNode instruction) {
        if (opcodeSource == null) {
//            new IllegalStateException(String.format("Opcode doesn't exist! %s at %d", NameUtil.getOpcodeName(instruction.getOpcode()), index)).printStackTrace();
            return;
        }
      /*  if (instruction.getOpcode() == Opcodes.NOP || instruction.getOpcode() == Opcodes.ATHROW || instruction.getOpcode() == Opcodes.GOTO) {
            // Fernflower ignores these instructions (possibly beacuse of way it handles edges, TODO)
            return;
        } else if (opcodeSource.getOpcode() != instruction.getOpcode()) {
            final int opcode = opcodeSource.getOpcode();
            if (instruction.getOpcode() == Opcodes.LDC && opcode >= Opcodes.LDC && opcode <= Opcodes.LDC + 3) {
                // This is ok because wide instructions
            } else if (instruction.getOpcode() >= Opcodes.ICONST_M1 && instruction.getOpcode() <= Opcodes.ICONST_5 && opcode == Opcodes.BIPUSH) {
                // Fernflower expands iconst_x to bipush
            } else {
                new IllegalStateException(String.format("Opcode doesn't match! %s / %s at %d", NameUtil.getOpcodeName(instruction.getOpcode()), NameUtil.getOpcodeName(opcode), index)).printStackTrace();
                return;
            }
        }*/

        opcodeSource.getComponentLinkedInstructions().add(instruction);
    }

    /**
     * Decodes a list of visual components from the text output.
     * @param rawOutput Raw output from the decompiler.
     * @return List of visual components to draw.
     * @throws IOException If an unrecoverable error occurred while decoding (bad input).
     */
    private void decode(String rawOutput) throws IOException {
        int index = 0;

        while (true) {
            final int start = rawOutput.indexOf(TAG_START, index);
            if (start == -1) {
                break;
            }

            if (index != start) {
                this.addComponent(new DecompilerComponent(rawOutput.substring(index, start)));
            }

            final int end = rawOutput.indexOf(TAG_END, start);
            index = end + TAG_END.length();

            final OutputMember outputMember = OutputMemberSerializer.deserialize(rawOutput.substring(start + TAG_START.length(), end));
            String componentText = rawOutput.substring(index, (index = index + outputMember.getLength()));

            // Should fix these at some point...
            while (componentText.contains(TAG_START)) componentText = sanityCheckEncoded(outputMember, componentText);

            if (outputMember instanceof BytecodeMarkerOutputMember) {
                this.handleBytecodeMarker((BytecodeMarkerOutputMember) outputMember);
                continue;
            }
            if (outputMember instanceof MethodStartEndOutputMember) {
                this.processMethodStartEnd((MethodStartEndOutputMember) outputMember);
            }

            DecompilerComponent component = new DecompilerComponent(componentText);
            DecompilerComponentInitializer initializer = new DecompilerComponentInitializer(decompiledClass.getTrinity(), component, componentText, decompiledClass.getClassInput(), currentMethod);
            outputMember.visit(initializer);
            this.addComponent(component);

        }

        if (index != rawOutput.length()) {
            this.addComponent(new DecompilerComponent(rawOutput.substring(index)));
        }

        this.setBytecodeMarkers();
    }

    private void processMethodStartEnd(MethodStartEndOutputMember startEnd) {
        if (startEnd.isStart()) {
            ClassInput classInput = decompiledClass.getTrinity().getExecution().getClassInput(startEnd.getOwner());
            if (classInput == null) {
                throw new NullPointerException(String.format("Class input is null for %s.", startEnd.getOwner()));
            }
            this.currentMethod = decompiledClass.createMethod(Objects.requireNonNull(classInput).getMethod(startEnd.getName(), startEnd.getDesc()));
        } else {
            if (this.currentMethod == null) {
                throw new RuntimeException();
            }
            this.currentMethod = null;
        }
    }

    private void addComponent(DecompilerComponent component) {
//        if (this.instructionsLinkedToComponent != null) {
//            component.setLinkedInstructions(this.lastInstructionsLinkedToComponent = this.instructionsLinkedToComponent);
//            this.instructionsLinkedToComponent = null;
//        }

        this.componentList.add(component);
    }

    public List<DecompilerComponent> getComponentList() {
        return componentList;
    }

    private String sanityCheckEncoded(OutputMember currentOutputMember, String targetString) {
        final int start = targetString.indexOf(TAG_START);
        if (start == -1) {
            return targetString;
        }
        final int end = targetString.indexOf(TAG_END);
        if (end == -1) {
            return targetString;
        }
        OutputMember outputMember = null;

        try {
            outputMember = OutputMemberSerializer.deserialize(targetString.substring(start + TAG_START.length(), end));
        } catch (IOException e) {
            Logging.warn("Unrecognizable tag is inside of another tag {}.", currentOutputMember);
        }

        if (outputMember != null) {
//            Logging.warn("Tag {} is inside of another tag {}.", outputMember, currentOutputMember);
        }

        return targetString.substring(0, start) + targetString.substring(end + TAG_END.length());
    }
}
