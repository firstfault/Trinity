package me.f1nal.trinity.database.inputs;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ProjectInputImportQueueTest {
    @Test
    void runsImportsOneAtATimeInSubmissionOrder() throws InterruptedException {
        ProjectInputImportQueue queue = new ProjectInputImportQueue("project-input-import-test");
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(3);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maximumRunning = new AtomicInteger();
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());

        try {
            assertFalse(queue.submit(task(1, firstStarted, releaseFirst, completed,
                    running, maximumRunning, order)));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            assertTrue(queue.submit(task(2, null, null, completed, running, maximumRunning, order)));
            assertTrue(queue.submit(task(3, null, null, completed, running, maximumRunning, order)));
            assertTrue(queue.isBusy());

            releaseFirst.countDown();
            assertTrue(completed.await(2, TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                while (queue.isBusy()) Thread.onSpinWait();
            });

            assertEquals(List.of(1, 2, 3), order);
            assertEquals(1, maximumRunning.get());
            assertFalse(queue.isBusy());
        } finally {
            queue.shutdownNow();
        }
    }

    private static Runnable task(int id, CountDownLatch started, CountDownLatch release,
                                 CountDownLatch completed, AtomicInteger running,
                                 AtomicInteger maximumRunning, List<Integer> order) {
        return () -> {
            int currentRunning = running.incrementAndGet();
            maximumRunning.accumulateAndGet(currentRunning, Math::max);
            try {
                order.add(id);
                if (started != null) started.countDown();
                if (release != null && !await(release)) fail("Interrupted while waiting to release first task");
            } finally {
                running.decrementAndGet();
                completed.countDown();
            }
        };
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
