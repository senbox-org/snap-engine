package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.SceneTransformProvider;
import org.esa.snap.core.transform.AbstractTransform2D;
import org.esa.snap.core.transform.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Tonio Fincke
 */
class SynL1COlciSceneTransformProvider implements SceneTransformProvider {

    private final SynL1COlciModelToSceneTransform modelToSceneTransform;
    private final SynL1COlciSceneToModelTransform sceneToModelTransform;

    SynL1COlciSceneTransformProvider(Band columnMisregistrationBand, Band rowMisregistrationBand) {
        modelToSceneTransform = new SynL1COlciModelToSceneTransform(columnMisregistrationBand, rowMisregistrationBand);
        sceneToModelTransform = new SynL1COlciSceneToModelTransform(columnMisregistrationBand, rowMisregistrationBand);
    }

    @Override
    public MathTransform2D getModelToSceneTransform() {
        return modelToSceneTransform;
    }

    @Override
    public MathTransform2D getSceneToModelTransform() {
        return sceneToModelTransform;
    }

    private class SynL1COlciModelToSceneTransform extends AbstractTransform2D {

        private final Band columnMisregistrationBand;
        private final Band rowMisregistrationBand;

        SynL1COlciModelToSceneTransform(Band columnMisregistrationBand, Band rowMisregistrationBand) {
            this.columnMisregistrationBand = columnMisregistrationBand;
            this.rowMisregistrationBand = rowMisregistrationBand;
        }

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            final double srcPtX = ptSrc.getX();
            final double srcPtY = ptSrc.getY();
            if (srcPtX < 0 || srcPtX >= columnMisregistrationBand.getRasterWidth()) {
                throw new TransformException("Could not transform");
            }
            final int columnMisregistration = columnMisregistrationBand.getSampleInt((int) srcPtX, 0);
            final int rowMisregistration = rowMisregistrationBand.getSampleInt((int) srcPtX, 0);
            if (ptDst != null) {
                ptDst.setLocation(srcPtX + columnMisregistration, srcPtY + rowMisregistration);
            } else {
                ptDst = new Point2D.Double(srcPtX + columnMisregistration, srcPtY + rowMisregistration);
            }
            return ptDst;
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return sceneToModelTransform;
        }

    }

    private class SynL1COlciSceneToModelTransform extends AbstractTransform2D {

        private static final int dims = 2;
        private final int[] columnMisregistration;
        private final int[] rowMisregistration;

        private final static int invalid_value = Integer.MIN_VALUE;

        SynL1COlciSceneToModelTransform(Band columnMisregistrationBand, Band rowMisregistrationBand) {
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
                            misregistrationArray[j] = (int) (lastValidValue +
                                    (lastValidValue - nextValidValue) *
                                            ((nextValidIndex - j) / (double) nextValidIndex - i));
                        }
                    }
                }
            }
            return misregistrationArray;
        }

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            final double srcPtX = ptSrc.getX();
            final double srcPtY = ptSrc.getY();
            if (srcPtX < 0 || srcPtX >= columnMisregistration.length) {
                throw new TransformException("Could not transform");
            }
            if (ptDst != null) {
                ptDst.setLocation(srcPtX + columnMisregistration[(int) srcPtX], srcPtY + rowMisregistration[(int) srcPtX]);
            } else {
                ptDst = new Point2D.Double(srcPtX + columnMisregistration[(int) srcPtX], srcPtY + rowMisregistration[(int) srcPtX]);
            }
            return ptDst;
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return modelToSceneTransform;
        }
    }

}
