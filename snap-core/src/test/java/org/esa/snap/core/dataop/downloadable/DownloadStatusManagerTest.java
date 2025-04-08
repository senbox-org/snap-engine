package org.esa.snap.core.dataop.downloadable;

import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.junit.Assert.*;

public class DownloadStatusManagerTest {

    private DownloadStatusManager downloadStatusManager;
    private TestPropertyChangeListener listener;

    @Before
    public void setUp() {
        downloadStatusManager = DownloadStatusManager.getInstance();
        listener = new TestPropertyChangeListener();
        downloadStatusManager.addListener(listener);
        downloadStatusManager.setDownloading(false, "");
        downloadStatusManager.setPreviousMessage("");
    }

    @Test
    public void testSingletonInstance() {
        DownloadStatusManager singleton1 = DownloadStatusManager.getInstance();
        DownloadStatusManager singleton2 = DownloadStatusManager.getInstance();
        assertSame(singleton1, singleton2);
    }

    @Test
    public void testDownloadingTrue() {
        String fileName = "test-file.tif";
        downloadStatusManager.setDownloading(true, fileName);

        assertTrue(downloadStatusManager.isDownloading());
        assertTrue(downloadStatusManager.getCurrentDownload().contains(fileName));
        assertEquals(true, listener.getLastNewValue());
    }

    @Test
    public void testSetDownloadingFalse() {
        String initialMessage = "Opening image view...";
        downloadStatusManager.setPreviousMessage(initialMessage);

        downloadStatusManager.setDownloading(false, "");

        assertFalse(downloadStatusManager.isDownloading());
        assertEquals(initialMessage, downloadStatusManager.getCurrentDownload());
        assertEquals(false, listener.getLastNewValue());
    }

    @Test
    public void testSetPreviousMessage() {
        String previousMessage = "Test Previous Message";
        downloadStatusManager.setPreviousMessage(previousMessage);
        downloadStatusManager.setDownloading(false, "");

        assertEquals(previousMessage, downloadStatusManager.getCurrentDownload());
    }

    @Test
    public void testRemoveListener() {
        downloadStatusManager.removeListener(listener);
        downloadStatusManager.setDownloading(true, "file.tif");

        assertNull(listener.getLastNewValue());
    }



    private static class TestPropertyChangeListener implements PropertyChangeListener {
        private Object lastNewValue;

        public Object getLastNewValue() {
            return lastNewValue;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            this.lastNewValue = evt.getNewValue();
        }
    }
}