package org.esa.s3tbx.idepix.algorithms.meris;

import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;

/**
 * The Idepix pixel classification for MERIS products
 *
 * @author olafd
 */
@OperatorMetadata(alias = "idepix.meris",
        category = "Optical/Pre-Processing",
        version = "2.3",
        copyright = "(c) 2014 by Brockmann Consult",
        description = "Pixel identification for MERIS.")
public class MerisOp extends BasisOp {
    @Override
    public void initialize() throws OperatorException {
        // todo - take from CawaOp in BEAM Idepix
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisOp.class);
        }
    }
}
