package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.SceneRasterTransform;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Tonio Fincke
 */
class SynL1COlciSceneRasterTransform implements SceneRasterTransform {

    private final SynL1COlciForwardMathTransform2D forward;
    private final SynL1COlciInverseMathTransform2D inverse;

    SynL1COlciSceneRasterTransform(Band columnMisregistrationBand, Band rowMisregistrationBand) {
        forward = new SynL1COlciForwardMathTransform2D(columnMisregistrationBand, rowMisregistrationBand);
        inverse = new SynL1COlciInverseMathTransform2D(columnMisregistrationBand, rowMisregistrationBand);
    }

    @Override
    public MathTransform2D getForward() {
        return forward;
    }

    @Override
    public MathTransform2D getInverse() {
        return inverse;
    }

    private class SynL1COlciForwardMathTransform2D extends AbstractMathTransform implements MathTransform2D {

        private final Band columnMisregistrationBand;
        private final Band rowMisregistrationBand;
        private static final int dims = 2;

        SynL1COlciForwardMathTransform2D(Band columnMisregistrationBand, Band rowMisregistrationBand) {
            this.columnMisregistrationBand = columnMisregistrationBand;
            this.rowMisregistrationBand = rowMisregistrationBand;
        }

        @Override
        public void transform(double[] srcPts, int srcOff,
                              double[] dstPts, int dstOff,
                              int numPts) throws TransformException {
            for (int i = 0; i < numPts; i++) {
                final int firstIndex = (dims * i);
                final int secondIndex = firstIndex + 1;
                final double srcPtX = srcPts[srcOff + firstIndex];
                final double srcPtY = srcPts[srcOff + secondIndex];
                final int columnMisregistration =
                        columnMisregistrationBand.getSampleInt((int) srcPtX, 0);
                final int rowMisregistration =
                        rowMisregistrationBand.getSampleInt((int) srcPtX, 0);
                dstPts[dstOff + firstIndex] = srcPtX + columnMisregistration;
                dstPts[dstOff + secondIndex] = srcPtY + rowMisregistration;
            }
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return inverse;
        }

        @Override
        public int getSourceDimensions() {
            return dims;
        }

        @Override
        public int getTargetDimensions() {
            return dims;
        }
    }

    private class SynL1COlciInverseMathTransform2D extends AbstractMathTransform implements MathTransform2D {

        private static final int dims = 2;
        private final int[] columnMisregistration;
        private final int[] rowMisregistration;

        private final static int invalid_value = Integer.MIN_VALUE;

        SynL1COlciInverseMathTransform2D(Band columnMisregistrationBand, Band rowMisregistrationBand) {
            columnMisregistration = createMisRegistrationArray(columnMisregistrationBand);
            rowMisregistration = createMisRegistrationArray(rowMisregistrationBand);
        }

        private int[] createMisRegistrationArray(Band misregistrationBand) {
            int[] misregistrationArray = new int[misregistrationBand.getRasterWidth()];
            Arrays.fill(misregistrationArray, invalid_value);
            int[] forwardRegistration = new int[misregistrationArray.length];
            try {
                misregistrationBand.readPixels(0, 0, misregistrationBand.getRasterWidth(),
                                               misregistrationBand.getRasterHeight(), forwardRegistration);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < misregistrationArray.length; i++) {
                final int columnPosition = i + forwardRegistration[i];
                if (columnPosition > 0 && columnPosition < misregistrationArray.length) {
                    misregistrationArray[columnPosition] = -1 * forwardRegistration[i];
                }
            }
            int lastValidValue = invalid_value;
            for (int i = 0; i < misregistrationArray.length; i++) {
                if (misregistrationArray[i] == invalid_value) {
                    if (i < misregistrationArray.length - 1) {
                        int nextValidValue = invalid_value;
                        int nextValidIndex = invalid_value;
                        for (int j = i; j < misregistrationArray.length; j++) {
                            if (misregistrationArray[j] != invalid_value) {
                                nextValidValue = misregistrationArray[j];
                                nextValidIndex = j;
                                break;
                            }
                        }
                        if (lastValidValue == Integer.MIN_VALUE) {
                            lastValidValue = nextValidValue;
                        }
                        if (nextValidIndex == invalid_value) {
                            nextValidIndex = misregistrationArray.length - 1;
                            nextValidValue = lastValidValue;
                        }
                        for (int j = nextValidIndex; j >= i; j--) {
                            misregistrationArray[j] = (int)(lastValidValue +
                                    (lastValidValue - nextValidValue) *
                                            ((nextValidIndex - j) / (double)nextValidIndex - i));
                        }
                    }
                }
            }
            return misregistrationArray;
        }

        @Override
        public void transform(double[] srcPts, int srcOff,
                              double[] dstPts, int dstOff,
                              int numPts) throws TransformException {
            for (int i = 0; i < numPts; i++) {
                final int firstIndex = (dims * i);
                final int secondIndex = firstIndex + 1;
                final double srcPtX = srcPts[srcOff + firstIndex];
                final double srcPtY = srcPts[srcOff + secondIndex];
                dstPts[dstOff + firstIndex] = srcPtX + columnMisregistration[(int) srcPtX];
                dstPts[dstOff + secondIndex] = srcPtY + rowMisregistration[(int) srcPtX];
            }
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return forward;
        }

        @Override
        public int getSourceDimensions() {
            return dims;
        }

        @Override
        public int getTargetDimensions() {
            return dims;
        }
    }
}
