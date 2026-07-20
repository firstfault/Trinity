package me.f1nal.trinity.gui.windows.impl.constant;

import imgui.ImGui;
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
    private static final int MAXIMUM_TITLE_DESCRIPTION_LENGTH = 80;
    private final ListFilterComponent<ConstantViewCache> listFilterComponent;
    private final TableComponent<ConstantViewCache> table = new TableComponent<>();

    public ConstantViewFrame(Trinity trinity, List<ConstantViewCache> constantList) {
        this(trinity, constantList, "All Constants");
    }

    public ConstantViewFrame(Trinity trinity, List<ConstantViewCache> constantList,
                             String searchDescription) {
        super(createTitle(searchDescription), 680, 300, trinity);
        this.listFilterComponent = new ListFilterComponent<>(constantList, new SearchBarFilter<>(), new KindFilter<>(XrefKind.values()));
        this.table.getColumns().add(new TableColumn<>("Constant", ConstantViewCache::getConstant));
        this.table.getColumns().add(new TableColumn<>("Where", new TableColumnRendererXrefWhere<>()));
        this.setCloseableByEscape(true);
        this.setInitialPositionAtMouse();
    }

    private static String createTitle(String searchDescription) {
        String description = searchDescription == null || searchDescription.isBlank()
                ? "All Constants" : searchDescription;
        if (description.length() > MAXIMUM_TITLE_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAXIMUM_TITLE_DESCRIPTION_LENGTH - 3) + "...";
        }
        return "Constant Viewer: " + description;
    }

    @Override
    protected void renderFrame() {
        this.listFilterComponent.draw();
        this.table.setElementList(this.listFilterComponent.getFilteredList());
        this.table.draw(Math.max(1.F, ImGui.getContentRegionAvailY()));
    }
}
