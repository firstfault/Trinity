package me.f1nal.trinity.gui.windows.impl.constant.search;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Locale;

/** Searches method-handle and field-handle constants, including bootstrap handles. */
public final class ConstantSearchTypeHandle extends ConstantSearchType {
    private final ImString query = new ImString(512);

    public ConstantSearchTypeHandle(Trinity trinity) {
        super("Method / Field Handle", trinity);
    }

    ConstantSearchTypeHandle(Trinity trinity, String query) {
        this(trinity);
        this.query.set(query);
    }

    @Override
    public boolean draw() {
        ImGui.inputTextWithHint("Handle", "owner, member name, descriptor, or handle kind", query);
        return true;
    }

    @Override
    public String getSearchDescription() {
        return query.get().isBlank() ? "All Method and Field Handles"
                : "Handles matching \"" + query.get().trim() + "\"";
    }

    @Override
    public void populate(List<ConstantViewCache> list) {
        new LdcConstantSearcher<Handle>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof Handle handle && matches(handle);
            }

            @Override
            protected String convertConstantToText(Handle value) {
                return format(value);
            }
        }.populate(list, getTrinity().getExecution());
    }

    boolean matches(Handle handle) {
        String needle = query.get().trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return true;
        String formatted = format(handle).toLowerCase(Locale.ROOT);
        return formatted.contains(needle)
                || formatted.replace('/', '.').contains(needle);
    }

    static String format(Handle handle) {
        return handle.getOwner() + "." + handle.getName() + handle.getDesc()
                + " (" + tagName(handle.getTag())
                + (handle.isInterface() ? ", interface" : "") + ")";
    }

    private static String tagName(int tag) {
        return switch (tag) {
            case Opcodes.H_GETFIELD -> "getfield";
            case Opcodes.H_GETSTATIC -> "getstatic";
            case Opcodes.H_PUTFIELD -> "putfield";
            case Opcodes.H_PUTSTATIC -> "putstatic";
            case Opcodes.H_INVOKEVIRTUAL -> "invokevirtual";
            case Opcodes.H_INVOKESTATIC -> "invokestatic";
            case Opcodes.H_INVOKESPECIAL -> "invokespecial";
            case Opcodes.H_NEWINVOKESPECIAL -> "newinvokespecial";
            case Opcodes.H_INVOKEINTERFACE -> "invokeinterface";
            default -> "tag " + tag;
        };
    }
}
