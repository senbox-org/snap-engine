package org.esa.snap.product.library.v2.database.model;

/**
 * Created by jcoravu on 16/9/2019.
 */
public class RemoteRepository {

    private final short id;
    private final String name;

    public RemoteRepository(short id, String name) {
        this.id = id;
        this.name = name;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
