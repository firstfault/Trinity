package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.theme.CodeColorScheme;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DecompilerComponent {
    private String text;
    private Supplier<String> textFunction;
    private Supplier<Integer> colorFunction = () -> CodeColorScheme.TEXT;
    private List<Consumer<PopupItemBuilder>> popupBuilders = new ArrayList<>();
    private Supplier<List<ColoredString>> tooltip;
    private DecompilerComponentRenameState renameState;
    private RenameHandler renameHandler;
    /**
     * Identifier that tells us if another {@link DecompilerComponent} is linked to this component and should get highlighted when clicked on it.
     */
    private String identifier;
    /**
     * Renderer to run after text drawing finished.
     */
    private Runnable customRenderer;
    // Temporary
    public Input<?> input;
    public String memberKey;

    public DecompilerComponent(String text) {
        this.text = text;
    }

    /**
     * Refresh this decompiler window's text.
     */
    public void refreshWindow() {
        Main.getTrinity().getEventManager().postEvent(new EventRefreshDecompilerText(dc -> dc.containsComponent(this)));
    }

    /**
     * Refreshes this component's text from the {@link DecompilerComponent#textFunction} supplier if it is not {@code null}
     */
    public void refreshText() {
        if (this.textFunction == null) {
            return;
        }

        this.setText(this.textFunction.get());
    }

    public void setIdentifier(OutputMember type, Object identifier) {
        this.identifier = type.getClass().getSimpleName().concat(Objects.toString(identifier));
    }

    public void setCustomRenderer(Runnable customRenderer) {
        this.customRenderer = customRenderer;
    }

    public boolean render() {
        if (this.customRenderer != null) {
            this.customRenderer.run();
            return true;
        }
        return false;
    }

    public DecompilerComponentRenameState getRenameState() {
        return renameState;
    }

    public RenameHandler getRenameHandler() {
        return renameHandler;
    }

    public boolean hasCustomRenderer() {
        return this.customRenderer != null;
    }

    public void setRenameHandler(RenameHandler renameHandler) {
        this.renameHandler = renameHandler;
        this.addPopupBuilder(builder -> {
            builder.menuItem("Rename", this::beginRenaming);
        });
    }

    public void addInputControls(Input<?> input) {
        this.setRenameHandler(input.getRenameHandler());
        this.addPopupBuilder(input::populatePopup);
    }

    public void stopRenaming(@Nullable String newName) {
        if (this.renameState == null) {
            return;
        }
        if (newName != null) {
            this.renameHandler.rename(Main.getTrinity().getRemapper(), newName);
            this.refreshWindow();
        }
        this.renameState = null;
    }

    public void beginRenaming() {
        this.renameState = new DecompilerComponentRenameState(this);
    }

    public void setTooltip(Supplier<List<ColoredString>> tooltip) {
        this.tooltip = tooltip;
    }

    public void addPopupBuilder(Consumer<PopupItemBuilder> builder) {
        this.popupBuilders.add(builder);
    }

    public void setTextFunction(Supplier<String> textFunction) {
        this.textFunction = textFunction;
    }

    public void setColorFunction(Supplier<Integer> colorFunction) {
        this.colorFunction = colorFunction;
    }

    public Supplier<Integer> getColorFunction() {
        return colorFunction;
    }

    public int getColor() {
        return colorFunction.get();
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public PopupItemBuilder createPopup() {
        PopupItemBuilder builder = PopupItemBuilder.create();
        this.popupBuilders.forEach(consumer -> consumer.accept(builder));
        return builder;
    }

    public List<ColoredString> createTooltip() {
        if (this.tooltip == null) {
            return null;
        }
        return this.tooltip.get();
    }

    public boolean isSameKind(DecompilerComponent component) {
        return this.identifier != null && Objects.equals(this.identifier, component.identifier);
    }
}
