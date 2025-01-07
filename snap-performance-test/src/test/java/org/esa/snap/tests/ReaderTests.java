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

@Ignore
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
    public void testSentinel_1() throws IOException {
        this.product = readProduct("Sentinel-1/S1A_IW_GRDH_1SDV_20160121T180933_20160121T181002_009596_00DF74_3705.zip");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void testSentinel_2() throws IOException {
        this.product = readProduct("Sentinel-2/S2B_MSIL1C_20190506T081609_N0207_R121_T36SXA_20190506T104054.SAFE");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void testSentinel_3_olci() throws IOException {
        this.product = readProduct("Sentinel-3/olci/S3A_OL_2_WFR____20070425T152940_20070425T153025_20140610T112151_0045_000_000______MAR_D_NR____.SEN3");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void testSentinel_3_slstr() throws IOException {
        this.product = readProduct("Sentinel-3/slstr/S3B_SL_1_RBT____20190603T025704_20190603T030004_20190603T043818_0179_026_089_3060_LN2_O_NR_003.SEN3");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void testSentinel_3_synergy() throws IOException {
        this.product = readProduct("Sentinel-3/synergy/S3A_SY_2_SYN____20130621T100932_20130621T101146_20140604T091546_0134_001_002______LN1_D_NC____.SEN3");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void test_meris() throws IOException {
        this.product = readProduct("Meris/MER_FRS_2PNMAP20090716_111956_000001632080_00438_38568_0001.N1");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void test_landsat() throws IOException {
        this.product = readProduct("Landsat/LC080270332018121901T1-SC20190129000900.tar.gz");
        assertNotNull(product);
    }

    @Test
    @STTM("SNAP-3712")
    public void test_enmap() throws IOException {
        this.product = readProduct("EnMap/ENMAP01-____L2A-DT0000001049_20220612T105735Z_028_V010303_20230922T131826Z.ZIP");
        assertNotNull(product);
    }

    private Product readProduct(String filename) throws IOException {
        Product product = ProductIO.readProduct(new File(this.testDataDir + filename));
        return product;
    }
}
