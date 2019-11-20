package org.esa.snap.product.library.v2.database;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 20/11/2019.
 */
public class LocalRepositoryParameterValues {

    private final List<RemoteMission> missions;
    private final Map<Short, Set<String>> attributeNamesPerMission;
    private final List<LocalRepositoryFolder> localRepositoryFolders;

    public LocalRepositoryParameterValues(List<RemoteMission> missions, Map<Short, Set<String>> attributeNamesPerMission, List<LocalRepositoryFolder> localRepositoryFolders) {
        this.missions = missions;
        this.attributeNamesPerMission = attributeNamesPerMission;
        this.localRepositoryFolders = localRepositoryFolders;
    }

    public List<RemoteMission> getMissions() {
        return missions;
    }

    public Map<Short, Set<String>> getAttributes() {
        return attributeNamesPerMission;
    }

    public List<LocalRepositoryFolder> getLocalRepositoryFolders() {
        return localRepositoryFolders;
    }
}
