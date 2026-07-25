package me.f1nal.trinity.adapter;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.application.ApplicationException;
import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.application.MutationService;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerClipboardCodec;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerDocument;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerValidationResult;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerValidator;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.util.AnnotationUtil;
import me.f1nal.trinity.util.InstructionUtil;
import me.f1nal.trinity.util.NameUtil;
import me.f1nal.trinity.util.annotations.AnnotationDescriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Desktop adapter for revision-protected mutations. */
final class LiveMutationService implements MutationService {
    private static final int MAX_RESOURCE_BYTES = 64 * 1024 * 1024;

    private final LiveApplicationState state;
    private final Map<String, StoredPreview> previews = new ConcurrentHashMap<>();

    LiveMutationService(LiveApplicationState state) {
        this.state = state;
    }

    @Override
    public MutationResult setName(NameMutation command) {
        return rename(command.target(), command.newName(), command.expectedRevision(), false);
    }

    @Override
    public MutationResult revertName(NameTarget target, long expectedRevision) {
        return rename(target, null, expectedRevision, true);
    }

    @Override
    public MutationResult createResource(ResourceMutation command) {
        String path = resourcePath(command.path());
        byte[] bytes = decode(command.encoding(), command.content());
        if (bytes.length > MAX_RESOURCE_BYTES) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "Resource exceeds the 64 MiB limit");
        }
        LiveApplicationState.Changed<String> changed = state.mutate(command.expectedRevision(), project -> {
            if (project.getExecution().getResourceMap().containsKey(path)) {
                throw new ApplicationException(ApplicationException.Code.TARGET_ALREADY_EXISTS,
                        "Resource already exists: " + path);
            }
            if (project.getExecution().createResource(project.getExecution().getRootPackage(), path, bytes) == null) {
                throw new ApplicationException(ApplicationException.Code.TARGET_ALREADY_EXISTS,
                        "Resource already exists: " + path);
            }
            return path;
        });
        return result("resource_create", path, changed, List.of(path));
    }

    @Override
    public MutationResult deleteResource(String path, long expectedRevision) {
        String normalized = resourcePath(path);
        LiveApplicationState.Changed<String> changed = state.mutate(expectedRevision, project -> {
            ResourceArchiveEntry resource = findResource(project, normalized);
            if (resource == null) {
                throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                        "Resource not found: " + normalized);
            }
            project.getExecution().deleteResource(resource);
            return normalized;
        });
        return result("resource_delete", normalized, changed, List.of(normalized));
    }

    @Override
    public BytecodeValidation validateBytecode(BytecodeCommand command) {
        return state.read(true, project -> {
            state.checkRevision(command.expectedRevision());
            MethodInput input = LiveBrowseService.requireMethod(project, command.method());
            Candidate candidate = candidate(input, command);
            return validation(candidate, state.revision());
        });
    }

    @Override
    public MutationResult replaceBytecode(BytecodeCommand command) {
        LiveApplicationState.Changed<String> changed = state.mutate(command.expectedRevision(), project -> {
            MethodInput input = LiveBrowseService.requireMethod(project, command.method());
            Candidate candidate = candidate(input, command);
            if (!candidate.validation().isValid()) {
                throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        String.join("; ", candidate.validation().getErrors()));
            }
            candidate.document().commit(candidate.method());
            project.getExecution().getXrefMap().refreshMethod(input);
            project.getDecompiler().invalidateCache(input.getOwningClass());
            project.getEventManager().postEvent(new EventMemberModified(input));
            return LiveBrowseService.identity(command.method());
        });
        return result("method_replace_bytecode", changed.value(), changed,
                List.of(changed.value()));
    }

    @Override
    public RefactorPreview previewRefactor(RefactorRequest request) {
        return state.read(true, project -> {
            state.checkRevision(request.expectedRevision());
            List<ProposedRename> renames = switch (normalizeMode(request.mode())) {
                case "full" -> fullRenames(project);
                case "enum_fields" -> enumFieldRenames(project);
                case "mixins" -> mixinRenames(project, request.mixinPackage());
                default -> throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        "mode must be full, enum_fields, or mixins");
            };
            renames = renames.stream()
                    .filter(rename -> !rename.currentName().equals(rename.proposedName()))
                    .toList();
            String token = UUID.randomUUID().toString();
            RefactorPreview preview = new RefactorPreview(token, normalizeMode(request.mode()),
                    renames, state.revision());
            previews.put(token, new StoredPreview(preview));
            return preview;
        });
    }

    @Override
    public MutationResult applyRefactor(ApplyRefactor command) {
        StoredPreview stored = previews.remove(command.previewToken());
        if (stored == null) {
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    "Refactor preview token is unknown or has already been used");
        }
        if (stored.preview().revision() != command.expectedRevision()) {
            throw new ApplicationException(ApplicationException.Code.REVISION_CONFLICT,
                    "The refactor preview does not belong to the expected revision");
        }
        LiveApplicationState.Changed<List<String>> changed = state.mutate(
                command.expectedRevision(), project -> {
                    List<String> targets = new ArrayList<>();
                    for (ProposedRename rename : stored.preview().renames()) {
                        applyName(project, rename.target(), rename.proposedName(), false);
                        targets.add(targetIdentity(rename.target()));
                    }
                    return targets;
                });
        return new MutationResult("refactor_apply", command.previewToken(),
                changed.previousRevision(), changed.revision(), changed.value());
    }

    private MutationResult rename(NameTarget target, String newName, long expectedRevision,
                                  boolean revert) {
        if (!revert && (newName == null || newName.isBlank())) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "newName must not be blank");
        }
        LiveApplicationState.Changed<String> changed = state.mutate(expectedRevision, project -> {
            applyName(project, target, newName, revert);
            return targetIdentity(target);
        });
        return result(revert ? "name_revert" : "name_set", changed.value(), changed,
                List.of(changed.value()));
    }

    private void applyName(Trinity project, NameTarget target, String newName, boolean revert) {
        String kind = normalizeMode(target.kind());
        switch (kind) {
            case "class" -> {
                ClassInput input = LiveBrowseService.requireClass(project, target.owner());
                input.rename(project.getRemapper(), revert ? original(input.getDisplayName()) : newName);
            }
            case "method" -> {
                MethodInput input = LiveBrowseService.requireMethod(project,
                        new BrowseService.MemberId(target.owner(), target.name(), target.descriptor()));
                input.rename(project.getRemapper(), revert ? original(input.getDisplayName()) : newName);
            }
            case "field" -> {
                FieldInput input = LiveBrowseService.requireField(project,
                        new BrowseService.MemberId(target.owner(), target.name(), target.descriptor()));
                input.rename(project.getRemapper(), revert ? original(input.getDisplayName()) : newName);
            }
            case "resource" -> {
                if (revert) throw unsupportedRevert(kind);
                ResourceArchiveEntry resource = findResource(project, resourcePath(target.path()));
                if (resource == null) throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                        "Resource not found: " + target.path());
                project.getExecution().renameResource(resource, resourcePath(newName));
            }
            case "package" -> {
                if (revert) throw unsupportedRevert(kind);
                Package pkg = project.getExecution().getAllPackages().stream()
                        .filter(candidate -> candidate.getPrettyPath().replace('.', '/').equals(target.path()))
                        .findFirst().orElseThrow(() -> new ApplicationException(
                                ApplicationException.Code.TARGET_NOT_FOUND,
                                "Package not found: " + target.path()));
                pkg.rename(project.getRemapper(), newName);
            }
            default -> throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "kind must be class, method, field, resource, or package");
        }
    }

    private Candidate candidate(MethodInput input, BytecodeCommand command) {
        AssemblerDocument document = new AssemblerDocument(input);
        Map<String, LabelNode> labels = clonedLabels(input, document.getMethod());
        final AssemblerClipboardCodec.ParsedInstructions parsed;
        try {
            parsed = AssemblerClipboardCodec.parse(command.instructions(), labels::get);
        } catch (IllegalArgumentException exception) {
            AssemblerValidationResult invalid = new AssemblerValidationResult();
            invalid.error(exception.getMessage());
            return new Candidate(document, document.getMethod(), invalid);
        }
        MethodNode method = document.buildCandidate(parsed.instructions());
        if (command.maxStack() != null) method.maxStack = command.maxStack();
        if (command.maxLocals() != null) method.maxLocals = command.maxLocals();
        int normalizedDebugEntries = sanitizeDebugMetadata(method);
        AssemblerValidationResult validation =
                AssemblerValidator.validate(input.getOwningClass(), method);
        if (normalizedDebugEntries > 0) {
            validation.warning("Normalized " + normalizedDebugEntries
                    + " debug metadata entries whose labels were absent from the replacement");
        }
        return new Candidate(document, method, validation);
    }

    private static int sanitizeDebugMetadata(MethodNode method) {
        java.util.Set<LabelNode> labels =
                java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        List<LabelNode> orderedLabels = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label && labels.add(label)) orderedLabels.add(label);
        }
        LabelNode first = orderedLabels.isEmpty() ? null : orderedLabels.get(0);
        LabelNode last = orderedLabels.isEmpty() ? null : orderedLabels.get(orderedLabels.size() - 1);
        int normalized = 0;
        if (method.localVariables != null) {
            for (var iterator = method.localVariables.iterator(); iterator.hasNext(); ) {
                var local = iterator.next();
                if (labels.contains(local.start) && labels.contains(local.end)) continue;
                if (first == null) {
                    iterator.remove();
                } else {
                    if (!labels.contains(local.start)) local.start = first;
                    if (!labels.contains(local.end)) local.end = last;
                }
                normalized++;
            }
        }
        if (method.visibleLocalVariableAnnotations != null) {
            int before = method.visibleLocalVariableAnnotations.size();
            method.visibleLocalVariableAnnotations.removeIf(annotation ->
                    !labels.containsAll(annotation.start) || !labels.containsAll(annotation.end));
            normalized += before - method.visibleLocalVariableAnnotations.size();
        }
        if (method.invisibleLocalVariableAnnotations != null) {
            int before = method.invisibleLocalVariableAnnotations.size();
            method.invisibleLocalVariableAnnotations.removeIf(annotation ->
                    !labels.containsAll(annotation.start) || !labels.containsAll(annotation.end));
            normalized += before - method.invisibleLocalVariableAnnotations.size();
        }
        return normalized;
    }

    private BytecodeValidation validation(Candidate candidate, long revision) {
        AssemblerValidationResult result = candidate.validation();
        return new BytecodeValidation(result.isValid(),
                AssemblerDocument.fingerprint(candidate.method()), result.getErrors(), result.getWarnings(),
                candidate.method().instructions.size(), candidate.method().maxStack,
                candidate.method().maxLocals, revision);
    }

    private static Map<String, LabelNode> clonedLabels(MethodInput input, MethodNode clone) {
        Map<org.objectweb.asm.Label, String> names = new IdentityHashMap<>();
        for (AbstractInsnNode instruction : input.getInstructions()) {
            if (instruction instanceof LabelNode label) {
                names.put(label.getLabel(), input.getLabelTable().getLabel(label.getLabel()).getName());
            }
        }
        List<String> orderedNames = new ArrayList<>(names.values());
        Map<String, LabelNode> output = new LinkedHashMap<>();
        int index = 0;
        for (AbstractInsnNode instruction : clone.instructions) {
            if (instruction instanceof LabelNode label && index < orderedNames.size()) {
                output.put(orderedNames.get(index++), label);
            }
        }
        return output;
    }

    private List<ProposedRename> fullRenames(Trinity project) {
        List<ProposedRename> output = new ArrayList<>();
        int classIndex = 0;
        int methodIndex = 0;
        int fieldIndex = 0;
        for (ClassInput input : project.getExecution().getClassList()) {
            output.add(proposal(classTarget(input), input.getDisplayName().getName(),
                    "Class" + ++classIndex));
            for (MethodInput method : input.getMethodMap().values()) {
                if (!method.isInitOrClinit()) output.add(proposal(methodTarget(method),
                        method.getDisplayName().getName(), "method" + ++methodIndex));
            }
            for (FieldInput field : input.getFieldMap().values()) {
                output.add(proposal(fieldTarget(field), field.getDisplayName().getName(),
                        "field" + ++fieldIndex));
            }
        }
        return List.copyOf(output);
    }

    private List<ProposedRename> enumFieldRenames(Trinity project) {
        List<ProposedRename> output = new ArrayList<>();
        for (ClassInput input : project.getExecution().getClassList()) {
            if (!input.getAccessFlags().isEnum()) continue;
            MethodInput clinit = input.getDeclaredMethod("<clinit>", "()V");
            if (clinit == null) continue;
            Map<String, FieldInput> fields = new HashMap<>();
            input.getFieldMap().values().stream().filter(field -> field.getAccessFlags().isEnum())
                    .forEach(field -> fields.put(field.getNode().name + field.getDescriptor(), field));
            for (AbstractInsnNode instruction : clinit.getInstructions()) {
                if (instruction.getOpcode() != Opcodes.NEW) continue;
                AbstractInsnNode current = instruction;
                try {
                    if (!NameUtil.internalToNormal(((TypeInsnNode) current).desc).equals(input.getRealName())) continue;
                    if ((current = current.getNext()).getOpcode() != Opcodes.DUP) continue;
                    if (!((current = current.getNext()) instanceof LdcInsnNode ldc)
                            || !(ldc.cst instanceof String proposed)) continue;
                    if (!InstructionUtil.isIntegerInstruction(current = current.getNext())) continue;
                    for (int depth = 0; depth < 8 && (current = current.getNext()) != null; depth++) {
                        if (current instanceof FieldInsnNode field && current.getOpcode() == Opcodes.PUTSTATIC) {
                            FieldInput target = fields.remove(field.name + field.desc);
                            if (target != null) output.add(proposal(fieldTarget(target),
                                    target.getDisplayName().getName(), proposed));
                            break;
                        }
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }
        return List.copyOf(output);
    }

    private List<ProposedRename> mixinRenames(Trinity project, String packageName) {
        String prefix = packageName == null || packageName.isBlank()
                ? "mixins/" : packageName.replace('.', '/');
        if (!prefix.endsWith("/")) prefix += "/";
        List<ProposedRename> output = new ArrayList<>();
        for (ClassInput input : project.getExecution().getClassList()) {
            AnnotationDescriptor mixin = AnnotationUtil.getAnnotation(input.getNode().invisibleAnnotations,
                    "org/spongepowered/asm/mixin/Mixin");
            if (mixin != null && mixin.getValues().get("value") instanceof List<?> values
                    && !values.isEmpty() && values.get(0) instanceof Type type) {
                output.add(proposal(classTarget(input), input.getDisplayName().getName(), prefix
                        + NameUtil.getSimpleName(NameUtil.internalToNormal(type.getInternalName())) + "Mixin"));
            }
            for (MethodInput method : input.getMethodMap().values()) {
                AnnotationDescriptor inject = AnnotationUtil.getAnnotation(method.getNode().visibleAnnotations,
                        "org/spongepowered/asm/mixin/injection/Inject");
                if (inject == null || !(inject.getValues().get("method") instanceof List<?> methods)
                        || methods.isEmpty() || !(methods.get(0) instanceof String name)) continue;
                int separator = Math.max(name.indexOf('('), name.indexOf('*'));
                String proposed = separator < 0 ? name : name.substring(0, separator);
                if (!proposed.isBlank()) output.add(proposal(methodTarget(method),
                        method.getDisplayName().getName(), proposed));
            }
        }
        return List.copyOf(output);
    }

    private static ProposedRename proposal(NameTarget target, String current, String proposed) {
        return new ProposedRename(target, current, proposed);
    }

    private static NameTarget classTarget(ClassInput input) {
        return new NameTarget("class", input.getRealName(), null, null, null);
    }

    private static NameTarget methodTarget(MethodInput input) {
        return new NameTarget("method", input.getOwningClass().getRealName(), input.getName(),
                input.getDescriptor(), null);
    }

    private static NameTarget fieldTarget(FieldInput input) {
        return new NameTarget("field", input.getOwningClass().getRealName(), input.getNode().name,
                input.getDescriptor(), null);
    }

    private static String original(DisplayName name) {
        return name.getOriginalName();
    }

    private static ApplicationException unsupportedRevert(String kind) {
        return new ApplicationException(ApplicationException.Code.UNSUPPORTED_OPERATION,
                "Original names are not retained for " + kind + " targets");
    }

    private static ResourceArchiveEntry findResource(Trinity project, String path) {
        return project.getExecution().getAllPackages().stream()
                .flatMap(pkg -> pkg.getEntries().stream())
                .filter(ResourceArchiveEntry.class::isInstance)
                .map(ResourceArchiveEntry.class::cast)
                .filter(resource -> resource.getRealName().equals(path))
                .findFirst().orElse(null);
    }

    private static byte[] decode(String encoding, String content) {
        String normalized = encoding == null || encoding.isBlank()
                ? "base64" : encoding.toLowerCase(Locale.ROOT);
        String value = Objects.requireNonNullElse(content, "");
        try {
            return switch (normalized) {
                case "base64" -> Base64.getDecoder().decode(value);
                case "utf8", "text" -> value.getBytes(StandardCharsets.UTF_8);
                case "hex" -> java.util.HexFormat.of().parseHex(value);
                default -> throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        "encoding must be base64, utf8, text, or hex");
            };
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "Invalid " + normalized + " resource content", exception);
        }
    }

    private static String resourcePath(String path) {
        if (path == null || path.isBlank()) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "path must not be blank");
        }
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.isBlank() || normalized.contains("../") || normalized.equals("..")) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "path must be a relative archive path without parent traversal");
        }
        return normalized;
    }

    private static String normalizeMode(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String targetIdentity(NameTarget target) {
        return switch (normalizeMode(target.kind())) {
            case "class" -> target.owner();
            case "method", "field" -> target.owner() + "." + target.name() + target.descriptor();
            case "resource", "package" -> target.path();
            default -> Objects.toString(target.kind(), "");
        };
    }

    private static MutationResult result(String operation, String target,
                                         LiveApplicationState.Changed<?> changed,
                                         List<String> targets) {
        return new MutationResult(operation, target, changed.previousRevision(),
                changed.revision(), targets);
    }

    private record Candidate(AssemblerDocument document, MethodNode method,
                             AssemblerValidationResult validation) {
    }

    private record StoredPreview(RefactorPreview preview) {
    }
}
