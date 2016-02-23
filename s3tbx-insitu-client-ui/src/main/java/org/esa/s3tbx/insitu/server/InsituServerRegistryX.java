package org.esa.s3tbx.insitu.server;

import org.esa.s3tbx.insitu.server.mermaid.MermaidInsituServerX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Marco Peters
 */
public class InsituServerRegistryX {

    private List<InsituServerSpiX> serverList;

    /**
     * Gets the singleton instance of the registry.
     *
     * @return The instance.
     */
    public static InsituServerRegistryX getInstance() {
        return Holder.instance;
    }

    private InsituServerRegistryX() {
        serverList = new ArrayList<>();
        serverList.add(new MermaidInsituServerX.Spi());
    }

    public List<InsituServerSpiX> getRegisteredServers() {
        return Collections.unmodifiableList(serverList);
    }

    public InsituServerSpiX getRegisteredServers(String serverName) {
        final Optional<InsituServerSpiX> optional = findSpi(serverName);
        return optional.isPresent() ? optional.get() : null;
    }

    boolean addServer(InsituServerSpiX serverSpi) {
        final Optional<InsituServerSpiX> first = findSpi(serverSpi.getName());
        return !first.isPresent() && serverList.add(serverSpi);
    }

    boolean removeServer(String serverName) {
        final Optional<InsituServerSpiX> first = findSpi(serverName);
        return first.isPresent() && removeServer(first.get());
    }

    boolean removeServer(InsituServerSpiX serverSpi) {
        return serverList.contains(serverSpi) && serverList.remove(serverSpi);
    }

    private Optional<InsituServerSpiX> findSpi(String name) {
        return serverList.stream().filter(inSituServerSpi -> inSituServerSpi.getName().equals(name)).findFirst();
    }

    private static class Holder {
        private static final InsituServerRegistryX instance = new InsituServerRegistryX();
    }

}
