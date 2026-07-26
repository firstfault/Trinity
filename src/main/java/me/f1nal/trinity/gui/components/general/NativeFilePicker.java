package me.f1nal.trinity.gui.components.general;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDFilterItem;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.util.nfd.NativeFileDialog.NFD_CANCEL;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_ERROR;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_FreePath;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_GetError;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Init;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_OKAY;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_OpenDialog;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_OpenDialogMultiple;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_Free;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_FreePath;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_GetCount;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_GetPath;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_PickFolder;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Quit;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_SaveDialog;

/**
 * Native open, save, and folder dialogs backed by LWJGL's NFD binding.
 */
public final class NativeFilePicker {
    private NativeFilePicker() {
    }

    public record Filter(String name, String... extensions) {
        public Filter {
            extensions = extensions.clone();
        }

        String specification() {
            return String.join(",", extensions);
        }
    }

    public static File openFile(String defaultDirectory, Filter filter) {
        return withNfd(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer output = stack.mallocPointer(1);
                int result = NFD_OpenDialog(output, createFilter(stack, filter), defaultDirectory);
                return readSingleResult(result, output);
            }
        });
    }

    public static File[] openFiles(String defaultDirectory, Filter filter) {
        return withNfd(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer output = stack.mallocPointer(1);
                int result = NFD_OpenDialogMultiple(output, createFilter(stack, filter), defaultDirectory);
                if (result == NFD_CANCEL) return new File[0];
                checkResult(result);

                long pathSet = output.get(0);
                try {
                    IntBuffer count = stack.mallocInt(1);
                    checkResult(NFD_PathSet_GetCount(pathSet, count));
                    List<File> files = new ArrayList<>(count.get(0));
                    PointerBuffer path = stack.mallocPointer(1);
                    for (int index = 0; index < count.get(0); index++) {
                        checkResult(NFD_PathSet_GetPath(pathSet, index, path));
                        long address = path.get(0);
                        try {
                            files.add(new File(MemoryUtil.memUTF8(address)));
                        } finally {
                            NFD_PathSet_FreePath(address);
                        }
                    }
                    return files.toArray(File[]::new);
                } finally {
                    NFD_PathSet_Free(pathSet);
                }
            }
        });
    }

    public static File saveFile(String defaultDirectory, String defaultName, Filter filter) {
        return withNfd(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer output = stack.mallocPointer(1);
                int result = NFD_SaveDialog(output, createFilter(stack, filter), defaultDirectory, defaultName);
                return readSingleResult(result, output);
            }
        });
    }

    public static File pickFolder(String defaultDirectory) {
        return withNfd(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer output = stack.mallocPointer(1);
                int result = NFD_PickFolder(output, defaultDirectory);
                return readSingleResult(result, output);
            }
        });
    }

    private static NFDFilterItem.Buffer createFilter(MemoryStack stack, Filter filter) {
        if (filter == null || filter.extensions().length == 0) return null;
        NFDFilterItem.Buffer filters = NFDFilterItem.malloc(1, stack);
        filters.get(0)
                .name(stack.UTF8(filter.name()))
                .spec(stack.UTF8(filter.specification()));
        return filters;
    }

    private static File readSingleResult(int result, PointerBuffer output) {
        if (result == NFD_CANCEL) return null;
        checkResult(result);

        long address = output.get(0);
        try {
            return new File(MemoryUtil.memUTF8(address));
        } finally {
            NFD_FreePath(address);
        }
    }

    private static void checkResult(int result) {
        if (result == NFD_OKAY) return;
        String message = result == NFD_ERROR ? NFD_GetError() : "Unexpected result code " + result;
        throw new IllegalStateException("Native file dialog failed: " + message);
    }

    private static <T> T withNfd(DialogCall<T> call) {
        checkResult(NFD_Init());
        try {
            return call.run();
        } finally {
            NFD_Quit();
        }
    }

    @FunctionalInterface
    private interface DialogCall<T> {
        T run();
    }
}
