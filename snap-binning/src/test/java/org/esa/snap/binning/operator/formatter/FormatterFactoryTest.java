package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.operator.formatter.DefaultFormatter;
import org.esa.snap.binning.operator.formatter.Formatter;
import org.esa.snap.binning.operator.formatter.FormatterFactory;
import org.esa.snap.binning.operator.formatter.IsinFormatter;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class FormatterFactoryTest {

    @Test
    public void testCreateWithoutKey() {
        Formatter formatter = FormatterFactory.get(null);
        assertTrue(formatter instanceof DefaultFormatter);

        formatter = FormatterFactory.get("");
        assertTrue(formatter instanceof DefaultFormatter);
    }

    @Test
    public void testCreateWithDefaultKey() {
        Formatter formatter = FormatterFactory.get("default");
        assertTrue(formatter instanceof DefaultFormatter);
    }

    @Test
    public void testCreateWithIsinKey() {
        Formatter formatter = FormatterFactory.get("isin");
        assertTrue(formatter instanceof IsinFormatter);
    }

    @Test
    public void testCreateWithInvalidKey() {
        try {
            FormatterFactory.get("unknown_formatter");
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void testGetOutputFormat_notInConfig() {
        final FormatterConfig config = new FormatterConfig();

        String outputFormat = FormatterFactory.getOutputFormat(config, new File("schlappi.nc"));
        assertEquals("NetCDF-BEAM", outputFormat);

        outputFormat = FormatterFactory.getOutputFormat(config, new File("schmappi.dim"));
        assertEquals("BEAM-DIMAP", outputFormat);

        outputFormat = FormatterFactory.getOutputFormat(config, new File("schnappi.tiff"));
        assertEquals("GeoTIFF", outputFormat);

        outputFormat = FormatterFactory.getOutputFormat(config, new File("schoappi.png"));
        assertEquals("PNG", outputFormat);

        outputFormat = FormatterFactory.getOutputFormat(config, new File("schpappi.jpg"));
        assertEquals("JPEG", outputFormat);
    }

    @Test
    public void testGetOutputFormat_notInConfig_unknownExtension() {
        final FormatterConfig config = new FormatterConfig();

        try {
            FormatterFactory.getOutputFormat(config, new File("schlappi.txt"));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetOutputFormat_fromConfig() {
        final FormatterConfig config = new FormatterConfig();
        config.setOutputFormat("NetCDF-BEAM");

        String outputFormat = FormatterFactory.getOutputFormat(config, new File("schlappi.whatever"));
        assertEquals("NetCDF-BEAM", outputFormat);

        config.setOutputFormat("BEAM-DIMAP");
        outputFormat = FormatterFactory.getOutputFormat(config, new File("schlappi.whatever"));
        assertEquals("BEAM-DIMAP", outputFormat);

        config.setOutputFormat("GeoTIFF");
        outputFormat = FormatterFactory.getOutputFormat(config, new File("schlappi.whatever"));
        assertEquals("GeoTIFF", outputFormat);

        config.setOutputFormat("PNG");
        outputFormat = FormatterFactory.getOutputFormat(config, new File("schlappi.whatever"));
        assertEquals("PNG", outputFormat);

        config.setOutputFormat("JPEG");
        outputFormat = FormatterFactory.getOutputFormat(config, new File("schlappi.whatever"));
        assertEquals("JPEG", outputFormat);
    }

    @Test
    public void testGetOutputFormat_fromConfig_invalidFormat() {
        final FormatterConfig config = new FormatterConfig();
        config.setOutputFormat("wurstnasen_files");

        try {
            FormatterFactory.getOutputFormat(config, new File("schlappi.png"));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
