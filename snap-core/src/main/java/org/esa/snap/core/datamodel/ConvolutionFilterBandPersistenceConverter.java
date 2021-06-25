/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Attribute;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.util.StringUtils;
import org.jdom.Element;

import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.addAncillaryElements;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.addImageToModelTransformElement;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setAncillaryRelations;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setAncillaryVariables;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setImageToModelTransform;

public class ConvolutionFilterBandPersistenceConverter extends PersistenceConverter<ConvolutionFilterBand> {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 will be used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "ConvFilterBand:1";

    static final String ROOT_NAME_SPECTRAL_BAND_INFO = "Spectral_Band_Info";
    static final String PROP_NAME_BAND_INDEX = "BAND_INDEX";
    static final String PROP_NAME_BAND_NAME = "BAND_NAME";
    static final String PROP_NAME_BAND_DESCRIPTION = "BAND_DESCRIPTION";
    static final String PROP_NAME_DATA_TYPE = "DATA_TYPE";
    static final String PROP_NAME_PHYSICAL_UNIT = "PHYSICAL_UNIT";
    static final String PROP_NAME_SOLAR_FLUX = "SOLAR_FLUX";
    static final String PROP_NAME_BAND_WAVELEN = "BAND_WAVELEN";
    static final String PROP_NAME_BANDWIDTH = "BANDWIDTH";
    static final String PROP_NAME_SCALING_FACTOR = "SCALING_FACTOR";
    static final String PROP_NAME_SCALING_OFFSET = "SCALING_OFFSET";
    static final String PROP_NAME_LOG_10_SCALED = "LOG10_SCALED";
    static final String PROP_NAME_NO_DATA_VALUE_USED = "NO_DATA_VALUE_USED";
    static final String PROP_NAME_NO_DATA_VALUE = "NO_DATA_VALUE";

    static final String NAME_FILTER_BAND_INFO = "Filter_Band_Info";
    static final String PROP_NAME_BAND_TYPE = "bandType";
    static final String VALUE_CONVOLUTION_FILTER_BAND = "ConvolutionFilterBand";
    static final String PROP_NAME_FILTER_SOURCE = "FILTER_SOURCE";

    static final String NAME_FILTER_KERNEL = "Filter_Kernel";
    static final String PROP_NAME_KERNEL_WIDTH = "KERNEL_WIDTH";
    static final String PROP_NAME_KERNEL_HEIGHT = "KERNEL_HEIGHT";
    static final String PROP_NAME_KERNEL_X_ORIGIN = "KERNEL_X_ORIGIN";
    static final String PROP_NAME_KERNEL_Y_ORIGIN = "KERNEL_Y_ORIGIN";
    static final String PROP_NAME_KERNEL_FACTOR = "KERNEL_FACTOR";
    static final String PROP_NAME_KERNEL_DATA = "KERNEL_DATA";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public Item encode(ConvolutionFilterBand cfb) {
        final Container root = createRootContainer(ROOT_NAME_SPECTRAL_BAND_INFO);
        root.add(new Property<>(PROP_NAME_BAND_INDEX, String.valueOf(cfb.getProduct().getBandIndex(cfb.getName()))));
        root.add(new Property<>(PROP_NAME_BAND_NAME, cfb.getName()));
        root.add(new Property<>(PROP_NAME_BAND_DESCRIPTION, cfb.getDescription()));
        root.add(new Property<>(PROP_NAME_DATA_TYPE, ProductData.getTypeString(cfb.getDataType())));
        root.add(new Property<>(PROP_NAME_PHYSICAL_UNIT, cfb.getUnit()));
        root.add(new Property<>(PROP_NAME_SOLAR_FLUX, String.valueOf(cfb.getSolarFlux())));
        root.add(new Property<>(PROP_NAME_BAND_WAVELEN, String.valueOf(cfb.getSpectralWavelength())));
        root.add(new Property<>(PROP_NAME_BANDWIDTH, String.valueOf(cfb.getSpectralBandwidth())));
        root.add(new Property<>(PROP_NAME_SCALING_FACTOR, String.valueOf(cfb.getScalingFactor())));
        root.add(new Property<>(PROP_NAME_SCALING_OFFSET, String.valueOf(cfb.getScalingOffset())));
        root.add(new Property<>(PROP_NAME_LOG_10_SCALED, String.valueOf(cfb.isLog10Scaled())));
        root.add(new Property<>(PROP_NAME_NO_DATA_VALUE_USED, String.valueOf(cfb.isNoDataValueUsed())));
        root.add(new Property<>(PROP_NAME_NO_DATA_VALUE, String.valueOf(cfb.getNoDataValue())));

        final Container filterBandInfo = new Container(NAME_FILTER_BAND_INFO);
        root.add(filterBandInfo);
        filterBandInfo.add(new Property<>(PROP_NAME_BAND_TYPE, VALUE_CONVOLUTION_FILTER_BAND));
        filterBandInfo.add(new Property<>(PROP_NAME_FILTER_SOURCE, cfb.getSource().getName()));
        filterBandInfo.add(convertKernelToContainer(cfb.getKernel()));

        addAncillaryElements(root, cfb);
        addImageToModelTransformElement(root, cfb);
        return root;
    }

    @Override
    public ConvolutionFilterBand decodeImpl(Item item, Product product) {
        final Container root = item.asContainer();
        final Container filterInfo = root.getContainer(NAME_FILTER_BAND_INFO);
        final Container kernelInfo = filterInfo.getContainer(NAME_FILTER_KERNEL);
        final Kernel kernel = convertElementToKernel(kernelInfo);
        final String sourceName = filterInfo.getProperty(PROP_NAME_FILTER_SOURCE).getValueString();
        final String bandName = root.getProperty(PROP_NAME_BAND_NAME).getValueString();
        final RasterDataNode sourceNode = product.getRasterDataNode(sourceName);
        // todo - read iterationCount
        final ConvolutionFilterBand cfb = new ConvolutionFilterBand(bandName, sourceNode, kernel, 1);
        cfb.setDescription(root.getProperty(PROP_NAME_BAND_DESCRIPTION).getValueString());
        cfb.setUnit(root.getProperty(PROP_NAME_PHYSICAL_UNIT).getValueString());
        cfb.setSolarFlux(root.getProperty(PROP_NAME_SOLAR_FLUX).getValueFloat());
        cfb.setSpectralWavelength(root.getProperty(PROP_NAME_BAND_WAVELEN).getValueFloat());
        cfb.setSpectralBandwidth(root.getProperty(PROP_NAME_BANDWIDTH).getValueFloat());
        cfb.setScalingFactor(root.getProperty(PROP_NAME_SCALING_FACTOR).getValueDouble());
        cfb.setScalingOffset(root.getProperty(PROP_NAME_SCALING_OFFSET).getValueDouble());
        cfb.setLog10Scaled(root.getProperty(PROP_NAME_LOG_10_SCALED).getValueBoolean());
        cfb.setNoDataValueUsed(root.getProperty(PROP_NAME_NO_DATA_VALUE_USED).getValueBoolean());
        cfb.setNoDataValue(root.getProperty(PROP_NAME_NO_DATA_VALUE).getValueDouble());
        setAncillaryRelations(root, cfb);
        setAncillaryVariables(root, cfb, product);
        setImageToModelTransform(root, cfb);
        return cfb;
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {
        return new HistoricalDecoder[]{
                new HistoricalDecoder0()
        };
    }

    private static class HistoricalDecoder0 extends DimapHistoricalDecoder {

        @Override
        public boolean canDecode(Item item) {
            if (item != null) {
                final String itemName = item.getName();
                if (item.isContainer() && itemName.equals(ROOT_NAME_SPECTRAL_BAND_INFO)) {
                    final Container container = item.asContainer();
                    final Container filterBandInfoCont = container.getContainer(NAME_FILTER_BAND_INFO);
                    if (filterBandInfoCont != null) {
                        final Attribute<?> bandTypeAttr = filterBandInfoCont.getAttribute(PROP_NAME_BAND_TYPE);
                        if (bandTypeAttr != null) {
                            return bandTypeAttr.getValueString().equals(VALUE_CONVOLUTION_FILTER_BAND);
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public Item decode(Item item, Product product) {
            final Container rootContainer = item.asContainer();
            final Container filterBandCont = rootContainer.getContainer(NAME_FILTER_BAND_INFO);
            final Attribute<?> attribute = filterBandCont.removeAttribute(PROP_NAME_BAND_TYPE);
            filterBandCont.add(new Property<>(attribute.getName(), attribute.getValueString()));
            return rootContainer;
        }
    }

    static Kernel convertElementToKernel(Container kernelInfo) {
        final String kernelDataString = kernelInfo.getProperty(PROP_NAME_KERNEL_DATA).getValueString();
        final double[] data = StringUtils.toDoubleArray(kernelDataString, ",");

        int width = kernelInfo.getProperty(PROP_NAME_KERNEL_WIDTH).getValueInt();
        int height = kernelInfo.getProperty(PROP_NAME_KERNEL_HEIGHT).getValueInt();

        String xOriginText = kernelInfo.getProperty(PROP_NAME_KERNEL_X_ORIGIN).getValueString();
        int xOrigin = (width - 1) / 2;
        if (xOriginText != null) {
            xOrigin = Integer.parseInt(xOriginText);
        }

        String yOriginText = kernelInfo.getProperty(PROP_NAME_KERNEL_Y_ORIGIN).getValueString();
        int yOrigin = (height - 1) / 2;
        if (yOriginText != null) {
            yOrigin = Integer.parseInt(yOriginText);
        }

        String factorText = kernelInfo.getProperty(PROP_NAME_KERNEL_FACTOR).getValueString();
        double factor = 1;
        if (factorText != null) {
            factor = Double.parseDouble(factorText);
        }

        return new Kernel(width, height, xOrigin, yOrigin, factor, data);
    }

    static Container convertKernelToContainer(Kernel kernel) {
        final Container filterKernel = new Container(NAME_FILTER_KERNEL);
        filterKernel.add(new Property<>(PROP_NAME_KERNEL_WIDTH, String.valueOf(kernel.getWidth())));
        filterKernel.add(new Property<>(PROP_NAME_KERNEL_HEIGHT, String.valueOf(kernel.getHeight())));
        filterKernel.add(new Property<>(PROP_NAME_KERNEL_X_ORIGIN, String.valueOf(kernel.getXOrigin())));
        filterKernel.add(new Property<>(PROP_NAME_KERNEL_Y_ORIGIN, String.valueOf(kernel.getYOrigin())));
        filterKernel.add(new Property<>(PROP_NAME_KERNEL_FACTOR, String.valueOf(kernel.getFactor())));
        filterKernel.add(new Property<>(PROP_NAME_KERNEL_DATA, toCsv(kernel.getKernelData(null))));
        return filterKernel;
    }

    static String toCsv(double[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            double v = data[i];
            if (i > 0) {
                sb.append(',');
            }
            if (v == (int) v) {
                sb.append((int) v);
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }
}

