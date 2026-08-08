package me.f1nal.trinity.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {
    private static final Unsafe unsafe;

    public static Unsafe getUnsafe() {
        return unsafe;
    }

    static {
        Field field;
        try {
            field = Unsafe.class.getDeclaredField("theUnsafe");
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException(exception);
        }
        field.setAccessible(true);
        try {
            unsafe = (Unsafe) field.get(null);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }
}
