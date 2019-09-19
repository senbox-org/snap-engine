package org.esa.snap.product.library.v2;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.ItemRenderer;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.SensorType;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class AllLocalFolderProductsRepository {

    public static final String START_DATE_PARAMETER = "startDate";
    public static final String END_DATE_PARAMETER = "endDate";
    public static final String FOOT_PRINT_PARAMETER = "footprint";
    public static final String SENSOR_TYPE_PARAMETER = "sensorType";
    public static final String METADATA_ATTRIBUTES_PARAMETER = "metadataAttributes";

    public AllLocalFolderProductsRepository() {
    }

    public List<QueryFilter> getParameters() {
        List<QueryFilter> parameters = new ArrayList<QueryFilter>();

        parameters.add(new QueryFilter(START_DATE_PARAMETER, Date.class, "Start date", null, false, null));
        parameters.add(new QueryFilter(END_DATE_PARAMETER, Date.class, "End date", null, false, null));

        ItemRenderer<Object> sensorTypeRenderer = new ItemRenderer<Object>() {
            @Override
            public String getDisplayName(Object item) {
                return ((SensorType)item).getName();
            }
        };
        SensorType[] sensorTypes = new SensorType[] {SensorType.ALTIMETRIC, SensorType.ATMOSPHERIC, SensorType.OPTICAL, SensorType.RADAR, SensorType.UNKNOWN};
        parameters.add(new QueryFilter(SENSOR_TYPE_PARAMETER, Object[].class, "Sensor", null, false, sensorTypeRenderer, sensorTypes));
        parameters.add(new QueryFilter(METADATA_ATTRIBUTES_PARAMETER, Attribute.class, "Attributes", null, false, null, null));
        parameters.add(new QueryFilter(FOOT_PRINT_PARAMETER, Rectangle2D.class, "Area of interest", null, false, null));

        return parameters;
    }
}
