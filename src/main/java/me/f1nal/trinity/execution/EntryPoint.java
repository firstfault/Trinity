package me.f1nal.trinity.execution;

import me.f1nal.trinity.execution.exception.MissingEntryPointException;
import me.f1nal.trinity.logging.Logging;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class EntryPoint {
    private final Execution execution;
    private final MethodInput entryMethod;

    public EntryPoint(Execution execution, Map<String, byte[]> resourceList) throws MissingEntryPointException {
        this.execution = execution;
        this.entryMethod = getEntryFromManifest(execution, resourceList);
    }

    public MethodInput getEntryMethod() {
        return entryMethod;
    }

    /**
     * Tries to get the entry point from the {@code MANIFEST.MF} resource file.
     * @param execution Execution instance.
     * @param resourceList JAR resource list.
     * @return An instance of {@link MethodInput} representing the entry method, or {@code null} if a manifest isn't present.
     * @throws MissingEntryPointException If the main class is present, but it doesn't contain a {@code main(String[])} method.
     */
    private static @Nullable MethodInput getEntryFromManifest(final Execution execution, final Map<String, byte[]> resourceList) throws MissingEntryPointException {
        for (Map.Entry<String, byte[]> entry : resourceList.entrySet()) {
            if (entry.getKey().endsWith("MANIFEST.MF")) {
                final String manifestData = new String(entry.getValue());
                final String targetString = "Main-Class: ";
                final int start = manifestData.indexOf(targetString);
                if (start == -1) {
                    continue;
                }
                String mainClass = manifestData.substring(start + targetString.length());
                final int end = Math.max(mainClass.indexOf('\r'), mainClass.indexOf('\n'));
                if (end != -1) {
                    mainClass = mainClass.substring(0, end - 1);
                }
                final ClassInput input = execution.getClassInput(mainClass.replace('.', '/'));
                if (input == null) {
                    return null;
                }
                final MethodInput main = input.createMethod("main", "([Ljava/lang/String;)V");
//                if (main == null) {
//                    throw new MissingEntryPointException(mainClass);
//                }
                Logging.info("Found main class {}.{}. Project java version {} (V21)", mainClass, main.getName(), input.getClassNode().version);
                return main;
            }
        }
        return null;
    }
}
