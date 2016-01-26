package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.transform.MathTransform2D;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class SynL1CSlstrSceneTransformProviderTest {

    @Test
    public void testGetSceneToModelTransform() throws Exception {
        final Band colMisregistrationBand = new Band("colMisregistrationBand", ProductData.TYPE_INT32, 4, 4);
        final ProductData colMisRegistrationData = ProductData.createInstance(new int[]{1, 2, 1, 2, 1, 2, 1, 2,
                1, 2, 1, 2, 1, 2, 1, 3});
        colMisregistrationBand.setData(colMisRegistrationData);
        final Band rowMisregistrationBand = new Band("rowMisregistrationBand", ProductData.TYPE_INT32, 4, 4);
//        final ProductData rowMisRegistrationData = ProductData.createInstance(new int[]{-1, -2, -1, -2, -1, -2, -1, -2,
//                -1, -2, -1, -2, -1, -2, -1, -3});
        final ProductData rowMisRegistrationData = ProductData.createInstance(new int[]{0, 1, 0, 1, 0, 1, 0, 1,
                0, 1, 0, 1, 0, 1, 0, 2});
        rowMisregistrationBand.setData(rowMisRegistrationData);
        final SynL1CSlstrSceneTransformProvider provider =
                new SynL1CSlstrSceneTransformProvider(colMisregistrationBand, rowMisregistrationBand);

        final MathTransform2D sceneToModelTransform = provider.getSceneToModelTransform();

        PixelPos resultPos = new PixelPos();
        sceneToModelTransform.transform(new PixelPos(0, 0), resultPos);
        assertEquals(1, (int)resultPos.getX());
        assertEquals(0, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(2, 1), resultPos);
        assertEquals(1, (int)resultPos.getX());
        assertEquals(0, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(1, 2), resultPos);
        assertEquals(2, (int)resultPos.getX());
        assertEquals(1, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(3, 3), resultPos);
        assertEquals(3, (int)resultPos.getX());
        assertEquals(2, (int)resultPos.getY());
    }
}