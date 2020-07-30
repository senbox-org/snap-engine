package org.esa.snap.product.library.v2.database.model;

/**
 * The data about a remote mission saved in the local database.
 *
 * Created by jcoravu on 16/9/2019.
 */
public class RemoteMission {

    private final short id;
    private final String name;
    private final RemoteRepository remoteRepository;

    public RemoteMission(short id, String name, RemoteRepository remoteRepository) {
        this.id = id;
        this.name = name;
        this.remoteRepository = remoteRepository;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RemoteRepository getRemoteRepository() {
        return remoteRepository;
    }
}
