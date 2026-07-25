package me.f1nal.trinity.application;

import java.util.List;
import java.util.Map;

/** Headless class, member, source, bytecode, and resource queries. */
public interface BrowseService {
    ClassInfo getClass(String internalName);

    ClassStructure getClassStructure(String internalName);

    SourceView decompileClass(String internalName);

    MethodInfo getMethod(MemberId method);

    SourceView decompileMethod(MemberId method);

    BytecodeView getMethodBytecode(MemberId method);

    FieldInfo getField(MemberId field);

    ResourceView readResource(String path, String encoding);

    HierarchyView getClassHierarchy(String internalName);

    record MemberId(String owner, String name, String descriptor) {
    }

    record ClassInfo(String internalName, String displayName, int access,
                     int classFileVersion, String signature, String superName,
                     List<String> interfaces, String sourceFile, String outerClass,
                     String nestHost, List<String> permittedSubclasses,
                     List<String> annotations, int methodCount, int fieldCount,
                     long revision) {
        public ClassInfo {
            interfaces = List.copyOf(interfaces);
            permittedSubclasses = List.copyOf(permittedSubclasses);
            annotations = List.copyOf(annotations);
        }
    }

    record ClassStructure(ClassInfo classInfo, List<MethodInfo> methods,
                          List<FieldInfo> fields, List<InnerClassInfo> innerClasses,
                          List<RecordComponentInfo> recordComponents) {
        public ClassStructure {
            methods = List.copyOf(methods);
            fields = List.copyOf(fields);
            innerClasses = List.copyOf(innerClasses);
            recordComponents = List.copyOf(recordComponents);
        }
    }

    record MethodInfo(MemberId id, String displayName, int access, String signature,
                      List<String> exceptions, List<String> annotations,
                      int instructionCount, int maxStack, int maxLocals,
                      int tryCatchBlockCount, List<ParameterInfo> parameters,
                      long revision) {
        public MethodInfo {
            exceptions = List.copyOf(exceptions);
            annotations = List.copyOf(annotations);
            parameters = List.copyOf(parameters);
        }
    }

    record FieldInfo(MemberId id, String displayName, int access, String signature,
                     Object constantValue, List<String> annotations, long revision) {
        public FieldInfo {
            annotations = List.copyOf(annotations);
        }
    }

    record ParameterInfo(int index, String descriptor, String name) {
    }

    record InnerClassInfo(String name, String outerName, String innerName, int access) {
    }

    record RecordComponentInfo(String name, String descriptor, String signature,
                               List<String> annotations) {
        public RecordComponentInfo {
            annotations = List.copyOf(annotations);
        }
    }

    record SourceView(String identity, String language, String source,
                      boolean complete, long revision) {
    }

    record BytecodeView(MemberId method, String format, String instructions,
                        String fingerprint, int instructionCount, int maxStack,
                        int maxLocals, long revision) {
    }

    record ResourceView(String path, String encoding, String content,
                        int byteCount, String sha256, long revision) {
    }

    record HierarchyView(String internalName, String directSuperClass,
                         List<String> superClasses, List<String> interfaces,
                         List<String> directSubclasses, List<String> inheritors,
                         Map<String, List<MemberId>> overrideFamilies,
                         long revision) {
        public HierarchyView {
            superClasses = List.copyOf(superClasses);
            interfaces = List.copyOf(interfaces);
            directSubclasses = List.copyOf(directSubclasses);
            inheritors = List.copyOf(inheritors);
            overrideFamilies = Map.copyOf(overrideFamilies);
        }
    }
}
