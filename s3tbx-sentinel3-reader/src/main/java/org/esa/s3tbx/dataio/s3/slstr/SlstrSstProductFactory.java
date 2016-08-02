package org.esa.s3tbx.dataio.s3.slstr;/*
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

import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.operation.transform.AffineTransform2D;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlstrSstProductFactory extends SlstrProductFactory {

    public final static String SLSTR_L2_SST_USE_PIXELGEOCODINGS = "s3tbx.reader.slstrl2sst.pixelGeoCodings";

    private Map<String, GeoCoding> geoCodingMap;
    private Double nadirStartOffset;
    private Double nadirTrackOffset;
    private Double obliqueStartOffset;
    private Double obliqueTrackOffset;

    public SlstrSstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        geoCodingMap = new HashMap<>();
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(new String[0]);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        final String sourceBandName = sourceBand.getName();
        if (sourceBand.getProduct().getName().contains("SST")) {
            targetNode.setName(sourceBand.getProduct().getName() + "_" + sourceBandName);
        }
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = productList.get(0);
        for (int i = 1; i < productList.size(); i++) {
            Product product = productList.get(i);
            if (product.getSceneRasterWidth() > masterProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() > masterProduct.getSceneRasterHeight() &&
                    !product.getName().contains("flags")) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        final MetadataElement slstrInformationElement = metadataElement.getElement("slstrProductInformation");
        for (int i = 0; i < slstrInformationElement.getNumElements(); i++) {
            final MetadataElement slstrElement = slstrInformationElement.getElementAt(i);
            final String slstrElementName = slstrElement.getName();
            if (slstrElementName.endsWith("ImageSize")) {
                final Double startOffset =
                        Double.parseDouble(slstrElement.getAttribute("startOffset").getData().getElemString());
                final Double trackOffset =
                        Double.parseDouble(slstrElement.getAttribute("trackOffset").getData().getElemString());
                if (slstrElementName.equals("nadirImageSize")) {
                    nadirStartOffset = startOffset;
                    nadirTrackOffset = trackOffset;
                    setReferenceStartOffset(startOffset);
                    setReferenceTrackOffset(trackOffset);
                    setReferenceResolutions(getResolutions("in"));
                } else {
                    obliqueStartOffset = startOffset;
                    obliqueTrackOffset = trackOffset;
                }
            }
        }
    }

    protected Double getStartOffset(String gridIndex) {
        if(gridIndex.endsWith("o")) {
            return obliqueStartOffset;
        }
        return nadirStartOffset;
    }

    protected Double getTrackOffset(String gridIndex) {
        if(gridIndex.endsWith("o")) {
            return obliqueTrackOffset;
        }
        return nadirTrackOffset;
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        final String sourceProductName = sourceBand.getProduct().getName();
        final String gridIndex = (sourceProductName).substring(sourceProductName.length() - 2);
        Double sourceStartOffset = getStartOffset(gridIndex);
        Double sourceTrackOffset = getTrackOffset(gridIndex);
        final short[] sourceResolutions = getResolutions(gridIndex);
        if (gridIndex.startsWith("t")) {
            final MetadataElement attributesElement =
                    sourceBand.getProduct().getMetadataRoot().getElement("Global_Attributes");
            //as these are not included in the manifest for tie point grids, we have to extract them from the product
            if (attributesElement != null) {
                final MetadataAttribute startOffsetAttribute = attributesElement.getAttribute("start_offset");
                if (startOffsetAttribute != null) {
                    sourceStartOffset = startOffsetAttribute.getData().getElemDouble();
                }
                final MetadataAttribute trackOffsetAttribute = attributesElement.getAttribute("track_offset");
                if (trackOffsetAttribute != null) {
                    sourceTrackOffset = trackOffsetAttribute.getData().getElemDouble();
                }
            }
        }
        if (sourceStartOffset == null || sourceTrackOffset == null) {
            return sourceBand;
        }
        if (gridIndex.startsWith("t")) {
            return copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);
        }
        final Band targetBand = new Band(sourceBandName, sourceBand.getDataType(),
                                         sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        targetProduct.addBand(targetBand);
        ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        final RenderedImage sourceRenderedImage = sourceBand.getSourceImage().getImage(0);
        //todo remove commented lines when resampling works with scenetransforms
        //if pixel band geo-codings are used, scenetransforms are set
//                if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L2_SST_USE_PIXELGEOCODINGS, false)) {
//                    targetBand.setSourceImage(sourceRenderedImage);
        final AffineTransform imageToModelTransform = new AffineTransform();
        final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
        imageToModelTransform.translate(offsets[0], offsets[1]);
        final short[] referenceResolutions = getReferenceResolutions();
        final int subSamplingX = sourceResolutions[0] / referenceResolutions[0];
        final int subSamplingY = sourceResolutions[1] / referenceResolutions[1];
        imageToModelTransform.scale(subSamplingX, subSamplingY);
        final DefaultMultiLevelModel targetModel =
                new DefaultMultiLevelModel(imageToModelTransform,
                                           sourceRenderedImage.getWidth(), sourceRenderedImage.getHeight());
        final DefaultMultiLevelSource targetMultiLevelSource =
                new DefaultMultiLevelSource(sourceRenderedImage, targetModel);
        targetBand.setSourceImage(new DefaultMultiLevelImage(targetMultiLevelSource));
//                }
        return targetBand;
    }

    @Override
    protected void setBandGeoCodings(Product product) throws IOException {
        if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L2_SST_USE_PIXELGEOCODINGS, false)) {
            setPixelBandGeoCodings(product);
        } else {
            setTiePointBandGeoCodings(product);
        }
    }

    private void setTiePointBandGeoCodings(Product product) throws IOException {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            setTiePointBandGeoCoding(product, band, getIdentifier(band.getName()));
        }
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            setTiePointBandGeoCoding(product, mask, getGridIndexFromMask(mask));
        }
    }

    private void setTiePointBandGeoCoding(Product product, Band band, String gridIndex) throws IOException {
        if (geoCodingMap.containsKey(gridIndex)) {
            band.setGeoCoding(geoCodingMap.get(gridIndex));
        } else {
            final TiePointGrid origLatGrid = product.getTiePointGrid("latitude_tx");
            final TiePointGrid origLonGrid = product.getTiePointGrid("longitude_tx");
            if (origLatGrid == null || origLonGrid == null) {
                return;
            }
            final short[] referenceResolutions = getReferenceResolutions();
            final short[] sourceResolutions = getResolutions(gridIndex);
            final Double sourceStartOffset = getStartOffset(gridIndex);
            final Double sourceTrackOffset = getTrackOffset(gridIndex);
            if (sourceStartOffset != null && sourceTrackOffset != null) {
                final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
                final float[] scalings = new float[]{
                        ((float) sourceResolutions[0]) / referenceResolutions[0],
                        ((float) sourceResolutions[1]) / referenceResolutions[1]
                };
                final AffineTransform transform = new AffineTransform();
                transform.translate(offsets[0], offsets[1]);
                transform.scale(scalings[0], scalings[1]);
                try {
                    final SlstrTiePointGeoCoding geoCoding =
                            new SlstrTiePointGeoCoding(origLatGrid, origLonGrid, new AffineTransform2D(transform));
                    band.setGeoCoding(geoCoding);
                    geoCodingMap.put(gridIndex, geoCoding);
                } catch (NoninvertibleTransformException e) {
                    throw new IOException("Could not create band-specific tie-point geo-coding. Try per-pixel geo-codings.");
                }
            }
        }
    }

    private void setPixelBandGeoCodings(Product product) {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final GeoCoding bandGeoCoding = getBandGeoCoding(product, getIdentifier(band.getName()));
            band.setGeoCoding(bandGeoCoding);
        }
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            final GeoCoding bandGeoCoding = getBandGeoCoding(product, getIdentifier(mask.getName()));
            mask.setGeoCoding(bandGeoCoding);
        }
    }

    private String getIdentifier(String bandName) {
        final String[] identifierCandidates = bandName.split("_");
        for (int i = identifierCandidates.length - 1; i >= 0; i--) {
            if (identifierCandidates[i].equals("io") || identifierCandidates[i].equals("in")) {
                return identifierCandidates[i];
            }
        }
        return "";
    }

    private GeoCoding getBandGeoCoding(Product product, String end) {
        if (geoCodingMap.containsKey(end)) {
            return geoCodingMap.get(end);
        } else {
            Band latBand = null;
            Band lonBand = null;
            switch (end) {
                case "in":
                    latBand = product.getBand("latitude_in");
                    lonBand = product.getBand("longitude_in");
                    break;
                case "io":
                    latBand = product.getBand("latitude_io");
                    lonBand = product.getBand("longitude_io");
                    break;
            }
            if (latBand != null && lonBand != null) {
                final BasicPixelGeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, "", 5);
                geoCodingMap.put(end, geoCoding);
                return geoCoding;
            }
        }
        return null;
    }

    private String getGridIndexFromMask(Mask mask) {
        final String maskName = mask.getName();
        if (maskName.contains("_in_")) {
            return "in";
        } else if (maskName.contains("_io_")) {
            return "io";
        }
        return "";
    }

}
