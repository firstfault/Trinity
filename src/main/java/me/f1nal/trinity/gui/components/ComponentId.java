package me.f1nal.trinity.gui.components;

import java.util.HashMap;
import java.util.Map;

public class ComponentId {
    private static Map<Class<?>, Integer> idMap = new HashMap<>();
    private static int dockspaceId = 666;

    public static int getNextDockspaceId() {
        return dockspaceId++;
    }

    public static String getId(Class<?> clazz) {
        int id = idMap.getOrDefault(clazz, 0);
        idMap.put(clazz, id + 1);
        return clazz.getName() + id;
    }
}
