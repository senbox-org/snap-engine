package org.esa.beam.dataio.bigtiff;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;

public class Spike_Delete_Me {

    public static void main(String[] args) throws IOException {
        final File inputFile = new File(args[0]);

        final BigGeoTiffProductReaderPlugIn plugIn = new BigGeoTiffProductReaderPlugIn();
        if (!(plugIn.getDecodeQualification(inputFile) == DecodeQualification.SUITABLE)) {
            throw new IOException("unable to decode");
        }

        final ProductReader reader = plugIn.createReaderInstance();
        final Product product = reader.readProductNodes(inputFile, null);

        product.dispose();

        System.out.println("done");
    }
}
