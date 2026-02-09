package org.esa.snap.speclib.impl;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.Raster;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


public class SpectralSampleProviderImplTest {


    @Test
    @STTM("SNAP-4128")
    public void test_readSamples_nullBandsListThrows() {
        SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
        try {
            p.readSamples(null, 0, 0, 0);
            fail("expected NullPointerException");
        } catch (NullPointerException ignored) {}
    }

    @Test
    @STTM("SNAP-4128")
    public void test_readSamples_nullBandYieldsNaN() {
        SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
        assertEquals(-9999.0, p.noDataValue(band), 0.0);

        verify(band).getGeophysicalNoDataValue();
    }

    @Test
    @STTM("SNAP-4128")
    public void test_readSamples_invalidMaskPixel_returnsNaN() {
        Band band = Mockito.mock(Band.class);
        Mockito.when(band.isValidMaskUsed()).thenReturn(true);

        ImageManager mgr = Mockito.mock(ImageManager.class);
        PlanarImage img = Mockito.mock(PlanarImage.class);
        Raster raster = Mockito.mock(Raster.class);

        Mockito.when(img.XToTileX(5)).thenReturn(0);
        Mockito.when(img.YToTileY(6)).thenReturn(0);
        Mockito.when(img.getTile(0, 0)).thenReturn(raster);
        Mockito.when(raster.getSample(5, 6, 0)).thenReturn(0);

        Mockito.when(mgr.getValidMaskImage(band, 0)).thenReturn(img);

        try (MockedStatic<ImageManager> im = Mockito.mockStatic(ImageManager.class)) {
            im.when(ImageManager::getInstance).thenReturn(mgr);

            SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
            double[] out = p.readSamples(List.of(band), 5, 6, 0);

            assertTrue(Double.isNaN(out[0]));
        }
    }

    @Test
    @STTM("SNAP-4128")
    public void test_readSamples_nodata_isNaN() {
        Band band = Mockito.mock(Band.class);
        Mockito.when(band.isValidMaskUsed()).thenReturn(false);
        Mockito.when(band.getGeophysicalNoDataValue()).thenReturn(-9999.0);

        try (MockedStatic<ProductUtils> pu = Mockito.mockStatic(ProductUtils.class)) {
            pu.when(() -> ProductUtils.getGeophysicalSampleAsDouble(band, 1, 1, 0)).thenReturn(-9999.0);

            SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
            double[] out = p.readSamples(List.of(band), 1, 1, 0);

            assertTrue(Double.isNaN(out[0]));
        }
    }

    @Test
    @STTM("SNAP-4128")
    public void test_readSamples_nanOrInfinite_isNaN() {
        Band band = Mockito.mock(Band.class);
        Mockito.when(band.isValidMaskUsed()).thenReturn(false);
        Mockito.when(band.getGeophysicalNoDataValue()).thenReturn(-9999.0);

        SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();

        try (MockedStatic<ProductUtils> pu = Mockito.mockStatic(ProductUtils.class)) {
            pu.when(() -> ProductUtils.getGeophysicalSampleAsDouble(band, 1, 1, 0)).thenReturn(Double.NaN);
            assertTrue(Double.isNaN(p.readSamples(List.of(band), 1, 1, 0)[0]));
        }

        try (MockedStatic<ProductUtils> pu = Mockito.mockStatic(ProductUtils.class)) {
            pu.when(() -> ProductUtils.getGeophysicalSampleAsDouble(band, 1, 1, 0)).thenReturn(Double.POSITIVE_INFINITY);
            assertTrue(Double.isNaN(p.readSamples(List.of(band), 1, 1, 0)[0]));
        }
    }

    @Test
    @STTM("SNAP-4128")
    public void test_readSamples_validMaskPixelValid_readsValue() {
        Band band = Mockito.mock(Band.class);
        Mockito.when(band.isValidMaskUsed()).thenReturn(true);
        Mockito.when(band.getGeophysicalNoDataValue()).thenReturn(-9999.0);
        Mockito.when(band.getRasterWidth()).thenReturn(10);
        Mockito.when(band.getRasterHeight()).thenReturn(10);

        ImageManager mgr = Mockito.mock(ImageManager.class);
        PlanarImage img = Mockito.mock(PlanarImage.class);
        Raster raster = Mockito.mock(Raster.class);
        Rectangle bounds = Mockito.mock(Rectangle.class);

        Mockito.when(img.XToTileX(7)).thenReturn(0);
        Mockito.when(img.YToTileY(8)).thenReturn(0);
        Mockito.when(img.getTile(0, 0)).thenReturn(raster);
        Mockito.when(raster.getBounds()).thenReturn(bounds);
        Mockito.when(bounds.contains(7,8)).thenReturn(true);
        Mockito.when(raster.getSample(7, 8, 0)).thenReturn(1);

        Mockito.when(mgr.getValidMaskImage(band, 0)).thenReturn(img);

        try (MockedStatic<ImageManager> im = Mockito.mockStatic(ImageManager.class);
             MockedStatic<ProductUtils> pu = Mockito.mockStatic(ProductUtils.class)) {

            im.when(ImageManager::getInstance).thenReturn(mgr);
            pu.when(() -> ProductUtils.getGeophysicalSampleAsDouble(band, 7, 8, 0)).thenReturn(1.23);

            SpectralSampleProviderImpl p = new SpectralSampleProviderImpl();
            double[] out = p.readSamples(List.of(band), 7, 8, 0);

            assertEquals(1.23, out[0], 1e-12);
        }
    }
}