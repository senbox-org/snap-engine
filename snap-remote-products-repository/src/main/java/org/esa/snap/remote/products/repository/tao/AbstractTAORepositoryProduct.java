package org.esa.snap.remote.products.repository.tao;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.DataFormatType;
import org.esa.snap.remote.products.repository.PixelType;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.SensorType;
import ro.cs.tao.eodata.EOProduct;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 28/8/2019.
 */
public abstract class AbstractTAORepositoryProduct implements RepositoryProduct {

    protected final EOProduct product;
    protected final List<Attribute> attributes;

    private final String mission;
    private final String downloadURL;

    private BufferedImage quickLookImage;

    protected AbstractTAORepositoryProduct(EOProduct product, String mission) {
        this.product = product;
        this.mission = mission;
        this.downloadURL = product.getLocation();

        List<ro.cs.tao.eodata.Attribute> remoteAttributes = product.getAttributes();
        this.attributes = new ArrayList<>(remoteAttributes.size());
        for (int i=0; i<remoteAttributes.size(); i++) {
            ro.cs.tao.eodata.Attribute remoteAttribute = remoteAttributes.get(i);
            this.attributes.add(new Attribute(remoteAttribute.getName(), remoteAttribute.getValue()));
        }
    }

    @Override
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public String getMission() {
        return mission;
    }

    @Override
    public String getName() {
        return this.product.getName();
    }

    @Override
    public long getApproximateSize() {
        return this.product.getApproximateSize();
    }

    @Override
    public String getDownloadQuickLookImageURL() {
        return this.product.getQuicklookLocation();
    }

    @Override
    public String getDownloadURL() {
        return this.downloadURL;
    }

    @Override
    public Date getAcquisitionDate() {
        return this.product.getAcquisitionDate();
    }

    @Override
    public BufferedImage getQuickLookImage() {
        return quickLookImage;
    }

    @Override
    public void setQuickLookImage(BufferedImage quickLookImage) {
        this.quickLookImage = quickLookImage;
    }

    @Override
    public PixelType getPixelType() {
        return convertToPixelType(this.product.getPixelType());
    }

    @Override
    public DataFormatType getDataFormatType() {
        return convertToDataFormatType(this.product.getFormatType());
    }

    @Override
    public SensorType getSensorType() {
        return convertToSensorType(this.product.getSensorType());
    }

    @Override
    public String getGeometry() {
        return this.product.getGeometry();
    }

    @Override
    public String getEntryPoint() {
        return this.product.getEntryPoint();
    }

    private static DataFormatType convertToDataFormatType(ro.cs.tao.eodata.enums.DataFormat dataFormat) {
        if (dataFormat == ro.cs.tao.eodata.enums.DataFormat.RASTER) {
            return DataFormatType.RASTER;
        }
        if (dataFormat == ro.cs.tao.eodata.enums.DataFormat.VECTOR) {
            return DataFormatType.VECTOR;
        }
        if (dataFormat == ro.cs.tao.eodata.enums.DataFormat.OTHER) {
            return DataFormatType.OTHER;
        }
        throw new IllegalArgumentException("Unknown data format: "+ dataFormat);
    }

    private static SensorType convertToSensorType(ro.cs.tao.eodata.enums.SensorType sensorType) {
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.OPTICAL) {
            return SensorType.OPTICAL;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.RADAR) {
            return SensorType.RADAR;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.ALTIMETRIC) {
            return SensorType.ALTIMETRIC;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.ATMOSPHERIC) {
            return SensorType.ATMOSPHERIC;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.UNKNOWN) {
            return SensorType.UNKNOWN;
        }
        throw new IllegalArgumentException("Unknown sensor type: "+ sensorType);
    }

    private static PixelType convertToPixelType(ro.cs.tao.eodata.enums.PixelType pixelType) {
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT8) {
            return PixelType.UINT8;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT8) {
            return PixelType.UINT8;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.INT8) {
            return PixelType.INT8;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT16) {
            return PixelType.UINT16;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT32) {
            return PixelType.UINT32;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.INT32) {
            return PixelType.INT32;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.FLOAT32) {
            return PixelType.FLOAT32;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.FLOAT64) {
            return PixelType.FLOAT64;
        }
        throw new IllegalArgumentException("Unknown pixel type: "+ pixelType);
    }
}
