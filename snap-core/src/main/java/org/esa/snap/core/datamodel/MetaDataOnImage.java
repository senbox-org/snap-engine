
package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.Range;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Metadata layer on the image
 *
 * @author Daniel Knowles
 */


public class MetaDataOnImage {



    private final TextGlyph[] _textGlyphsHeader;
    private final TextGlyph[] _textGlyphsFooter;
    private final TextGlyph[] _textGlyphsFooter2;


    public enum TextLocation {
        NORTH,
        SOUTH,
        WEST,
        EAST,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }


    private MetaDataOnImage(TextGlyph[] textGlyphHeader, TextGlyph[] textGlyphFooter, TextGlyph[] textGlyphFooter2) {
        _textGlyphsHeader = textGlyphHeader;
        _textGlyphsFooter = textGlyphFooter;
        _textGlyphsFooter2 = textGlyphFooter2;
    }


    public TextGlyph[] getTextGlyphsHeader() {
        return _textGlyphsHeader;
    }
    public TextGlyph[] get_textGlyphsFooter() {
        return _textGlyphsFooter;
    }
    public TextGlyph[] get_textGlyphsFooter2() {
        return _textGlyphsFooter2;
    }


    /**
     * Creates a metadata layer image for the given product.
     *
     * @param raster the product
     * @return the metadata layer or null, if it could not be created
     */
    public static MetaDataOnImage create(RasterDataNode raster, List<String> headerList, List<String> footerList, List<String> footer2List) {
        Guardian.assertNotNull("product", raster);
        final GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding == null || raster.getRasterWidth() < 16 || raster.getRasterHeight() < 16) {
            return null;
        }

        TextGlyph[] textGlyphHeader = createTextGlyphsHeader(headerList);
        TextGlyph[] textGlyphFooter = createTextGlyphsHeader(footerList);
        TextGlyph[] textGlyphFooter2 = createTextGlyphsHeader(footer2List);

        return new MetaDataOnImage(textGlyphHeader, textGlyphFooter, textGlyphFooter2);
    }






    private static TextGlyph[] createTextGlyphsHeader(List<String> headers) {

        final List<TextGlyph> textGlyphs = new ArrayList<TextGlyph>();

        PixelPos headerPixelPos = new PixelPos(0, 0);

        for (String header : headers) {
            PixelPos headerPixelPos2 = new PixelPos((float) (headerPixelPos.getX()), (float) (headerPixelPos.getY() + 1));
            textGlyphs.add(createHeaderGlyph(header, headerPixelPos, headerPixelPos2));
        }

        return textGlyphs.toArray(new TextGlyph[0]);
    }


    static TextGlyph createHeaderGlyph(String text, PixelPos coord1, PixelPos coord2) {
        final double angle = Math.atan2(coord2.y - coord1.y,
                coord2.x - coord1.x);
        return new TextGlyph(text, coord1.x, coord1.y, angle);
    }



    public static class TextGlyph {

        private final String text;
        private final double x;
        private final double y;
        private final double angle;

        TextGlyph(String text, double x, double y, double angle) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.angle = angle;
        }

        public String getText() {
            return text;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getAngle() {
            return angle;
        }
    }

    static class Coord {
        GeoPos geoPos;
        PixelPos pixelPos;

        public Coord(GeoPos geoPos, PixelPos pixelPos) {
            this.geoPos = geoPos;
            this.pixelPos = pixelPos;
        }
    }

    static class GeoPosLatComparator extends GeoPosComparator {
        @Override
        public int compare(GeoPos geoPos1, GeoPos geoPos2) {
            return getCompare(geoPos1.lat - geoPos2.lat);
        }
    }

    static class GeoPosLonComparator extends GeoPosComparator {
        @Override
        public int compare(GeoPos geoPos1, GeoPos geoPos2) {
            return getCompare(geoPos1.lon - geoPos2.lon);
        }
    }

    abstract static class GeoPosComparator implements Comparator<GeoPos> {

        int getCompare(double delta) {
            if (delta < 0f) {
                return -1;
            } else if (delta > 0f) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}

