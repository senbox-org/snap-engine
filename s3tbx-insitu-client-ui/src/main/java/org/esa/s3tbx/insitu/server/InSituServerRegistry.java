package org.esa.s3tbx.insitu.server;

import org.esa.s3tbx.insitu.server.mermaid.MermaidInSituServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Marco Peters
 */
public class InSituServerRegistry {

    private List<InSituServerSpi> serverList;

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static InSituServerRegistry getInstance() {
        return Holder.instance;
    }

    private InSituServerRegistry() {
        serverList = new ArrayList<>();
        serverList.add(new MermaidInSituServer.Spi());
    }

    public List<InSituServerSpi> getRegisteredServers() {
        return Collections.unmodifiableList(serverList);
    }

    boolean addServer(InSituServerSpi serverSpi) {
        final Optional<InSituServerSpi> first = findSpi(serverSpi.getName());
        return !first.isPresent() && serverList.add(serverSpi);
    }

    boolean removeServer(String serverName) {
        final Optional<InSituServerSpi> first = findSpi(serverName);
        return first.isPresent() && removeServer(first.get());
    }

    boolean removeServer(InSituServerSpi serverSpi) {
        return serverList.contains(serverSpi) && serverList.remove(serverSpi);
    }

    private Optional<InSituServerSpi> findSpi(String name) {
        return serverList.stream().filter(inSituServerSpi -> inSituServerSpi.getName().equals(name)).findFirst();
    }

    private static class Holder {
        private static final InSituServerRegistry instance = new InSituServerRegistry();
    }

}
