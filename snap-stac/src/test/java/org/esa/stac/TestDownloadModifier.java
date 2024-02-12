package org.esa.stac;

import com.bc.ceres.binding.Validator;
import org.esa.stac.internal.EstablishedModifiers;
import org.junit.Assert;
import org.junit.Test;

public class TestDownloadModifier {
    @Test
    public void testPlanetaryModifier() {
        String tifURL = "https://sentinel2l2a01.blob.core.windows.net/sentinel2-l2/10/T/CR/2022/08/28/S2A_MSIL2A_20220828T190931_N0400_R056_T10TCR_20220830T153754.SAFE/GRANULE/L2A_T10TCR_A037519_20220828T191548/IMG_DATA/R10m/T10TCR_20220828T190931_B08_10m.tif";
        String signed = EstablishedModifiers.planetaryComputer().signURL(tifURL);
        Assert.assertNotEquals(tifURL, signed);
        Assert.assertTrue(signed.contains(tifURL));
    }
}
