package org.esa.snap.tests;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

//@Ignore
public class ReaderTests {

    String testDataDir = "T:/SNAP/Performance_Test_DiMap_ZNAP/";
    Product product;

    @After
    public void tearDown() {
        if (product != null) {
            this.product.dispose();
        }
        System.gc();
    }

    @Test
    @STTM("SNAP-3712")
    public void testSentinel1() throws IOException {
        this.product = readProduct("Sentinel-1/S1A_IW_GRDH_1SDH_20140413T034943_20140413T035012_000135_000075_3662.zip");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void testSentinel1_2() throws IOException {
        System.out.println(System.getProperty("java.class.path"));
        this.product = readProduct("Sentinel-2/S2B_MSIL1C_20190506T081609_N0207_R121_T36SXA_20190506T104054.SAFE");
        assertNotNull(product);
    }

    private Product readProduct(String filename) throws IOException {
        Product product = ProductIO.readProduct(new File(testDataDir + filename));
        return product;
    }
}
