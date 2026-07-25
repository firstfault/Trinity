package me.f1nal.trinity.database.inputs;

import java.util.Objects;

/** Raw DEX container entry retained for native parsing and persistence. */
public final class UnreadDexBytes {
    private final String entryName;
    private final byte[] bytes;

    public UnreadDexBytes(String entryName, byte[] bytes) {
        this.entryName = Objects.requireNonNull(entryName, "entryName");
        this.bytes = Objects.requireNonNull(bytes, "bytes");
    }

    public String getEntryName() {
        return entryName;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
