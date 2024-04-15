package me.f1nal.trinity.appdata.shutdown;

import me.f1nal.trinity.util.INameable;
import org.jetbrains.annotations.NotNull;

public class ShutdownHook implements Runnable, INameable {
    private final String name;
    private final Runnable runnable;
    private final Object lock = new Object();
    private volatile boolean ran;

    public ShutdownHook(String name, @NotNull Runnable runnable) {
        this.name = name;
        this.runnable = runnable;
    }

    @Override
    public String getName() {
        return name;
    }

    public void run() {
        synchronized (this.lock) {
            if (this.ran) {
                return;
            }
            this.ran = true;
        }

        this.runnable.run();
    }
}
