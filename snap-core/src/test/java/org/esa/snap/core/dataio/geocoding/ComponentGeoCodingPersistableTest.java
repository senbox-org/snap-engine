package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.esa.snap.core.datamodel.Product;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistable.*;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ComponentGeoCodingPersistableTest {

    private DimapPersistable persistable;
    private Product product;

    private static final String LS = System.lineSeparator();
    private static String EXPECTED_GEO_CRS = "GEOGCS[\"WGS84(DD)\", " + LS +
            "  DATUM[\"WGS84\", " + LS +
            "    SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], " + LS +
            "  PRIMEM[\"Greenwich\", 0.0], " + LS +
            "  UNIT[\"degree\", 0.017453292519943295], " + LS +
            "  AXIS[\"Geodetic longitude\", EAST], " + LS +
            "  AXIS[\"Geodetic latitude\", NORTH]]";

    @Before
    public void setUp() throws Exception {
        persistable = new ComponentGeoCodingPersistableSpi().createPersistable();
        product = createProduct();
    }

    @Test
    public void testToAndFromXML_withBands() {
        final boolean interpolating = false;
        final boolean quadTree = false;
        final boolean antimeridian = true;
        final ComponentGeoCoding initialGeocoding = initializeWithBands(product, interpolating, quadTree, antimeridian);
        assertThat(initialGeocoding.isCrossingMeridianAt180(), is(true));

        final Element xmlFromObject = persistable.createXmlFromObject(initialGeocoding);

        assertThat(xmlFromObject, is(notNullValue()));
        assertThat(xmlFromObject.getName(), is(equalTo(TAG_COMPONENT_GEO_CODING)));
        assertThat(xmlFromObject.getAttributes().size(), is(0));
        assertThat(xmlFromObject.getChildren().size(), is(11));

        assertThat(xmlFromObject.getChild(TAG_FORWARD_CODING_KEY), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_INVERSE_CODING_KEY), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_GEO_CHECKS), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_GEO_CRS), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_LON_VARIABLE_NAME), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_LAT_VARIABLE_NAME), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_RASTER_RESOLUTION_KM), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_OFFSET_X), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_OFFSET_Y), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_SUBSAMPLING_X), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_SUBSAMPLING_Y), is(notNullValue()));

        assertThat(xmlFromObject.getChildTextTrim(TAG_FORWARD_CODING_KEY), is("FWD_PIXEL"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_INVERSE_CODING_KEY), is("INV_PIXEL_GEO_INDEX"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_GEO_CHECKS), is("ANTIMERIDIAN"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_GEO_CRS), is(EXPECTED_GEO_CRS));
        assertThat(xmlFromObject.getChildTextTrim(TAG_LON_VARIABLE_NAME), is("Lon"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_LAT_VARIABLE_NAME), is("Lat"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_RASTER_RESOLUTION_KM), is("300.0"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_OFFSET_X), is("0.5"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_OFFSET_Y), is("0.5"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_SUBSAMPLING_X), is("1.0"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_SUBSAMPLING_Y), is("1.0"));

        final Element generalGeoCodingElem = new Element("parent");
        generalGeoCodingElem.addContent(xmlFromObject);

        assertThat(product.getSceneGeoCoding(), is(notNullValue()));
        product.setSceneGeoCoding(null);
        assertThat(product.getSceneGeoCoding(), is(nullValue()));

        final Object objectFromXml = persistable.createObjectFromXml(generalGeoCodingElem, product, null);

        assertThat(objectFromXml, is(instanceOf(ComponentGeoCoding.class)));
        final ComponentGeoCoding newGeoCoding = (ComponentGeoCoding) objectFromXml;
        assertNotSame(newGeoCoding, initialGeocoding);
        assertThat(newGeoCoding.getForwardCoding().getKey(), is(equalTo(initialGeocoding.getForwardCoding().getKey())));
        assertThat(newGeoCoding.getInverseCoding().getKey(), is(equalTo(initialGeocoding.getInverseCoding().getKey())));
        assertThat(newGeoCoding.getGeoChecks().name(), is(equalTo(initialGeocoding.getGeoChecks().name())));
        assertThat(newGeoCoding.getGeoCRS().toWKT(), is(equalTo(initialGeocoding.getGeoCRS().toWKT())));
        assertThat(newGeoCoding.isCrossingMeridianAt180(), is(true));

        final GeoRaster initialGeoRaster = initialGeocoding.getGeoRaster();
        final GeoRaster newGeoRaster = newGeoCoding.getGeoRaster();
        assertThat(newGeoRaster.getRasterWidth(), is(equalTo(initialGeoRaster.getRasterWidth())));
        assertThat(newGeoRaster.getRasterHeight(), is(equalTo(initialGeoRaster.getRasterHeight())));
        assertThat(newGeoRaster.getRasterResolutionInKm(), is(equalTo(initialGeoRaster.getRasterResolutionInKm())));
        assertThat(newGeoRaster.getLonVariableName(), is(equalTo(initialGeoRaster.getLonVariableName())));
        assertThat(newGeoRaster.getLatVariableName(), is(equalTo(initialGeoRaster.getLatVariableName())));
        assertThat(newGeoRaster.getSceneWidth(), is(equalTo(initialGeoRaster.getSceneWidth())));
        assertThat(newGeoRaster.getSceneHeight(), is(equalTo(initialGeoRaster.getSceneHeight())));
        assertThat(newGeoRaster.getSubsamplingX(), is(equalTo(initialGeoRaster.getSubsamplingX())));
        assertThat(newGeoRaster.getSubsamplingY(), is(equalTo(initialGeoRaster.getSubsamplingY())));
        assertThat(newGeoRaster.getOffsetX(), is(equalTo(initialGeoRaster.getOffsetX())));
        assertThat(newGeoRaster.getOffsetY(), is(equalTo(initialGeoRaster.getOffsetY())));
        assertNotSame(newGeoRaster.getLongitudes(), initialGeoRaster.getLongitudes());
        assertArrayEquals(newGeoRaster.getLongitudes(), initialGeoRaster.getLongitudes(), Double.MIN_VALUE);
        assertNotSame(newGeoRaster.getLatitudes(), initialGeoRaster.getLatitudes());
        assertArrayEquals(newGeoRaster.getLatitudes(), initialGeoRaster.getLatitudes(), Double.MIN_VALUE);
    }

    @Test
    public void testToAndFromXML_withTiePointGrids() {
        final boolean bilinear = true;
        final boolean antimeridian = true;
        final ComponentGeoCoding initialGeocoding = initializeWithTiePoints(product, bilinear, antimeridian);
        assertThat(initialGeocoding.isCrossingMeridianAt180(), is(true));

        final Element xmlFromObject = persistable.createXmlFromObject(initialGeocoding);

        assertThat(xmlFromObject, is(notNullValue()));
        assertThat(xmlFromObject.getName(), is(equalTo(TAG_COMPONENT_GEO_CODING)));
        assertThat(xmlFromObject.getAttributes().size(), is(0));
        assertThat(xmlFromObject.getChildren().size(), is(11));

        assertThat(xmlFromObject.getChild(TAG_FORWARD_CODING_KEY), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_INVERSE_CODING_KEY), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_GEO_CHECKS), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_GEO_CRS), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_LON_VARIABLE_NAME), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_LAT_VARIABLE_NAME), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_RASTER_RESOLUTION_KM), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_OFFSET_X), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_OFFSET_Y), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_SUBSAMPLING_X), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_SUBSAMPLING_Y), is(notNullValue()));

        assertThat(xmlFromObject.getChildTextTrim(TAG_FORWARD_CODING_KEY), is("FWD_TIE_POINT_BILINEAR"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_INVERSE_CODING_KEY), is("INV_TIE_POINT"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_GEO_CHECKS), is("ANTIMERIDIAN"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_GEO_CRS), is(EXPECTED_GEO_CRS));
        assertThat(xmlFromObject.getChildTextTrim(TAG_LON_VARIABLE_NAME), is("tpLon"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_LAT_VARIABLE_NAME), is("tpLat"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_RASTER_RESOLUTION_KM), is("300.0"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_OFFSET_X), is("0.5"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_OFFSET_Y), is("0.5"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_SUBSAMPLING_X), is("5.0"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_SUBSAMPLING_Y), is("5.0"));

        final Element generalGeoCodingElem = new Element("parent");
        generalGeoCodingElem.addContent(xmlFromObject);

        assertThat(product.getSceneGeoCoding(), is(notNullValue()));
        product.setSceneGeoCoding(null);
        assertThat(product.getSceneGeoCoding(), is(nullValue()));

        final Object objectFromXml = persistable.createObjectFromXml(generalGeoCodingElem, product, null);

        assertThat(objectFromXml, is(instanceOf(ComponentGeoCoding.class)));
        final ComponentGeoCoding newGeoCoding = (ComponentGeoCoding) objectFromXml;
        assertNotSame(newGeoCoding, initialGeocoding);
        assertThat(newGeoCoding.getForwardCoding().getKey(), is(equalTo(initialGeocoding.getForwardCoding().getKey())));
        assertThat(newGeoCoding.getInverseCoding().getKey(), is(equalTo(initialGeocoding.getInverseCoding().getKey())));
        assertThat(newGeoCoding.getGeoChecks().name(), is(equalTo(initialGeocoding.getGeoChecks().name())));
        assertThat(newGeoCoding.getGeoCRS().toWKT(), is(equalTo(initialGeocoding.getGeoCRS().toWKT())));
        assertThat(newGeoCoding.isCrossingMeridianAt180(), is(true));

        final GeoRaster initialGeoRaster = initialGeocoding.getGeoRaster();
        final GeoRaster newGeoRaster = newGeoCoding.getGeoRaster();
        assertThat(newGeoRaster.getRasterWidth(), is(equalTo(initialGeoRaster.getRasterWidth())));
        assertThat(newGeoRaster.getRasterHeight(), is(equalTo(initialGeoRaster.getRasterHeight())));
        assertThat(newGeoRaster.getRasterResolutionInKm(), is(equalTo(initialGeoRaster.getRasterResolutionInKm())));
        assertThat(newGeoRaster.getLonVariableName(), is(equalTo(initialGeoRaster.getLonVariableName())));
        assertThat(newGeoRaster.getLatVariableName(), is(equalTo(initialGeoRaster.getLatVariableName())));
        assertThat(newGeoRaster.getSceneWidth(), is(equalTo(initialGeoRaster.getSceneWidth())));
        assertThat(newGeoRaster.getSceneHeight(), is(equalTo(initialGeoRaster.getSceneHeight())));
        assertThat(newGeoRaster.getSubsamplingX(), is(equalTo(initialGeoRaster.getSubsamplingX())));
        assertThat(newGeoRaster.getSubsamplingY(), is(equalTo(initialGeoRaster.getSubsamplingY())));
        assertThat(newGeoRaster.getOffsetX(), is(equalTo(initialGeoRaster.getOffsetX())));
        assertThat(newGeoRaster.getOffsetY(), is(equalTo(initialGeoRaster.getOffsetY())));
        assertNotSame(newGeoRaster.getLongitudes(), initialGeoRaster.getLongitudes());
        assertArrayEquals(newGeoRaster.getLongitudes(), initialGeoRaster.getLongitudes(), Double.MIN_VALUE);
        assertNotSame(newGeoRaster.getLatitudes(), initialGeoRaster.getLatitudes());
        assertArrayEquals(newGeoRaster.getLatitudes(), initialGeoRaster.getLatitudes(), Double.MIN_VALUE);
    }
}