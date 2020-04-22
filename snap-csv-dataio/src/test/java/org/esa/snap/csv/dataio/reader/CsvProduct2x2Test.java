package org.esa.snap.csv.dataio.reader;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(LongTestRunner.class)
public class CsvProduct2x2Test {

    @Test
    public void test2x2Product() throws Exception {
        URL urlDimap = getClass().getResource("MER_FR__1PNUPA20030808_073810_000000982018_00450_07518_6007.dim");
        URI uriDimap = new URI(urlDimap.toString());
        final String dimap = uriDimap.getPath();

        URL urlCsv = getClass().getResource("MER_FR__1PNUPA20030808_073810_000000982018_00450_07518_6007.csv");
        URI uriCsv = new URI(urlCsv.toString());
        final String csv = uriCsv.getPath();

        Product dimPro = ProductIO.readProduct(dimap);
        Product csvPro = ProductIO.readProduct(csv);

        assertEquals("org.esa.snap.core.dataio.dimap.DimapProductReader", dimPro.getProductReader().getClass().getName());
        assertTrue(dimPro.containsBand("new_band_2"));
        assertTrue(dimPro.containsBand("new_band_3"));

        assertEquals("org.esa.snap.csv.dataio.reader.CsvProductReader", csvPro.getProductReader().getClass().getName());
        assertTrue(csvPro.containsBand("new_band_2"));
        assertTrue(csvPro.containsBand("new_band_3"));

        Band dimBand2 = dimPro.getBand("new_band_2");
        Band dimBand3 = dimPro.getBand("new_band_3");
        Band csvBand2 = csvPro.getBand("new_band_2");
        Band csvBand3 = csvPro.getBand("new_band_3");


        for (int y = 0; y < dimBand2.getRasterHeight(); y++) {
            for (int x = 0; x < dimBand2.getRasterWidth(); x++) {
                float[] expecteds = dimBand2.readPixels(x, y, 1, 1, new float[1]);
                float[] actuals = csvBand2.readPixels(x, y, 1, 1, new float[1]);
                assertEquals("new_band_2 value at x="+x+", y="+y, expecteds[0], actuals[0], 0.0001f);

                expecteds = dimBand3.readPixels(x, y, 1, 1, new float[1]);
                actuals = csvBand3.readPixels(x, y, 1, 1, new float[1]);
                assertEquals("new_band_3 value at x=" + x + ", y=" + y, expecteds[0], actuals[0], 0.0001f);
            }
        }
    }
}