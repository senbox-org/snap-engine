package org.esa.snap.core.subset;

import org.locationtech.jts.geom.Polygon;

import java.awt.*;

/**
 * @author Adrian Draghici
 */
public class SubsetRegionInfo {

    private final Rectangle subsetExtent;
    private final Polygon subsetPolygon;

    public SubsetRegionInfo(Rectangle subsetExtent, Polygon subsetPolygon) {
        this.subsetExtent = subsetExtent;
        this.subsetPolygon = subsetPolygon;
    }

    public Rectangle getSubsetExtent() {
        return subsetExtent;
    }

    public Polygon getSubsetPolygon() {
        return subsetPolygon;
    }
}
