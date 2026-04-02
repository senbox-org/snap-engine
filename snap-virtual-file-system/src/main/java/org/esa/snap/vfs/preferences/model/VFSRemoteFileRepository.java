package org.esa.snap.vfs.preferences.model;

import java.util.List;

/**
 * VFS Remote File Repository data structure for storing VFS connection data.
 *
 * @author Adrian Drăghici
 */
public record VFSRemoteFileRepository(String name, String scheme, String address, List<Property> properties) {

    /**
     * Creates a new VFS Remote File Repository data structure.
     *
     * @param name       The name of VFS Remote File Repository
     * @param scheme     The scheme of VFS Remote File Repository
     * @param address    The address of VFS Remote File Repository
     * @param properties The properties of VFS Remote File Repository
     */
    public VFSRemoteFileRepository {
    }

    /**
     * Gets the name of VFS Remote File Repository
     *
     * @return The name of VFS Remote File Repository
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Gets the root of VFS Remote File Repository
     *
     * @return The root of VFS Remote File Repository
     */
    public String getRoot() {
        return this.name + ":";
    }

    /**
     * Gets the scheme of VFS Remote File Repository
     *
     * @return The scheme of VFS Remote File Repository
     */
    @Override
    public String scheme() {
        return this.scheme;
    }

    /**
     * Gets the address of VFS Remote File Repository
     *
     * @return The address of VFS Remote File Repository
     */
    @Override
    public String address() {
        return this.address;
    }

    /**
     * Gets the properties of VFS Remote File Repository
     *
     * @return The properties of VFS Remote File Repository
     */
    @Override
    public List<Property> properties() {
        return this.properties;
    }
}
