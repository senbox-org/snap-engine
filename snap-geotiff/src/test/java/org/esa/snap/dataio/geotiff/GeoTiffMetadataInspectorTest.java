package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.metadata.MetadataInspector;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by jcoravu on 21/1/2020.
 */
public class GeoTiffMetadataInspectorTest {

    public GeoTiffMetadataInspectorTest() {
    }

    @Test
    public void testMetadataInspector() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("tiger-minisblack-strip-16.tif");
        assertNotNull(resource);

        File productFile = new File(resource.toURI());

        GeoTiffMetadataInspector metadataInspector = new GeoTiffMetadataInspector();
        MetadataInspector.Metadata metadata = metadataInspector.getMetadata(productFile.toPath());
        assertNotNull(metadata);
        assertEquals(73, metadata.getProductWidth());
        assertEquals(76, metadata.getProductHeight());

        assertNull(metadata.getGeoCoding());

        assertNotNull(metadata.getBandList());
        assertEquals(1, metadata.getBandList().size());
        assertTrue(metadata.getBandList().contains("band_1"));

        assertNotNull(metadata.getMaskList());
        assertEquals(0, metadata.getMaskList().size());
    }
}
