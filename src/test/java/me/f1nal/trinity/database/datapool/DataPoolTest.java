package me.f1nal.trinity.database.datapool;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.compression.DatabaseCompressionTypeRaw;
import me.f1nal.trinity.execution.dex.DexTestFixture;
import me.f1nal.trinity.execution.loading.tasks.DexInputReaderLoadTask;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataPoolTest {
    @Test
    void rejectsEntryLargerThanRemainingInput() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeShort(5);
            output.writeInt(1);
            output.writeLong(0);
            output.writeLong(1);
            output.writeByte(1);
            output.writeUTF("Loose Files");
            output.writeInt(-1);
            output.writeInt(1);
            output.writeUTF("Broken.class");
            output.writeBoolean(false);
            writeDefaultMetadata(output);
            output.writeInt(5);
            output.writeByte(1);
        }

        Database database = new Database(
                "test", new File("test.trinity"), new DatabaseCompressionTypeRaw());
        database.loadTasks = new ArrayList<>();

        assertThrows(IOException.class, () -> new DataPool().deserialize(
                database, new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }

    @Test
    void restoresNativeDexSection() throws Exception {
        byte[] dex = DexTestFixture.create();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeShort(5);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(1);
            output.writeUTF("classes.dex");
            output.writeInt(dex.length);
            output.write(dex);
        }

        Database database = new Database(
                "test", new File("test.trinity"), new DatabaseCompressionTypeRaw());
        database.loadTasks = new ArrayList<>();

        new DataPool().deserialize(
                database, new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(3, database.loadTasks.size());
        assertInstanceOf(DexInputReaderLoadTask.class, database.loadTasks.get(2));
    }

    private static void writeDefaultMetadata(DataOutputStream output) throws IOException {
        output.writeInt(Integer.MAX_VALUE);
        output.writeByte(8);
        output.writeLong(-1);
        output.writeLong(-1);
        output.writeLong(-1);
        output.writeInt(-1);
        output.writeInt(-1);
        output.writeLong(-1);
        output.writeLong(-1);
    }
}
