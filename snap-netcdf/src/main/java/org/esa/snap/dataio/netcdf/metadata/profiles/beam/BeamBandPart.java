/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import com.bc.ceres.multilevel.support.DefaultMultiLevelImage;
import eu.esa.snap.core.datamodel.group.BandGroup;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistable;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.*;
import org.geotools.referencing.CRS;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.esa.snap.core.dataio.Constants.GEOCODING;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistable.TAG_COMPONENT_GEO_CODING;

public class BeamBandPart extends ProfilePartIO {

    public static final String BANDWIDTH = "bandwidth";
    public static final String WAVELENGTH = "wavelength";
    public static final String VALID_PIXEL_EXPRESSION = "valid_pixel_expression";
    public static final String AUTO_GROUPING = "auto_grouping";
    public static final String QUICKLOOK_BAND_NAME = "quicklook_band_name";
    public static final String SOLAR_FLUX = "solar_flux";
    public static final String SPECTRAL_BAND_INDEX = "spectral_band_index";

    private static final int LON_INDEX = 0;
    private static final int LAT_INDEX = 1;

    private static void readBeamBandAttributes(Variable variable, Band band) {
        // todo se -- units for bandwidth and wavelength

        Attribute attribute = variable.findAttribute(BANDWIDTH);
        if (attribute != null) {
            band.setSpectralBandwidth(attribute.getNumericValue().floatValue());
        }
        attribute = variable.findAttribute(WAVELENGTH);
        if (attribute != null) {
            band.setSpectralWavelength(attribute.getNumericValue().floatValue());
        }
        attribute = variable.findAttribute(SPECTRAL_BAND_INDEX);
        if (attribute != null) {
            band.setSpectralBandIndex(attribute.getNumericValue().intValue());
        }
        attribute = variable.findAttribute(VALID_PIXEL_EXPRESSION);
        if (attribute != null) {
            band.setValidPixelExpression(attribute.getStringValue());
        }
        attribute = variable.findAttribute(SOLAR_FLUX);
        if (attribute != null) {
            band.setSolarFlux(attribute.getNumericValue().floatValue());
        }

        band.setName(ReaderUtils.getRasterName(variable));
    }

    private static void applySolarFluxFromMetadata(Band band, int spectralIndex) {
        MetadataElement metadataRoot = band.getProduct().getMetadataRoot();
        band.setSolarFlux(getSolarFluxFromMetadata(metadataRoot, spectralIndex));
    }

    private static float getSolarFluxFromMetadata(MetadataElement metadataRoot, int bandIndex) {
        if (metadataRoot != null) {
            MetadataElement scalingFactorGads = metadataRoot.getElement("Scaling_Factor_GADS");
            if (scalingFactorGads != null) {
                MetadataAttribute sunSpecFlux = scalingFactorGads.getAttribute("sun_spec_flux");
                ProductData data = sunSpecFlux.getData();
                if (data.getNumElems() > bandIndex) {
                    return data.getElemFloatAt(bandIndex);
                }
            }
        }
        return 0.0f;
    }

    public static void writeBeamBandAttributes(Band band, NVariable variable) throws IOException {
        // todo se -- units for bandwidth and wavelength

        final float spectralBandwidth = band.getSpectralBandwidth();
        if (spectralBandwidth > 0) {
            variable.addAttribute(BANDWIDTH, spectralBandwidth);
        }
        final float spectralWavelength = band.getSpectralWavelength();
        if (spectralWavelength > 0) {
            variable.addAttribute(WAVELENGTH, spectralWavelength);
        }
        final String validPixelExpression = band.getValidPixelExpression();
        if (validPixelExpression != null && validPixelExpression.trim().length() > 0) {
            variable.addAttribute(VALID_PIXEL_EXPRESSION, validPixelExpression);
        }
        final float solarFlux = band.getSolarFlux();
        if (solarFlux > 0) {
            variable.addAttribute(SOLAR_FLUX, solarFlux);
        }
        final float spectralBandIndex = band.getSpectralBandIndex();
        if (spectralBandIndex >= 0) {
            variable.addAttribute(SPECTRAL_BAND_INDEX, spectralBandIndex);
        }
        if (!band.getName().equals(variable.getName())) {
            variable.addAttribute(Constants.ORIG_NAME_ATT_NAME, band.getName());
        }
    }

    @Override
    public void preDecode(ProfileReadContext ctx, Product p) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final Attribute geoCodingAtt = netcdfFile.findGlobalAttribute(GEOCODING);
        if (geoCodingAtt != null) {
            final String xml = geoCodingAtt.getStringValue();
            if (xml.contains(TAG_COMPONENT_GEO_CODING)) {
                try {
                    final SAXBuilder saxBuilder = new SAXBuilder();
                    final String stripped = xml.replace("\n", "").replace("\r", "").replaceAll("> *<", "><");
                    final org.jdom2.Document build = saxBuilder.build(new StringReader(stripped));
                    final Element rootElement = build.getRootElement();

                    final ComponentGeoCodingPersistable pers = new ComponentGeoCodingPersistable();
                    final String[] geoVariableNames = pers.getGeoVariableNames(rootElement);
                    final Variable lonVariable = netcdfFile.findVariable(geoVariableNames[0]);
                    addBandToProduct(ctx, p, lonVariable);
                    final Variable latVariable = netcdfFile.findVariable(geoVariableNames[1]);
                    addBandToProduct(ctx, p, latVariable);
                } catch (JDOMException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        final NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final List<Variable> variables = netcdfFile.getVariables();
        for (Variable variable : variables) {
            UnsignedChecker.setUnsignedType(variable);
            final List<Dimension> dimensions = variable.getDimensions();
            if (dimensions.size() != 2) {
                continue;
            }
            addBandToProduct(ctx, p, variable);
        }
        // Work around for a bug in version 1.0.101
        // The solar flux and spectral band index were not preserved.
        // In order to overcome this bug, without having to rewrite the NetCDF files,
        // the following method was introduced at 16.06.2011.
        // The fix is mainly needed for the CoastColour project and only considers MERIS data.
        maybeApplySpectralIndexAndSolarFluxFromMetadata(p);
        Attribute autoGroupingAttribute = netcdfFile.findGlobalAttribute(AUTO_GROUPING);
        if (autoGroupingAttribute != null) {
            String autoGrouping = autoGroupingAttribute.getStringValue();
            if (autoGrouping != null) {
                p.setAutoGrouping(autoGrouping);
            }
        }
        Attribute quicklookBandNameAttribute = netcdfFile.findGlobalAttribute(QUICKLOOK_BAND_NAME);
        if (quicklookBandNameAttribute != null) {
            String quicklookBandName = quicklookBandNameAttribute.getStringValue();
            if (quicklookBandName != null) {
                p.setQuicklookBandName(quicklookBandName);
            }
        }
    }

    private void addBandToProduct(ProfileReadContext ctx, Product p, Variable variable) throws IOException {
        final int yDimIndex = 0;
        final int xDimIndex = 1;
        final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
        List<Dimension> dimensions = variable.getDimensions();
        final int width = dimensions.get(xDimIndex).getLength();
        final int height = dimensions.get(yDimIndex).getLength();
        Band band;
        String variableFullName = variable.getFullName();
        if (height == p.getSceneRasterHeight() && width == p.getSceneRasterWidth()) {
            if (p.containsBand(variableFullName)) {
                return;
            }
            band = p.addBand(variableFullName, rasterDataType);
        } else {
            if (dimensions.get(xDimIndex).getFullName().startsWith("tp_") ||
                    dimensions.get(yDimIndex).getFullName().startsWith("tp_")) {
                return;
            }
            band = new Band(variableFullName, rasterDataType, width, height);
            setGeoCoding(ctx, p, variable, band);
            p.addBand(band);
        }
        CfBandPart.readCfBandAttributes(variable, band);
        readBeamBandAttributes(variable, band);
        band.setSourceImage(new DefaultMultiLevelImage(new NetcdfMultiLevelSource(band, variable, ctx)));
    }

    /**
     * No public API !  Package visible for testing purposes only.
     */
    void setGeoCoding(ProfileReadContext ctx, Product p, Variable variable, Band band) throws IOException {
        final Attribute geoCodingAttribute = variable.findAttribute(GEOCODING);
        final NetcdfFile netcdfFile = ctx.getNetcdfFile();
        if (geoCodingAttribute != null) {
            final String geoCodingValue = geoCodingAttribute.getStringValue();
            final String expectedCRSName = "crs_" + variable.getFullName();
            if (geoCodingValue.equals(expectedCRSName)) {
                final Variable crsVariable = netcdfFile.getRootGroup().findVariable(expectedCRSName);
                if (crsVariable != null) {
                    final Attribute wktAtt = crsVariable.findAttribute("wkt");
                    final Attribute i2mAtt = crsVariable.findAttribute("i2m");
                    if (wktAtt != null && i2mAtt != null) {
                        band.setGeoCoding(createGeoCodingFromWKT(p, wktAtt.getStringValue(), i2mAtt.getStringValue()));
                    }
                }
            } else if (geoCodingValue.contains("<" + TAG_COMPONENT_GEO_CODING + ">")) {
                try {
                    final SAXBuilder saxBuilder = new SAXBuilder();
                    String xml = geoCodingValue.replace("\n", "").replace("\r", "").replaceAll("> *<", "><");
                    final org.jdom2.Document build = saxBuilder.build(new StringReader(xml));
                    final Element rootElement = build.getRootElement();
                    final Element parent = new Element("parent");
                    parent.addContent(rootElement.detach());
                    final ComponentGeoCodingPersistable pers = new ComponentGeoCodingPersistable();
                    final Object objectFromXml = pers.createObjectFromXml(parent, p);
                    if (objectFromXml instanceof GeoCoding) {
                        band.setGeoCoding((GeoCoding) objectFromXml);
                    }
                } catch (JDOMException | IOException e) {
                    SystemUtils.LOG.warning("Unable to instanciate ComponentGeoCoding for Band '" + band.getName() + "' from NetCDF.");
                    SystemUtils.LOG.warning(e.getMessage());
                }
            } else {
                final String[] tpGridNames = geoCodingValue.split(" ");
                if (tpGridNames.length == 2
                        && p.containsTiePointGrid(tpGridNames[LON_INDEX])
                        && p.containsTiePointGrid(tpGridNames[LAT_INDEX])) {
                    final TiePointGrid lon = p.getTiePointGrid(tpGridNames[LON_INDEX]);
                    final TiePointGrid lat = p.getTiePointGrid(tpGridNames[LAT_INDEX]);
                    band.setGeoCoding(new TiePointGeoCoding(lat, lon));
                }
            }
        }
    }

    private GeoCoding createGeoCodingFromWKT(Product p, String wktString, String i2mString) {
        try {
            CoordinateReferenceSystem crs = CRS.parseWKT(wktString);
            String[] parameters = StringUtils.csvToArray(i2mString);
            double[] matrix = new double[parameters.length];
            for (int i = 0; i < matrix.length; i++) {
                matrix[i] = Double.valueOf(parameters[i]);
            }
            AffineTransform i2m = new AffineTransform(matrix);
            Rectangle imageBounds = new Rectangle(p.getSceneRasterWidth(), p.getSceneRasterHeight());
            return new CrsGeoCoding(crs, imageBounds, i2m);
        } catch (FactoryException ignore) {
        } catch (TransformException ignore) {
        }
        return null;
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final String productDimensions = ncFile.getDimensions();
        final HashMap<String, String> dimMap = new HashMap<String, String>();
        for (Band band : p.getBands()) {
            if (isPixelGeoCodingBand(band)) {
                continue;
            }
            int dataType;
            if (band.isLog10Scaled()) {
                dataType = band.getGeophysicalDataType();
                // In order to inform the writer that it shall write the geophysical values of log-scaled bands
                // we set this property here.
                ctx.setProperty(Constants.CONVERT_LOGSCALED_BANDS_PROPERTY, true);
            } else {
                dataType = band.getDataType();
            }

            final DataType ncDataType = DataTypeUtils.getNetcdfDataType(dataType);
            String variableName = ReaderUtils.getVariableName(band);
            if (!ncFile.isNameValid(variableName)) {
                variableName = ncFile.makeNameValid(variableName);
            }
            NVariable variable;
            final int bandSceneRasterWidth = band.getRasterWidth();
            final int bandSceneRasterHeight = band.getRasterHeight();
            if (bandSceneRasterWidth != p.getSceneRasterWidth() || bandSceneRasterHeight != p.getSceneRasterHeight()) {
                final String key = bandSceneRasterWidth + " " + bandSceneRasterHeight;
                String dimString = dimMap.get(key);
                if (dimString == null) {
                    final int size = dimMap.size();
                    final String suffix = "" + (size + 1);
                    ncFile.addDimension("y" + suffix, bandSceneRasterHeight);
                    ncFile.addDimension("x" + suffix, bandSceneRasterWidth);
                    dimString = "y" + suffix + " " + "x" + suffix;
                    dimMap.put(key, dimString);
                }
                final java.awt.Dimension tileSize = JAIUtils.computePreferredTileSize(bandSceneRasterWidth, bandSceneRasterHeight, 1);
                variable = ncFile.addVariable(variableName, ncDataType, ncDataType.isUnsigned(), tileSize, dimString);
                encodeGeoCoding(ncFile, band, p, variable);
            } else {
                final java.awt.Dimension tileSize = ImageManager.getPreferredTileSize(p);
                variable = ncFile.addVariable(variableName, ncDataType, ncDataType.isUnsigned(), tileSize, productDimensions);
            }
            CfBandPart.writeCfBandAttributes(band, variable);
            writeBeamBandAttributes(band, variable);
        }
        BandGroup autoGrouping = p.getAutoGrouping();
        if (autoGrouping != null) {
            ncFile.addGlobalAttribute(AUTO_GROUPING, autoGrouping.toString());
        }
        String quicklookBandName = p.getQuicklookBandName();
        if (quicklookBandName != null && !quicklookBandName.isEmpty()) {
            ncFile.addGlobalAttribute(QUICKLOOK_BAND_NAME, quicklookBandName);
        }
    }

    /**
     * No public API !  Package visible for testing purposes only.
     */
    void encodeGeoCoding(NFileWriteable ncFile, Band band, Product product, NVariable variable) throws IOException {
        final GeoCoding geoCoding = band.getGeoCoding();
        if (!geoCoding.equals(product.getSceneGeoCoding())) {
            if (geoCoding instanceof ComponentGeoCoding) {
                final ComponentGeoCodingPersistable persistable = new ComponentGeoCodingPersistable();
                final Element xmlFromObject = persistable.createXmlFromObject(geoCoding);
                final String value = StringUtils.toXMLString(xmlFromObject);
                variable.addAttribute(GEOCODING, value);
            } else if (geoCoding instanceof TiePointGeoCoding tpGC) {
                final String[] names = new String[2];
                names[LON_INDEX] = tpGC.getLonGrid().getName();
                names[LAT_INDEX] = tpGC.getLatGrid().getName();
                final String value = StringUtils.arrayToString(names, " ");
                variable.addAttribute(GEOCODING, value);
            } else {
                if (geoCoding instanceof CrsGeoCoding) {
                    final CoordinateReferenceSystem crs = geoCoding.getMapCRS();
                    final double[] matrix = new double[6];
                    final MathTransform transform = geoCoding.getImageToMapTransform();
                    if (transform instanceof AffineTransform) {
                        ((AffineTransform) transform).getMatrix(matrix);
                    }
                    final String crsName = "crs_" + band.getName();
                    final NVariable crsVariable = ncFile.addScalarVariable(crsName, DataType.INT);
                    crsVariable.addAttribute("wkt", crs.toWKT());
                    crsVariable.addAttribute("i2m", StringUtils.arrayToCsv(matrix));
                    variable.addAttribute(GEOCODING, crsName);
                }
            }
        }
    }

    private boolean isPixelGeoCodingBand(Band band) {
        final GeoCoding geoCoding = band.getGeoCoding();
        if (geoCoding instanceof BasicPixelGeoCoding pixelGeoCoding) {
            return pixelGeoCoding.getLatBand() == band || pixelGeoCoding.getLonBand() == band;
        }
        return false;

    }

    private void maybeApplySpectralIndexAndSolarFluxFromMetadata(Product p) {
        Band[] bands = p.getBands();
        int spectralIndex = 0;
        for (Band band : bands) {
            boolean isSpectralBand = band.getSpectralWavelength() != 0.0f;
            boolean isSpectralBandIndexSet = band.getSpectralBandIndex() != -1;
            if (isSpectralBand && !isSpectralBandIndexSet) {
                band.setSpectralBandIndex(spectralIndex);
                boolean isSolarFluxSet = band.getSolarFlux() != 0.0f;
                if (!isSolarFluxSet) {
                    applySolarFluxFromMetadata(band, spectralIndex);
                }
                spectralIndex++;
            }
        }
    }
}
