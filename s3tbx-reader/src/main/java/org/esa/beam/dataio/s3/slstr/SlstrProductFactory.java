package org.esa.beam.dataio.s3.slstr;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.s3.AbstractProductFactory;
import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.dataio.s3.SourceImageScaler;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.IOException;

public abstract class SlstrProductFactory extends AbstractProductFactory {

    private double referenceStartOffset;
    private double referenceTrackOffset;
    private short[] referenceResolutions;

    protected SlstrProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        final int sourceBandNameLength = sourceBandName.length();
        String gridIndex = sourceBandName;
        if(sourceBandNameLength > 1) {
            gridIndex = sourceBandName.substring(sourceBandNameLength - 2);
        }
        final Integer sourceStartOffset = getStartOffset(gridIndex);
        final Integer sourceTrackOffset = getTrackOffset(gridIndex);
        if (sourceStartOffset != null && sourceTrackOffset != null) {
            final short[] sourceResolutions = getResolutions(gridIndex);
            if (gridIndex.startsWith("t")) {
                return copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);
            } else {
                final Band targetBand = copyBand(sourceBand, targetProduct, false);
                final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
                final RenderedImage sourceImage = createSourceImage(masterProduct, sourceBand, offsets, targetBand,
                                                                    sourceResolutions);
                targetBand.setSourceImage(sourceImage);
                return targetBand;
            }
        }
        return sourceBand;
    }

    protected abstract Integer getStartOffset(String gridIndex);

    protected abstract Integer getTrackOffset(String gridIndex);

    protected short[] getResolutions(String gridIndex) {
        short[] resolutions;
        if (gridIndex.startsWith("i")) {
            resolutions = new short[]{1000, 1000};
        } else if (gridIndex.startsWith("t")) {
            resolutions = new short[]{16000, 16000};
        } else {
            resolutions = new short[]{500, 500};
        }
        return resolutions;
    }

    protected void setReferenceStartOffset(int startOffset) {
        referenceStartOffset = startOffset;
    }

    protected void setReferenceTrackOffset(int trackOffset) {
        referenceTrackOffset = trackOffset;
    }

    protected void setReferenceResolutions(short[] resolutions) {
        referenceResolutions = resolutions;
    }

    private RenderedImage createSourceImage(Product masterProduct, Band sourceBand, float[] offsets,
                                            Band targetBand, short[] sourceResolutions) {
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(targetBand);
        final RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        renderingHints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                              BorderExtender.createInstance(
                                                      BorderExtender.BORDER_COPY)
        ));
        final MultiLevelImage sourceImage = sourceBand.getSourceImage();
        final float[] scalings = new float[]{
                ((float) sourceResolutions[0]) / referenceResolutions[0],
                ((float) sourceResolutions[1]) / referenceResolutions[1]
        };
        final MultiLevelImage masterImage = masterProduct.getBandAt(0).getSourceImage();
        return SourceImageScaler.scaleMultiLevelImage(masterImage, sourceImage, scalings, null, offsets, renderingHints,
                                                      targetBand.getNoDataValue(),
                                                      Interpolation.getInstance(Interpolation.INTERP_NEAREST));
    }

    protected float[] getOffsets(double sourceStartOffset, double sourceTrackOffset, short[] sourceResolutions) {
        float offsetX = (float) (referenceTrackOffset - sourceTrackOffset * (sourceResolutions[0] / referenceResolutions[0]));
        float offsetY = (float) (sourceStartOffset * (sourceResolutions[1] / referenceResolutions[1]) - referenceStartOffset);
        return new float[]{offsetX, offsetY};
    }

    @Deprecated // scale images instead
    private RasterDataNode copyTiePointGrid(Band sourceBand, Product targetProduct, double sourceStartOffset,
                                            double sourceTrackOffset, short[] sourceResolutions) {
        final int subSamplingX = sourceResolutions[0] / referenceResolutions[0];
        final int subSamplingY = sourceResolutions[1] / referenceResolutions[1];
        final float[] tiePointGridOffsets = getTiePointGridOffsets(sourceStartOffset, sourceTrackOffset,
                                                                   subSamplingX, subSamplingY, sourceResolutions);
        return copyBandAsTiePointGrid(sourceBand, targetProduct, subSamplingX, subSamplingY,
                                      tiePointGridOffsets[0], tiePointGridOffsets[1]);
    }

    protected float[] getTiePointGridOffsets(double sourceStartOffset, double sourceTrackOffset,
                                             int subSamplingX, int subSamplingY, short[] sourceResolutions) {
        float[] tiePointGridOffsets = new float[2];
        tiePointGridOffsets[0] = (float) (referenceTrackOffset - sourceTrackOffset * subSamplingX);
        tiePointGridOffsets[1] = (float) (sourceStartOffset * subSamplingY - referenceStartOffset);
        return tiePointGridOffsets;
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        final String sourceBandName = sourceBand.getName();
        final String sourceProductName = sourceBand.getProduct().getName();
        if (sourceProductName.contains(sourceBandName)) {
            targetNode.setName(sourceProductName);
        } else {
            targetNode.setName(sourceProductName + "_" + sourceBandName);
        }
//        final String targetNodeName = targetNode.getName();
//        final String targetNodeNameStart = targetNodeName.substring(0, 2);
//        if (nameToWavelengthMap.containsKey(targetNodeNameStart)) {
//            ((Band) targetNode).setSpectralWavelength(nameToWavelengthMap.get(targetNodeNameStart));
//            ((Band) targetNode).setSpectralBandIndex(nameToIndexMap.get(targetNodeNameStart));
//            ((Band) targetNode).setSpectralBandwidth(nameToBandwidthMap.get(targetNodeNameStart));
//        }
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        TiePointGrid latGrid = null;
        TiePointGrid lonGrid = null;
        for (final TiePointGrid grid : targetProduct.getTiePointGrids()) {
            if (latGrid == null && grid.getName().endsWith("latitude")) {
                latGrid = grid;
            }
            if (lonGrid == null && grid.getName().endsWith("longitude")) {
                lonGrid = grid;
            }
        }
        if (latGrid != null && lonGrid != null) {
            targetProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        }
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping(getAutoGroupingString(sourceProducts));
    }

    protected String getAutoGroupingString(Product[] sourceProducts) {
        final StringBuilder patternBuilder = new StringBuilder();
        for (final Product sourceProduct : sourceProducts) {
            final String sourceProductName = sourceProduct.getName();
            if (sourceProduct.getAutoGrouping() != null) {
                for (final String[] groups : sourceProduct.getAutoGrouping()) {
                    if (patternBuilder.length() > 0) {
                        patternBuilder.append(":");
                    }
                    patternBuilder.append(sourceProductName);
                    for (final String group : groups) {
                        patternBuilder.append("/");
                        patternBuilder.append(group);
                    }
                }
            }
            String patternName = sourceProductName;
            String[] unwantedPatternContents = new String[]{
                    "_an", "_ao", "_bn", "_bo", "_cn", "_co", "_in", "_io",
                    "_tn", "_to", "_tx"
            };
            for (String unwantedPatternContent : unwantedPatternContents) {
                if (sourceProductName.contains(unwantedPatternContent)) {
                    patternName = sourceProductName.substring(0, sourceProductName.lastIndexOf(unwantedPatternContent));
                    break;
                }
            }
            if (!patternBuilder.toString().contains(":" + patternName + ":") &&
                    !patternBuilder.toString().endsWith(":" + patternName)) {
                if (patternBuilder.length() > 0) {
                    patternBuilder.append(":");
                }
                patternBuilder.append(patternName);
            }
        }
        return patternBuilder.toString();
    }

}
