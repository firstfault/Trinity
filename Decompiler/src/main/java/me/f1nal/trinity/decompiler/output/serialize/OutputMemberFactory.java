package me.f1nal.trinity.decompiler.output.serialize;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.impl.*;

public class OutputMemberFactory {
    public static Class<?> getClass(int id) {
        if (id < 0 || id >= instantiable.length) {
            return null;
        }
        return instantiable[id];
    }

    public static int getId(Class<? extends OutputMember> clazz) {
        for (int i = 0; i < instantiable.length; i++) {
            if (clazz == instantiable[i]) {
                return i;
            }
        }
        return -1;
    }

    private static final Class<?>[] instantiable = new Class[] {
            MethodOutputMember.class,
            ClassOutputMember.class,
            KeywordOutputMember.class,
            VariableOutputMember.class,
            FieldOutputMember.class,
            MethodStartEndOutputMember.class,
            ConstOutputMember.class,
            FieldDeclarationOutputMember.class,
            CommentOutputMember.class,
            PackageOutputMember.class,
            StringOutputMember.class,
            BytecodeMarkerOutputMember.class,
    };
}
