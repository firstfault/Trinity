package me.f1nal.trinity.gui.components.general.table;

import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Shared hover emphasis, counts, and click-to-filter state for categorical table columns. */
public final class TableTypeFocusController<T, K> {
    private K focusedType;
    private K hoveredType;
    private T hoveredRow;
    private K selectedType;
    private T selectedRow;
    private final Map<K, Integer> typeCounts = new HashMap<>();

    public void beginFrame(List<T> rows, Function<T, K> typeResolver) {
        hoveredType = null;
        hoveredRow = null;
        typeCounts.clear();
        for (T row : rows) typeCounts.merge(typeResolver.apply(row), 1, Integer::sum);
    }

    public void hover(T row, K type) {
        hoveredRow = row;
        hoveredType = type;
    }

    public void toggleSelection(T row, K type) {
        if (Objects.equals(selectedType, type)) {
            clearSelection();
        } else {
            select(row, type);
        }
    }

    public void select(T row, K type) {
        selectedRow = row;
        selectedType = type;
    }

    public void clearSelection() {
        selectedRow = null;
        selectedType = null;
    }

    public boolean isSelected(K type) {
        return Objects.equals(selectedType, type);
    }

    public boolean shouldShowCount(T row, K type) {
        if (selectedType != null) {
            return selectedRow == row && Objects.equals(selectedType, type);
        }
        return hoveredRow == row && Objects.equals(hoveredType, type);
    }

    public int getCount(K type) {
        return typeCounts.getOrDefault(type, 0);
    }

    public int getColor(K type, int activeColor) {
        return focusedType == null || Objects.equals(focusedType, type)
                ? activeColor : CodeColorScheme.DISABLED;
    }

    public List<T> filterSelected(List<T> rows, Function<T, K> typeResolver) {
        if (selectedType == null) return rows;
        return rows.stream()
                .filter(row -> Objects.equals(selectedType, typeResolver.apply(row)))
                .toList();
    }

    public void endFrame() {
        focusedType = hoveredType;
    }
}
