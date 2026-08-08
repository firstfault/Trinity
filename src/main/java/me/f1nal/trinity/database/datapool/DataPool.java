package me.f1nal.trinity.database.datapool;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.inputs.ProjectContainerInput;
import me.f1nal.trinity.database.inputs.ProjectInputSet;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.execution.dependency.DependencyKind;
import me.f1nal.trinity.execution.dex.DexFileUnit;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.loading.tasks.DependencyArchiveLoadTask;
import me.f1nal.trinity.execution.loading.tasks.DexInputReaderLoadTask;
import me.f1nal.trinity.execution.loading.tasks.RuntimeDependencyLoadTask;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;
import me.f1nal.trinity.logging.Logging;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Binary project data, including project containers and dependency references. */
public class DataPool {
    private static final int VERSION = 5;
    private static final int OLDEST_SUPPORTED_VERSION = 2;
    static final int MAX_ENTRY_BYTES = 256 * 1024 * 1024;
    private static final int MAX_PROJECT_CONTAINERS = 10_000;
    private static final int MAX_CONTAINER_ENTRIES = 1_000_000;
    private static final int MAX_DEPENDENCY_ARCHIVES = 10_000;
    private static final int MAX_DEPENDENCY_CLASSES = 100_000;

    public void deserialize(Database database, DataInputStream input) throws IOException {
        int version = input.readUnsignedShort();
        if (version < OLDEST_SUPPORTED_VERSION || version > VERSION) {
            throw new IOException("Unsupported data pool version " + version
                    + "; expected " + OLDEST_SUPPORTED_VERSION + "-" + VERSION);
        }

        long started = System.currentTimeMillis();
        ProjectInputSet projectInput = new ProjectInputSet();
        int containerCount = readCount(input, "project container", MAX_PROJECT_CONTAINERS);

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

            int classCount = readCount(input, "class", MAX_CONTAINER_ENTRIES);
            for (int j = 0; j < classCount; j++) {
                String entryName = input.readUTF();
                boolean rebuildRequired = input.readBoolean();
                ZipEntryMetadata metadata = readMetadata(input);
                classPath.addClass(new UnreadClassBytes(entryName, readBytes(input), metadata, rebuildRequired));
            }

            int resourceCount = readCount(input, "resource", MAX_CONTAINER_ENTRIES);
            for (int j = 0; j < resourceCount; j++) {
                String entryName = input.readUTF();
                ZipEntryMetadata metadata = readMetadata(input);
                classPath.putResource(entryName, readBytes(input), metadata);
            }
            int directoryCount = readCount(input, "directory", MAX_CONTAINER_ENTRIES);
            for (int j = 0; j < directoryCount; j++) {
                classPath.getDirectories().add(new ArchiveDirectoryEntry(input.readUTF(), readMetadata(input)));
            }
            projectInput.add(new ProjectContainerInput(id, name, kind, classPath));
        }
        if (version >= 4) {
            database.loadTasks.add(new DependencyArchiveLoadTask(readDependencyArchives(input)));
        } else if (version == 3) {
            database.loadTasks.add(new DependencyArchiveLoadTask(
                    readEmbeddedDependencyArchives(input)));
        } else {
            // Version 2 predates persisted classpaths. Migrate it with the
            // same java.base dependency provided to newly created projects.
            database.loadTasks.add(new RuntimeDependencyLoadTask());
        }
        database.loadTasks.add(new ClassInputReaderLoadTask(projectInput));
        if (version >= 5) {
            List<UnreadDexBytes> dexFiles = readDexFiles(input);
            if (!dexFiles.isEmpty()) {
                database.loadTasks.add(new DexInputReaderLoadTask(dexFiles));
            }
        }
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

        writeDependencyArchives(output, execution.getDependencies().getArchives());
        writeDexFiles(output, execution.getDexIndex().getFiles());
    }

    static List<DependencyArchive> readDependencyArchives(DataInputStream input) throws IOException {
        int dependencyCount = readCount(input, "dependency", MAX_DEPENDENCY_ARCHIVES);
        List<DependencyArchive> dependencies = new ArrayList<>(dependencyCount);
        for (int i = 0; i < dependencyCount; i++) {
            UUID id = new UUID(input.readLong(), input.readLong());
            String name = input.readUTF();
            int kindIndex = input.readUnsignedByte();
            if (kindIndex >= DependencyKind.values().length) {
                throw new IOException("Unknown dependency kind " + kindIndex);
            }
            try {
                dependencies.add(new DependencyArchive(id, name, DependencyKind.values()[kindIndex],
                        readString(input), readString(input), readString(input)));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid persisted dependency archive " + name, exception);
            }
        }
        return dependencies;
    }

    static List<DependencyArchive> readEmbeddedDependencyArchives(DataInputStream input)
            throws IOException {
        int dependencyCount = readCount(input, "dependency", MAX_DEPENDENCY_ARCHIVES);
        List<DependencyArchive> dependencies = new ArrayList<>(dependencyCount);
        for (int i = 0; i < dependencyCount; i++) {
            UUID id = new UUID(input.readLong(), input.readLong());
            String name = input.readUTF();
            int classCount = readCount(input, "dependency class", MAX_DEPENDENCY_CLASSES);
            for (int j = 0; j < classCount; j++) {
                input.readUTF();
                readBytes(input);
            }
            dependencies.add(name.equals("java.base")
                    ? new DependencyArchive(id, name, DependencyKind.RUNTIME_MODULE,
                            null, null, "java.base")
                    : new DependencyArchive(id, name, DependencyKind.ARCHIVE,
                            name, null, null));
        }
        return dependencies;
    }

    static void writeDependencyArchives(DataOutputStream output, List<DependencyArchive> dependencies)
            throws IOException {
        output.writeInt(dependencies.size());
        for (DependencyArchive dependency : dependencies) {
            output.writeLong(dependency.getId().getMostSignificantBits());
            output.writeLong(dependency.getId().getLeastSignificantBits());
            output.writeUTF(dependency.getName());
            output.writeByte(dependency.getKind().ordinal());
            writeString(output, dependency.getRelativePath());
            writeString(output, dependency.getAbsolutePath());
            writeString(output, dependency.getRuntimeModule());
        }
    }

    private static List<UnreadDexBytes> readDexFiles(DataInputStream input) throws IOException {
        int count = readCount(input, "DEX file", MAX_CONTAINER_ENTRIES);
        List<UnreadDexBytes> dexFiles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            dexFiles.add(new UnreadDexBytes(input.readUTF(), readBytes(input)));
        }
        return dexFiles;
    }

    private static void writeDexFiles(DataOutputStream output, Iterable<DexFileUnit> dexFiles)
            throws IOException {
        List<DexFileUnit> files = new ArrayList<>();
        dexFiles.forEach(files::add);
        output.writeInt(files.size());
        for (DexFileUnit dexFile : files) {
            output.writeUTF(dexFile.getName());
            writeBytes(output, dexFile.getBytes());
        }
    }

    private static int readCount(DataInputStream input, String type, int maximum) throws IOException {
        int count = input.readInt();
        if (count < 0) throw new IOException("Negative " + type + " entry count");
        if (count > maximum) throw new IOException("Too many " + type + " entries: " + count);
        return count;
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_ENTRY_BYTES || length > input.available()) {
            throw new IOException("Invalid data pool entry size: " + length);
        }
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
        if (length < -1 || length > MAX_ENTRY_BYTES || length > input.available()) {
            throw new IOException("Invalid optional data size: " + length);
        }
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
