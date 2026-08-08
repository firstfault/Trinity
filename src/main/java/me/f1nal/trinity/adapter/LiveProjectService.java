package me.f1nal.trinity.adapter;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.application.ApplicationException;
import me.f1nal.trinity.application.Page;
import me.f1nal.trinity.application.ProjectService;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.database.compression.DatabaseCompressionType;
import me.f1nal.trinity.database.compression.DatabaseCompressionTypeManager;
import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.ApkmArchiveLayout;
import me.f1nal.trinity.database.inputs.ProjectInputSet;
import me.f1nal.trinity.database.inputs.impl.ProjectInputAPKMFile;
import me.f1nal.trinity.database.inputs.impl.ProjectInputClassFile;
import me.f1nal.trinity.database.inputs.impl.ProjectInputDEXFile;
import me.f1nal.trinity.database.inputs.impl.ProjectInputJARFile;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.dex.DexClassEntry;
import me.f1nal.trinity.execution.dex.DexDescriptors;
import me.f1nal.trinity.execution.dex.DexFileUnit;
import me.f1nal.trinity.execution.compile.SafeClassWriter;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.util.FileUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/** Desktop adapter for project use cases; no protocol or MCP types cross this boundary. */
final class LiveProjectService implements ProjectService {
    private static final int MAX_INPUT_BYTES = 512 * 1024 * 1024;
    private static final int MAX_PAGE_SIZE = 500;

    private final LiveApplicationState state;

    LiveProjectService(LiveApplicationState state) {
        this.state = state;
    }

    @Override
    public ProjectSnapshot current() {
        return state.read(false, this::snapshot);
    }

    @Override
    public ProjectSnapshot create(CreateProject command) {
        requireText(command.name(), "name");
        Path databasePath = absolutePath(command.databasePath(), "databasePath");
        if (Files.exists(databasePath)) {
            throw new ApplicationException(ApplicationException.Code.TARGET_ALREADY_EXISTS,
                    "Database already exists: " + databasePath);
        }
        DatabaseCompressionType compression = compression(command.compression());
        ProjectInputSet projectInput = readProjectInput(command.inputPaths());
        boolean hasCode = projectInput.getContainers().stream().anyMatch(input ->
                !input.getClassPath().getClasses().isEmpty()
                        || !input.getClassPath().getDexFiles().isEmpty());
        if (!hasCode) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "At least one JVM class or DEX input is required");
        }

        try {
            Path parent = databasePath.getParent();
            if (parent != null) Files.createDirectories(parent);
            LiveApplicationState.Changed<Trinity> installed = state.createAndInstall(() -> {
                Database database = new Database(command.name().trim(), databasePath.toFile(), compression);
                Trinity project = new Trinity(database, projectInput);
                database.setLoaded(project);
                return project;
            });
            return snapshot(installed.value(), installed.revision());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException(ApplicationException.Code.IO_FAILURE,
                    "Unable to create project: " + exception.getMessage(), exception);
        }
    }

    @Override
    public ProjectSnapshot open(OpenProject command) {
        Path path = absolutePath(command.databasePath(), "databasePath");
        if (!Files.isRegularFile(path)) {
            throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                    "Database does not exist: " + path);
        }
        try {
            LiveApplicationState.Changed<Trinity> installed =
                    state.createAndInstall(() -> DatabaseLoader.loadProject(path.toFile()));
            return snapshot(installed.value(), installed.revision());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException(ApplicationException.Code.IO_FAILURE,
                    "Unable to open project: " + exception.getMessage(), exception);
        }
    }

    @Override
    public ProjectSnapshot save(long expectedRevision) {
        return state.read(false, project -> {
            state.checkRevision(expectedRevision);
            File path = project.getDatabase().getPath();
            if (path == null) {
                throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        "The active project has no database path");
            }
            try {
                DatabaseLoader.saveProject(project, path);
            } catch (IOException exception) {
                throw new ApplicationException(ApplicationException.Code.IO_FAILURE,
                        "Unable to save project: " + exception.getMessage(), exception);
            }
            return snapshot(project);
        });
    }

    @Override
    public ProjectSnapshot close(CloseProject command) {
        ProjectSnapshot closing = state.read(false, project -> {
            state.checkRevision(command.expectedRevision());
            if (command.save()) {
                try {
                    DatabaseLoader.saveProject(project, project.getDatabase().getPath());
                } catch (IOException exception) {
                    throw new ApplicationException(ApplicationException.Code.IO_FAILURE,
                            "Unable to save project before closing: " + exception.getMessage(), exception);
                }
            }
            return snapshot(project);
        });
        LiveApplicationState.Changed<Trinity> removed = state.uninstall(command.expectedRevision());
        return new ProjectSnapshot(closing.name(), closing.databasePath(), closing.compression(),
                false, closing.classCount(), closing.resourceCount(), closing.packageCount(),
                removed.revision());
    }

    @Override
    public ExportResult exportJar(ExportJar command) {
        Path output = absolutePath(command.outputPath(), "outputPath");
        return state.read(true, project -> {
            state.checkRevision(command.expectedRevision());
            try {
                Path parent = output.getParent();
                if (parent != null) Files.createDirectories(parent);
                Map<String, byte[]> entries = exportEntries(project);
                try (JarOutputStream jar = new JarOutputStream(new BufferedOutputStream(
                        Files.newOutputStream(output, StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING)))) {
                    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                        jar.putNextEntry(new ZipEntry(entry.getKey()));
                        jar.write(entry.getValue());
                        jar.closeEntry();
                    }
                }
                return new ExportResult(output.toString(), Files.size(output),
                        project.getExecution().getClassList().size()
                                + project.getExecution().getDexIndex().classCount(),
                        project.getExecution().getResourceMap().size(), state.revision());
            } catch (Exception exception) {
                throw new ApplicationException(ApplicationException.Code.IO_FAILURE,
                        "Unable to export archive: " + exception.getMessage(), exception);
            }
        });
    }

    @Override
    public Page<TreeEntry> tree(TreeQuery query) {
        int limit = pageLimit(query.limit());
        int offset = pageOffset(query.offset());
        String prefix = normalize(query.prefix());
        String kind = normalizeKind(query.kind());
        return state.read(true, project -> {
            List<TreeEntry> entries = new ArrayList<>();
            if (includes(kind, "package")) {
                packageNames(project).stream()
                        .filter(path -> matchesPrefix(path, prefix))
                        .forEach(path -> entries.add(new TreeEntry("package", path, path, null)));
            }
            if (includes(kind, "class")) {
                project.getExecution().getClassList().stream()
                        .sorted(Comparator.comparing(ClassInput::getRealName))
                        .filter(input -> matchesPrefix(input.getRealName(), prefix))
                        .forEach(input -> entries.add(new TreeEntry("class", input.getRealName(),
                                input.getDisplayName().getName(), null)));
            }
            if (includes(kind, "dex_class")) {
                project.getExecution().getDexIndex().getClasses().stream()
                        .sorted(Comparator.comparing(DexClassEntry::getInternalName))
                        .filter(input -> matchesPrefix(input.getInternalName(), prefix))
                        .forEach(input -> entries.add(new TreeEntry("dex_class",
                                input.getInternalName(), input.getInternalName(), null)));
            }
            if (includes(kind, "resource")) {
                project.getExecution().getResourceMap().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .filter(entry -> matchesPrefix(entry.getKey(), prefix))
                        .forEach(entry -> entries.add(new TreeEntry("resource", entry.getKey(),
                                entry.getKey(), (long) entry.getValue().length)));
            }
            entries.sort(Comparator.comparing(TreeEntry::path).thenComparing(TreeEntry::kind));
            return Page.slice(entries, offset, limit);
        });
    }

    @Override
    public Page<SearchResult> search(SearchQuery query) {
        int limit = pageLimit(query.limit());
        int offset = pageOffset(query.offset());
        String term = normalize(query.query()).toLowerCase(Locale.ROOT);
        String kind = normalizeKind(query.kind());
        return state.read(true, project -> {
            List<SearchResult> results = new ArrayList<>();
            if (includes(kind, "class")) {
                for (ClassInput input : project.getExecution().getClassList()) {
                    addSearch(results, "class", input.getRealName(), input.getDisplayName().getName(),
                            null, null, term);
                    if (includes(kind, "method")) {
                        for (MethodInput method : input.getMethodMap().values()) {
                            addSearch(results, "method", identity(method), method.getDisplayName().getName(),
                                    input.getRealName(), method.getDescriptor(), term);
                        }
                    }
                    if (includes(kind, "field")) {
                        for (FieldInput field : input.getFieldMap().values()) {
                            addSearch(results, "field", identity(field), field.getDisplayName().getName(),
                                    input.getRealName(), field.getDescriptor(), term);
                        }
                    }
                }
            } else {
                for (ClassInput input : project.getExecution().getClassList()) {
                    if (includes(kind, "method")) for (MethodInput method : input.getMethodMap().values()) {
                        addSearch(results, "method", identity(method), method.getDisplayName().getName(),
                                input.getRealName(), method.getDescriptor(), term);
                    }
                    if (includes(kind, "field")) for (FieldInput field : input.getFieldMap().values()) {
                        addSearch(results, "field", identity(field), field.getDisplayName().getName(),
                                input.getRealName(), field.getDescriptor(), term);
                    }
                }
            }
            for (DexClassEntry input : project.getExecution().getDexIndex().getClasses()) {
                if (includes(kind, "dex_class")) {
                    addSearch(results, "dex_class", input.getInternalName(), input.getInternalName(),
                            null, input.getClassDef().getType(), term);
                }
                if (includes(kind, "dex_method")) {
                    for (Method method : input.getClassDef().getMethods()) {
                        String descriptor = DexDescriptors.methodDescriptor(method);
                        addSearch(results, "dex_method",
                                String.format("%s.%s%s", input.getInternalName(), method.getName(), descriptor),
                                method.getName(), input.getInternalName(), descriptor, term);
                    }
                }
                if (includes(kind, "dex_field")) {
                    for (Field field : input.getClassDef().getFields()) {
                        addSearch(results, "dex_field",
                                String.format("%s.%s:%s", input.getInternalName(), field.getName(), field.getType()),
                                field.getName(), input.getInternalName(), field.getType(), term);
                    }
                }
            }
            if (includes(kind, "resource")) {
                project.getExecution().getResourceMap().keySet().forEach(path ->
                        addSearch(results, "resource", path, path, null, null, term));
            }
            if (includes(kind, "package")) {
                packageNames(project).forEach(path ->
                        addSearch(results, "package", path, path, null, null, term));
            }
            results.sort(Comparator.comparingInt(SearchResult::score).reversed()
                    .thenComparing(SearchResult::identity));
            return Page.slice(results, offset, limit);
        });
    }

    private Map<String, byte[]> exportEntries(Trinity project) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>(project.getExecution().getResourceMap());
        for (ClassInput input : project.getExecution().getClassList()) {
            SafeClassWriter writer = new SafeClassWriter(
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS,
                    name -> type(project, name), (format, arguments) -> { });
            input.getNode().accept(writer);
            String entryName = String.format("%s.class", input.getNode().name);
            if (entries.putIfAbsent(entryName, writer.toByteArray()) != null) {
                throw new IllegalArgumentException(
                        "Archive entry collision while exporting JVM class: " + entryName);
            }
        }
        for (DexFileUnit dexFile : project.getExecution().getDexIndex().getFiles()) {
            String entryName = ApkmArchiveLayout.isBundleEntry(dexFile.getName())
                    ? dexFile.getName() : dexArchiveEntryName(dexFile.getName());
            if (entries.putIfAbsent(entryName, dexFile.getBytes()) != null) {
                throw new IllegalArgumentException(String.format(
                        "Archive entry collision while exporting DEX file: %s", entryName));
            }
        }
        return ApkmArchiveLayout.materialize(entries);
    }

    private static String dexArchiveEntryName(String name) {
        int separator = name.indexOf("!/");
        return separator < 0 ? name : name.substring(separator + 2);
    }

    private static ClassNode type(Trinity project, String name) {
        ClassInput input = project.getExecution().getClassInput(name);
        return input == null ? project.getExecution().getDependencies().getClass(name) : input.getNode();
    }

    private ProjectInputSet readProjectInput(List<String> inputPaths) {
        if (inputPaths == null || inputPaths.isEmpty()) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    "inputPaths must contain at least one JAR, ZIP, APK, APKM, class, or DEX file");
        }
        ProjectInputSet projectInput = new ProjectInputSet();
        for (String value : inputPaths) {
            Path path = absolutePath(value, "inputPaths");
            if (!Files.isRegularFile(path)) {
                throw new ApplicationException(ApplicationException.Code.TARGET_NOT_FOUND,
                        "Input does not exist: " + path);
            }
            try {
                byte[] bytes = FileUtil.readAllBytes(path.toFile(), MAX_INPUT_BYTES, "Project input");
                if (bytes.length < Integer.BYTES) {
                    throw new IOException("Input is too short");
                }
                AbstractProjectInputFile input;
                if (ByteBuffer.wrap(bytes, 0, Integer.BYTES).getInt(0) == 0xcafebabe) {
                    input = new ProjectInputClassFile(path.toFile(), bytes);
                } else if (bytes[0] == 'd' && bytes[1] == 'e'
                        && bytes[2] == 'x' && bytes[3] == '\n') {
                    input = new ProjectInputDEXFile(path.toFile(), bytes);
                } else if (path.getFileName().toString().toLowerCase(Locale.ROOT)
                        .endsWith(".apkm")) {
                    input = new ProjectInputAPKMFile(path.toFile(), bytes);
                } else {
                    input = new ProjectInputJARFile(path.toFile(), bytes);
                }
                if (input.getContainerKind() == ProjectContainerKind.JAR) {
                    projectInput.addJar(input.getName(), input.getClassPath());
                } else {
                    projectInput.addLoose(input.getClassPath());
                }
            } catch (IOException exception) {
                throw new ApplicationException(ApplicationException.Code.IO_FAILURE,
                        "Unable to read input " + path + ": " + exception.getMessage(), exception);
            }
        }
        return projectInput;
    }

    private ProjectSnapshot snapshot(Trinity project) {
        return snapshot(project, state.revision());
    }

    private ProjectSnapshot snapshot(Trinity project, long revision) {
        Database database = project.getDatabase();
        File path = database.getPath();
        return new ProjectSnapshot(database.getName(), path == null ? "" : path.getAbsolutePath(),
                database.getCompressionType().getName(),
                project.getExecution().getAsynchronousLoad().isFinished(),
                project.getExecution().getClassList().size()
                        + project.getExecution().getDexIndex().classCount(),
                project.getExecution().getResourceMap().size(),
                project.getExecution().getAllPackages().size(), revision);
    }

    private static DatabaseCompressionType compression(String name) {
        String expected = name == null || name.isBlank() ? "LZ4" : name.trim();
        return DatabaseCompressionTypeManager.getTypes().stream()
                .filter(type -> type.getName().equalsIgnoreCase(expected)
                        || type.getClass().getSimpleName().equalsIgnoreCase("DatabaseCompressionType" + expected))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        "Unknown compression '" + expected + "'"));
    }

    private static TreeSet<String> packageNames(Trinity project) {
        TreeSet<String> packages = new TreeSet<>();
        project.getExecution().getClassList().forEach(input -> addPackages(packages, input.getRealName()));
        project.getExecution().getResourceMap().keySet().forEach(path -> addPackages(packages, path));
        return packages;
    }

    private static void addPackages(TreeSet<String> packages, String path) {
        int separator = path.lastIndexOf('/');
        while (separator > 0) {
            packages.add(path.substring(0, separator));
            separator = path.lastIndexOf('/', separator - 1);
        }
    }

    private static void addSearch(List<SearchResult> output, String kind, String identity,
                                  String displayName, String owner, String descriptor, String term) {
        int score = score(identity, displayName, term);
        if (score >= 0) output.add(new SearchResult(kind, identity, displayName, owner, descriptor, score));
    }

    private static int score(String identity, String displayName, String term) {
        if (term.isEmpty()) return 0;
        String id = identity.toLowerCase(Locale.ROOT);
        String display = displayName.toLowerCase(Locale.ROOT);
        if (id.equals(term) || display.equals(term)) return 1000;
        if (id.startsWith(term) || display.startsWith(term)) return 750;
        if (id.contains(term) || display.contains(term)) return 500 - Math.min(400, id.length() - term.length());
        return -1;
    }

    private static String identity(MethodInput input) {
        return input.getOwningClass().getRealName() + "." + input.getName() + input.getDescriptor();
    }

    private static String identity(FieldInput input) {
        return input.getOwningClass().getRealName() + "." + input.getNode().name + ":" + input.getDescriptor();
    }

    private static int pageLimit(int value) {
        return value <= 0 ? 100 : Math.min(MAX_PAGE_SIZE, value);
    }

    private static int pageOffset(int value) {
        if (value < 0) throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                "offset must not be negative");
        return value;
    }

    private static String normalizeKind(String value) {
        if (value == null || value.isBlank()) return "all";
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "packages" -> "package";
            case "classes" -> "class";
            case "methods" -> "method";
            case "fields" -> "field";
            case "resources" -> "resource";
            case "dex_classes" -> "dex_class";
            case "dex_methods" -> "dex_method";
            case "dex_fields" -> "dex_field";
            default -> value.trim().toLowerCase(Locale.ROOT);
        };
    }

    private static boolean includes(String requested, String candidate) {
        return requested.equals("all") || requested.equals(candidate);
    }

    private static boolean matchesPrefix(String path, String prefix) {
        return prefix.isEmpty() || path.startsWith(prefix);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Path absolutePath(String value, String field) {
        requireText(value, field);
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                    field + " must not be blank");
        }
    }
}
