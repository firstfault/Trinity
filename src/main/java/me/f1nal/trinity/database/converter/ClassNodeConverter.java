package me.f1nal.trinity.database.converter;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Base64;

public class ClassNodeConverter implements Converter {
    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
        ClassWriter classWriter = new ClassWriter(0);
        ((ClassNode) o).accept(classWriter);
        byte[] classBytes = classWriter.toByteArray();
        hierarchicalStreamWriter.setValue(Base64.getEncoder().encodeToString(classBytes));
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(Base64.getDecoder().decode(hierarchicalStreamReader.getValue()));
        classReader.accept(classNode, 0);
        return classNode;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return aClass == ClassNode.class;
    }
}
