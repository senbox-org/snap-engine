package org.esa.snap.remote.products.repository.cdse;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

class CdseProductDownloader {

    private final CdseHttpClient httpClient;
    private final CdseAuthClient authClient;
    private final CdseZipExtractor zipExtractor;

    CdseProductDownloader(CdseHttpClient httpClient, CdseAuthClient authClient, CdseZipExtractor zipExtractor) {
        this.httpClient = httpClient;
        this.authClient = authClient;
        this.zipExtractor = zipExtractor;
    }

    Path download(RepositoryProduct product, Credentials credentials, Path targetFolder, ProgressListener progressListener,
                  boolean uncompressedDownloadedProduct) throws IOException {
        if (product == null) {
            throw new NullPointerException("The repository product is null.");
        }
        String token = authClient.accessToken(credentials);
        String productUrl = productUrl(product);
        Path zipFile = targetFolder.resolve(zipFileName(product.getName()));
        CdseHttpRequest request = new CdseHttpRequest("GET", productUrl, Map.of("Authorization", "Bearer " + token), null);
        httpClient.download(request, zipFile, progressListener);
        if (!uncompressedDownloadedProduct) {
            return zipFile;
        }
        return zipExtractor.extract(zipFile, targetFolder, product.getName(), progressListener);
    }

    private static String productUrl(RepositoryProduct product) {
        if (product.getURL() != null && !product.getURL().isBlank()) {
            return product.getURL();
        }
        if (product instanceof CdseRepositoryProduct cdseProduct && cdseProduct.getId() != null && !cdseProduct.getId().isBlank()) {
            return CdseProductsRepositoryProvider.downloadUrl(cdseProduct.getId());
        }
        throw new IllegalArgumentException("CDSE product download URL is missing.");
    }

    private static String zipFileName(String productName) {
        if (productName == null || productName.isBlank()) {
            return "product.zip";
        }
        return productName.toLowerCase().endsWith(".zip") ? productName : productName + ".zip";
    }
}
