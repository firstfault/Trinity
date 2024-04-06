package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;

public abstract class AbstractDatabaseObject {
    /**
     * Loads this object back into Trinity.
     *
     * @return {@code true} if this object is still valid.
     */
    public abstract boolean load(Trinity trinity);

    @Override
    public int hashCode() {
        return databaseHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractDatabaseObject) {
            return obj.hashCode() == this.hashCode();
        }
        return super.equals(obj);
    }

    protected abstract int databaseHashCode();
}
