package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.esa.snap.core.metadata.MetadataInspector;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by jcoravu on 21/1/2020.
 */
public class GeoTiffMetadataInspectorTest {

    public GeoTiffMetadataInspectorTest() {
    }

    @Test
    public void testMetadataInspector() throws IOException {
        URL resource = getClass().getResource("tiger-minisblack-strip-16.tif");
        assertNotNull(resource);

        Path productFile = UrlHelper.urlToFile(resource);

        GeoTiffMetadataInspector metadataInspector = new GeoTiffMetadataInspector();
        MetadataInspector.Metadata metadata = metadataInspector.getMetadata(productFile);
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
