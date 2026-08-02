package me.f1nal.trinity.gui.components.filter.kind;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.gui.components.filter.Filter;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class KindFilter<T extends IKind> extends Filter<T> {
    private final Map<IKindType, KindState<T>> kindMap = new LinkedHashMap<>();
    private final IKindType[] order;
    private final Predicate<IKindType> initialEnabled;
    private final List<Runnable> stateChangeListeners = new ArrayList<>();
    private IKindType[] exclude;

    public KindFilter(IKindType[] order) {
        this(order, kind -> true);
    }

    public KindFilter(IKindType[] order, Predicate<IKindType> initialEnabled) {
        this.order = order;
        this.initialEnabled = initialEnabled;
    }

    public KindFilter() {
        this(new IKindType[0]);
    }

    public void setExclude(IKindType[] exclude) {
        this.exclude = exclude;
    }

    public void addStateChangeListener(Runnable listener) {
        this.stateChangeListeners.add(listener);
    }

    public boolean isEnabled(IKindType kind) {
        KindState<T> state = kindMap.get(kind);
        return state == null ? initialEnabled.test(kind) : state.enabled;
    }

    Collection<String> getPresentTypeNames(IKindType kind) {
        KindState<T> state = kindMap.get(kind);
        return state == null ? Collections.emptySet() : state.presentTypeNames;
    }

    @Override
    public void initialize(Collection<T> collection) {
        kindMap.clear();

        for (IKindType kind : order) {
            kindMap.put(kind, new KindState<>(initialEnabled.test(kind)));
        }

        for (T instance : collection) {
            if (instance.getKind() == null) continue;
            kindMap.computeIfAbsent(instance.getKind(),
                    kind -> new KindState<>(initialEnabled.test(kind))).count++;
        }

        if (exclude != null) {
            Arrays.stream(exclude).forEach(kindMap::remove);
        }
    }

    @Override
    public void update(Collection<T> collection) {
        kindMap.values().forEach(state -> state.presentTypeNames.clear());
        for (T instance : collection) {
            if (instance.getKind() == null || !(instance instanceof IKindTypeName named)) {
                continue;
            }
            KindState<T> state = kindMap.get(instance.getKind());
            if (state != null) state.presentTypeNames.add(named.getKindTypeName());
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
        boolean first = true;
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 1.F);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, CodeColorScheme.HIGHLIGHT_BACKGROUND);
        for (Map.Entry<IKindType, KindState<T>> entry : kindMap.entrySet()) {
            IKindType kind = entry.getKey();
            KindState<T> state = entry.getValue();

            if (state.count == 0) continue;

            String countText = "(" + state.count + ")";
            if (!first) {
                float spacing = ImGui.getStyle().getItemSpacingX();
                float groupWidth = ImGui.getFrameHeight()
                        + ImGui.getStyle().getItemInnerSpacingX()
                        + ImGui.calcTextSize(kind.getName()).x
                        + 4.F + ImGui.calcTextSize(countText).x;
                float contentRight = ImGui.getWindowPosX() + ImGui.getWindowContentRegionMax().x;
                if (ImGui.getItemRectMaxX() + spacing + groupWidth <= contentRight) {
                    ImGui.sameLine(0.F, spacing);
                }
            }

            ImGui.pushStyleColor(ImGuiCol.CheckMark, kind.getColor());
            if (GuiUtil.smallCheckbox(kind.getName(), state.enabled)) {
                state.enabled = !state.enabled;
                refresh = true;
            }
            KindTooltip.draw(kind, state.presentTypeNames);
            ImGui.popStyleColor();
            ImGui.sameLine(0.F, 4.F);
            ImGui.textDisabled(countText);
            first = false;
        }

        ImGui.popStyleVar();
        ImGui.popStyleColor();
        if (refresh) {
            stateChangeListeners.forEach(Runnable::run);
        }
        return refresh;
    }

    private static class KindState<T> {
        public boolean enabled;
        public int count;
        private final Collection<String> presentTypeNames = new LinkedHashSet<>();

        private KindState(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
