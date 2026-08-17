package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.type.ImString;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class EditField<T> {
    private final Supplier<T> getterSupplier;
    private final Consumer<T> setterConsumer;
    private Runnable updateEvent;
    private final ImString inlineText = new ImString(8192);

    EditField(Supplier<T> getterSupplier, Consumer<T> setter) {
        this.getterSupplier = getterSupplier;
        this.setterConsumer = setter;
    }

    public void setUpdateEvent(Runnable updateEvent) {
        this.updateEvent = updateEvent;
    }

    public abstract void draw();
    public abstract void updateField();
    
    /**
     * @return If the input in this field is valid and the instruction may be edited with this data.
     */
    public abstract boolean isValidInput();

    /** Short field name used by the assembler's compact inline editor. */
    public String getInlineLabel() {
        return "Argument";
    }

    /** Text representation used while this field is edited inline. */
    protected String formatInlineValue() {
        T value = this.get();
        return value == null ? "" : String.valueOf(value);
    }

    /** Applies text entered by the inline editor and updates field validity. */
    public boolean applyInlineValue(String value) {
        return false;
    }

    /** Candidate values which may be ranked and displayed below the inline input. */
    public List<String> getInlineSuggestions() {
        return List.of();
    }

    /** Optional parse error displayed as a tooltip by the inline editor. */
    public String getInlineError() {
        return null;
    }

    public final void prepareInlineValue() {
        this.inlineText.set(this.formatInlineValue());
    }

    public final ImString getInlineText() {
        return inlineText;
    }

    protected final T get() {
        return getterSupplier.get();
    }

    /**
     * To be called by overriding classes when this value is changed.
     * @param value New value.
     */
    protected final void set(T value) {
        setterConsumer.accept(value);
        this.update();
    }

    protected final void update() {
        if (updateEvent != null) updateEvent.run();
    }
}
