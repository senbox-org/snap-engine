package org.esa.snap.core.util;

public abstract class ThreadRunnable implements Runnable {

    protected Exception exception = null;
    protected String description;

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
        }
    }

    public boolean hasError() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }
}
