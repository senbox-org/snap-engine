package org.esa.snap.remote.products.repository.listener;

/**
 * Notifies the progress, the size of a downloading product.
 *
 * Created by jcoravu on 19/8/2019.
 */
public interface ProgressListener {

    public void notifyProgress(short progressPercent);

    public void notifyApproximateSize(long approximateSize);
}
