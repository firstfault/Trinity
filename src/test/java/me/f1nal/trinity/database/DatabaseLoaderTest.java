package me.f1nal.trinity.database;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseLoaderTest {
    @Test
    void readsSizedFieldWithinLimit() throws IOException {
        byte[] payload = {1, 2, 3};

        assertArrayEquals(payload, DatabaseLoader.readSizedBytes(
                sizedInput(payload.length, payload), payload.length, "field"));
    }

    @Test
    void rejectsSizedFieldAboveConfiguredLimit() throws IOException {
        assertThrows(IOException.class, () -> DatabaseLoader.readSizedBytes(
                sizedInput(5, new byte[5]), 4, "field"));
    }

    @Test
    void rejectsSizedFieldLargerThanRemainingInput() throws IOException {
        assertThrows(IOException.class, () -> DatabaseLoader.readSizedBytes(
                sizedInput(5, new byte[1]), 5, "field"));
    }

    private static DataInputStream sizedInput(int declaredSize, byte[] payload) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(declaredSize);
            output.write(payload);
        }
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }
}
