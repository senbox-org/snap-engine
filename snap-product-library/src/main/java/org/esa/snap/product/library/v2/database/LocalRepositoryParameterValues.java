package org.esa.snap.product.library.v2.database;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.RemoteMission;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 20/11/2019.
 */
public class LocalRepositoryParameterValues {

    private final List<String> remoteMissionNames;
    private final Map<Short, Set<String>> attributeNamesPerMission;
    private final List<LocalRepositoryFolder> localRepositoryFolders;

    public LocalRepositoryParameterValues(List<String> remoteMissionNames, Map<Short, Set<String>> attributeNamesPerMission, List<LocalRepositoryFolder> localRepositoryFolders) {
        this.remoteMissionNames = remoteMissionNames;
        this.attributeNamesPerMission = attributeNamesPerMission;
        this.localRepositoryFolders = localRepositoryFolders;
    }

    public List<String> getRemoteMissionNames() {
        return remoteMissionNames;
    }

    public Map<Short, Set<String>> getAttributes() {
        return attributeNamesPerMission;
    }

    public List<LocalRepositoryFolder> getLocalRepositoryFolders() {
        return localRepositoryFolders;
    }
}
