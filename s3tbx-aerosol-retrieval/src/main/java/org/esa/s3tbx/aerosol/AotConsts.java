/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

import org.esa.snap.core.datamodel.ProductData;

/**
 *
 * @author akheckel
 */
public enum AotConsts {
    aot("aot", "aerosol optical thickness", "dl", ProductData.TYPE_FLOAT32, -1, true, 1, 0),
    aotErr("aot_err", "aot uncertainty", "dl", ProductData.TYPE_FLOAT32, -1, true, 1, 0),
    aotFlags("aot_flags", "aerosol retrieval qa flags", "dl", ProductData.TYPE_UINT8, 0, false, 1, 0);

    public final String name;
    public final String description;
    public final String unit;
    public final int type;
    public final double noDataValue;
    public final boolean noDataUsed;
    public final double scale;
    public final double offset;
    
    AotConsts(String name, String description, String unit, int type,
                      double noDataValue, boolean noDataUsed,
                      double scale, double offset) {
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.type = type;
        this.scale = scale;
        this.offset = offset;
        this.noDataValue = noDataValue;
        this.noDataUsed = noDataUsed;
    }
}
