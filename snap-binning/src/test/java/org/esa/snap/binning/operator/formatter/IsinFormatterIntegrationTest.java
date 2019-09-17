package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.support.IsinPlanetaryGrid;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.grid.isin.IsinPoint;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.fail;

public class IsinFormatterIntegrationTest {

    private File testDir;

    @Before
    public void setUp() {
        final File tempDir = new File(System.getProperty("java.io.tmpdir"));
        testDir = new File(tempDir, "isin_formatter_test");
        if (!testDir.mkdirs()) {
            fail("unable to create test directory");
        }
    }

    @After
    public void tearDown() {
        if (testDir != null){
            FileUtils.deleteTree(testDir);
        }
    }

    @Test
    public void testRunWithSingleTilePixel() throws Exception {
        final IsinFormatter formatter = new IsinFormatter();

        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFile(testDir.getAbsolutePath());
        formatterConfig.setOutputType("Product");

        final ArrayList<TemporalBin> binList = new ArrayList<>();
        final long binIndex = IsinPlanetaryGrid.toBinIndex(new IsinPoint(100, 200, 18, 11));
        final TemporalBin bin = new TemporalBin();
        bin.setIndex(binIndex);
        binList.add(bin);
        final BinningOp.SimpleTemporalBinSource binSource = new BinningOp.SimpleTemporalBinSource(binList);

        final IsinPlanetaryGrid planetaryGrid = new IsinPlanetaryGrid(21600);
        final String[] featureNames = {"mean_man", "stan_dev"};

        formatter.format(planetaryGrid, binSource, featureNames, formatterConfig, null, null, null, null);
    }
}
