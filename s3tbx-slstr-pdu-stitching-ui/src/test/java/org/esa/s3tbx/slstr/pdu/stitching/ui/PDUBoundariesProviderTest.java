package org.esa.s3tbx.slstr.pdu.stitching.ui;

import org.esa.snap.core.datamodel.GeoPos;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;

/**
 * @author Tonio Fincke
 */
public class PDUBoundariesProviderTest {

    private static final String FIRST_FILE = "S3A_SL_1_RBT____20130707T153252_20130707T153752_20150217T183530_0299_158_182______SVL_O_NR_001.SEN3";
    private static final String SECOND_FILE = "S3A_SL_1_RBT____20130707T153752_20130707T154252_20150217T183530_0299_158_182______SVL_O_NR_001.SEN3";
    private static final String NONSENSE_1_FILE = "nonsense1";
    private static final String NONSENSE_2_FILE = "S3A_SL_1_RBT____20130707T154252_20130707T154752_20150217T183537_0299_158_182______SVL_O_NR_001.SEN3";
    private static final GeoPos[] expectedFirstGeoBoundary = {new GeoPos(45.5154, -87.5954), new GeoPos(43.7689, -87.8511),
            new GeoPos(42.0217, -88.1109), new GeoPos(40.2738, -88.375), new GeoPos(38.5253, -88.6431),
            new GeoPos(36.7761, -88.9153), new GeoPos(35.0263, -89.1916), new GeoPos(33.2759, -89.4719),
            new GeoPos(31.525, -89.7564), new GeoPos(29.7735, -90.0451), new GeoPos(28.0304, -90.3366),
            new GeoPos(27.761, -88.4102), new GeoPos(27.4648, -86.4938), new GeoPos(27.1425, -84.5879),
            new GeoPos(26.7944, -82.6935), new GeoPos(26.4212, -80.8109), new GeoPos(26.0233, -78.9409),
            new GeoPos(25.6013, -77.0837), new GeoPos(25.1559, -75.24), new GeoPos(24.6877, -73.4099),
            new GeoPos(24.1973, -71.5937), new GeoPos(23.8153, -70.2409), new GeoPos(25.5389, -69.6432),
            new GeoPos(27.2588, -69.0219), new GeoPos(28.9746, -68.3749), new GeoPos(30.686, -67.7003),
            new GeoPos(32.3925, -66.9955), new GeoPos(34.0937, -66.2579), new GeoPos(35.7889, -65.4847),
            new GeoPos(37.4776, -64.6724), new GeoPos(39.1592, -63.8174), new GeoPos(40.8246, -62.92),
            new GeoPos(41.4768, -65.0384), new GeoPos(42.0891, -67.1983), new GeoPos(42.66, -69.3987),
            new GeoPos(43.1878, -71.6382), new GeoPos(43.6709, -73.915), new GeoPos(44.108, -76.2268),
            new GeoPos(44.4976, -78.571), new GeoPos(44.8384, -80.9447), new GeoPos(45.1293, -83.3445),
            new GeoPos(45.3693, -85.7666), new GeoPos(45.5154, -87.5954)};
    private static final GeoPos[] expectedSecondGeoBoundary = {new GeoPos(28.0216, -90.3381), new GeoPos(26.2692, -90.6355),
            new GeoPos(24.5165, -90.9374), new GeoPos(22.7634, -91.2439), new GeoPos(21.01, -91.5553),
            new GeoPos(19.2564, -91.8716), new GeoPos(17.5027, -92.1931), new GeoPos(15.7489, -92.52),
            new GeoPos(13.995, -92.8525), new GeoPos(12.2413, -93.1909), new GeoPos(10.4965, -93.5337),
            new GeoPos(10.1579, -91.8143), new GeoPos(9.8103, -90.0987), new GeoPos(9.45406, -88.3867),
            new GeoPos(9.0895, -86.6782), new GeoPos(8.71695, -84.9732), new GeoPos(8.33675, -83.2716),
            new GeoPos(7.94927, -81.5733), new GeoPos(7.55485, -79.8783), new GeoPos(7.15384, -78.1863),
            new GeoPos(6.74662, -76.4973), new GeoPos(6.43734, -75.2325), new GeoPos(8.18338, -74.7973),
            new GeoPos(9.92809, -74.3502), new GeoPos(11.6713, -73.8907), new GeoPos(13.4129, -73.4177),
            new GeoPos(15.1526, -72.9304), new GeoPos(16.8902, -72.4277), new GeoPos(18.6255, -71.9087),
            new GeoPos(20.3583, -71.3722), new GeoPos(22.0884, -70.8167), new GeoPos(23.8067, -70.2438),
            new GeoPos(24.3132, -72.0493), new GeoPos(24.7981, -73.8688), new GeoPos(25.2606, -75.7022),
            new GeoPos(25.7002, -77.5493), new GeoPos(26.1162, -79.4095), new GeoPos(26.508, -81.2826),
            new GeoPos(26.875, -83.168), new GeoPos(27.2167, -85.0653), new GeoPos(27.5325, -86.9737),
            new GeoPos(27.822, -88.8925), new GeoPos(28.0216, -90.3381)};

    @Test
    public void extractGeoBoundariesFromFile() throws Exception {
        final PDUBoundariesProvider provider = new PDUBoundariesProvider();
        final File firstFile = new File(PDUBoundariesProviderTest.class.getResource(FIRST_FILE).getFile());
        final File secondFile = new File(PDUBoundariesProviderTest.class.getResource(SECOND_FILE + "/xfdumanifest.xml").getFile());
        final File nonsense1File = new File(PDUBoundariesProviderTest.class.getResource(NONSENSE_1_FILE + "/xfdumanifest.xml").getFile());
        final File nonsense2File = new File(PDUBoundariesProviderTest.class.getResource(NONSENSE_2_FILE).getFile());

        provider.extractBoundaryFromFile(firstFile);
        assertEquals(1, provider.getNumberOfElements());
        provider.extractBoundaryFromFile(secondFile);
        assertEquals(2, provider.getNumberOfElements());
        provider.extractBoundaryFromFile(nonsense1File);
        assertEquals(2, provider.getNumberOfElements());
        provider.extractBoundaryFromFile(nonsense2File);
        assertEquals(2, provider.getNumberOfElements());

        assertEquals(FIRST_FILE, provider.getName(0));
        assertEquals(SECOND_FILE, provider.getName(1));

        final GeoPos[] firstGeoBoundary = provider.getGeoBoundary(0);
        assertEquals(expectedFirstGeoBoundary.length, firstGeoBoundary.length);
        for (int i = 0; i < firstGeoBoundary.length; i++) {
            assertEquals(expectedFirstGeoBoundary[i].getLat(), firstGeoBoundary[i].getLat());
            assertEquals(expectedFirstGeoBoundary[i].getLon(), firstGeoBoundary[i].getLon());
        }
        final GeoPos[] secondGeoBoundary = provider.getGeoBoundary(1);
        assertEquals(expectedSecondGeoBoundary.length, secondGeoBoundary.length);
        for (int i = 0; i < secondGeoBoundary.length; i++) {
            assertEquals(expectedSecondGeoBoundary[i].getLat(), secondGeoBoundary[i].getLat());
            assertEquals(expectedSecondGeoBoundary[i].getLon(), secondGeoBoundary[i].getLon());
        }

        provider.clear();
        assertEquals(0, provider.getNumberOfElements());
    }

}