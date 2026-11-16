package org.esa.snap.statistics;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import java.nio.file.Path;
import java.io.IOException;

public class ProductLoader {

    public Product loadProduct(Path path) throws IOException {
        try {
            return ProductIO.readProduct(path);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }
}
