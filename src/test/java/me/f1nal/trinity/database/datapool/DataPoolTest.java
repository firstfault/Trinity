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
            output.writeShort(0);
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
            output.writeShort(1);
            output.writeInt(-2);
            output.writeInt(-3);
            output.writeInt(dex.length);
            output.write(dex);
            output.writeUTF("classes.dex");
            output.writeInt(-1);
        }

        Database database = new Database(
                "test", new File("test.trinity"), new DatabaseCompressionTypeRaw());
        database.loadTasks = new ArrayList<>();

        new DataPool().deserialize(
                database, new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(2, database.loadTasks.size());
        assertInstanceOf(DexInputReaderLoadTask.class, database.loadTasks.get(1));
    }
}
