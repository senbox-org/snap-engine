package org.esa.beam.dataio.avhrr;

import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.datamodel.RGBImageProfileManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AvhrrReaderPlugInTest {
    @Test
    public void testRgbProfileAdded() throws Exception {

        int profileCount1 = RGBImageProfileManager.getInstance().getProfileCount();

        // just touching the class to trigger the RGB profile registration
        new AvhrrReaderPlugIn();

        int profileCount2 = RGBImageProfileManager.getInstance().getProfileCount();

        assertEquals(profileCount1 + 4, profileCount2);
    }
}
