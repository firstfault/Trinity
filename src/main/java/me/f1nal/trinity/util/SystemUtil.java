package me.f1nal.trinity.util;

import me.f1nal.trinity.Main;
import org.lwjgl.glfw.GLFW;

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
}
