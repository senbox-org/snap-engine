package org.esa.snap.statistics.tools;

import org.esa.snap.core.datamodel.ProductData;

public class TimeInterval {

    int id;
    ProductData.UTC intervalStart;
    ProductData.UTC intervalEnd;

    public TimeInterval(int id, ProductData.UTC intervalStart, ProductData.UTC intervalEnd) {
        this.id = id;
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
    }

    public int getId() {
        return id;
    }

    public ProductData.UTC getIntervalStart() {
        return intervalStart;
    }

    public ProductData.UTC getIntervalEnd() {
        return intervalEnd;
    }
}
