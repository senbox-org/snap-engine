package org.esa.snap.dataio.netcdf;

import org.junit.Test;


import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class DefaultNetCdfReaderTest {

    @Test
    public void testExtractProductName() {
        final Path ncFile = Path.of("MER_FRS_CCL2R_20120225_085936_000001973112_00079_52248_0001.nc");
        assertEquals("MER_FRS_CCL2R_20120225_085936_000001973112_00079_52248_0001", DefaultNetCdfReader.extractProductName(ncFile));

        final Path ncGzFile = Path.of("MER_FSG_CCL2W_20060528_074246_000002952048_00078_22176_0001.nc.gz");
        assertEquals("MER_FSG_CCL2W_20060528_074246_000002952048_00078_22176_0001", DefaultNetCdfReader.extractProductName(ncGzFile));

        final Path ncAtsFile = Path.of("ATS_NR__2PNPDK20101103_130629_000060673096_00096_45369_7055.nc");
        assertEquals("ATS_NR__2PNPDK20101103_130629_000060673096_00096_45369_7055", DefaultNetCdfReader.extractProductName(ncAtsFile));

        final Path ncHdfFile = Path.of("MCD43C1.A2010041.005.2010064142011.hdf");
        assertEquals("MCD43C1.A2010041.005.2010064142011", DefaultNetCdfReader.extractProductName(ncHdfFile));

        final Path ncOifNc3File = Path.of("oif_sample.nc3");
        assertEquals("oif_sample", DefaultNetCdfReader.extractProductName(ncOifNc3File));

        final Path ncOifNc4File = Path.of("oif_sample.nc4");
        assertEquals("oif_sample", DefaultNetCdfReader.extractProductName(ncOifNc4File));

        final Path ncOifNc4cFile = Path.of("oif_sample.nc4c");
        assertEquals("oif_sample", DefaultNetCdfReader.extractProductName(ncOifNc4cFile));
    }
}
