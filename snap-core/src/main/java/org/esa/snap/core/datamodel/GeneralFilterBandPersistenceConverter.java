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

//import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
//import org.esa.snap.core.dataio.persistence.Attribute;
import org.esa.snap.core.dataio.persistence.Container;
//import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;

import static org.esa.snap.core.datamodel.ConvolutionFilterBandPersistenceConverter.convertContainerToKernel;
import static org.esa.snap.core.datamodel.ConvolutionFilterBandPersistenceConverter.convertKernelToContainer;
import static org.esa.snap.core.datamodel.ConvolutionFilterBandPersistenceConverter.initRootContainer;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.addAncillaryElements;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.addImageToModelTransformElement;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setAncillaryRelations;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setAncillaryVariables;
import static org.esa.snap.core.datamodel.RasterDataNodePersistenceHelper.setImageToModelTransform;

/**
 * Caution!
 * This PersistenceConverter does not support historical dimap variations of GeneralFilterBand.
 */
public class GeneralFilterBandPersistenceConverter extends PersistenceConverter<GeneralFilterBand> {

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 will be used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "GenFilterBand:1";

    public static final String ROOT_NAME_SPECTRAL_BAND_INFO = "Spectral_Band_Info";
    public static final String PROP_NAME_BAND_NAME = "BAND_NAME";
    static final String PROP_NAME_BAND_INDEX = "BAND_INDEX";
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
    static final String VALUE_GENERAL_FILTER_BAND_TYPE = "GeneralFilterBand";

    static final String PROP_NAME_FILTER_SOURCE = "FILTER_SOURCE";
    static final String PROP_NAME_FILTER_OP_TYPE = "FILTER_OP_TYPE";
    static final String PROP_NAME_FILTER_ITERATION_COUNT = "ITERATION_COUNT";

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
    public Item encode(GeneralFilterBand gfb) {
        final Container root = createRootContainer(ROOT_NAME_SPECTRAL_BAND_INFO);
        initRootContainer(gfb, root);

        final Container filterBandInfo = new Container(NAME_FILTER_BAND_INFO);
        root.add(filterBandInfo);
        filterBandInfo.add(new Property<>(PROP_NAME_BAND_TYPE, VALUE_GENERAL_FILTER_BAND_TYPE));
        filterBandInfo.add(new Property<>(PROP_NAME_FILTER_SOURCE, gfb.getSource().getName()));
        filterBandInfo.add(new Property<>(PROP_NAME_FILTER_OP_TYPE, gfb.getOpType().toString()));
        filterBandInfo.add(new Property<>(PROP_NAME_FILTER_ITERATION_COUNT, gfb.getIterationCount()));
        filterBandInfo.add(convertKernelToContainer(gfb.getStructuringElement()));

        addAncillaryElements(root, gfb);
        addImageToModelTransformElement(root, gfb);
        return root;
    }

    @Override
    public GeneralFilterBand decodeImpl(Item item, Product product) {
        final Container root = item.asContainer();
        final Container filterInfo = root.getContainer(NAME_FILTER_BAND_INFO);
        final Container kernelInfo = filterInfo.getContainer(NAME_FILTER_KERNEL);
        final Kernel kernel = convertContainerToKernel(kernelInfo);
        final String sourceName = filterInfo.getProperty(PROP_NAME_FILTER_SOURCE).getValueString();
        final String opType = filterInfo.getProperty(PROP_NAME_FILTER_OP_TYPE).getValueString();
        final int iterationCount = filterInfo.getProperty(PROP_NAME_FILTER_ITERATION_COUNT).getValueInt();
        final String bandName = root.getProperty(PROP_NAME_BAND_NAME).getValueString();
        final RasterDataNode sourceNode = product.getRasterDataNode(sourceName);

        final GeneralFilterBand gfb = new GeneralFilterBand(
                bandName, sourceNode, GeneralFilterBand.OpType.valueOf(opType), kernel, iterationCount);

        gfb.setDescription(root.getProperty(PROP_NAME_BAND_DESCRIPTION).getValueString());
        gfb.setUnit(root.getProperty(PROP_NAME_PHYSICAL_UNIT).getValueString());
        gfb.setSolarFlux(root.getProperty(PROP_NAME_SOLAR_FLUX).getValueFloat());
        gfb.setSpectralWavelength(root.getProperty(PROP_NAME_BAND_WAVELEN).getValueFloat());
        gfb.setSpectralBandwidth(root.getProperty(PROP_NAME_BANDWIDTH).getValueFloat());
        gfb.setScalingFactor(root.getProperty(PROP_NAME_SCALING_FACTOR).getValueDouble());
        gfb.setScalingOffset(root.getProperty(PROP_NAME_SCALING_OFFSET).getValueDouble());
        gfb.setLog10Scaled(root.getProperty(PROP_NAME_LOG_10_SCALED).getValueBoolean());
        gfb.setNoDataValueUsed(root.getProperty(PROP_NAME_NO_DATA_VALUE_USED).getValueBoolean());
        gfb.setNoDataValue(root.getProperty(PROP_NAME_NO_DATA_VALUE).getValueDouble());
        setAncillaryRelations(root, gfb);
        setAncillaryVariables(root, gfb, product);
        setImageToModelTransform(root, gfb);
        return gfb;
    }

//    @Override
//    public HistoricalDecoder[] getHistoricalDecoders() {
//        return new HistoricalDecoder[]{
//                new HistoricalDecoder0()
//        };
//    }
//
//    private static class HistoricalDecoder0 extends DimapHistoricalDecoder {
//
//        @Override
//        public boolean canDecode(Item item) {
//            if (item != null) {
//                final String itemName = item.getName();
//                if (item.isContainer() && itemName.equals(ROOT_NAME_SPECTRAL_BAND_INFO)) {
//                    final Container container = item.asContainer();
//                    final Container filterBandInfoCont = container.getContainer(NAME_FILTER_BAND_INFO);
//                    if (filterBandInfoCont != null) {
//                        final Attribute<?> bandTypeAttr = filterBandInfoCont.getAttribute(PROP_NAME_BAND_TYPE);
//                        if (bandTypeAttr != null) {
//                            return bandTypeAttr.getValueString().equals(VALUE_GENERAL_FILTER_BAND_TYPE);
//                        }
//                    }
//                }
//            }
//            return false;
//        }
//
//        @Override
//        public Item decode(Item item, Product product) {
//            final Container rootContainer = item.asContainer();
//            final Container filterBandCont = rootContainer.getContainer(NAME_FILTER_BAND_INFO);
//            final Attribute<?> attribute = filterBandCont.removeAttribute(PROP_NAME_BAND_TYPE);
//            filterBandCont.add(new Property<>(attribute.getName(), attribute.getValueString()));
//            return rootContainer;
//        }
//    }
}

