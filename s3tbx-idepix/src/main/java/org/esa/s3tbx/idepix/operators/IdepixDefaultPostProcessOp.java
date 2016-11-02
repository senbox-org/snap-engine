package org.esa.s3tbx.idepix.operators;

import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 24.10.2016
 * Time: 11:46
 *
 * @author olafd
 */
public abstract class IdepixDefaultPostProcessOp extends Operator {
    @Override
    public void initialize() throws OperatorException {

    }

    abstract void setCloudShadow(int x, int y, Tile inFlagTile, Tile outFlagTile);

    public void combineFlags(int x, int y, Tile inFlagTile, Tile outFlagTile) {
        int sourceFlags = inFlagTile.getSampleInt(x, y);
        int computedFlags = outFlagTile.getSampleInt(x, y);
        outFlagTile.setSample(x, y, sourceFlags | computedFlags);
    }


    abstract void consolidateFlagging(int x, int y, Tile inFlagTile, Tile outFlagTile);
}
