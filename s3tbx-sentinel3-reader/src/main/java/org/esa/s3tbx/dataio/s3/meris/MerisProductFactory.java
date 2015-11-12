package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReaderFactory;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
public class MerisProductFactory extends AbstractProductFactory {

    private Map<String, Float> nameToWavelengthMap;
    private Map<String, Float> nameToBandwidthMap;
    private Map<String, Integer> nameToIndexMap;
    private int subSamplingX;
    private int subSamplingY;
    private int rows;
    private int columns;
    public final static String MERIS_SAFE_USE_PIXELGEOCODING = "s3tbx.reader.meris.pixelGeoCoding";

    public MerisProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        nameToWavelengthMap = new HashMap<>();
        nameToBandwidthMap = new HashMap<>();
        nameToIndexMap = new HashMap<>();
        rows = -1;
        columns = -1;
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final List<String> fileNames = manifest.getFileNames(new String[0]);
        final MetadataElement metadataElement = manifest.getMetadata().getElement("metadataSection");
        final MetadataElement merisInformationElement = metadataElement.getElement("merisProductInformation");
        final MetadataElement imageSizeElement = merisInformationElement.getElement("imageSize");
        rows = Integer.parseInt(imageSizeElement.getAttribute("rows").getData().toString());
        columns = Integer.parseInt(imageSizeElement.getAttribute("columns").getData().toString());
        return fileNames;
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        final MetadataElement merisInformationElement = metadataElement.getElement("merisProductInformation");
        final MetadataElement samplingParametersElement = merisInformationElement.getElement("samplingParameters");
        subSamplingY = Integer.parseInt(samplingParametersElement.getAttribute("rowsPerTiePoint").getData().toString());
        subSamplingX = Integer.parseInt(samplingParametersElement.getAttribute("columnsPerTiePoint").getData().toString());
        final MetadataElement bandDescriptionsElement = merisInformationElement.getElement("bandDescriptions");
        if (bandDescriptionsElement != null) {
            for (int i = 0; i < bandDescriptionsElement.getNumElements(); i++) {
                final MetadataElement bandDescriptionElement = bandDescriptionsElement.getElementAt(i);
                final String bandName = bandDescriptionElement.getAttribute("name").getData().getElemString();
                final float wavelength =
                        Float.parseFloat(bandDescriptionElement.getAttribute("centralWavelength").getData().getElemString());
                final float bandWidth =
                        Float.parseFloat(bandDescriptionElement.getAttribute("bandWidth").getData().getElemString());
                nameToWavelengthMap.put(bandName, wavelength);
                nameToBandwidthMap.put(bandName, bandWidth);
                nameToIndexMap.put(bandName, i);
            }
        }
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = null;
        if (rows > 0 && columns > 0) {
            for (Product product : productList) {
                if (product.getSceneRasterWidth() == columns &&
                        product.getSceneRasterHeight() == rows) {
                    masterProduct = product;
                }
            }
        }
        if (masterProduct == null) {
            int masterproductWidth = -1;
            int masterproductHeight = -1;
            for (Product product : productList) {
                if (product.getSceneRasterWidth() > masterproductWidth &&
                        product.getSceneRasterHeight() > masterproductHeight &&
                        !product.getName().contains("flags") &&
                        !product.getName().contains("time") &&
                        !product.getName().contains("tie")
                        ) {
                    masterProduct = product;
                    masterproductWidth = product.getSceneRasterWidth();
                    masterproductHeight = product.getSceneRasterHeight();
                }
            }
        }
        return masterProduct;
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        if (targetProduct.containsBand(sourceBandName)) {
            sourceBand.setName("TP_" + sourceBandName);
        }
        return copyBandAsTiePointGrid(sourceBand, targetProduct, subSamplingX, subSamplingY, 0.0f, 0.0f);
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        if (Config.instance("s3tbx").load().preferences().getBoolean(MERIS_SAFE_USE_PIXELGEOCODING, false)) {
            final Band latBand = targetProduct.getBand("latitude");
            final Band lonBand = targetProduct.getBand("longitude");
            if (latBand != null && lonBand != null) {
                targetProduct.setSceneGeoCoding(
                        GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, getValidExpression(), 5));
            }
        }
        if (targetProduct.getSceneGeoCoding() == null) {
            if (targetProduct.getTiePointGrid("latitude") != null && targetProduct.getTiePointGrid(
                    "longitude") != null) {
                targetProduct.setSceneGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("latitude"),
                                                                      targetProduct.getTiePointGrid("longitude")));
            }
        }
        if (targetProduct.getSceneGeoCoding() == null) {
            if (targetProduct.getTiePointGrid("TP_latitude") != null && targetProduct.getTiePointGrid(
                    "TP_longitude") != null) {
                targetProduct.setSceneGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("TP_latitude"),
                                                                      targetProduct.getTiePointGrid("TP_longitude")));
            }
        }
    }

    @Override
    protected Product readProduct(String fileName) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        final S3NetcdfReader reader = S3NetcdfReaderFactory.createS3NetcdfReader(file);
        addSeparatingDimensions(reader.getSuffixesForSeparatingDimensions());
        return reader.readProduct();
    }

    protected String getValidExpression() {
        return "";
    }

    protected float getWavelength(String name) {
        return nameToWavelengthMap.get(name);
    }

    protected float getBandwidth(String name) {
        return nameToBandwidthMap.get(name);
    }

    protected int getBandindex(String name) {
        return nameToIndexMap.get(name);
    }

}
