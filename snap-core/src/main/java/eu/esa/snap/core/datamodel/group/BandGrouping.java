package eu.esa.snap.core.datamodel.group;

import java.util.List;

/**
 * AutoGrouping can be used by an application to auto-group a long list of product nodes (e.g. bands)
 * as a tree of product nodes.
 *
 * @since BEAM 4.8
 */
public interface BandGrouping extends List<String[]> {

    static BandGrouping parse(String text) {
        return BandGroupingImpl.parse(text);
    }

    /**
     * Gets the index of the first group path that matches the given name.
     *
     * @param name A product node name.
     * @return The index of the group path or {@code -1} if no group path matches the given name.
     */
    int indexOf(String name);

    /**
     * Retrieves the name of the BandGrouping.
     *
     * @return a name or an empty String
     */
    String getName();

    /**
     * Associates a name with the BandGrouping
     *
     * @param name the name
     */
    void setName(String name);
}