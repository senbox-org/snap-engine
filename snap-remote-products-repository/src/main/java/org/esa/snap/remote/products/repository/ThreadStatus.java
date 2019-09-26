package org.esa.snap.remote.products.repository;

/**
 * Created by jcoravu on 13/8/2019.
 */
public interface ThreadStatus {

    public boolean isRunning();

    public static void checkCancelled(ThreadStatus thread) throws java.lang.InterruptedException {
        if (thread != null && !thread.isRunning()) {
            throw new InterruptedException(); // stop running
        }
    }
}
