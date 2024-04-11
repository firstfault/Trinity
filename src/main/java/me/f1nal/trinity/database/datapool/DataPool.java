package me.f1nal.trinity.database.datapool;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
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
    private final int version = 0;

    public void deserialize(Database database, DataInputStream dataInputStream) throws IOException {
        if (dataInputStream.readUnsignedShort() > version) {
            throw new IOException("Data pool version is too high.");
        }

        long time = System.currentTimeMillis();
        List<byte[]> classBytes = new ArrayList<>();
        Map<String, byte[]> resourceMap = new HashMap<>();
        boolean resources = false;
        int size;
        while ((size = dataInputStream.readInt()) != -1) {
            // -2 marks start of resource array
            if (size == -2 && !resources) {
                resources = true;
                continue;
            }
            if (size < 0) throw new IOException(String.format("size < 0: %d", size));

            byte[] bytes = new byte[size];
            dataInputStream.readFully(bytes);

            if (resources) {
                String resourceName = dataInputStream.readUTF();
                resourceMap.put(resourceName, bytes);
            } else {
                classBytes.add(bytes);
            }
        }
        database.loadTasks.add(new ClassInputReaderLoadTask(classBytes, resourceMap));
        database.setDataPoolLoadTime(System.currentTimeMillis() - time);
    }

    public void serialize(Execution execution, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.version);

        for (ClassInput classInput : execution.getClassList()) {
            final byte[] bytes = writeClassNode(classInput.getNode());

            if (bytes.length == 0) {
                Logging.error("Why are class bytes zero? {}", classInput.getFullName());
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

        dataOutputStream.writeInt(-1);
    }

    public static byte[] writeClassNode(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
