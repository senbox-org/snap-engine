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

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.datamodel.group.BandGroup;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Color;
import java.lang.reflect.Field;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CollocateOpTest {

    @Test
    public void testCollocate1Type() {
        final Product masterProduct = createTestProduct1();
        final Product slaveProduct = createTestProduct1();

        CollocateOp op = new CollocateOp();
        op.setParameterDefaultValues();
        op.setSecondaryComponentPattern("${ORIGINAL_NAME}_S");
        op.setCopySecondaryMetadata(true);

        // test default settings
        assertEquals("COLLOCATED", op.getTargetProductType());
        assertTrue(op.getRenameReferenceComponents());
        assertTrue(op.getRenameSecondaryComponents());
        assertEquals("${ORIGINAL_NAME}_M", op.getReferenceComponentPattern());
        assertEquals("${ORIGINAL_NAME}_S", op.getSecondaryComponentPattern());
        assertEquals(ResamplingType.NEAREST_NEIGHBOUR, op.getResamplingType());

        op.setReferenceProduct(masterProduct);
        op.setSecondary(slaveProduct);

        Product targetProduct = op.getTargetProduct();

        final MetadataElement secMetadata = targetProduct.getMetadataRoot().getElement("SecondaryMetadata");
        final MetadataElement secMeta = secMetadata.getElement("MER_RR_1P");
        assertEquals(1, secMeta.getNumElements());
        assertNotNull(secMeta.getElement("test1"));

        int numMasterBands = masterProduct.getNumBands();
        int numSlaveBands = slaveProduct.getNumBands();
        int numMasterTPGs = masterProduct.getNumTiePointGrids();
        int numSlaveTPGs = slaveProduct.getNumTiePointGrids();
        assertEquals(numMasterBands + numSlaveBands + numSlaveTPGs + 1, targetProduct.getNumBands());
        assertEquals(numMasterTPGs, targetProduct.getNumTiePointGrids());

        assertEquals("radiance_1_M", targetProduct.getBandAt(0).getName());
        assertEquals("radiance_2_M", targetProduct.getBandAt(1).getName());
        assertEquals("l1_flags_M", targetProduct.getBandAt(15).getName());
        assertEquals("l1_class_M", targetProduct.getBandAt(16).getName());

        assertEquals("radiance_1_S", targetProduct.getBandAt(16 + 1).getName());
        assertEquals("radiance_2_S", targetProduct.getBandAt(16 + 2).getName());
        assertEquals("l1_flags_S", targetProduct.getBandAt(16 + 16).getName());
        assertEquals("l1_class_S", targetProduct.getBandAt(16 + 17).getName());

        assertEquals("latitude_S", targetProduct.getBandAt(16 + 17 + 1).getName());
        assertEquals("longitude_S", targetProduct.getBandAt(16 + 17 + 2).getName());
        assertEquals("dem_altitude_S", targetProduct.getBandAt(16 + 17 + 3).getName());

        assertEquals("!l1_flags_M.INVALID && radiance_1_M > 10", targetProduct.getBandAt(0).getValidMaskExpression());
        assertEquals("!l1_flags_M.INVALID && radiance_1_M > 10", targetProduct.getBandAt(1).getValidMaskExpression());

        assertEquals("!l1_flags_S.INVALID && radiance_1_S > 10",
                targetProduct.getBandAt(16 + 1).getValidMaskExpression());
        assertEquals("!l1_flags_S.INVALID && radiance_1_S > 10",
                targetProduct.getBandAt(16 + 2).getValidMaskExpression());

        assertEquals(4, targetProduct.getMaskGroup().getNodeCount());
        Mask mask1 = targetProduct.getMaskGroup().get(0);
        Mask mask2 = targetProduct.getMaskGroup().get(1);
        Mask mask3 = targetProduct.getMaskGroup().get(2);
        Mask mask4 = targetProduct.getMaskGroup().get(3);
        assertNotNull(mask1);
        assertNotNull(mask2);
        assertNotNull(mask3);
        assertNotNull(mask4);
        assertEquals("bitmask_M", mask1.getName());
        assertEquals("INVALID_M", mask2.getName());
        assertEquals("bitmask_S", mask3.getName());
        assertEquals("INVALID_S", mask4.getName());
        assertEquals("radiance_1_M > 10", Mask.BandMathsType.getExpression(mask1));
        assertEquals("l1_flags_M.INVALID", Mask.BandMathsType.getExpression(mask2));
        assertEquals("radiance_1_S > 10", Mask.BandMathsType.getExpression(mask3));
        assertEquals("l1_flags_S.INVALID", Mask.BandMathsType.getExpression(mask4));
        assertEquals(Color.RED, mask1.getImageColor());
        assertEquals(Color.GREEN, mask2.getImageColor());
        assertEquals(Color.RED, mask3.getImageColor());
        assertEquals(Color.GREEN, mask4.getImageColor());
        assertEquals(0.5, mask1.getImageTransparency(), 0.00001);
        assertEquals(0.5, mask2.getImageTransparency(), 0.00001);
        assertEquals(0.5, mask3.getImageTransparency(), 0.00001);
        assertEquals(0.5, mask4.getImageTransparency(), 0.00001);

        assertEquals(2 + 1, targetProduct.getFlagCodingGroup().getNodeCount());
        assertNotNull(targetProduct.getFlagCodingGroup().get("l1_flags_M"));
        assertNotNull(targetProduct.getFlagCodingGroup().get("l1_flags_S"));

        assertEquals(2, targetProduct.getIndexCodingGroup().getNodeCount());
        assertNotNull(targetProduct.getIndexCodingGroup().get("l1_class_M"));
        assertNotNull(targetProduct.getIndexCodingGroup().get("l1_class_S"));

        BandGroup autoGrouping = targetProduct.getAutoGrouping();
        assertNotNull(autoGrouping);
        assertEquals(2, autoGrouping.size());
        assertArrayEquals(new String[]{"*radiance*_M"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"*radiance*_S"}, autoGrouping.get(1));
    }

    @Test
    public void testCollocate2Types() {
        final Product masterProduct = createTestProduct1();
        final Product slaveProduct = createTestProduct2();

        CollocateOp op = new CollocateOp();
        op.setParameterDefaultValues();
        op.setSecondaryComponentPattern("${ORIGINAL_NAME}_S");
        op.setCopySecondaryMetadata(true);

        // test default settings
        assertEquals("COLLOCATED", op.getTargetProductType());
        assertTrue(op.getRenameReferenceComponents());
        assertTrue(op.getRenameSecondaryComponents());
        assertEquals("${ORIGINAL_NAME}_M", op.getReferenceComponentPattern());
        assertEquals("${ORIGINAL_NAME}_S", op.getSecondaryComponentPattern());
        assertEquals(ResamplingType.NEAREST_NEIGHBOUR, op.getResamplingType());

        op.setReferenceProduct(masterProduct);
        op.setSecondary(slaveProduct);

        Product targetProduct = op.getTargetProduct();

        final MetadataElement secMetadata = targetProduct.getMetadataRoot().getElement("SecondaryMetadata");
        final MetadataElement secMeta = secMetadata.getElement("MER_RR_2P");
        assertEquals(1, secMeta.getNumElements());
        assertNotNull(secMeta.getElement("test2"));

        int numMasterBands = masterProduct.getNumBands();
        int numSlaveBands = slaveProduct.getNumBands();
        int numMasterTPGs = masterProduct.getNumTiePointGrids();
        int numSlaveTPGs = slaveProduct.getNumTiePointGrids();
        assertEquals(numMasterBands + numSlaveBands + numSlaveTPGs + 1, targetProduct.getNumBands());
        assertEquals(numMasterTPGs, targetProduct.getNumTiePointGrids());

        assertEquals("radiance_1_M", targetProduct.getBandAt(0).getName());
        assertEquals("radiance_2_M", targetProduct.getBandAt(1).getName());
        assertEquals("l1_flags_M", targetProduct.getBandAt(15).getName());
        assertEquals("l1_class_M", targetProduct.getBandAt(16).getName());

        assertEquals("reflec_1_S", targetProduct.getBandAt(16 + 1).getName());
        assertEquals("reflec_2_S", targetProduct.getBandAt(16 + 2).getName());
        assertEquals("l2_flags_S", targetProduct.getBandAt(16 + 16).getName());
        assertEquals("l2_class_S", targetProduct.getBandAt(16 + 17).getName());

        assertEquals("latitude_S", targetProduct.getBandAt(16 + 17 + 1).getName());
        assertEquals("longitude_S", targetProduct.getBandAt(16 + 17 + 2).getName());
        assertEquals("dem_altitude_S", targetProduct.getBandAt(16 + 17 + 3).getName());

        assertEquals("!l1_flags_M.INVALID && radiance_1_M > 10", targetProduct.getBandAt(0).getValidMaskExpression());
        assertEquals("!l1_flags_M.INVALID && radiance_1_M > 10", targetProduct.getBandAt(1).getValidMaskExpression());

        assertEquals("!l2_flags_S.INVALID && reflec_1_S > 0.1",
                targetProduct.getBandAt(16 + 1).getValidMaskExpression());
        assertEquals("!l2_flags_S.INVALID && reflec_1_S > 0.1",
                targetProduct.getBandAt(16 + 2).getValidMaskExpression());

        assertEquals(3, targetProduct.getMaskGroup().getNodeCount());
        Mask mask1 = targetProduct.getMaskGroup().get(0);
        Mask mask2 = targetProduct.getMaskGroup().get(1);
        Mask mask3 = targetProduct.getMaskGroup().get(2);
        assertNotNull(mask1);
        assertNotNull(mask2);
        assertNotNull(mask3);
        assertEquals("bitmask_M", mask1.getName());
        assertEquals("INVALID_M", mask2.getName());
        assertEquals("INVALID_S", mask3.getName());
        assertEquals("radiance_1_M > 10", Mask.BandMathsType.getExpression(mask1));
        assertEquals("l1_flags_M.INVALID", Mask.BandMathsType.getExpression(mask2));
        assertEquals("l2_flags_S.INVALID", Mask.BandMathsType.getExpression(mask3));
        assertEquals(Color.RED, mask1.getImageColor());
        assertEquals(Color.GREEN, mask2.getImageColor());
        assertEquals(Color.BLUE, mask3.getImageColor());
        assertEquals(0.5, mask1.getImageTransparency(), 0.00001);
        assertEquals(0.5, mask2.getImageTransparency(), 0.00001);
        assertEquals(0.5, mask3.getImageTransparency(), 0.00001);

        assertEquals(2 + 1, targetProduct.getFlagCodingGroup().getNodeCount());
        assertNotNull(targetProduct.getFlagCodingGroup().get("l1_flags_M"));
        assertNotNull(targetProduct.getFlagCodingGroup().get("l2_flags_S"));

        assertEquals(2, targetProduct.getIndexCodingGroup().getNodeCount());
        assertNotNull(targetProduct.getIndexCodingGroup().get("l1_class_M"));
        assertNotNull(targetProduct.getIndexCodingGroup().get("l2_class_S"));

        BandGroup autoGrouping = targetProduct.getAutoGrouping();
        assertNotNull(autoGrouping);
        assertEquals(2, autoGrouping.size());
        assertArrayEquals(new String[]{"*radiance*_M"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"*reflec*_S"}, autoGrouping.get(1));
    }

    @Test
    public void testAutogroupingAATSRStyle() {
        final Product masterProduct = createTestProductAATSR();
        final Product slaveProduct = createTestProduct1();

        CollocateOp op = new CollocateOp();
        op.setParameterDefaultValues();
        op.setSecondaryComponentPattern("${ORIGINAL_NAME}_S");

        op.setReferenceProduct(masterProduct);
        op.setSecondary(slaveProduct);

        Product targetProduct = op.getTargetProduct();
        BandGroup autoGrouping = targetProduct.getAutoGrouping();
        assertNotNull(autoGrouping);
        assertEquals(3, autoGrouping.size());
        assertArrayEquals(new String[]{"*nadir*_M"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"*fward*_M"}, autoGrouping.get(1));
        assertArrayEquals(new String[]{"*radiance*_S"}, autoGrouping.get(2));
    }

    @Test
    public void testCollocate_failsWhenSlaveRenamingPatternIsMissing() {
        final Product masterProduct = createTestProduct1();
        final Product slaveProduct = createTestProduct1();

        CollocateOp op = new CollocateOp();
        op.setParameterDefaultValues();
        op.setRenameSecondaryComponents(false);
        op.setSecondaryComponentPattern("");

        op.setReferenceProduct(masterProduct);
        op.setSecondary(slaveProduct);

        try {
            op.getTargetProduct();
            fail("Exception expected");
        } catch (OperatorException oe) {
            assertEquals("Target product already contains a raster data node with name 'latitude'. " +
                            "Parameter secondaryComponentPattern must be set.",
                    oe.getMessage());
        }
    }

    @Test
    public void testCollocate_SampleCodingSOfMasterWhenNotRenamingMaster() {
        final Product masterProduct = createTestProduct1();
        final Product slaveProduct = createTestProduct1();

        CollocateOp op = new CollocateOp();
        op.setParameterDefaultValues();
        op.setRenameReferenceComponents(false);

        op.setReferenceProduct(masterProduct);
        op.setSecondary(slaveProduct);

        Product targetProduct = op.getTargetProduct();
        ProductNodeGroup<FlagCoding> flagCodingGroup = targetProduct.getFlagCodingGroup();
        assertEquals(3, flagCodingGroup.getNodeCount());
        assertTrue(flagCodingGroup.contains("l1_flags"));
        assertTrue(flagCodingGroup.contains("l1_flags_S"));
        assertTrue(flagCodingGroup.contains("collocationFlags"));
        assertTrue(flagCodingGroup.contains(targetProduct.getBand("l1_flags").getFlagCoding()));
        assertTrue(flagCodingGroup.contains(targetProduct.getBand("l1_flags_S").getFlagCoding()));

        ProductNodeGroup<IndexCoding> indexCodingGroup = targetProduct.getIndexCodingGroup();
        assertEquals(2, indexCodingGroup.getNodeCount());
        assertTrue(indexCodingGroup.contains("l1_class"));
        assertTrue(indexCodingGroup.contains("l1_class_S"));
        assertTrue(indexCodingGroup.contains(targetProduct.getBand("l1_class").getIndexCoding()));
        assertTrue(indexCodingGroup.contains(targetProduct.getBand("l1_class_S").getIndexCoding()));
    }

    @Test
    public void testCollocate_SampleCodingOfMasterWhenRenamingMaster() {
        final Product masterProduct = createTestProduct1();
        final Product slaveProduct = createTestProduct1();

        CollocateOp op = new CollocateOp();
        op.setParameterDefaultValues();
        op.setRenameReferenceComponents(true);

        op.setReferenceProduct(masterProduct);
        op.setSecondary(slaveProduct);

        Product targetProduct = op.getTargetProduct();
        ProductNodeGroup<FlagCoding> flagCodingGroup = targetProduct.getFlagCodingGroup();
        assertEquals(3, flagCodingGroup.getNodeCount());
        assertTrue(flagCodingGroup.contains("l1_flags_M"));
        assertTrue(flagCodingGroup.contains("l1_flags_S"));
        assertTrue(flagCodingGroup.contains("collocationFlags"));
        assertTrue(flagCodingGroup.contains(targetProduct.getBand("l1_flags_M").getFlagCoding()));
        assertTrue(flagCodingGroup.contains(targetProduct.getBand("l1_flags_S").getFlagCoding()));

        ProductNodeGroup<IndexCoding> indexCodingGroup = targetProduct.getIndexCodingGroup();
        assertEquals(2, indexCodingGroup.getNodeCount());
        assertTrue(indexCodingGroup.contains("l1_class_M"));
        assertTrue(indexCodingGroup.contains("l1_class_S"));
        assertTrue(indexCodingGroup.contains(targetProduct.getBand("l1_class_M").getIndexCoding()));
        assertTrue(indexCodingGroup.contains(targetProduct.getBand("l1_class_S").getIndexCoding()));
    }

    @Test
    @STTM("SNAP-3660")
    public void testParameterNaming() throws NoSuchFieldException {
        final CollocateOp op = new CollocateOp();
        final Class<? extends CollocateOp> collOpClass = op.getClass();

        final Field sourceProductsField = collOpClass.getDeclaredField("sourceProducts");
        final SourceProducts[] spAnnotation = sourceProductsField.getDeclaredAnnotationsByType(SourceProducts.class);
        assertEquals(1, spAnnotation.length);
        assertEquals("The source product(s) which serve(s) as secondary.", spAnnotation[0].description());
        assertEquals("sourceProducts", spAnnotation[0].alias());

        final Field referenceField = collOpClass.getDeclaredField("reference");
        final SourceProduct[] refAnnotation = referenceField.getDeclaredAnnotationsByType(SourceProduct.class);
        assertEquals(1, refAnnotation.length);
        assertEquals("The source product which serves as reference.", refAnnotation[0].description());
        assertEquals("reference", refAnnotation[0].alias());

        final Field secondaryField = collOpClass.getDeclaredField("secondary");
        final SourceProduct[] secAnnotation = secondaryField.getDeclaredAnnotationsByType(SourceProduct.class);
        assertEquals(1, secAnnotation.length);
        assertEquals("The source product which serves as secondary.", secAnnotation[0].description());
        assertEquals("secondary", secAnnotation[0].alias());

        final Field refProdNameField = collOpClass.getDeclaredField("referenceProductName");
        final Parameter[] refProdNameAnnotations = refProdNameField.getDeclaredAnnotationsByType(Parameter.class);
        assertEquals(1, refProdNameAnnotations.length);
        assertEquals("referenceProductName", refProdNameAnnotations[0].alias());
        assertEquals("Reference product name", refProdNameAnnotations[0].label());
        assertEquals("The name of the reference product.", refProdNameAnnotations[0].description());

        final Field targetProdField = collOpClass.getDeclaredField("targetProduct");
        final TargetProduct[] targetProdAnnotations = targetProdField.getDeclaredAnnotationsByType(TargetProduct.class);
        assertEquals(1, targetProdAnnotations.length);
        assertEquals("The target product which will use the reference's grid.", targetProdAnnotations[0].description());

        final Field copySecondaryMetadataField = collOpClass.getDeclaredField("copySecondaryMetadata");
        final Parameter[] copySecondaryMetaAnnotations = copySecondaryMetadataField.getDeclaredAnnotationsByType(Parameter.class);
        assertEquals(1, copySecondaryMetaAnnotations.length);
        assertEquals("Copy metadata of secondary products", copySecondaryMetaAnnotations[0].label());
        assertEquals("Copies also the metadata of the secondary products to the target.", copySecondaryMetaAnnotations[0].description());

        final Field renameRefCompField = collOpClass.getDeclaredField("renameReferenceComponents");
        final Parameter[] renameRefCompAnnotations = renameRefCompField.getDeclaredAnnotationsByType(Parameter.class);
        assertEquals(1, renameRefCompAnnotations.length);
        assertEquals("Whether or not components of the reference product shall be renamed in the target product.", renameRefCompAnnotations[0].description());

        final Field renameSecCompField = collOpClass.getDeclaredField("renameSecondaryComponents");
        final Parameter[] renameSecCompAnnotations = renameSecCompField.getDeclaredAnnotationsByType(Parameter.class);
        assertEquals(1, renameSecCompAnnotations.length);
        assertEquals("Whether or not components of the secondary product(s) shall be renamed in the target product.", renameSecCompAnnotations[0].description());

        final Field refCompPatternField = collOpClass.getDeclaredField("referenceComponentPattern");
        final Parameter[] refCompPatternAnnotations = refCompPatternField.getDeclaredAnnotationsByType(Parameter.class);
        assertEquals(1, refCompPatternAnnotations.length);
        assertEquals("The text pattern to be used when renaming reference components.", refCompPatternAnnotations[0].description());

        final Field secCompPatternField = collOpClass.getDeclaredField("secondaryComponentPattern");
        final Parameter[] secCompPatternAnnotations = secCompPatternField.getDeclaredAnnotationsByType(Parameter.class);
        assertEquals(1, secCompPatternAnnotations.length);
        assertEquals("The text pattern to be used when renaming secondary components.", secCompPatternAnnotations[0].description());

        final Field resamplingTypeField = collOpClass.getDeclaredField("resamplingType");
        final Parameter[] resampTypeAnnotations = resamplingTypeField.getDeclaredAnnotationsByType(Parameter.class);
        assertEquals(1, resampTypeAnnotations.length);
        assertEquals("The method to be used when resampling the secondary grid onto the reference grid.", resampTypeAnnotations[0].description());
    }

    private static final float[] wl = {
            412.6395569f,
            442.5160217f,
            489.8732910f,
            509.8299866f,
            559.7575684f,
            619.7247925f,
            664.7286987f,
            680.9848022f,
            708.4989624f,
            753.5312500f,
            761.7092285f,
            778.5520020f,
            864.8800049f,
            884.8975830f,
            899.9100342f
    };

    private static Product createTestProduct1() {
        final Product product = new Product("MER_RR_1P", "MER_RR_1P", 16, 16);
        for (int i = 0; i < wl.length; i++) {
            Band band = new VirtualBand("radiance_" + (i + 1), ProductData.TYPE_FLOAT32, 16, 16, "X+Y");
            band.setValidPixelExpression("!l1_flags.INVALID && radiance_1 > 10");
            band.setSpectralWavelength(wl[i]);
            band.setSpectralBandIndex(i);
            product.addBand(band);
        }
        addFlagCoding(product, "l1_flags");
        addIndexCoding(product, "l1_class");
        product.getMetadataRoot().addElement(new MetadataElement("test1"));

        product.addTiePointGrid(createTPG("latitude"));
        product.addTiePointGrid(createTPG("longitude"));
        product.addTiePointGrid(createTPG("dem_altitude"));
        setSceneGeoCoding(product);
        product.addMask("bitmask", "radiance_1 > 10", null, Color.RED, 0.5f);
        product.addMask("INVALID", "l1_flags.INVALID", null, Color.GREEN, 0.5f);
        product.setAutoGrouping("radiance");
        return product;
    }

    private static Product createTestProduct2() {
        final Product product = new Product("MER_RR_2P", "MER_RR_2P", 16, 16);
        for (int i = 0; i < wl.length; i++) {
            Band band = new VirtualBand("reflec_" + (i + 1), ProductData.TYPE_FLOAT32, 16, 16, "X*Y");
            band.setValidPixelExpression("!l2_flags.INVALID && reflec_1 > 0.1");
            band.setSpectralWavelength(wl[i]);
            band.setSpectralBandIndex(i);
            product.addBand(band);
        }
        addFlagCoding(product, "l2_flags");
        addIndexCoding(product, "l2_class");
        product.getMetadataRoot().addElement(new MetadataElement("test2"));

        product.addTiePointGrid(createTPG("latitude"));
        product.addTiePointGrid(createTPG("longitude"));
        product.addTiePointGrid(createTPG("dem_altitude"));
        setSceneGeoCoding(product);
        product.addMask("INVALID", "l2_flags.INVALID", null, Color.BLUE, 0.5f);
        product.setAutoGrouping("reflec");
        return product;
    }

    private static Product createTestProductAATSR() {
        final Product product = new Product("ATS_TOA_1P", "ATS_TOA_1P", 16, 16);
        product.addBand(new VirtualBand("btemp_nadir_1200", ProductData.TYPE_FLOAT32, 16, 16, "X*Y"));
        product.addBand(new VirtualBand("btemp_nadir_1100", ProductData.TYPE_FLOAT32, 16, 16, "X+Y"));
        product.addBand(new VirtualBand("btemp_nadir_0370", ProductData.TYPE_FLOAT32, 16, 16, "X*X"));
        product.addBand(new VirtualBand("btemp_fward_1200", ProductData.TYPE_FLOAT32, 16, 16, "Y"));
        product.addBand(new VirtualBand("btemp_fward_1100", ProductData.TYPE_FLOAT32, 16, 16, "Y*Y"));

        setSceneGeoCoding(product);
        product.setAutoGrouping("nadir:fward");
        return product;
    }

    private static void setSceneGeoCoding(Product product) {
        try {
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 16, 16, 0.2, 0.2, 0.1, 0.1));
        } catch (FactoryException | TransformException e) {
            fail("Test product could not be created");
        }
    }

    private static void addIndexCoding(Product product, String indexCodingName) {
        Band classBand = product.addBand(indexCodingName, ProductData.TYPE_UINT16);
        IndexCoding indexCoding = new IndexCoding(indexCodingName);
        indexCoding.addIndex("CLASS_1", 1, "first class");
        indexCoding.addIndex("CLASS_2", 2, "first class");
        classBand.setSampleCoding(indexCoding);
        product.getIndexCodingGroup().add(indexCoding);
    }

    private static void addFlagCoding(Product product, String flagCodingName) {
        Band flagsBand = product.addBand(flagCodingName, ProductData.TYPE_UINT32);
        FlagCoding flagCoding = new FlagCoding(flagCodingName);
        flagCoding.addFlag("INVALID", 1, "invalid description");
        flagsBand.setSampleCoding(flagCoding);
        product.getFlagCodingGroup().add(flagCoding);
    }

    private static TiePointGrid createTPG(String name) {
        return new TiePointGrid(name, 5, 5, 0.5f, 0.5f, 4f, 4f, new float[5 * 5]);
    }
}
