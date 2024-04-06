package me.f1nal.trinity.util;

public final class DescriptorUtil {

    public static boolean isMatchingParameters(String desc1, String desc2) {
        int indexOf1 = desc1.lastIndexOf(')'), indexOf2 = desc2.lastIndexOf(')');
        if (indexOf1 != indexOf2 || indexOf1 == -1) {
            return false;
        }
        return desc1.substring(indexOf1).equals(desc2.substring(indexOf2));
    }
}
