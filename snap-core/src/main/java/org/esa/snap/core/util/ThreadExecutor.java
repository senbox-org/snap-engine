package org.esa.snap.core.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadExecutor {

    private final ThreadPoolExecutor executor;
    private boolean debugSingleThreaded = false;
    private Runnable runnable;
    private final int numConsecutiveThreads;

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

        this.runnable = runnable;
        if(debugSingleThreaded) {
            runnable.process();
        } else {
            executor.execute(runnable);
        }
    }

    public void complete() throws Exception {
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(50);
        }

        if (runnable instanceof ThreadRunnable) {
            ThreadRunnable threadRunnable = (ThreadRunnable) runnable;
            if (threadRunnable.hasError()) {
                if(threadRunnable.description != null) {
                    System.out.println("Error in " + threadRunnable.description +": "+ threadRunnable.getException().getMessage());
                    threadRunnable.getException().printStackTrace();
                }
                throw threadRunnable.getException();
            }
        }
    }
}
