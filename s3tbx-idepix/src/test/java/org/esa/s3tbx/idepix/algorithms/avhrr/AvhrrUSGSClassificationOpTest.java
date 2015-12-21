/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.idepix.algorithms.avhrr;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public class AvhrrUSGSClassificationOpTest {


    private double latPoint;
    private double lonPoint;
    private double sza;
    private String ddmmyy;
    private double latSat;
    private double lonSat;
    private double relAziExpected;

    @BeforeClass
    public static void beforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        latPoint = 41.981514;   // product 9507011153_pr, pixel 1520/3600
        lonPoint = 23.374292;
        latSat = 41.278893;     // product 9507011153_pr, pixel 1024/3600
        lonSat = 18.130579;
        sza = 26.382229;
        relAziExpected = 28.90293;
        ddmmyy = "010795";
    }

    @After
    public void tearDown() throws Exception {
    }

    // todo: reactivate when AVHRR-AC algo is integrated in Idepix

//    @Test
////    @Ignore
//    public void testSaaFromSunAnglesCalculation() {
//        final Calendar dateAsCalendar = AvhrrAcUtils.getProductDateAsCalendar(ddmmyy);
//        final SunAngles sunAngles = SunAnglesCalculator.calculate(dateAsCalendar, latPoint, lonPoint);
//        assertEquals(sza, sunAngles.getZenithAngle(), 2.E-1);
//    }
//
//    @Test
////    @Ignore
//    public void testSunPositionCalculation() {
//        final SunPosition sunPosition = AvhrrAcTestClassificationOp.computeSunPosition("210697");
//        assertEquals(23.5, sunPosition.getLat(), 1.E-1);
//        assertEquals(0.5, sunPosition.getLon(), 1.E-1);
//    }
//
//
//    @Test
////    @Ignore
//    public void testSunAzimuthAngleCalculation() {
//        final Calendar dateAsCalendar = AvhrrAcUtils.getProductDateAsCalendar(ddmmyy);
//        final SunAngles sunAngles = SunAnglesCalculator.calculate(dateAsCalendar, latPoint, lonPoint);
//
//        final double latPointRad = latPoint * MathUtils.DTOR;
//        final double lonPointRad = lonPoint * MathUtils.DTOR;
//        final SunPosition sunPosition = AvhrrAcTestClassificationOp.computeSunPosition(ddmmyy);
//        final double latSunRad = sunPosition.getLat() * MathUtils.DTOR;
//        final double lonSunRad = sunPosition.getLon() * MathUtils.DTOR;
//        final double saaRad = AvhrrAcTestClassificationOp.computeSaa(sza, latPointRad, lonPointRad, latSunRad, lonSunRad);
//
//        assertEquals(saaRad*MathUtils.RTOD, sunAngles.getAzimuthAngle(), 1.0);
//    }
//
//    @Test
////    @Ignore
//    public void testRelativeAzimuthAngleCalculation() {
//        // todo: further investigate
//        final double latPointRad = latPoint * MathUtils.DTOR;
//        final double lonPointRad = lonPoint * MathUtils.DTOR;
//        final SunPosition sunPosition = AvhrrAcTestClassificationOp.computeSunPosition(ddmmyy);  // this is the unknown we have to fix!!!
//        final double latSunRad = sunPosition.getLat() * MathUtils.DTOR;
//        final double lonSunRad = sunPosition.getLon() * MathUtils.DTOR;
//        final double saaRad = AvhrrAcTestClassificationOp.computeSaa(sza, latPointRad, lonPointRad, latSunRad, lonSunRad);
//
//        final double latSatRad = latSat * MathUtils.DTOR;
//        final double lonSatRad = lonSat * MathUtils.DTOR;
//        final double greatCirclePointToSatRad =
//                AvhrrAcTestClassificationOp.computeGreatCircleFromPointToSat(latPointRad, lonPointRad, latSatRad, lonSatRad);
//        final double vaaRad = AvhrrAcTestClassificationOp.computeVaa(latPointRad, lonPointRad, latSatRad, lonSatRad,
//                                                                     greatCirclePointToSatRad);
//
//        final double relAziRad = AvhrrAcTestClassificationOp.correctRelAzimuthRange(vaaRad, saaRad);
//        final double relAziDeg = relAziRad * MathUtils.RTOD;
//        assertEquals(relAziExpected, relAziDeg, 1.E-1);
//    }
//
//    @Test
//    public void testLaplaceEquationPerformance() {
//        int nPoints=50;
//        float[][] u = new float[nPoints][nPoints];
//        final float dx = (float) (Math.PI/(nPoints-1));
//        final float dy = (float) (Math.PI/(nPoints-1));
//        for (int i=0; i<nPoints; i++) {
//            for (int j=0; j<nPoints; j++) {
//                u[i][j] = (float) (Math.sin(i*dx)*Math.sin(j*dy));
//            }
//        }
//        LaplaceTestObj laplaceTestObj = new LaplaceTestObj(u, 2);
//
//        int iter =0;
//        final long t1 = System.currentTimeMillis();
//        while (iter < 5000) {
//            computeLaplace(laplaceTestObj, dx, dy);
//            iter++;
//        }
//        final long t2 = System.currentTimeMillis();
//        System.out.println("time java (ms) = " + (t2-t1));
//        System.out.println("iter = " + iter);
//        System.out.println("err = " + laplaceTestObj.err);
//
//        assertTrue(laplaceTestObj.getErr() < 1.E-6);
//    }
//
//    private LaplaceTestObj computeLaplace(LaplaceTestObj laplaceTestObj, float dx, float dy) {
//        final float dx2 = dx * dx;
//        final float dy2 = dy * dy;
//
//        final float dnrInv = 0.5f/(dx2 + dy2);
//        float[][] u = laplaceTestObj.getU();
//
//        float err = 0.0f;
//        for (int i=1; i<u[0].length-1; i++) {
//            for (int j=1; j<u[0].length-1; j++) {
//                final float tmp = u[i][j];
//                u[i][j] = ((u[i-1][j] +  u[i+1][j]) * dx2 +
//                           (u[i][j-1] +  u[i][j+1]) * dy2) * dnrInv;
//                final float diff = u[i][j] - tmp;
//                err += diff*diff;
//            }
//        }
//        laplaceTestObj.setU(u);
//        laplaceTestObj.setErr(err);
//
//        return laplaceTestObj;
//    }
//
//    private class LaplaceTestObj {
//        float[][] u;
//        float err;
//
//        LaplaceTestObj(float[][] u, float err) {
//            this.u = u;
//            this.err = err;
//        }
//
//        public float[][] getU() {
//            return u;
//        }
//
//        public void setU(float[][] u) {
//            this.u = u;
//        }
//
//        public float getErr() {
//            return err;
//        }
//
//        public void setErr(float err) {
//            this.err = err;
//        }
//    }
}
