package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.IDescribable;
import me.f1nal.trinity.util.INameable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public class ListBoxComponent<T extends INameable> {
    private List<T> elementList;
    private T selection;
    private final String componentId = ComponentId.getId(this.getClass());

    public ListBoxComponent(List<T> elementList, T selection) {
        this.elementList = elementList;
        this.selection = selection;
    }

    public ListBoxComponent(List<T> elementList) {
        this(elementList, elementList.isEmpty() ? null : elementList.get(0));
    }

    @Unmodifiable
    public List<T> getElementList() {
        return elementList;
    }

    public void addElement(T element) {
        this.elementList.add(element);

        if (this.selection == null) {
            this.selection = element;
        }
    }

    public void removeElement(T element) {
        this.elementList.remove(element);

        if (this.selection == element) {
            this.selection = this.elementList.isEmpty() ? null : this.elementList.get(0);
        }
    }

    public void draw(float sizeX, float sizeY) {
        if (!ImGui.beginListBox("###" + this.componentId, sizeX, sizeY)) {
            return;
        }
        for (T element : elementList) {
            if (ImGui.selectable(element.getName(), element == this.selection)) {
                this.selection = element;
            }
            if (element instanceof IDescribable) GuiUtil.tooltip(((IDescribable) element).getDescription());
        }
        ImGui.endListBox();
    }

    public T getSelection() {
        return selection;
    }

    public int getSelectionIndex() {
        return elementList.indexOf(selection);
    }

    public int getElementCount() {
        return elementList.size();
    }

    public void setSelection(T selection) {
        this.selection = selection;
    }
}
