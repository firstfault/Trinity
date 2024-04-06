package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.decompiler.main.Fernflower;
import me.f1nal.trinity.decompiler.main.extern.IBytecodeProvider;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerLogger;
import me.f1nal.trinity.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.Manifest;

public class ClassDecompileTask extends IFernflowerLogger implements Runnable, IBytecodeProvider, IResultSaver {
    private final byte[] classBytes;
    private final Map<String, Object> options;
    private final Consumer<String> output;

    public ClassDecompileTask(byte[] classBytes, Map<String, Object> options, Consumer<String> output) {
        this.classBytes = classBytes;
        this.options = options;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            Fernflower fernflower = new Fernflower(this, this, this.options, this);
            fernflower.addSource(new File("X.class"));

            try {
                fernflower.decompileContext();
            } finally {
                fernflower.clearContext();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        output.accept(null);
    }

    // TODO: Needs better implementation. I did this in a rush.

    @Override
    public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
        return this.classBytes;
    }

    @Override
    public void saveFolder(String path) {
//        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
//        throw new IllegalArgumentException("Not implemented");
        System.out.println(path);
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        this.output.accept(content);
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entry) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void closeArchive(String path, String archiveName) {
        throw new IllegalArgumentException("Not implemented");
    }

    // TODO?

    @Override
    public void writeMessage(String message, Severity severity) {

    }

    @Override
    public void writeMessage(String message, Severity severity, Throwable t) {

    }
}
