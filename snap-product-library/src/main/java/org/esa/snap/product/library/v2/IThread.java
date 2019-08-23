package org.esa.snap.product.library.v2;

/**
 * Created by jcoravu on 13/8/2019.
 */
public interface IThread {

    public boolean isRunning();

    public static void checkCancelled(IThread thread) throws InterruptedException {
        if (thread != null && !thread.isRunning()) {
            throw new InterruptedException(); // stop running
        }
    }
}
