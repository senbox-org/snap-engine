package org.esa.snap.product.library.v2.database;

/**
 * Created by jcoravu on 17/9/2019.
 */
public class SaveProductData {

    private final RemoteMission remoteMission;
    private final LocalRepositoryFolder localRepositoryFolder;

    public SaveProductData(RemoteMission remoteMission, LocalRepositoryFolder localRepositoryFolder) {
        this.remoteMission = remoteMission;
        this.localRepositoryFolder = localRepositoryFolder;
    }

    public RemoteMission getRemoteMission() {
        return remoteMission;
    }

    public LocalRepositoryFolder getLocalRepositoryFolder() {
        return localRepositoryFolder;
    }
}
