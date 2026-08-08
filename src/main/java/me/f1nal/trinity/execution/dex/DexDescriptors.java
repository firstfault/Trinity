package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;

/** Descriptor conversions that preserve DEX identities without JVM translation. */
public final class DexDescriptors {
    private DexDescriptors() {
    }

    public static String internalName(String typeDescriptor) {
        if (typeDescriptor != null && typeDescriptor.length() > 2
                && typeDescriptor.charAt(0) == 'L'
                && typeDescriptor.charAt(typeDescriptor.length() - 1) == ';') {
            return typeDescriptor.substring(1, typeDescriptor.length() - 1);
        }
        return typeDescriptor;
    }

    public static String methodDescriptor(MethodReference method) {
        StringBuilder descriptor = new StringBuilder("(");
        method.getParameterTypes().forEach(descriptor::append);
        return descriptor.append(')').append(method.getReturnType()).toString();
    }

    public static String methodIdentity(Method method) {
        return String.format("%s.%s%s", internalName(method.getDefiningClass()), method.getName(),
                methodDescriptor(method));
    }
}
