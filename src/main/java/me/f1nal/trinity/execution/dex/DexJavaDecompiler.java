package me.f1nal.trinity.execution.dex;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxPreparePass;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.rename.RenameVisitor;
import jadx.plugins.input.dex.DexInputPlugin;
import me.f1nal.trinity.application.BrowseService;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/** Reusable Java-like source projection for native DEX bytes; smali remains authoritative. */
public final class DexJavaDecompiler {
    public static final String FORMAT = "java-jadx-1.5.6";
    private static final int MAX_USAGE_THREADS = 8;

    private DexJavaDecompiler() {
    }

    public static ClassView decompile(byte[] dexBytes, String dexName, String internalName) {
        return decompile(List.of(new Input(dexName, dexBytes)), internalName);
    }

    public static ClassView decompile(List<Input> dexInputs, String internalName) {
        try (Session session = new Session(dexInputs)) {
            return session.decompile(internalName);
        }
    }

    /**
     * Owns at most one JADX model. Repeated requests reuse it until the DEX input snapshot changes.
     */
    public static final class Workspace implements AutoCloseable {
        private SessionRef current;
        private int modelBuildCount;

        public ClassView decompile(List<Input> dexInputs, String internalName) {
            SessionRef ref = acquire(dexInputs);
            boolean failed = false;
            try {
                return ref.session().decompile(internalName);
            } catch (RuntimeException exception) {
                failed = true;
                throw exception;
            } finally {
                release(ref, failed);
            }
        }

        public synchronized void invalidate() {
            retire(current);
            current = null;
        }

        @Override
        public void close() {
            invalidate();
        }

        synchronized int modelBuildCount() {
            return modelBuildCount;
        }

        private synchronized SessionRef acquire(List<Input> dexInputs) {
            if (current == null || !current.hasInputs(dexInputs)) {
                retire(current);
                current = new SessionRef(dexInputs);
                modelBuildCount++;
            }
            current.users++;
            return current;
        }

        private synchronized void release(SessionRef ref, boolean failed) {
            ref.users--;
            if (failed && current == ref) {
                current = null;
                ref.retired = true;
            }
            if (ref.retired && ref.users == 0) {
                ref.close();
            }
        }

        private static void retire(SessionRef ref) {
            if (ref == null) return;
            ref.retired = true;
            if (ref.users == 0) {
                ref.close();
            }
        }
    }

    private static final class SessionRef {
        private final List<Input> inputs;
        private final FutureTask<Session> task;
        private int users;
        private boolean retired;

        private SessionRef(List<Input> inputs) {
            this.inputs = List.copyOf(inputs);
            this.task = new FutureTask<>(() -> new Session(this.inputs));
        }

        private boolean hasInputs(List<Input> candidate) {
            if (inputs.size() != candidate.size()) return false;
            for (int i = 0; i < inputs.size(); i++) {
                Input left = inputs.get(i);
                Input right = candidate.get(i);
                if (!left.name().equals(right.name()) || left.bytes() != right.bytes()) {
                    return false;
                }
            }
            return true;
        }

        private Session session() {
            task.run();
            try {
                return task.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while initializing JADX", exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Unable to initialize JADX", cause);
            }
        }

        private void close() {
            if (!task.isDone() || task.isCancelled()) return;
            try {
                task.get().close();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ignored) {
                // A failed session has no live JADX state to release.
            }
        }
    }

    private static final class Session implements AutoCloseable {
        private final JadxDecompiler jadx;

        private Session(List<Input> dexInputs) {
            if (dexInputs.isEmpty()) {
                throw new IllegalArgumentException("At least one DEX input is required");
            }
            JadxArgs args = new JadxArgs();
            args.setSkipResources(true);
            args.setShowInconsistentCode(true);
            args.setThreadsCount(Math.min(MAX_USAGE_THREADS,
                    Runtime.getRuntime().availableProcessors()));
            jadx = new JadxDecompiler(args);
            try {
                jadx.addCustomPass(new InMemoryRenamePass());
                for (Input input : dexInputs) {
                    jadx.addCustomCodeLoader(new DexInputPlugin().loadDex(
                            input.bytes(), input.name()));
                }
                jadx.load();
            } catch (RuntimeException exception) {
                jadx.close();
                throw exception;
            }
        }

        private synchronized ClassView decompile(String internalName) {
            JavaClass target = jadx.getClassesWithInners().stream()
                    .filter(javaClass -> internalName(javaClass).equals(internalName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "JADX class not found: " + internalName));
            String source = target.getCode();
            Map<String, String> methods = new LinkedHashMap<>();
            for (JavaMethod method : target.getMethods()) {
                String methodSource = method.getCodeStr();
                methods.put(method.getMethodNode().getMethodInfo().getShortId(),
                        methodSource == null ? "" : methodSource);
            }
            return new ClassView(source, methods, jadx.getErrorsCount(), jadx.getWarnsCount());
        }

        @Override
        public void close() {
            jadx.close();
        }
    }

    private static final class InMemoryRenamePass implements JadxPreparePass {
        private static final JadxPassInfo INFO = new SimpleJadxPassInfo(
                "TrinityInMemoryRename", "Repair Java identifiers for in-memory DEX input");
        private static final File LOGICAL_INPUT = new File("trinity-in-memory.dex");

        @Override
        public JadxPassInfo getInfo() {
            return INFO;
        }

        @Override
        public void init(RootNode root) {
            root.getArgs().getInputFiles().add(LOGICAL_INPUT);
            try {
                new RenameVisitor().init(root);
            } finally {
                root.getArgs().getInputFiles().remove(LOGICAL_INPUT);
            }
        }
    }

    public static String methodSource(ClassView view, BrowseService.MemberId method) {
        String source = view.methods().get(method.name() + method.descriptor());
        if (source == null) {
            throw new IllegalArgumentException(String.format(
                    "JADX method not found: %s.%s%s", method.owner(), method.name(),
                    method.descriptor()));
        }
        return source;
    }

    private static String internalName(JavaClass javaClass) {
        return javaClass.getRawName().replace('.', '/');
    }

    public record Input(String name, byte[] bytes) {
        public Input {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(bytes, "bytes");
        }
    }

    public record ClassView(String source, Map<String, String> methods,
                            int errorCount, int warningCount) {
        public ClassView {
            methods = Map.copyOf(methods);
        }
    }
}
