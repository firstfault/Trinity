package me.f1nal.trinity.gui.windows.impl.xref;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.general.table.TableColumn;
import me.f1nal.trinity.gui.components.general.table.TableColumnRendererXrefWhere;
import me.f1nal.trinity.gui.components.general.table.TableComponent;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;

import java.util.Collection;

public class XrefViewerFrame extends ClosableWindow {
    private final Collection<AbstractXref> xrefViewList;
    private final XrefBuilder builder;
    private final ListFilterComponent<AbstractXref> listFilterComponent;
    private final TableComponent<AbstractXref> xrefTable = new TableComponent<>(null);

    public XrefViewerFrame(XrefBuilder builder, Trinity trinity, boolean autofollowXref) {
        super("", 680, 300, trinity);

        this.xrefViewList = builder.createXrefs();
        this.listFilterComponent = new ListFilterComponent<>(this.xrefViewList, new SearchBarFilter<>(), new KindFilter<>(XrefKind.values()));
        this.builder = builder;
        this.setCloseableByEscape(true);

        this.xrefTable.getColumns().add(new TableColumn<>("Access", AbstractXref::getAccessText));
        this.xrefTable.getColumns().add(new TableColumn<>("Invocation", AbstractXref::getInvocation));
        this.xrefTable.getColumns().add(new TableColumn<>("Where", new TableColumnRendererXrefWhere<>()));

        if (autofollowXref && this.xrefViewList.size() == 1) this.followFirstXref();
    }

    public XrefViewerFrame(XrefBuilder builder, Trinity trinity) {
        this(builder, trinity, true);
    }

    private void followFirstXref() {
        if (Main.getPreferences().isAutoviewXref()) {
            // FIXME: Broken with wnd mgr
            this.close();
            for (AbstractXref xref : this.xrefViewList) {
                xref.getWhere().followInDecompiler();
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
        this.xrefTable.setElementList(this.listFilterComponent.getFilteredList());
        this.xrefTable.draw();
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof XrefViewerFrame && ((XrefViewerFrame) otherWindow).builder.equals(this.builder);
    }
}
