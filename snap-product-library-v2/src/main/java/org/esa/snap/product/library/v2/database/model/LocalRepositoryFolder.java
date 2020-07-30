package org.esa.snap.product.library.v2.database.model;

import java.nio.file.Path;

/**
 * The data about a local repository folder saved in the local database.
 *
 * Created by jcoravu on 16/9/2019.
 */
public class LocalRepositoryFolder {

    private final short id;
    private final Path path;

    public LocalRepositoryFolder(short id, Path path) {
        this.id = id;
        this.path = path;
    }

    public short getId() {
        return id;
    }

    public Path getPath() {
        return path;
    }
}
