/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s3tbx.aerosol.lut;

import org.esa.s3tbx.aerosol.InputPixelData;
import org.esa.s3tbx.aerosol.util.PixelGeometry;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.math.ColumnMajorMatrixFactory;
import org.esa.snap.core.util.math.MatrixLookupTable;
import org.esa.snap.core.util.math.VectorLookupTable;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * class to read the lookuptables for the atmospheric correction
 * for GlobAlbedo provided by L.Guanter, FUB, Berlin
 * The LUTs contain values of the following parameters:
 * 1 - atmospheric path radiance
 * 2 - Tdown * Tup = product of downward and upward atmospheric transmission
 * 3 - spherical albedo of the atmosphere at ground level
 * 4 - ratio diff / total downward radiation
 * 5 - ratio diff / total upward radiation
 * <p/>
 * Surface pressure values outside the LUT range are allowed
 * the routines rely on the feature of LookupTable class that values
 * outside the range are treated as equal to the highest or lowest values.
 *
 * @author akheckel
 */
public class MomoLut {

    private final int nWvl;
    private final float[] vza;
    private final float[] sza;
    private final float[] azi;
    private final float[] hsf;
    private final float[] aot;

    private final MatrixLookupTable sdrLut;
    private final VectorLookupTable gasTransLut;
    private final Map<DimSelector, LutLimits> lutLimits;

    /**
     * Standard constructor reading the binary LUT file from "lutName"
     * the number of channels or wavelength for which the LUTs is given
     * is not contained in the file
     *
     * @param aotIis - image input stream of the binary AOT LUT (original format from FUB)
     * @param gasIis - image input stream of the binary GasTrans LUT (original format from FUB)
     * @param nWvl   - number of spectral channels
     */
    public MomoLut(ImageInputStream aotIis, ImageInputStream gasIis, int nWvl) throws IOException {
        final int nParameter = 5;
        this.nWvl = nWvl;
        try {
            // read LUT dimensions and values
            this.vza = Luts.readDimension(aotIis);
            this.sza = Luts.readDimension(aotIis);
            this.azi = Luts.readDimension(aotIis);
            this.hsf = Luts.readDimension(aotIis);
            // invert order to store stirctly increasing dimension in lut
            // swapping the actual values in the lut is taken care off in readValues()
            for (int i = 0; i < hsf.length / 2; i++) {
                float swap = hsf[hsf.length - 1 - i];
                hsf[hsf.length - 1 - i] = hsf[i];
                hsf[i] = swap;
            }
            this.aot = Luts.readDimension(aotIis);

            float[] values = readValues(aotIis, aot.length, hsf.length, azi.length, sza.length, vza.length, nParameter);

            float[][] dimensions = new float[][]{hsf, vza, sza, azi, aot};
            sdrLut = new MatrixLookupTable(nWvl, nParameter, new ColumnMajorMatrixFactory(), values, dimensions);
            lutLimits = getLutLimits();
        } finally {
            aotIis.close();
        }

        gasTransLut = readGasTransTable(gasIis);
    }

    /**
     * todo add Javadoc
     *
     * @param inPix -
     * @param tau -
     */
    public synchronized void getSdrAndDiffuseFrac(InputPixelData inPix, double tau) {
        Guardian.assertEquals("InputPixelData.nSpecWvl", inPix.nSpecWvl, nWvl);
        Guardian.assertNotNull("InputPixelData.diffuseFrac[][]", inPix.diffuseFrac);
        Guardian.assertNotNull("InputPixelData.surfReflec[][]", inPix.surfReflec);
        PixelGeometry geom;
        for (int iView = 0; iView < 2; iView++) {
            geom = (iView == 0) ? inPix.geom : inPix.geomFward;
            final double[] toaR = (iView == 0) ? inPix.toaReflec : inPix.toaReflecFward;
            final float geomAMF = (float) ((1 / Math.cos(Math.toRadians(geom.sza))
                    + 1 / Math.cos(Math.toRadians(geom.vza))));
            final double[] gasT = getGasTransmission(geomAMF, (float) inPix.wvCol, (float) (inPix.o3du / 1000));
            double[][] lutValues = sdrLut.getValues(inPix.surfPressure, geom.vza, geom.sza, geom.razi, tau);

            for (int iWvl = 0; iWvl < inPix.nSpecWvl; iWvl++) {
                double rhoPath = lutValues[iWvl][0] * Math.PI / Math.cos(Math.toRadians(geom.sza));
                double tupTdown = lutValues[iWvl][1] / Math.cos(Math.toRadians(geom.sza));
                double spherAlb = lutValues[iWvl][2];
                //double tgO3 = Math.exp(inPix.o3du * o3corr[i] * geomAMF/2); // my o3 correction scheme uses AMF=SC/VC not AMF=SC
                double toaCorr = toaR[iWvl] / gasT[iWvl];
                double a = (toaCorr - rhoPath) / tupTdown;
                inPix.surfReflec[iView][iWvl] = a / (1 + spherAlb * a);
                inPix.diffuseFrac[iView][iWvl] = 1.0 - lutValues[iWvl][3];
            }
        }
    }

    /**
     * todo add Javadoc
     *
     * @param ipd -
     *
     * @return -
     */
    public boolean isInsideLut(InputPixelData ipd) {
        return (ipd.geom.vza >= lutLimits.get(DimSelector.VZA).min)
                && (ipd.geom.vza <= lutLimits.get(DimSelector.VZA).max)
                && (ipd.geom.sza >= lutLimits.get(DimSelector.SZA).min)
                && (ipd.geom.sza <= lutLimits.get(DimSelector.SZA).max)
                && (ipd.geom.razi >= lutLimits.get(DimSelector.AZI).min)
                && (ipd.geom.razi <= lutLimits.get(DimSelector.AZI).max);
        //&& (ipd.surfPressure >= lutLimits.get(DimSelector.HSF).min)
        //&& (ipd.surfPressure <= lutLimits.get(DimSelector.HSF).max);
    }

    /**
     * todo add Javadoc
     *
     * @param ipd -
     *
     * @return -
     */
    public synchronized double getMaxAOT(InputPixelData ipd) {
        final float geomAMF = (float) ((1 / Math.cos(Math.toRadians(ipd.geom.sza))
                + 1 / Math.cos(Math.toRadians(ipd.geom.vza))));
        final double[] gasT = getGasTransmission(geomAMF, (float) ipd.wvCol, (float) (ipd.o3du / 1000));
        final double toa = ipd.toaReflec[0] / gasT[0];
        int iAot = 0;
        double[][] lutValues = sdrLut.getValues(ipd.surfPressure, ipd.geom.vza, ipd.geom.sza, ipd.geom.razi, aot[iAot]);
        double rhoPath1 = lutValues[0][0] * Math.PI / Math.cos(Math.toRadians(ipd.geom.sza));
        double rhoPath0 = rhoPath1;
        while (iAot < aot.length - 1 && rhoPath1 < toa) {
            rhoPath0 = rhoPath1;
            iAot++;
            lutValues = sdrLut.getValues(ipd.surfPressure, ipd.geom.vza, ipd.geom.sza, ipd.geom.razi, aot[iAot]);
            rhoPath1 = lutValues[0][0] * Math.PI / Math.cos(Math.toRadians(ipd.geom.sza));
        }
        if (iAot == 0) return 0.005;
        if (rhoPath1 < toa) return 2.0;
        return aot[iAot - 1] + (aot[iAot] - aot[iAot - 1]) * (toa - rhoPath0) / (rhoPath1 - rhoPath0);
    }

    // private methods

    private Map<DimSelector, LutLimits> getLutLimits() {
        Map<DimSelector, LutLimits> limits = new HashMap<>(5);
        limits.put(DimSelector.VZA, new LutLimits(vza[0], vza[vza.length - 1]));
        limits.put(DimSelector.SZA, new LutLimits(sza[0], sza[sza.length - 1]));
        limits.put(DimSelector.AZI, new LutLimits(azi[0], azi[azi.length - 1]));
        limits.put(DimSelector.HSF, new LutLimits(hsf[0], hsf[hsf.length - 1]));
        limits.put(DimSelector.AOT, new LutLimits(aot[0], aot[aot.length - 1]));
        return limits;
    }

    private int calcPosition(int[] indices, int[] sizes) {
        int pos = 0;
        for (int i = 0; i < sizes.length; i++) {
            pos = (pos * sizes[i] + indices[i]);
        }
        return pos;
    }

    private double[] getGasTransmission(float geomAmf, float wvCol, float o3AtmCm) {
        return gasTransLut.getValues(geomAmf, wvCol, o3AtmCm);
    }

    private float[] readValues(ImageInputStream iis, int nAot, int nHsf, int nAzi, int nSza, int nVza, int nParameter) throws IOException {
        int len = nWvl * nAot * nHsf * nAzi * nSza * nVza * nParameter;
        float[] val = new float[len];
        for (int iWvl = 0; iWvl < nWvl; iWvl++) {
            for (int iAot = 0; iAot < nAot; iAot++) {
                for (int iHsf = nHsf - 1; iHsf >= 0; iHsf--) {
                    for (int iAzi = 0; iAzi < nAzi; iAzi++) {
                        for (int iSza = 0; iSza < nSza; iSza++) {
                            for (int iVza = 0; iVza < nVza; iVza++) {
                                for (int iPar = 0; iPar < nParameter; iPar++) {
                                    int pos = calcPosition(new int[]{iHsf, iVza, iSza, iAzi, iAot, iPar, iWvl},
                                                           new int[]{nHsf, nVza, nSza, nAzi, nAot, nParameter, nWvl});
                                    val[pos] = iis.readFloat();
                                }
                            }
                        }
                    }
                }
            }
        }
        return val;
    }

    private VectorLookupTable readGasTransTable(ImageInputStream iis) throws IOException {
        try {
            int nAng = iis.readInt();
            int nCwv = iis.readInt();
            int nOzo = iis.readInt();

            float[] ang = Luts.readDimension(iis, nAng);
            float[] cwv = Luts.readDimension(iis, nCwv);
            float[] ozo = Luts.readDimension(iis, nOzo);
            float[] tgLut = new float[nAng * nCwv * nOzo * nWvl];
            iis.readFully(tgLut, 0, tgLut.length);

            float[] geomAmf = new float[nAng];
            for (int i = 0; i < nAng; i++) geomAmf[i] = (float) (2.0 / Math.cos(Math.toRadians(ang[i])));
            return new VectorLookupTable(nWvl, tgLut, geomAmf, cwv, ozo);
        } finally {
            iis.close();
        }
    }

    private enum DimSelector {
        VZA, SZA, AZI, HSF, AOT
    }

    private static class LutLimits {
        public final float min;
        public final float max;

        public LutLimits(float min, float max) {
            this.min = min;
            this.max = max;
        }
    }
}
