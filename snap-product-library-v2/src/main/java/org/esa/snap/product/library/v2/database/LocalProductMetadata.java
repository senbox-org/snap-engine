package org.esa.snap.product.library.v2.database;

import java.util.Date;

/**
 * Created by jcoravu on 9/10/2019.
 */
public class LocalProductMetadata {

    private final int id;
    private final String relativePath;
    private final Date lastModifiedDate;

    public LocalProductMetadata(int id, String relativePath, Date lastModifiedDate) {
        this.id = id;
        this.relativePath = relativePath;
        this.lastModifiedDate = lastModifiedDate;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getId() {
        return id;
    }

    public String getRelativePath() {
        return relativePath;
    }
}
