package org.esa.snap.jp2.reader;

import org.esa.snap.core.metadata.XmlMetadataParser;
import org.esa.snap.core.metadata.XmlMetadataParserFactory;
import org.esa.snap.jp2.reader.metadata.Jp2XmlMetadata;
import org.esa.snap.jp2.reader.metadata.OpjDumpFile;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.lib.openjpeg.utils.OpenJpegUtils;
import org.esa.snap.runtime.LogUtils4Tests;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.esa.snap.lib.openjpeg.utils.OpenJpegUtils.validateOpenJpegExecutables;
import static org.junit.Assert.*;

public class Jp2XmlMetadataTest {

    public Jp2XmlMetadataTest(){}

    @BeforeClass
    public static void initialize() throws Exception {
        LogUtils4Tests.initLogger();
        OpenJPEGLibraryInstaller.install();
    }

    @Test
    public void testJp2XmlMetadata_usual_use_case() throws URISyntaxException, IOException, InterruptedException {
        URL resource = getClass().getResource("S2_subset_sample.jp2");
        XmlMetadataParserFactory.registerParser(Jp2XmlMetadata.class, new XmlMetadataParser<>(Jp2XmlMetadata.class));
        assertNotNull(resource);
        File productFile = new File(resource.toURI());

        OpjDumpFile opjDumpFile = new OpjDumpFile();
        if (OpenJpegUtils.canReadJP2FileHeaderWithOpenJPEG()) {
            if (!validateOpenJpegExecutables(OpenJpegExecRetriever.getOpjDump(), OpenJpegExecRetriever.getOpjDecompress())) {
                throw new IOException("Invalid OpenJpeg executables");
            }

            opjDumpFile.readHeaderWithOpenJPEG(productFile.toPath());
        } else {
            opjDumpFile.readHeaderWithInputStream(productFile.toPath(), 5 * 1024, true);
        }
        Jp2XmlMetadata metadataHeader = opjDumpFile.getMetadata();

        assertEquals(false, metadataHeader.isReversedAxisOrder());
        assertEquals(new Point2D.Double(505950.0, 2888070.0), metadataHeader.getOrigin());
        assertEquals(60.0, metadataHeader.getStepX(), 0);
        assertEquals(-60.0, metadataHeader.getStepY(), 0);
        assertEquals("EPSG::32639", metadataHeader.getCrsGeocoding());
    }

    @Test
    public void testJp2XmlMetadata_epsg_4326_y_x_axis_use_case() throws URISyntaxException, IOException, InterruptedException {
        URL resource = getClass().getResource("subset_epsg_4326_axis_order_y_x_300.jp2");
        XmlMetadataParserFactory.registerParser(Jp2XmlMetadata.class, new XmlMetadataParser<>(Jp2XmlMetadata.class));
        assertNotNull(resource);
        File productFile = new File(resource.toURI());

        OpjDumpFile opjDumpFile = new OpjDumpFile();
        if (OpenJpegUtils.canReadJP2FileHeaderWithOpenJPEG()) {
            if (!validateOpenJpegExecutables(OpenJpegExecRetriever.getOpjDump(), OpenJpegExecRetriever.getOpjDecompress())) {
                throw new IOException("Invalid OpenJpeg executables");
            }

            opjDumpFile.readHeaderWithOpenJPEG(productFile.toPath());
        } else {
            opjDumpFile.readHeaderWithInputStream(productFile.toPath(), 5 * 1024, true);
        }
        Jp2XmlMetadata metadataHeader = opjDumpFile.getMetadata();

        assertEquals(true, metadataHeader.isReversedAxisOrder());
        assertEquals(new Point2D.Double(23.276245370370784, 42.783675925925515), metadataHeader.getOrigin());
        assertEquals(4.62962963E-6, metadataHeader.getStepX(), 0);
        assertEquals(-4.62962963E-6, metadataHeader.getStepY(), 0);
        assertEquals("EPSG::4326", metadataHeader.getCrsGeocoding());
    }
}
