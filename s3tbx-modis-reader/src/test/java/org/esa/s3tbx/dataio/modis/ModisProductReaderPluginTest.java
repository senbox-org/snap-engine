package org.esa.s3tbx.dataio.modis;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModisProductReaderPluginTest {

    private ModisProductReaderPlugIn plugIn;
    private File testFile;

    @Test
    public void testInputTypes() {
        final Class[] inputTypes = plugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader readerInstance = plugIn.createReaderInstance();
        assertNotNull(readerInstance);
        assertTrue(readerInstance instanceof ModisProductReader);
    }

    @Test
    public void testGetDefaultFileExtension() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertNotNull(defaultFileExtensions);
        assertEquals(1, defaultFileExtensions.length);
        assertEquals(".hdf", defaultFileExtensions[0]);
    }

    @Test
    public void testGetDescription() {
        String description = plugIn.getDescription(Locale.getDefault());
        assertNotNull(description);
        assertEquals("MODIS HDF4 Data Products", description);

        description = plugIn.getDescription(null);
        assertNotNull(description);
        assertEquals("MODIS HDF4 Data Products", description);
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("MODIS", formatNames[0]);
    }

    @Test
    public void testGetInputFile_nullInput() {
        assertNull(ModisProductReaderPlugIn.getInputFile(null));
    }

    @Test
    public void testGetInputFile_stringInput() {
        String testFileName = "test.file";
        final File file = ModisProductReaderPlugIn.getInputFile(testFileName);
        assertNotNull(file);
        assertEquals(testFileName, file.getName());
    }

    @Test
    public void testGetInputFile_fileInput() {
        final File inputFile = new File("I_am_a.file");
        final File file = ModisProductReaderPlugIn.getInputFile(inputFile);
        assertNotNull(file);
        assertEquals(inputFile.getName(), file.getName());
    }

    @Test
    public void testHasHdfFileExtension() {
        assertFalse(ModisProductReaderPlugIn.hasHdfFileExtension(null));
        assertFalse(ModisProductReaderPlugIn.hasHdfFileExtension(new File("tonio_und.tom")));
        assertTrue(ModisProductReaderPlugIn.hasHdfFileExtension(new File("I_am_but.hdf")));
    }

    @Test
    public void testGetProductFileFilter() {
        final SnapFileFilter productFileFilter = plugIn.getProductFileFilter();
        assertNotNull(productFileFilter);

        assertEquals("MODIS", productFileFilter.getFormatName());
        assertEquals(".hdf", productFileFilter.getDefaultExtension());
        assertEquals("MODIS HDF4 Data Products (*.hdf)", productFileFilter.getDescription());
    }

    @Test
    public void testIsValidInputFile_nullFile() {
        assertFalse(ModisProductReaderPlugIn.isValidInputFile(null));
    }

    @Test
    public void testIsValidInputFile_notExistingFile() {
        assertFalse(ModisProductReaderPlugIn.isValidInputFile(new File("I/don/not/exist.hdf")));
    }

    @Test
    public void testIsValidInputFile_nonHdfFile() throws IOException {
        testFile = new File("I_do_exist.txt");
        if (!testFile.createNewFile()) {
            fail("unable to create TestFile");
        }

        assertFalse(ModisProductReaderPlugIn.isValidInputFile(testFile));
    }

    @Test
    public void testIsValidInputFile_hdfFile() throws IOException {
        testFile = new File("I_do_exist.hdf");
        if (!testFile.createNewFile()) {
            fail("unable to create TestFile");
        }

        assertTrue(ModisProductReaderPlugIn.isValidInputFile(testFile));
    }

    @Before
    public void setUp() {
        plugIn = new ModisProductReaderPlugIn();
    }

    @After
    public void tearDown() throws Exception {
        if (testFile != null) {
            if (!testFile.delete()) {
                fail("unable to delete test file");
            }
        }
    }
}
