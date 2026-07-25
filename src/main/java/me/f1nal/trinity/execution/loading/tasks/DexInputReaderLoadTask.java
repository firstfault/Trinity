package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;
import me.f1nal.trinity.execution.dex.DexFileUnit;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Parses DEX payloads natively and installs their classes on the render thread. */
public final class DexInputReaderLoadTask extends ProgressiveLoadTask {
    private final List<UnreadDexBytes> inputs;

    public DexInputReaderLoadTask(List<UnreadDexBytes> inputs) {
        super("Reading DEX Input");
        this.inputs = List.copyOf(inputs);
    }

    @Override
    public void runImpl() {
        startWork(inputs.size());
        List<DexFileUnit> parsed = new ArrayList<>(inputs.size());
        for (UnreadDexBytes input : inputs) {
            try {
                parsed.add(getTrinity().getExecution().getDexIndex()
                        .parse(input.getEntryName(), input.getBytes()));
            } catch (Exception exception) {
                throw new IllegalArgumentException(String.format("Unable to load %s",
                        input.getEntryName()), exception);
            }
            finishedWork();
        }

        try {
            Main.runLater(() ->
                    parsed.forEach(getTrinity().getExecution().getDexIndex()::install)).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while installing DEX input", exception);
        } catch (ExecutionException exception) {
            throw new IllegalArgumentException("Unable to install DEX input", exception.getCause());
        }
    }
}
