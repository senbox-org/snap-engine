package eu.esa.snap.core.util;

import com.bc.ceres.core.ProgressMonitor;

import java.util.concurrent.CancellationException;

/**
 * Binds the current operation's ProgressMonitor to the executing thread.
 * This is used around JAI/image tile computation entry points where SNAP has
 * a real monitor at the outer operation boundary, but downstream callbacks
 * cannot receive it as an explicit method argument. Code reached from those
 * callbacks can use this context to observe cancellation or pass the monitor
 * on to APIs that support it.
 */
public final class ProgressMonitorContext {

    private static final ThreadLocal<ProgressMonitor> CURRENT = new ThreadLocal<>();
    private static final String CANCELED_MESSAGE = "Process terminated by user.";

    private ProgressMonitorContext() {
    }

    public static Scope use(ProgressMonitor progressMonitor) {
        final ProgressMonitor previous = CURRENT.get();
        if (isRealProgressMonitor(progressMonitor)) {
            CURRENT.set(progressMonitor);
            return new Scope(previous, true);
        }
        return new Scope(previous, false);
    }

    public static ProgressMonitor getCurrentProgressMonitor() {
        final ProgressMonitor progressMonitor = CURRENT.get();
        return progressMonitor != null ? progressMonitor : ProgressMonitor.NULL;
    }

    public static ProgressMonitor getCurrentProgressMonitorOrDefault(ProgressMonitor defaultProgressMonitor) {
        final ProgressMonitor progressMonitor = CURRENT.get();
        if (progressMonitor != null) {
            return progressMonitor;
        }
        return defaultProgressMonitor != null ? defaultProgressMonitor : ProgressMonitor.NULL;
    }

    public static boolean isRealProgressMonitor(ProgressMonitor progressMonitor) {
        return progressMonitor != null && progressMonitor != ProgressMonitor.NULL;
    }

    public static boolean isCancellationRequested() {
        return isCancellationRequested(null);
    }

    public static boolean isCancellationRequested(ProgressMonitor progressMonitor) {
        final ProgressMonitor currentProgressMonitor = CURRENT.get();
        return Thread.currentThread().isInterrupted()
                || isCanceled(progressMonitor)
                || (currentProgressMonitor != progressMonitor && isCanceled(currentProgressMonitor));
    }

    public static void checkCanceled() {
        checkCanceled(null);
    }

    public static void checkCanceled(ProgressMonitor progressMonitor) {
        if (isCancellationRequested(progressMonitor)) {
            throw new CancellationException(CANCELED_MESSAGE);
        }
    }

    private static boolean isCanceled(ProgressMonitor progressMonitor) {
        return isRealProgressMonitor(progressMonitor) && progressMonitor.isCanceled();
    }

    public static final class Scope implements AutoCloseable {
        private final ProgressMonitor previous;
        private final boolean changed;
        private boolean closed;

        private Scope(ProgressMonitor previous, boolean changed) {
            this.previous = previous;
            this.changed = changed;
        }

        @Override
        public void close() {
            if (closed || !changed) {
                return;
            }
            if (previous != null) {
                CURRENT.set(previous);
            } else {
                CURRENT.remove();
            }
            closed = true;
        }
    }
}
