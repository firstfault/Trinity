package me.f1nal.trinity.util;

import com.thoughtworks.xstream.XStream;

public class SerializationUtil {
    public static void addAlias(XStream stream, Class<?> clazz, String alias) {
        stream.processAnnotations(clazz);
        stream.alias(alias, clazz);
        stream.allowTypes(new Class[]{clazz});
    }
}
