package org.esa.snap.jp2.reader;

import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.jp2.reader.metadata.JP2MetadataInspector;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jcoravu on 21/1/2020.
 */
public class JP2MetadataInspectorTest extends AbstractJP2Test{

    public JP2MetadataInspectorTest() {
    }

    @Test
    public void testMetadataInspector() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("sample.jp2");
        assertNotNull(resource);

        File productFile = new File(resource.toURI());

        JP2MetadataInspector metadataInspector = new JP2MetadataInspector();
        MetadataInspector.Metadata metadata = metadataInspector.getMetadata(productFile.toPath());
        assertNotNull(metadata);
        assertEquals(400, metadata.getProductWidth());
        assertEquals(300, metadata.getProductHeight());

        assertNull(metadata.getGeoCoding());

        assertNotNull(metadata.getBandList());
        assertEquals(3, metadata.getBandList().size());
        assertTrue(metadata.getBandList().contains("band_1"));
        assertTrue(metadata.getBandList().contains("band_2"));
        assertTrue(metadata.getBandList().contains("band_3"));

        assertNotNull(metadata.getMaskList());
        assertEquals(0, metadata.getMaskList().size());
    }
}
