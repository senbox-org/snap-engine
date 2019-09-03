package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.product.library.v2.activator.DerbyDatabaseActivator;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.nio.file.Path;
import java.sql.Connection;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class DerbyDAL {

    private DerbyDAL() {

    }

    public static void saveProduct(RepositoryProduct productToSave, Path productPath) throws Exception {
        //TODO Jean remove
        productPath = productPath.resolve("MTD_MSIL1C.xml");

        Product sourceProduct = CommonReaders.readProduct(productPath.toFile());
        if (sourceProduct != null) {
            try (Connection connection = DerbyDatabaseActivator.getConnection(false)) {
                System.out.println("save the product");
            }
        }

    }
}
