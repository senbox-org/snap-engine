package org.esa.snap.remote.products.repository.donwload;

import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 19/8/2019.
 */
class DownloadProductProgressListener implements ro.cs.tao.ProgressListener {

    private static final Logger logger = Logger.getLogger(DownloadProductProgressListener.class.getName());

    private final ProgressListener progressListener;
    private final String dataSourceName;
    private final String missionName;
    private final String productName;

    DownloadProductProgressListener(ProgressListener progressListener, String dataSourceName, String missionName, String productName) {
        this.progressListener = progressListener;
        this.dataSourceName = dataSourceName;
        this.missionName = missionName;
        this.productName = productName;
    }

    @Override
    public void started(String taskName) {
    }

    @Override
    public void subActivityStarted(String subTaskName) {
    }

    @Override
    public void subActivityEnded(String subTaskName) {
    }

    @Override
    public void ended() {
    }

    @Override
    public void notifyProgress(double progressValue) {
        short progressPercent = (short)(progressValue * 100.0d);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Update the downloading progress percent " + progressPercent + "% of the product '" + this.productName+"' from the '"+this.dataSourceName+"' remote repository using the '" + this.missionName + "' mission.");
        }

        this.progressListener.notifyProgress(progressPercent);
    }

    @Override
    public void notifyProgress(String subTaskName, double subTaskProgress) {
    }

    @Override
    public void notifyProgress(String subTaskName, double subTaskProgress, double overallProgress) {
    }
}
