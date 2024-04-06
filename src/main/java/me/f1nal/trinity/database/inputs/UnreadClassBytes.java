package me.f1nal.trinity.database.inputs;

/**
 * Bytes of a class that have not yet been processed into a {@link org.objectweb.asm.tree.ClassNode}
 */
public class UnreadClassBytes {
    private final String entryName;
    private final byte[] bytes;

    public UnreadClassBytes(String entryName, byte[] bytes) {
        this.entryName = entryName;
        this.bytes = bytes;
    }

    public String getEntryName() {
        return entryName;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
