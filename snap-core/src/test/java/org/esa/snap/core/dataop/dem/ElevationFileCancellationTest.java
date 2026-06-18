package org.esa.snap.core.dataop.dem;

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

public class ElevationFileCancellationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    @STTM("SNAP-4209")
    public void testHttpDownloadStopsWhenProgressMonitorIsCanceled() throws Exception {
        SlowHttpServer server = new SlowHttpServer("tile.zip");
        server.start();
        try {
            TestElevationFile elevationFile = new TestElevationFile(new File(temporaryFolder.getRoot(), "tile.dat"));
            ProgressMonitor progressMonitor = new CancelAfterChecksProgressMonitor(1);

            assertThrows(CancellationException.class, () -> elevationFile.download(server.getBaseUrl(), progressMonitor));

            assertFalse(new File(temporaryFolder.getRoot(), "tile.zip").exists());
        } finally {
            server.close();
        }
    }

    private static class TestElevationFile extends ElevationFile {

        TestElevationFile(File localFile) {
            super(localFile, null);
        }

        Boolean download(String baseUrl, ProgressMonitor progressMonitor) throws IOException {
            return getRemoteHttpFile(baseUrl, progressMonitor);
        }

        @Override
        protected Boolean getRemoteFile() {
            return false;
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

        private final String fileName;
        private final ServerSocket serverSocket;
        private Thread thread;

        SlowHttpServer(String fileName) throws IOException {
            this.fileName = fileName;
            serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        }

        void start() {
            thread = new Thread(this::serve, getClass().getSimpleName());
            thread.start();
        }

        String getBaseUrl() {
            return "http://" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort() + "/";
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
                        "Content-Type: application/octet-stream\r\n" +
                        "Content-Length: " + chunk.length * chunkCount + "\r\n" +
                        "Content-Disposition: attachment; filename=\"" + fileName + "\"\r\n" +
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
