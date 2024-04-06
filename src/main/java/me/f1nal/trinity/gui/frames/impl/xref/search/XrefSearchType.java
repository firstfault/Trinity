package me.f1nal.trinity.gui.frames.impl.xref.search;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.util.INameable;

public abstract class XrefSearchType implements INameable {
    private final String name;
    protected final Trinity trinity;

    protected XrefSearchType(String name, Trinity trinity) {
        this.name = name;
        this.trinity = trinity;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Draws this xref type.
     * @return {@code true} If we can search right now.
     */
    public abstract boolean draw();

    /**
     * Get a xref builder for this xref type.
     */
    public abstract XrefBuilder search();
}
