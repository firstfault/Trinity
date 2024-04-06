package me.f1nal.trinity.database.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseCompressionTypeRaw extends DatabaseCompressionType {
    public DatabaseCompressionTypeRaw() {
        super("Raw", "No compression applied to the database file.");
    }

    @Override
    public void compress(OutputStream stream, byte[] bytes) throws IOException {
        stream.write(bytes);
    }

    @Override
    public byte[] decompress(InputStream stream) throws IOException {
        return stream.readAllBytes();
    }
}
