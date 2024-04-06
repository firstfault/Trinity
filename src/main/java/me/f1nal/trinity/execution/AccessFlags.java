package me.f1nal.trinity.execution;

import me.f1nal.trinity.execution.access.AccessFlagsMaskProvider;
import me.f1nal.trinity.util.ModifyNotifiable;
import me.f1nal.trinity.util.ModifyPriority;
import org.objectweb.asm.Opcodes;

public final class AccessFlags {
    private final ModifyNotifiable modifyNotifiable;
    private final AccessFlagsMaskProvider provider;

    public AccessFlags(ModifyNotifiable modifyNotifiable, AccessFlagsMaskProvider provider) {
        this.modifyNotifiable = modifyNotifiable;
        this.provider = provider;
    }

    public boolean isFlag(int flag) {
        return (provider.getAccessFlagsMask() & flag) != 0;
    }

    public boolean isFlag(Flag flag) {
        return isFlag(flag.getMask());
    }

    public void toggleFlag(Flag flag) {
        if (this.isFlag(flag)) {
            this.unsetFlag(flag);
        } else {
            this.setFlag(flag);
        }
        if (this.modifyNotifiable != null) {
            this.modifyNotifiable.notifyModified(ModifyPriority.LOW);
        }
    }

    public void unsetFlag(Flag flag) {
        provider.setAccessFlagsMask(provider.getAccessFlagsMask() & ~flag.getMask());
    }

    public void setFlag(Flag flag) {
        provider.setAccessFlagsMask(provider.getAccessFlagsMask() | flag.getMask());
    }

    public boolean isStatic() {
        return isFlag(Opcodes.ACC_STATIC);
    }

    public boolean isNative() {
        return isFlag(Opcodes.ACC_NATIVE);
    }

    public boolean isAnnotation() {
        return isFlag(Opcodes.ACC_ANNOTATION);
    }

    public boolean isEnum() {
        return isFlag(Opcodes.ACC_ENUM);
    }

    public boolean isAbstract() {
        return isFlag(Opcodes.ACC_ABSTRACT);
    }

    public boolean isInterface() {
        return isFlag(Opcodes.ACC_INTERFACE);
    }

    public String minimalText() {
        StringBuilder sb = new StringBuilder();
        for (Flag flag : flags) {
            if (isFlag(flag.getMask())) {
                sb.append(flag.name.charAt(0));
            }
        }
        return sb.toString();
    }

    public boolean isSynthetic() {
        return isFlag(Opcodes.ACC_SYNTHETIC);
    }

    public static Flag[] getFlags() {
        return flags;
    }

    private static int TYPE_CLASS = 0x0001, TYPE_METHOD = 0x0002, TYPE_FIELD = 0x0004, TYPE_PARAMETER = 0x0008, TYPE_MODULE = 0x0010;

    public static final Flag FLAG_PUBLIC = new Flag("PUBLIC", 0x0001, TYPE_CLASS | TYPE_METHOD | TYPE_FIELD);
    public static final Flag FLAG_PRIVATE = new Flag("PRIVATE", 0x0002, TYPE_CLASS | TYPE_METHOD | TYPE_FIELD);
    public static final Flag FLAG_PROTECTED = new Flag("PROTECTED", 0x0004, TYPE_CLASS | TYPE_METHOD | TYPE_FIELD);
    public static final Flag FLAG_STATIC = new Flag("STATIC", 0x0008, TYPE_METHOD | TYPE_FIELD);
    public static final Flag FLAG_FINAL = new Flag("FINAL", 0x0010, TYPE_CLASS | TYPE_METHOD | TYPE_FIELD | TYPE_PARAMETER);
    public static final Flag FLAG_SUPER = new Flag("SUPER", 0x0020, TYPE_CLASS);
    public static final Flag FLAG_SYNCHRONIZED = new Flag("SYNCHRONIZED", 0x0020, TYPE_METHOD);
    public static final Flag FLAG_OPEN = new Flag("OPEN", 0x0020, TYPE_MODULE);
    public static final Flag FLAG_TRANSITIVE = new Flag("TRANSITIVE", 0x0020, TYPE_MODULE);
    public static final Flag FLAG_VOLATILE = new Flag("VOLATILE", 0x0040, TYPE_FIELD);
    public static final Flag FLAG_BRIDGE = new Flag("BRIDGE", 0x0040, TYPE_METHOD);
    public static final Flag FLAG_STATIC_PHASE = new Flag("STATIC_PHASE", 0x0040, TYPE_MODULE);
    public static final Flag FLAG_VARARGS = new Flag("VARARGS", 0x0080, TYPE_METHOD);
    public static final Flag FLAG_TRANSIENT = new Flag("TRANSIENT", 0x0080, TYPE_FIELD);
    public static final Flag FLAG_NATIVE = new Flag("NATIVE", 0x0100, TYPE_METHOD);
    public static final Flag FLAG_INTERFACE = new Flag("INTERFACE", 0x0200, TYPE_CLASS);
    public static final Flag FLAG_ABSTRACT = new Flag("ABSTRACT", 0x0400, TYPE_CLASS | TYPE_METHOD);
    public static final Flag FLAG_STRICT = new Flag("STRICT", 0x0800, TYPE_METHOD);
    public static final Flag FLAG_SYNTHETIC = new Flag("SYNTHETIC", 0x1000, TYPE_CLASS | TYPE_METHOD | TYPE_FIELD | TYPE_PARAMETER | TYPE_MODULE);
    public static final Flag FLAG_ANNOTATION = new Flag("ANNOTATION", 0x2000, TYPE_CLASS);
    public static final Flag FLAG_ENUM = new Flag("ENUM", 0x4000, TYPE_CLASS | TYPE_FIELD);
    public static final Flag FLAG_MANDATED = new Flag("MANDATED", 0x8000, TYPE_METHOD | TYPE_FIELD | TYPE_PARAMETER | TYPE_MODULE);
    public static final Flag FLAG_MODULE = new Flag("MODULE", 0x8000, TYPE_CLASS);

    public static final Flag[] flags = new Flag[] {
            FLAG_PUBLIC,
            FLAG_PRIVATE,
            FLAG_PROTECTED,
            FLAG_STATIC,
            FLAG_FINAL,
            FLAG_SUPER,
            FLAG_SYNCHRONIZED,
            FLAG_OPEN,
            FLAG_TRANSITIVE,
            FLAG_VOLATILE,
            FLAG_BRIDGE,
            FLAG_STATIC_PHASE,
            FLAG_VARARGS,
            FLAG_TRANSIENT,
            FLAG_NATIVE,
            FLAG_INTERFACE,
            FLAG_ABSTRACT,
            FLAG_STRICT,
            FLAG_SYNTHETIC,
            FLAG_ANNOTATION,
            FLAG_ENUM,
            FLAG_MANDATED,
            FLAG_MODULE,
    };

    public static class Flag {
        private final String name;
        private final int mask;
        private final int type;

        public Flag(String name, int mask, int type) {
            this.name = name;
            this.mask = mask;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public int getMask() {
            return mask;
        }

        public int getType() {
            return type;
        }

        public boolean isClassFlag() {
            return (type & TYPE_CLASS) != 0;
        }

        public boolean isMethodFlag() {
            return (type & TYPE_METHOD) != 0;
        }

        public boolean isFieldFlag() {
            return (type & TYPE_FIELD) != 0;
        }

        public boolean isParameterFlag() {
            return (type & TYPE_PARAMETER) != 0;
        }

        public boolean isModuleFlag() {
            return (type & TYPE_MODULE) != 0;
        }
    }
}
