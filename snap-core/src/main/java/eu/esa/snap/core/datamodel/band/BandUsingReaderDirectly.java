package eu.esa.snap.core.datamodel.band;

import org.esa.snap.core.datamodel.Band;

public class BandUsingReaderDirectly extends Band {


    /**
     * Constructs a new <code>Band</code>.
     *
     * @param name     the name of the new object
     * @param dataType the raster data type, must be one of the multiple <code>ProductData.TYPE_<i>X</i></code>
     *                 constants, with the exception of <code>ProductData.TYPE_UINT32</code>
     * @param width    the width of the raster in pixels
     * @param height   the height of the raster in pixels
     */
    public BandUsingReaderDirectly(String name, int dataType, int width, int height) {
        super(name, dataType, width, height);
    }

    @Override
    public boolean isProductReaderDirectlyUsable() {
        return true;
    }
}
