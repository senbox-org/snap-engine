package org.esa.snap.product.library.v2;

import org.esa.snap.remote.products.repository.QueryFilter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class AllLocalFolderProductsRepository {

    public AllLocalFolderProductsRepository() {

    }

    public List<QueryFilter> getParameters() {
        List<QueryFilter> parameters = new ArrayList<QueryFilter>();

        //parameters.add(new QueryFilter("mission", String.class, "Mission", null, false, new String[0]));
        parameters.add(new QueryFilter("start", Date.class, "Start date", null, false, null));
        parameters.add(new QueryFilter("end", Date.class, "End date", null, false, null));
        parameters.add(new QueryFilter("footprint", Rectangle.Double.class, "Area of interest", null, false, null));

        return parameters;
    }
}
