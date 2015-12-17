package org.esa.s3tbx.idepix.algorithms.modis;

import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;

/**
 * The Idepix pixel classification for MODIS products
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "idepix.modis",
        version = "1.0",
        authors = "Olaf Danne, Marco Zuehlke",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Pixel identification and classification for MODIS.")
public class ModisOp extends BasisOp {
    @Override
    public void initialize() throws OperatorException {
        // todo - take from OccciOp in BEAM Idepix
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ModisOp.class);
        }
    }
}
