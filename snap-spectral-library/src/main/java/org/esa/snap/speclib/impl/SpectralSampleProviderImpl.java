package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.speclib.api.SpectralSampleProvider;

import javax.media.jai.PlanarImage;
import java.awt.image.Raster;
import java.util.Objects;


public class SpectralSampleProviderImpl implements SpectralSampleProvider {


    @Override
    public double readSample(Band band, int x, int y, int level) {
        Objects.requireNonNull(band, "band must not be null");
        return ProductUtils.getGeophysicalSampleAsDouble(band, x, y, level);
    }

    @Override
    public double noDataValue(Band band) {
        Objects.requireNonNull(band, "band must not be null");
        return band.getGeophysicalNoDataValue();
    }

    @Override
    public boolean isPixelValid(Band band, int x, int y, int level) {
        Objects.requireNonNull(band, "band must not be null");
        if (!band.isValidMaskUsed()) {
            return true;
        }
        PlanarImage image = ImageManager.getInstance().getValidMaskImage(band, level);
        Raster tile = image.getTile(image.XToTileX(x), image.YToTileY(y));
        return tile.getSample(x, y, 0) != 0;
    }
}
