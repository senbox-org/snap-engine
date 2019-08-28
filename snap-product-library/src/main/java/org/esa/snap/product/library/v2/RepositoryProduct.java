package org.esa.snap.product.library.v2;

import java.util.Date;

/**
 * Created by jcoravu on 12/8/2019.
 */
public interface RepositoryProduct {

    public String getName();

    public String getType();

    public String getInstrument();

    public long getApproximateSize();

    public String getQuickLookLocation();

    public String getLocation();

    public Date getAcquisitionDate();

    public String getMission();
}
