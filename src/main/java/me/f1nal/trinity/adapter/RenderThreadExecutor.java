package me.f1nal.trinity.adapter;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.application.ApplicationException;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/** The only application-adapter component allowed to schedule work on Trinity's render thread. */
final class RenderThreadExecutor {
    private static final long TIMEOUT_SECONDS = 30L;

    <T> T call(Callable<T> operation) {
        try {
            if (Main.isRenderThread()) {
                return operation.call();
            }
            return Main.callLater(operation).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ApplicationException exception) {
            throw exception;
        } catch (java.util.concurrent.ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ApplicationException applicationException) {
                throw applicationException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ApplicationException(ApplicationException.Code.INTERNAL_ERROR,
                    "Application operation failed", cause);
        } catch (java.util.concurrent.TimeoutException exception) {
            throw new ApplicationException(ApplicationException.Code.TIMEOUT,
                    "Timed out waiting for Trinity's render thread", exception);
        } catch (Exception exception) {
            throw new ApplicationException(ApplicationException.Code.INTERNAL_ERROR,
                    "Application operation failed", exception);
        }
    }

    void run(CheckedRunnable operation) {
        call(() -> {
            operation.run();
            return null;
        });
    }

    @FunctionalInterface
    interface CheckedRunnable {
        void run() throws Exception;
    }
}
