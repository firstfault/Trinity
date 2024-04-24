package me.f1nal.trinity.gui.components.general.table;

import imgui.flag.ImGuiTableColumnFlags;
import me.f1nal.trinity.util.ByteUtil;

import java.util.function.Function;

public class TableColumn<T> {
    private final String header;
    private int flags = ImGuiTableColumnFlags.None;
    private final ITableCellRenderer<T> renderer;

    public TableColumn(String header, Function<T, String> text) {
        this(header, new TableColumnRendererText<>(text));
    }

    public TableColumn(String header, ITableCellRenderer<T> renderer) {
        this.header = header;
        this.renderer = renderer;
    }

    public TableColumn<T> setResizable(boolean resizable) {
        this.flags = ByteUtil.setBitflag(this.flags, ImGuiTableColumnFlags.NoResize, !resizable);
        return this;
    }

    public boolean isResizable() {
        return ByteUtil.getBitflag(this.flags, ImGuiTableColumnFlags.NoResize);
    }

    public String getHeader() {
        return header;
    }

    public int getFlags() {
        return flags;
    }

    public void draw(T element) {
        this.renderer.render(this, element);
    }
}
