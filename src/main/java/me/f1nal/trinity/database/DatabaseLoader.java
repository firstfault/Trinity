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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DatabaseLoader {
    private static final int DATABASE_VERSION = 2;
    static final int MAX_DATABASE_XML_BYTES = 128 * 1024 * 1024;

    public static final DatabaseSemaphore save = new DatabaseSemaphore(
            path -> saveProject(Main.getTrinity(), path), true);

    public static final DatabaseSemaphore load = new DatabaseSemaphore(
            path -> Main.getDisplayManager().setDatabase(loadProject(path)), false);

    /** Serializes one explicit workspace without consulting presentation state. */
    public static void saveProject(Trinity trinity, File path) throws IOException {
        Objects.requireNonNull(trinity, "trinity");
        Objects.requireNonNull(path, "path");
        Database database = trinity.getDatabase();
        DatabaseCompressionType compressionType = database.getCompressionType();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeChar('T');
        dataOutputStream.writeInt(DATABASE_VERSION);
        dataOutputStream.writeByte(DatabaseCompressionTypeManager.getIndex(compressionType));

        ByteArrayOutputStream dataByteStream = new ByteArrayOutputStream();
        DataOutputStream compressedDataOutputStream = new DataOutputStream(dataByteStream);
        byte[] xmlBytes = DatabaseLoader.toXML(database).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        compressedDataOutputStream.writeInt(xmlBytes.length);
        compressedDataOutputStream.write(xmlBytes);

        DataPool dataPool = new DataPool();
        dataPool.serialize(trinity.getExecution(), compressedDataOutputStream);
        compressionType.compress(byteArrayOutputStream, dataByteStream.toByteArray());

        byte[] bytes = byteArrayOutputStream.toByteArray();
        database.setDatabaseSize(bytes.length);
        java.nio.file.Files.write(path.toPath(), bytes);
    }

    /** Loads one workspace without installing it into a GUI or other presentation adapter. */
    public static Trinity loadProject(File path) throws Exception {
        Objects.requireNonNull(path, "path");
        final DatabaseCompressionType databaseCompressionType;
        final byte[] decompressedBytes;
        try (DataInputStream dataInputStream = new DataInputStream(
                new BufferedInputStream(new FileInputStream(path)))) {
            final char magic = dataInputStream.readChar();
            if (magic != 'T') {
                throw new IOException(String.format("Unexpected magic number: %s", magic));
            }
            final int version = dataInputStream.readInt();
            final byte compressionTypeIndex = dataInputStream.readByte();
            databaseCompressionType = DatabaseCompressionTypeManager.getType(compressionTypeIndex);
            if (databaseCompressionType == null) {
                throw new IOException(String.format(
                        "Bad database compression type %s from version %d", compressionTypeIndex, version));
            }
            decompressedBytes = databaseCompressionType.decompress(dataInputStream);
        }

        DataInputStream decompressedDataInputStream =
                new DataInputStream(new ByteArrayInputStream(decompressedBytes));
        byte[] xmlBytes = readSizedBytes(
                decompressedDataInputStream, MAX_DATABASE_XML_BYTES, "Database XML");
        Database database = DatabaseLoader.fromXML(
                new String(xmlBytes, java.nio.charset.StandardCharsets.UTF_8));
        database.setCompressionType(databaseCompressionType);
        database.setPath(path);
        database.loadTasks = new ArrayList<>();

        DataPool dataPool = new DataPool();
        dataPool.deserialize(database, decompressedDataInputStream);
        database.loadTasks.add(new DatabaseReadObjectsLoadTask());
        database.setDatabaseSize(path.length());
        return new Trinity(database, null);
    }

    static byte[] readSizedBytes(DataInputStream input, int maximumBytes, String description)
            throws IOException {
        int size = input.readInt();
        if (size < 0 || size > maximumBytes || size > input.available()) {
            throw new IOException("Invalid " + description + " size: " + size);
        }
        byte[] bytes = new byte[size];
        input.readFully(bytes);
        return bytes;
    }

    private static final XStream stream = new XStream();
    private static final Map<Class<?>, String> aliases = new HashMap<>();

    public static String toXML(Database database) {
        return stream.toXML(database);
    }

    public static Database fromXML(String xml) {
        Database database = (Database) stream.fromXML(xml);
        return database;
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
