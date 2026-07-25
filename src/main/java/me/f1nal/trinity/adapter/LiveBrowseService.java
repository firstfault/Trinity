package me.f1nal.trinity.adapter;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.application.ApplicationException;
import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.hierarchy.MethodHierarchy;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerClipboardCodec;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Desktop adapter for read-only workspace queries; transport-neutral DTOs leave this class. */
final class LiveBrowseService implements BrowseService {
    private static final long DECOMPILE_TIMEOUT_SECONDS = 30L;

    private final LiveApplicationState state;

    LiveBrowseService(LiveApplicationState state) {
        this.state = state;
    }

    @Override
    public ClassInfo getClass(String internalName) {
        return state.read(true, project -> classInfo(requireClass(project, internalName)));
    }

    @Override
    public ClassStructure getClassStructure(String internalName) {
        return state.read(true, project -> {
            ClassInput input = requireClass(project, internalName);
            List<MethodInfo> methods = input.getMethodMap().values().stream()
                    .map(this::methodInfo)
                    .sorted(Comparator.comparing(info -> info.id().name() + info.id().descriptor()))
                    .toList();
            List<FieldInfo> fields = input.getFieldMap().values().stream()
                    .map(this::fieldInfo)
                    .sorted(Comparator.comparing(info -> info.id().name() + info.id().descriptor()))
                    .toList();
            List<InnerClassInfo> innerClasses = list(input.getNode().innerClasses).stream()
                    .map(this::innerClassInfo).toList();
            List<RecordComponentInfo> records = list(input.getNode().recordComponents).stream()
                    .map(this::recordComponentInfo).toList();
            return new ClassStructure(classInfo(input), methods, fields, innerClasses, records);
        });
    }

    @Override
    public SourceView decompileClass(String internalName) {
        long revision = state.revision();
        DecompiledClass output = decompile(internalName);
        return new SourceView(internalName, "java", output.getText(), true, revision);
    }

    @Override
    public MethodInfo getMethod(MemberId method) {
        return state.read(true, project -> methodInfo(requireMethod(project, method)));
    }

    @Override
    public SourceView decompileMethod(MemberId method) {
        long revision = state.revision();
        MethodInput input = state.read(true, project -> requireMethod(project, method));
        DecompiledClass output = decompile(input.getOwningClass().getRealName());
        DecompiledClass.MethodPreview preview = output.getMethodPreview(input, Integer.MAX_VALUE);
        if (preview.lines().isEmpty()) {
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    "Decompiler output did not contain method " + identity(method));
        }
        StringBuilder source = new StringBuilder();
        preview.lines().forEach(line -> {
            line.forEach(component -> source.append(component.getText()));
            source.append('\n');
        });
        return new SourceView(identity(method), "java", source.toString(),
                !preview.skippedLeading() && !preview.hasMoreLines(), revision);
    }

    @Override
    public BytecodeView getMethodBytecode(MemberId method) {
        return state.read(true, project -> {
            MethodInput input = requireMethod(project, method);
            String formatted = AssemblerClipboardCodec.format(
                    List.of(input.getInstructions().toArray()),
                    label -> input.getLabelTable().getLabel(label.getLabel()).getName());
            MethodNode node = input.getNode();
            return new BytecodeView(method, "trinity-assembler-v1", formatted,
                    sha256((node.access + "\n" + node.name + "\n" + node.desc + "\n"
                            + node.maxStack + "\n" + node.maxLocals + "\n" + formatted)
                            .getBytes(StandardCharsets.UTF_8)),
                    node.instructions.size(), node.maxStack, node.maxLocals, state.revision());
        });
    }

    @Override
    public FieldInfo getField(MemberId field) {
        return state.read(true, project -> fieldInfo(requireField(project, field)));
    }

    @Override
    public ResourceView readResource(String path, String encoding) {
        requireText(path, "path");
        String normalizedEncoding = encoding == null || encoding.isBlank()
                ? "base64" : encoding.toLowerCase(Locale.ROOT);
        return state.read(true, project -> {
            byte[] bytes = project.getExecution().getResourceMap().get(path);
            if (bytes == null) {
                throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                        "Resource not found: " + path);
            }
            String content = switch (normalizedEncoding) {
                case "base64" -> Base64.getEncoder().encodeToString(bytes);
                case "utf8", "text" -> new String(bytes, StandardCharsets.UTF_8);
                case "hex" -> java.util.HexFormat.of().formatHex(bytes);
                default -> throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        "encoding must be base64, utf8, text, or hex");
            };
            return new ResourceView(path, normalizedEncoding, content, bytes.length,
                    sha256(bytes), state.revision());
        });
    }

    @Override
    public HierarchyView getClassHierarchy(String internalName) {
        return state.read(true, project -> {
            ClassInput input = requireClass(project, internalName);
            var hierarchy = input.getClassHierarchy();
            List<String> directSubclasses = hierarchy.getInheritors().stream()
                    .filter(candidate -> Objects.equals(candidate.getSuperName(), input.getRealName())
                            || candidate.getInterfaces().contains(input.getRealName()))
                    .map(ClassInput::getRealName).sorted().toList();
            Map<String, List<MemberId>> overrides = new LinkedHashMap<>();
            for (MethodInput method : input.getMethodMap().values()) {
                MethodHierarchy family = method.getMethodHierarchy();
                if (family == null || family.getLinkedMethods().size() < 2) continue;
                overrides.put(method.getName() + method.getDescriptor(), family.getLinkedMethods().stream()
                        .map(LiveBrowseService::memberId)
                        .sorted(Comparator.comparing(LiveBrowseService::identity))
                        .toList());
            }
            return new HierarchyView(input.getRealName(),
                    hierarchy.getSuperClass() == null ? input.getSuperName()
                            : hierarchy.getSuperClass().getRealName(),
                    hierarchy.getSuperClasses().stream().map(ClassInput::getRealName).sorted().toList(),
                    hierarchy.getInterfaces().stream().map(ClassInput::getRealName).sorted().toList(),
                    directSubclasses,
                    hierarchy.getInheritors().stream().map(ClassInput::getRealName).sorted().toList(),
                    overrides, state.revision());
        });
    }

    private DecompiledClass decompile(String internalName) {
        CompletableFuture<DecompiledClass> future = new CompletableFuture<>();
        state.read(true, project -> {
            ClassInput input = requireClass(project, internalName);
            project.getDecompiler().decompile(input, result -> {
                if (result == null) future.completeExceptionally(new IllegalStateException("Decompiler failed"));
                else future.complete(result);
            });
            return null;
        });
        try {
            return future.get(DECOMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException exception) {
            throw new ApplicationException(ApplicationException.Code.TIMEOUT,
                    "Timed out decompiling " + internalName, exception);
        } catch (Exception exception) {
            Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                    ? exception.getCause() : exception;
            throw new ApplicationException(ApplicationException.Code.INTERNAL_ERROR,
                    "Unable to decompile " + internalName + ": " + cause.getMessage(), cause);
        }
    }

    private ClassInfo classInfo(ClassInput input) {
        var node = input.getNode();
        return new ClassInfo(input.getRealName(), input.getDisplayName().getName(), node.access,
                node.version, node.signature, node.superName, list(node.interfaces), node.sourceFile,
                node.outerClass, node.nestHostClass, list(node.permittedSubclasses),
                annotations(node.visibleAnnotations, node.invisibleAnnotations),
                node.methods.size(), node.fields.size(), state.revision());
    }

    private MethodInfo methodInfo(MethodInput input) {
        MethodNode node = input.getNode();
        Type[] argumentTypes = Type.getArgumentTypes(node.desc);
        List<ParameterInfo> parameters = new ArrayList<>(argumentTypes.length);
        for (int i = 0; i < argumentTypes.length; i++) {
            String name = node.parameters != null && i < node.parameters.size()
                    ? node.parameters.get(i).name : null;
            parameters.add(new ParameterInfo(i, argumentTypes[i].getDescriptor(), name));
        }
        return new MethodInfo(memberId(input), input.getDisplayName().getName(), node.access,
                node.signature, list(node.exceptions),
                annotations(node.visibleAnnotations, node.invisibleAnnotations),
                node.instructions.size(), node.maxStack, node.maxLocals,
                size(node.tryCatchBlocks), parameters, state.revision());
    }

    private FieldInfo fieldInfo(FieldInput input) {
        var node = input.getNode();
        return new FieldInfo(memberId(input), input.getDisplayName().getName(), node.access,
                node.signature, jsonValue(node.value),
                annotations(node.visibleAnnotations, node.invisibleAnnotations), state.revision());
    }

    private InnerClassInfo innerClassInfo(InnerClassNode node) {
        return new InnerClassInfo(node.name, node.outerName, node.innerName, node.access);
    }

    private RecordComponentInfo recordComponentInfo(RecordComponentNode node) {
        return new RecordComponentInfo(node.name, node.descriptor, node.signature,
                annotations(node.visibleAnnotations, node.invisibleAnnotations));
    }

    static ClassInput requireClass(Trinity project, String internalName) {
        requireText(internalName, "internalName");
        ClassInput input = project.getExecution().getClassInput(internalName);
        if (input == null) {
            if (project.getExecution().getDexIndex().getClass(internalName) != null) {
                throw new ApplicationException(ApplicationException.Code.UNSUPPORTED_OPERATION,
                        String.format("DEX class %s is read-only; use the native DEX tools", internalName));
            }
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    String.format("Class not found: %s", internalName));
        }
        return input;
    }

    static MethodInput requireMethod(Trinity project, MemberId id) {
        requireText(id.owner(), "owner");
        requireText(id.name(), "name");
        requireText(id.descriptor(), "descriptor");
        ClassInput owner = requireClass(project, id.owner());
        MethodInput method = owner.getDeclaredMethod(id.name(), id.descriptor());
        if (method == null) {
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    "Method not found: " + identity(id));
        }
        return method;
    }

    static FieldInput requireField(Trinity project, MemberId id) {
        requireText(id.owner(), "owner");
        requireText(id.name(), "name");
        requireText(id.descriptor(), "descriptor");
        ClassInput owner = requireClass(project, id.owner());
        FieldInput field = owner.getDeclaredField(id.name(), id.descriptor());
        if (field == null) {
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    "Field not found: " + identity(id));
        }
        return field;
    }

    static MemberId memberId(MethodInput input) {
        return new MemberId(input.getOwningClass().getRealName(), input.getName(), input.getDescriptor());
    }

    static MemberId memberId(FieldInput input) {
        return new MemberId(input.getOwningClass().getRealName(), input.getNode().name, input.getDescriptor());
    }

    static String identity(MemberId id) {
        return String.format("%s.%s%s", id.owner(), id.name(), id.descriptor());
    }

    private static List<String> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        List<String> output = new ArrayList<>();
        list(visible).stream().map(annotation -> annotation.desc).forEach(output::add);
        list(invisible).stream().map(annotation -> annotation.desc).forEach(output::add);
        return List.copyOf(output);
    }

    private static Object jsonValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return value.toString();
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static <T> List<T> list(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    field + " must not be blank");
        }
    }
}
