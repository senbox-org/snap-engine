package org.esa.s3tbx.idepix.algorithms.avhrr;

import org.esa.s3tbx.idepix.operators.BasisOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

/**
 * The Idepix pixel classification for AVHRR products
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "idepix.avhrr",
        version = "1.0",
        authors = "Olaf Danne, Grit Kirches",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Pixel identification and classification for AVHRR.")
public class AvhrrOp extends BasisOp {

    @SourceProduct(alias = "sourceProduct",
            label = "Landsat 8 product",
            description = "The Landsat 8 source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;


    @Parameter(defaultValue = "2.15",
            label = " Schiller NN cloud ambiguous lower boundary ",
            description = " Schiller NN cloud ambiguous lower boundary ")
    double avhrracSchillerNNCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "3.45",
            label = " Schiller NN cloud ambiguous/sure separation value ",
            description = " Schiller NN cloud ambiguous cloud ambiguous/sure separation value ")
    double avhrracSchillerNNCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.45",
            label = " Schiller NN cloud sure/snow separation value ",
            description = " Schiller NN cloud ambiguous cloud sure/snow separation value ")
    double avhrracSchillerNNCloudSureSnowSeparationValue;


    @Override
    public void initialize() throws OperatorException {
        // todo - take from AvhrrAcOp in BEAM Idepix
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AvhrrOp.class);
        }
    }
}
