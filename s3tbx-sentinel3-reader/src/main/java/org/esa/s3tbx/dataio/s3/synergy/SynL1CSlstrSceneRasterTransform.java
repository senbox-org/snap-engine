package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.SceneRasterTransform;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/**
 * @author Tonio Fincke
 */
public class SynL1CSlstrSceneRasterTransform implements SceneRasterTransform {

    private final InverseTransform inverseTransform;

    public SynL1CSlstrSceneRasterTransform(Band colCorrespondenceBand, Band rowCorrespondenceBand) {
        inverseTransform = new InverseTransform(colCorrespondenceBand, rowCorrespondenceBand);
    }

    @Override
    public MathTransform2D getForward() {
        //todo change this: Throw SceneRasterTransformException instead
        return null;
    }

    @Override
    public MathTransform2D getInverse() {
        return inverseTransform;
    }

    private class InverseTransform extends AbstractMathTransform implements MathTransform2D {

        private final Band colCorrespondenceBand;
        private final Band rowCorrespondenceBand;
        private static final int dims = 2;

        private InverseTransform(Band colCorrespondenceBand, Band rowCorrespondenceBand) {
            this.colCorrespondenceBand = colCorrespondenceBand;
            this.rowCorrespondenceBand = rowCorrespondenceBand;
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
                final int columnCorresponce =
                        colCorrespondenceBand.getSampleInt((int) srcPtX, (int) srcPtY);
                final int rowCorrespondence =
                        rowCorrespondenceBand.getSampleInt((int) srcPtX, (int) srcPtY);
                dstPts[dstOff + firstIndex] = columnCorresponce;
                dstPts[dstOff + secondIndex] = rowCorrespondence;
            }
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            throw new NoninvertibleTransformException("Cannot invert transformation");
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
