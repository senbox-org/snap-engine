package org.esa.snap.product.library.v2.database;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.RemoteMission;
import org.esa.snap.remote.products.repository.Attribute;

import java.util.List;

/**
 * The data about a product saved in the local database.
 *
 * Created by jcoravu on 17/9/2019.
 */
public class SaveProductData {

    private final int productId;
    private final RemoteMission remoteMission;
    private final LocalRepositoryFolder localRepositoryFolder;
    private final List<Attribute> localAttributes;

    public SaveProductData(int productId, RemoteMission remoteMission, LocalRepositoryFolder localRepositoryFolder, List<Attribute> localAttributes) {
        this.productId = productId;
        this.remoteMission = remoteMission;
        this.localRepositoryFolder = localRepositoryFolder;
        this.localAttributes = localAttributes;
    }

    public int getProductId() {
        return productId;
    }

    public RemoteMission getRemoteMission() {
        return remoteMission;
    }

    public LocalRepositoryFolder getLocalRepositoryFolder() {
        return localRepositoryFolder;
    }

    public List<Attribute> getLocalAttributes() {
        return localAttributes;
    }
}
