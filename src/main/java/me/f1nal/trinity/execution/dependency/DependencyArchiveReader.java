package me.f1nal.trinity.execution.dependency;

import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Reads persisted dependency classpaths from archives or the running JDK. */
public final class DependencyArchiveReader {
    private static final int MAX_CLASS_COUNT = 100_000;
    private static final int MAX_CLASS_SIZE = 16 * 1024 * 1024;
    private static final long MAX_ARCHIVE_CLASS_BYTES = 512L * 1024L * 1024L;

    private DependencyArchiveReader() {
    }

    public static DependencyArchive read(File file) throws IOException {
        return read(file, null);
    }

    public static DependencyArchive read(File file, File databaseFile) throws IOException {
        return read(file, databaseFile, UUID.randomUUID());
    }

    public static DependencyArchive read(File file, File databaseFile, UUID id) throws IOException {
        if (file == null || !file.isFile()) throw new IOException("Dependency archive does not exist");
        Path absolute = file.toPath().toAbsolutePath().normalize();
        String relative = createRelativePath(absolute, databaseFile);
        DependencyArchive archive = new DependencyArchive(id, file.getName(), DependencyKind.ARCHIVE,
                relative, absolute.toString(), null);
        resolveOrThrow(archive, databaseFile);
        return archive;
    }

    public static void resolve(DependencyArchive archive, File databaseFile) {
        try {
            resolveOrThrow(archive, databaseFile);
        } catch (IOException | RuntimeException exception) {
            archive.setResolutionError(exception.getMessage());
        }
    }

    private static void resolveOrThrow(DependencyArchive archive, File databaseFile) throws IOException {
        if (archive.getKind() == DependencyKind.RUNTIME_MODULE) {
            readRuntimeModule(archive);
            return;
        }
        File file = resolveArchivePath(archive, databaseFile);
        if (file == null) {
            throw new IOException("Archive not found: " + archive.getStoredReference());
        }
        LinkedHashMap<String, byte[]> classes = new LinkedHashMap<>();
        long totalSize = 0L;
        try (ZipFile zipFile = new ZipFile(file)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")
                        || entry.getName().startsWith("META-INF/versions/")) {
                    continue;
                }
                if (classes.size() >= MAX_CLASS_COUNT) {
                    throw new IOException("Dependency archive contains too many classes");
                }
                byte[] bytes;
                try (InputStream input = zipFile.getInputStream(entry)) {
                    bytes = readClassBytes(input);
                }
                totalSize += bytes.length;
                if (totalSize > MAX_ARCHIVE_CLASS_BYTES) {
                    throw new IOException("Dependency archive expands beyond the 512 MiB classpath limit");
                }
                String className;
                try {
                    className = new ClassReader(bytes).getClassName();
                } catch (RuntimeException exception) {
                    throw new IOException("Invalid dependency class " + entry.getName(), exception);
                }
                if (classes.putIfAbsent(className, bytes) != null) {
                    throw new IOException("Duplicate dependency class " + className);
                }
            }
        }
        if (classes.isEmpty()) throw new IOException("Dependency archive contains no class files");
        archive.setResolved(classes, file.getAbsolutePath());
    }

    public static DependencyArchive readRuntimeJavaBase() throws IOException {
        DependencyArchive archive = new DependencyArchive(UUID.randomUUID(), "java.base",
                DependencyKind.RUNTIME_MODULE, null, null, "java.base");
        readRuntimeModule(archive);
        return archive;
    }

    private static void readRuntimeModule(DependencyArchive archive) throws IOException {
        URI uri = URI.create("jrt:/");
        FileSystem fileSystem;
        try {
            fileSystem = FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException exception) {
            fileSystem = FileSystems.newFileSystem(uri, Map.of());
        }

        Path module = fileSystem.getPath("/modules/" + archive.getRuntimeModule());
        if (!Files.isDirectory(module)) {
            throw new IOException("The running JDK does not expose module " + archive.getRuntimeModule());
        }

        LinkedHashMap<String, byte[]> classes = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(module)) {
            var iterator = paths.filter(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().endsWith(".class")).iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                byte[] bytes = Files.readAllBytes(path);
                if (bytes.length > MAX_CLASS_SIZE) {
                    throw new IOException("Runtime class exceeds the class-size limit: " + path);
                }
                String className = new ClassReader(bytes).getClassName();
                if (classes.putIfAbsent(className, bytes) != null) {
                    throw new IOException("Duplicate runtime class " + className);
                }
            }
        }
        if (classes.isEmpty()) {
            throw new IOException("The running JDK module " + archive.getRuntimeModule() + " contains no classes");
        }
        archive.setResolved(classes, "runtime:" + archive.getRuntimeModule());
    }

    private static File resolveArchivePath(DependencyArchive archive, File databaseFile) {
        if (archive.getRelativePath() != null && databaseFile != null) {
            File parent = databaseFile.getAbsoluteFile().getParentFile();
            if (parent != null) {
                File relative = new File(parent,
                        archive.getRelativePath().replace('/', File.separatorChar)).toPath().normalize().toFile();
                if (relative.isFile()) return relative;
            }
        }
        if (archive.getAbsolutePath() != null) {
            File absolute = new File(archive.getAbsolutePath());
            if (absolute.isFile()) return absolute;
        }
        return null;
    }

    private static String createRelativePath(Path dependency, File databaseFile) {
        if (databaseFile == null) return null;
        File parentFile = databaseFile.getAbsoluteFile().getParentFile();
        if (parentFile == null) return null;
        try {
            Path parent = parentFile.toPath().toAbsolutePath().normalize();
            return parent.relativize(dependency).toString().replace(File.separatorChar, '/');
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static byte[] readClassBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > MAX_CLASS_SIZE) {
                throw new IOException("Dependency class exceeds the 16 MiB class-size limit");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
