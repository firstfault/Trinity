package me.f1nal.trinity.database.datapool;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.dex.DexFileUnit;
import me.f1nal.trinity.execution.loading.tasks.DexInputReaderLoadTask;
import me.f1nal.trinity.logging.Logging;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pool handing large amounts of binary data to not clog the XML database part.
 */
public class DataPool {
    static final int MAX_ENTRY_BYTES = 256 * 1024 * 1024;

    private final int version = 1;

    public void deserialize(Database database, DataInputStream dataInputStream) throws IOException {
        if (dataInputStream.readUnsignedShort() > version) {
            throw new IOException("Data pool version is too high.");
        }

        long time = System.currentTimeMillis();
        List<byte[]> classBytes = new ArrayList<>();
        Map<String, byte[]> resourceMap = new HashMap<>();
        List<UnreadDexBytes> dexFiles = new ArrayList<>();
        Section section = Section.CLASSES;
        int size;
        while ((size = dataInputStream.readInt()) != -1) {
            if (size == -2) {
                section = Section.RESOURCES;
                continue;
            }
            if (size == -3) {
                section = Section.DEX;
                continue;
            }
            if (size < 0 || size > MAX_ENTRY_BYTES || size > dataInputStream.available()) {
                throw new IOException(String.format("Invalid data pool entry size: %d", size));
            }
            byte[] bytes = new byte[size];
            dataInputStream.readFully(bytes);

            if (section == Section.CLASSES) {
                classBytes.add(bytes);
            } else {
                String entryName = dataInputStream.readUTF();
                if (section == Section.RESOURCES) {
                    resourceMap.put(entryName, bytes);
                } else {
                    dexFiles.add(new UnreadDexBytes(entryName, bytes));
                }
            }
        }
        database.loadTasks.add(new ClassInputReaderLoadTask(classBytes, resourceMap));
        if (!dexFiles.isEmpty()) {
            database.loadTasks.add(new DexInputReaderLoadTask(dexFiles));
        }
        database.setDataPoolLoadTime(System.currentTimeMillis() - time);
    }

    public void serialize(Execution execution, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.version);

        for (ClassInput classInput : execution.getClassList()) {
            final byte[] bytes = writeClassNode(classInput.getNode());

            if (bytes.length == 0) {
                Logging.error("Why are class bytes zero? {}", classInput.getRealName());
                continue;
            }

            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);
        }


        dataOutputStream.writeInt(-2);

        for (Map.Entry<String, byte[]> entry : execution.getResourceMap().entrySet()) {
            String name = entry.getKey();
            byte[] bytes = entry.getValue();

            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);

            dataOutputStream.writeUTF(name);
        }

        dataOutputStream.writeInt(-3);

        for (DexFileUnit dexFile : execution.getDexIndex().getFiles()) {
            byte[] bytes = dexFile.getBytes();
            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);
            dataOutputStream.writeUTF(dexFile.getName());
        }

        dataOutputStream.writeInt(-1);
    }

    private enum Section {
        CLASSES,
        RESOURCES,
        DEX
    }

    public static byte[] writeClassNode(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
