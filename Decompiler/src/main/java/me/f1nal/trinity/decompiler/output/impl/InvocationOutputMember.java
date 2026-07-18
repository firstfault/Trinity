package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A method reference that represents a concrete invocation expression.
 *
 * <p>The regular {@link MethodOutputMember} intentionally contains only the
 * target member identity. This subtype also transports call-site information
 * that is otherwise lost when Fernflower's text output is decoded by the UI.</p>
 */
public class InvocationOutputMember extends MethodOutputMember {
    private String invocationType;
    private String functionType;
    private boolean staticInvocation;
    private boolean boxingCall;
    private boolean unboxingCall;
    private boolean varArgsCall;
    private int expressionId;
    private String receiverExpression;
    private String receiverType;
    private String receiverKind;
    private String dynamicClassSuffix;
    private List<Argument> arguments = new ArrayList<>();
    private List<String> bootstrapArguments = new ArrayList<>();
    private List<Integer> bytecodeOffsets = new ArrayList<>();

    public InvocationOutputMember(int length) {
        super(length);
    }

    public InvocationOutputMember(int length,
                                  String ownerName,
                                  String methodName,
                                  String methodDescriptor,
                                  String invocationType,
                                  String functionType,
                                  boolean staticInvocation,
                                  boolean boxingCall,
                                  boolean unboxingCall,
                                  boolean varArgsCall,
                                  int expressionId,
                                  String receiverExpression,
                                  String receiverType,
                                  String receiverKind,
                                  String dynamicClassSuffix,
                                  List<Argument> arguments,
                                  List<String> bootstrapArguments,
                                  List<Integer> bytecodeOffsets) {
        super(length, ownerName, methodName, methodDescriptor);
        this.invocationType = invocationType;
        this.functionType = functionType;
        this.staticInvocation = staticInvocation;
        this.boxingCall = boxingCall;
        this.unboxingCall = unboxingCall;
        this.varArgsCall = varArgsCall;
        this.expressionId = expressionId;
        this.receiverExpression = receiverExpression;
        this.receiverType = receiverType;
        this.receiverKind = receiverKind;
        this.dynamicClassSuffix = dynamicClassSuffix;
        this.arguments = new ArrayList<>(arguments);
        this.bootstrapArguments = new ArrayList<>(bootstrapArguments);
        this.bytecodeOffsets = new ArrayList<>(bytecodeOffsets);
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        super.serializeImpl(dataOutput);
        dataOutput.writeUTF(invocationType);
        dataOutput.writeUTF(functionType);
        dataOutput.writeBoolean(staticInvocation);
        dataOutput.writeBoolean(boxingCall);
        dataOutput.writeBoolean(unboxingCall);
        dataOutput.writeBoolean(varArgsCall);
        dataOutput.writeInt(expressionId);
        writeNullable(dataOutput, receiverExpression);
        writeNullable(dataOutput, receiverType);
        writeNullable(dataOutput, receiverKind);
        writeNullable(dataOutput, dynamicClassSuffix);

        dataOutput.writeInt(arguments.size());
        for (Argument argument : arguments) {
            argument.serialize(dataOutput);
        }

        dataOutput.writeInt(bootstrapArguments.size());
        for (String bootstrapArgument : bootstrapArguments) {
            dataOutput.writeUTF(bootstrapArgument);
        }

        dataOutput.writeInt(bytecodeOffsets.size());
        for (Integer bytecodeOffset : bytecodeOffsets) {
            dataOutput.writeInt(bytecodeOffset);
        }
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        super.deserializeImpl(dataInput);
        invocationType = dataInput.readUTF();
        functionType = dataInput.readUTF();
        staticInvocation = dataInput.readBoolean();
        boxingCall = dataInput.readBoolean();
        unboxingCall = dataInput.readBoolean();
        varArgsCall = dataInput.readBoolean();
        expressionId = dataInput.readInt();
        receiverExpression = readNullable(dataInput);
        receiverType = readNullable(dataInput);
        receiverKind = readNullable(dataInput);
        dynamicClassSuffix = readNullable(dataInput);

        arguments = new ArrayList<>();
        int argumentCount = dataInput.readInt();
        for (int i = 0; i < argumentCount; i++) {
            arguments.add(Argument.deserialize(dataInput));
        }

        bootstrapArguments = new ArrayList<>();
        int bootstrapArgumentCount = dataInput.readInt();
        for (int i = 0; i < bootstrapArgumentCount; i++) {
            bootstrapArguments.add(dataInput.readUTF());
        }

        bytecodeOffsets = new ArrayList<>();
        int bytecodeOffsetCount = dataInput.readInt();
        for (int i = 0; i < bytecodeOffsetCount; i++) {
            bytecodeOffsets.add(dataInput.readInt());
        }
    }

    private static void writeNullable(DataOutput dataOutput, String value) throws IOException {
        dataOutput.writeBoolean(value != null);
        if (value != null) {
            dataOutput.writeUTF(value);
        }
    }

    private static String readNullable(DataInput dataInput) throws IOException {
        return dataInput.readBoolean() ? dataInput.readUTF() : null;
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitInvocation(this);
    }

    public String getInvocationType() {
        return invocationType;
    }

    public String getFunctionType() {
        return functionType;
    }

    public boolean isStaticInvocation() {
        return staticInvocation;
    }

    public boolean isBoxingCall() {
        return boxingCall;
    }

    public boolean isUnboxingCall() {
        return unboxingCall;
    }

    public boolean isVarArgsCall() {
        return varArgsCall;
    }

    public int getExpressionId() {
        return expressionId;
    }

    public String getReceiverExpression() {
        return receiverExpression;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public String getReceiverKind() {
        return receiverKind;
    }

    public String getDynamicClassSuffix() {
        return dynamicClassSuffix;
    }

    public List<Argument> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public List<String> getBootstrapArguments() {
        return Collections.unmodifiableList(bootstrapArguments);
    }

    public List<Integer> getBytecodeOffsets() {
        return Collections.unmodifiableList(bytecodeOffsets);
    }

    public static final class Argument {
        private final String expression;
        private final String inferredType;
        private final String declaredType;
        private final String expressionKind;

        public Argument(String expression, String inferredType, String declaredType, String expressionKind) {
            this.expression = expression;
            this.inferredType = inferredType;
            this.declaredType = declaredType;
            this.expressionKind = expressionKind;
        }

        private void serialize(DataOutput dataOutput) throws IOException {
            dataOutput.writeUTF(expression);
            dataOutput.writeUTF(inferredType);
            dataOutput.writeUTF(declaredType);
            dataOutput.writeUTF(expressionKind);
        }

        private static Argument deserialize(DataInput dataInput) throws IOException {
            return new Argument(dataInput.readUTF(), dataInput.readUTF(), dataInput.readUTF(), dataInput.readUTF());
        }

        public String getExpression() {
            return expression;
        }

        public String getInferredType() {
            return inferredType;
        }

        public String getDeclaredType() {
            return declaredType;
        }

        public String getExpressionKind() {
            return expressionKind;
        }
    }
}
