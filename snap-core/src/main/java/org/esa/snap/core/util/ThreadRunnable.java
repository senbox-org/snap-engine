package org.esa.snap.core.util;

public abstract class ThreadRunnable implements Runnable {

    protected Exception exception = null;
    protected String description;
    private volatile Throwable error = null;

    public ThreadRunnable() {}

    public ThreadRunnable(final String description) {
        this.description = description;
    }

    public void throwException(final Exception e) {
        this.exception = e;
    }

    public abstract void process() throws Exception;

    @Override
    public void run() {
        try {
            process();
        } catch (Exception e) {
            throwException(e);
        } catch (Throwable t) {
            // Errors (e.g. OutOfMemoryError) must not be lost: record them so that ThreadExecutor.complete() rethrows them
            error = t;
        }
    }

    public boolean hasError() {
        return exception != null || error != null;
    }

    /**
     * @return the Exception thrown by {@link #process()}, or null. An {@link Error} is not an Exception and is
     * returned by {@link #getError()} only.
     */
    public Exception getException() {
        return exception;
    }

    /**
     * @return the failure of this task: the Exception if there is one, otherwise the Error thrown by
     * {@link #process()}, or null if the task did not fail.
     */
    public Throwable getError() {
        return exception != null ? exception : error;
    }
}
