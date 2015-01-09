package org.esa.beam.dataio.bigtiff;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    private TIFFField getTiffField(int type, Object data) {
        final TIFFTag tag = new TIFFTag("test", 1, type);
        if (data != null) {
            return new TIFFField(tag, type, 1, data);
        } else {
            return new TIFFField(tag, type, 1);
        }
    }
}
