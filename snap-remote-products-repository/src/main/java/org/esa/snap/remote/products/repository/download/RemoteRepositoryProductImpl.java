package org.esa.snap.remote.products.repository.download;

import org.esa.snap.remote.products.repository.*;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;

/**
 * The implementation class for a remote repository product.
 *
 * Created by jcoravu on 28/8/2019.
 */
public class RemoteRepositoryProductImpl implements RepositoryProduct {

    private final String id;
    private final String name;
    private final RemoteMission remoteMission;
    private final String downloadURL;
    private final AbstractGeometry2D polygon;
    private final Date acquisitionDate;

    private List<Attribute> remoteAttributes;
    private List<Attribute> localAttributes;
    private long approximateSize;
    private BufferedImage quickLookImage;
    private SensorType sensorType;
    private DataFormatType dataFormatType;
    private PixelType pixelType;
    private String downloadQuickLookImageURL;

    RemoteRepositoryProductImpl(String id, String name, String downloadURL, RemoteMission remoteMission, AbstractGeometry2D polygon, Date acquisitionDate, long approximateSize) {
        this.id = id;
        this.name = name;
        this.remoteMission = remoteMission;
        this.downloadURL = downloadURL;
        this.polygon = polygon;
        this.acquisitionDate = acquisitionDate;
        this.approximateSize = approximateSize;
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
        return this.localAttributes;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public long getApproximateSize() {
        return this.approximateSize;
    }

    @Override
    public void setApproximateSize(long approximateSize) {
        this.approximateSize = approximateSize;
    }

    @Override
    public String getDownloadQuickLookImageURL() {
        return this.downloadQuickLookImageURL;
    }

    @Override
    public String getURL() {
        return this.downloadURL;
    }

    @Override
    public Date getAcquisitionDate() {
        return this.acquisitionDate;
    }

    @Override
    public BufferedImage getQuickLookImage() {
        return this.quickLookImage;
    }

    @Override
    public void setQuickLookImage(BufferedImage quickLookImage) {
        this.quickLookImage = quickLookImage;
    }

    @Override
    public PixelType getPixelType() {
        return this.pixelType;
    }

    @Override
    public DataFormatType getDataFormatType() {
        return this.dataFormatType;
    }

    @Override
    public SensorType getSensorType() {
        return this.sensorType;
    }

    @Override
    public RemoteMission getRemoteMission() {
        return this.remoteMission;
    }

    @Override
    public void setLocalAttributes(List<Attribute> localAttributes) {
        this.localAttributes = localAttributes;
    }

    void setDownloadQuickLookImageURL(String downloadQuickLookImageURL) {
        this.downloadQuickLookImageURL = downloadQuickLookImageURL;
    }

    String getId() {
        return id;
    }

    void setRemoteAttributes(List<Attribute> attributes) {
        this.remoteAttributes = attributes;
    }

    void setDataFormatType(DataFormatType dataFormatType) {
        this.dataFormatType = dataFormatType;
    }

    void setPixelType(PixelType pixelType) {
        this.pixelType = pixelType;
    }

    void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }
}
