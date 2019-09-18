package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.support.IsinPlanetaryGrid;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.grid.isin.IsinPoint;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IsinFormatterIntegrationTest {

    private File testDir;
    private IsinFormatter formatter;

    @Before
    public void setUp() {
        final File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "isin_formatter_test");
        if (!testDir.mkdirs()) {
            fail("unable to create test directory");
        }

        formatter = new IsinFormatter();
    }

    @After
    public void tearDown() {
        if (testDir != null){
            FileUtils.deleteTree(testDir);
        }
    }

    @Test
    public void testRunWithSingleTilePixel() throws Exception {
        final FormatterConfig formatterConfig = getFormatterConfig();

        final IsinPlanetaryGrid planetaryGrid = new IsinPlanetaryGrid(18 * 1200);
        final String[] featureNames = {"OGVI_mean", "OGVI_sigma"};

        final ProductData.UTC startDate = ProductData.UTC.create(new Date(1570200000000L), 0);
        final ProductData.UTC endDate = ProductData.UTC.create(new Date(1570400000000L), 0);

        final ArrayList<TemporalBin> binList = new ArrayList<>();
        final long binIndex = IsinPlanetaryGrid.toBinIndex(new IsinPoint(100, 200, 18, 11));
        final TemporalBin bin = new TemporalBin();
        bin.setIndex(binIndex);

        binList.add(bin);
        final BinningOp.SimpleTemporalBinSource binSource = new BinningOp.SimpleTemporalBinSource(binList);

        formatter.format(planetaryGrid, binSource, featureNames, formatterConfig, null, startDate, endDate, null);

        final String expectedProduct = IsinFormatter.getProductName(startDate.getAsDate(), endDate.getAsDate(), 18, 11);
        assertTrue(new File(testDir, expectedProduct).isFile());
    }

    @Test
    public void testRunWithTwoTilesPixels() throws Exception {
        final FormatterConfig formatterConfig = getFormatterConfig();

        final IsinPlanetaryGrid planetaryGrid = new IsinPlanetaryGrid(18 * 2400);
        final String[] featureNames = {"OGVI_mean", "OGVI_sigma", "OGVI_count"};

        final ProductData.UTC startDate = ProductData.UTC.create(new Date(1570300000000L), 0);
        final ProductData.UTC endDate = ProductData.UTC.create(new Date(1570500000000L), 0);
        final ArrayList<TemporalBin> binList = new ArrayList<>();
        long binIndex = IsinPlanetaryGrid.toBinIndex(new IsinPoint(100, 200, 18, 11));
        TemporalBin bin = new TemporalBin();
        bin.setIndex(binIndex);
        binList.add(bin);

        binIndex = IsinPlanetaryGrid.toBinIndex(new IsinPoint(101, 201, 19, 11));
        bin = new TemporalBin();
        bin.setIndex(binIndex);
        binList.add(bin);
        final BinningOp.SimpleTemporalBinSource binSource = new BinningOp.SimpleTemporalBinSource(binList);

        formatter.format(planetaryGrid, binSource, featureNames, formatterConfig, null, startDate, endDate, null);

        String expectedProduct = IsinFormatter.getProductName(startDate.getAsDate(), endDate.getAsDate(), 18, 11);
        assertTrue(new File(testDir, expectedProduct).isFile());

        expectedProduct = IsinFormatter.getProductName(startDate.getAsDate(), endDate.getAsDate(), 19, 11);
        assertTrue(new File(testDir, expectedProduct).isFile());
    }

    private FormatterConfig getFormatterConfig() {
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFile(testDir.getAbsolutePath());
        formatterConfig.setOutputFormat("NetCDF-BEAM");
        return formatterConfig;
    }
}
