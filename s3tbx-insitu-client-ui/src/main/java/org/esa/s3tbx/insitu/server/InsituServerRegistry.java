package org.esa.s3tbx.insitu.server;

import org.esa.s3tbx.insitu.server.mermaid.MermaidInsituServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Marco Peters
 */
public class InsituServerRegistry {

    private List<InsituServerSpi> serverList;

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static InsituServerRegistry getInstance() {
        return Holder.instance;
    }

    private InsituServerRegistry() {
        serverList = new ArrayList<>();
        serverList.add(new MermaidInsituServer.Spi());
    }

    public List<InsituServerSpi> getRegisteredServers() {
        return Collections.unmodifiableList(serverList);
    }

    public InsituServerSpi getRegisteredServers(String serverName) {
        final Optional<InsituServerSpi> optional = findSpi(serverName);
        return optional.isPresent() ? optional.get() : null;
    }

    boolean addServer(InsituServerSpi serverSpi) {
        final Optional<InsituServerSpi> first = findSpi(serverSpi.getName());
        return !first.isPresent() && serverList.add(serverSpi);
    }

    boolean removeServer(String serverName) {
        final Optional<InsituServerSpi> first = findSpi(serverName);
        return first.isPresent() && removeServer(first.get());
    }

    boolean removeServer(InsituServerSpi serverSpi) {
        return serverList.contains(serverSpi) && serverList.remove(serverSpi);
    }

    private Optional<InsituServerSpi> findSpi(String name) {
        return serverList.stream().filter(inSituServerSpi -> inSituServerSpi.getName().equals(name)).findFirst();
    }

    private static class Holder {
        private static final InsituServerRegistry instance = new InsituServerRegistry();
    }

}
