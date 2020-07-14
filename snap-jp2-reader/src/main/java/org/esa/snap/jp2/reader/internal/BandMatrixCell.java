package org.esa.snap.jp2.reader.internal;

import org.esa.snap.core.image.MosaicMatrix;

/**
 * Created by jcoravu on 30/4/2020.
 */
public interface BandMatrixCell extends MosaicMatrix.MatrixCell {

    public int getDataBufferType();
}
