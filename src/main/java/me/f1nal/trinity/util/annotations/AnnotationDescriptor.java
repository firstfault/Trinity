package me.f1nal.trinity.util.annotations;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnnotationDescriptor {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public Map<String, Object> getValues() {
        return values;
    }
}
