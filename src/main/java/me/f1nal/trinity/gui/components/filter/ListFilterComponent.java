package me.f1nal.trinity.gui.components.filter;

import imgui.ImGui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListFilterComponent<T> {
    private final Collection<T> elementList;
    private final Filter<T>[] filters;
    /**
     * List after filtering is done.
     */
    private List<T> filteredList;
    private List<Runnable> filterChangeListeners = new ArrayList<>(1);

    @SafeVarargs
    public ListFilterComponent(Collection<T> elementList, Filter<T>... filters) {
        this.elementList = elementList;
        this.filters = filters;

        for (Filter<T> filter : filters) {
            filter.initialize(elementList);
        }

        this.refreshFilteredList();
    }

    public void addFilterChangeListener(Runnable listener) {
        this.filterChangeListeners.add(listener);
        listener.run();
    }

    public Collection<T> getElementList() {
        return elementList;
    }

    public Filter<T>[] getFilters() {
        return filters;
    }

    private void refreshFilteredList() {
        List<T> current = new ArrayList<>(this.elementList);

        for (Filter<T> filter : filters) {
            filter.update(current);
            current = current.stream().filter(filter.filter()).toList();
        }

        this.filteredList = current;
        this.filterChangeListeners.forEach(Runnable::run);
    }

    public List<T> getFilteredList() {
        return filteredList;
    }

    public void draw() {
        final float indentW = 3.5F;
        ImGui.indent(indentW);
        boolean refresh = false;
        for (Filter<T> filter : filters) {
            refresh |= filter.draw();
        }
        if (refresh) {
            this.refreshFilteredList();
        }
        ImGui.unindent(indentW);
    }
}
