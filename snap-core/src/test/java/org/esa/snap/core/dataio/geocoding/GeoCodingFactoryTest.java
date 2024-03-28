package org.esa.snap.core.dataio.geocoding;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import static org.esa.snap.core.dataio.geocoding.MERIS.MER_FSG_LAT;
import static org.esa.snap.core.dataio.geocoding.MERIS.MER_FSG_LON;
import static org.esa.snap.core.dataio.geocoding.OLCI.OLCI_L2_LAT;
import static org.esa.snap.core.dataio.geocoding.OLCI.OLCI_L2_LON;
import static org.esa.snap.core.dataio.geocoding.S3_SYN.SLSTR_OL_LAT;
import static org.esa.snap.core.dataio.geocoding.S3_SYN.SLSTR_OL_LON;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GeoCodingFactoryTest {

    @Test
    @STTM("SNAP-1585")
    public void testMustTranslate() {
        final Rectangle rectangle = new Rectangle(12, 22, 100, 200);
        assertTrue(GeoCodingFactory.mustTranslate(rectangle));

        rectangle.setLocation(0, 22);
        assertTrue(GeoCodingFactory.mustTranslate(rectangle));

        rectangle.setLocation(12, 0);
        assertTrue(GeoCodingFactory.mustTranslate(rectangle));

        rectangle.setLocation(0, 0);
        assertFalse(GeoCodingFactory.mustTranslate(rectangle));

        assertFalse(GeoCodingFactory.mustTranslate(null));
    }

    @Test
    @STTM("SNAP-1585")
    public void testMustSubsample()  {
        assertTrue(GeoCodingFactory.mustSubSample(2, 1));
        assertTrue(GeoCodingFactory.mustSubSample(1, 3));

        assertFalse(GeoCodingFactory.mustSubSample(1, 1));
    }

    @Test
    @STTM("SNAP-1585")
    public void testCreatePixelGeoCoding() throws IOException {
        final Band latBand = mock(Band.class);
        final MultiLevelImage geoPhysLatImage = mock(MultiLevelImage.class);
        final RenderedImage latRenderedImage = mock(RenderedImage.class);
        final Raster latRaster = mock(Raster.class);

        when(latBand.getGeophysicalImage()).thenReturn(geoPhysLatImage);
        when(geoPhysLatImage.getImage(0)).thenReturn(latRenderedImage);
        when(latRenderedImage.getData()).thenReturn(latRaster);
        when(latRaster.getPixels(anyInt(),anyInt(), anyInt(), anyInt(), any(double[].class))).thenReturn(OLCI_L2_LAT);

        final Band lonBand = mock(Band.class);
        final MultiLevelImage geoPhysLonImage = mock(MultiLevelImage.class);
        final RenderedImage lonRenderedImage = mock(RenderedImage.class);
        final Raster lonRaster = mock(Raster.class);

        when(lonBand.getRasterWidth()).thenReturn(32);
        when(lonBand.getRasterHeight()).thenReturn(36);
        when(lonBand.getGeophysicalImage()).thenReturn(geoPhysLonImage);
        when(geoPhysLonImage.getImage(0)).thenReturn(lonRenderedImage);
        when(lonRenderedImage.getData()).thenReturn(lonRaster);
        when(lonRaster.getPixels(anyInt(),anyInt(), anyInt(), anyInt(), any(double[].class))).thenReturn(OLCI_L2_LON);

        final ComponentGeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, 0.3);
        assertNotNull(geoCoding);
        assertTrue(geoCoding.getForwardCoding() instanceof PixelForward);
        assertTrue(geoCoding.getInverseCoding() instanceof PixelQuadTreeInverse);

        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(11.5, 17.5), null);
        assertEquals(66.48079, geoPos.lat, 1e-8);
        assertEquals(-24.146797, geoPos.lon, 1e-8);

        final PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(66.48079, -24.146797), null);
        assertEquals(10.5, pixelPos.x, 1e-8);
        assertEquals(17.5, pixelPos.y, 1e-8);

        verify(latBand, times(1)).loadRasterData();
        verify(lonBand, times(1)).loadRasterData();
        verify(latBand, times(1)).getGeophysicalImage();
        verify(lonBand, times(1)).getGeophysicalImage();
        verify(geoPhysLatImage, times(1)).getImage(0);
        verify(geoPhysLonImage, times(1)).getImage(0);
        verify(latRenderedImage, times(1)).getData();
        verify(lonRenderedImage, times(1)).getData();
    }

    @Test
    @STTM("SNAP-1585")
    public void testCreatePixelGeoCoding_with_estimated_resolution() throws IOException {
        final Band latBand = mock(Band.class);
        final MultiLevelImage geoPhysLatImage = mock(MultiLevelImage.class);
        final RenderedImage latRenderedImage = mock(RenderedImage.class);
        final Raster latRaster = mock(Raster.class);

        when(latBand.getGeophysicalImage()).thenReturn(geoPhysLatImage);
        when(geoPhysLatImage.getImage(0)).thenReturn(latRenderedImage);
        when(latRenderedImage.getData()).thenReturn(latRaster);
        when(latRaster.getPixels(anyInt(),anyInt(), anyInt(), anyInt(), any(double[].class))).thenReturn(OLCI_L2_LAT);

        final Band lonBand = mock(Band.class);
        final MultiLevelImage geoPhysLonImage = mock(MultiLevelImage.class);
        final RenderedImage lonRenderedImage = mock(RenderedImage.class);
        final Raster lonRaster = mock(Raster.class);

        when(lonBand.getRasterWidth()).thenReturn(32);
        when(lonBand.getRasterHeight()).thenReturn(36);
        when(lonBand.getGeophysicalImage()).thenReturn(geoPhysLonImage);
        when(geoPhysLonImage.getImage(0)).thenReturn(lonRenderedImage);
        when(lonRenderedImage.getData()).thenReturn(lonRaster);
        when(lonRaster.getPixels(anyInt(),anyInt(), anyInt(), anyInt(), any(double[].class))).thenReturn(OLCI_L2_LON);

        final ComponentGeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand);
        assertNotNull(geoCoding);
        assertTrue(geoCoding.getForwardCoding() instanceof PixelForward);
        assertTrue(geoCoding.getInverseCoding() instanceof PixelQuadTreeInverse);

        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(11.5, 17.5), null);
        assertEquals(66.48079, geoPos.lat, 1e-8);
        assertEquals(-24.146797, geoPos.lon, 1e-8);

        final PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(66.48079, -24.146797), null);
        assertEquals(10.5, pixelPos.x, 1e-8);
        assertEquals(17.5, pixelPos.y, 1e-8);

        verify(latBand, times(1)).loadRasterData();
        verify(lonBand, times(1)).loadRasterData();
        verify(latBand, times(1)).getGeophysicalImage();
        verify(lonBand, times(1)).getGeophysicalImage();
        verify(geoPhysLatImage, times(1)).getImage(0);
        verify(geoPhysLonImage, times(1)).getImage(0);
        verify(latRenderedImage, times(1)).getData();
        verify(lonRenderedImage, times(1)).getData();
    }

    @Test
    @STTM("SNAP-1585")
    public void testEstimateGroundResolution() {
        double resolutionInKm = GeoCodingFactory.estimateGroundResolutionInKm(MER_FSG_LAT, MER_FSG_LON, 26, 35);
        assertEquals(0.3309646930645129, resolutionInKm, 1e-8);

        resolutionInKm = GeoCodingFactory.estimateGroundResolutionInKm(OLCI_L2_LAT, OLCI_L2_LON, 32, 36);
        assertEquals(0.23993391392740082, resolutionInKm, 1e-8);

        resolutionInKm = GeoCodingFactory.estimateGroundResolutionInKm(SLSTR_OL_LAT, SLSTR_OL_LON, 32, 26);
        assertEquals(0.3121911353533701, resolutionInKm, 1e-8);
    }
}
