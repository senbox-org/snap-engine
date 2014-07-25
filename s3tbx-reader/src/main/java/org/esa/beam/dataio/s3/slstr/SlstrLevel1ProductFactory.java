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
import org.esa.beam.dataio.s3.Manifest;
import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.dataio.s3.SourceImageScaler;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlstrLevel1ProductFactory extends SlstrProductFactory {

    private final Map<String, String> gridTypeToGridIndex;
    private final Map<String, Integer> gridIndexToTrackOffset;
    private final Map<String, Integer> gridIndexToStartOffset;
    private Map<String, Float> nameToWavelengthMap;
    private Map<String, Float> nameToBandwidthMap;
    private Map<String, Integer> nameToIndexMap;

    public SlstrLevel1ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        gridTypeToGridIndex = new HashMap<String, String>();
        gridTypeToGridIndex.put("1 km", "i");
        gridTypeToGridIndex.put("0.5 km stripe A", "a");
        gridTypeToGridIndex.put("0.5 km stripe B", "b");
        gridTypeToGridIndex.put("0.5 km TDI", "c");
        gridTypeToGridIndex.put("Tie Points", "t");
        gridIndexToTrackOffset = new HashMap<String, Integer>();
        gridIndexToStartOffset = new HashMap<String, Integer>();
        nameToWavelengthMap = new HashMap<String, Float>();
        nameToBandwidthMap = new HashMap<String, Float>();
        nameToIndexMap = new HashMap<String, Integer>();
    }

    protected Integer getStartOffset(String gridIndex) {
        return gridIndexToStartOffset.get(gridIndex);
    }

    protected Integer getTrackOffset(String gridIndex) {
        return gridIndexToTrackOffset.get(gridIndex);
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        final MetadataElement slstrInformationElement = metadataElement.getElement("slstrProductInformation");
        final Product masterProduct = findMasterProduct();
        final int numberOfMasterColumns = masterProduct.getSceneRasterWidth();
        final int numberOfMasterRows = masterProduct.getSceneRasterHeight();
        for (int i = 0; i < slstrInformationElement.getNumElements(); i++) {
            final MetadataElement slstrElement = slstrInformationElement.getElementAt(i);
            final String slstrElementName = slstrElement.getName();
            if (slstrElementName.endsWith("ImageSize")) {
                if(slstrElement.containsAttribute("grid")) {
                    final String firstLetter =
                            gridTypeToGridIndex.get(slstrElement.getAttribute("grid").getData().getElemString());
                    String index;
//                if (firstLetter.equals("t")) {
//                    index = "tx";
//                } else {
                    if (slstrElementName.equals("nadirImageSize")) {
                        index = firstLetter + "n";
                    } else {
                        index = firstLetter + "o";
                    }
//                }
                    final int startOffset =
                            Integer.parseInt(slstrElement.getAttribute("startOffset").getData().getElemString());
                    final int trackOffset =
                            Integer.parseInt(slstrElement.getAttribute("trackOffset").getData().getElemString());
                    gridIndexToStartOffset.put(index, startOffset);
                    gridIndexToTrackOffset.put(index, trackOffset);
                    if (firstLetter.equals("t")) {
                        gridIndexToStartOffset.put("tx", startOffset);
                        gridIndexToTrackOffset.put("tx", trackOffset);
                    }
                    final int numberOfRows =
                            Integer.parseInt(slstrElement.getAttribute("rows").getData().getElemString());
                    final int numberOfColumns =
                            Integer.parseInt(slstrElement.getAttribute("columns").getData().getElemString());
                    if (numberOfColumns == numberOfMasterColumns && numberOfRows == numberOfMasterRows) {
//                        referenceStartOffset = startOffset;
                        setReferenceStartOffset(startOffset);
//                        referenceTrackOffset = trackOffset;
                        setReferenceTrackOffset(trackOffset);
//                        referenceResolutions = getResolutions(index);
                        setReferenceResolutions(getResolutions(index));
                    }
                }
            }
            if (slstrElementName.equals("bandDescriptions")) {
                for (int j = 0; j < slstrElement.getNumElements(); j++) {
                    final MetadataElement bandElement = slstrElement.getElementAt(j);
                    final String bandName = bandElement.getAttribute("name").getData().getElemString();
                    final float wavelength =
                            Float.parseFloat(bandElement.getAttribute("centralWavelength").getData().getElemString());
                    final float bandWidth =
                            Float.parseFloat(bandElement.getAttribute("bandWidth").getData().getElemString());
                    nameToWavelengthMap.put(bandName, wavelength);
                    nameToBandwidthMap.put(bandName, bandWidth);
                    nameToIndexMap.put(bandName, j);
                }
            }
        }
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        super.configureTargetNode(sourceBand, targetNode);
//        final String sourceBandName = sourceBand.getName();
//        final String sourceProductName = sourceBand.getProduct().getName();
//        if (sourceProductName.contains(sourceBandName)) {
//            targetNode.setName(sourceProductName);
//        } else {
//            targetNode.setName(sourceProductName + "_" + sourceBandName);
//        }
        final String sourceBandName = sourceBand.getName();
        final String sourceBandNameStart = sourceBandName.substring(0, 2);
        if (nameToWavelengthMap.containsKey(sourceBandNameStart)) {
            ((Band) targetNode).setSpectralWavelength(nameToWavelengthMap.get(sourceBandNameStart));
            ((Band) targetNode).setSpectralBandIndex(nameToIndexMap.get(sourceBandNameStart));
            ((Band) targetNode).setSpectralBandwidth(nameToBandwidthMap.get(sourceBandNameStart));
        }
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final File directory = getInputFileParentDirectory();

        final String[] fileNames = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nc") && (name.contains("radiance") || name.contains("flags")
                        || name.contains("geodetic_tx") || name.contains("BT") || name.contains("cartesian_tx")
                        || name.contains("indices") || name.contains("met")
                );
            }
        });


        return Arrays.asList(fileNames);
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
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        String bandGrouping = getAutoGroupingString(sourceProducts);
        String[] unwantedGroups = new String[]{"F1_BT", "F2_BT", "S1_radiance", "S2_radiance", "S3_radiance",
                "S4_radiance", "S5_radiance", "S6_radiance", "S7_BT", "S8_BT", "S9_BT"};
        for (String unwantedGroup : unwantedGroups) {
            if (bandGrouping.startsWith(unwantedGroup)) {
                bandGrouping = bandGrouping.replace(unwantedGroup + ":", "");
            } else if (bandGrouping.contains(unwantedGroup)) {
                bandGrouping = bandGrouping.replace(":" + unwantedGroup, "");
            }
        }
        StringBuilder patternGrouping = new StringBuilder("F*BT_in*:F*BT_io*:radiance_an:" +
                                                                  "radiance_ao:radiance_bn:" +
                                                                  "radiance_bo:radiance_cn:" +
                                                                  "radiance_co:S*BT_in*:" +
                                                                  "S*BT_io*:");
        patternGrouping.append(bandGrouping);
        targetProduct.setAutoGrouping(patternGrouping.toString());
    }
}
