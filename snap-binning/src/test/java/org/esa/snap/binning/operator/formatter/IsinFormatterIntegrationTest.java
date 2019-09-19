package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.support.IsinPlanetaryGrid;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.grid.isin.IsinPoint;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IsinFormatterIntegrationTest {

    private File testDir;
    private IsinFormatter formatter;
    private final int[] shape;

    public IsinFormatterIntegrationTest() {
        shape = new int[]{1, 1};
    }

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

        final IsinPlanetaryGrid planetaryGrid = new IsinPlanetaryGrid(18 * 2400);
        final String[] featureNames = {"OGVI_mean", "OGVI_sigma"};

        final ProductData.UTC startDate = ProductData.UTC.create(new Date(1570200000000L), 0);
        final ProductData.UTC endDate = ProductData.UTC.create(new Date(1570400000000L), 0);

        final ArrayList<TemporalBin> binList = new ArrayList<>();
        final long binIndex = IsinPlanetaryGrid.toBinIndex(new IsinPoint(100, 200, 18, 11));
        binList.add(createBin(binIndex, new float[]{11.8f, 12.9f}));
        final BinningOp.SimpleTemporalBinSource binSource = new BinningOp.SimpleTemporalBinSource(binList);

        formatter.format(planetaryGrid, binSource, featureNames, formatterConfig, null, startDate, endDate, null);

        final String expectedProduct = IsinFormatter.getProductName(startDate.getAsDate(), endDate.getAsDate(), 18, 11);
        final File expectedFile = new File(testDir, expectedProduct);
        assertTrue(expectedFile.isFile());

        final NetcdfFile netcdfFile = NetcdfFileOpener.open(expectedFile);
        try {
            assertVariableValue(Float.NaN, 99, 200, netcdfFile, "OGVI_mean");
            assertVariableValue(11.8f, 100, 200, netcdfFile, "OGVI_mean");
            assertVariableValue(Float.NaN, 101, 200, netcdfFile, "OGVI_mean");

            assertVariableValue(Float.NaN, 100, 199, netcdfFile, "OGVI_sigma");
            assertVariableValue(12.9f, 100, 200, netcdfFile, "OGVI_sigma");
            assertVariableValue(Float.NaN, 100, 201, netcdfFile, "OGVI_sigma");
        }finally {
            netcdfFile.close();
        }
    }

    @Test
    public void testRunWithTwoTilesPixels() throws Exception {
        final FormatterConfig formatterConfig = getFormatterConfig();

        final IsinPlanetaryGrid planetaryGrid = new IsinPlanetaryGrid(18 * 1200);
        final String[] featureNames = {"OGVI_mean", "OGVI_sigma", "OGVI_count"};

        final ProductData.UTC startDate = ProductData.UTC.create(new Date(1570300000000L), 0);
        final ProductData.UTC endDate = ProductData.UTC.create(new Date(1570500000000L), 0);

        final ArrayList<TemporalBin> binList = new ArrayList<>();
        long binIndex = IsinPlanetaryGrid.toBinIndex(new IsinPoint(100, 200, 18, 11));
        binList.add(createBin(binIndex, new float[]{12.8f, 13.9f, 14}));

        binIndex = IsinPlanetaryGrid.toBinIndex(new IsinPoint(101, 201, 19, 11));
        binList.add(createBin(binIndex, new float[]{13.8f, 14.9f, 15}));

        final BinningOp.SimpleTemporalBinSource binSource = new BinningOp.SimpleTemporalBinSource(binList);

        formatter.format(planetaryGrid, binSource, featureNames, formatterConfig, null, startDate, endDate, null);

        String expectedProduct = IsinFormatter.getProductName(startDate.getAsDate(), endDate.getAsDate(), 18, 11);
        File expectedFile = new File(testDir, expectedProduct);
        assertTrue(expectedFile.isFile());
        NetcdfFile netcdfFile = NetcdfFileOpener.open(expectedFile);
        try {
            assertVariableValue(Float.NaN, 99, 200, netcdfFile, "OGVI_mean");
            assertVariableValue(12.8f, 100, 200, netcdfFile, "OGVI_mean");
            assertVariableValue(Float.NaN, 101, 200, netcdfFile, "OGVI_mean");

            assertVariableValue(Float.NaN, 100, 199, netcdfFile, "OGVI_sigma");
            assertVariableValue(13.9f, 100, 200, netcdfFile, "OGVI_sigma");
            assertVariableValue(Float.NaN, 100, 201, netcdfFile, "OGVI_sigma");

            assertVariableValue(Integer.MIN_VALUE, 100, 199, netcdfFile, "OGVI_count");
            assertVariableValue(14, 100, 200, netcdfFile, "OGVI_count");
            assertVariableValue(Integer.MIN_VALUE, 100, 201, netcdfFile, "OGVI_count");
        }finally {
            netcdfFile.close();
        }

        expectedProduct = IsinFormatter.getProductName(startDate.getAsDate(), endDate.getAsDate(), 19, 11);
        expectedFile = new File(testDir, expectedProduct);
        assertTrue(expectedFile.isFile());
        netcdfFile = NetcdfFileOpener.open(expectedFile);
        try {
            assertVariableValue(Float.NaN, 100, 201, netcdfFile, "OGVI_mean");
            assertVariableValue(13.8f, 101, 201, netcdfFile, "OGVI_mean");
            assertVariableValue(Float.NaN, 102, 201, netcdfFile, "OGVI_mean");

            assertVariableValue(Float.NaN, 101, 200, netcdfFile, "OGVI_sigma");
            assertVariableValue(14.9f, 101, 201, netcdfFile, "OGVI_sigma");
            assertVariableValue(Float.NaN, 101, 202, netcdfFile, "OGVI_sigma");

            assertVariableValue(Integer.MIN_VALUE, 101, 200, netcdfFile, "OGVI_count");
            assertVariableValue(15, 101, 201, netcdfFile, "OGVI_count");
            assertVariableValue(Integer.MIN_VALUE, 101, 202, netcdfFile, "OGVI_count");
        }finally {
            netcdfFile.close();
        }
    }

    private FormatterConfig getFormatterConfig() {
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFile(testDir.getAbsolutePath());
        formatterConfig.setOutputFormat("NetCDF-BEAM");
        return formatterConfig;
    }


    private TemporalBin createBin(long binIndex, float[] values) {
        final TemporalBin bin = new TemporalBin();
        bin.setIndex(binIndex);
        bin.setNumFeatures(values.length);
        final float[] featureValues = bin.getFeatureValues();
        for (int i = 0; i < values.length; i++) {
            featureValues[i] = values[i];
        }
        return bin;
    }

    private void assertVariableValue(float expected, int x, int y, NetcdfFile netcdfFile, String variableName) throws IOException, InvalidRangeException {
        final Variable ogvi_mean = netcdfFile.findVariable(variableName);
        final int[] origin = {y, x};
        final Array ogvi_mean_data = ogvi_mean.read(origin, shape);
        assertEquals(expected, ogvi_mean_data.getFloat(0), 1e-8);
    }
}
