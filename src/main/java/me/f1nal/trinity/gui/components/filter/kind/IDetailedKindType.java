package me.f1nal.trinity.gui.components.filter.kind;

import me.f1nal.trinity.util.IDescribable;

import java.util.List;

/** A filter kind with enough information to explain its contents in the UI. */
public interface IDetailedKindType extends IKindType, IDescribable {
    List<String> getTypeNames();

    default boolean matchesTypeName(String listedType, String resultType) {
        if (listedType == null || resultType == null) return false;
        if (listedType.equalsIgnoreCase(resultType)) return true;
        if (resultType.length() <= listedType.length()) return false;
        int start = resultType.length() - listedType.length();
        return resultType.charAt(start - 1) == ' '
                && resultType.regionMatches(true, start, listedType, 0, listedType.length());
    }
}
