package me.f1nal.trinity.util;

import org.objectweb.asm.Type;

public class TypeUtil {
    public static String getTypeDesc(String className) {
        return String.format("L%s;", className);
    }

    public static boolean isPrimitive(Type type) {
        int sort = type.getSort();
        return sort > Type.VOID && sort < Type.ARRAY;
    }

    public static int getSizeInBytes(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
                return 1;
            case Type.SHORT:
            case Type.CHAR:
                return 2;
            case Type.INT:
            case Type.FLOAT:
                return 4;
            case Type.LONG:
            case Type.DOUBLE:
                return 8;
            default:
                throw new RuntimeException("Unknown type size: " + type.toString());
        }
    }
}
