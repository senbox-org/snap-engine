package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerRegistry;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
@Ignore("Should not run with automatic tests")
public class MermaidServerTest {

    @Test
    public void testMermaidServer() throws Exception {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        final InsituServerRegistry registry = InsituServerRegistry.getInstance();
        final InsituServerSpi serverSpi = registry.getRegisteredServer("MERMAID");
        final InsituServer server = serverSpi.createServer();

        final InsituQuery query = new InsituQuery();

//        http://mermaid.acri.fr/s3tbx/v1/observations?lon_min=20.5&lat_min=3.2&lon_max=80.9&lat_max=9.7&start_date=2003-09-01 17:20:11&stop_date=2003-09-23 12:15:36&param=es_412,es_443
        query.lonMin(20.5);
        query.lonMax(80.9);
        query.latMin(3.2);
        query.latMax(9.7);
        query.startDate(dateFormat.parse("01-SEP-2003 15:20:11"));
        query.stopDate(dateFormat.parse("23-SEP-2003 10:15:36"));
        query.param(new String[]{"es_412", "es_443"});
        query.subject("observations");
        final InsituResponse response = server.query(query);

        assertEquals(InsituResponse.STATUS_CODE.OK, response.getStatus());
        assertEquals(12, response.getObservationCount());

    }
}