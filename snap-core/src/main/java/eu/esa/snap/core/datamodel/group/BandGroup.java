package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.datamodel.Product;

import java.util.List;

/**
 * BandGroup can be used by an application to define named lists of bands that match vertain properties.
 */
public interface BandGroup extends List<String[]> {

    /**
     * Creates a BandGroup object from textual representation
     *
     * @param text group definition String
     * @return the BandGroup
     */
    static BandGroup parse(String text) {
        return BandGroupImpl.parse(text);
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

    /**
     * Retrieves the names of bands that match the grouping criterion.
     *
     * @param product the input
     * @return the resulting band names
     */
    String[] getMatchingBandNames(Product product);
}