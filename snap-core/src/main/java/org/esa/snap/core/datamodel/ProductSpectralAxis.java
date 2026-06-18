package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;

import java.util.List;

/**
 * Describes a product-specific spectral series by the product band names that belong to it.
 */
public final class ProductSpectralAxis {

    public static final String DEFAULT_ID = "default";
    public static final String DEFAULT_NAME = "Spectral bands";

    private final String id;
    private final String name;
    private final List<String> bandNames;

    public ProductSpectralAxis(String id, String name, List<String> bandNames) {
        Assert.notNull(id, "id");
        Assert.notNull(name, "name");
        Assert.notNull(bandNames, "bandNames");
        this.id = id;
        this.name = name;
        this.bandNames = List.copyOf(bandNames);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getBandNames() {
        return bandNames;
    }

    @Override
    public String toString() {
        return name;
    }
}
