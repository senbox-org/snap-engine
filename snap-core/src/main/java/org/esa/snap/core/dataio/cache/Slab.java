package org.esa.snap.core.dataio.cache;

import java.awt.*;

public class Slab {

    private final Rectangle region;

    private long lastAccess;

    public Slab(Rectangle region) {
        this.region = region;
        this.lastAccess = -1L;
    }

    public Rectangle getRegion() {
        return region;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }
}
