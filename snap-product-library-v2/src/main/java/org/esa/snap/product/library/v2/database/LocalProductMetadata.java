package org.esa.snap.product.library.v2.database;


import java.time.LocalDateTime;

/**
 * The data about a local product.
 *
 * Created by jcoravu on 9/10/2019.
 */
public class LocalProductMetadata {

    private final int id;
    private final String relativePath;
    private final LocalDateTime lastModifiedDate;

    public LocalProductMetadata(int id, String relativePath, LocalDateTime lastModifiedDate) {
        this.id = id;
        this.relativePath = relativePath;
        this.lastModifiedDate = lastModifiedDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getId() {
        return id;
    }

    public String getRelativePath() {
        return relativePath;
    }
}
