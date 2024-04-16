package me.f1nal.trinity.database;

import com.google.common.io.Files;
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
        database.setDatabaseSize(byteArray.length);
        Files.write(byteArray, path);
    }, true);

    public static final DatabaseSemaphore load = new DatabaseSemaphore((path) -> {
        byte[] byteArray = Files.toByteArray(path);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        final char magic = dataInputStream.readChar();
        if (magic != 'T') {
            throw new IOException(String.format("Unexpected magic number: %s", magic));
        }

        final int version = dataInputStream.readInt();
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
//        aliases.put(ClassNode.class, "asmClassNode");
        aliases.forEach((clazz, alias) -> {
            stream.processAnnotations(clazz);
            stream.alias(alias, clazz);
            stream.allowTypes(new Class[]{clazz});
        });

//        stream.registerConverter(new ClassNodeConverter());
    }
}
