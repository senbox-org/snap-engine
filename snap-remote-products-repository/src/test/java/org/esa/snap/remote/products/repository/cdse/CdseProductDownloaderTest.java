package org.esa.snap.remote.products.repository.cdse;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CdseProductDownloaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void downloadsProductWithBearerTokenToZipFile() throws Exception {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        httpClient.enqueue(new CdseHttpResponse(200, "{\"access_token\":\"token\"}".getBytes(StandardCharsets.UTF_8)));
        byte[] productBytes = "downloaded product".getBytes(StandardCharsets.UTF_8);
        httpClient.enqueue(new CdseHttpResponse(200, productBytes));
        CdseProductDownloader downloader = new CdseProductDownloader(httpClient, new CdseAuthClient(httpClient), new CdseZipExtractor());
        RepositoryProduct product = new CdseRepositoryProduct(
                "product-id",
                "S3A_OL_1_EFR____20240627T090259.SEN3",
                CdseProductsRepositoryProvider.downloadUrl("product-id"),
                new RemoteMission("Sentinel3", CdseProductsRepositoryProvider.REPOSITORY_NAME),
                null,
                LocalDateTime.of(2024, 6, 27, 9, 2, 59),
                productBytes.length);

        Path downloaded = downloader.download(
                product,
                new UsernamePasswordCredentials("user", "password"),
                temporaryFolder.getRoot().toPath(),
                new NoOpProgressListener(),
                false);

        assertEquals(temporaryFolder.getRoot().toPath().resolve("S3A_OL_1_EFR____20240627T090259.SEN3.zip"), downloaded);
        assertArrayEquals(productBytes, Files.readAllBytes(downloaded));
        CdseHttpRequest downloadRequest = httpClient.requests.get(1);
        assertEquals("GET", downloadRequest.getMethod());
        assertEquals(CdseProductsRepositoryProvider.downloadUrl("product-id"), downloadRequest.getUrl());
        assertEquals("Bearer token", downloadRequest.getHeaders().get("Authorization"));
    }

    private static class RecordingHttpClient implements CdseHttpClient {
        final List<CdseHttpRequest> requests = new ArrayList<>();
        final Queue<CdseHttpResponse> responses = new ArrayDeque<>();

        void enqueue(CdseHttpResponse response) {
            responses.add(response);
        }

        @Override
        public CdseHttpResponse execute(CdseHttpRequest request) {
            requests.add(request);
            return responses.remove();
        }
    }

    private static class NoOpProgressListener implements ProgressListener {
        @Override
        public void notifyProgress(short progressPercent) {
        }

        @Override
        public void notifyApproximateSize(long approximateSize) {
        }

        @Override
        public void notifyProductStatus(String productStatus) {
        }
    }
}
