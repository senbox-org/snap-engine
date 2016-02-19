package org.esa.s3tbx.insitu;

import org.junit.Test;

/**
 * @author Marco Peters
 */
public class QueryBuilderTest {

    @Test
    public void testQueryCreation() throws Exception {
        final QueryBuilder builder = new QueryBuilder("interest");
        builder.campaign("Muscheln");
        builder.latMin(-10.943);
        builder.latMax(46.12);
        builder.lonMin(5.0);
        builder.lonMax(15.36);


    }
}