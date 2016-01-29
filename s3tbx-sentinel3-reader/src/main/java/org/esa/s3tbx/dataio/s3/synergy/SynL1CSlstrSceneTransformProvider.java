package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.SceneTransformProvider;
import org.esa.snap.core.transform.AbstractTransform2D;
import org.esa.snap.core.transform.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

/**
 * @author Tonio Fincke
 */
public class SynL1CSlstrSceneTransformProvider implements SceneTransformProvider {

    private final SynL1CSceneToModelTransform sceneToModelTransform;

    public SynL1CSlstrSceneTransformProvider(Band colCorrespondenceBand, Band rowCorrespondenceBand) {
        sceneToModelTransform = new SynL1CSceneToModelTransform(colCorrespondenceBand, rowCorrespondenceBand);
    }

    @Override
    public MathTransform2D getModelToSceneTransform() {
        return MathTransform2D.NULL;
    }

    @Override
    public MathTransform2D getSceneToModelTransform() {
        return sceneToModelTransform;
    }

    private class SynL1CSceneToModelTransform extends AbstractTransform2D {

        private final Band colCorrespondenceBand;
        private final Band rowCorrespondenceBand;

        private SynL1CSceneToModelTransform(Band colCorrespondenceBand, Band rowCorrespondenceBand) {
            this.colCorrespondenceBand = colCorrespondenceBand;
            this.rowCorrespondenceBand = rowCorrespondenceBand;
        }

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            final double srcPtX = ptSrc.getX();
            final double srcPtY = ptSrc.getY();
            if (Double.isNaN(srcPtX) || Double.isNaN(srcPtY) || srcPtX < 0 || srcPtX >= colCorrespondenceBand.getRasterWidth() ||
                    srcPtY < 0 || srcPtY >= colCorrespondenceBand.getRasterHeight()) {
                throw new TransformException("Could not transform");
            }
            final int columnCorrespondence =
                    colCorrespondenceBand.getSampleInt((int) srcPtX, (int) srcPtY);
            final int rowCorrespondence =
                    rowCorrespondenceBand.getSampleInt((int) srcPtX, (int) srcPtY);
            if (columnCorrespondence < 0 || rowCorrespondence < 0) {
                throw new TransformException("Could not transform");
            }
            if (ptDst == null) {
                ptDst = new Point2D.Double();
            }
            ptDst.setLocation(columnCorrespondence, rowCorrespondence);
            return ptDst;
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            throw new NoninvertibleTransformException("Cannot invert transformation");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof SynL1CSceneToModelTransform)) {
                return false;
            }
            return ((SynL1CSceneToModelTransform) object).colCorrespondenceBand == colCorrespondenceBand &&
                    ((SynL1CSceneToModelTransform) object).rowCorrespondenceBand == rowCorrespondenceBand;
        }

        @Override
        public int hashCode() {
            return colCorrespondenceBand.getName().hashCode() + rowCorrespondenceBand.getName().hashCode();
        }
    }

}
