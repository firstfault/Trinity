package me.f1nal.trinity.adapter;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.Reference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.iface.reference.TypeReference;
import me.f1nal.trinity.application.ApplicationException;
import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.application.DexService;
import me.f1nal.trinity.application.Page;
import me.f1nal.trinity.execution.dex.DexClassEntry;
import me.f1nal.trinity.execution.dex.DexDescriptors;
import me.f1nal.trinity.execution.dex.DexDisassembler;
import me.f1nal.trinity.execution.dex.DexFileUnit;
import me.f1nal.trinity.execution.dex.DexEditor;
import me.f1nal.trinity.execution.dex.DexIndex;
import me.f1nal.trinity.execution.dex.DexJavaDecompiler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Native dexlib2-backed implementation of the headless DEX contract. */
final class LiveDexService implements DexService {
    private static final int MAX_PAGE_SIZE = 500;

    private final LiveApplicationState state;

    LiveDexService(LiveApplicationState state) {
        this.state = state;
    }

    @Override
    public Page<DexFileInfo> files(int offset, int limit) {
        return state.read(true, project -> {
            List<DexFileInfo> output = project.getExecution().getDexIndex().getFiles().stream()
                    .map(this::fileInfo)
                    .sorted(Comparator.comparing(DexFileInfo::name))
                    .toList();
            return Page.slice(output, offset, pageLimit(limit));
        });
    }

    @Override
    public Page<DexClassSummary> classes(String query, int offset, int limit) {
        return state.read(true, project -> {
            String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            List<DexClassSummary> output = project.getExecution().getDexIndex().getClasses().stream()
                    .filter(entry -> term.isEmpty()
                            || entry.getInternalName().toLowerCase(Locale.ROOT).contains(term))
                    .map(this::classSummary)
                    .sorted(Comparator.comparing(DexClassSummary::internalName))
                    .toList();
            return Page.slice(output, offset, pageLimit(limit));
        });
    }

    @Override
    public DexClassStructure getClass(String internalName) {
        return state.read(true, project -> classStructure(requireClass(project, internalName)));
    }

    @Override
    public DexMethodInfo getMethod(BrowseService.MemberId method) {
        return state.read(true, project -> {
            DexClassEntry owner = requireClass(project, method.owner());
            return methodInfo(owner, requireMethod(owner, method), directMethods(owner.getClassDef()));
        });
    }

    @Override
    public SmaliView disassembleClass(String internalName) {
        return state.read(true, project -> {
            DexClassEntry entry = requireClass(project, internalName);
            return new SmaliView(entry.getInternalName(), "smali-v3", entry.disassemble(), state.revision());
        });
    }

    @Override
    public SmaliView disassembleMethod(BrowseService.MemberId method) {
        return state.read(true, project -> {
            DexClassEntry owner = requireClass(project, method.owner());
            Method target = requireMethod(owner, method);
            String smali = DexDisassembler.disassembleMethod(owner.getFile(), owner.getClassDef(), target);
            return new SmaliView(DexDescriptors.methodIdentity(target), "smali-v3", smali, state.revision());
        });
    }

    @Override
    public DexJavaView decompileClass(String internalName) {
        JavaDecompileRequest request = state.read(true, project -> {
            DexClassEntry entry = requireClass(project, internalName);
            List<DexJavaDecompiler.Input> inputs = project.getExecution().getDexIndex()
                    .getFiles().stream()
                    .map(file -> new DexJavaDecompiler.Input(file.getName(), file.getBytes()))
                    .toList();
            return new JavaDecompileRequest(
                    project.getExecution().getDexIndex().getJavaDecompiler(),
                    inputs, entry.getInternalName(), state.revision());
        });
        DexJavaDecompiler.ClassView view = decompile(request);
        return new DexJavaView(request.internalName(), DexJavaDecompiler.FORMAT, view.source(),
                view.errorCount(), view.warningCount(), request.revision());
    }

    @Override
    public DexJavaView decompileMethod(BrowseService.MemberId method) {
        JavaDecompileRequest request = state.read(true, project -> {
            DexClassEntry entry = requireClass(project, method.owner());
            requireMethod(entry, method);
            List<DexJavaDecompiler.Input> inputs = project.getExecution().getDexIndex()
                    .getFiles().stream()
                    .map(file -> new DexJavaDecompiler.Input(file.getName(), file.getBytes()))
                    .toList();
            return new JavaDecompileRequest(
                    project.getExecution().getDexIndex().getJavaDecompiler(),
                    inputs, entry.getInternalName(), state.revision());
        });
        DexJavaDecompiler.ClassView view = decompile(request);
        return new DexJavaView(methodIdentity(method), DexJavaDecompiler.FORMAT,
                DexJavaDecompiler.methodSource(view, method), view.errorCount(),
                view.warningCount(), request.revision());
    }

    @Override
    public DexValidation validateClass(DexClassMutation command) {
        requireClassCommand(command);
        return state.read(true, project -> {
            state.checkRevision(command.expectedRevision());
            DexClassEntry target = requireClass(project, command.internalName());
            return validation(DexEditor.replaceClass(target, command.smali()),
                    target.getInternalName(), state.revision());
        });
    }

    @Override
    public DexMutationResult replaceClass(DexClassMutation command) {
        requireClassCommand(command);
        LiveApplicationState.Changed<DexEditor.Candidate> changed = state.mutate(
                command.expectedRevision(), project -> {
                    DexIndex index = project.getExecution().getDexIndex();
                    DexEditor.Candidate candidate = DexEditor.replaceClass(
                            requireClass(project, command.internalName()), command.smali());
                    requireValid(candidate);
                    DexFileUnit replacement = index.parse(candidate.dexFile(), candidate.bytes());
                    index.replace(index.getClass(command.internalName()).getFile(), replacement);
                    return candidate;
                });
        DexEditor.Candidate candidate = changed.value();
        return new DexMutationResult("dex_class_replace_smali", candidate.target(),
                candidate.dexFile(), changed.previousRevision(), changed.revision(),
                candidate.bytes().length, List.of(candidate.target()));
    }

    @Override
    public DexValidation validateMethod(DexMethodMutation command) {
        requireMethodCommand(command);
        return state.read(true, project -> {
            state.checkRevision(command.expectedRevision());
            DexClassEntry owner = requireClass(project, command.method().owner());
            requireMethod(owner, command.method());
            return validation(DexEditor.replaceMethod(owner, command.method(), command.smali()),
                    methodIdentity(command.method()), state.revision());
        });
    }

    @Override
    public DexMutationResult replaceMethod(DexMethodMutation command) {
        requireMethodCommand(command);
        LiveApplicationState.Changed<DexEditor.Candidate> changed = state.mutate(
                command.expectedRevision(), project -> {
                    DexIndex index = project.getExecution().getDexIndex();
                    DexClassEntry owner = requireClass(project, command.method().owner());
                    requireMethod(owner, command.method());
                    DexEditor.Candidate candidate = DexEditor.replaceMethod(
                            owner, command.method(), command.smali());
                    requireValid(candidate);
                    DexFileUnit replacement = index.parse(candidate.dexFile(), candidate.bytes());
                    index.replace(owner.getFile(), replacement);
                    return candidate;
                });
        DexEditor.Candidate candidate = changed.value();
        return new DexMutationResult("dex_method_replace_smali",
                methodIdentity(command.method()), candidate.dexFile(),
                changed.previousRevision(), changed.revision(), candidate.bytes().length,
                List.of(candidate.target()));
    }

    @Override
    public Page<DexReference> findReferences(ReferenceQuery query) {
        return state.read(true, project -> {
            String kind = normalizeReferenceKind(query.kind());
            validateReferenceTarget(kind, query);
            List<DexReference> output = new ArrayList<>();
            for (DexClassEntry entry : project.getExecution().getDexIndex().getClasses()) {
                for (Method method : entry.getClassDef().getMethods()) {
                    MethodImplementation implementation = method.getImplementation();
                    if (implementation == null) continue;
                    BrowseService.MemberId caller = memberId(method);
                    int index = 0;
                    for (Instruction instruction : implementation.getInstructions()) {
                        if (instruction instanceof ReferenceInstruction referenceInstruction
                                && matchesReference(kind, query, referenceInstruction.getReference())) {
                            Reference reference = referenceInstruction.getReference();
                            output.add(new DexReference(referenceKind(reference),
                                    DexFormatter.INSTANCE.getReference(reference), caller, index,
                                    instruction.getOpcode().name));
                        }
                        index++;
                    }
                }
            }
            return Page.slice(output, query.offset(), pageLimit(query.limit()));
        });
    }

    @Override
    public Page<DexConstant> searchConstants(ConstantQuery query) {
        return state.read(true, project -> {
            String type = normalizeConstantType(query.type());
            List<DexConstant> output = new ArrayList<>();
            for (DexClassEntry entry : project.getExecution().getDexIndex().getClasses()) {
                for (Method method : entry.getClassDef().getMethods()) {
                    MethodImplementation implementation = method.getImplementation();
                    if (implementation == null) continue;
                    BrowseService.MemberId caller = memberId(method);
                    int index = 0;
                    for (Instruction instruction : implementation.getInstructions()) {
                        Object value = constant(instruction);
                        if (value != null && matchesConstant(type, query, value)) {
                            output.add(new DexConstant(value instanceof String ? "string" : "number",
                                    value, caller, index, instruction.getOpcode().name));
                        }
                        index++;
                    }
                }
            }
            return Page.slice(output, query.offset(), pageLimit(query.limit()));
        });
    }

    private DexClassStructure classStructure(DexClassEntry entry) {
        ClassDef classDef = entry.getClassDef();
        Set<String> direct = directMethods(classDef);
        List<DexMethodInfo> methods = new ArrayList<>();
        classDef.getMethods().forEach(method -> methods.add(methodInfo(entry, method, direct)));
        methods.sort(Comparator.comparing((DexMethodInfo info) -> info.id().name())
                .thenComparing(info -> info.id().descriptor()));
        List<DexFieldInfo> fields = new ArrayList<>();
        classDef.getFields().forEach(field -> fields.add(fieldInfo(field)));
        fields.sort(Comparator.comparing((DexFieldInfo info) -> info.id().name())
                .thenComparing(info -> info.id().descriptor()));
        return new DexClassStructure(classSummary(entry), classDef.getAccessFlags(),
                DexDescriptors.internalName(classDef.getSuperclass()),
                classDef.getInterfaces().stream().map(DexDescriptors::internalName).toList(),
                classDef.getSourceFile(), annotations(classDef.getAnnotations()), methods, fields,
                state.revision());
    }

    private DexFileInfo fileInfo(DexFileUnit file) {
        return new DexFileInfo(file.getName(), file.getBytes().length,
                file.getDexFile().getOpcodes().api, file.getClasses().size());
    }

    private DexClassSummary classSummary(DexClassEntry entry) {
        return new DexClassSummary(entry.getInternalName(), entry.getClassDef().getType(),
                entry.getFile().getName(), count(entry.getClassDef().getMethods()),
                count(entry.getClassDef().getFields()));
    }

    private DexMethodInfo methodInfo(DexClassEntry owner, Method method, Set<String> direct) {
        MethodImplementation implementation = method.getImplementation();
        List<String> parameters = method.getParameterTypes().stream().map(CharSequence::toString).toList();
        return new DexMethodInfo(memberId(method), method.getAccessFlags(),
                direct.contains(DexDescriptors.methodIdentity(method)), method.getReturnType(), parameters,
                implementation == null ? null : implementation.getRegisterCount(),
                implementation == null ? 0 : count(implementation.getInstructions()),
                annotations(method.getAnnotations()), state.revision());
    }

    private DexFieldInfo fieldInfo(Field field) {
        Object initialValue = field.getInitialValue() == null ? null
                : DexFormatter.INSTANCE.getEncodedValue(field.getInitialValue());
        return new DexFieldInfo(new BrowseService.MemberId(
                DexDescriptors.internalName(field.getDefiningClass()), field.getName(), field.getType()),
                field.getAccessFlags(), AccessFlags.STATIC.isSet(field.getAccessFlags()), initialValue,
                annotations(field.getAnnotations()), state.revision());
    }

    private static DexClassEntry requireClass(me.f1nal.trinity.Trinity project, String internalName) {
        if (internalName == null || internalName.isBlank()) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "internalName must not be blank");
        }
        DexClassEntry entry = project.getExecution().getDexIndex().getClass(internalName.trim());
        if (entry == null) {
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    String.format("DEX class not found: %s", internalName));
        }
        return entry;
    }

    private static Method requireMethod(DexClassEntry owner, BrowseService.MemberId method) {
        Method target = owner.findMethod(method.name(), method.descriptor());
        if (target == null) {
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    String.format("DEX method not found: %s.%s%s", owner.getInternalName(),
                            method.name(), method.descriptor()));
        }
        return target;
    }

    private static BrowseService.MemberId memberId(Method method) {
        return new BrowseService.MemberId(DexDescriptors.internalName(method.getDefiningClass()),
                method.getName(), DexDescriptors.methodDescriptor(method));
    }

    private static Set<String> directMethods(ClassDef classDef) {
        Set<String> direct = new HashSet<>();
        classDef.getDirectMethods().forEach(method -> direct.add(DexDescriptors.methodIdentity(method)));
        return direct;
    }

    private static List<String> annotations(Iterable<? extends Annotation> annotations) {
        List<String> types = new ArrayList<>();
        annotations.forEach(annotation -> types.add(annotation.getType()));
        types.sort(String::compareTo);
        return List.copyOf(types);
    }

    private static boolean matchesReference(String kind, ReferenceQuery query, Reference reference) {
        String ownerDescriptor = toDescriptor(query.owner());
        return switch (kind) {
            case "class" -> (reference instanceof TypeReference type
                    && type.getType().equals(ownerDescriptor))
                    || (reference instanceof MethodReference method
                    && method.getDefiningClass().equals(ownerDescriptor))
                    || (reference instanceof FieldReference field
                    && field.getDefiningClass().equals(ownerDescriptor));
            case "method" -> reference instanceof MethodReference method
                    && method.getDefiningClass().equals(ownerDescriptor)
                    && method.getName().equals(query.name())
                    && DexDescriptors.methodDescriptor(method).equals(query.descriptor());
            case "field" -> reference instanceof FieldReference field
                    && field.getDefiningClass().equals(ownerDescriptor)
                    && field.getName().equals(query.name())
                    && field.getType().equals(query.descriptor());
            default -> false;
        };
    }

    private static void validateReferenceTarget(String kind, ReferenceQuery query) {
        toDescriptor(query.owner());
        if (!kind.equals("class")
                && (query.name() == null || query.name().isBlank()
                || query.descriptor() == null || query.descriptor().isBlank())) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "name and descriptor are required for method and field references");
        }
    }

    private static String referenceKind(Reference reference) {
        if (reference instanceof MethodReference) return "method";
        if (reference instanceof FieldReference) return "field";
        if (reference instanceof TypeReference) return "class";
        if (reference instanceof StringReference) return "string";
        return "reference";
    }

    private static Object constant(Instruction instruction) {
        if (instruction instanceof ReferenceInstruction referenceInstruction
                && referenceInstruction.getReference() instanceof StringReference string) {
            return string.getString();
        }
        if (instruction instanceof NarrowLiteralInstruction narrow) return narrow.getNarrowLiteral();
        if (instruction instanceof WideLiteralInstruction wide) return wide.getWideLiteral();
        return null;
    }

    private static boolean matchesConstant(String type, ConstantQuery query, Object value) {
        if (type.equals("string") && !(value instanceof String)) return false;
        if (type.equals("number") && !(value instanceof Number)) return false;
        String candidate = String.valueOf(value);
        String expected = query.value() == null ? "" : query.value();
        if (!query.caseSensitive()) {
            candidate = candidate.toLowerCase(Locale.ROOT);
            expected = expected.toLowerCase(Locale.ROOT);
        }
        return query.exact() ? candidate.equals(expected) : candidate.contains(expected);
    }

    private static String normalizeReferenceKind(String value) {
        String kind = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!kind.equals("class") && !kind.equals("method") && !kind.equals("field")) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "kind must be class, method, or field");
        }
        return kind;
    }

    private static String normalizeConstantType(String value) {
        String type = value == null || value.isBlank() ? "all" : value.trim().toLowerCase(Locale.ROOT);
        if (!type.equals("all") && !type.equals("string") && !type.equals("number")) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "type must be all, string, or number");
        }
        return type;
    }

    private static DexValidation validation(DexEditor.Candidate candidate, String target,
                                            long revision) {
        List<DexDiagnostic> diagnostics = candidate.diagnostics().stream()
                .map(value -> new DexDiagnostic(value.line(), value.column(),
                        value.severity(), value.message()))
                .toList();
        return new DexValidation(candidate.valid(), target, candidate.dexFile(),
                candidate.fingerprint(), candidate.bytes() == null ? 0 : candidate.bytes().length,
                diagnostics, revision);
    }

    private static void requireValid(DexEditor.Candidate candidate) {
        if (candidate.valid()) return;
        DexEditor.Diagnostic first = candidate.diagnostics().get(0);
        String location = first.line() <= 0 ? ""
                : String.format(" at line %d, column %d", first.line(), first.column());
        throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                "Invalid smali" + location + ": " + first.message());
    }

    private static void requireClassCommand(DexClassMutation command) {
        if (command == null) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "command must not be null");
        }
    }

    private static void requireMethodCommand(DexMethodMutation command) {
        if (command == null || command.method() == null) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "method must not be null");
        }
        BrowseService.MemberId method = command.method();
        if (method.owner() == null || method.owner().isBlank()
                || method.name() == null || method.name().isBlank()
                || method.descriptor() == null || method.descriptor().isBlank()) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "method owner, name, and descriptor must not be blank");
        }
    }

    private static String methodIdentity(BrowseService.MemberId method) {
        return String.format("%s.%s%s", method.owner(), method.name(), method.descriptor());
    }

    private static DexJavaDecompiler.ClassView decompile(JavaDecompileRequest request) {
        try {
            return request.decompiler().decompile(request.inputs(), request.internalName());
        } catch (RuntimeException exception) {
            throw new ApplicationException(ApplicationException.Code.INTERNAL_ERROR,
                    "Unable to decompile DEX class " + request.internalName() + ": "
                            + exception.getMessage(), exception);
        }
    }

    private static String toDescriptor(String internalName) {
        if (internalName == null || internalName.isBlank()) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "owner must not be blank");
        }
        String value = internalName.trim();
        return value.startsWith("L") && value.endsWith(";")
                ? value : String.format("L%s;", value);
    }

    private static int pageLimit(int limit) {
        if (limit <= 0) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "limit must be positive");
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private record JavaDecompileRequest(DexJavaDecompiler.Workspace decompiler,
                                        List<DexJavaDecompiler.Input> inputs,
                                        String internalName, long revision) {
    }

    private static int count(Iterable<?> values) {
        int count = 0;
        for (Object ignored : values) count++;
        return count;
    }
}
