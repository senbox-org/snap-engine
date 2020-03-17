package org.esa.snap.remote.products.repository.tao;

import org.esa.snap.remote.products.repository.*;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 28/8/2019.
 */
public class TAORepositoryProduct implements RepositoryProduct {

    private final String id;
    private final String name;
    private final RemoteMission remoteMission;
    private final String downloadURL;
    private final AbstractGeometry2D polygon;
    private final Date acquisitionDate;
    private final long approximateSize;

    private List<Attribute> attributes;
    private BufferedImage quickLookImage;
    private SensorType sensorType;
    private DataFormatType dataFormatType;
    private PixelType pixelType;
    private String downloadQuickLookImageURL;

    TAORepositoryProduct(String id, String name, String downloadURL, RemoteMission remoteMission, AbstractGeometry2D polygon, Date acquisitionDate, long approximateSize) {
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
    public List<Attribute> getAttributes() {
        return attributes;
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

    void setDownloadQuickLookImageURL(String downloadQuickLookImageURL) {
        this.downloadQuickLookImageURL = downloadQuickLookImageURL;
    }

    String getId() {
        return id;
    }

    void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
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
