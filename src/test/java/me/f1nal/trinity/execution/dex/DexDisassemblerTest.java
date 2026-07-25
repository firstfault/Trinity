package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DexDisassemblerTest {
    @Test
    void disassemblesNativeDexWithoutJvmConversion() throws Exception {
        byte[] bytes = DexTestFixture.create();
        DexBackedDexFile dexFile = new DexBackedDexFile(null, bytes);
        ClassDef classDef = dexFile.getClasses().iterator().next();
        Method method = classDef.getMethods().iterator().next();
        DexFileUnit file = new DexFileUnit("classes.dex", bytes, dexFile);

        String classSmali = DexDisassembler.disassembleClass(file, classDef);
        String methodSmali = DexDisassembler.disassembleMethod(file, classDef, method);

        assertEquals("sample/DexMain", DexDescriptors.internalName(classDef.getType()));
        assertTrue(classSmali.contains(".class public Lsample/DexMain;"));
        assertTrue(classSmali.contains("const-string v0, \"native-dex\""));
        assertTrue(methodSmali.contains(".method public static run()V"));
        assertTrue(methodSmali.contains("return-void"));
    }
}
