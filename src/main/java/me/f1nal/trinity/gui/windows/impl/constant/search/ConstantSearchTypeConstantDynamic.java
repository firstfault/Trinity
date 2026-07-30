package me.f1nal.trinity.gui.windows.impl.constant.search;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import org.objectweb.asm.ConstantDynamic;

import java.util.List;
import java.util.Locale;

/** Searches ConstantDynamic entries, including entries nested in bootstrap arguments. */
public final class ConstantSearchTypeConstantDynamic extends ConstantSearchType {
    private final ImString query = new ImString(512);

    public ConstantSearchTypeConstantDynamic(Trinity trinity) {
        super("Constant Dynamic", trinity);
    }

    ConstantSearchTypeConstantDynamic(Trinity trinity, String query) {
        this(trinity);
        this.query.set(query);
    }

    @Override
    public boolean draw() {
        ImGui.inputTextWithHint("Constant Dynamic",
                "constant name, descriptor, or bootstrap method", query);
        return true;
    }

    @Override
    public String getSearchDescription() {
        return query.get().isBlank() ? "All ConstantDynamic Values"
                : "ConstantDynamic matching \"" + query.get().trim() + "\"";
    }

    @Override
    public void populate(List<ConstantViewCache> list) {
        new LdcConstantSearcher<ConstantDynamic>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof ConstantDynamic dynamic && matches(dynamic);
            }

            @Override
            protected String convertConstantToText(ConstantDynamic value) {
                return format(value);
            }
        }.populate(list, getTrinity().getExecution());
    }

    boolean matches(ConstantDynamic dynamic) {
        String needle = query.get().trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return true;
        String formatted = format(dynamic).toLowerCase(Locale.ROOT);
        return formatted.contains(needle)
                || formatted.replace('/', '.').contains(needle);
    }

    static String format(ConstantDynamic dynamic) {
        return dynamic.getName() + " : " + dynamic.getDescriptor()
                + " (bootstrap "
                + ConstantSearchTypeHandle.format(dynamic.getBootstrapMethod()) + ")";
    }
}
