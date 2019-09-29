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
package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.DataNode;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ForLoop;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.DimKey;
import org.esa.snap.dataio.netcdf.util.NetcdfMultiLevelImage;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.units.ConversionException;
import ucar.units.PrefixDBException;
import ucar.units.SpecificationException;
import ucar.units.Unit;
import ucar.units.UnitDBException;
import ucar.units.UnitFormat;
import ucar.units.UnitFormatManager;
import ucar.units.UnitSystemException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CfBandPart extends ProfilePartIO {

    private static final DataTypeWorkarounds dataTypeWorkarounds = new DataTypeWorkarounds();
    private static final String NANO_METER = "nm";
    private static UnitFormat unitFormatManager = UnitFormatManager.instance();

    public static void readCfBandAttributes(Variable variable, RasterDataNode rasterDataNode) {
        rasterDataNode.setDescription(variable.getDescription());
        rasterDataNode.setUnit(variable.getUnitsString());

        rasterDataNode.setScalingFactor(getScalingFactor(variable));
        rasterDataNode.setScalingOffset(getAddOffset(variable));

        final Number noDataValue = getNoDataValue(variable);
        if (noDataValue != null) {
            rasterDataNode.setNoDataValue(noDataValue.doubleValue());
            rasterDataNode.setNoDataValueUsed(true);
        }
        if (rasterDataNode instanceof Band) {
            final Band band = (Band) rasterDataNode;
            band.setSpectralWavelength(getSpectralWavelength(variable));
        }
    }

    public static void writeCfBandAttributes(RasterDataNode rasterDataNode, NVariable variable) throws IOException {
        final String description = rasterDataNode.getDescription();
        if (description != null) {
            variable.addAttribute("long_name", description);
        }
        String unit = rasterDataNode.getUnit();
        if (unit != null) {
            unit = CfCompliantUnitMapper.tryFindUnitString(unit);
            variable.addAttribute("units", unit);
        }
        final boolean unsigned = isUnsigned(rasterDataNode);
        if (unsigned) {
            variable.addAttribute("_Unsigned", String.valueOf(true));
        }

        double noDataValue;
        if (!rasterDataNode.isLog10Scaled()) {
            final double scalingFactor = rasterDataNode.getScalingFactor();
            if (scalingFactor != 1.0) {
                variable.addAttribute(Constants.SCALE_FACTOR_ATT_NAME, scalingFactor);
            }
            final double scalingOffset = rasterDataNode.getScalingOffset();
            if (scalingOffset != 0.0) {
                variable.addAttribute(Constants.ADD_OFFSET_ATT_NAME, scalingOffset);
            }
            noDataValue = rasterDataNode.getNoDataValue();
        } else {
            // scaling information is not written anymore for log10 scaled bands
            // instead we always write geophysical values
            // we do this because log scaling is not supported by NetCDF-CF conventions
            noDataValue = rasterDataNode.getGeophysicalNoDataValue();
        }
        if (rasterDataNode.isNoDataValueUsed()) {
            Number fillValue = DataTypeUtils.convertTo(noDataValue, variable.getDataType());
            variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, fillValue);
        }
        variable.addAttribute("coordinates", "lat lon");
        if (rasterDataNode instanceof Band) {
            final Band band = (Band) rasterDataNode;
            final float spectralWavelength = band.getSpectralWavelength();
            if (spectralWavelength > 0) {
                variable.addAttribute(Constants.RADIATION_WAVELENGTH, spectralWavelength);
                variable.addAttribute(Constants.RADIATION_WAVELENGTH_UNIT, NANO_METER);
            }
        }
    }

    static void defineRasterDataNodes(ProfileWriteContext ctx, RasterDataNode[] rasterDataNodes) throws IOException {
        final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final String dimensions = ncFile.getDimensions();
        for (RasterDataNode rasterDataNode : rasterDataNodes) {
            String variableName = ReaderUtils.getVariableName(rasterDataNode);

            int dataType;
            if (rasterDataNode.isLog10Scaled()) {
                dataType = rasterDataNode.getGeophysicalDataType();
            } else {
                dataType = rasterDataNode.getDataType();
            }
            DataType netcdfDataType = DataTypeUtils.getNetcdfDataType(dataType);
            java.awt.Dimension tileSize = ImageManager.getPreferredTileSize(rasterDataNode.getProduct());
            final NVariable variable = ncFile.addVariable(variableName, netcdfDataType, tileSize, dimensions);
            writeCfBandAttributes(rasterDataNode, variable);
        }
    }

    @Override
    public void decode(final ProfileReadContext ctx, final Product p) throws IOException {
        for (final Variable variable : ctx.getRasterDigest().getRasterVariables()) {
            final List<Dimension> dimensions = variable.getDimensions();
            final int rank = dimensions.size();
            final String bandBasename = variable.getShortName();

            if (rank == 2) {
                addBand(ctx, p, variable, new int[]{}, bandBasename);
            } else {
                final int[] sizeArray = new int[rank - 2];
                final int startIndexToCopy = DimKey.findStartIndexOfBandVariables(dimensions);
                System.arraycopy(variable.getShape(), startIndexToCopy, sizeArray, 0, sizeArray.length);
                ForLoop.execute(sizeArray, (indexes, sizes) -> {
                    final StringBuilder bandNameBuilder = new StringBuilder(bandBasename);
                    for (int i = 0; i < sizes.length; i++) {
                        final Dimension zDim = dimensions.get(i + startIndexToCopy);
                        String zName = zDim.getShortName();
                        final String skipPrefix = "n_";
                        if (zName != null
                                && zName.toLowerCase().startsWith(skipPrefix)
                                && zName.length() > skipPrefix.length()) {
                            zName = zName.substring(skipPrefix.length());
                        }
                        if (zDim.getLength() > 1) {
                            if (zName != null) {
                                bandNameBuilder.append(String.format("_%s%d", zName, (indexes[i] + 1)));
                            } else {
                                bandNameBuilder.append(String.format("_%d", (indexes[i] + 1)));
                            }
                        }

                    }
                    addBand(ctx, p, variable, indexes, bandNameBuilder.toString());
                });
            }
        }
        p.setAutoGrouping(getAutoGrouping(ctx));
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        // In order to inform the writer that it shall write the geophysical values of log scaled bands
        // we set this property here.
        ctx.setProperty(Constants.CONVERT_LOGSCALED_BANDS_PROPERTY, true);
        defineRasterDataNodes(ctx, p.getBands());
    }

    private static void addBand(ProfileReadContext ctx, Product p, Variable variable, int[] origin,
                                String bandBasename) {
        final int rasterDataType = getRasterDataType(variable, dataTypeWorkarounds);
        if (variable.getDataType() == DataType.LONG) {
            final Band lowerBand = p.addBand(bandBasename + "_lsb", rasterDataType);
            readCfBandAttributes(variable, lowerBand);
            if (lowerBand.getDescription() != null) {
                lowerBand.setDescription(lowerBand.getDescription() + "(least significant bytes)");
            }
            lowerBand.setSourceImage(new NetcdfMultiLevelImage(lowerBand, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, lowerBand, variable, variable.getFullName() + "_lsb", false);

            final Band upperBand = p.addBand(bandBasename + "_msb", rasterDataType);
            readCfBandAttributes(variable, upperBand);
            if (upperBand.getDescription() != null) {
                upperBand.setDescription(upperBand.getDescription() + "(most significant bytes)");
            }
            upperBand.setSourceImage(new NetcdfMultiLevelImage(upperBand, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, upperBand, variable, variable.getFullName() + "_msb", true);
        } else {
            final Band band;
            if (variable.getGroup().isRoot()) {
                band = p.addBand(bandBasename, rasterDataType);
            } else {
                band = p.addBand(bandBasename + "_" + variable.getGroup().getName(), rasterDataType);
            }
            readCfBandAttributes(variable, band);
            band.setSourceImage(new NetcdfMultiLevelImage(band, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, band, variable, variable.getFullName(), false);
        }
    }

    private static double getScalingFactor(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.SCALE_FACTOR_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.SLOPE_ATT_NAME);
        }
        if (attribute == null) {
            attribute = variable.findAttribute("scaling_factor");
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 1.0;
    }

    private static double getAddOffset(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.ADD_OFFSET_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.INTERCEPT_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 0.0;
    }

    static float getSpectralWavelength(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.RADIATION_WAVELENGTH);
        if (attribute == null) {
            return 0;
        }
        final float value = getAttributeValue(attribute).floatValue();

        final Attribute attUnit = variable.findAttribute(Constants.RADIATION_WAVELENGTH_UNIT);
        if (attUnit == null) {
            return value;
        }
        final String unitStr = attUnit.getStringValue().trim();
        if (unitStr.equals(NANO_METER)) {
            return value;
        }
        try {
            final Unit sourceUnit = unitFormatManager.parse(unitStr);
            final Unit nanoMeter = unitFormatManager.parse(NANO_METER);
            if (sourceUnit.isCompatible(nanoMeter)) {
                return sourceUnit.convertTo(value, nanoMeter);
            }
        } catch (SpecificationException | UnitDBException | PrefixDBException | UnitSystemException | ConversionException e) {
            final Logger global = Logger.getGlobal();
            global.log(Level.WARNING, e.getMessage(), e);
        }
        return 0;
    }

    private static Number getNoDataValue(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.FILL_VALUE_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.MISSING_VALUE_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute);
        }
        return null;
    }

    private static Number getAttributeValue(Attribute attribute) {
        if (attribute.isString()) {
            String stringValue = attribute.getStringValue();
            if (stringValue.endsWith("b")) {
                // Special management for bytes; Can occur in e.g. ASCAT files from EUMETSAT
                return Byte.parseByte(stringValue.substring(0, stringValue.length() - 1));
            } else if (!stringValue.isEmpty()) {
                return Double.parseDouble(stringValue);
            } else {
                return 0;
            }
        } else {
            return attribute.getNumericValue();
        }

    }

    private static int getRasterDataType(Variable variable, DataTypeWorkarounds workarounds) {
        if (workarounds != null && workarounds.hasWorkaround(variable.getFullName(), variable.getDataType())) {
            return workarounds.getRasterDataType(variable.getFullName(), variable.getDataType());
        }
        int rasterDataType = DataTypeUtils.getRasterDataType(variable);
        if (variable.getDataType() == DataType.LONG) {
            rasterDataType = variable.isUnsigned() ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        }
        return rasterDataType;
    }

    private static boolean isUnsigned(DataNode dataNode) {
        return ProductData.isUIntType(dataNode.getDataType());
    }

    private static void addSampleCodingOrMasksIfApplicable(Product p, Band band, Variable variable,
                                                           String sampleCodingName,
                                                           boolean msb) {
        Attribute flagMeanings = variable.findAttribute("flag_meanings");
        if (flagMeanings == null) {
            flagMeanings = variable.findAttribute("flag_meaning");
        }
        if (flagMeanings == null) {
            return;
        }
        final Attribute flagMasks = variable.findAttribute("flag_masks");
        final Attribute flagValues = variable.findAttribute("flag_values");

        if (flagMasks != null) {
            if (!p.getFlagCodingGroup().contains(sampleCodingName)) {
                final FlagCoding flagCoding = new FlagCoding(sampleCodingName);
                if (flagValues != null) {
                    addSamples(flagCoding, flagMeanings, flagMasks, flagValues, msb);
                } else {
                    addSamples(flagCoding, flagMeanings, flagMasks, msb);
                }
                p.getFlagCodingGroup().add(flagCoding);
            }
            band.setSampleCoding(p.getFlagCodingGroup().get(sampleCodingName));
        } else if (flagValues != null) {
            if (!p.getIndexCodingGroup().contains(sampleCodingName)) {
                final IndexCoding indexCoding = new IndexCoding(sampleCodingName);
                addSamples(indexCoding, flagMeanings, flagValues, msb);
                p.getIndexCodingGroup().add(indexCoding);
            }
            band.setSampleCoding(p.getIndexCodingGroup().get(sampleCodingName));
        }
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleValues,
                                   boolean msb) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        String[] uniqueNames = StringUtils.makeStringsUnique(meanings);
        final int sampleCount = Math.min(uniqueNames.length, sampleValues.getLength());

        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = CfFlagCodingPart.replaceNonWordCharacters(uniqueNames[i]);
            switch (sampleValues.getDataType()) {
                case BYTE:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedByteToShort(
                                                   sampleValues.getNumericValue(i).byteValue()), null);
                    break;
                case SHORT:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedShortToInt(
                                                   sampleValues.getNumericValue(i).shortValue()), null);
                    break;
                case INT:
                    sampleCoding.addSample(sampleName, sampleValues.getNumericValue(i).intValue(), null);
                    break;
                case LONG:
                    final long sampleValue = sampleValues.getNumericValue(i).longValue();
                    if (msb) {
                        final long sampleValueMsb = sampleValue >>> 32;
                        if (sampleValueMsb > 0) {
                            sampleCoding.addSample(sampleName, (int) sampleValueMsb, null);
                        }
                    } else {
                        final long sampleValueLsb = sampleValue & 0x00000000FFFFFFFFL;
                        if (sampleValueLsb > 0 || sampleValue == 0L) {
                            sampleCoding.addSample(sampleName, (int) sampleValueLsb, null);
                        }
                    }
                    break;
            }
        }
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleMasks,
                                   Attribute sampleValues, boolean msb) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        String[] uniqueNames = StringUtils.makeStringsUnique(meanings);
        final int sampleCount = Math.min(uniqueNames.length, sampleMasks.getLength());
        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = CfFlagCodingPart.replaceNonWordCharacters(uniqueNames[i]);
            switch (sampleMasks.getDataType()) {
                case BYTE:
                    int[] byteValues = {DataType.unsignedByteToShort(sampleMasks.getNumericValue(i).byteValue()),
                            DataType.unsignedByteToShort(sampleValues.getNumericValue(i).byteValue())};
                    if (byteValues[0] == byteValues[1]) {
                        sampleCoding.addSample(sampleName, byteValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, byteValues, null);
                    }
                    break;
                case SHORT:
                    int[] shortValues = {DataType.unsignedShortToInt(sampleMasks.getNumericValue(i).shortValue()),
                            DataType.unsignedShortToInt(sampleValues.getNumericValue(i).shortValue())};
                    if (shortValues[0] == shortValues[1]) {
                        sampleCoding.addSample(sampleName, shortValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, shortValues, null);
                    }
                    break;
                case INT:
                    int[] intValues = {sampleMasks.getNumericValue(i).intValue(),
                            sampleValues.getNumericValue(i).intValue()};
                    if (intValues[0] == intValues[1]) {
                        sampleCoding.addSample(sampleName, intValues[0], null);
                    } else {
                        sampleCoding.addSamples(sampleName, intValues, null);
                    }
                    break;
                case LONG:
                    long[] longValues = {sampleMasks.getNumericValue(i).longValue(),
                            sampleValues.getNumericValue(i).longValue()};
                    if (msb) {
                        int[] intLongValues =
                                {(int) (longValues[0] >>> 32), (int) (longValues[1] >>> 32)};
                        if (longValues[0] > 0) {
                            if (intLongValues[0] == intLongValues[1]) {
                                sampleCoding.addSample(sampleName, intLongValues[0], null);
                            } else {
                                sampleCoding.addSamples(sampleName, intLongValues, null);
                            }
                        }
                    } else {
                        int[] intLongValues =
                                {(int) (longValues[0] & 0x00000000FFFFFFFFL), (int) (longValues[1] & 0x00000000FFFFFFFFL)};
                        if (intLongValues[0] > 0 || longValues[0] == 0L) {
                            if (intLongValues[0] == intLongValues[1]) {
                                sampleCoding.addSample(sampleName, intLongValues[0], null);
                            } else {
                                sampleCoding.addSamples(sampleName, intLongValues, null);
                            }
                        }
                    }
                    break;
            }
        }
    }

    private static String[] getSampleMeanings(Attribute sampleMeanings) {
        final int sampleMeaningsCount = sampleMeanings.getLength();
        if (sampleMeaningsCount > 1) {
            // handle a common misunderstanding of CF conventions, where flag meanings are stored as array of strings
            final String[] strings = new String[sampleMeaningsCount];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = sampleMeanings.getStringValue(i);
            }
            return strings;
        }
        return sampleMeanings.getStringValue().split(" ");
    }

    private String getAutoGrouping(ProfileReadContext ctx) {
        ArrayList<String> bandNames = new ArrayList<>();
        for (final Variable variable : ctx.getRasterDigest().getRasterVariables()) {
            final List<Dimension> dimensions = variable.getDimensions();
            int rank = dimensions.size();
            for (int i = 0; i < rank - 2; i++) {
                Dimension dim = dimensions.get(i);
                if (dim.getLength() > 1) {
                    bandNames.add(variable.getFullName());
                    break;
                }
            }
        }
        return StringUtils.join(bandNames, ":");
    }
}
