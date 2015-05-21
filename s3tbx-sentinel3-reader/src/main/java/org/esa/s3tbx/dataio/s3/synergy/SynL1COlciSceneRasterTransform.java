package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.SceneRasterTransform;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

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
            return new SynL1COlciInverseMathTransform2D(columnMisregistrationBand, rowMisregistrationBand);
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

        private final Band columnMisregistrationBand;
        private final Band rowMisregistrationBand;
        private static final int dims = 2;

        SynL1COlciInverseMathTransform2D(Band columnMisregistrationBand, Band rowMisregistrationBand) {
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
                dstPts[dstOff + firstIndex] = srcPtX - columnMisregistration;
                dstPts[dstOff + secondIndex] = srcPtY - rowMisregistration;
            }
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return new SynL1COlciForwardMathTransform2D(columnMisregistrationBand, rowMisregistrationBand);
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
