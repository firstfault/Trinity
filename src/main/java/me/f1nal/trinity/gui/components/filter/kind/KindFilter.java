package me.f1nal.trinity.gui.components.filter.kind;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.gui.components.filter.Filter;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class KindFilter<T extends IKind> extends Filter<T> {
    private final Map<IKindType, KindState<T>> kindMap = new LinkedHashMap<>();
    private final IKindType[] order;
    private IKindType[] exclude;

    public KindFilter(IKindType[] order) {
        this.order = order;
    }

    public KindFilter() {
        this(new IKindType[0]);
    }

    public void setExclude(IKindType[] exclude) {
        this.exclude = exclude;
    }

    @Override
    public void initialize(Collection<T> collection) {
        kindMap.clear();

        for (IKindType kind : order) {
            kindMap.put(kind, new KindState<>());
        }

        for (T instance : collection) {
            if (instance.getKind() == null) continue;
            kindMap.computeIfAbsent(instance.getKind(), k -> new KindState<>()).count++;
        }

        if (exclude != null) {
            Arrays.stream(exclude).forEach(kindMap::remove);
        }
    }

    @Override
    public Predicate<T> filter() {
        return kind -> {
            IKindType kindType = kind.getKind();
            if (kindType == null) {
                return true;
            }
            KindState<T> state = kindMap.get(kindType);
            return state == null || state.enabled;
        };
    }

    @Override
    public boolean draw() {
        if (kindMap.isEmpty()) {
            return false;
        }

        boolean refresh = false;
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 1.F);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, CodeColorScheme.HIGHLIGHT_BACKGROUND);
        for (Map.Entry<IKindType, KindState<T>> entry : kindMap.entrySet()) {
            IKindType kind = entry.getKey();
            KindState<T> state = entry.getValue();

            if (state.count == 0) continue;

            ImGui.pushStyleColor(ImGuiCol.CheckMark, kind.getColor());
            if (GuiUtil.smallCheckbox(kind.getName(), state.enabled)) {
                state.enabled = !state.enabled;
                refresh = true;
            }
            ImGui.popStyleColor();
            ImGui.sameLine(0.F, 4.F);
            ImGui.textDisabled("(" + state.count + ")");
            ImGui.sameLine();
        }

        ImGui.popStyleVar();
        ImGui.popStyleColor();
        ImGui.newLine();
        return refresh;
    }

    private static class KindState<T> {
        public boolean enabled = true;
        public int count;
    }
}
