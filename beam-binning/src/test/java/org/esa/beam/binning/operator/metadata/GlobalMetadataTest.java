package org.esa.beam.binning.operator.metadata;

import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.util.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.SortedMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GlobalMetadataTest {

    public static final String TEST_DIR = "test_dir";
    private static final String TEST_PROPERTIES = "param_a = aaaaa\n" +
            "param_b = bbbb\n" +
            "param_c = CCCC";

    // @todo 1 tb/tb check ha the mocking of the SPI stuff does not work correctly tb 2014-10-08
//    @Test
//    public void testCreate() {
//        final String file = "output.file";
//        final OperatorDescriptor descriptor = mock(OperatorDescriptor.class);
//        when(descriptor.getName()).thenReturn("operator_name");
//        when(descriptor.getAlias()).thenReturn("operator_alias");
//        when(descriptor.getVersion()).thenReturn("1.0.9");
//
//        // @todo 2 tb/tb duplicated code - remove and extract method tb 2014-10-08
//        final BinningOp binningOp = mock(BinningOp.class);
//        final BinningOp.Spi spi = mock(BinningOp.Spi.class);
//        when(binningOp.getSpi()).thenReturn(spi);
//        when(spi.getOperatorDescriptor()).thenReturn(descriptor);
//        when(binningOp.getOutputFile()).thenReturn(file);
//        when(binningOp.getStartDateTime()).thenReturn("2013-05-01");
//        when(binningOp.getPeriodDuration()).thenReturn(15.56);
//
//        final GlobalMetadata metadata = GlobalMetadata.create(binningOp);
//        assertNotNull(metadata);
//
//        final SortedMap<String, String> metaProperties = metadata.asSortedMap();
//        assertNotNull(metaProperties);
//        assertEquals(FileUtils.getFilenameWithoutExtension(file), metaProperties.get("product_name"));
//        assertEquals("operator_name", metaProperties.get("software_qualified_name"));
//        assertEquals("operator_alias", metaProperties.get("software_name"));
//        assertEquals("1.0.9", metaProperties.get("software_version"));
//        assertNotNull(metaProperties.get("processing_time"));
//        assertEquals("2013-05-01", metaProperties.get("aggregation_period_start"));
//        assertEquals("15.56 day(s)", metaProperties.get("aggregation_period_duration"));
//    }

    // @todo 2 tb/tb tests with missing parameters - check no Excptions thrown tb 2014-06-30

    @Test
    public void testLoad_fileIsNull() throws IOException {
        final Logger logger = mock(Logger.class);
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        globalMetadata.load(null, logger);

        final SortedMap<String, String> metaProperties = globalMetadata.asSortedMap();
        assertNotNull(metaProperties);
        assertEquals(0, metaProperties.size());

        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testLoad_fileDoesNotExist() throws IOException {
        final Logger logger = mock(Logger.class);
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        globalMetadata.load(new File("over_the_rain.bow"), logger);

        final SortedMap<String, String> metaProperties = globalMetadata.asSortedMap();
        assertNotNull(metaProperties);
        assertEquals(0, metaProperties.size());

        verify(logger, times(1)).warning("Metadata properties file 'over_the_rain.bow' not found");
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testLoad() throws IOException {
        final Logger logger = mock(Logger.class);

        final GlobalMetadata globalMetadata = new GlobalMetadata();
        try {
            final File propertiesFile = writePropertiesFile();
            globalMetadata.load(propertiesFile, logger);

            final SortedMap<String, String> metaProperties = globalMetadata.asSortedMap();
            assertEquals("aaaaa", metaProperties.get("param_a"));
            assertEquals("bbbb", metaProperties.get("param_b"));
            assertEquals("CCCC", metaProperties.get("param_c"));

            verify(logger, times(1)).info(contains("Reading metadata properties file"));
            verifyNoMoreInteractions(logger);
        } finally {
            deletePropertiesFile();
        }
    }

    // @todo 1 tb/tb check ha the mocking of the SPI stuff does not work correctly tb 2014-10-08
//    @Test
//    public void testAsMetadataElement() {
//        final String file = "the.file";
//        final OperatorDescriptor descriptor = mock(OperatorDescriptor.class);
//        when(descriptor.getName()).thenReturn("Veronica");
//        when(descriptor.getAlias()).thenReturn("Vero");
//        when(descriptor.getVersion()).thenReturn("2.1.1");
//
//        final BinningOp binningOp = mock(BinningOp.class);
//        final BinningOp.Spi spi = mock(BinningOp.Spi.class);
//        when(binningOp.getSpi()).thenReturn(spi);
//        when(spi.getOperatorDescriptor()).thenReturn(descriptor);
//        when(binningOp.getOutputFile()).thenReturn(file);
//
//        final GlobalMetadata globalMetadata = GlobalMetadata.create(binningOp);
//
//        final MetadataElement metadataElement = globalMetadata.asMetadataElement();
//        assertNotNull(metadataElement);
//        assertEquals("Global_Attributes", metadataElement.getName());
//        assertEquals(5, metadataElement.getNumAttributes());
//
//        final MetadataAttribute software_qualified_name = metadataElement.getAttribute("software_qualified_name");
//        assertNotNull(software_qualified_name);
//        assertEquals("Veronica", software_qualified_name.getData().getElemString());
//    }

    @Test
    public void testAsMetadataElement_noMetadataContained() {
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        final MetadataElement metadataElement = globalMetadata.asMetadataElement();
        assertNotNull(metadataElement);
        assertEquals("Global_Attributes", metadataElement.getName());
        assertEquals(0, metadataElement.getNumAttributes());
    }

    private void deletePropertiesFile() {
        final File testDir = new File(TEST_DIR);
        if (testDir.isDirectory()) {
            if (!FileUtils.deleteTree(testDir)) {
                fail("unable to delete test directory");
            }
        }
    }

    private File writePropertiesFile() throws IOException {
        final File testDir = new File(TEST_DIR);
        if (!testDir.mkdirs()) {
            fail("unable to create test directory");
        }

        final File testPropertiesFile = new File(testDir, "test.properties");
        if (!testPropertiesFile.createNewFile()) {
            fail("unable to create test file");
        }

        final FileOutputStream fileOutputStream = new FileOutputStream(testPropertiesFile);
        fileOutputStream.write(TEST_PROPERTIES.getBytes());
        fileOutputStream.close();

        return testPropertiesFile;
    }


}
