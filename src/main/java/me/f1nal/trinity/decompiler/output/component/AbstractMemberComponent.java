package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.decompiler.output.effect.TooltipEffect;
import me.f1nal.trinity.gui.frames.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.util.ModifyPriority;
import me.f1nal.trinity.util.NameUtil;
import me.f1nal.trinity.util.SystemUtil;
import imgui.ImGui;

public abstract class AbstractMemberComponent<I extends Input> extends AbstractRenameableComponent {
    private final I input;
    private final MemberDetails details;

    protected AbstractMemberComponent(String text, I input, MemberDetails details) {
        super(text);
        this.input = input;
        this.details = details;
        this.getEffectList().add(new TooltipEffect(() -> {/*        ImGui.beginTooltip();
        ImGui.textColored(CodeColorScheme.CLASS_REF, NameUtil.cleanNewlines(details.getOwner()));
        ImGui.sameLine(0, 0);
        ImGui.textColored(CodeColorScheme.DISABLED, ".");
        ImGui.sameLine(0, 0);
        ImGui.textColored(getTextColor(), NameUtil.cleanNewlines(details.getName()));
        ImGui.sameLine(0, 0);
        ImGui.textColored(CodeColorScheme.DISABLED, "#");
        ImGui.sameLine(0, 0);
        ImGui.textColored(getTextColor(), NameUtil.cleanNewlines(details.getDesc()));
        ImGui.endTooltip();
        */
            return ColoredStringBuilder.create().
                    text(CodeColorScheme.CLASS_REF, NameUtil.cleanNewlines(details.getOwner())).
                    text(CodeColorScheme.DISABLED, ".").
                    text(getTextColor(), details.getName()).
                    text(CodeColorScheme.DISABLED, ".").
                    text(getTextColor(), details.getDesc()).get();
        }));
    }

    @Override
    public String getName() {
        return this.details.getName();
    }

    @Override
    protected void notifyRefreshed() {
        ClassInput classInput = Main.getTrinity().getExecution().getClassInput(this.details.getOwner());
        if (classInput == null) {
            throw new RuntimeException("Tried to refresh null class");
        }
        classInput.notifyModified(ModifyPriority.HIGH);
    }

    public MemberDetails getDetails() {
        return details;
    }

    @Override
    public boolean handleItemHover() {
        if (super.handleItemHover()) {
            return true;
        }
        if (this.getInput() != null && !ImGui.getIO().getKeyCtrl() && ImGui.isMouseClicked(0)) {
            Main.getDisplayManager().openDecompilerView(this.getInput());
            return true;
        }
        return false;
    }

    public I getInput() {
        return input;
    }

    protected void showDetails() {
        if (ImGui.beginMenu("Details...")) {
            if (ImGui.menuItem("Owner")) SystemUtil.copyToClipboard(getDetails().getOwner());
            if (ImGui.menuItem("Name")) SystemUtil.copyToClipboard(getDetails().getName());
            if (ImGui.menuItem("Descriptor")) SystemUtil.copyToClipboard(getDetails().getDesc());
            ImGui.endMenu();
        }
    }

    protected void showXref() {
        if (ImGui.menuItem("View xrefs", "X")) {
            this.openXrefs();
        }
    }

    private void openXrefs() {
        Trinity trinity = Main.getTrinity();
        Main.getDisplayManager().addClosableWindow(new XrefViewerFrame(new XrefBuilderMemberRef(trinity.getExecution().getXrefMap(), this.getDetails()), trinity));
    }
}
