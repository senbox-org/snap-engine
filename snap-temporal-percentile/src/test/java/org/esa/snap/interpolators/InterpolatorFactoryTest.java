package org.esa.snap.interpolators;

import org.junit.Test;

import static org.junit.Assert.*;

public class InterpolatorFactoryTest {

    @Test
    public void testCreateInterpolator() {
        Interpolator interpolator = InterpolatorFactory.createInterpolator("gapFillingLinearInterpolation");
        assertTrue(interpolator instanceof LinearInterpolator);

        interpolator = InterpolatorFactory.createInterpolator("gapFillingSplineInterpolation");
        assertTrue(interpolator instanceof SplineInterpolator);

        interpolator = InterpolatorFactory.createInterpolator("gapFillingQuadraticInterpolation");
        assertTrue(interpolator instanceof QuadraticInterpolator);

        interpolator = InterpolatorFactory.createInterpolator("firlefanz");
        assertNotNull(interpolator);
        assertNull(interpolator.interpolate(null, null));
        assertEquals(1, interpolator.getMinNumPoints());
    }
}
