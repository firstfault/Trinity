package me.f1nal.trinity.gui.components.tabs;

import me.f1nal.trinity.gui.components.general.ListBoxComponent;

import java.util.List;

public class ListBoxTabsComponent<T extends TabFrame> extends ListBoxComponent<T> {
    public ListBoxTabsComponent(List<T> elementList, T selection) {
        super(elementList, selection);
    }

    public ListBoxTabsComponent(List<T> elementList) {
        super(elementList);
    }

    @Override
    public void draw(float sizeX, float sizeY) {
        super.draw(sizeX, sizeY);
    }
}
