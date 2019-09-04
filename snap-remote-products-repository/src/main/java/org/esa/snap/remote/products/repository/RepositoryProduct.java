package org.esa.snap.remote.products.repository;

import java.util.Date;

/**
 * Created by jcoravu on 12/8/2019.
 */
public interface RepositoryProduct {

    public Attribute[] getAttributes();

    public String getName();

    public long getApproximateSize();

    public String getDownloadQuickLookImageURL();

    public String getDownloadURL();

    public Date getAcquisitionDate();

    public String getMission();
}
