package me.f1nal.trinity.events.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventManagerTest {
    @Test
    void unregisterRemovesAnActiveListenerFromTheEventBus() {
        TestEventManager manager = new TestEventManager();
        CountingListener listener = new CountingListener();

        assertSame(listener, manager.registerListener(listener));
        manager.setRegistered(true);
        manager.postEvent(new TestEvent());
        assertEquals(1, listener.events);

        assertTrue(manager.unregisterListener(listener));
        manager.postEvent(new TestEvent());
        assertEquals(1, listener.events);
        assertFalse(manager.unregisterListener(listener));

        // The removed listener must not be unregistered a second time.
        manager.setRegistered(false);
    }

    private static final class TestEventManager extends EventManager {
        private final EventBus eventBus = new EventBus();

        @Override
        protected EventBus getEventBus() {
            return eventBus;
        }
    }

    private static final class CountingListener implements IEventListener {
        private int events;

        @Subscribe
        public void onEvent(TestEvent event) {
            events++;
        }
    }

    private static final class TestEvent {
    }
}
