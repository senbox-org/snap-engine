package org.esa.snap.product.library.v2.database;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.RemoteMission;

import java.util.Set;

/**
 * Created by jcoravu on 9/10/2019.
 */
public class SaveDownloadedProductData extends SaveProductData {

    private final Set<String> productAttributeNames;

    public SaveDownloadedProductData(int productId, RemoteMission remoteMission, LocalRepositoryFolder localRepositoryFolder, Set<String> productAttributeNames) {
        super(productId, remoteMission, localRepositoryFolder);

        this.productAttributeNames = productAttributeNames;
    }

    public Set<String> getProductAttributeNames() {
        return productAttributeNames;
    }
}
