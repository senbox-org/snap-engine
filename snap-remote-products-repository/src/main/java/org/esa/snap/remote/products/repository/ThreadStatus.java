package org.esa.snap.remote.products.repository;

/**
 * Created by jcoravu on 13/8/2019.
 */
public interface ThreadStatus {

    public boolean isFinished();

    public static void checkCancelled(ThreadStatus thread) throws java.lang.InterruptedException {
        if (thread != null && thread.isFinished()) {
            throw new InterruptedException(); // stop running
        }
    }
}
