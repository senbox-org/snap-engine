package org.esa.beam.dataio.s3;/*
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

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.SampleCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractProductFactory implements ProductFactory {

    private final List<Product> openProductList = new ArrayList<Product>();
    private final Sentinel3ProductReader productReader;
    private final Logger logger;

    private volatile Manifest manifest;

    public AbstractProductFactory(Sentinel3ProductReader productReader) {
        this.productReader = productReader;
        this.logger = Logger.getLogger(getClass().getSimpleName());
    }

    protected final Logger getLogger() {
        return logger;
    }

    protected static Band copyBand(Band sourceBand, Product targetProduct, boolean copySourceImage) {
        return ProductUtils.copyBand(sourceBand.getName(), sourceBand.getProduct(), targetProduct, copySourceImage);
    }

    protected static TiePointGrid copyBandAsTiePointGrid(Band sourceBand, Product targetProduct, int subSamplingX,
                                                         int subSamplingY,
                                                         float offsetX, float offsetY) {
        final RenderedImage sourceImage = sourceBand.getGeophysicalImage();
        final int w = sourceImage.getWidth();
        final int h = sourceImage.getHeight();
        final float[] tiePoints = sourceImage.getData().getSamples(0, 0, w, h, 0, new float[w * h]);

        final String unit = sourceBand.getUnit();
        final TiePointGrid tiePointGrid = new TiePointGrid(sourceBand.getName(), w, h,
                                                           offsetX,
                                                           offsetY,
                                                           subSamplingX,
                                                           subSamplingY,
                                                           tiePoints,
                                                           unit != null && unit.toLowerCase().contains("degree"));
        final String description = sourceBand.getDescription();
        tiePointGrid.setDescription(description);
        tiePointGrid.setGeophysicalNoDataValue(sourceBand.getGeophysicalNoDataValue());
        tiePointGrid.setUnit(unit);
        targetProduct.addTiePointGrid(tiePointGrid);

        return tiePointGrid;
    }

    @Override
    public final Product createProduct() throws IOException {
        manifest = createManifest(getInputFile());

        final List<String> fileNames = getFileNames(manifest);
        readProducts(fileNames);

        final String productName = getProductName();
        final String productType = productName.substring(0, 12);
        final Product masterProduct = findMasterProduct();
        final int w = getSceneRasterWidth(masterProduct);
        final int h = masterProduct.getSceneRasterHeight();
        final Product targetProduct = new Product(productName, productType, w, h, productReader);
        targetProduct.setFileLocation(getInputFile());
        targetProduct.setNumResolutionsMax(masterProduct.getNumResolutionsMax());

        if (masterProduct.getGeoCoding() instanceof CrsGeoCoding) {
            ProductUtils.copyGeoCoding(masterProduct, targetProduct);
        }
        targetProduct.getMetadataRoot().addElement(manifest.getMetadata());
        processProductSpecificMetadata(manifest.getMetadata().getElement("metadataSection"));
        for (final Product p : openProductList) {
            final MetadataElement productAttributes = new MetadataElement(p.getName());
            final MetadataElement datasetAttributes = new MetadataElement("Dataset_Attributes");
            final MetadataElement variableAttributes = new MetadataElement("Variable_Attributes");
            ProductUtils.copyMetadata(p.getMetadataRoot().getElement("Global_Attributes"), datasetAttributes);
            for (final MetadataElement element : p.getMetadataRoot().getElement("Variable_Attributes").getElements()) {
                variableAttributes.addElement(element.createDeepClone());
            }
            productAttributes.addElement(datasetAttributes);
            productAttributes.addElement(variableAttributes);
            targetProduct.getMetadataRoot().addElement(productAttributes);
        }

        addDataNodes(masterProduct, targetProduct);
        addSpecialVariables(masterProduct, targetProduct);
        setMasks(targetProduct);
        setTimes(targetProduct);
        if (targetProduct.getGeoCoding() == null) {
            setGeoCoding(targetProduct);
        }
        final Product[] sourceProducts = openProductList.toArray(new Product[openProductList.size()]);
        setAutoGrouping(sourceProducts, targetProduct);

        return targetProduct;
    }

    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
    }

    protected int getSceneRasterWidth(Product masterProduct) {
        return masterProduct.getSceneRasterWidth();
    }

    protected void addSpecialVariables(Product masterProduct, Product targetProduct) throws IOException {
    }

    protected Product findMasterProduct() {
        return openProductList.get(0);
    }

    protected final List<Product> getOpenProductList() {
        return Collections.unmodifiableList(openProductList);
    }

    protected void setMasks(Product targetProduct) {
        final Band[] bands = targetProduct.getBands();
        for (Band band : bands) {
            final SampleCoding sampleCoding = band.getSampleCoding();
            if (sampleCoding != null) {
                final String bandName = band.getName();
                final boolean flagBand = band.isFlagBand();
                for (int i = 0; i < sampleCoding.getNumAttributes(); i++) {
                    final String sampleName = sampleCoding.getSampleName(i);
                    final int sampleValue = sampleCoding.getSampleValue(i);
                    if (!"spare".equals(sampleName)) {
                        final String expression;
                        if (flagBand) {
                            expression = bandName + " & " + sampleValue + " == " + sampleValue;
                        } else {
                            expression = bandName + " == " + sampleValue;
                        }
                        final String maskName = bandName + "_" + sampleName;
                        targetProduct.addMask(maskName, expression, expression, Color.RED, 0.5);
                    }
                }
            }
        }
    }

    private void setTimes(Product targetProduct) {
        final Product sourceProduct = findMasterProduct();
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        if (targetProduct.getStartTime() == null) {
            targetProduct.setStartTime(manifest.getStartTime());
        }
        if (targetProduct.getEndTime() == null) {
            targetProduct.setEndTime(manifest.getStopTime());
        }
    }

    @Override
    public final void dispose() throws IOException {
        for (final Product product : openProductList) {
            product.dispose();
        }
        openProductList.clear();
    }

    private Band addBand(Band sourceBand, Product targetProduct) {
        return copyBand(sourceBand, targetProduct, true);
    }

    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        return null;
    }

    protected void setGeoCoding(Product targetProduct) throws IOException {
    }

    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
    }

    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        final StringBuilder patternBuilder = new StringBuilder();
        for (final Product sourceProduct : sourceProducts) {
            if (sourceProduct.getAutoGrouping() != null) {
                if (patternBuilder.length() > 0) {
                    patternBuilder.append(":");
                }
                patternBuilder.append(sourceProduct.getAutoGrouping());
            }
        }
        targetProduct.setAutoGrouping(patternBuilder.toString());
    }

    protected void addDataNodes(Product masterProduct, Product targetProduct) throws IOException {
        final int w = targetProduct.getSceneRasterWidth();
        final int h = targetProduct.getSceneRasterHeight();

        for (final Product sourceProduct : openProductList) {
            final Map<String, String> mapping = new HashMap<String, String>();
            for (final Band sourceBand : sourceProduct.getBands()) {
                final RasterDataNode targetNode;
                if (sourceBand.getSceneRasterWidth() == w && sourceBand.getSceneRasterHeight() == h) {
                    targetNode = addBand(sourceBand, targetProduct);
                } else {
                    targetNode = addSpecialNode(masterProduct, sourceBand, targetProduct);
                }
                if (targetNode != null) {
                    configureTargetNode(sourceBand, targetNode);
                    mapping.put(sourceBand.getName(), targetNode.getName());
                }
            }
            copyMasks(sourceProduct, targetProduct, mapping);
        }
    }

    protected final void copyMasks(Product sourceProduct, Product targetProduct, Map<String, String> mapping) {
        final ProductNodeGroup<Mask> maskGroup = prepareMasksForCopying(sourceProduct.getMaskGroup());
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            final Mask.ImageType imageType = mask.getImageType();
            if (imageType == Mask.BandMathsType.INSTANCE) {
                String name = mask.getName();
                if (!name.equals("spare")) {
                    String expression = Mask.BandMathsType.getExpression(mask);
                    for (final String sourceBandName : mapping.keySet()) {
                        if (expression.contains(sourceBandName)) {
                            final String targetBandName = mapping.get(sourceBandName);
                            if (!sourceBandName.equals(targetBandName)) {
                                name = name.replaceAll(sourceBandName, targetBandName);
                                expression = expression.replaceAll(sourceBandName, targetBandName);
                            }
                            final String description = sourceProduct.getDisplayName() + "." + mask.getDisplayName();
                            targetProduct.addMask(name, expression, description, mask.getImageColor(), mask.getImageTransparency());
                            break;
                        }
                    }
                }
            }
        }
    }

    //todo this method has been added as a workaround to deal with incorrect test data. Remove it when masks are correct
    protected ProductNodeGroup<Mask> prepareMasksForCopying(ProductNodeGroup<Mask> maskGroup) {
        return maskGroup;
    }

    private void readProducts(List<String> fileNames) throws IOException {
        for (final String fileName : fileNames) {
            readProduct(fileName);
        }
    }

    private Product readProduct(String fileName) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        final ProductReader reader = ProductIO.getProductReaderForInput(file);
        if (reader == null) {
            final String msg = MessageFormat.format("Cannot read file ''{0}''. No appropriate reader found.", fileName);
            logger.log(Level.SEVERE, msg);
            throw new IOException(msg);
        }

        final Product product = reader.readProductNodes(file, null);
        if (product == null) {
            final String msg = MessageFormat.format("Cannot read file ''{0}''.", fileName);
            logger.log(Level.SEVERE, msg);
            throw new IOException(msg);
        }
        // Todo remove when numResolutionsMax is assigned by ProductReader
        if (product.getNumBands() > 0) {
            product.setNumResolutionsMax(product.getBandAt(0).getSourceImage().getModel().getLevelCount());
        }
        openProductList.add(product);
        return product;
    }

    protected final File getInputFile() {
        return productReader.getInputFile();
    }

    protected final File getInputFileParentDirectory() {
        return productReader.getInputFileParentDirectory();
    }

    protected final String getProductName() {
        return FileUtils.getFilenameWithoutExtension(getInputFileParentDirectory());
    }

    protected abstract List<String> getFileNames(Manifest manifest);

    private Manifest createManifest(File file) throws IOException {
        final InputStream inputStream = new FileInputStream(file);
        try {
            final Document xmlDocument = createXmlDocument(inputStream);
            return XfduManifest.createManifest(xmlDocument);
        } finally {
            inputStream.close();
        }
    }

    private Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";

        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException e) {
            getLogger().log(Level.SEVERE, msg, e);
            throw new IOException(msg, e);
        } catch (ParserConfigurationException e) {
            getLogger().log(Level.SEVERE, msg, e);
            throw new IOException(msg, e);
        }
    }

}
