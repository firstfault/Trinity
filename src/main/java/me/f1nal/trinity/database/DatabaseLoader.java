package me.f1nal.trinity.database;

import com.thoughtworks.xstream.XStream;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.compression.DatabaseCompressionType;
import me.f1nal.trinity.database.compression.DatabaseCompressionTypeManager;
import me.f1nal.trinity.database.datapool.DataPool;
import me.f1nal.trinity.database.object.*;
import me.f1nal.trinity.database.semaphore.DatabaseSemaphore;
import me.f1nal.trinity.execution.loading.tasks.DatabaseReadObjectsLoadTask;
import me.f1nal.trinity.logging.Logging;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DatabaseLoader {
    private static final int DATABASE_VERSION = 3;
    public static final DatabaseSemaphore save = new DatabaseSemaphore((path) -> {
        Trinity trinity = Main.getTrinity();
        Database database = trinity.getDatabase();
        DatabaseCompressionType compressionType = database.getCompressionType();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        dataOutputStream.writeChar('T'); // magic
        dataOutputStream.writeInt(DATABASE_VERSION); // version
        dataOutputStream.writeByte(DatabaseCompressionTypeManager.getIndex(compressionType)); // compression type

        ByteArrayOutputStream dataByteStream = new ByteArrayOutputStream();
        DataOutputStream compressedDataOutputStream = new DataOutputStream(dataByteStream);

        // XML data
        byte[] xmlBytes = DatabaseLoader.toXML(database).getBytes();
        compressedDataOutputStream.writeInt(xmlBytes.length);
        compressedDataOutputStream.write(xmlBytes);

        // Data pool data
        DataPool dataPool = new DataPool();
        dataPool.serialize(trinity.getExecution(), compressedDataOutputStream);

        compressionType.compress(byteArrayOutputStream, dataByteStream.toByteArray());

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        writeDatabaseAtomically(path, byteArray);
        database.setDatabaseSize(byteArray.length);
    }, true);

    public static final DatabaseSemaphore load = new DatabaseSemaphore((path) -> {
        byte[] byteArray = Files.readAllBytes(path.toPath());
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        final char magic = dataInputStream.readChar();
        if (magic != 'T') {
            throw new IOException(String.format("Unexpected magic number: %s", magic));
        }

        final int version = dataInputStream.readInt();
        if (version != DATABASE_VERSION) {
            throw new IOException(String.format("Unsupported Trinity database version %d; expected %d", version, DATABASE_VERSION));
        }
        final byte compressionTypeIndex = dataInputStream.readByte();
        final DatabaseCompressionType databaseCompressionType = DatabaseCompressionTypeManager.getType(compressionTypeIndex);

        if (databaseCompressionType == null) {
            throw new IOException(String.format("Bad database compression type %s from version %d", compressionTypeIndex, version));
        }

        byte[] decompressedBytes = databaseCompressionType.decompress(byteArrayInputStream);
        ByteArrayInputStream decompresedByteStream = new ByteArrayInputStream(decompressedBytes);
        DataInputStream decompressedDataInputStream = new DataInputStream(decompresedByteStream);

        // XML data
        final byte[] xmlBytes = new byte[decompressedDataInputStream.readInt()];
        decompressedDataInputStream.readFully(xmlBytes);

        Database database = DatabaseLoader.fromXML(new String(xmlBytes));
        database.setCompressionType(databaseCompressionType);
        database.setPath(path);
        database.loadTasks = new ArrayList<>();

        // Data pool data
        DataPool dataPool = new DataPool();
        dataPool.deserialize(database, decompressedDataInputStream);

        database.loadTasks.add(new DatabaseReadObjectsLoadTask());
        database.setDatabaseSize(byteArray.length);

        Main.getDisplayManager().setDatabase(new Trinity(database, null));
    }, false);

    private static final XStream stream = new XStream();
    private static final Map<Class<?>, String> aliases = new HashMap<>();

    public static String toXML(Database database) {
        return stream.toXML(database);
    }

    public static Database fromXML(String xml) {
        Database database = (Database) stream.fromXML(xml);
        return database;
    }

    static void writeDatabaseAtomically(File file, byte[] bytes) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(bytes, "bytes");

        Path destination = file.toPath().toAbsolutePath();
        Path parent = destination.getParent();
        if (parent == null) {
            throw new IOException("Database destination has no parent directory: " + destination);
        }
        Files.createDirectories(parent);

        String fileName = destination.getFileName().toString();
        Path temporary = Files.createTempFile(
                parent, "." + fileName + ".", ".trinity.tmp");
        try {
            writeAndSync(temporary, bytes);
            preservePermissions(destination, temporary);
            createBackup(destination, parent);
            replaceAtomically(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeAndSync(Path file, byte[] bytes) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file.toFile())) {
            output.write(bytes);
            output.getFD().sync();
        }
    }

    private static void createBackup(Path destination, Path parent) {
        if (!Files.isRegularFile(destination)) return;

        Path backup = destination.resolveSibling(destination.getFileName() + ".bak");
        Path temporaryBackup = null;
        try {
            temporaryBackup = Files.createTempFile(
                    parent, "." + destination.getFileName() + ".backup.", ".tmp");
            Files.copy(destination, temporaryBackup,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            syncFile(temporaryBackup);
            replaceAtomically(temporaryBackup, backup);
        } catch (IOException | RuntimeException exception) {
            Logging.warn("Unable to preserve database backup '{}': {}", backup, exception);
        } finally {
            if (temporaryBackup != null) {
                try {
                    Files.deleteIfExists(temporaryBackup);
                } catch (IOException exception) {
                    Logging.warn("Unable to remove temporary database backup '{}': {}",
                            temporaryBackup, exception);
                }
            }
        }
    }

    private static void syncFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void preservePermissions(Path destination, Path temporary) {
        if (!Files.exists(destination)) return;
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(destination);
            Files.setPosixFilePermissions(temporary, permissions);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Logging.warn("Unable to preserve database permissions from '{}': {}",
                    destination, exception);
        }
    }

    private static void replaceAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String getAlias(Class<?> type) {
        return Objects.requireNonNull(aliases.get(type), "No alias for " + type.getName());
    }

    static {
        aliases.put(Database.class, "database");
        aliases.put(ClassPath.class, "classPath");
        aliases.put(DatabaseVariable.class, "methodVariableObj");
        aliases.put(DatabaseClassDisplayName.class, "classObj");
        aliases.put(DatabaseMethodDisplayName.class, "methodObj");
        aliases.put(DatabaseFieldDisplayName.class, "fieldObj");
        aliases.put(DatabasePackage.class, "packageObj");
        aliases.put(DatabaseDecompiler.class, "decompiler");
        aliases.put(DatabaseExportJarSettings.class, "exportJarSettings");
        aliases.put(DatabaseXrefViewerSettings.class, "xrefViewerSettings");
        aliases.put(DatabaseNavigationHistory.class, "navigationHistory");
        aliases.put(DatabaseNavigationEntry.class, "navigationEntry");
//        aliases.put(ClassNode.class, "asmClassNode");
        aliases.forEach((clazz, alias) -> {
            stream.processAnnotations(clazz);
            stream.alias(alias, clazz);
            stream.allowTypes(new Class[]{clazz});
        });

//        stream.registerConverter(new ClassNodeConverter());
    }
}
