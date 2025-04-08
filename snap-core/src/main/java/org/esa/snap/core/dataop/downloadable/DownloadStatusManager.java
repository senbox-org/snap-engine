package org.esa.snap.core.dataop.downloadable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class DownloadStatusManager {

    private static final DownloadStatusManager INSTANCE = new DownloadStatusManager();
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private boolean downloading = false;
    private String currentDownload = "";
    private String previousMessage = "";

    private DownloadStatusManager() {}

    public static DownloadStatusManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setDownloading(boolean downloading, String fileName) {
        boolean oldValue = this.downloading;
        this.downloading = downloading;
        this.currentDownload = downloading ? "Downloading: " + fileName : this.previousMessage;
        support.firePropertyChange("downloading", oldValue, downloading);
    }

    public boolean isDownloading() {
        return downloading;
    }

    public String getCurrentDownload() {
        return currentDownload;
    }

    public void addListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void setPreviousMessage(String previousMessage) {
        this.previousMessage = previousMessage;
    }
}
