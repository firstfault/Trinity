package me.f1nal.trinity.decompiler.output.serialize;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.impl.KeywordOutputMember;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.function.Function;

public final class OutputMemberSerializer {
    public static String serialize(OutputMember outputMember) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(OutputMemberFactory.getId(outputMember.getClass()));
        outputMember.serialize(dos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static String serializeTags(OutputMember outputMember) {
        try {
            return "<TrinityOMObject>" + serialize(outputMember) + "</TrinityOMObject>";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String tag(String text, Function<Integer, OutputMember> function) {
        return serializeTags(function.apply(text.length())).concat(text);
    }

    public static String keyword(String text) {
        return tag(text, KeywordOutputMember::new);
    }

    public static OutputMember deserialize(String encoded) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
        DataInputStream dis = new DataInputStream(bais);
        int id = dis.readUnsignedByte();
        Class<?> clazz = OutputMemberFactory.getClass(id);
        if (clazz == null) {
            throw new IOException("Unknown output member type: " + id);
        }
        Constructor<?> constructor = null;
        try {
            constructor = clazz.getConstructor(int.class);
        } catch (NoSuchMethodException e) {
            throw new IOException("Constructor not found for type: " + clazz.getSimpleName(), e);
        }
        Object instance = null;
        try {
            instance = constructor.newInstance(dis.readInt());
            if (!(instance instanceof OutputMember)) {
                throw new InstantiationException("Not of OutputMember type (WTF?)");
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Instantiation failed for type: " + clazz.getSimpleName(), e);
        }
        OutputMember outputMember = (OutputMember) instance;
        outputMember.deserialize(dis);
        return outputMember;
    }
}
