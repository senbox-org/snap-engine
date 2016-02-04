package org.esa.s3tbx.idepix.algorithms.probav;

import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;

/**
 * The Idepix pixel classification for PROBA-V Synthesis products
 *
 * @author olafd
 */
@OperatorMetadata(alias = "idepix.probav",
        category = "Optical/Pre-Processing",
        version = "1.0",
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for PROBA-V.")
public class ProbaVOp extends BasisOp {
    @Override
    public void initialize() throws OperatorException {
        // todo - take from GlobAlbedoOp in BEAM Idepix
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ProbaVOp.class);
        }
    }
}
