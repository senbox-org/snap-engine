package org.esa.snap.remote.products.repository;

/**
 * The data of a remote mission.
 *
 * Created by jcoravu on 14/2/2020.
 */
public class RemoteMission {

    private final String name;
    private final String repositoryName;

    public RemoteMission(String name, String repositoryName) {
        this.name = name;
        this.repositoryName = repositoryName;
    }

    public String getName() {
        return name;
    }

    public String getRepositoryName() {
        return repositoryName;
    }
}
