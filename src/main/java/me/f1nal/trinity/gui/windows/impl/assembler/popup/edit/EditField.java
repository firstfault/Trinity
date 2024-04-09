package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class EditField<T> {
    private final Supplier<T> getterSupplier;
    private final Consumer<T> setterConsumer;
    private Runnable updateEvent;

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
