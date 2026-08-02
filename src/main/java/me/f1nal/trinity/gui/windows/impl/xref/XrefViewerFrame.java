package me.f1nal.trinity.gui.windows.impl.xref;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.XrefViewerSettings;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.general.table.TableColumn;
import me.f1nal.trinity.gui.components.general.table.TableColumnRendererXrefInvocation;
import me.f1nal.trinity.gui.components.general.table.TableColumnRendererXrefWhere;
import me.f1nal.trinity.gui.components.general.table.TableComponent;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderClassRef;

import java.util.Collection;
import java.util.List;

public class XrefViewerFrame extends ClosableWindow {
    private final Collection<AbstractXref> xrefViewList;
    private final XrefBuilder builder;
    private final ListFilterComponent<AbstractXref> listFilterComponent;
    private final SearchBarFilter<AbstractXref> searchFilter;
    private final KindFilter<AbstractXref> kindFilter;
    private final TableColumnRendererXrefInvocation invocationRenderer =
            new TableColumnRendererXrefInvocation();
    private final TableComponent<AbstractXref> xrefTable = new TableComponent<>(null);

    public XrefViewerFrame(XrefBuilder builder, Trinity trinity, boolean autofollowXref) {
        super("", 680, 300, trinity);

        this.xrefViewList = builder.createXrefs();
        this.searchFilter = new SearchBarFilter<>();
        XrefViewerSettings settings = trinity.getXrefViewerSettings();
        this.kindFilter = new KindFilter<>(XrefKind.values(),
                kind -> settings.isKindEnabled((XrefKind) kind));
        this.listFilterComponent = new ListFilterComponent<>(this.xrefViewList,
                this.searchFilter, this.kindFilter);
        this.kindFilter.addStateChangeListener(() -> {
            for (XrefKind kind : XrefKind.values()) {
                settings.setKindEnabled(kind, this.kindFilter.isEnabled(kind));
            }
            trinity.getDatabase().save(settings);
        });
        this.builder = builder;
        this.setDialog(true);

        this.xrefTable.getColumns().add(new TableColumn<AbstractXref>("Invocation", this.invocationRenderer)
                .setSortKey(AbstractXref::getInvocation)
                .setWidthWeight(1.F));
        this.xrefTable.getColumns().add(new TableColumn<AbstractXref>("Where", new TableColumnRendererXrefWhere<>(
                builder instanceof XrefBuilderClassRef,
                xref -> this.invocationRenderer.createContextMenu(xref, true)))
                .setSortKey(xref -> xref.getWhere().getText())
                .setWidthWeight(3.F));

        if (autofollowXref && this.xrefViewList.size() == 1) this.followFirstXref();
    }

    public XrefViewerFrame(XrefBuilder builder, Trinity trinity) {
        this(builder, trinity, true);
    }

    private void followFirstXref() {
        if (Main.getPreferences().isAutoviewXref()) {
            this.close();
            for (AbstractXref xref : this.xrefViewList) {
                Main.runLater(() -> xref.getWhere().followInDecompiler(NavigationAction.FOLLOW_SINGLE_XREF));
                break;
            }
        }
    }

    @Override
    public String getTitle() {
        return "Xref Viewer: " + builder.getTitle();
    }

    @Override
    protected void renderFrame() {
        this.listFilterComponent.draw();
        List<AbstractXref> filteredXrefs = this.listFilterComponent.getFilteredList();
        this.invocationRenderer.beginFrame(filteredXrefs);
        this.xrefTable.setElementList(this.invocationRenderer.filterSelectedType(filteredXrefs));
        this.xrefTable.draw(Math.max(1.F, ImGui.getContentRegionAvailY()));
        this.invocationRenderer.endFrame();
    }

    @Override
    protected void onOpen() {
        this.searchFilter.getSearchBar().requestFocus();
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof XrefViewerFrame && ((XrefViewerFrame) otherWindow).builder.equals(this.builder);
    }
}
