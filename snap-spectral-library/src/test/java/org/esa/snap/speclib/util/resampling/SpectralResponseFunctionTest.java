package org.esa.snap.speclib.util.resampling;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpectralResponseFunctionTest {


    @Test
    @STTM("SNAP-4174")
    public void test_setupFwhmFromCsv_enmap() throws Exception {
        SpectralResponseFunction srf = new SpectralResponseFunction("enmap");
        assertNotNull(srf.getID());
        assertEquals("enmap", srf.getID());
        assertNotNull(srf.getFwhmList());
        assertEquals(0, srf.getFwhmList().size());

        final URL resource = getClass().getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(resource);
        final File csvFile = new File(resource.toURI());
        final CsvTable fwhmTableFromCsv = SpectralResponseFunction.readFwhmFromCsv(csvFile);

        assertEquals(List.of("wavelength", "fwhm"), fwhmTableFromCsv.header());
        assertEquals(224, fwhmTableFromCsv.rows().size());
        assertEquals(List.of("418.24", "6.99561"), fwhmTableFromCsv.rows().getFirst());

        assertEquals("418.24", fwhmTableFromCsv.rows().getFirst().getFirst());
        assertEquals("6.99561", fwhmTableFromCsv.rows().getFirst().get(1));

        assertEquals(418.24f, Float.parseFloat(fwhmTableFromCsv.rows().getFirst().getFirst()), 1.E-2);
        assertEquals(6.99561f, Float.parseFloat(fwhmTableFromCsv.rows().getFirst().get(1)), 1.E-5);

        srf.setFWHMList(fwhmTableFromCsv);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();

        assertEquals(224, fwhmList.size());
        assertEquals(418.24f, fwhmList.getFirst().getWvl(), 1.E-2);
        assertEquals(6.99561f, fwhmList.getFirst().getFwhm(), 1.E-5);
        assertEquals(911.715f, fwhmList.get(81).getWvl(), 1.E-2);
        assertEquals(10.2508f, fwhmList.get(81).getFwhm(), 1.E-5);
        assertEquals(2445.53f, fwhmList.get(223).getWvl(), 1.E-2);
        assertEquals(7.1581f, fwhmList.get(223).getFwhm(), 1.E-5);
    }

    @Test
    @STTM("SNAP-4174")
    public void test_setupFwhmFromCsv_prisma() throws Exception {
        SpectralResponseFunction srf = new SpectralResponseFunction("prisma");
        assertNotNull(srf.getID());
        assertEquals("prisma", srf.getID());
        assertNotNull(srf.getFwhmList());
        assertEquals(0, srf.getFwhmList().size());

        final URL resource = getClass().getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(resource);
        final File csvFile = new File(resource.toURI());
        final CsvTable fwhmTableFromCsv = SpectralResponseFunction.readFwhmFromCsv(csvFile);

        assertEquals(List.of("wavelength", "fwhm"), fwhmTableFromCsv.header());
        assertEquals(234, fwhmTableFromCsv.rows().size());
        assertEquals(List.of("402.5", "11.4"), fwhmTableFromCsv.rows().getFirst());

        assertEquals("402.5", fwhmTableFromCsv.rows().getFirst().getFirst());
        assertEquals("11.4", fwhmTableFromCsv.rows().getFirst().get(1));

        assertEquals(402.5f, Float.parseFloat(fwhmTableFromCsv.rows().getFirst().getFirst()), 1.E-1);
        assertEquals(11.4f, Float.parseFloat(fwhmTableFromCsv.rows().getFirst().get(1)), 1.E-1);

        srf.setFWHMList(fwhmTableFromCsv);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();

        assertEquals(234, fwhmList.size());
        assertEquals(402.5f, fwhmList.getFirst().getWvl(), 1.E-1);
        assertEquals(11.4f, fwhmList.getFirst().getFwhm(), 1.E-1);
        assertEquals(1626.8f, fwhmList.get(128).getWvl(), 1.E-1);
        assertEquals(13.3f, fwhmList.get(128).getFwhm(), 1.E-1);
        assertEquals(2496.9f, fwhmList.getLast().getWvl(), 1.E-1);
        assertEquals(9.5f, fwhmList.getLast().getFwhm(), 1.E-1);
    }

    @Test
    @STTM("SNAP-4174")
    public void test_setupFwhmFromCsv_olci() throws Exception {
        SpectralResponseFunction srf = new SpectralResponseFunction("olci");
        assertNotNull(srf.getID());
        assertEquals("olci", srf.getID());
        assertNotNull(srf.getFwhmList());
        assertEquals(0, srf.getFwhmList().size());

        final URL resource = getClass().getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(resource);
        final File csvFile = new File(resource.toURI());
        final CsvTable fwhmTableFromCsv = SpectralResponseFunction.readFwhmFromCsv(csvFile);

        assertEquals(List.of("wavelength", "fwhm"), fwhmTableFromCsv.header());
        assertEquals(21, fwhmTableFromCsv.rows().size());
        assertEquals(List.of("400.0", "14.128492"), fwhmTableFromCsv.rows().getFirst());

        assertEquals("400.0", fwhmTableFromCsv.rows().getFirst().getFirst());
        assertEquals("14.128492", fwhmTableFromCsv.rows().getFirst().get(1));

        assertEquals(400.0f, Float.parseFloat(fwhmTableFromCsv.rows().getFirst().getFirst()), 1.E-1);
        assertEquals(14.128492f, Float.parseFloat(fwhmTableFromCsv.rows().getFirst().get(1)), 1.E-1);

        srf.setFWHMList(fwhmTableFromCsv);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();

        assertEquals(21, fwhmList.size());
        assertEquals(400.0f, fwhmList.getFirst().getWvl(), 1.E-1);
        assertEquals(14.128492f, fwhmList.getFirst().getFwhm(), 1.E-1);
        assertEquals(761.25f, fwhmList.get(12).getWvl(), 1.E-1);
        assertEquals(2.564887f, fwhmList.get(12).getFwhm(), 1.E-1);
        assertEquals(1020.0f, fwhmList.getLast().getWvl(), 1.E-1);
        assertEquals(26.936745f, fwhmList.getLast().getFwhm(), 1.E-1);
        // TODO
    }

    @Test
    @STTM("SNAP-4174")
    public void test_getFullyDefinedSrf_olci() throws Exception {
        SpectralResponseFunction srf = new SpectralResponseFunction("olci");

        final URL fwhmResource = getClass().getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(fwhmResource);
        final File fwhmCsvFile = new File(fwhmResource.toURI());
        final CsvTable fwhmTable = SpectralResponseFunction.readFwhmFromCsv(fwhmCsvFile);

        srf.setFWHMList(fwhmTable);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();
        List<SpectralResponseFunction> fullSrfList =
                SpectralResponseFunction.getFullyDefinedSrf(fwhmList);

        assertNotNull(fullSrfList);
        assertEquals(21, fullSrfList.size());

        assertEquals(400.0f, fullSrfList.getFirst().getRefWvl(), 1.E-1);
        assertEquals(37, fullSrfList.getFirst().getSpectralResponsesList().size());
        assertEquals(382.0, fullSrfList.getFirst().getSpectralResponsesList().getFirst().getWvl(), 1.E-1);
        assertEquals(0.011, fullSrfList.getFirst().getSpectralResponsesList().getFirst().getWeight(), 1.E-3);
        assertEquals(400.0, fullSrfList.getFirst().getSpectralResponsesList().get(18).getWvl(), 1.E-1);
        assertEquals(1.0, fullSrfList.getFirst().getSpectralResponsesList().get(18).getWeight(), 1.E-3);
        assertEquals(418.0, fullSrfList.getFirst().getSpectralResponsesList().getLast().getWvl(), 1.E-1);
        assertEquals(0.011, fullSrfList.getFirst().getSpectralResponsesList().getLast().getWeight(), 1.E-3);

        assertEquals(865.0f, fullSrfList.get(16).getRefWvl(), 1.E-1);
        assertEquals(53, fullSrfList.get(16).getSpectralResponsesList().size());
        assertEquals(839.0, fullSrfList.get(16).getSpectralResponsesList().getFirst().getWvl(), 1.E-1);
        assertEquals(0.009, fullSrfList.get(16).getSpectralResponsesList().getFirst().getWeight(), 1.E-3);
        assertEquals(857.0, fullSrfList.get(16).getSpectralResponsesList().get(18).getWvl(), 1.E-1);
        assertEquals(0.639, fullSrfList.get(16).getSpectralResponsesList().get(18).getWeight(), 1.E-3);
        assertEquals(891.0, fullSrfList.get(16).getSpectralResponsesList().getLast().getWvl(), 1.E-1);
        assertEquals(0.009, fullSrfList.get(16).getSpectralResponsesList().getLast().getWeight(), 1.E-3);

        assertEquals(1020.0f, fullSrfList.getLast().getRefWvl(), 1.E-1);
        assertEquals(71, fullSrfList.getLast().getSpectralResponsesList().size());
        assertEquals(985.0, fullSrfList.getLast().getSpectralResponsesList().getFirst().getWvl(), 1.E-1);
        assertEquals(0.009, fullSrfList.getLast().getSpectralResponsesList().getFirst().getWeight(), 1.E-3);
        assertEquals(1003.0, fullSrfList.getLast().getSpectralResponsesList().get(18).getWvl(), 1.E-1);
        assertEquals(0.331, fullSrfList.getLast().getSpectralResponsesList().get(18).getWeight(), 1.E-3);
        assertEquals(1055.0, fullSrfList.getLast().getSpectralResponsesList().getLast().getWvl(), 1.E-1);
        assertEquals(0.009, fullSrfList.getLast().getSpectralResponsesList().getLast().getWeight(), 1.E-3);
    }

    @Test
    @STTM("SNAP-4174")
    public void test_getFullyDefinedSrf_enmap() throws Exception {
        SpectralResponseFunction srf = new SpectralResponseFunction("enmap");

        final URL fwhmResource = getClass().getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(fwhmResource);
        final File fwhmCsvFile = new File(fwhmResource.toURI());
        final CsvTable fwhmTable = SpectralResponseFunction.readFwhmFromCsv(fwhmCsvFile);

        srf.setFWHMList(fwhmTable);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();
        List<SpectralResponseFunction> fullSrfList =
                SpectralResponseFunction.getFullyDefinedSrf(fwhmList);

        assertNotNull(fullSrfList);
        assertEquals(224, fullSrfList.size());
    }

    @Test
    @STTM("SNAP-4174")
    public void test_getFullyDefinedSrf_prisma() throws Exception {
        SpectralResponseFunction srf = new SpectralResponseFunction("prisma");

        final URL fwhmResource = getClass().getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(fwhmResource);
        final File fwhmCsvFile = new File(fwhmResource.toURI());
        final CsvTable fwhmTable = SpectralResponseFunction.readFwhmFromCsv(fwhmCsvFile);

        srf.setFWHMList(fwhmTable);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();
        List<SpectralResponseFunction> fullSrfList =
                SpectralResponseFunction.getFullyDefinedSrf(fwhmList);

        assertNotNull(fullSrfList);
        assertEquals(234, fullSrfList.size());
    }
}
