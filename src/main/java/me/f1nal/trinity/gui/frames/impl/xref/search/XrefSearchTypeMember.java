package me.f1nal.trinity.gui.frames.impl.xref.search;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.gui.components.ClassSelectComponent;
import me.f1nal.trinity.gui.components.MemberSelectComponent;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilderMemberRefPattern;

import java.util.regex.Pattern;

public class XrefSearchTypeMember extends XrefSearchType {
    private final MemberSelectComponent memberSelectComponent;
    private MemberDetails details;

    protected XrefSearchTypeMember(Trinity trinity, ClassSelectComponent classSelectComponent) {
        super("Method/Field", trinity);
        this.memberSelectComponent = new MemberSelectComponent(classSelectComponent);
    }

    @Override
    public boolean draw() {
        this.details = memberSelectComponent.draw();
        ImGui.textDisabled("(Leave empty to match anything)");
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
        return Pattern.compile(pattern.toString());
    }

    private void addPattern(StringBuilder pattern, String field) {
        if (field.isEmpty()) {
            pattern.append("(?s).*");
        } else {
            pattern.append(Pattern.quote(field));
        }
    }
}
