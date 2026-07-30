package me.f1nal.trinity.events.api;

import com.google.common.eventbus.EventBus;
import me.f1nal.trinity.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventManager {
    /**
     * If this Trinity instance is registered for events.
     */
    private volatile boolean registered;
    private final List<IEventListener> eventListeners = new ArrayList<>();

    public synchronized <T extends IEventListener> T registerListener(T listener) {
        Objects.requireNonNull(listener, "listener");
        if (this.indexOf(listener) != -1) return listener;
        if (this.isRegistered()) {
            this.getEventBus().register(listener);
        }
        this.eventListeners.add(listener);
        return listener;
    }

    /**
     * Removes a listener from this project and, when active, from the shared event bus.
     *
     * @return {@code true} when the listener was registered with this manager.
     */
    public synchronized boolean unregisterListener(IEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = this.indexOf(listener);
        if (index == -1) return false;
        this.eventListeners.remove(index);
        if (this.isRegistered()) {
            this.getEventBus().unregister(listener);
        }
        return true;
    }

    private int indexOf(IEventListener listener) {
        for (int index = 0; index < this.eventListeners.size(); index++) {
            if (this.eventListeners.get(index) == listener) return index;
        }
        return -1;
    }

    public boolean isRegistered() {
        return registered;
    }

    public synchronized void setRegistered(boolean registered) {
        if (this.registered == registered) return;
        this.registered = registered;

        EventBus eventBus = this.getEventBus();
        for (IEventListener listener : eventListeners) {
            if (this.registered) {
                eventBus.register(listener);
            } else {
                eventBus.unregister(listener);
            }
        }
    }

    public void postEvent(Object event) {
        if (this.isRegistered()) {
            this.getEventBus().post(event);
        }
    }

    protected EventBus getEventBus() {
        return Main.getEventBus();
    }
}
