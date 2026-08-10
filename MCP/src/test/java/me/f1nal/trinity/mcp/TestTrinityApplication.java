package me.f1nal.trinity.mcp;

import me.f1nal.trinity.application.AnalysisService;
import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.application.DexService;
import me.f1nal.trinity.application.MutationService;
import me.f1nal.trinity.application.Page;
import me.f1nal.trinity.application.ProjectService;
import me.f1nal.trinity.application.TrinityApplication;
import me.f1nal.trinity.application.TrinityStatus;

import java.util.List;
import java.util.Map;

final class TestTrinityApplication implements TrinityApplication {
    private long revision = 7;
    private final TrinityStatus status;
    private ProjectService.SearchQuery lastProjectSearchQuery;
    private AnalysisService.ConstantQuery lastConstantQuery;
    private DexService.DexClassQuery lastDexClassQuery;
    private DexService.ConstantQuery lastDexConstantQuery;

    TestTrinityApplication(TrinityStatus status) {
        this.status = status;
    }

    @Override
    public String version() {
        return status.version();
    }

    @Override
    public TrinityStatus status() {
        return status;
    }

    @Override
    public ProjectService projects() {
        return new ProjectService() {
            @Override
            public ProjectSnapshot current() {
                return project();
            }

            @Override
            public ProjectSnapshot create(CreateProject command) {
                return project();
            }

            @Override
            public ProjectSnapshot open(OpenProject command) {
                return project();
            }

            @Override
            public ProjectSnapshot save(long expectedRevision) {
                return project();
            }

            @Override
            public ProjectSnapshot close(CloseProject command) {
                return project();
            }

            @Override
            public ExportResult exportJar(ExportJar command) {
                return new ExportResult(command.outputPath(), 1, 1, 1, revision);
            }

            @Override
            public Page<TreeEntry> tree(TreeQuery query) {
                return Page.slice(List.of(new TreeEntry("class", "sample/Main",
                        "sample/Main", null)), query.offset(), query.limit());
            }

            @Override
            public Page<SearchResult> search(SearchQuery query) {
                lastProjectSearchQuery = query;
                return Page.slice(List.of(new SearchResult("class", "sample/Main",
                        "sample/Main", null, null, 1000)), query.offset(), query.limit());
            }
        };
    }

    @Override
    public BrowseService browse() {
        return new BrowseService() {
            @Override
            public ClassInfo getClass(String internalName) {
                return classInfo(internalName);
            }

            @Override
            public ClassStructure getClassStructure(String internalName) {
                return new ClassStructure(classInfo(internalName), List.of(methodInfo()),
                        List.of(fieldInfo()), List.of(), List.of());
            }

            @Override
            public SourceView decompileClass(String internalName) {
                return new SourceView(internalName, "java", "class Main {}\n", true, revision);
            }

            @Override
            public MethodInfo getMethod(MemberId method) {
                return methodInfo();
            }

            @Override
            public SourceView decompileMethod(MemberId method) {
                return new SourceView(method.owner() + "." + method.name(), "java",
                        "void run() {}\n", true, revision);
            }

            @Override
            public BytecodeView getMethodBytecode(MemberId method) {
                return new BytecodeView(method, "trinity-assembler-v1", "return", "hash",
                        1, 0, 1, revision);
            }

            @Override
            public FieldInfo getField(MemberId field) {
                return fieldInfo();
            }

            @Override
            public ResourceView readResource(String path, String encoding) {
                return new ResourceView(path, encoding, "dGVzdA==", 4, "hash", revision);
            }

            @Override
            public HierarchyView getClassHierarchy(String internalName) {
                return new HierarchyView(internalName, "java/lang/Object", List.of(), List.of(),
                        List.of(), List.of(), Map.of(), revision);
            }
        };
    }

    @Override
    public AnalysisService analysis() {
        return new AnalysisService() {
            @Override
            public Page<XrefResult> findClassReferences(ClassReferenceQuery query) {
                return Page.slice(List.of(), query.offset(), query.limit());
            }

            @Override
            public Page<XrefResult> findMemberReferences(MemberReferenceQuery query) {
                return Page.slice(List.of(), query.offset(), query.limit());
            }

            @Override
            public Page<ConstantResult> searchConstants(ConstantQuery query) {
                lastConstantQuery = query;
                return Page.slice(List.of(), query.offset(), query.limit());
            }

            @Override
            public PatternValidation validatePattern(PatternQuery query) {
                return new PatternValidation(true, 1, List.of());
            }

            @Override
            public Page<PatternMatch> searchPattern(PatternSearch query) {
                return Page.slice(List.of(), query.offset(), query.limit());
            }

            @Override
            public InvocationDetails getInvocation(InvocationQuery query) {
                return new InvocationDetails(query.caller(), query.instructionIndex(), "invokevirtual",
                        "method", "sample/Main", "run", "()V", false, true,
                        query.caller(), "void", List.of(), List.of(), revision);
            }
        };
    }

    @Override
    public DexService dex() {
        return new DexService() {
            @Override
            public Page<DexFileInfo> files(int offset, int limit) {
                return Page.slice(List.of(new DexFileInfo("classes.dex", 128, 35, 1)), offset, limit);
            }

            @Override
            public Page<DexClassSummary> classes(DexClassQuery query) {
                lastDexClassQuery = query;
                return Page.slice(List.of(dexClassSummary()), query.offset(), query.limit());
            }

            @Override
            public DexClassStructure getClass(String internalName) {
                return new DexClassStructure(dexClassSummary(), 1, "java/lang/Object", List.of(),
                        "Main.java", List.of(), List.of(dexMethod()), List.of(), revision);
            }

            @Override
            public DexMethodInfo getMethod(BrowseService.MemberId method) {
                return dexMethod();
            }

            @Override
            public SmaliView disassembleClass(String internalName) {
                return new SmaliView(internalName, "smali-v3", ".class public Lsample/DexMain;\n", revision);
            }

            @Override
            public SmaliView disassembleMethod(BrowseService.MemberId method) {
                return new SmaliView(String.format("%s.%s", method.owner(), method.name()), "smali-v3",
                        ".method public run()V\n.end method\n", revision);
            }

            @Override
            public DexJavaView decompileClass(String internalName) {
                return new DexJavaView(internalName, "java-jadx-1.5.6",
                        "package sample;\npublic class DexMain {}\n", 0, 0, revision);
            }

            @Override
            public DexJavaView decompileMethod(BrowseService.MemberId method) {
                return new DexJavaView(String.format("%s.%s%s", method.owner(), method.name(),
                        method.descriptor()), "java-jadx-1.5.6",
                        "public void run() {}\n", 0, 0, revision);
            }

            @Override
            public DexValidation validateClass(DexClassMutation command) {
                return new DexValidation(true, command.internalName(), "classes.dex",
                        "class-fingerprint", 144, List.of(), revision);
            }

            @Override
            public DexMutationResult replaceClass(DexClassMutation command) {
                long previous = revision++;
                return new DexMutationResult("dex_class_replace_smali", command.internalName(),
                        "classes.dex", previous, revision, 144, List.of(command.internalName()));
            }

            @Override
            public DexValidation validateMethod(DexMethodMutation command) {
                return new DexValidation(true, command.method().name(), "classes.dex",
                        "method-fingerprint", 144, List.of(), revision);
            }

            @Override
            public DexMutationResult replaceMethod(DexMethodMutation command) {
                long previous = revision++;
                return new DexMutationResult("dex_method_replace_smali", command.method().name(),
                        "classes.dex", previous, revision, 144, List.of(command.method().owner()));
            }

            @Override
            public Page<DexReference> findReferences(ReferenceQuery query) {
                return Page.slice(List.of(), query.offset(), query.limit());
            }

            @Override
            public Page<DexConstant> searchConstants(ConstantQuery query) {
                lastDexConstantQuery = query;
                return Page.slice(List.of(), query.offset(), query.limit());
            }
        };
    }

    ProjectService.SearchQuery lastProjectSearchQuery() {
        return lastProjectSearchQuery;
    }

    AnalysisService.ConstantQuery lastConstantQuery() {
        return lastConstantQuery;
    }

    DexService.DexClassQuery lastDexClassQuery() {
        return lastDexClassQuery;
    }

    DexService.ConstantQuery lastDexConstantQuery() {
        return lastDexConstantQuery;
    }

    @Override
    public MutationService mutations() {
        return new MutationService() {
            @Override
            public MutationResult setName(NameMutation command) {
                return mutation("name_set", command.target().owner());
            }

            @Override
            public MutationResult revertName(NameTarget target, long expectedRevision) {
                return mutation("name_revert", target.owner());
            }

            @Override
            public MutationResult createResource(ResourceMutation command) {
                return mutation("resource_create", command.path());
            }

            @Override
            public MutationResult deleteResource(String path, long expectedRevision) {
                return mutation("resource_delete", path);
            }

            @Override
            public BytecodeValidation validateBytecode(BytecodeCommand command) {
                return new BytecodeValidation(true, "hash", List.of(), List.of(), 1, 0, 1, revision);
            }

            @Override
            public MutationResult replaceBytecode(BytecodeCommand command) {
                return mutation("method_replace_bytecode", command.method().name());
            }

            @Override
            public RefactorPreview previewRefactor(RefactorRequest request) {
                return new RefactorPreview("token", request.mode(), List.of(), revision);
            }

            @Override
            public MutationResult applyRefactor(ApplyRefactor command) {
                return mutation("refactor_apply", command.previewToken());
            }
        };
    }

    private ProjectService.ProjectSnapshot project() {
        return new ProjectService.ProjectSnapshot("sample", "C:/sample.tdb", "LZ4", true,
                1, 1, 1, revision);
    }

    private BrowseService.ClassInfo classInfo(String name) {
        return new BrowseService.ClassInfo(name, name, 1, 61, null, "java/lang/Object",
                List.of(), "Main.java", null, null, List.of(), List.of(), 1, 1, revision);
    }

    private BrowseService.MethodInfo methodInfo() {
        BrowseService.MemberId id = new BrowseService.MemberId("sample/Main", "run", "()V");
        return new BrowseService.MethodInfo(id, "run", 1, null, List.of(), List.of(),
                1, 0, 1, 0, List.of(), revision);
    }

    private BrowseService.FieldInfo fieldInfo() {
        BrowseService.MemberId id = new BrowseService.MemberId("sample/Main", "value", "I");
        return new BrowseService.FieldInfo(id, "value", 1, null, 1, List.of(), revision);
    }

    private DexService.DexClassSummary dexClassSummary() {
        return new DexService.DexClassSummary(
                "sample/DexMain", "Lsample/DexMain;", "classes.dex", 1, 0);
    }

    private DexService.DexMethodInfo dexMethod() {
        BrowseService.MemberId id = new BrowseService.MemberId("sample/DexMain", "run", "()V");
        return new DexService.DexMethodInfo(
                id, 1, false, "V", List.of(), 1, 1, List.of(), revision);
    }

    private MutationService.MutationResult mutation(String operation, String target) {
        long previous = revision++;
        return new MutationService.MutationResult(operation, target, previous, revision, List.of(target));
    }
}
