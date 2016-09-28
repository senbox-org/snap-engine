package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc .
 */
public class DetectInstrumentTest {

    @Test
    public void testAutoDetectForMERISProduct() {
        Product product = new Product("dummy", "mer_r", 2, 2);
        product.setProductType("MER_RR__2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));

        product.setProductType("MER_FRS_2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));

        product.setProductType("MER_RR__2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));

        product.setProductType("MER_FR__2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));
    }


    @Test
    public void testAutoDetectForOLCIProduct() {
        Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("Oa01_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa02_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa03_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa04_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa05_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa06_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa07_reflectance", ProductData.TYPE_INT8);
        assertEquals(Instrument.OLCI, DetectInstrument.getInstrument(product));
    }

    @Test
    public void testAutoDetectForMODISProduct() {
        Product product = new Product("dummy", "modis", 2, 2);
        MetadataElement root = product.getMetadataRoot();
        MetadataElement dsd23 = new MetadataElement("Global_Attributes");
        dsd23.setAttributeString("title", "HMODISA Level-2 Data");
        root.addElement(dsd23);

        assertEquals(Instrument.MODIS, DetectInstrument.getInstrument(product));
    }

    @Test
    public void testAutoDetectForSEAWIFSProduct() {
        Product product = new Product("dummy", "seawifs", 2, 2);
        MetadataElement root = product.getMetadataRoot();
        MetadataElement dsd23 = new MetadataElement("Global_Attributes");
        dsd23.setAttributeString("Title", "SeaWiFS Level-2 Data");
        root.addElement(dsd23);

        assertEquals(Instrument.SEAWIFS, DetectInstrument.getInstrument(product));
    }
}