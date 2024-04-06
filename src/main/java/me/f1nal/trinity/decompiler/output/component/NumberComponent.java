package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.decompiler.output.component.number.NumberDisplayType;
import me.f1nal.trinity.decompiler.output.component.number.NumberDisplayTypeEnum;
import me.f1nal.trinity.decompiler.output.effect.TooltipEffect;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.gui.frames.impl.constant.ConstantViewCache;
import me.f1nal.trinity.gui.frames.impl.constant.ConstantViewFrame;
import me.f1nal.trinity.gui.frames.impl.constant.search.ConstantSearchType;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.StringUtil;
import me.f1nal.trinity.util.SystemUtil;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class NumberComponent extends AbstractPopupTextComponent {
    private final Number number;
    private final NumberDisplayType[] displayTypes;
    private NumberDisplayType displayType;

    private static final NumberDisplayType[] integerDisplayTypes;

    private static NumberDisplayType[] getDisplayTypes(NumberDisplayTypeEnum... type) {
        ArrayList<NumberDisplayType> types = new ArrayList<>();
        for (NumberDisplayTypeEnum typeEnum : type) {
            types.add(typeEnum.getInstance());
        }
        return types.toArray(new NumberDisplayType[0]);
    }

    static {
        integerDisplayTypes = getDisplayTypes(NumberDisplayTypeEnum.DECIMAL, NumberDisplayTypeEnum.HEX, NumberDisplayTypeEnum.OCTAL, NumberDisplayTypeEnum.BINARY, NumberDisplayTypeEnum.ASCII);
    }

    public NumberComponent(String text, Number number) {
        super(text);
        this.number = number;
        this.displayTypes = integerDisplayTypes;
        this.addEffect(new TooltipEffect(() -> ColoredStringBuilder.create().text(getTextColor(), StringUtil.capitalizeFirstLetter(this.number.getClass().getSimpleName())).get()));
    }

    @Override
    protected void drawPopup() {
        for (NumberDisplayType type : displayTypes) {
            String label = String.format("%s (%s)", type.getLabel(), type.getText(number));
            if (ImGui.menuItem(label)) {
                this.displayType = type;
                Main.getTrinity().getEventManager().postEvent(new EventRefreshDecompilerText(dc -> dc.containsComponent(this)));
            }
        }
        ImGui.separator();
        if (ImGui.menuItem("Copy")) {
            SystemUtil.copyToClipboard(this.getText());
        }
        if (ImGui.menuItem("Search All Occurrences...")) {
            Trinity trinity = Main.getTrinity();
            ConstantSearchType constantSearchType = this.getDisplayType().getConstantSearchType(trinity, number);
            List<ConstantViewCache> constantViewList = new ArrayList<>();
            constantSearchType.populate(constantViewList);
            Main.getDisplayManager().addClosableWindow(new ConstantViewFrame(trinity, constantViewList));
        }
        ImGui.endPopup();
    }

    @Override
    public String getText() {
        return getDisplayType().getText(this.number);
    }

    private NumberDisplayType getDisplayType() {
        return this.displayType == null ? Main.getPreferences().getDefaultNumberDisplayType().getInstance() : this.displayType;
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.NUMBER;
    }
}
