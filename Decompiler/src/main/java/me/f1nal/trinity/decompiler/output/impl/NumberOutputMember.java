package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;
import me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NumberOutputMember extends OutputMember {
    private ConstType type;
    private Number number;

    public NumberOutputMember(int length) {
        super(length);
    }

    public NumberOutputMember(int length, ConstType type, Number value) {
        super(length);
        this.type = type;
        this.number = value;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(type.ordinal());
        type.serialize(dataOutput, this.number);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        type = ConstType.values()[dataInput.readByte()];
        number = type.deserialize(dataInput);
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitNumber(this);
    }

    public static String getConst(String string, ConstType type, Number value) {
        return OutputMemberSerializer.tag(string, l -> new NumberOutputMember(l, type, value));
    }

    public ConstType getType() {
        return type;
    }

    public Number getNumber() {
        return number;
    }

    public enum ConstType {
        CHAR(new Coder() {
            @Override
            public void serialize(DataOutput output, Number value) throws IOException {
                output.writeShort(value.shortValue());
            }

            @Override
            public Number deserialize(DataInput input) throws IOException {
                return input.readShort();
            }
        }),
        SHORT(CHAR.coder),
        INTEGER(new Coder() {
            @Override
            public void serialize(DataOutput output, Number value) throws IOException {
                output.writeInt(value.intValue());
            }

            @Override
            public Number deserialize(DataInput input) throws IOException {
                return input.readInt();
            }
        }),
        FLOAT(new Coder() {
            @Override
            public void serialize(DataOutput output, Number value) throws IOException {
                output.writeFloat(value.floatValue());
            }

            @Override
            public Number deserialize(DataInput input) throws IOException {
                return input.readFloat();
            }
        }),
        LONG(new Coder() {
            @Override
            public void serialize(DataOutput output, Number value) throws IOException {
                output.writeLong(value.longValue());
            }

            @Override
            public Number deserialize(DataInput input) throws IOException {
                return input.readLong();
            }
        }),
        DOUBLE(new Coder() {
            @Override
            public void serialize(DataOutput output, Number value) throws IOException {
                output.writeDouble(value.doubleValue());
            }

            @Override
            public Number deserialize(DataInput input) throws IOException {
                return input.readDouble();
            }
        })
        ;

        private final Coder coder;

        ConstType(Coder coder) {
            this.coder = coder;
        }

        public void serialize(DataOutput dataOutput, Number value) throws IOException {
            coder.serialize(dataOutput, value);
        }

        public Number deserialize(DataInput dataInput) throws IOException {
            return coder.deserialize(dataInput);
        }

        private static interface Coder {
            void serialize(DataOutput output, Number value) throws IOException;
            Number deserialize(DataInput input) throws IOException;
        }
    }
}
