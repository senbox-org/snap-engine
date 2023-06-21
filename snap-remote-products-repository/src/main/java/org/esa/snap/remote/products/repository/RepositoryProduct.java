package org.esa.snap.remote.products.repository;

import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The data about the repository product.
 *
 * Created by jcoravu on 12/8/2019.
 */
public interface RepositoryProduct {

    public AbstractGeometry2D getPolygon();

    public List<Attribute> getRemoteAttributes();

    public List<Attribute> getLocalAttributes();

    public void setLocalAttributes(List<Attribute> localAttributes);

    public String getName();

    public long getApproximateSize();

    public void setApproximateSize(long approximateSize);

    public String getDownloadQuickLookImageURL();

    public String getURL();

    public LocalDateTime getAcquisitionDate();

    public void setQuickLookImage(BufferedImage quickLookImage);

    public BufferedImage getQuickLookImage();

    public PixelType getPixelType();

    public DataFormatType getDataFormatType();

    public SensorType getSensorType();

    public RemoteMission getRemoteMission();

    public String getMetadataMission();

    public void setMetadataMission(String metadataMission);
}
