package org.esa.snap.product.library.v2.database.model;

import org.esa.snap.remote.products.repository.*;
import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class LocalRepositoryProduct implements RepositoryProduct {

    private final int id;
    private final String name;
    private final Date acquisitionDate;
    private final long sizeInBytes;
    private final AbstractGeometry2D polygon;

    private Path path;
    private org.esa.snap.remote.products.repository.RemoteMission remoteMission;
    private List<Attribute> remoteAttributes;
    private List<Attribute> localAttributes;
    private BufferedImage quickLookImage;

    public LocalRepositoryProduct(int id, String name, Date acquisitionDate, Path path, long sizeInBytes, AbstractGeometry2D polygon) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.acquisitionDate = acquisitionDate;
        this.sizeInBytes = sizeInBytes;
        this.polygon = polygon;
    }

    @Override
    public AbstractGeometry2D getPolygon() {
        return this.polygon;
    }

    @Override
    public List<Attribute> getRemoteAttributes() {
        return this.remoteAttributes;
    }

    @Override
    public List<Attribute> getLocalAttributes() {
        return this.localAttributes;
    }

    @Override
    public void setLocalAttributes(List<Attribute> localAttributes) {
        this.localAttributes = localAttributes;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public long getApproximateSize() {
        return this.sizeInBytes;
    }

    @Override
    public String getDownloadQuickLookImageURL() {
        return null;
    }

    @Override
    public String getURL() {
        return this.path.toString();
    }

    @Override
    public Date getAcquisitionDate() {
        return this.acquisitionDate;
    }

    @Override
    public BufferedImage getQuickLookImage() {
        return quickLookImage;
    }

    @Override
    public PixelType getPixelType() {
        return null;
    }

    @Override
    public DataFormatType getDataFormatType() {
        return null;
    }

    @Override
    public SensorType getSensorType() {
        return null;
    }

    @Override
    public void setQuickLookImage(BufferedImage quickLookImage) {
        this.quickLookImage = quickLookImage;
    }

    @Override
    public RemoteMission getRemoteMission() {
        return this.remoteMission;
    }

    public void setRemoteAttributes(List<Attribute> remoteAttributes) {
        this.remoteAttributes = remoteAttributes;
    }

    public void setRemoteMission(org.esa.snap.remote.products.repository.RemoteMission remoteMission) {
        this.remoteMission = remoteMission;
    }

    public int getId() {
        return id;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
