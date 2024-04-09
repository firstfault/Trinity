package me.f1nal.trinity.gui.windows.impl.xref.search;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.gui.components.ClassSelectComponent;
import me.f1nal.trinity.gui.components.MemberSelectComponent;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderMemberRefPattern;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.GuiUtil;

import java.util.regex.Pattern;

public class XrefSearchTypeMember extends XrefSearchType {
    private final MemberSelectComponent memberSelectComponent;
    private MemberDetails details;
    private static final MemorableCheckboxComponent exact = new MemorableCheckboxComponent("xrefSearchMemberCaseInsensitive", true);
    private static final MemorableCheckboxComponent caseInsensitive = new MemorableCheckboxComponent("xrefSearchMemberCaseInsensitive", true);

    protected XrefSearchTypeMember(Trinity trinity, ClassSelectComponent classSelectComponent) {
        super("Method/Field", trinity);
        this.memberSelectComponent = new MemberSelectComponent(classSelectComponent);
    }

    @Override
    public boolean draw() {
        this.details = memberSelectComponent.draw();
        GuiUtil.smallWidget(() -> exact.drawCheckbox("Exact"));
        GuiUtil.tooltip("If not exact, the field only has to contain the provided string.");
        ImGui.sameLine();
        GuiUtil.smallWidget(() -> caseInsensitive.drawCheckbox("Case Insensitive"));
        return true;
    }

    @Override
    public XrefBuilder search() {
        return new XrefBuilderMemberRefPattern(trinity.getExecution().getXrefMap(), this.buildPattern(), this.details.getAll());
    }

    private Pattern buildPattern() {
        StringBuilder pattern = new StringBuilder();
        addPattern(pattern, this.details.getOwner());
        pattern.append("\\.");
        addPattern(pattern, this.details.getName());
        pattern.append("\\.");
        addPattern(pattern, this.details.getDesc());

        Logging.debug("Search pattern is {}", pattern.toString());
        return Pattern.compile(pattern.toString());
    }

    private void addPattern(StringBuilder pattern, String field) {
        if (caseInsensitive.getState()) pattern.append("(?i)");

        if (field.isEmpty()) {
            pattern.append("(?s).*");
        } else {
            final String quote = Pattern.quote(field);
            if (exact.getState()) {
                pattern.append(quote);
            } else {
                // FIXME: This (contains) is broken
                pattern.append("/^.*").append(quote).append(".*$/");
            }
        }
    }
}
