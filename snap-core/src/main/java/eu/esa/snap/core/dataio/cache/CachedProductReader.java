package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;

public interface CachedProductReader extends ProductReader {

    CacheTile readCacheTile(Band band, int xOffset, int yOffset, int width, int height);

    StorageDimensions getStorageDimensions(Band band);

}
