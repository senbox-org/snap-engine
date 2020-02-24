package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.esa.snap.core.datamodel.Product;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersitable.*;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ComponentGeoCodingPersitableTest {

    private DimapPersistable persistable;
    private Product product;

    @Before
    public void setUp() throws Exception {
        persistable = new ComponentGeoCodingPersitableSpi().createPersistable();
        product = createProduct();
    }

    @Test
    public void testToAndFromXML() {
        final ComponentGeoCoding initialGeocoding = initializeWithBands(product, false, false, false);

        final Element xmlFromObject = persistable.createXmlFromObject(initialGeocoding);

        assertThat(xmlFromObject, is(notNullValue()));
        assertThat(xmlFromObject.getName(), is(equalTo(TAG_COMPONENT_GEO_CODING)));
        assertThat(xmlFromObject.getAttributes().size(), is(0));
        assertThat(xmlFromObject.getChildren().size(), is(7));

        assertThat(xmlFromObject.getChild(TAG_FORWARD_CODING_KEY), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_INVERSE_CODING_KEY), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_GEO_CHECKS), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_GEO_CRS), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_LON_VARIABLE_NAME), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_LAT_VARIABLE_NAME), is(notNullValue()));
        assertThat(xmlFromObject.getChild(TAG_RASTER_RESOLUTION_KM), is(notNullValue()));

        assertThat(xmlFromObject.getChildTextTrim(TAG_FORWARD_CODING_KEY), is("FWD_PIXEL"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_INVERSE_CODING_KEY), is("INV_PIXEL_GEO_INDEX"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_GEO_CHECKS), is("ANTIMERIDIAN"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_GEO_CRS), is("GEOGCS[\"WGS84(DD)\", \r\n  DATUM[\"WGS84\", \r\n    SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], \r\n  PRIMEM[\"Greenwich\", 0.0], \r\n  UNIT[\"degree\", 0.017453292519943295], \r\n  AXIS[\"Geodetic longitude\", EAST], \r\n  AXIS[\"Geodetic latitude\", NORTH]]"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_LON_VARIABLE_NAME), is("Lon"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_LAT_VARIABLE_NAME), is("Lat"));
        assertThat(xmlFromObject.getChildTextTrim(TAG_RASTER_RESOLUTION_KM), is("300.0"));

        final Element generalGeoCodingElem = new Element("parent");
        generalGeoCodingElem.addContent(xmlFromObject);

        assertThat(product.getSceneGeoCoding(), is(notNullValue()));
        product.setSceneGeoCoding(null);
        assertThat(product.getSceneGeoCoding(), is(nullValue()));

        final Object objectFromXml = persistable.createObjectFromXml(generalGeoCodingElem, product);

        assertThat(objectFromXml, is(instanceOf(ComponentGeoCoding.class)));
        final ComponentGeoCoding newGeoCoding = (ComponentGeoCoding) objectFromXml;
        assertNotSame(newGeoCoding, initialGeocoding);
        assertThat(newGeoCoding.getForwardCoding().getKey(), is(equalTo(initialGeocoding.getForwardCoding().getKey())));
        assertThat(newGeoCoding.getInverseCoding().getKey(), is(equalTo(initialGeocoding.getInverseCoding().getKey())));
        assertThat(newGeoCoding.getGeoChecks().name(), is(equalTo(initialGeocoding.getGeoChecks().name())));
        assertThat(newGeoCoding.getGeoCRS().toWKT(), is(equalTo(initialGeocoding.getGeoCRS().toWKT())));

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