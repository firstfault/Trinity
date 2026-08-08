package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MethodImplementationBuilder;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c;
import com.android.tools.smali.dexlib2.writer.builder.BuilderField;
import com.android.tools.smali.dexlib2.writer.builder.BuilderMethod;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DexTestFixture {
    private DexTestFixture() {
    }

    public static byte[] create() throws IOException {
        return create("sample/DexMain", "native-dex");
    }

    public static byte[] create(String internalName, String constant) throws IOException {
        return create(Map.of(internalName, constant));
    }

    public static byte[] createWithField(String internalName, String fieldType)
            throws IOException {
        DexBuilder builder = new DexBuilder(Opcodes.forApi(28));
        String classDescriptor = String.format("L%s;", internalName);
        BuilderField field = builder.internField(
                classDescriptor, "dependency", String.format("L%s;", fieldType),
                AccessFlags.PUBLIC.getValue(), null, Set.of(), Set.of());
        builder.internClassDef(
                classDescriptor, AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;",
                List.of(), "DexFixture.java", Set.of(), List.of(field), List.of());

        MemoryDataStore output = new MemoryDataStore();
        builder.writeTo(output);
        return output.getData();
    }

    public static byte[] create(Map<String, String> classes) throws IOException {
        DexBuilder builder = new DexBuilder(Opcodes.forApi(28));
        for (Map.Entry<String, String> entry : classes.entrySet()) {
            String classDescriptor = String.format("L%s;", entry.getKey());
            MethodImplementationBuilder implementation = new MethodImplementationBuilder(1);
            implementation.addInstruction(new BuilderInstruction21c(
                    Opcode.CONST_STRING, 0, builder.internStringReference(entry.getValue())));
            implementation.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

            BuilderMethod method = builder.internMethod(
                    classDescriptor, "run", List.of(), "V",
                    AccessFlags.PUBLIC.getValue() | AccessFlags.STATIC.getValue(),
                    Set.of(), Set.of(), implementation.getMethodImplementation());
            builder.internClassDef(
                    classDescriptor, AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;",
                    List.of(), "DexFixture.java", Set.of(), List.of(), List.of(method));
        }

        MemoryDataStore output = new MemoryDataStore();
        builder.writeTo(output);
        return output.getData();
    }
}
