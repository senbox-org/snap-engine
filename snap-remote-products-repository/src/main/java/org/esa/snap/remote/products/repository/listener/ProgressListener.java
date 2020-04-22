package org.esa.snap.remote.products.repository.listener;

/**
 * Created by jcoravu on 19/8/2019.
 */
public interface ProgressListener {

    public void notifyProgress(short progressPercent);
}
