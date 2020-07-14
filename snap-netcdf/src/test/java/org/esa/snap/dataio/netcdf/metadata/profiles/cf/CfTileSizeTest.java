package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.netcdf.NetCdfActivator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;


/**
 * @author Marco Peters
 */
public class CfTileSizeTest {

    @BeforeClass
    public static void beforeClass() {
        SystemUtils.init3rdPartyLibs(Object.class);
        NetCdfActivator.activate();
    }

    @Test
    public void testTileSizeIsConsidered() throws Exception {
        CfNetCdfReaderPlugIn plugIn = new CfNetCdfReaderPlugIn();
        ProductReader reader = plugIn.createReaderInstance();
        Product product = reader.readProductNodes(getTestFile(), null);
        java.awt.Dimension preferredTileSize = product.getPreferredTileSize();
        assertEquals(new Dimension(10, 10), preferredTileSize);
    }

    private File getTestFile() throws URISyntaxException {
        URL url = CfTileSizeTest.class.getResource("tileSizeTest_nc4CF.nc");
        return new File(url.toURI());
    }

}