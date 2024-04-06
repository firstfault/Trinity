package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import java.util.function.Consumer;

public abstract class EditField<T> {
    private final Consumer<T> setterConsumer;
    private Runnable updateEvent;

    EditField(Consumer<T> setter) {
        this.setterConsumer = setter;
    }

    public void setUpdateEvent(Runnable updateEvent) {
        this.updateEvent = updateEvent;
    }

    public abstract void draw();

    /**
     * @return If the input in this field is valid and the instruction may be edited with this data.
     */
    public abstract boolean isValidInput();

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
