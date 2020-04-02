package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;
import org.geotools.referencing.CRS;
import org.jdom.Element;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.util.stream.IntStream;

public class ComponentGeoCodingPersistable implements DimapPersistable {

    public static final String TAG_COMPONENT_GEO_CODING = "ComponentGeoCoding";
    public static final String TAG_FORWARD_CODING_KEY = "ForwardCodingKey";
    public static final String TAG_INVERSE_CODING_KEY = "InverseCodingKey";
    public static final String TAG_GEO_CHECKS = "GeoChecks";
    public static final String TAG_GEO_CRS = "GeoCRS";
    public static final String TAG_LON_VARIABLE_NAME = "LonVariableName";
    public static final String TAG_LAT_VARIABLE_NAME = "LatVariableName";
    public static final String TAG_RASTER_RESOLUTION_KM = "RasterResolutionKm";
    public static final String TAG_OFFSET_X = "OffsetX";
    public static final String TAG_OFFSET_Y = "OffsetY";
    public static final String TAG_SUBSAMPLING_X = "SubsamplingX";
    public static final String TAG_SUBSAMPLING_Y = "SubsamplingY";

    @Override
    public Object createObjectFromXml(Element element, Product product, Dimension regionRasterSize) {
        final String gcElemName = element.getName();

        final Element codingMain = element.getChild(TAG_COMPONENT_GEO_CODING);
        if (codingMain == null) {
            SystemUtils.LOG.warning("Child element <" + TAG_COMPONENT_GEO_CODING + "> expected in element <" + gcElemName + ">");
            return null;
        }

        final String forwardKey = codingMain.getChildTextTrim(TAG_FORWARD_CODING_KEY);
        final String inverseKey = codingMain.getChildTextTrim(TAG_INVERSE_CODING_KEY);
        final String geoChecksName = codingMain.getChildTextTrim(TAG_GEO_CHECKS);
        final String geoCrsWKT = codingMain.getChildTextTrim(TAG_GEO_CRS);
        final String lonVarName = codingMain.getChildTextTrim(TAG_LON_VARIABLE_NAME);
        final String latVarName = codingMain.getChildTextTrim(TAG_LAT_VARIABLE_NAME);
        final String resolutionKmStr = codingMain.getChildTextTrim(TAG_RASTER_RESOLUTION_KM);

        final boolean forwardInvalid = forwardKey == null;
        final boolean inverseInvalid = inverseKey == null;
        final boolean geoChecksInvalid = geoChecksName == null;
        final boolean geoCrsWKTInvalid = geoCrsWKT == null;
        final boolean lonVarNameInvalid = lonVarName == null;
        final boolean latVarNameInvalid = latVarName == null;
        final boolean resolutionKmInvalid = resolutionKmStr == null;

        if (forwardInvalid) {
            SystemUtils.LOG.warning("Child element <" + TAG_FORWARD_CODING_KEY + "> expected in element <" + TAG_COMPONENT_GEO_CODING + ">.");
        }
        if (inverseInvalid) {
            SystemUtils.LOG.warning("Child element <" + TAG_INVERSE_CODING_KEY + "> expected in element <" + TAG_COMPONENT_GEO_CODING + ">.");
        }
        if (geoChecksInvalid) {
            SystemUtils.LOG.warning("Child element <" + TAG_GEO_CHECKS + "> expected in element <" + TAG_COMPONENT_GEO_CODING + ">.");
        }
        if (geoCrsWKTInvalid) {
            SystemUtils.LOG.warning("Child element <" + TAG_GEO_CRS + "> expected in element <" + TAG_COMPONENT_GEO_CODING + ">.");
        }
        if (lonVarNameInvalid) {
            SystemUtils.LOG.warning("Child element <" + TAG_LON_VARIABLE_NAME + "> expected in element <" + TAG_COMPONENT_GEO_CODING + ">.");
        }
        if (latVarNameInvalid) {
            SystemUtils.LOG.warning("Child element <" + TAG_LAT_VARIABLE_NAME + "> expected in element <" + TAG_COMPONENT_GEO_CODING + ">.");
        }
        if (resolutionKmInvalid) {
            SystemUtils.LOG.warning("Child element <" + TAG_RASTER_RESOLUTION_KM + "> expected in element <" + TAG_COMPONENT_GEO_CODING + ">.");
        }

        Double resolutionInKm = null;
        try {
            resolutionInKm = Double.parseDouble(resolutionKmStr);
        } catch (NumberFormatException e) {
            SystemUtils.LOG.warning(e.getMessage());
            SystemUtils.LOG.warning("The value of tag <" + TAG_RASTER_RESOLUTION_KM + "> is not a parseable text representation of a double value.");
        }

        boolean invalidValueGeoChecks = false;
        try {
            GeoChecks.valueOf(geoChecksName);
        } catch (IllegalArgumentException e) {
            invalidValueGeoChecks = true;
            SystemUtils.LOG.warning(e.getMessage());
            SystemUtils.LOG.warning("The value '" + geoChecksName + "' of tag <" + TAG_GEO_CHECKS + "> is not valid.");
        }

        CoordinateReferenceSystem geoCRS = null;
        try {
            geoCRS = CRS.parseWKT(geoCrsWKT);
        } catch (FactoryException e) {
            SystemUtils.LOG.warning(e.getMessage());
            SystemUtils.LOG.warning("The WKT value '" + geoCrsWKT + "' of tag <" + TAG_GEO_CRS + "> is not valid.");
        }

        if (forwardInvalid
                || inverseInvalid
                || geoChecksInvalid
                || lonVarNameInvalid
                || latVarNameInvalid
                || resolutionKmInvalid
                || resolutionInKm == null
                || geoChecksName == null
                || invalidValueGeoChecks
                || geoCRS == null) {
            SystemUtils.LOG.warning("Unable to create " + TAG_COMPONENT_GEO_CODING + ".");
            return null;
        }

        final RasterDataNode lonRaster = product.getRasterDataNode(lonVarName);
        final RasterDataNode latRaster = product.getRasterDataNode(latVarName);

        if (lonRaster == null || latRaster == null) {
            if (lonRaster == null) {
                SystemUtils.LOG.warning("Unable to find expected longitude raster '" + lonVarName + "' in product.");
            }
            if (latRaster == null) {
                SystemUtils.LOG.warning("Unable to find expected latitude raster '" + lonVarName + "' in product.");
            }
            SystemUtils.LOG.warning("Unable to create " + TAG_COMPONENT_GEO_CODING + ".");
            return null;
        }

        final GeoRaster geoRaster;
        if (lonRaster instanceof TiePointGrid) {
            final int sceneWidth = product.getSceneRasterWidth();
            final int sceneHeight = product.getSceneRasterHeight();
            final TiePointGrid lonTPG = (TiePointGrid) lonRaster;
            final TiePointGrid latTPG = (TiePointGrid) latRaster;

            final int gridWidth = lonTPG.getGridWidth();
            final int gridHeight = lonTPG.getGridHeight();

            final float[] lons = (float[]) lonTPG.getGridData().getElems();
            final double[] longitudes = IntStream.range(0, lons.length).mapToDouble(i -> lons[i]).toArray();

            final float[] lats = (float[]) latTPG.getGridData().getElems();
            final double[] latitudes = IntStream.range(0, lats.length).mapToDouble(i -> lats[i]).toArray();

            final double offsetX = lonTPG.getOffsetX();
            final double offsetY = lonTPG.getOffsetY();
            final double subsamplingX = lonTPG.getSubSamplingX();
            final double subsamplingY = lonTPG.getSubSamplingY();

            geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName, gridWidth, gridHeight,
                                      sceneWidth, sceneHeight, resolutionInKm,
                                      offsetX, offsetY, subsamplingX, subsamplingY);
        } else {
            final int rasterWidth = lonRaster.getRasterWidth();
            final int rasterHeight = lonRaster.getRasterHeight();
            final int size = rasterWidth * rasterHeight;
            final double[] longitudes = lonRaster.getSourceImage().getImage(0).getData()
                    .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);
            final double[] latitudes = latRaster.getSourceImage().getImage(0).getData()
                    .getPixels(0, 0, rasterWidth, rasterHeight, new double[size]);
            geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName, rasterWidth, rasterHeight,
                                      resolutionInKm);
        }

        final ForwardCoding forwardCoding = ComponentFactory.getForward(forwardKey);
        final InverseCoding inverseCoding = ComponentFactory.getInverse(inverseKey);
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.valueOf(geoChecksName), geoCRS);
        geoCoding.initialize();
        return geoCoding;
    }

    @Override
    public Element createXmlFromObject(Object object) {
        if (!(object instanceof ComponentGeoCoding)) {
            return null;
        }
        final ComponentGeoCoding geoCoding = (ComponentGeoCoding) object;
        final String forwardKey = geoCoding.getForwardCoding().getKey();
        final String inverseKey = geoCoding.getInverseCoding().getKey();
        final GeoChecks geoChecks = geoCoding.getGeoChecks();
        final CoordinateReferenceSystem geoCRS = geoCoding.getGeoCRS();
        final GeoRaster geoRaster = geoCoding.getGeoRaster();
        final String lonVarName = geoRaster.getLonVariableName();
        final String latVarName = geoRaster.getLatVariableName();
        final double resolutionKm = geoRaster.getRasterResolutionInKm();
        final double offsetX = geoRaster.getOffsetX();
        final double offsetY = geoRaster.getOffsetY();
        final double subsamplingX = geoRaster.getSubsamplingX();
        final double subsamplingY = geoRaster.getSubsamplingY();

        final Element codingMain = new Element(TAG_COMPONENT_GEO_CODING);
        final Element forwardKeyElem = new Element(TAG_FORWARD_CODING_KEY);
        final Element inverseKeyElem = new Element(TAG_INVERSE_CODING_KEY);
        final Element geoChecksElem = new Element(TAG_GEO_CHECKS);
        final Element geoCRSElem = new Element(TAG_GEO_CRS);
        final Element lonVarNameElem = new Element(TAG_LON_VARIABLE_NAME);
        final Element latVarNameElem = new Element(TAG_LAT_VARIABLE_NAME);
        final Element resolutionKmElem = new Element(TAG_RASTER_RESOLUTION_KM);
        final Element offsetXElem = new Element(TAG_OFFSET_X);
        final Element offsetYElem = new Element(TAG_OFFSET_Y);
        final Element subsamplingXElem = new Element(TAG_SUBSAMPLING_X);
        final Element subsamplingYElem = new Element(TAG_SUBSAMPLING_Y);

        codingMain.addContent(forwardKeyElem);
        codingMain.addContent(inverseKeyElem);
        codingMain.addContent(geoChecksElem);
        codingMain.addContent(geoCRSElem);
        codingMain.addContent(lonVarNameElem);
        codingMain.addContent(latVarNameElem);
        codingMain.addContent(resolutionKmElem);
        codingMain.addContent(offsetXElem);
        codingMain.addContent(offsetYElem);
        codingMain.addContent(subsamplingXElem);
        codingMain.addContent(subsamplingYElem);

        forwardKeyElem.setText(forwardKey);
        inverseKeyElem.setText(inverseKey);
        geoChecksElem.setText(geoChecks.name());
        geoCRSElem.setText(geoCRS.toWKT());
        lonVarNameElem.setText(lonVarName);
        latVarNameElem.setText(latVarName);
        resolutionKmElem.setText(String.valueOf(resolutionKm));
        offsetXElem.setText(String.valueOf(offsetX));
        offsetYElem.setText(String.valueOf(offsetY));
        subsamplingXElem.setText(String.valueOf(subsamplingX));
        subsamplingYElem.setText(String.valueOf(subsamplingY));

        return codingMain;
    }
}
