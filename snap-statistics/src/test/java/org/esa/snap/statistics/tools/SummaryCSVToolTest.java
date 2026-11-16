package org.esa.snap.statistics.tools;

import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.core.runtime.internal.UrlHelper;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SummaryCSVToolTest {

    private SummaryCSVTool.ShapeFileReader shapeFileReader;
    private Logger logger;
    private SummaryCSVTool summaryCSVTool;
    private Path inputDir;

    @Before
    public void setUp() throws Exception {
        shapeFileReader = mock(SummaryCSVTool.ShapeFileReader.class);
        logger = mock(Logger.class);
        final URL resource = SummaryCSVToolTest.class.getResource("20070504_out_cwbody_desh_gk3.shp");
        Assume.assumeNotNull(resource);
        final Path existingShapeFile = UrlHelper.urlToFile(resource);
        Assume.assumeNotNull(existingShapeFile);
        inputDir = Mockito.spy(existingShapeFile.getParent());
        summaryCSVTool = new SummaryCSVTool(logger, shapeFileReader, "NAME");
    }

    @Test
    public void testThatAnErrorWasLoggedIfAnIOExceptionIsThrownWhileReadingAShapeFile() throws IOException {
        //preparation
        final String exceptionMessage = "Unable To read";
        when(shapeFileReader.read(any(Path.class))).thenThrow(new IOException(exceptionMessage));

        //execution
        summaryCSVTool.summarize(inputDir);

        //verification
        verify(logger, times(1)).log(Level.WARNING, exceptionMessage);
    }

    @Test
    public void testThatNoErrorIsLoggedWhenShapeFileNameDoesCorrespondToDatePattern() throws IOException {
        //preparation
        when(shapeFileReader.read(any(Path.class))).thenReturn(null);
        inputDir = IOHelper.createATempDir();
        IOHelper.createFile(inputDir.resolve("20120509_chl.shp"));

        //execution
        summaryCSVTool.summarize(inputDir);

        //verification
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testThatAnErrorIsLoggedWhenShapeFileNameDoesNotCorrespondToDatePattern() {
        //preparation
        inputDir = IOHelper.createATempDir();
        try {
            IOHelper.createFile(inputDir.resolve("blah_20120509.shp"));
        } catch (IOException ignored) {
        }

        //execution
        summaryCSVTool.summarize(inputDir);

        //verification
        String expectedMessage = "The filename 'blah_20120509.shp' does not match the pattern yyyyMMdd_*.shp.";
        verify(logger, times(1)).log(Level.WARNING, expectedMessage);
    }
}
