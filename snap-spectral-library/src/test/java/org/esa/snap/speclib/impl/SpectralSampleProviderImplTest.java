package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;

import javax.media.jai.PlanarImage;

import java.awt.image.Raster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


public class SpectralSampleProviderImplTest {


    @Test
    @STTM("SNAP-4128")
    public void test_readSample_delegatesToProductUtils() {
        Band band = mock(Band.class);
        SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();

        try (MockedStatic<ProductUtils> mocked = mockStatic(ProductUtils.class)) {
            mocked.when(() -> ProductUtils.getGeophysicalSampleAsDouble(band, 3, 4, 1))
                    .thenReturn(42.0);

            double v = p.readSample(band, 3, 4, 1);
            assertEquals(42.0, v, 0.0);

            mocked.verify(() -> ProductUtils.getGeophysicalSampleAsDouble(band, 3, 4, 1));
        }
    }

    @Test
    @STTM("SNAP-4128")
    public void test_noDataValue_delegatesToBand() {
        Band band = mock(Band.class);
        when(band.getGeophysicalNoDataValue()).thenReturn(-9999.0);

        SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
        assertEquals(-9999.0, p.noDataValue(band), 0.0);

        verify(band).getGeophysicalNoDataValue();
    }

    @Test
    @STTM("SNAP-4128")
    public void test_isPixelValid_returnsTrueIfNoValidMaskUsed() {
        Band band = mock(Band.class);
        when(band.isValidMaskUsed()).thenReturn(false);

        SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
        assertTrue(p.isPixelValid(band, 10, 20, 0));

        verify(band).isValidMaskUsed();
        verifyNoMoreInteractions(band);
    }

    @Test
    @STTM("SNAP-4128")
    public void test_isPixelValid_usesValidMask_sampleNonZero_isTrue() {
        Band band = mock(Band.class);
        when(band.isValidMaskUsed()).thenReturn(true);

        ImageManager mgr = mock(ImageManager.class);
        PlanarImage maskImg = mock(PlanarImage.class);
        Raster tile = mock(Raster.class);

        int x = 10, y = 20, level = 0;
        when(mgr.getValidMaskImage(band, level)).thenReturn(maskImg);
        when(maskImg.XToTileX(x)).thenReturn(1);
        when(maskImg.YToTileY(y)).thenReturn(2);
        when(maskImg.getTile(1, 2)).thenReturn(tile);
        when(tile.getSample(x, y, 0)).thenReturn(1);

        try (MockedStatic<ImageManager> mocked = mockStatic(ImageManager.class)) {
            mocked.when(ImageManager::getInstance).thenReturn(mgr);

            SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
            assertTrue(p.isPixelValid(band, x, y, level));

            mocked.verify(ImageManager::getInstance);
            verify(mgr).getValidMaskImage(band, level);
            verify(maskImg).getTile(1, 2);
            verify(tile).getSample(x, y, 0);
        }
    }

    @Test
    @STTM("SNAP-4128")
    public void test_isPixelValid_usesValidMask_sampleZero_isFalse() {
        Band band = mock(Band.class);
        when(band.isValidMaskUsed()).thenReturn(true);

        ImageManager mgr = mock(ImageManager.class);
        PlanarImage maskImg = mock(PlanarImage.class);
        Raster tile = mock(Raster.class);

        int x = 10, y = 20, level = 0;
        when(mgr.getValidMaskImage(band, level)).thenReturn(maskImg);
        when(maskImg.XToTileX(x)).thenReturn(1);
        when(maskImg.YToTileY(y)).thenReturn(2);
        when(maskImg.getTile(1, 2)).thenReturn(tile);
        when(tile.getSample(x, y, 0)).thenReturn(0);

        try (MockedStatic<ImageManager> mocked = mockStatic(ImageManager.class)) {
            mocked.when(ImageManager::getInstance).thenReturn(mgr);

            SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
            assertFalse(p.isPixelValid(band, x, y, level));
        }
    }
}