/*
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

package org.esa.snap.collocation;

import com.bc.ceres.binding.Property;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import eu.esa.snap.core.datamodel.group.BandGroup;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.text.MessageFormat.format;

/**
 * This operator is used to spatially collocate two data products. It requires two source products,
 * a {@code master} product which provides the Coordinate reference system and grid into which
 * the raster data sets of the {@code slave} product are resampled to.
 *
 * @author Ralf Quast
 * @author Norman Fomferra
 * @since BEAM 4.1
 */
@OperatorMetadata(alias = "Collocate",
        category = "Raster/Geometric",
        version = "1.4",
        authors = "Ralf Quast, Norman Fomferra, Tom Block",
        copyright = "(c) 2007-2024 by Brockmann Consult",
        description = "Collocates two products based on their geo-codings.")
public class CollocateOp extends Operator {

    public static final String SOURCE_NAME_REFERENCE = "${ORIGINAL_NAME}";
    public static final String SECONDARY_NUMBER_ID_REFERENCE = "${SLAVE_NUMBER_ID}";
    public static final String DEFAULT_REFERENCE_COMPONENT_PATTERN = "${ORIGINAL_NAME}_M";
    public static final String DEFAULT_SECONDARY_COMPONENT_PATTERN = "${ORIGINAL_NAME}_S${SLAVE_NUMBER_ID}";
    private static final String NEAREST_NEIGHBOUR = "NEAREST_NEIGHBOUR";


    @SourceProducts(alias = "sourceProducts", description = "The source product(s) which serve(s) as secondary.")
    private Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products")
    String[] sourceProductPaths;

    @SourceProduct(alias = "reference", description = "The source product which serves as reference.", optional = true)
    private Product reference;

    @SourceProduct(alias = "secondary", description = "The source product which serves as secondary.", optional = true)
    private Product secondary;

    @Parameter(alias = "referenceProductName", label = "Reference product name", description = "The name of the reference product.")
    String referenceProductName;

    private Product[] secondaryProducts;
    private Product referenceProduct;

    @TargetProduct(description = "The target product which will use the reference's grid.")
    private Product targetProduct;

    @Parameter(defaultValue = "_collocated",
            description = "The name of the target product")
    @Deprecated
    private String targetProductName;

    @Parameter(defaultValue = "COLLOCATED",
            description = "The product type string for the target product (informal)")
    private String targetProductType;

    @Parameter(defaultValue = "false", label = "Copy metadata of secondary products",
            description = "Copies also the metadata of the secondary products to the target.")
    private boolean copySecondaryMetadata;

    @Parameter(defaultValue = "true",
            description = "Whether or not components of the reference product shall be renamed in the target product.")
    private boolean renameReferenceComponents;

    @Parameter(defaultValue = "true",
            description = "Whether or not components of the secondary product(s) shall be renamed in the target product.")
    private boolean renameSecondaryComponents;

    @Parameter(defaultValue = DEFAULT_REFERENCE_COMPONENT_PATTERN,
            description = "The text pattern to be used when renaming reference components.")
    private String referenceComponentPattern;

    @Parameter(defaultValue = DEFAULT_SECONDARY_COMPONENT_PATTERN,
            description = "The text pattern to be used when renaming secondary components.")
    private String secondaryComponentPattern;

    @Parameter(defaultValue = NEAREST_NEIGHBOUR,
            description = "The method to be used when resampling the secondary grid onto the reference grid.")
    private ResamplingType resamplingType;

    // maps target bands to source bands or tie-point grids
    private Map<Band, RasterDataNode> sourceRasterMap;
    private Band[] collocationFlagBands;

    ArrayList<Product> disposableProducts = new ArrayList<>();

    public void setReferenceProduct(Product referenceProduct) {
        reference = referenceProduct;
        referenceProductName = referenceProduct.getName();
    }

    public Product getSecondary() {
        return secondary;
    }

    public void setSecondary(Product secondary) {
        this.secondary = secondary;
    }

    public String getTargetProductType() {
        return targetProductType;
    }

    public void setTargetProductType(String targetProductType) {
        this.targetProductType = targetProductType;
    }

    public void setCopySecondaryMetadata(boolean copySecondaryMetadata) {
        this.copySecondaryMetadata = copySecondaryMetadata;
    }

    public boolean getRenameReferenceComponents() {
        return renameReferenceComponents;
    }

    public void setRenameReferenceComponents(boolean renameReferenceComponents) {
        this.renameReferenceComponents = renameReferenceComponents;
    }

    public boolean getRenameSecondaryComponents() {
        return renameSecondaryComponents;
    }

    public void setRenameSecondaryComponents(boolean renameSecondaryComponents) {
        this.renameSecondaryComponents = renameSecondaryComponents;
    }

    public String getReferenceComponentPattern() {
        return referenceComponentPattern;
    }

    public void setReferenceComponentPattern(String referenceComponentPattern) {
        this.referenceComponentPattern = referenceComponentPattern;
    }

    public String getSecondaryComponentPattern() {
        return secondaryComponentPattern;
    }

    public void setSecondaryComponentPattern(String secondaryComponentPattern) {
        this.secondaryComponentPattern = secondaryComponentPattern;
    }

    public ResamplingType getResamplingType() {
        return resamplingType;
    }

    public void setResamplingType(ResamplingType resamplingType) {
        this.resamplingType = resamplingType;
    }

    @Override
    public void initialize() throws OperatorException {

        //for compatibility, allow have slave and master instead of sourceProducts
        //The first step is to copy them to sourceProducts
        if (reference != null) {
            if ((referenceProductName != null && referenceProductName.length() > 0) && !referenceProductName.equals(reference.getName())) {
                throw new OperatorException("Incompatible master definition");
            }
            referenceProductName = reference.getName();
            addToSourceProducts(reference);
        }
        if (secondary != null) {
            addToSourceProducts(secondary);
        }

        int sourcePathCount = 0;
        if (sourceProductPaths != null) {
            sourcePathCount = sourceProductPaths.length;
        }

        //sourceProducts and sourcePAths must be at least two
        if (sourceProducts.length + sourcePathCount < 2) {
            throw new OperatorException("At least two source products have to be defined");
        }

        //Define master and slave products from sourceProducts and sourceProductPaths
        ArrayList<Product> slaveProductList = new ArrayList<>();
        boolean found = false;
        for (Product product : sourceProducts) {
            if (product.getName().equals(referenceProductName) && !found) {
                referenceProduct = product;
                found = true;
            } else {
                slaveProductList.add(product);
            }
        }

        if (sourceProductPaths != null) {
            for (String sourceProductPath : sourceProductPaths) {
                File file = new File(sourceProductPath);
                try {
                    Product sourceProduct = ProductIO.readProduct(file);
                    if (sourceProduct.getName().equals(referenceProductName) && referenceProduct == null) {
                        referenceProduct = sourceProduct;
                    } else {
                        slaveProductList.add(sourceProduct);
                        disposableProducts.add(sourceProduct);
                    }
                } catch (IOException e) {
                    String msgPattern = "Failed to read file '%s'. %s: %s";
                    getLogger().severe(String.format(msgPattern, file, e.getClass().getSimpleName(), e.getMessage()));
                }
            }
        }

        //Set the master product from the source product if referenceProductName has not been defined: The first single sized product.
        if (referenceProduct == null && (referenceProductName == null || referenceProductName.length() == 0)) {
            //no master product, so the first one single size will be selected
            for (Product product : slaveProductList) {
                if (!product.isMultiSize()) {
                    getLogger().warning(String.format("Reference product selected automatically: %s", product.getName()));
                    referenceProduct = product;
                    slaveProductList.remove(product);
                    break;
                }
            }
        }

        //If no master product Operator Exception
        if (referenceProduct == null) {
            throw new OperatorException("Reference product not found in sourceProducts");
        }

        secondaryProducts = slaveProductList.toArray(new Product[slaveProductList.size()]);
        validateProduct(referenceProduct);

        //for slave, we only need geoCoding. It is not needed singleSize anymore.
        for (Product slaveProduct : secondaryProducts) {
            ensureSceneGeoCoding(slaveProduct);
        }

        if (renameReferenceComponents && StringUtils.isNullOrEmpty(referenceComponentPattern)) {
            throw new OperatorException(format("Parameter ''{0}'' must be set to a non-empty string pattern.", "referenceComponentPattern"));
        }
        if (renameSecondaryComponents && StringUtils.isNullOrEmpty(secondaryComponentPattern)) {
            throw new OperatorException(format("Parameter ''{0}'' must be set to a non-empty string pattern.", "secondaryComponentPattern"));
        }

        Map<String, String> originalMasterNames = new HashMap<>(31);
        Map<String, String> originalSlaveNames = new HashMap<>(31);
        List<RasterDataNode> masterRasters = new ArrayList<>(32);
        List<RasterDataNode> slaveRasters = new ArrayList<>(32);
        sourceRasterMap = new HashMap<>(31);

        String autoTargetProductName = referenceProduct.getName();
        if (targetProductName == null) {
            if (secondaryProducts.length == 1) {
                autoTargetProductName = autoTargetProductName + "_" + secondaryProducts[0].getName();
            } else {
                autoTargetProductName = autoTargetProductName + "_collocated";
            }
        }

        targetProduct = new Product(
                targetProductName != null ? targetProductName : autoTargetProductName,
                targetProductType != null ? targetProductType : referenceProduct.getProductType(),
                referenceProduct.getSceneRasterWidth(),
                referenceProduct.getSceneRasterHeight());

        final ProductData.UTC utc1 = referenceProduct.getStartTime();
        if (utc1 != null) {
            targetProduct.setStartTime(new ProductData.UTC(utc1.getMJD()));
        }
        final ProductData.UTC utc2 = referenceProduct.getEndTime();
        if (utc2 != null) {
            targetProduct.setEndTime(new ProductData.UTC(utc2.getMJD()));
        }

        if (copySecondaryMetadata) {
            final MetadataElement secondaryTargetMetadata = new MetadataElement("SecondaryMetadata");
            targetProduct.getMetadataRoot().addElement(secondaryTargetMetadata);
            for (Product secProduct : secondaryProducts) {
                final MetadataElement currentMetadata = new MetadataElement(secProduct.getName());
                final MetadataElement metadataRoot = secProduct.getMetadataRoot();
                ProductUtils.copyMetadata(metadataRoot, currentMetadata);
                secondaryTargetMetadata.addElement(currentMetadata);
            }
        }

        ProductUtils.copyMetadata(referenceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(referenceProduct, targetProduct);

        // Add master bands
        ProductNodeGroup<FlagCoding> flagCodingGroup = targetProduct.getFlagCodingGroup();
        ProductNodeGroup<IndexCoding> indexCodingGroup = targetProduct.getIndexCodingGroup();
        for (Band sourceBand : referenceProduct.getBands()) {
            Band targetBand = ProductUtils.copyBand(sourceBand.getName(), referenceProduct, targetProduct, true);
            FlagCoding flagCoding = targetBand.getFlagCoding();
            if (flagCoding != null) {
                // first remove FlagCoding potentially added by copyBand() then handle the FlagCoding
                flagCodingGroup.remove(flagCoding);
                targetBand.setSampleCoding(null);
                handleFlagCoding(sourceBand, targetBand, renameReferenceComponents, referenceComponentPattern);
            }
            IndexCoding indexCoding = targetBand.getIndexCoding();
            if (indexCoding != null) {
                // first remove IndexCoding potentially added by copyBand() then handle the IndexCoding
                indexCodingGroup.remove(indexCoding);
                targetBand.setSampleCoding(null);
                handleIndexCoding(sourceBand, targetBand, renameReferenceComponents, referenceComponentPattern);
            }
            sourceRasterMap.put(targetBand, sourceBand);
            if (renameReferenceComponents) {
                targetBand.setName(referenceComponentPattern.replace(SOURCE_NAME_REFERENCE, sourceBand.getName()));
            }
            originalMasterNames.put(targetBand.getName(), sourceBand.getName());
            masterRasters.add(targetBand);
        }

        // Add master masks
        copyMasks(referenceProduct, renameReferenceComponents, referenceComponentPattern, originalMasterNames, masterRasters);

        String[] collocationFlagsBandNames = new String[secondaryProducts.length];
        String[] collocationFlagsBandDescription = new String[secondaryProducts.length];
        collocationFlagBands = new Band[secondaryProducts.length];
        if (secondaryProducts.length == 1) {
            int collocationCount = 0;
            collocationFlagsBandNames[0] = "collocationFlags";
            while (targetProduct.containsBand(collocationFlagsBandNames[0])) {
                ++collocationCount;
                collocationFlagsBandNames[0] = "collocationFlags" + collocationCount;
            }
            collocationFlagsBandDescription[0] =  collocationFlagsBandNames[0];
        } else {
            for (int i = 0; i < secondaryProducts.length; i++) {
                int collocationCount = 0;
                 collocationFlagsBandNames[i] = secondaryComponentPattern.replace(SOURCE_NAME_REFERENCE, "collocationFlags").replace(SECONDARY_NUMBER_ID_REFERENCE, String.valueOf(i));
                collocationFlagsBandDescription[i] = String.format("collocationFlags_%s", secondaryProducts[i].getName());
                while (targetProduct.containsBand(collocationFlagsBandNames[i])) {
                    ++collocationCount;
                    collocationFlagsBandNames[i] = secondaryComponentPattern.replace(SOURCE_NAME_REFERENCE, "collocationFlags").replace(SECONDARY_NUMBER_ID_REFERENCE, i + "_" + collocationCount);
                    collocationFlagsBandDescription[i] = String.format("collocationFlags_%s", secondaryProducts[i].getName()) + collocationCount;
                }
            }
        }

        for (int i = 0; i < secondaryProducts.length; i++) {
            Product slaveProduct = secondaryProducts[i];
            // Add slave bands
            for (Band sourceBand : slaveProduct.getBands()) {
                String targetBandName = getTargetBandName(sourceBand, i);
                // first creating the band, then copying the properties and then adding it to the target product
                // if the
                Band targetBand = new Band(targetBandName, sourceBand.getDataType(),
                        targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                targetProduct.addBand(targetBand);
                handleSampleCodings(sourceBand, targetBand, renameSecondaryComponents, secondaryComponentPattern);
                sourceRasterMap.put(targetBand, sourceBand);
                originalSlaveNames.put(targetBand.getName(), sourceBand.getName());
                slaveRasters.add(targetBand);
            }

            // Add slave tie-point grids as bands
            for (TiePointGrid sourceGrid : slaveProduct.getTiePointGrids()) {
                String targetBandName = getTargetBandName(sourceGrid, i);
                originalSlaveNames.put(sourceGrid.getName(), targetBandName);
                Band targetBand = targetProduct.addBand(targetBandName, sourceGrid.getDataType());
                ProductUtils.copyRasterDataNodeProperties(sourceGrid, targetBand);
                sourceRasterMap.put(targetBand, sourceGrid);
                originalSlaveNames.put(targetBand.getName(), sourceGrid.getName());
                slaveRasters.add(targetBand);
            }

            //add PRESENT flag
            if (secondaryProducts.length == 1) {
                collocationFlagBands[i] = targetProduct.addBand(collocationFlagsBandNames[i], ProductData.TYPE_INT8);
                collocationFlagBands[i].setDescription(collocationFlagsBandDescription[i]);
                FlagCoding collocationFlagCoding = new FlagCoding(collocationFlagsBandNames[i]);
                collocationFlagCoding.addFlag("SECONDARY_PRESENT", 1, "Data for the secondary is present.");
                collocationFlagBands[i].setSampleCoding(collocationFlagCoding);
                targetProduct.getFlagCodingGroup().add(collocationFlagCoding);
            } else {
                collocationFlagBands[i] = targetProduct.addBand(collocationFlagsBandNames[i], ProductData.TYPE_INT8);
                collocationFlagBands[i].setDescription(collocationFlagsBandDescription[i]);
                FlagCoding collocationFlagCoding = new FlagCoding(collocationFlagsBandNames[i]);
                collocationFlagCoding.addFlag(String.format("SECONDARY_%d_PRESENT", i), 1, "Data for the secondary is present.");
                collocationFlagBands[i].setSampleCoding(collocationFlagCoding);
                targetProduct.getFlagCodingGroup().add(collocationFlagCoding);
            }

            // Copy master geo-coding
            ProductUtils.copyGeoCoding(referenceProduct, targetProduct);
            // Add slave masks
            copyMasks(slaveProduct, renameSecondaryComponents, secondaryComponentPattern, originalSlaveNames, slaveRasters);

            if (renameSecondaryComponents) {
                updateExpressions(originalSlaveNames, slaveRasters);
            }
        }

        if (renameReferenceComponents) {
            updateExpressions(originalMasterNames, masterRasters);
        }

        setAutoGrouping();

        final MetadataElement abstractedMetadataTarget = targetProduct.getMetadataRoot().getElement("Abstracted_Metadata");
        if (abstractedMetadataTarget != null) {
            final MetadataElement secondaryHeadingAngles = new MetadataElement("Secondary_Heading_Angles");
            abstractedMetadataTarget.addElement(secondaryHeadingAngles);
            for (int i = 0; i < secondaryProducts.length; i++) {
                final Product slaveProduct = secondaryProducts[i];
                final MetadataElement abstractedMetadataSource = slaveProduct.getMetadataRoot().getElement("Abstracted_Metadata");
                if (abstractedMetadataSource != null) {
                    final double centreHeading = abstractedMetadataSource.getAttributeDouble("centre_heading");
                    secondaryHeadingAngles.setAttributeDouble(String.format("centre_heading_%d", i), centreHeading);
                }
            }
        }
    }


    private String getTargetBandName(RasterDataNode rasterDataNode, int productIndex) {
        String rasterDataNodeName = rasterDataNode.getName();
        if (renameSecondaryComponents) {
            return rename(rasterDataNodeName, productIndex);
        } else if (targetProduct.containsRasterDataNode(rasterDataNodeName)) {
            if (StringUtils.isNullOrEmpty(secondaryComponentPattern)) {
                throw new OperatorException(format(
                        "Target product already contains a raster data node with name ''{0}''. " +
                                "Parameter 'secondaryComponentPattern' must be set.",
                        rasterDataNodeName));
            }
            return rename(rasterDataNodeName, productIndex);
        }
        return rasterDataNodeName;
    }

    private String rename(String rasterDataNodeName, int productIndex) {
        rasterDataNodeName = secondaryComponentPattern.replace(SOURCE_NAME_REFERENCE, rasterDataNodeName);
        if (secondaryProducts.length > 1) {
            return rasterDataNodeName.replace(SECONDARY_NUMBER_ID_REFERENCE, String.valueOf(productIndex));
        } else {
            return rasterDataNodeName.replace(SECONDARY_NUMBER_ID_REFERENCE, "");
        }
    }


    private void updateExpressions(Map<String, String> originalNames, List<RasterDataNode> rasters) {
        for (String newName : originalNames.keySet()) {
            String newExternalName = BandArithmetic.createExternalName(newName);
            String oldExternalName = BandArithmetic.createExternalName(originalNames.get(newName));
            for (RasterDataNode raster : rasters) {
                raster.setValidPixelExpression(replace(raster.getValidPixelExpression(), oldExternalName, newExternalName));
                if (raster instanceof Mask) {
                    Mask mask = (Mask) raster;
                    Mask.ImageType imageType = mask.getImageType();
                    if (imageType instanceof Mask.BandMathsType) {
                        Mask.BandMathsType mathsType = (Mask.BandMathsType) imageType;
                        Mask.BandMathsType.setExpression(mask, replace(Mask.BandMathsType.getExpression(mask), oldExternalName, newExternalName));
                    } else {
                        imageType.handleRename(mask, oldExternalName, newExternalName);
                    }
                }
                if (raster instanceof VirtualBand) {
                    VirtualBand virtualBand = (VirtualBand) raster;
                    virtualBand.setExpression(replace(virtualBand.getExpression(), oldExternalName, newName));
                }
            }
        }
    }

    private String replace(String expression, String oldName, String newName) {
        if (expression == null || expression.trim().length() == 0) {
            return expression;
        }
        String[] fragments = expression.split("\\b");
        for (int i = 0; i < fragments.length; i++) {
            if (oldName.equals(fragments[i])) {
                if (i == 0 || !".".equals(fragments[i - 1].trim())) {
                    fragments[i] = newName;
                }
            }
        }
        return String.join("", fragments);
    }

    private void validateProduct(Product product) {
        ensureSceneGeoCoding(product);
        ensureSingleRasterSize(product);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws
            OperatorException {
        pm.beginTask("Collocating bands...", targetProduct.getNumBands() + 1);
        try {
            for (final Band targetBand : targetProduct.getBands()) {
                RasterDataNode sourceRaster = sourceRasterMap.get(targetBand);
                PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                        sourceRaster.getGeoCoding(),
                        sourceRaster.getRasterWidth(),
                        sourceRaster.getRasterHeight(),
                        referenceProduct.getSceneGeoCoding(),
                        targetRectangle);
                Rectangle sourceRectangle = getBoundingBox(
                        sourcePixelPositions,
                        sourceRaster.getRasterWidth(),
                        sourceRaster.getRasterHeight());
                pm.worked(1);

                checkForCancellation();
                final Tile targetTile = targetTileMap.get(targetBand);
                ProgressMonitor subPM = SubProgressMonitor.create(pm, 1);

                int collocationFlagId = -1;
                for (int i = 0; i < collocationFlagBands.length; i++) {
                    if (collocationFlagBands[i].getName().equals(targetBand.getName())) {
                        collocationFlagId = i;
                        break;
                    }
                }
                if (collocationFlagId != -1) {
                    sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                            secondaryProducts[collocationFlagId].getSceneGeoCoding(),
                            secondaryProducts[collocationFlagId].getSceneRasterWidth(),
                            secondaryProducts[collocationFlagId].getSceneRasterHeight(),
                            referenceProduct.getSceneGeoCoding(),
                            targetRectangle);
                    sourceRectangle = getBoundingBox(
                            sourcePixelPositions,
                            secondaryProducts[collocationFlagId].getSceneRasterWidth(),
                            secondaryProducts[collocationFlagId].getSceneRasterHeight());
                    pm.worked(1);
                    computePresenceFlag(sourceRectangle, sourcePixelPositions, targetTile, subPM);
                } else {
                    collocateSourceBand(sourceRaster, sourceRectangle, sourcePixelPositions, targetTile, subPM);
                }
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final RasterDataNode sourceRaster = sourceRasterMap.get(targetBand);

        int collocationFlagId = -1;
        for (int i = 0; i < collocationFlagBands.length; i++) {
            if (collocationFlagBands[i].getName().equals(targetBand.getName())) {
                collocationFlagId = i;
                break;
            }
        }
        if (collocationFlagId != -1 || sourceRaster.getProduct() != referenceProduct) {
            if (collocationFlagId != -1) {
                PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                        secondaryProducts[collocationFlagId].getSceneGeoCoding(),
                        secondaryProducts[collocationFlagId].getSceneRasterWidth(),
                        secondaryProducts[collocationFlagId].getSceneRasterHeight(),
                        referenceProduct.getSceneGeoCoding(),
                        targetTile.getRectangle());
                Rectangle sourceRectangle = getBoundingBox(
                        sourcePixelPositions,
                        secondaryProducts[collocationFlagId].getSceneRasterWidth(),
                        secondaryProducts[collocationFlagId].getSceneRasterHeight());
                computePresenceFlag(sourceRectangle, sourcePixelPositions, targetTile, pm);
            } else {
                PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                        sourceRaster.getGeoCoding(),
                        sourceRaster.getRasterWidth(),
                        sourceRaster.getRasterHeight(),
                        referenceProduct.getSceneGeoCoding(),
                        targetTile.getRectangle());
                Rectangle sourceRectangle = getBoundingBox(
                        sourcePixelPositions,
                        sourceRaster.getRasterWidth(),
                        sourceRaster.getRasterHeight());
                collocateSourceBand(sourceRaster, sourceRectangle, sourcePixelPositions, targetTile, pm);
            }
        } else {
            targetTile.setRawSamples(getSourceTile(sourceRaster, targetTile.getRectangle()).getRawSamples());
        }
    }

    @Override
    public void dispose() {
        sourceRasterMap = null;
        for (Product product : disposableProducts) {
            product.dispose();
        }
        super.dispose();
    }

    private void collocateSourceBand(RasterDataNode sourceBand, Rectangle sourceRectangle,
                                     PixelPos[] sourcePixelPositions,
                                     Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask(format("collocating band {0}", sourceBand.getName()), targetTile.getHeight());
        try {
            final RasterDataNode targetBand = targetTile.getRasterDataNode();
            final Rectangle targetRectangle = targetTile.getRectangle();

            final int sourceRasterHeight = sourceBand.getRasterHeight();
            final int sourceRasterWidth = sourceBand.getRasterWidth();

            final Resampling resampling;
            if (isFlagBand(sourceBand) || isValidPixelExpressionUsed(sourceBand)) {
                resampling = ResamplingType.NEAREST_NEIGHBOUR.getResampling();
            } else {
                resampling = resamplingType.getResampling();
            }
            final Resampling.Index resamplingIndex = resampling.createIndex();
            final double noDataValue = targetBand.getGeophysicalNoDataValue();

            if (sourceRectangle != null) {
                final Tile sourceTile = getSourceTile(sourceBand, sourceRectangle);
                final ResamplingRaster resamplingRaster = new ResamplingRaster(sourceTile);

                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        final PixelPos sourcePixelPos = sourcePixelPositions[index];

                        if (sourcePixelPos != null) {
                            resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                                    sourceRasterWidth, sourceRasterHeight, resamplingIndex);
                            double sample;
                            if (resampling == Resampling.NEAREST_NEIGHBOUR) {
                                sample = sourceTile.getSampleDouble((int) resamplingIndex.i0, (int) resamplingIndex.j0);
                            } else {
                                try {
                                    sample = resampling.resample(resamplingRaster, resamplingIndex);
                                } catch (Exception e) {
                                    throw new OperatorException(e.getMessage());
                                }
                            }
                            if (Double.isNaN(sample)) {
                                sample = noDataValue;
                            }
                            targetTile.setSample(x, y, sample);
                        } else {
                            targetTile.setSample(x, y, noDataValue);
                        }
                    }
                    checkForCancellation();
                    pm.worked(1);
                }
            } else {
                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        targetTile.setSample(x, y, noDataValue);
                    }
                    checkForCancellation();
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void computePresenceFlag(Rectangle sourceRectangle, PixelPos[] sourcePixelPositions, Tile targetTile, ProgressMonitor pm) {
        pm.beginTask("collocating presence flag band ", targetTile.getHeight());
        try {
            final Rectangle targetRectangle = targetTile.getRectangle();
            if (sourceRectangle != null) {
                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        if (sourcePixelPositions[index] != null) {
                            targetTile.setSample(x, y, 1);
                        }
                    }
                    checkForCancellation();
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void copyMasks(Product sourceProduct, boolean rename, String pattern, Map<String, String> originalNames, List<RasterDataNode> rasters) {
        ProductNodeGroup<Mask> maskGroup = sourceProduct.getMaskGroup();
        Mask[] sourceMasks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
        for (Mask sourceMask : sourceMasks) {
            if (!targetProduct.getMaskGroup().contains(sourceMask.getName())) {
                Mask.ImageType imageType = sourceMask.getImageType();
                Mask targetMask = new Mask(sourceMask.getName(),
                        targetProduct.getSceneRasterWidth(),
                        targetProduct.getSceneRasterHeight(),
                        imageType);
                targetMask.setDescription(sourceMask.getDescription());
                for (Property property : sourceMask.getImageConfig().getProperties()) {
                    targetMask.getImageConfig().setValue(property.getDescriptor().getName(), property.getValue());
                }
                if (rename) {
                    if (secondaryProducts.length == 1) {
                        targetMask.setName(pattern.replace(SOURCE_NAME_REFERENCE, sourceMask.getName()).replace(SECONDARY_NUMBER_ID_REFERENCE, ""));
                    } else {
                        int id = -1;
                        for (int i = 0; i < secondaryProducts.length; i++) {
                            if (sourceProduct.getName().equals(secondaryProducts[i].getName())) {
                                id = i;
                                break;
                            }
                        }
                        targetMask.setName(pattern.replace(SOURCE_NAME_REFERENCE, sourceMask.getName()).replace(SECONDARY_NUMBER_ID_REFERENCE, String.valueOf(id)));
                    }
                }
                targetProduct.getMaskGroup().add(targetMask);
                originalNames.put(targetMask.getName(), sourceMask.getName());
                rasters.add(targetMask);
            }
        }
    }

    private void handleSampleCodings(Band sourceBand, Band targetBand, boolean renameComponents, String renamePattern) {
        if (sourceBand.getFlagCoding() != null) {
            handleFlagCoding(sourceBand, targetBand, renameComponents, renamePattern);
        }
        if (sourceBand.getIndexCoding() != null) {
            handleIndexCoding(sourceBand, targetBand, renameComponents, renamePattern);
        }
    }

    private void handleFlagCoding(Band sourceBand, Band targetBand, boolean renameComponents, String renamePattern) {
        setFlagCoding(targetBand, sourceBand.getFlagCoding(), renameComponents, renamePattern);
    }

    private void handleIndexCoding(Band sourceBand, Band targetBand, boolean renameComponents, String renamePattern) {
        setIndexCoding(targetBand, sourceBand.getIndexCoding(), renameComponents, renamePattern);
    }

    private void setFlagCoding(Band band, FlagCoding flagCoding, boolean rename, String pattern) {
        String flagCodingName = flagCoding.getName();
        if (rename) {
            if (secondaryProducts.length == 1) {
                flagCodingName = pattern.replace(SOURCE_NAME_REFERENCE, flagCodingName).replace(SECONDARY_NUMBER_ID_REFERENCE, "");
            } else {
                int id = -1;
                for (int i = 0; i < secondaryProducts.length; i++) {
                    if (band.getProduct().getName().equals(secondaryProducts[i].getName())) {
                        id = i;
                        break;
                    }
                }
                flagCodingName = pattern.replace(SOURCE_NAME_REFERENCE, flagCodingName).replace(SECONDARY_NUMBER_ID_REFERENCE, String.valueOf(id));
            }
        }
        final Product product = band.getProduct();
        if (!product.getFlagCodingGroup().contains(flagCodingName)) {
            addFlagCoding(product, flagCoding, flagCodingName);
        }
        band.setSampleCoding(product.getFlagCodingGroup().get(flagCodingName));
    }

    private void setIndexCoding(Band band, IndexCoding indexCoding, boolean rename, String pattern) {
        String indexCodingName = indexCoding.getName();
        if (rename) {
            if (secondaryProducts.length == 1) {
                indexCodingName = pattern.replace(SOURCE_NAME_REFERENCE, indexCodingName).replace(SECONDARY_NUMBER_ID_REFERENCE, "");
            } else {
                int id = -1;
                for (int i = 0; i < secondaryProducts.length; i++) {
                    if (band.getProduct().getName().equals(secondaryProducts[i].getName())) {
                        id = i;
                        break;
                    }
                }
                indexCodingName = pattern.replace(SOURCE_NAME_REFERENCE, indexCodingName).replace(SECONDARY_NUMBER_ID_REFERENCE, String.valueOf(id));
            }
        }
        final Product product = band.getProduct();
        if (!product.getIndexCodingGroup().contains(indexCodingName)) {
            addIndexCoding(product, indexCoding, indexCodingName);
        }
        band.setSampleCoding(product.getIndexCodingGroup().get(indexCodingName));
    }

    private static void addFlagCoding(Product product, FlagCoding flagCoding, String flagCodingName) {
        final FlagCoding targetFlagCoding = new FlagCoding(flagCodingName);

        targetFlagCoding.setDescription(flagCoding.getDescription());
        ProductUtils.copyMetadata(flagCoding, targetFlagCoding);
        product.getFlagCodingGroup().add(targetFlagCoding);
    }

    private static void addIndexCoding(Product product, IndexCoding indexCoding, String indexCodingName) {
        final IndexCoding targetIndexCoding = new IndexCoding(indexCodingName);

        targetIndexCoding.setDescription(indexCoding.getDescription());
        ProductUtils.copyMetadata(indexCoding, targetIndexCoding);
        product.getIndexCodingGroup().add(targetIndexCoding);
    }

    private static Rectangle getBoundingBox(PixelPos[] pixelPositions, int maxWidth, int maxHeight) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final PixelPos pixelsPos : pixelPositions) {
            if (pixelsPos != null) {
                final int x = (int) Math.floor(pixelsPos.getX());
                final int y = (int) Math.floor(pixelsPos.getY());

                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - 2, 0);
        maxX = Math.min(maxX + 2, maxWidth - 1);
        minY = Math.max(minY - 2, 0);
        maxY = Math.min(maxY + 2, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isFlagBand(RasterDataNode sourceRaster) {
        return (sourceRaster instanceof Band && ((Band) sourceRaster).isFlagBand());
    }

    private static boolean isValidPixelExpressionUsed(RasterDataNode sourceRaster) {
        final String validPixelExpression = sourceRaster.getValidPixelExpression();
        return validPixelExpression != null && !validPixelExpression.trim().isEmpty();
    }

    private void setAutoGrouping() {
        List<String> paths = new ArrayList<>();
        collectAutoGrouping(paths, referenceProduct, renameReferenceComponents ? referenceComponentPattern : null);
        for (Product slaveProduct : secondaryProducts) {
            collectAutoGrouping(paths, slaveProduct, renameSecondaryComponents ? secondaryComponentPattern : null);
        }
        targetProduct.setAutoGrouping(String.join(":", paths));
    }

    private void collectAutoGrouping(List<String> paths, Product product, String componentPattern) {
        BandGroup autoGrouping = product.getAutoGrouping();
        if (autoGrouping == null) {
            return;
        }
        String replacementSlaveNumberId = "";
        if (secondaryProducts.length == 1) {
            replacementSlaveNumberId = "";
        } else {
            int id = -1;
            for (int i = 0; i < secondaryProducts.length; i++) {
                if (product.getName().equals(secondaryProducts[i].getName())) {
                    id = i;
                    break;
                }
            }
            replacementSlaveNumberId = String.valueOf(id);
        }

        for (String[] pattern : autoGrouping) {
            String[] clone = pattern.clone();
            if (componentPattern != null) {
                String last = clone[clone.length - 1];
                if (!last.endsWith("*")) {
                    last += "*";
                    // If an asterisk has been added also one is needed at the beginning.
                    // If it is not added the node name must start with the pattern.
                    if (!last.startsWith("*")) {
                        last = "*" + last;
                    }
                }

                last = componentPattern.replace(SOURCE_NAME_REFERENCE, last).replace(SECONDARY_NUMBER_ID_REFERENCE, "*");
                clone[clone.length - 1] = last;
            }
            paths.add(String.join("/", clone));
        }
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;

        public ResamplingRaster(Tile tile) {
            this.tile = tile;
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(int[] x, int[] y, double[][] samples) {
            boolean allValid = true;
            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {
                    samples[i][j] = tile.getSampleDouble(x[j], y[i]);
                    if (isNoDataValue(samples[i][j])) {
                        allValid = false;
                    }
                }
            }
            return allValid;
        }

        private boolean isNoDataValue(double sample) {
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();

            if (rasterDataNode.isNoDataValueUsed()) {
                if (rasterDataNode.isScalingApplied()) {
                    return rasterDataNode.getGeophysicalNoDataValue() == sample;
                } else {
                    return rasterDataNode.getNoDataValue() == sample;
                }
            }

            return false;
        }
    }

    private void addToSourceProducts(Product product) {
        if (sourceProducts == null) {
            sourceProducts = new Product[1];
            sourceProducts[0] = product;
        } else {
            ArrayList<Product> productList = new ArrayList<>();
            Collections.addAll(productList, sourceProducts);
            productList.add(product);
            sourceProducts = productList.toArray(new Product[productList.size()]);
        }
    }

    /**
     * Collocation operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CollocateOp.class);
        }
    }
}
