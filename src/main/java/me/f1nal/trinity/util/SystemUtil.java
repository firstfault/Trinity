package me.f1nal.trinity.util;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.logging.Logging;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.net.URI;

public class SystemUtil {
    public static void copyToClipboard(String text) {
        try {
            GLFW.glfwSetClipboardString(Main.getDisplayManager().getHandle(), text);
        } catch (Throwable ignored) {
        }
    }

    public static String getClipboard() {
        try {
            return GLFW.glfwGetClipboardString(Main.getDisplayManager().getHandle());
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static void browseURL(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Throwable throwable) {
            Logging.warn("Failed to browse URL '{}': {}", url, throwable);
        }
    }

    public static boolean openDirectory(File directory) {
        try {
            File target = directory == null ? null : directory.getAbsoluteFile();
            if (target == null || !target.isDirectory()
                    || !Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                return false;
            }
            Desktop.getDesktop().open(target);
            return true;
        } catch (Throwable throwable) {
            Logging.warn("Failed to open directory '{}': {}", directory, throwable);
            return false;
        }
    }
}
