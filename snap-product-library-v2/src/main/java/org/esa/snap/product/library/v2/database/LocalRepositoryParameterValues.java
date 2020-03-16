package org.esa.snap.product.library.v2.database;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 20/11/2019.
 */
public class LocalRepositoryParameterValues {

    private final List<String> remoteMissionNames;
    private final Map<Short, Set<String>> remoteAttributeNamesPerMission;
    private final List<LocalRepositoryFolder> localRepositoryFolders;
    private final Set<String> localAttributeNames;

    public LocalRepositoryParameterValues(List<String> remoteMissionNames, Map<Short, Set<String>> remoteAttributeNamesPerMission,
                                          Set<String> localAttributeNames, List<LocalRepositoryFolder> localRepositoryFolders) {

        this.remoteMissionNames = remoteMissionNames;
        this.remoteAttributeNamesPerMission = remoteAttributeNamesPerMission;
        this.localAttributeNames = localAttributeNames;
        this.localRepositoryFolders = localRepositoryFolders;
    }

    public Set<String> getLocalAttributeNames() {
        return localAttributeNames;
    }

    public List<String> getRemoteMissionNames() {
        return remoteMissionNames;
    }

    public Map<Short, Set<String>> getRemoteAttributeNamesPerMission() {
        return remoteAttributeNamesPerMission;
    }

    public List<LocalRepositoryFolder> getLocalRepositoryFolders() {
        return localRepositoryFolders;
    }
}
