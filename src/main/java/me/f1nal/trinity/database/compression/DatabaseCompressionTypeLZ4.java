package me.f1nal.trinity.database.compression;

import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseCompressionTypeLZ4 extends DatabaseCompressionType {
    public DatabaseCompressionTypeLZ4() {
        super("LZ4", "Very fast compression algorithm but bigger file size.");
    }

    @Override
    public void compress(OutputStream stream, byte[] bytes) throws IOException {
        LZ4FrameOutputStream xzOutputStream = new LZ4FrameOutputStream(stream);
        xzOutputStream.write(bytes);
        xzOutputStream.close();
    }

    @Override
    public byte[] decompress(InputStream stream) throws IOException {
        LZ4FrameInputStream xzInputStream = new LZ4FrameInputStream(stream);
        byte[] bytes = xzInputStream.readAllBytes();
        xzInputStream.close();
        return bytes;
    }
}
