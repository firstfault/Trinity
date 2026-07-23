package me.f1nal.trinity.execution.compile;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
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
import java.nio.file.attribute.FileTime;
import java.util.zip.CRC32;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public ClassWriterTask(ProjectContainer container, Trinity trinity, Console console,
                           File outputFile, boolean removeSignatures) {
        if (!container.isJar()) throw new IllegalArgumentException("Only JAR containers can be exported as JARs");
        this.container = container;
        this.trinity = trinity;
        this.console = console;
        this.outputFile = outputFile;
        this.removeSignatures = removeSignatures;
    }

    private ClassNode getType(String typeName) {
        ClassInput classInput = trinity.getExecution().getClassInput(typeName);
        return classInput != null ? classInput.getNode() : trinity.getJrtInput().getClass(typeName);
    }

    public void build(Consumer<Float> progressConsumer, Runnable finish) {
        executor.submit(() -> {
            try {
                buildJar(progressConsumer);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                console.error("Export failed: {}", String.valueOf(throwable.getMessage()));
            } finally {
                executor.shutdown();
                Main.runLater(finish);
            }
        });
    }

    void buildJar(Consumer<Float> progressConsumer) throws Exception {
        List<OutputEntry> entries = new ArrayList<>();
        Set<String> entryNames = new HashSet<>();
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
                        ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, this::getType, console);
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
            progressConsumer.accept(classCount == 0 ? 1.F : (float) written / classCount);
        }

        File absoluteOutput = outputFile.getAbsoluteFile();
        File parent = absoluteOutput.getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        File temporary = Files.createTempFile(parent.toPath(), absoluteOutput.getName() + ".", ".trinity.tmp").toFile();
        try {
            writeJar(temporary, entries, container.getArchiveComment());
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
        if (removedSignatures != 0) {
            console.info("Removed {} invalid signature file(s).", String.valueOf(removedSignatures));
        }
    }

    private static void addEntry(List<OutputEntry> entries, Set<String> names, OutputEntry entry) {
        if (!names.add(entry.name())) throw new IllegalStateException("Duplicate JAR entry " + entry.name());
        entries.add(entry);
    }

    private static void writeJar(File file, List<OutputEntry> entries, String archiveComment) throws Exception {
        try (JarOutputStream output = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            if (archiveComment != null) output.setComment(archiveComment);
            entries.sort(Comparator.comparingInt(entry -> entry.metadata().getOrder()));
            for (OutputEntry entry : entries) {
                ZipEntry zipEntry = createZipEntry(entry);
                output.putNextEntry(zipEntry);
                output.write(entry.bytes());
                output.closeEntry();
            }
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

    static boolean isSignatureEntry(String name) {
        String upper = name.replace('\\', '/').toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;
        return upper.endsWith(".SF") || upper.endsWith(".RSA")
                || upper.endsWith(".DSA") || upper.endsWith(".EC");
    }
}
