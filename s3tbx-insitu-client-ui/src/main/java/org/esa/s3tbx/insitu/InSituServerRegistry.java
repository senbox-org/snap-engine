package org.esa.s3tbx.insitu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Marco Peters
 */
public class InSituServerRegistry {

    List<InSituServerSpi> serverList;

    private InSituServerRegistry() {
        serverList = new ArrayList<>();
        serverList.add(new MermaidInSituServer.Spi());
    }

    public List<InSituServerSpi> getRegisteredServers() {
        return Collections.unmodifiableList(serverList);
    }

    boolean addServer(InSituServerSpi serverSpi) {
        final Optional<InSituServerSpi> first = serverList.stream().filter(
                inSituServerSpi -> inSituServerSpi.getName().equals(serverSpi.getName())).findFirst();
        return !first.isPresent() && serverList.add(serverSpi);
    }

    boolean removeServer(String serverName) {
        final Optional<InSituServerSpi> first = serverList.stream().filter(
                inSituServerSpi -> inSituServerSpi.getName().equals(serverName)).findFirst();
        return first.isPresent() && removeServer(first.get());
    }

    boolean removeServer(InSituServerSpi serverSpi) {
        return serverList.contains(serverSpi) && serverList.remove(serverSpi);
    }

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static InSituServerRegistry getInstance() {
        return Holder.instance;
    }


    private static class Holder {
        private static final InSituServerRegistry instance = new InSituServerRegistry();
    }



}
