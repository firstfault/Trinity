package me.f1nal.trinity.execution.compile;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.dependency.ClassDependencyScanner;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;
import me.f1nal.trinity.util.ByteUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.nio.file.attribute.FileTime;
import java.util.zip.CRC32;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/** Builds one project-owned JAR container. */
public class ClassWriterTask {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ProjectContainer container;
    private final Trinity trinity;
    private final Console console;
    private final File outputFile;
    private final boolean removeSignatures;
    private final boolean ignoreUnresolvedDependencies;

    public ClassWriterTask(ProjectContainer container, Trinity trinity, Console console,
                           File outputFile, boolean removeSignatures) {
        this(container, trinity, console, outputFile, removeSignatures, false);
    }

    public ClassWriterTask(ProjectContainer container, Trinity trinity, Console console,
                           File outputFile, boolean removeSignatures, boolean ignoreUnresolvedDependencies) {
        if (!container.isJar()) throw new IllegalArgumentException("Only JAR containers can be exported as JARs");
        this.container = container;
        this.trinity = trinity;
        this.console = console;
        this.outputFile = outputFile;
        this.removeSignatures = removeSignatures;
        this.ignoreUnresolvedDependencies = ignoreUnresolvedDependencies;
    }

    private ClassNode getType(String typeName) {
        if (trinity == null) return null;
        ClassInput classInput = trinity.getExecution().getClassInput(typeName);
        return classInput != null ? classInput.getNode()
                : trinity.getExecution().getDependencies().getClass(typeName);
    }

    public void build(Consumer<Float> progressConsumer, Runnable finish) {
        build(progressConsumer, ignored -> finish.run());
    }

    public void build(Consumer<Float> progressConsumer, Consumer<ExportResult> finish) {
        executor.submit(() -> {
            ExportResult result;
            try {
                result = buildJar(progressConsumer);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                console.error("Export failed: {}", describeFailure(throwable));
                result = ExportResult.failed(outputFile.getAbsoluteFile(), throwable);
            } finally {
                executor.shutdown();
            }
            ExportResult completed = result;
            Main.runLater(() -> finish.accept(completed));
        });
    }

    ExportResult buildJar(Consumer<Float> progressConsumer) throws Exception {
        progressConsumer.accept(0.02F);
        List<OutputEntry> entries = new ArrayList<>();
        Set<String> entryNames = new HashSet<>();
        Set<String> unresolvedDependencies = findUnresolvedDependencies();
        progressConsumer.accept(0.10F);
        AtomicReference<Console.ExpandableLog> unresolvedLog =
                new AtomicReference<>(reportUnresolvedDependencies(unresolvedDependencies));
        int removedSignatures = 0;
        for (ResourceArchiveEntry resource : container.getResources()) {
            if (removeSignatures && isSignatureEntry(resource.getRealName())) {
                removedSignatures++;
                continue;
            }
            addEntry(entries, entryNames, new OutputEntry(resource.getRealName(), resource.getBytes(),
                    resource.getZipMetadata(), null, -1L));
        }
        for (ArchiveDirectoryEntry directory : container.getDirectories()) {
            if (removeSignatures && isSignatureEntry(directory.getName())) continue;
            addEntry(entries, entryNames, new OutputEntry(directory.getName(), new byte[0],
                    directory.getMetadata(), null, -1L));
        }

        int classCount = container.getClasses().size();
        int written = 0;
        for (var target : container.getClasses()) {
            ClassInput classInput = target.getInput();
            if (classInput == null) continue;
            ClassNode classNode = classInput.getNode();
            byte[] bytes;
            ClassInput rebuilt = null;
            long rebuiltRevision = -1L;
            String entryName = classInput.getExportEntryName();
            if (classInput.isRebuildRequired()) {
                rebuiltRevision = classInput.getBytecodeRevision();
                SafeClassWriter classWriter = new SafeClassWriter(
                        ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, this::getType, console,
                        type -> {
                            if (unresolvedDependencies.add(type)) {
                                updateUnresolvedDependenciesLog(unresolvedLog, unresolvedDependencies);
                            }
                        }, !ignoreUnresolvedDependencies);
                classNode.accept(classWriter);
                bytes = classWriter.toByteArray();
                rebuilt = classInput;
            } else {
                bytes = classInput.getExportBytes();
                if (bytes == null) throw new IllegalStateException("Missing original bytes for " + classNode.name);
            }
            addEntry(entries, entryNames, new OutputEntry(entryName, bytes,
                    classInput.getClassTarget().getZipMetadata(), rebuilt, rebuiltRevision));
            written++;
            progressConsumer.accept(classCount == 0 ? 0.75F
                    : 0.10F + 0.65F * written / classCount);
        }
        if (classCount == 0) progressConsumer.accept(0.75F);

        File absoluteOutput = outputFile.getAbsoluteFile();
        File parent = absoluteOutput.getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        File temporary = Files.createTempFile(parent.toPath(), absoluteOutput.getName() + ".", ".trinity.tmp").toFile();
        try {
            writeJar(temporary, entries, container.getArchiveComment(),
                    value -> progressConsumer.accept(0.75F + 0.24F * value));
            try {
                Files.move(temporary.toPath(), absoluteOutput.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), absoluteOutput.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }

        List<OutputEntry> rebuiltEntries = entries.stream().filter(entry -> entry.rebuiltClass() != null).toList();
        if (!rebuiltEntries.isEmpty()) {
            Main.runLater(() -> rebuiltEntries.forEach(entry ->
                    entry.rebuiltClass().markRebuilt(entry.bytes(), entry.name(), entry.rebuiltRevision())));
        }

        progressConsumer.accept(1.F);
        console.info("Exported {} entries ({}) to {}.", String.valueOf(entries.size()),
                ByteUtil.getHumanReadableByteCountSI(absoluteOutput.length()), absoluteOutput.getAbsolutePath());
        if (!unresolvedDependencies.isEmpty() && !ignoreUnresolvedDependencies) {
            console.warn("Export completed with {} unresolved dependencies. The output may contain verification errors.",
                    String.valueOf(unresolvedDependencies.size()));
        }
        if (removedSignatures != 0) {
            console.info("Removed {} invalid signature file(s).", String.valueOf(removedSignatures));
        }
        return ExportResult.success(absoluteOutput, entries.size(),
                unresolvedDependencies.size(), removedSignatures);
    }

    private static String describeFailure(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }

    Set<String> findUnresolvedDependencies() {
        Set<String> unresolved = new LinkedHashSet<>();
        if (trinity == null) return unresolved;
        for (var target : container.getClasses()) {
            ClassInput input = target.getInput();
            if (input == null) continue;
            Set<String> referenced;
            try {
                referenced = ClassDependencyScanner.collect(input.getNode());
            } catch (RuntimeException exception) {
                if (!ignoreUnresolvedDependencies) {
                    console.warn("Unable to inspect dependencies for {}: {}",
                            input.getNode().name, String.valueOf(exception.getMessage()));
                }
                continue;
            }
            for (String type : referenced) {
                if (!isTypeAvailable(type)) unresolved.add(type);
            }
        }
        return unresolved;
    }

    private boolean isTypeAvailable(String type) {
        return trinity.getExecution().getClassInput(type) != null
                || trinity.getExecution().getDependencies().containsClass(type);
    }

    private Console.ExpandableLog reportUnresolvedDependencies(Set<String> unresolved) {
        if (unresolved.isEmpty()) return null;
        if (ignoreUnresolvedDependencies) {
            console.info("Ignoring {} unresolved dependencies as requested.",
                    String.valueOf(unresolved.size()));
            return null;
        }
        List<String> details = unresolved.stream().sorted().toList();
        return console.warnExpandable(
                "Unresolved Dependencies ({}) - exporting with errors",
                details, String.valueOf(unresolved.size()));
    }

    private void updateUnresolvedDependenciesLog(AtomicReference<Console.ExpandableLog> log,
                                                  Set<String> unresolved) {
        if (ignoreUnresolvedDependencies) return;
        Console.ExpandableLog current = log.get();
        if (current == null) {
            log.set(reportUnresolvedDependencies(unresolved));
            return;
        }
        current.update("Unresolved Dependencies ({}) - exporting with errors",
                unresolved.stream().sorted().toList(), String.valueOf(unresolved.size()));
    }

    private static void addEntry(List<OutputEntry> entries, Set<String> names, OutputEntry entry) {
        if (!names.add(entry.name())) throw new IllegalStateException("Duplicate JAR entry " + entry.name());
        entries.add(entry);
    }

    private static void writeJar(File file, List<OutputEntry> entries, String archiveComment,
                                 Consumer<Float> progressConsumer) throws Exception {
        try (JarOutputStream output = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            if (archiveComment != null) output.setComment(archiveComment);
            entries.sort(Comparator.comparingInt(entry -> entry.metadata().getOrder()));
            int written = 0;
            for (OutputEntry entry : entries) {
                ZipEntry zipEntry = createZipEntry(entry);
                output.putNextEntry(zipEntry);
                output.write(entry.bytes());
                output.closeEntry();
                written++;
                progressConsumer.accept(entries.isEmpty() ? 1.F : (float) written / entries.size());
            }
            if (entries.isEmpty()) progressConsumer.accept(1.F);
        }
    }

    private static ZipEntry createZipEntry(OutputEntry outputEntry) {
        ZipEntry entry = new ZipEntry(outputEntry.name());
        ZipEntryMetadata metadata = outputEntry.metadata();
        entry.setMethod(metadata.getMethod());
        if (metadata.getComment() != null) entry.setComment(metadata.getComment());
        byte[] extra = metadata.getExtra();
        if (extra != null) entry.setExtra(extra);
        if (metadata.getModifiedTime() != ZipEntryMetadata.MISSING_TIME) {
            entry.setLastModifiedTime(FileTime.fromMillis(metadata.getModifiedTime()));
        }
        if (metadata.getAccessTime() != ZipEntryMetadata.MISSING_TIME) {
            entry.setLastAccessTime(FileTime.fromMillis(metadata.getAccessTime()));
        }
        if (metadata.getCreationTime() != ZipEntryMetadata.MISSING_TIME) {
            entry.setCreationTime(FileTime.fromMillis(metadata.getCreationTime()));
        }
        if (metadata.getMethod() == ZipEntry.STORED) {
            CRC32 crc = new CRC32();
            crc.update(outputEntry.bytes());
            entry.setSize(outputEntry.bytes().length);
            entry.setCompressedSize(outputEntry.bytes().length);
            entry.setCrc(crc.getValue());
        }
        return entry;
    }

    private record OutputEntry(String name, byte[] bytes, ZipEntryMetadata metadata,
                               ClassInput rebuiltClass, long rebuiltRevision) {
    }

    public record ExportResult(File outputFile, int entryCount, long outputSize,
                               int unresolvedDependencyCount, int removedSignatureCount,
                               Throwable failure) {
        private static ExportResult success(File outputFile, int entryCount,
                                            int unresolvedDependencyCount,
                                            int removedSignatureCount) {
            return new ExportResult(outputFile, entryCount, outputFile.length(),
                    unresolvedDependencyCount, removedSignatureCount, null);
        }

        private static ExportResult failed(File outputFile, Throwable failure) {
            return new ExportResult(outputFile, 0, 0L, 0, 0, failure);
        }

        public boolean isSuccessful() {
            return failure == null;
        }
    }

    static boolean isSignatureEntry(String name) {
        String upper = name.replace('\\', '/').toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;
        return upper.endsWith(".SF") || upper.endsWith(".RSA")
                || upper.endsWith(".DSA") || upper.endsWith(".EC");
    }
}
