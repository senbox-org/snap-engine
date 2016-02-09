package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class SlstrGeoCodingSceneTransformProviderTest {

    private SlstrGeoCodingSceneTransformProvider sceneTransformProvider;

    @Before
    public void setUp() {
        final TiePointGrid sceneLatGrid = new TiePointGrid("sceneLat", 2, 2, 0, 0, 4, 4, new float[]{52, 52, 50, 50});
        final TiePointGrid sceneLonGrid = new TiePointGrid("sceneLon", 2, 2, 0, 0, 4, 4, new float[]{4, 6, 4, 6});
        final TiePointGeoCoding sceneGeoCoding = new TiePointGeoCoding(sceneLatGrid, sceneLonGrid);
        final TiePointGrid modelLatGrid = new TiePointGrid("modelLat", 2, 2, 0, 0, 4, 4, new float[]{51, 51, 49, 49});
        final TiePointGrid modelLonGrid = new TiePointGrid("modelLon", 2, 2, 0, 0, 4, 4, new float[]{5, 7, 5, 7});
        final TiePointGeoCoding modelGeoCoding = new TiePointGeoCoding(modelLatGrid, modelLonGrid);
        sceneTransformProvider = new SlstrGeoCodingSceneTransformProvider(sceneGeoCoding, modelGeoCoding);
    }

    @Test
    public void testGetSceneToModelTransform() throws TransformException {
        final Point2D.Double[] ptSrcs = new Point2D.Double[]{new Point2D.Double(2, 2), new Point2D.Double(3.5, 2.5)};
        final Point2D.Double[] expected = new Point2D.Double[]{new Point2D.Double(0, 0), new Point2D.Double(1.5, 0.5)};
        for (int i = 0; i < 2; i++) {
            final Point2D.Double ptDst = new Point2D.Double();
            final Point2D result = sceneTransformProvider.getSceneToModelTransform().transform(ptSrcs[i], ptDst);
            assertNotNull(result);
            assertEquals(ptDst.getX(), result.getX(), 1e-8);
            assertEquals(ptDst.getY(), result.getY(), 1e-8);
            assertEquals(expected[i].getX(), ptDst.getX(), 1e-8);
            assertEquals(expected[i].getY(), ptDst.getY(), 1e-8);
        }
    }

    @Test
    public void testGetModelToSceneTransform() throws TransformException {
        final Point2D.Double[] ptSrcs = new Point2D.Double[]{new Point2D.Double(0, 0), new Point2D.Double(1.5, 0.5)};
        final Point2D.Double[] expected = new Point2D.Double[]{new Point2D.Double(2, 2), new Point2D.Double(3.5, 2.5)};
        for (int i = 0; i < 2; i++) {
            final Point2D.Double ptDst = new Point2D.Double();
            final Point2D result = sceneTransformProvider.getModelToSceneTransform().transform(ptSrcs[i], ptDst);
            assertNotNull(result);
            assertEquals(ptDst.getX(), result.getX(), 1e-8);
            assertEquals(ptDst.getY(), result.getY(), 1e-8);
            assertEquals(expected[i].getX(), ptDst.getX(), 1e-8);
            assertEquals(expected[i].getY(), ptDst.getY(), 1e-8);
        }
    }

}