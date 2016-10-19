package org.esa.s3tbx.idepix.algorithms.seawifs;

import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;

/**
 * SeaWiFS post processing operator, operating on tiles:
 * - cloud buffer
 * - ...
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Seawifs.Postprocess",
        version = "2.2",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Refines the SeaWiFS pixel classification.",
        internal = true)
public class SeaWifsPostProcessingOp extends BasisOp {

    @Override
    public void initialize() throws OperatorException {

    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SeaWifsPostProcessingOp.class);
        }
    }

}
