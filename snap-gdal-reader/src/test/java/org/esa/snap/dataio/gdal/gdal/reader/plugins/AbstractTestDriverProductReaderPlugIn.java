package org.esa.snap.dataio.gdal.gdal.reader.plugins;

import org.esa.lib.gdal.activator.GDALInstallInfo;
import org.esa.snap.dataio.gdal.GDALLoader;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.gdal.reader.plugins.AbstractDriverProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Jean Coravu
 */
public abstract class AbstractTestDriverProductReaderPlugIn {
    private final AbstractDriverProductReaderPlugIn readerPlugIn;
    private final Set<String> extensions;
    private final String driverName;

    protected AbstractTestDriverProductReaderPlugIn(String driverName, AbstractDriverProductReaderPlugIn readerPlugIn) {
        this.driverName = driverName;
        this.readerPlugIn = readerPlugIn;
        this.extensions = new HashSet<>();
    }

    protected AbstractTestDriverProductReaderPlugIn(String extension, String driverName, AbstractDriverProductReaderPlugIn readerPlugIn) {
        this(driverName, readerPlugIn);

        addExtensin(extension);
    }

    @Before
    public final void setUp() throws Exception {
        GDALLoader.ensureGDALInitialised();
    }

    @Test
    public final void testPluginIsLoaded() {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            String formatName = getFormatNameToCheck();
            Iterator<ProductReaderPlugIn> iterator = ProductIOPlugInManager.getInstance().getReaderPlugIns(formatName);
            assertTrue(iterator.hasNext());

            ProductReaderPlugIn loadedPlugIn = iterator.next();
            assertEquals(this.readerPlugIn.getClass(), loadedPlugIn.getClass());

            assertTrue(!iterator.hasNext());
        }
    }

    @Test
    public final void testFormatNames() {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            String[] formatNames = this.readerPlugIn.getFormatNames();
            assertNotNull(formatNames);

            assertEquals(1, formatNames.length);

            String formatName = getFormatNameToCheck();
            assertEquals(formatName, formatNames[0]);
        }
    }

    @Test
    public final void testInputTypes() {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            Class[] classes = this.readerPlugIn.getInputTypes();
            assertNotNull(classes);

            assertEquals(2, classes.length);

            List<Class> list = Arrays.asList(classes);
            assertTrue(list.contains(File.class));
            assertTrue(list.contains(String.class));
        }
    }

    @Test
    public void testProductFileFilter() {
        if (GDALInstallInfo.INSTANCE.isPresent()) {
            SnapFileFilter snapFileFilter = this.readerPlugIn.getProductFileFilter();
            assertNotNull(snapFileFilter);

            String[] defaultExtensions = new String[this.extensions.size()];
            this.extensions.toArray(defaultExtensions);
            assertArrayEquals(defaultExtensions, snapFileFilter.getExtensions());

            String formatName = getFormatNameToCheck();
            assertEquals(formatName, snapFileFilter.getFormatName());

            assertTrue(snapFileFilter.getDescription().contains(this.readerPlugIn.getDescription(Locale.getDefault())));
        }
    }

    final void addExtensin(String extension) {
        this.extensions.add(extension);
    }

    private String getFormatNameToCheck() {
        return "GDAL-" + this.driverName + "-READER";
    }
}
