package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.framework.datamodel.Product;

/**
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "Slstr.PDU.Stitching", authors = "Tonio Fincke", copyright = "Brockmann Consult GmbH",
        category = "Raster/Geometric Operations",
        version = "1.0",
        description = "Stitches multiple Sentinel-3 SLSTR L1B products into a single one.")
public class SlstrPduStitchingOp extends Operator {

    @SourceProduct(alias = "source", label = "Source product", description="The source product.")
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {

    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SlstrPduStitchingOp.class);
        }
    }

}
