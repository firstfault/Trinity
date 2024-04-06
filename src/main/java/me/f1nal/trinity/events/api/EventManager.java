package me.f1nal.trinity.events.api;

import com.google.common.eventbus.EventBus;
import me.f1nal.trinity.Main;

import java.util.ArrayList;
import java.util.List;

public class EventManager {
    /**
     * If this Trinity instance is registered for events.
     */
    private boolean registered;
    private final List<IEventListener> eventListeners = new ArrayList<>();

    public <T extends IEventListener> T registerListener(T listener) {
        if (this.isRegistered()) {
            Main.getEventBus().register(listener);
        }
        this.eventListeners.add(listener);
        return listener;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;

        EventBus eventBus = Main.getEventBus();
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
            Main.getEventBus().post(event);
        }
    }

}
