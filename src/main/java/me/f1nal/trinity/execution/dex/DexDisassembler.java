package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.Adaptors.MethodDefinition;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;

import java.io.IOException;
import java.io.StringWriter;

/** Lossless human-readable Dalvik disassembly backed by baksmali. */
public final class DexDisassembler {
    private DexDisassembler() {
    }

    public static String disassembleClass(DexFileUnit file, ClassDef classDef) {
        BaksmaliOptions options = options(file);
        StringWriter output = new StringWriter();
        try {
            new ClassDefinition(options, classDef).writeTo(new BaksmaliWriter(output));
            return output.toString();
        } catch (IOException exception) {
            throw new IllegalStateException(String.format("Unable to disassemble DEX class %s",
                    classDef.getType()), exception);
        }
    }

    public static String disassembleMethod(DexFileUnit file, ClassDef classDef, Method method) {
        BaksmaliOptions options = options(file);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        StringWriter output = new StringWriter();
        BaksmaliWriter writer = new BaksmaliWriter(output);
        try {
            MethodImplementation implementation = method.getImplementation();
            if (implementation == null) {
                MethodDefinition.writeEmptyMethodTo(writer, method, classDefinition);
            } else {
                new MethodDefinition(classDefinition, method, implementation).writeTo(writer);
            }
            return output.toString();
        } catch (IOException exception) {
            throw new IllegalStateException(String.format("Unable to disassemble DEX method %s",
                    DexDescriptors.methodIdentity(method)), exception);
        }
    }

    private static BaksmaliOptions options(DexFileUnit file) {
        BaksmaliOptions options = new BaksmaliOptions();
        options.apiLevel = file.getDexFile().getOpcodes().api;
        options.sequentialLabels = true;
        options.codeOffsets = true;
        return options;
    }
}
