package me.f1nal.trinity.gui.components.general.table;

import imgui.flag.ImGuiTableColumnFlags;
import me.f1nal.trinity.util.ByteUtil;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

public class TableColumn<T> {
    private final String header;
    private int flags = ImGuiTableColumnFlags.None;
    private float widthWeight;
    private final ITableCellRenderer<T> renderer;
    private Comparator<T> comparator;

    public TableColumn(String header, Function<T, String> text) {
        this(header, new TableColumnRendererText<>(text));
        this.setSortKey(text);
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

    public TableColumn<T> setWidthWeight(float widthWeight) {
        this.widthWeight = widthWeight;
        this.flags = ByteUtil.setBitflag(this.flags, ImGuiTableColumnFlags.WidthStretch, true);
        return this;
    }

    public float getWidthWeight() {
        return widthWeight;
    }

    public TableColumn<T> setSortKey(Function<T, String> sortKey) {
        Objects.requireNonNull(sortKey);
        this.comparator = Comparator.comparing(sortKey,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        return this;
    }

    public TableColumn<T> setComparator(Comparator<T> comparator) {
        this.comparator = Objects.requireNonNull(comparator);
        return this;
    }

    Comparator<T> getComparator() {
        return comparator;
    }

    public String getHeader() {
        return header;
    }

    public int getFlags() {
        return this.comparator == null ? flags | ImGuiTableColumnFlags.NoSort : flags;
    }

    public void draw(T element) {
        this.renderer.render(this, element);
    }
}
