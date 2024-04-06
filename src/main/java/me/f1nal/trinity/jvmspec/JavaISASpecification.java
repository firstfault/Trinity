package me.f1nal.trinity.jvmspec;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

public class JavaISASpecification {
    private static final List<JVMInstruction> instructions;

    static {
        try {
            instructions = getInstructionSpecs();
        } catch (Throwable e) {
            throw new RuntimeException("Parsing ISA specification", e);
        }
    }

    private static List<JVMInstruction> getInstructionSpecs() throws Throwable {
        List<JVMInstruction> list = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(JavaISASpecification.class.getClassLoader().getResourceAsStream("jvm_isa.html"));

        return list;
    }
}
