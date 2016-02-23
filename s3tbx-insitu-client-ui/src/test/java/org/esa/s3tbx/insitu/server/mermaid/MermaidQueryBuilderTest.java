package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class MermaidQueryBuilderTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testQueryCreation() throws Exception {
        final MermaidQueryBuilder builder = new MermaidQueryBuilder();
        builder.subject("interest");
        builder.campaign("Muscheln");
        builder.latMin(-10.943);
        builder.latMax(46.12);
        builder.lonMin(5.0);
        builder.lonMax(15.36);
        builder.startDate(ProductData.UTC.parse("01-Jan-2014 00:00:00"));
        builder.stopDate(ProductData.UTC.parse("31-Dec-2015 00:00:00"));
        builder.param(new String[]{"param1", "param2", "param3"});
        builder.limit(10);
        builder.shift(5);
        builder.countOnly(true);
        String query = builder.createQuery();
        assertTrue(query.startsWith("/interest?"));
        assertTrue(query.contains("campaign=Muscheln"));
        assertTrue(query.contains("latMin=-10.943"));
        assertTrue(query.contains("lonMin=5.0"));
        assertTrue(query.contains("latMax=46.12"));
        assertTrue(query.contains("lonMax=15.36"));
        assertTrue(query.contains("startDate=2014-01-01 00:00:00"));
        assertTrue(query.contains("stopDate=2015-12-31 00:00:00"));
        assertTrue(query.contains("param=param1,param2,param3"));
        assertTrue(query.contains("limit=10"));
        assertTrue(query.contains("shift=5"));
        assertTrue(query.contains("count_only"));
        assertEquals(10, query.chars().filter(value -> value == '&').count());
        final int lastIndex = query.length() - 1;
        assertTrue(query.charAt(lastIndex) != '&');

    }
}