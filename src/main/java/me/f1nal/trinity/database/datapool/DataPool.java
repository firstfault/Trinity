package me.f1nal.trinity.database.datapool;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.inputs.ProjectContainerInput;
import me.f1nal.trinity.database.inputs.ProjectInputSet;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;
import me.f1nal.trinity.logging.Logging;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

/** Binary project data, grouped by its persisted JAR/loose container. */
public class DataPool {
    private static final int VERSION = 2;

    public void deserialize(Database database, DataInputStream input) throws IOException {
        int version = input.readUnsignedShort();
        if (version != VERSION) {
            throw new IOException("Unsupported data pool version " + version + "; expected " + VERSION);
        }

        long started = System.currentTimeMillis();
        ProjectInputSet projectInput = new ProjectInputSet();
        int containerCount = input.readInt();
        if (containerCount < 0) throw new IOException("Negative project container count");

        for (int i = 0; i < containerCount; i++) {
            UUID id = new UUID(input.readLong(), input.readLong());
            int kindIndex = input.readUnsignedByte();
            if (kindIndex >= ProjectContainerKind.values().length) {
                throw new IOException("Unknown project container kind " + kindIndex);
            }
            ProjectContainerKind kind = ProjectContainerKind.values()[kindIndex];
            String name = input.readUTF();
            ClassPath classPath = new ClassPath();
            classPath.setArchiveComment(readString(input));

            int classCount = readCount(input, "class");
            for (int j = 0; j < classCount; j++) {
                String entryName = input.readUTF();
                boolean rebuildRequired = input.readBoolean();
                ZipEntryMetadata metadata = readMetadata(input);
                classPath.addClass(new UnreadClassBytes(entryName, readBytes(input), metadata, rebuildRequired));
            }

            int resourceCount = readCount(input, "resource");
            for (int j = 0; j < resourceCount; j++) {
                String entryName = input.readUTF();
                ZipEntryMetadata metadata = readMetadata(input);
                classPath.putResource(entryName, readBytes(input), metadata);
            }
            int directoryCount = readCount(input, "directory");
            for (int j = 0; j < directoryCount; j++) {
                classPath.getDirectories().add(new ArchiveDirectoryEntry(input.readUTF(), readMetadata(input)));
            }
            projectInput.add(new ProjectContainerInput(id, name, kind, classPath));
        }

        database.loadTasks.add(new ClassInputReaderLoadTask(projectInput));
        database.setDataPoolLoadTime(System.currentTimeMillis() - started);
    }

    public void serialize(Execution execution, DataOutputStream output) throws IOException {
        output.writeShort(VERSION);
        output.writeInt(execution.getContainers().size());

        for (ProjectContainer container : execution.getContainers()) {
            output.writeLong(container.getId().getMostSignificantBits());
            output.writeLong(container.getId().getLeastSignificantBits());
            output.writeByte(container.getKind().ordinal());
            output.writeUTF(container.getName());
            writeString(output, container.getArchiveComment());

            output.writeInt(container.getClasses().size());
            for (var target : container.getClasses()) {
                ClassInput classInput = target.getInput();
                if (classInput == null) throw new IOException("Container contains unresolved class " + target.getRealName());
                byte[] bytes = classInput.isRebuildRequired()
                        ? writeClassNode(classInput.getNode()) : classInput.getExportBytes();
                if (bytes == null || bytes.length == 0) {
                    Logging.error("Class bytes are empty: {}", target.getRealName());
                    throw new IOException("Class bytes are empty: " + target.getRealName());
                }
                output.writeUTF(classInput.getExportEntryName());
                output.writeBoolean(classInput.isRebuildRequired());
                writeMetadata(output, target.getZipMetadata());
                writeBytes(output, bytes);
            }

            output.writeInt(container.getResources().size());
            for (ResourceArchiveEntry resource : container.getResources()) {
                output.writeUTF(resource.getRealName());
                writeMetadata(output, resource.getZipMetadata());
                writeBytes(output, resource.getBytes());
            }

            output.writeInt(container.getDirectories().size());
            for (ArchiveDirectoryEntry directory : container.getDirectories()) {
                output.writeUTF(directory.getName());
                writeMetadata(output, directory.getMetadata());
            }
        }
    }

    private static int readCount(DataInputStream input, String type) throws IOException {
        int count = input.readInt();
        if (count < 0) throw new IOException("Negative " + type + " entry count");
        return count;
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0) throw new IOException("Negative entry size");
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return bytes;
    }

    private static void writeBytes(DataOutputStream output, byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static ZipEntryMetadata readMetadata(DataInputStream input) throws IOException {
        return new ZipEntryMetadata(input.readInt(), input.readUnsignedByte(), input.readLong(),
                input.readLong(), input.readLong(), readString(input), readNullableBytes(input),
                input.readLong(), input.readLong());
    }

    private static void writeMetadata(DataOutputStream output, ZipEntryMetadata metadata) throws IOException {
        output.writeInt(metadata.getOrder());
        output.writeByte(metadata.getMethod());
        output.writeLong(metadata.getModifiedTime());
        output.writeLong(metadata.getAccessTime());
        output.writeLong(metadata.getCreationTime());
        writeString(output, metadata.getComment());
        writeNullableBytes(output, metadata.getExtra());
        output.writeLong(metadata.getCrc());
        output.writeLong(metadata.getCompressedSize());
    }

    private static String readString(DataInputStream input) throws IOException {
        byte[] bytes = readNullableBytes(input);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        writeNullableBytes(output, value == null ? null : value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] readNullableBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length == -1) return null;
        if (length < -1) throw new IOException("Negative optional data size");
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return bytes;
    }

    private static void writeNullableBytes(DataOutputStream output, byte[] bytes) throws IOException {
        output.writeInt(bytes == null ? -1 : bytes.length);
        if (bytes != null) output.write(bytes);
    }

    public static byte[] writeClassNode(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
