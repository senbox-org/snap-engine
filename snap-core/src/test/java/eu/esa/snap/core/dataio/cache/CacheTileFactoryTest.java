package eu.esa.snap.core.dataio.cache;

import org.junit.Test;

import static eu.esa.snap.core.dataio.cache.TileType.TYPE_NETCDF_ARRAY;
import static eu.esa.snap.core.dataio.cache.TileType.TYPE_PRODUCT_DATA;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CacheTileFactoryTest {

    @Test
    public void testCreate_ProductData() {
        CacheTileFactory.initialize(TYPE_PRODUCT_DATA);

        final CacheTile tile = CacheTileFactory.create();
        assertTrue(tile instanceof ProductDataTile);
    }

    @Test
    public void testCreate_NetCDFArray() {
        CacheTileFactory.initialize(TYPE_NETCDF_ARRAY);

        final CacheTile tile = CacheTileFactory.create();
        assertTrue(tile instanceof NCArrayTile);
    }

    @Test
    public void testCreate_notInitialized() {
        CacheTileFactory.initialize(null);
        try {
            CacheTileFactory.create();
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }
}
