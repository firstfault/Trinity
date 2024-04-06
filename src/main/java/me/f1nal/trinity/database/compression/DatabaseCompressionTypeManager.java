package me.f1nal.trinity.database.compression;

import java.util.List;

public class DatabaseCompressionTypeManager {
    private static final List<DatabaseCompressionType> types = List.of(
            new DatabaseCompressionTypeLZ4(),
            new DatabaseCompressionTypeGZIP(),
            new DatabaseCompressionTypeLZMA2(),
            new DatabaseCompressionTypeRaw()
    );

    public static DatabaseCompressionType getType(int index) {
        if (index < 0 || index >= types.size()) {
            return null;
        }
        return types.get(index);
    }

    public static List<DatabaseCompressionType> getTypes() {
        return types;
    }

    public static int getIndex(DatabaseCompressionType compressionType) {
        final int indexOf = types.indexOf(compressionType);
        if (indexOf == -1) throw new RuntimeException();
        return indexOf;
    }
}
