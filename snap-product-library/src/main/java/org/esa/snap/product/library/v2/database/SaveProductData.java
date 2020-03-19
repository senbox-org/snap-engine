package org.esa.snap.product.library.v2.database;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.RemoteMission;

/**
 * Created by jcoravu on 17/9/2019.
 */
public class SaveProductData {

    private final int productId;
    private final RemoteMission remoteMission;
    private final LocalRepositoryFolder localRepositoryFolder;

    public SaveProductData(int productId, RemoteMission remoteMission, LocalRepositoryFolder localRepositoryFolder) {
        this.productId = productId;
        this.remoteMission = remoteMission;
        this.localRepositoryFolder = localRepositoryFolder;
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
}
