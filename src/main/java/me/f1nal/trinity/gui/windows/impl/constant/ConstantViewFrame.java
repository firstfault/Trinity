package me.f1nal.trinity.gui.windows.impl.constant;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.general.table.TableColumn;
import me.f1nal.trinity.gui.components.general.table.TableColumnRendererXrefWhere;
import me.f1nal.trinity.gui.components.general.table.TableComponent;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;

import java.util.List;

public class ConstantViewFrame extends ClosableWindow {
    private final ListFilterComponent<ConstantViewCache> listFilterComponent;
    private final TableComponent<ConstantViewCache> table = new TableComponent<>();

    public ConstantViewFrame(Trinity trinity, List<ConstantViewCache> constantList) {
        super("Constant Viewer", 680, 300, trinity);
        this.listFilterComponent = new ListFilterComponent<>(constantList, new SearchBarFilter<>(), new KindFilter<>(XrefKind.values()));
        this.table.getColumns().add(new TableColumn<>("Constant", ConstantViewCache::getConstant));
        this.table.getColumns().add(new TableColumn<>("Where", new TableColumnRendererXrefWhere<>()));
        this.setCloseableByEscape(true);
    }

    @Override
    protected void renderFrame() {
        this.listFilterComponent.draw();
        this.table.setElementList(this.listFilterComponent.getFilteredList());
        this.table.draw();
    }
}
