package org.esa.snap.remote.products.repository;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.cdse.CdseProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RemoteProductsRepositoryProviderApiTest {

    @Test
    public void returnsNativeCdseProviderByRepositoryName() {
        RemoteProductsRepositoryProvider provider = RemoteProductsRepositoryProvider.getRemoteProductsRepositoryProvider(CdseProductsRepositoryProvider.REPOSITORY_NAME);

        assertEquals(CdseProductsRepositoryProvider.REPOSITORY_NAME, provider.getRepositoryName());
        assertEquals("Sentinel3", provider.getAvailableMissions()[0]);
    }

    @Test
    public void defaultMaxResultsOverloadDelegatesAndTrimsLegacyResults() throws Exception {
        LegacyProvider provider = new LegacyProvider();

        List<RepositoryProduct> products = provider.downloadProductList(null, "Sentinel3", 20, 1, Map.of(), null, () -> false);

        assertEquals(20, provider.pageSize);
        assertEquals(1, products.size());
        assertEquals("legacy-a", products.get(0).getName());
    }

    private static class LegacyProvider implements RemoteProductsRepositoryProvider {
        int pageSize;

        @Override
        public int getMaximumAllowedTransfersPerAccount() {
            return 1;
        }

        @Override
        public boolean requiresAuthentication() {
            return false;
        }

        @Override
        public String getRepositoryName() {
            return "Legacy";
        }

        @Override
        public String[] getAvailableMissions() {
            return new String[]{"Sentinel3"};
        }

        @Override
        public List<RepositoryQueryParameter> getMissionParameters(String mission) {
            return List.of();
        }

        @Override
        public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, int pageSize, Map<String, Object> parameterValues, ProductListDownloaderListener downloaderListener, ThreadStatus thread) {
            this.pageSize = pageSize;
            List<RepositoryProduct> products = new ArrayList<>();
            products.add(new LegacyProduct("legacy-a"));
            products.add(new LegacyProduct("legacy-b"));
            return products;
        }

        @Override
        public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread) throws IOException {
            return null;
        }

        @Override
        public Map<String, String> getDisplayedAttributes() {
            return Map.of();
        }

        @Override
        public void cancelDownloadProduct(RepositoryProduct repositoryProduct) {
        }

        @Override
        public Path downloadProduct(RepositoryProduct repositoryProduct, Credentials credentials, Path targetFolderPath, ProgressListener progressListener, boolean uncompressedDownloadedProduct) {
            return targetFolderPath;
        }
    }

    private static class LegacyProduct implements RepositoryProduct {
        private final String name;

        private LegacyProduct(String name) {
            this.name = name;
        }

        @Override
        public AbstractGeometry2D getPolygon() {
            return null;
        }

        @Override
        public List<Attribute> getRemoteAttributes() {
            return List.of();
        }

        @Override
        public List<Attribute> getLocalAttributes() {
            return List.of();
        }

        @Override
        public void setLocalAttributes(List<Attribute> localAttributes) {
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getApproximateSize() {
            return 0;
        }

        @Override
        public void setApproximateSize(long approximateSize) {
        }

        @Override
        public String getDownloadQuickLookImageURL() {
            return null;
        }

        @Override
        public String getURL() {
            return null;
        }

        @Override
        public LocalDateTime getAcquisitionDate() {
            return null;
        }

        @Override
        public void setQuickLookImage(BufferedImage quickLookImage) {
        }

        @Override
        public BufferedImage getQuickLookImage() {
            return null;
        }

        @Override
        public PixelType getPixelType() {
            return null;
        }

        @Override
        public DataFormatType getDataFormatType() {
            return DataFormatType.OTHER;
        }

        @Override
        public SensorType getSensorType() {
            return SensorType.UNKNOWN;
        }

        @Override
        public RemoteMission getRemoteMission() {
            return new RemoteMission("Sentinel3", "Legacy");
        }

        @Override
        public String getMetadataMission() {
            return null;
        }

        @Override
        public void setMetadataMission(String metadataMission) {
        }
    }
}
