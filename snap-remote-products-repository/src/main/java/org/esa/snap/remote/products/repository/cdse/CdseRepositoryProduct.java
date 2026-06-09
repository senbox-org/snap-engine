package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.DataFormatType;
import org.esa.snap.remote.products.repository.PixelType;
import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.SensorType;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

class CdseRepositoryProduct implements RepositoryProduct {

    private final String id;
    private final String name;
    private final String downloadUrl;
    private final RemoteMission remoteMission;
    private final AbstractGeometry2D polygon;
    private final LocalDateTime acquisitionDate;
    private long approximateSize;
    private List<Attribute> remoteAttributes = Collections.emptyList();
    private List<Attribute> localAttributes = Collections.emptyList();
    private BufferedImage quickLookImage;
    private PixelType pixelType;
    private DataFormatType dataFormatType;
    private SensorType sensorType;
    private String metadataMission;
    private String downloadQuickLookImageUrl;

    CdseRepositoryProduct(String id, String name, String downloadUrl, RemoteMission remoteMission,
                          AbstractGeometry2D polygon, LocalDateTime acquisitionDate, long approximateSize) {
        this.id = id;
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.remoteMission = remoteMission;
        this.polygon = polygon;
        this.acquisitionDate = acquisitionDate;
        this.approximateSize = approximateSize;
    }

    public String getId() {
        return id;
    }

    @Override
    public AbstractGeometry2D getPolygon() {
        return polygon;
    }

    @Override
    public List<Attribute> getRemoteAttributes() {
        return remoteAttributes;
    }

    @Override
    public List<Attribute> getLocalAttributes() {
        return localAttributes;
    }

    @Override
    public void setLocalAttributes(List<Attribute> localAttributes) {
        this.localAttributes = localAttributes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getApproximateSize() {
        return approximateSize;
    }

    @Override
    public void setApproximateSize(long approximateSize) {
        this.approximateSize = approximateSize;
    }

    @Override
    public String getDownloadQuickLookImageURL() {
        return downloadQuickLookImageUrl;
    }

    @Override
    public String getURL() {
        return downloadUrl;
    }

    @Override
    public LocalDateTime getAcquisitionDate() {
        return acquisitionDate;
    }

    @Override
    public void setQuickLookImage(BufferedImage quickLookImage) {
        this.quickLookImage = quickLookImage;
    }

    @Override
    public BufferedImage getQuickLookImage() {
        return quickLookImage;
    }

    @Override
    public PixelType getPixelType() {
        return pixelType;
    }

    @Override
    public DataFormatType getDataFormatType() {
        return dataFormatType;
    }

    @Override
    public SensorType getSensorType() {
        return sensorType;
    }

    @Override
    public RemoteMission getRemoteMission() {
        return remoteMission;
    }

    @Override
    public String getMetadataMission() {
        return metadataMission;
    }

    @Override
    public void setMetadataMission(String metadataMission) {
        this.metadataMission = metadataMission;
    }

    void setRemoteAttributes(List<Attribute> remoteAttributes) {
        this.remoteAttributes = remoteAttributes;
    }

    void setPixelType(PixelType pixelType) {
        this.pixelType = pixelType;
    }

    void setDataFormatType(DataFormatType dataFormatType) {
        this.dataFormatType = dataFormatType;
    }

    void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    void setDownloadQuickLookImageUrl(String downloadQuickLookImageUrl) {
        this.downloadQuickLookImageUrl = downloadQuickLookImageUrl;
    }
}
