package me.f1nal.trinity.decompiler.modules.decompiler.exps;

import me.f1nal.trinity.decompiler.output.impl.BytecodeMarkerOutputMember;
import me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer;
import me.f1nal.trinity.decompiler.code.Instruction;
import me.f1nal.trinity.decompiler.main.collectors.BytecodeMappingTracer;
import me.f1nal.trinity.decompiler.util.TextBuffer;

import java.util.ArrayList;
import java.util.List;

public class TrinityBytecodeMarkerExprent extends Exprent {
    private final Instruction instruction;
    private final int methodId;

    public TrinityBytecodeMarkerExprent(Instruction instruction, int methodId) {
        super(EXPRENT_BYTECODE_MARKER);
        this.instruction = instruction;
        this.methodId = methodId;
    }

    @Override
    public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {
        return new TextBuffer(OutputMemberSerializer.serializeTags(new BytecodeMarkerOutputMember(0, methodId, instruction.opcode, instruction.offsetFromMethodStart)));
    }

    @Override
    public List<Exprent> getAllExprents() {
        return new ArrayList<>();
    }

    @Override
    public Exprent copy() {
        return new TrinityBytecodeMarkerExprent(instruction, methodId);
    }
}
