package me.f1nal.trinity.database.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DatabaseCompressionTypeGZIP extends DatabaseCompressionType {
    public DatabaseCompressionTypeGZIP() {
        super("GZIP", "Decently fast with good compression.");
    }

    @Override
    public void compress(OutputStream stream, byte[] bytes) throws IOException {
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(stream);
        gzipOutputStream.write(bytes);
        gzipOutputStream.close();
    }

    @Override
    public byte[] decompress(InputStream stream) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(stream);
        byte[] bytes = gzipInputStream.readAllBytes();
        gzipInputStream.close();
        return bytes;
    }
}
