package me.f1nal.trinity.application;

import java.util.List;

/** Native Android DEX discovery, Java-like decompilation, smali, mutation, and analysis. */
public interface DexService {
    Page<DexFileInfo> files(int offset, int limit);

    Page<DexClassSummary> classes(String query, int offset, int limit);

    DexClassStructure getClass(String internalName);

    DexMethodInfo getMethod(BrowseService.MemberId method);

    SmaliView disassembleClass(String internalName);

    SmaliView disassembleMethod(BrowseService.MemberId method);

    DexJavaView decompileClass(String internalName);

    DexJavaView decompileMethod(BrowseService.MemberId method);
    DexValidation validateClass(DexClassMutation command);

    DexMutationResult replaceClass(DexClassMutation command);

    DexValidation validateMethod(DexMethodMutation command);

    DexMutationResult replaceMethod(DexMethodMutation command);


    Page<DexReference> findReferences(ReferenceQuery query);

    Page<DexConstant> searchConstants(ConstantQuery query);

    record DexFileInfo(String name, long byteCount, int apiLevel, int classCount) {
    }

    record DexClassSummary(String internalName, String descriptor, String dexFile,
                           int methodCount, int fieldCount) {
    }

    record DexClassStructure(DexClassSummary classInfo, int access, String superName,
                             List<String> interfaces, String sourceFile,
                             List<String> annotations, List<DexMethodInfo> methods,
                             List<DexFieldInfo> fields, long revision) {
        public DexClassStructure {
            interfaces = List.copyOf(interfaces);
            annotations = List.copyOf(annotations);
            methods = List.copyOf(methods);
            fields = List.copyOf(fields);
        }
    }

    record DexMethodInfo(BrowseService.MemberId id, int access, boolean direct,
                         String returnType, List<String> parameterTypes,
                         Integer registerCount, int instructionCount,
                         List<String> annotations, long revision) {
        public DexMethodInfo {
            parameterTypes = List.copyOf(parameterTypes);
            annotations = List.copyOf(annotations);
        }
    }

    record DexFieldInfo(BrowseService.MemberId id, int access, boolean isStatic,
                        Object initialValue, List<String> annotations, long revision) {
        public DexFieldInfo {
            annotations = List.copyOf(annotations);
        }
    }

    record SmaliView(String identity, String format, String smali, long revision) {
    }

    record DexJavaView(String identity, String format, String source,
                       int errorCount, int warningCount, long revision) {
    }

    record DexClassMutation(String internalName, String smali, long expectedRevision) {
    }

    record DexMethodMutation(BrowseService.MemberId method, String smali,
                             long expectedRevision) {
    }

    record DexDiagnostic(int line, int column, String severity, String message) {
    }

    record DexValidation(boolean valid, String target, String dexFile, String fingerprint,
                         long byteCount, List<DexDiagnostic> diagnostics, long revision) {
        public DexValidation {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    record DexMutationResult(String operation, String target, String dexFile,
                             long previousRevision, long revision, long byteCount,
                             List<String> changedClasses) {
        public DexMutationResult {
            changedClasses = List.copyOf(changedClasses);
        }
    }

    record ReferenceQuery(String kind, String owner, String name, String descriptor,
                          int offset, int limit) {
    }

    record DexReference(String kind, String target, BrowseService.MemberId caller,
                        int instructionIndex, String opcode) {
    }

    record ConstantQuery(String type, String value, boolean exact,
                         boolean caseSensitive, int offset, int limit) {
    }

    record DexConstant(String type, Object value, BrowseService.MemberId method,
                       int instructionIndex, String opcode) {
    }
}
