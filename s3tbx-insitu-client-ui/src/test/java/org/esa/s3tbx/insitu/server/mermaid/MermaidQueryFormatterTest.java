package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituQuery;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class MermaidQueryFormatterTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testQueryCreation() throws Exception {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        final InsituQuery query= new InsituQuery();
        query.subject(InsituQuery.SUBJECT.PARAMETERS);
        query.datasets(new String[]{"Muscheln", "Robben"});
        query.latMin(-10.943);
        query.latMax(46.12);
        query.lonMin(5.0);
        query.lonMax(15.36);
        query.startDate(dateFormat.parse("01-Jan-2014 00:00:00"));
        query.stopDate(dateFormat.parse("31-Dec-2015 00:00:00"));
        query.param(new String[]{"param1", "param2", "param3"});
        query.limit(10);
        query.shift(5);
        query.countOnly(true);
        String queryString = MermaidQueryFormatter.format(query);
        assertTrue(queryString.startsWith("/parameters?"));
        assertTrue(queryString.contains("campaign=Muscheln,Robben"));
        assertTrue(queryString.contains("lat_min=-10.943"));
        assertTrue(queryString.contains("lon_min=5.0"));
        assertTrue(queryString.contains("lat_max=46.12"));
        assertTrue(queryString.contains("lon_max=15.36"));
        assertTrue(queryString.contains("start_date=2014-01-01+00%3A00%3A00")); // URL encoded
        assertTrue(queryString.contains("stop_date=2015-12-31+00%3A00%3A00")); // URL encoded
        assertTrue(queryString.contains("param=param1,param2,param3"));
        assertTrue(queryString.contains("limit=10"));
        assertTrue(queryString.contains("shift=5"));
        assertTrue(queryString.contains("count_only"));
        assertEquals(10, queryString.chars().filter(value -> value == '&').count());
        final int lastIndex = queryString.length() - 1;
        assertTrue(queryString.charAt(lastIndex) != '&');

    }
}