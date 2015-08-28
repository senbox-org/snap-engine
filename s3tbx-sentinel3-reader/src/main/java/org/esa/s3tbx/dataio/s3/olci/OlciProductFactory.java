package org.esa.s3tbx.dataio.s3.olci;

import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReaderFactory;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoCodingFactory;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.datamodel.TiePointGeoCoding;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
abstract class OlciProductFactory extends AbstractProductFactory {

    private final static String[] excludedIDs = new String[]{"removedPixelsData"};

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
        subSamplingY = Integer.parseInt(samplingParametersElement.getAttribute("rowsPerTiePoint").getData().toString());
        subSamplingX = Integer.parseInt(samplingParametersElement.getAttribute("columnsPerTiePoint").getData().toString());
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
        if (Config.instance().preferences().getBoolean("s3tbx.reader.olci.pixelGeoCoding", false)) {
            setPixelGeoCoding(targetProduct);
        } else {
            setTiePointGeoCoding(targetProduct);
        }
    }

    private void setPixelGeoCoding(Product targetProduct) {
        final Band latBand = targetProduct.getBand("latitude");
        final Band lonBand = targetProduct.getBand("longitude");
        if (latBand != null && lonBand != null) {
            targetProduct.setGeoCoding(
                    GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, getValidExpression(), 5));
        }
    }

    private void setTiePointGeoCoding(Product targetProduct) {
        if (targetProduct.getGeoCoding() == null) {
            if (targetProduct.getTiePointGrid("latitude") != null && targetProduct.getTiePointGrid(
                    "longitude") != null) {
                targetProduct.setGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("latitude"),
                                                                 targetProduct.getTiePointGrid("longitude")));
            }
        }
        if (targetProduct.getGeoCoding() == null) {
            if (targetProduct.getTiePointGrid("TP_latitude") != null && targetProduct.getTiePointGrid(
                    "TP_longitude") != null) {
                targetProduct.setGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("TP_latitude"),
                                                                 targetProduct.getTiePointGrid("TP_longitude")));
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
        // convert log10 scaled variables int concentrations and also their error bands
        // the unit string follows the CF conventions.
        // See: http://www.unidata.ucar.edu/software/udunits/udunits-2.0.4/udunits2lib.html#Syntax
        if (targetNode.getName().startsWith("ADG443_NN") ||
                targetNode.getName().startsWith("CHL_NN") ||
                targetNode.getName().startsWith("CHL_OC4ME") ||
                targetNode.getName().startsWith("KD490_M07") ||
                targetNode.getName().startsWith("TSM_NN")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                String unit = targetBand.getUnit();
                Pattern pattern = Pattern.compile("lg\\s*\\(\\s*re:?\\s*(.*)\\)");
                final Matcher m = pattern.matcher(unit);
                if (m.matches()) {
                    targetBand.setLog10Scaled(true);
                    targetBand.setUnit(m.group(1));
                    String description = targetBand.getDescription();
                    description = description.replace("log10 scaled ", "");
                    targetBand.setDescription(description);
                } else {
                    getLogger().log(Level.WARNING, "Unit extraction not working for band " + targetNode.getName());
                }

            }
        }
        targetNode.setValidPixelExpression(getValidExpression());
    }

    protected abstract String getValidExpression();

    @Override
    protected Product readProduct(String fileName) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        final S3NetcdfReader reader = S3NetcdfReaderFactory.createS3NetcdfReader(file);
        addSeparatingDimensions(reader.getSuffixesForSeparatingDimensions());
        return reader.readProduct();
    }

}
