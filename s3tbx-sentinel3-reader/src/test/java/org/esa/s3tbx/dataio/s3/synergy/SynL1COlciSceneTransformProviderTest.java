package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.transform.MathTransform2D;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class SynL1COlciSceneTransformProviderTest {

    private SynL1COlciSceneTransformProvider provider;

    @Before
    public void setUp() {
        final Band colMisregistrationBand = new Band("colMisregistrationBand", ProductData.TYPE_INT32, 16, 1);
        final ProductData colMisRegistrationData = ProductData.createInstance(new int[]{1, 2, 1, 2, 1, 2, 1, 2,
                1, 2, 1, 2, 1, 2, 1, 2});
        colMisregistrationBand.setData(colMisRegistrationData);
        final Band rowMisregistrationBand = new Band("rowMisregistrationBand", ProductData.TYPE_INT32, 16, 1);
        final ProductData rowMisRegistrationData = ProductData.createInstance(new int[]{1, -2, -1, -2, -1, -2, -1, -2,
                -1, -2, -1, -2, -1, -2, -1, -2});
        rowMisregistrationBand.setData(rowMisRegistrationData);
        provider = new SynL1COlciSceneTransformProvider(colMisregistrationBand, rowMisregistrationBand);
    }

    @Test
    public void testGetForward() throws Exception {
        PixelPos resultPos = new PixelPos();
        provider.getModelToSceneTransform().transform(new PixelPos(0, 0), resultPos);
        assertEquals(1, (int)resultPos.getX());
        assertEquals(1, (int)resultPos.getY());

        provider.getModelToSceneTransform().transform(new PixelPos(2, 2), resultPos);
        assertEquals(3, (int)resultPos.getX());
        assertEquals(1, (int)resultPos.getY());

        provider.getModelToSceneTransform().transform(new PixelPos(5, 7), resultPos);
        assertEquals(7, (int)resultPos.getX());
        assertEquals(5, (int)resultPos.getY());

        provider.getModelToSceneTransform().transform(new PixelPos(13, 11), resultPos);
        assertEquals(15, (int)resultPos.getX());
        assertEquals(9, (int)resultPos.getY());

        provider.getModelToSceneTransform().transform(new PixelPos(15, 15), resultPos);
        assertEquals(17, (int)resultPos.getX());
        assertEquals(13, (int)resultPos.getY());
    }

    @Test
    public void testGetInverse() throws Exception {
        PixelPos resultPos = new PixelPos();
        final MathTransform2D sceneToModelTransform = provider.getSceneToModelTransform();

        sceneToModelTransform.transform(new PixelPos(1, 1), resultPos);
        assertEquals(0, (int)resultPos.getX());
        assertEquals(3, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(2, 2), resultPos);
        assertEquals(1, (int)resultPos.getX());
        assertEquals(4, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(3, 3), resultPos);
        assertEquals(2, (int)resultPos.getX());
        assertEquals(5, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(4, 4), resultPos);
        assertEquals(3, (int)resultPos.getX());
        assertEquals(6, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(5, 5), resultPos);
        assertEquals(4, (int)resultPos.getX());
        assertEquals(7, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(6, 6), resultPos);
        assertEquals(5, (int)resultPos.getX());
        assertEquals(8, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(7, 7), resultPos);
        assertEquals(6, (int)resultPos.getX());
        assertEquals(9, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(8, 8), resultPos);
        assertEquals(7, (int)resultPos.getX());
        assertEquals(10, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(9, 9), resultPos);
        assertEquals(8, (int)resultPos.getX());
        assertEquals(11, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(10, 10), resultPos);
        assertEquals(9, (int)resultPos.getX());
        assertEquals(12, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(11, 11), resultPos);
        assertEquals(10, (int)resultPos.getX());
        assertEquals(13, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(12, 12), resultPos);
        assertEquals(11, (int)resultPos.getX());
        assertEquals(14, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(13, 13), resultPos);
        assertEquals(12, (int)resultPos.getX());
        assertEquals(15, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(14, 14), resultPos);
        assertEquals(13, (int)resultPos.getX());
        assertEquals(16, (int)resultPos.getY());

        sceneToModelTransform.transform(new PixelPos(15, 15), resultPos);
        assertEquals(14, (int)resultPos.getX());
        assertEquals(17, (int)resultPos.getY());
    }

}
