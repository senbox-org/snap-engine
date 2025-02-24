package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.datamodel.Product;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_COMPONENT_GEO_CODING;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_FORWARD_CODING_KEY;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_GEO_CHECKS;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_GEO_CRS;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_INVERSE_CODING_KEY;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_LAT_VARIABLE_NAME;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_LON_VARIABLE_NAME;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_OFFSET_X;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_OFFSET_Y;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_RASTER_RESOLUTION_KM;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_SUBSAMPLING_X;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_SUBSAMPLING_Y;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.createProduct;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.initializeWithBands;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.initializeWithTiePoints;
import static org.esa.snap.core.dataio.persistence.PersistenceDecoder.KEY_PERSISTENCE_ID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @author Sabine Embacher
 */
public class ComponentGeoCodingPersistenceConverterTest {

    private ComponentGeoCodingPersistenceConverter converter;
    private Product product;

    private static final String LS = System.lineSeparator();
    private static final String EXPECTED_GEO_CRS = "GEOGCS[\"WGS84(DD)\", " + LS +
                                                   "  DATUM[\"WGS84\", " + LS +
                                                   "    SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], " + LS +
                                                   "  PRIMEM[\"Greenwich\", 0.0], " + LS +
                                                   "  UNIT[\"degree\", 0.017453292519943295], " + LS +
                                                   "  AXIS[\"Geodetic longitude\", EAST], " + LS +
                                                   "  AXIS[\"Geodetic latitude\", NORTH], " + LS +
                                                   "  AUTHORITY[\"EPSG\",\"4326\"]]";

    @Before
    public void setUp() throws Exception {
        converter = new ComponentGeoCodingPersistenceConverter();
        product = createProduct();
    }

    @Test
    public void testHistoricalDecoders() {
        final HistoricalDecoder[] historicalDecoders = converter.getHistoricalDecoders();

        assertThat(historicalDecoders, isNotNull());
        assertThat(historicalDecoders.length, is(1));
        assertThat(historicalDecoders[0], is(instanceOf(DimapHistoricalDecoder.class)));
    }

    @Test
    public void testToAndFromXML_withBands() {
        final boolean interpolating = false;
        final boolean quadTree = false;
        final boolean antimeridian = true;
        final ComponentGeoCoding initialGeocoding = initializeWithBands(product, interpolating, quadTree, antimeridian);
        assertThat(initialGeocoding.isCrossingMeridianAt180(), is(true));

        final Item itemFromObject = converter.encode(initialGeocoding);

        assertThat(itemFromObject, isNotNull());
        assertThat(itemFromObject.getName(), is(equalTo(NAME_COMPONENT_GEO_CODING)));
        assertThat(itemFromObject.isContainer(), is(true));
        Container container = itemFromObject.asContainer();
        assertThat(container.getAttributes().length, is(0));
        assertThat(container.getProperties().length, is(12));

        assertThat(container.getProperty(KEY_PERSISTENCE_ID), isNotNull());
        assertThat(container.getProperty(NAME_FORWARD_CODING_KEY), isNotNull());
        assertThat(container.getProperty(NAME_INVERSE_CODING_KEY), isNotNull());
        assertThat(container.getProperty(NAME_GEO_CHECKS), isNotNull());
        assertThat(container.getProperty(NAME_GEO_CRS), isNotNull());
        assertThat(container.getProperty(NAME_LON_VARIABLE_NAME), isNotNull());
        assertThat(container.getProperty(NAME_LAT_VARIABLE_NAME), isNotNull());
        assertThat(container.getProperty(NAME_RASTER_RESOLUTION_KM), isNotNull());
        assertThat(container.getProperty(NAME_OFFSET_X), isNotNull());
        assertThat(container.getProperty(NAME_OFFSET_Y), isNotNull());
        assertThat(container.getProperty(NAME_SUBSAMPLING_X), isNotNull());
        assertThat(container.getProperty(NAME_SUBSAMPLING_Y), isNotNull());

        assertThat(container.getProperty(KEY_PERSISTENCE_ID).getValueString(), is(converter.getID()));
        assertThat(container.getProperty(NAME_FORWARD_CODING_KEY).getValueString(), is("FWD_PIXEL"));
        assertThat(container.getProperty(NAME_INVERSE_CODING_KEY).getValueString(), is("INV_PIXEL_GEO_INDEX"));
        assertThat(container.getProperty(NAME_GEO_CHECKS).getValueString(), is("ANTIMERIDIAN"));
        assertThat(container.getProperty(NAME_GEO_CRS).getValueString(), is(EXPECTED_GEO_CRS));
        assertThat(container.getProperty(NAME_LON_VARIABLE_NAME).getValueString(), is("Lon"));
        assertThat(container.getProperty(NAME_LAT_VARIABLE_NAME).getValueString(), is("Lat"));
        assertThat(container.getProperty(NAME_RASTER_RESOLUTION_KM).getValueString(), is("300.0"));
        assertThat(container.getProperty(NAME_OFFSET_X).getValueString(), is("0.5"));
        assertThat(container.getProperty(NAME_OFFSET_Y).getValueString(), is("0.5"));
        assertThat(container.getProperty(NAME_SUBSAMPLING_X).getValueString(), is("1.0"));
        assertThat(container.getProperty(NAME_SUBSAMPLING_Y).getValueString(), is("1.0"));

        assertThat(product.getSceneGeoCoding(), isNotNull());
        product.setSceneGeoCoding(null);
        assertThat(product.getSceneGeoCoding(), isNull());

        // remove ID ... make container a historical version
        container.removeProperty(KEY_PERSISTENCE_ID);
        assertThat(container.getProperty(KEY_PERSISTENCE_ID), isNull());
        final HistoricalDecoder preHistoricalDecoder = converter.getHistoricalDecoders()[0];
        assertThat(preHistoricalDecoder.canDecode(container), is(true));
        container = preHistoricalDecoder.decode(container, null).asContainer();
        final Property<?> persistenceIdProp = container.getProperty(KEY_PERSISTENCE_ID);
        assertThat(persistenceIdProp, isNotNull());
        assertThat(persistenceIdProp.getValueString(), is(converter.getID()));

        // continue with item decoded by historical decoder
        final ComponentGeoCoding newGeoCoding = converter.decode(container, product);

        assertThat(newGeoCoding, is(instanceOf(ComponentGeoCoding.class)));
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
        assertArrayEquals(newGeoRaster.getLongitudes(), initialGeoRaster.getLongitudes(), Double.MIN_VALUE);
        assertArrayEquals(newGeoRaster.getLatitudes(), initialGeoRaster.getLatitudes(), Double.MIN_VALUE);
    }

    @Test
    public void testToAndFromXML_withTiePointGrids_withSameOffsetAndSubsampling() {
        final boolean bilinear = true;
        final boolean antimeridian = true;
        final ComponentGeoCoding initialGeocoding = initializeWithTiePoints(product, bilinear, antimeridian, 0.5, 0.5, 5, 5);
        assertThat(initialGeocoding.isCrossingMeridianAt180(), is(true));

        final Item itemFromObject = converter.encode(initialGeocoding);

        assertThat(itemFromObject, isNotNull());
        assertThat(itemFromObject.getName(), is(equalTo(NAME_COMPONENT_GEO_CODING)));
        assertThat(itemFromObject.isContainer(), is(true));
        Container container = itemFromObject.asContainer();
        assertThat(container.getAttributes().length, is(0));
        assertThat(container.getProperties().length, is(12));

        assertThat(container.getProperty(KEY_PERSISTENCE_ID), isNotNull());
        assertThat(container.getProperty(NAME_FORWARD_CODING_KEY), isNotNull());
        assertThat(container.getProperty(NAME_INVERSE_CODING_KEY), isNotNull());
        assertThat(container.getProperty(NAME_GEO_CHECKS), isNotNull());
        assertThat(container.getProperty(NAME_GEO_CRS), isNotNull());
        assertThat(container.getProperty(NAME_LON_VARIABLE_NAME), isNotNull());
        assertThat(container.getProperty(NAME_LAT_VARIABLE_NAME), isNotNull());
        assertThat(container.getProperty(NAME_RASTER_RESOLUTION_KM), isNotNull());
        assertThat(container.getProperty(NAME_OFFSET_X), isNotNull());
        assertThat(container.getProperty(NAME_OFFSET_Y), isNotNull());
        assertThat(container.getProperty(NAME_SUBSAMPLING_X), isNotNull());
        assertThat(container.getProperty(NAME_SUBSAMPLING_Y), isNotNull());

        assertThat(container.getProperty(KEY_PERSISTENCE_ID).getValueString(), is(converter.getID()));
        assertThat(container.getProperty(NAME_FORWARD_CODING_KEY).getValueString(), is("FWD_TIE_POINT_BILINEAR"));
        assertThat(container.getProperty(NAME_INVERSE_CODING_KEY).getValueString(), is("INV_TIE_POINT"));
        assertThat(container.getProperty(NAME_GEO_CHECKS).getValueString(), is("ANTIMERIDIAN"));
        assertThat(container.getProperty(NAME_GEO_CRS).getValueString(), is(EXPECTED_GEO_CRS));
        assertThat(container.getProperty(NAME_LON_VARIABLE_NAME).getValueString(), is("tpLon"));
        assertThat(container.getProperty(NAME_LAT_VARIABLE_NAME).getValueString(), is("tpLat"));
        assertThat(container.getProperty(NAME_RASTER_RESOLUTION_KM).getValueString(), is("300.0"));
        assertThat(container.getProperty(NAME_OFFSET_X).getValueString(), is("0.5"));
        assertThat(container.getProperty(NAME_OFFSET_Y).getValueString(), is("0.5"));
        assertThat(container.getProperty(NAME_SUBSAMPLING_X).getValueString(), is("5.0"));
        assertThat(container.getProperty(NAME_SUBSAMPLING_Y).getValueString(), is("5.0"));

        assertThat(product.getSceneGeoCoding(), isNotNull());
        product.setSceneGeoCoding(null);
        assertThat(product.getSceneGeoCoding(), isNull());

        // remove ID ... make container a historical version
        container.removeProperty(KEY_PERSISTENCE_ID);
        assertThat(container.getProperty(KEY_PERSISTENCE_ID), isNull());
        final HistoricalDecoder preHistoricalDecoder = converter.getHistoricalDecoders()[0];
        assertThat(preHistoricalDecoder.canDecode(container), is(true));
        container = preHistoricalDecoder.decode(container, null).asContainer();
        final Property<?> persistenceIdProp = container.getProperty(KEY_PERSISTENCE_ID);
        assertThat(persistenceIdProp, isNotNull());
        assertThat(persistenceIdProp.getValueString(), is(converter.getID()));

        // continue with item decoded by historical decoder
        final ComponentGeoCoding newGeoCoding = converter.decode(container, product);

        assertThat(newGeoCoding, is(instanceOf(ComponentGeoCoding.class)));
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
    public void testToAndFromXML_withTiePointGrids_withSameDifferentOffsetAndSubsampling() {
        final boolean bilinear = true;
        final boolean antimeridian = true;
        // for a band 3 times bigger, than the actual band of the product.
        final ComponentGeoCoding initialGeocoding = initializeWithTiePoints(product, bilinear, antimeridian, 1.5, 1.5, 15, 15);
        assertThat(initialGeocoding.isCrossingMeridianAt180(), is(true));

        final Item itemFromObject = converter.encode(initialGeocoding);

        assertThat(itemFromObject, isNotNull());
        assertThat(itemFromObject.getName(), is(equalTo(NAME_COMPONENT_GEO_CODING)));
        assertThat(itemFromObject.isContainer(), is(true));
        Container container = itemFromObject.asContainer();
        assertThat(container.getAttributes().length, is(0));
        assertThat(container.getProperties().length, is(12));

        assertThat(container.getProperty(KEY_PERSISTENCE_ID), isNotNull());
        assertThat(container.getProperty(NAME_FORWARD_CODING_KEY), isNotNull());
        assertThat(container.getProperty(NAME_INVERSE_CODING_KEY), isNotNull());
        assertThat(container.getProperty(NAME_GEO_CHECKS), isNotNull());
        assertThat(container.getProperty(NAME_GEO_CRS), isNotNull());
        assertThat(container.getProperty(NAME_LON_VARIABLE_NAME), isNotNull());
        assertThat(container.getProperty(NAME_LAT_VARIABLE_NAME), isNotNull());
        assertThat(container.getProperty(NAME_RASTER_RESOLUTION_KM), isNotNull());
        assertThat(container.getProperty(NAME_OFFSET_X), isNotNull());
        assertThat(container.getProperty(NAME_OFFSET_Y), isNotNull());
        assertThat(container.getProperty(NAME_SUBSAMPLING_X), isNotNull());
        assertThat(container.getProperty(NAME_SUBSAMPLING_Y), isNotNull());

        assertThat(container.getProperty(KEY_PERSISTENCE_ID).getValueString(), is(converter.getID()));
        assertThat(container.getProperty(NAME_FORWARD_CODING_KEY).getValueString(), is("FWD_TIE_POINT_BILINEAR"));
        assertThat(container.getProperty(NAME_INVERSE_CODING_KEY).getValueString(), is("INV_TIE_POINT"));
        assertThat(container.getProperty(NAME_GEO_CHECKS).getValueString(), is("ANTIMERIDIAN"));
        assertThat(container.getProperty(NAME_GEO_CRS).getValueString(), is(EXPECTED_GEO_CRS));
        assertThat(container.getProperty(NAME_LON_VARIABLE_NAME).getValueString(), is("tpLon"));
        assertThat(container.getProperty(NAME_LAT_VARIABLE_NAME).getValueString(), is("tpLat"));
        assertThat(container.getProperty(NAME_RASTER_RESOLUTION_KM).getValueString(), is("300.0"));
        assertThat(container.getProperty(NAME_OFFSET_X).getValueString(), is("1.5"));
        assertThat(container.getProperty(NAME_OFFSET_Y).getValueString(), is("1.5"));
        assertThat(container.getProperty(NAME_SUBSAMPLING_X).getValueString(), is("15.0"));
        assertThat(container.getProperty(NAME_SUBSAMPLING_Y).getValueString(), is("15.0"));

        assertThat(product.getSceneGeoCoding(), isNotNull());
        product.setSceneGeoCoding(null);
        assertThat(product.getSceneGeoCoding(), isNull());

        // remove ID ... make container a historical version
        container.removeProperty(KEY_PERSISTENCE_ID);
        assertThat(container.getProperty(KEY_PERSISTENCE_ID), isNull());
        final HistoricalDecoder preHistoricalDecoder = converter.getHistoricalDecoders()[0];
        assertThat(preHistoricalDecoder.canDecode(container), is(true));
        container = preHistoricalDecoder.decode(container, null).asContainer();
        final Property<?> persistenceIdProp = container.getProperty(KEY_PERSISTENCE_ID);
        assertThat(persistenceIdProp, isNotNull());
        assertThat(persistenceIdProp.getValueString(), is(converter.getID()));

        // continue with item decoded by historical decoder
        final ComponentGeoCoding newGeoCoding = converter.decode(container, product);

        assertThat(newGeoCoding, is(instanceOf(ComponentGeoCoding.class)));
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

    private Matcher<Object> isNull() {
        return is(nullValue());
    }

    private Matcher<Object> isNotNull() {
        return is(notNullValue());
    }
}