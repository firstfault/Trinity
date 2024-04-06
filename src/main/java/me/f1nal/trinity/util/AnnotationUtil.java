package me.f1nal.trinity.util;

import me.f1nal.trinity.util.annotations.AnnotationDescriptor;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;

public class AnnotationUtil {
    public static AnnotationDescriptor getAnnotation(List<AnnotationNode> list, String typeName) {
        if (list != null) {
            typeName = "L" + typeName + ";";

            for (AnnotationNode annotationNode : list) {
                if (annotationNode.desc.equals(typeName)) {
                    AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor();

                    if (annotationNode.values != null) for (int i = 0; i < annotationNode.values.size(); i+=2) {
                        String name = (String) annotationNode.values.get(i);
                        Object value = annotationNode.values.get(i + 1);

                        annotationDescriptor.getValues().put(name, value);
                    }

                    return annotationDescriptor;
                }
            }
        }
        return null;
    }
}
