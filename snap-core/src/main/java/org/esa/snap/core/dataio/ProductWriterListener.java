package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

/**
 * This interface must be implemented by classes that want to trace the progress of a product read operation
 */
public interface ProductWriterListener {

    /**
     * Called right before a a product is written.
     *
     * @param pm A progress monitor that monitors the works before the product is written
     */
    void aboutToWriteProduct(ProgressMonitor pm);

}
