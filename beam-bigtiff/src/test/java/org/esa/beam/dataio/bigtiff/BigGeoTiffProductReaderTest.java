package org.esa.beam.dataio.bigtiff;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BigGeoTiffProductReaderTest {

    @Test
    public void testIsAsciiField_isAscii() {
        final TIFFField field = getTiffField(TIFFTag.TIFF_ASCII, null);

        assertTrue(BigGeoTiffProductReader.isAsciiField(field));
    }


    @Test
    public void testIsAsciiField_null() {
        assertFalse(BigGeoTiffProductReader.isAsciiField(null));
    }

    @Test
    public void testIsAsciiField_notAscii() {
        final TIFFField field = getTiffField(TIFFTag.TIFF_DOUBLE, null);

        assertFalse(BigGeoTiffProductReader.isAsciiField(field));
    }

    @Test
    public void testIsDimapField_null() {
        assertFalse(BigGeoTiffProductReader.isDimapField(null));
    }

    @Test
    public void testIsDimapField_isDimap() {
        final TIFFField field = getTiffField(TIFFTag.TIFF_ASCII, new String[]{"<Dimap_Document> blabla"});

        assertTrue(BigGeoTiffProductReader.isDimapField(field));
    }

    @Test
    public void testIsDimapField_notDimap() {
        final TIFFField field = getTiffField(TIFFTag.TIFF_ASCII, new String[]{"<Strange_tag> blabla"});

        assertFalse(BigGeoTiffProductReader.isDimapField(field));
    }

    @Test
    public void testIBadTiling_goodTiling() throws IOException {
        final TIFFImageReader reader = mock(TIFFImageReader.class);
        when(reader.getHeight(0)).thenReturn(20);
        when(reader.getWidth(0)).thenReturn(40);
        when(reader.getTileHeight(0)).thenReturn(10);
        when(reader.getTileWidth(0)).thenReturn(20);

        assertFalse(BigGeoTiffProductReader.isBadTiling(reader));

        when(reader.getHeight(0)).thenReturn(8000);
        when(reader.getWidth(0)).thenReturn(2756);
        when(reader.getTileHeight(0)).thenReturn(199);
        when(reader.getTileWidth(0)).thenReturn(287);

        assertFalse(BigGeoTiffProductReader.isBadTiling(reader));
    }

    @Test
    public void testIBadTiling_badTiling() throws IOException {
        final TIFFImageReader reader = mock(TIFFImageReader.class);
        when(reader.getHeight(0)).thenReturn(20);
        when(reader.getWidth(0)).thenReturn(40);
        when(reader.getTileHeight(0)).thenReturn(1);
        when(reader.getTileWidth(0)).thenReturn(20);

        assertTrue(BigGeoTiffProductReader.isBadTiling(reader));

        when(reader.getHeight(0)).thenReturn(20);
        when(reader.getWidth(0)).thenReturn(40);
        when(reader.getTileHeight(0)).thenReturn(10);
        when(reader.getTileWidth(0)).thenReturn(1);

        assertTrue(BigGeoTiffProductReader.isBadTiling(reader));

        when(reader.getHeight(0)).thenReturn(20);
        when(reader.getWidth(0)).thenReturn(40);
        when(reader.getTileHeight(0)).thenReturn(20);
        when(reader.getTileWidth(0)).thenReturn(20);

        assertTrue(BigGeoTiffProductReader.isBadTiling(reader));

        when(reader.getHeight(0)).thenReturn(20);
        when(reader.getWidth(0)).thenReturn(40);
        when(reader.getTileHeight(0)).thenReturn(10);
        when(reader.getTileWidth(0)).thenReturn(40);

        assertTrue(BigGeoTiffProductReader.isBadTiling(reader));
    }


    private TIFFField getTiffField(int type, Object data) {
        final TIFFTag tag = new TIFFTag("test", 1, type);
        if (data != null) {
            return new TIFFField(tag, type, 1, data);
        } else {
            return new TIFFField(tag, type, 1);
        }
    }
}
