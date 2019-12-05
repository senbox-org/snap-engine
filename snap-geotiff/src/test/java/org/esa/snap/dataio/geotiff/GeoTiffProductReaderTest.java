package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.dataio.ProductReader;
import org.junit.Before;
import org.junit.Test;

public class GeoTiffProductReaderTest {

    private ProductReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new GeoTiffProductReader(new GeoTiffProductReaderPlugIn());
    }

    // TODO: add tests!
    @Test
    public void testSomething() {
    }
}
