package org.esa.snap.remote.products.repository;

/**
 * Created by jcoravu on 17/9/2019.
 */
public interface ItemRenderer<ItemType> {

    public String getDisplayName(ItemType item);
}
