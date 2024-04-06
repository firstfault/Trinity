package me.f1nal.trinity.database.compression;

import me.f1nal.trinity.util.IDescribable;
import me.f1nal.trinity.util.INameable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class DatabaseCompressionType implements INameable, IDescribable {
    private final String name;
    private final String description;

    public DatabaseCompressionType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public abstract void compress(OutputStream stream, byte[] bytes) throws IOException;
    public abstract byte[] decompress(InputStream stream) throws IOException;
}
