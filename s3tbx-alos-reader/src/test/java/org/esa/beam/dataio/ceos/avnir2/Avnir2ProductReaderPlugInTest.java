package org.esa.beam.dataio.ceos.avnir2;

import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.datamodel.RGBImageProfileManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class Avnir2ProductReaderPlugInTest {
    @Test
    public void testRgbProfileAdded() throws Exception {

        int profileCount1 = RGBImageProfileManager.getInstance().getProfileCount();

        new Avnir2ProductReaderPlugIn();

        int profileCount2 = RGBImageProfileManager.getInstance().getProfileCount();

        assertEquals(profileCount2, profileCount1 + 1);

        RGBImageProfile profile = RGBImageProfileManager.getInstance().getProfile(profileCount1);
        assertEquals("AVNIR-2 - 3,2,1", profile.getName());
    }
}
