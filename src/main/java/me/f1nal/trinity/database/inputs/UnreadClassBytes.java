package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.execution.packages.ZipEntryMetadata;

/**
 * Bytes of a class that have not yet been processed into a {@link org.objectweb.asm.tree.ClassNode}
 */
public class UnreadClassBytes {
    private final String entryName;
    private final byte[] bytes;
    private final ZipEntryMetadata metadata;
    private final boolean rebuildRequired;

    public UnreadClassBytes(String entryName, byte[] bytes) {
        this(entryName, bytes, ZipEntryMetadata.createDefault(), false);
    }

    public UnreadClassBytes(String entryName, byte[] bytes, ZipEntryMetadata metadata,
                            boolean rebuildRequired) {
        this.entryName = entryName;
        this.bytes = bytes;
        this.metadata = metadata == null ? ZipEntryMetadata.createDefault() : metadata;
        this.rebuildRequired = rebuildRequired;
    }

    public String getEntryName() {
        return entryName;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public ZipEntryMetadata getMetadata() {
        return metadata;
    }

    public boolean isRebuildRequired() {
        return rebuildRequired;
    }
}
