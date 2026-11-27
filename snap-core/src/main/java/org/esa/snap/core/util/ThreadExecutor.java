package org.esa.snap.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadExecutor {

    private final ThreadPoolExecutor executor;
    private boolean debugSingleThreaded = false;
    private final int numConsecutiveThreads;
    private final List<Failure> failures = Collections.synchronizedList(new ArrayList<>());

    public ThreadExecutor() {
        this.numConsecutiveThreads = Runtime.getRuntime().availableProcessors() * 2;
        this.executor = createThreadPool(numConsecutiveThreads);
    }

    public ThreadExecutor(final int maxThreads) {
        this.numConsecutiveThreads = maxThreads;
        this.executor = createThreadPool(numConsecutiveThreads);
    }

    private ThreadPoolExecutor createThreadPool(final int maxThreads) {
        return new ThreadPoolExecutor(maxThreads, maxThreads, 300, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public int getNumConsecutiveThreads() {
        return numConsecutiveThreads;
    }

    public void setDebugSingleThreaded(final boolean debugSingleThreaded) {
        this.debugSingleThreaded = debugSingleThreaded;
    }

    public void execute(final ThreadRunnable runnable) throws Exception {
        if(executor.isShutdown()) {
            throw new Exception("Executor already shutdown!!");
        }

        if(debugSingleThreaded) {
            runnable.process();
        } else {
            // the wrapper records the failure only, not the runnable, so finished tasks do not stay reachable
            executor.execute(() -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    failures.add(new Failure(runnable.description, t));   // a run() override that threw
                    return;
                }
                if (runnable.hasError()) {
                    failures.add(new Failure(runnable.description, runnable.getError()));
                }
            });
        }
    }

    /**
     * Waits for all submitted tasks to finish. If any task failed, rethrows the failure of the first task that
     * failed (an Exception as is, an Error such as OutOfMemoryError as is), with the failures of the other tasks
     * added to it as suppressed exceptions.
     */
    public void complete() throws Exception {
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(50);
        }

        final List<Failure> failed;
        synchronized (failures) {
            failed = new ArrayList<>(failures);
        }
        if (failed.isEmpty()) {
            return;
        }
        final Failure first = failed.get(0);
        if (first.description != null) {
            System.out.println("Error in " + first.description + ": " + first.throwable.getMessage());
            first.throwable.printStackTrace();
        }
        final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(first.throwable);
        for (Failure other : failed.subList(1, failed.size())) {
            if (seen.add(other.throwable)) {
                first.throwable.addSuppressed(other.throwable);
            }
        }
        if (first.throwable instanceof Exception) {
            throw (Exception) first.throwable;
        }
        if (first.throwable instanceof Error) {
            throw (Error) first.throwable;
        }
        throw new Exception(first.throwable);
    }

    private static final class Failure {
        final String description;
        final Throwable throwable;

        Failure(String description, Throwable throwable) {
            this.description = description;
            this.throwable = throwable;
        }
    }
}
