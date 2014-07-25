package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.s3.AbstractProductFactory;
import org.esa.beam.dataio.s3.Manifest;
import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCodingFactory;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
abstract class OlciProductFactory extends AbstractProductFactory {

    private final static String[] excludedIDs = new String[]{"timeCoordinatesData", "removedPixelsData"};

    private Map<String, Float> nameToWavelengthMap;
    private Map<String, Float> nameToBandwidthMap;
    private Map<String, Integer> nameToIndexMap;
    private int subSamplingX;
    private int subSamplingY;

    public OlciProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        nameToWavelengthMap = new HashMap<String, Float>();
        nameToBandwidthMap = new HashMap<String, Float>();
        nameToIndexMap = new HashMap<String, Integer>();
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(excludedIDs);
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        final MetadataElement olciInformationElement = metadataElement.getElement("olciProductInformation");
        final MetadataElement samplingParametersElement = olciInformationElement.getElement("samplingParameters");
        subSamplingX = samplingParametersElement.getAttribute("rowsPerTiePoint").getData().getElemInt();
        subSamplingY = samplingParametersElement.getAttribute("columnsPerTiePoint").getData().getElemInt();
        final MetadataElement bandDescriptionsElement = olciInformationElement.getElement("bandDescriptions");
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

    private float getWavelength(String name) {
        return nameToWavelengthMap.get(name);
    }

    private float getBandwidth(String name) {
        return nameToBandwidthMap.get(name);
    }

    private int getBandindex(String name) {
        return nameToIndexMap.get(name);
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
        final Band latBand = targetProduct.getBand("latitude");
        final Band lonBand = targetProduct.getBand("longitude");
        if (latBand != null && lonBand != null) {

            targetProduct.setGeoCoding(
                    GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, getValidExpression(), 5));
        }
        if (targetProduct.getGeoCoding() == null) {
            if (targetProduct.getTiePointGrid("latitude") != null && targetProduct.getTiePointGrid(
                    "longitude") != null) {
                targetProduct.setGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("latitude"),
                                                                 targetProduct.getTiePointGrid("longitude")));
            }
        }
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        if (targetNode.getName().matches("Oa[0-2][0-9].*")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                String cutName = targetBand.getName().substring(0, 4);
                targetBand.setSpectralBandIndex(getBandindex(cutName));
                targetBand.setSpectralWavelength(getWavelength(cutName));
                targetBand.setSpectralBandwidth(getBandwidth(cutName));
            }
        }
        targetNode.setValidPixelExpression(getValidExpression());
    }

    protected abstract String getValidExpression();

}
