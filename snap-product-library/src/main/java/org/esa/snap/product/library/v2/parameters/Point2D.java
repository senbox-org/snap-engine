package org.esa.snap.product.library.v2.parameters;

import ro.cs.tao.eodata.Polygon2D;

/**
 * Created by jcoravu on 19/8/2019.
 */
public class Point2D extends Polygon2D {

    public Point2D() {
    }

    @Override
    public String[] toWKTArray(int precision) {
        String[] values = super.toWKTArray(precision);
        String startValue = "POLYGON((";
        String endValue = "))";
        if (values[0].startsWith(startValue) && values[0].endsWith(endValue)) {
            int length = values[0].length();
            String str = "POINT(" + values[0].substring(startValue.length(), length - endValue.length()) + ")";
            values[0] = str;
        } else {
            throw new IllegalStateException("Wrong value '" + values[0] + "'.");
        }
        return values;
    }
}
