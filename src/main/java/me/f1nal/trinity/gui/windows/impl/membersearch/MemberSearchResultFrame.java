package me.f1nal.trinity.gui.windows.impl.membersearch;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventDependenciesChanged;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.membersearch.MemberSearchQuery;
import me.f1nal.trinity.execution.membersearch.MemberSearchResult;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.general.table.TableColumn;
import me.f1nal.trinity.gui.components.general.table.TableComponent;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerPreviewRenderer;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.Comparator;
import java.util.List;

/** Sortable, post-filterable Member Search results with stable navigation targets. */
public final class MemberSearchResultFrame extends ClosableWindow implements IEventListener {
    private final MemberSearchQuery query;
    private final List<MemberSearchResult> results;
    private final SearchBarFilter<MemberSearchResult> searchFilter = new SearchBarFilter<>(true);
    private final ListFilterComponent<MemberSearchResult> filter;
    private final TableComponent<MemberSearchResult> table = new TableComponent<>();
    private final int unresolvedHierarchyComparisons;
    private boolean stale;

    public MemberSearchResultFrame(Trinity trinity, MemberSearchQuery query,
                                   List<MemberSearchResult> results,
                                   int unresolvedHierarchyComparisons) {
        super(title(query, results.size()), 900.F, 420.F, trinity);
        this.query = query;
        this.results = List.copyOf(results);
        this.unresolvedHierarchyComparisons = unresolvedHierarchyComparisons;
        this.filter = new ListFilterComponent<>(this.results, searchFilter);
        buildColumns(query.target());
        this.setDialog(true);
        trinity.getEventManager().registerListener(this);
    }

    @Override
    protected void renderFrame() {
        drawToolbar();
        filter.draw();

        List<MemberSearchResult> filtered = filter.getFilteredList();
        int displayLimit = Main.getPreferences().getSearchMaxDisplay().getMax();
        String count = filtered.size() == results.size()
                ? results.size() + (results.size() == 1 ? " result" : " results")
                : filtered.size() + " of " + results.size() + " results";
        if (filtered.size() > displayLimit) count += "; showing first " + displayLimit;
        ImGui.textColored(CodeColorScheme.DISABLED, count);
        if (unresolvedHierarchyComparisons > 0) {
            ImGui.sameLine();
            ImGui.textColored(CodeColorScheme.NOTIFY_WARN,
                    "    " + unresolvedHierarchyComparisons + " unresolved hierarchy "
                            + (unresolvedHierarchyComparisons == 1 ? "comparison" : "comparisons"));
        }

        table.setElementList(filtered);
        table.draw(Math.max(1.F, ImGui.getContentRegionAvailY()));
    }

    private void drawToolbar() {
        if (ImGui.button("Refine Search")) {
            MemberSearchFrame frame = Main.getWindowManager().addStaticWindow(MemberSearchFrame.class);
            frame.applyQuery(query);
            Main.getWindowManager().requestFocus(frame);
        }
        ImGui.sameLine();
        if (ImGui.button("Run Again")) {
            MemberSearchFrame frame = Main.getWindowManager().addStaticWindow(MemberSearchFrame.class);
            frame.applyAndRun(query);
            Main.getWindowManager().requestFocus(frame);
        }
        if (stale) {
            ImGui.sameLine();
            ImGui.textColored(CodeColorScheme.NOTIFY_WARN,
                    "Project changed. These results may be out of date.");
        }
    }

    private void buildColumns(MemberSearchQuery.Target target) {
        switch (target) {
            case CLASS -> {
                addInteractiveColumn("Class", MemberSearchResult::name,
                        CodeColorScheme.CLASS_REF, 2.4F);
                addTextColumn("Kind", MemberSearchResult::kind, 0.8F);
                addTextColumn("Extends / Implements", MemberSearchResult::type, 2.1F);
                addTextColumn("Access", MemberSearchResult::access, 1.2F);
                addTextColumn("Package", MemberSearchResult::packageName, 1.4F);
                addTextColumn("Archive", MemberSearchResult::container, 1.2F);
                addNumberColumn("Refs", MemberSearchResult::referenceCount, 0.55F);
            }
            case FIELD -> {
                addInteractiveColumn("Field", MemberSearchResult::name,
                        CodeColorScheme.FIELD_REF, 1.4F);
                addTextColumn("Owner", MemberSearchResult::owner, 1.9F);
                addTextColumn("Type", MemberSearchResult::type, 1.7F);
                addTextColumn("Access", MemberSearchResult::access, 1.2F);
                addTextColumn("Package", MemberSearchResult::packageName, 1.3F);
                addTextColumn("Archive", MemberSearchResult::container, 1.1F);
                addNumberColumn("Refs", MemberSearchResult::referenceCount, 0.55F);
            }
            case METHOD -> {
                addInteractiveColumn("Method", MemberSearchResult::name,
                        CodeColorScheme.METHOD_REF, 1.25F);
                addTextColumn("Owner", MemberSearchResult::owner, 1.65F);
                addTextColumn("Signature", MemberSearchResult::type, 2.65F);
                addTextColumn("Access", MemberSearchResult::access, 1.15F);
                addNumberColumn("Instructions", MemberSearchResult::instructionCount, 0.85F);
                addTextColumn("Package", MemberSearchResult::packageName, 1.2F);
                addTextColumn("Archive", MemberSearchResult::container, 1.F);
                addNumberColumn("Refs", MemberSearchResult::referenceCount, 0.5F);
            }
        }
    }

    private void addInteractiveColumn(String header,
                                      java.util.function.Function<MemberSearchResult, String> value,
                                      int color, float width) {
        table.getColumns().add(new TableColumn<MemberSearchResult>(header, (column, result) -> {
            ImGui.textColored(color, value.apply(result));
            handleResultInteraction(result);
        }).setSortKey(value).setWidthWeight(width));
    }

    private void addTextColumn(String header,
                               java.util.function.Function<MemberSearchResult, String> value,
                               float width) {
        table.getColumns().add(new TableColumn<MemberSearchResult>(header, (column, result) -> {
            ImGui.textUnformatted(value.apply(result));
            handleResultInteraction(result);
        }).setSortKey(value).setWidthWeight(width));
    }

    private void addNumberColumn(String header,
                                 java.util.function.ToIntFunction<MemberSearchResult> value,
                                 float width) {
        table.getColumns().add(new TableColumn<MemberSearchResult>(header, (column, result) -> {
            ImGui.textUnformatted(Integer.toString(value.applyAsInt(result)));
            handleResultInteraction(result);
        }).setComparator(Comparator.comparingInt(value)).setWidthWeight(width));
    }

    private void handleResultInteraction(MemberSearchResult result) {
        if (!ImGui.isItemHovered()) return;
        Input<?> input = result.resolve(trinity);
        if (input != null) {
            ImGui.beginTooltip();
            DecompilerPreviewRenderer preview = new DecompilerPreviewRenderer(trinity);
            preview.drawInputPreview(input);
            preview.finish();
            ImGui.endTooltip();
        }
        if (ImGui.isMouseDoubleClicked(0) && input != null) {
            Main.getDisplayManager().followDecompilerView(input, NavigationAction.FOLLOW_MEMBER);
        }
        if (ImGui.isMouseClicked(1) && input != null) {
            PopupItemBuilder popup = PopupItemBuilder.create();
            input.populatePopup(popup);
            Main.getDisplayManager().getPopupMenu().show(popup);
        }
    }

    @Override
    protected void onOpen() {
        searchFilter.getSearchBar().requestFocus();
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        stale = true;
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        stale = true;
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        stale = true;
    }

    @Subscribe
    public void onPackageStructureChanged(EventPackageStructureReload event) {
        stale = true;
    }

    @Subscribe
    public void onDependenciesChanged(EventDependenciesChanged event) {
        stale = true;
    }

    @Override
    protected void onDispose() {
        trinity.getEventManager().unregisterListener(this);
    }

    private static String title(MemberSearchQuery query, int count) {
        return "Member Search: " + query.target().getName() + " (" + count + ")";
    }
}
