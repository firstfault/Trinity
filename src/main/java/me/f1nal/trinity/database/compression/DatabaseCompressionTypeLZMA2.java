package me.f1nal.trinity.database.compression;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseCompressionTypeLZMA2 extends DatabaseCompressionType {
    public DatabaseCompressionTypeLZMA2() {
        super("LZMA2/XZ", "Very slow algorithm but very good compression.");
    }

    @Override
    public void compress(OutputStream stream, byte[] bytes) throws IOException {
        XZOutputStream xzOutputStream = new XZOutputStream(stream, new LZMA2Options(LZMA2Options.PRESET_MAX));
        xzOutputStream.write(bytes);
        xzOutputStream.close();
    }

    @Override
    public byte[] decompress(InputStream stream) throws IOException {
        XZInputStream xzInputStream = new XZInputStream(stream);
        byte[] bytes = xzInputStream.readAllBytes();
        xzInputStream.close();
        return bytes;
    }
}
