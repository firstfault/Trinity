package me.f1nal.trinity.database;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.object.AbstractDatabaseObject;

public interface IDatabaseSavable<T extends AbstractDatabaseObject> {
    T createDatabaseObject();

    default void save() {
        Main.getTrinity().getDatabase().save(this);
    }

    default boolean isSaveRequired() {
        return true;
    }
}
