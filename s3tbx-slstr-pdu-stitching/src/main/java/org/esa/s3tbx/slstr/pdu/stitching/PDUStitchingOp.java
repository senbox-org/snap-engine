package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "PduStitchingOp",
        category = "Optical",
        version = "1.0",
        authors = "Tonio Fincke",
        copyright = "Copyright (C) 2015 by Brockmann Consult (info@brockmann-consult.de)",
        description = "Stitches multiple SLSTR L1B product dissemination units (PDUs) of the same orbit to a single product.",
        autoWriteDisabled = true)
public class PDUStitchingOp extends Operator {

    @SourceProducts
    private Product[] sourceProducts;

    @Parameter
    private File targetDir;

    @Override
    public void initialize() throws OperatorException {
        final File[] files = new File[sourceProducts.length];
        for (int i = 0; i < sourceProducts.length; i++) {
            files[i] = sourceProducts[i].getFileLocation();
        }
        try {
            SlstrPduStitcher.createStitchedSlstrL1BFile(targetDir, files);
            setDummyTargetProduct();
        } catch (IOException | PDUStitchingException | ParserConfigurationException | TransformerException e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private void setDummyTargetProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PDUStitchingOp.class);
        }
    }

}
