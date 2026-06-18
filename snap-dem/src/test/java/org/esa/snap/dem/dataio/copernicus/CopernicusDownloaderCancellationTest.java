package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class CopernicusDownloaderCancellationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    @STTM("SNAP-4209")
    public void testDownloadTilesStopsWhenProgressMonitorIsCanceled() throws Exception {
        SlowHttpServer server = new SlowHttpServer();
        server.start();
        try {
            CopernicusDownloader downloader = new CopernicusDownloader(temporaryFolder.getRoot(),
                                                                       server.getBaseUrl(),
                                                                       server.getBaseUrl());
            ProgressMonitor progressMonitor = new CancelAfterChecksProgressMonitor(1);

            assertThrows(CancellationException.class, () -> downloader.downloadTiles(3, -80, 90, progressMonitor));
            assertFalse(new File(temporaryFolder.getRoot(), "Copernicus_DSM_COG_30_N03_00_W080_00_DEM.tif").exists());
        } finally {
            server.close();
        }
    }

    private static class CancelAfterChecksProgressMonitor implements ProgressMonitor {

        private final int allowedChecks;
        private int checks;

        CancelAfterChecksProgressMonitor(int allowedChecks) {
            this.allowedChecks = allowedChecks;
        }

        @Override
        public void beginTask(String taskName, int totalWork) {
        }

        @Override
        public void done() {
        }

        @Override
        public void internalWorked(double work) {
        }

        @Override
        public boolean isCanceled() {
            checks++;
            return checks > allowedChecks;
        }

        @Override
        public void setCanceled(boolean canceled) {
        }

        @Override
        public void setTaskName(String taskName) {
        }

        @Override
        public void setSubTaskName(String subTaskName) {
        }

        @Override
        public void worked(int work) {
        }
    }

    private static class SlowHttpServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private Thread thread;

        SlowHttpServer() throws IOException {
            serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        }

        void start() {
            thread = new Thread(this::serve, getClass().getSimpleName());
            thread.start();
        }

        String getBaseUrl() {
            return "http://" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort();
        }

        private void serve() {
            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    // consume request headers
                }

                byte[] chunk = new byte[4096];
                int chunkCount = 16;
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: image/tiff\r\n" +
                        "Content-Length: " + chunk.length * chunkCount + "\r\n" +
                        "\r\n").getBytes(StandardCharsets.US_ASCII));
                for (int i = 0; i < chunkCount; i++) {
                    outputStream.write(chunk);
                    outputStream.flush();
                    Thread.sleep(5);
                }
            } catch (IOException | InterruptedException ignore) {
                // The client is expected to close the connection when cancellation works.
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }
}
