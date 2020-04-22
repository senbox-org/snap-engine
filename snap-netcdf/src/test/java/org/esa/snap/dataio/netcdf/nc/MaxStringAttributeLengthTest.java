package org.esa.snap.dataio.netcdf.nc;

import org.esa.snap.dataio.netcdf.NetCdfActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.DataType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.fail;


/**
 * @author Marco Peters
 */
public class MaxStringAttributeLengthTest {

    @BeforeClass
    public static void setupTestClass() {
        NetCdfActivator.activate();
    }

    private static final int TOO_LONG = N4Variable.MAX_ATTRIBUTE_LENGTH + 10;
    private NFileWriteable nc4Writable;
    private NFileWriteable nc3Writable;
    private Path nc4TempFile;
    private Path nc3TempFile;

    @Before
    public void setUp() throws Exception {
        nc4TempFile = Files.createTempFile(getClass().getSimpleName(), "nc4");
        nc4Writable = NWritableFactory.create(nc4TempFile.toString(),"netcdf4");
        nc3TempFile = Files.createTempFile(getClass().getSimpleName(), "nc3");
        nc3Writable = NWritableFactory.create(nc3TempFile.toString(),"netcdf3");
    }

    @After
    public void tearDown() throws Exception {
        nc4Writable.create();
        nc4Writable.close();
        nc3Writable.create();
        nc3Writable.close();
        Files.delete(nc4TempFile);
        Files.delete(nc3TempFile);
    }

    @Test
    public void testMaxStringGlobalAttributeLengthNC4() throws IOException {
        nc4Writable.addGlobalAttribute("longGlobalAttributeValue", createLongString(TOO_LONG));
    }

    @Test
    public void testMaxStringGlobalAttributeLengthNC3() throws IOException {
        nc3Writable.addGlobalAttribute("longGlobalAttributeValue", createLongString(TOO_LONG));
    }

    @Test
    public void testMaxStringVariableAttributeLengthNC4() throws IOException {
        NVariable variable = nc4Writable.addScalarVariable("metadataVariable", DataType.BYTE);
        variable.addAttribute("longVariableAttributeValue", createLongString(TOO_LONG));
    }

    @Test
    public void testMaxStringVariableAttributeLengthNC3() throws IOException {
        Path tempFile = Files.createTempFile(getClass().getSimpleName(), null);
        NFileWriteable ncFile = NWritableFactory.create(tempFile.toString(),"netcdf3");
        try {
            NVariable variable = ncFile.addScalarVariable("metadataVariable", DataType.BYTE);
            variable.addAttribute("longVariableAttributeValue", createLongString(TOO_LONG));
        } finally {
            if (!Files.deleteIfExists(tempFile)) {
                fail("unable to delete test file");
            }
        }
    }

    private String createLongString(int length) {
        StringBuilder sb = new StringBuilder();
        IntStream randomStream = new Random(123456).ints(length);
        randomStream.forEach(value -> sb.append((char) (value & 0xFF)));
        return sb.toString();
    }
}