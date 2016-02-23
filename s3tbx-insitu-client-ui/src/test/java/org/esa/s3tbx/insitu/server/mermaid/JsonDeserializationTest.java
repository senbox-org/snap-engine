package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.esa.s3tbx.insitu.server.InsituObservation;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class JsonDeserializationTest {

    private Gson gson;

    @Before
    public void setUp() throws Exception {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new UtcDateTypeAdapter());
        gson = gsonBuilder.create();
    }

    @Test
    public void testParseCampaignsResponse() {
        final InputStream jsonStream = JsonDeserializationTest.class.getResourceAsStream("CampaignsResponse.json");
        final MermaidResponse response = gson.fromJson(new InputStreamReader(jsonStream), MermaidResponse.class);
        assertNotNull(response);

        assertEquals(InsituResponse.STATUS_CODE.OK, response.getStatus());
        assertNull(response.getFailureReasons());
        final List<CampaignDescr> campaigns = response.getCampaignDescriptions();
        assertNotNull(campaigns);
        assertEquals(1, campaigns.size());
        final CampaignDescr campaignDescr = campaigns.get(0);
        assertEquals("BOUSSOLE", campaignDescr.getIdentifier());
        assertEquals("David Antoine", campaignDescr.getPi());
    }

    @Test
    public void testParseParametersResponse() {
        final InputStream jsonStream = JsonDeserializationTest.class.getResourceAsStream("ParametersResponse.json");
        final MermaidResponse response = gson.fromJson(new InputStreamReader(jsonStream), MermaidResponse.class);
        assertNotNull(response);

        assertEquals(InsituResponse.STATUS_CODE.OK, response.getStatus());
        assertNull(response.getFailureReasons());
        final List<MermaidParameter> parameters = response.getParameters();
        assertNotNull(parameters);
        assertEquals(23, parameters.size());
        final MermaidParameter parameter = parameters.get(4);
        assertEquals("es_510", parameter.getName());
        assertEquals("radiance", parameter.getType());
        assertEquals("mW.m-2.nm-1", parameter.getUnit());
        assertEquals("Sea-level solar illumination at 510nm", parameter.getDescription());
    }

    @Test
    public void testParseObservationsResponse() throws ParseException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        final InputStream jsonStream = JsonDeserializationTest.class.getResourceAsStream("ObservationsResponse.json");
        final MermaidResponse response = gson.fromJson(new InputStreamReader(jsonStream), MermaidResponse.class);
        assertNotNull(response);

        assertEquals(InsituResponse.STATUS_CODE.OK, response.getStatus());
        assertNull(response.getFailureReasons());
        assertEquals(484, response.getObservationCount());
        final List<Campaign> campaignList = response.getCampaignList();
        assertNotNull(campaignList);
        assertEquals(2, campaignList.size());

        final Campaign aeronetCampaign = campaignList.get(0);
        assertEquals("AERONET", aeronetCampaign.getName());
        final List<MermaidObservation> aeronetObservations = aeronetCampaign.getObservations();
        assertEquals(200, aeronetObservations.size());
        final InsituObservation aeronetObs = aeronetObservations.get(12);
        assertEquals("es_412", aeronetObs.getParam());
        assertEquals(45.314, aeronetObs.getLon(), 1.0e-6);
        assertEquals(12.508, aeronetObs.getLat(), 1.0e-6);
        assertEquals(1043.825711, aeronetObs.getValue(), 1.0e-6);

        assertEquals(dateFormat.parse("22-APR-2002 12:21:56"), aeronetObs.getDate());

        final Campaign boussoleCampaign = campaignList.get(1);
        assertEquals("BOUSSOLE", boussoleCampaign.getName());
        final List<MermaidObservation> boussoleObservations = boussoleCampaign.getObservations();
        assertEquals(284, boussoleObservations.size());
        final InsituObservation boussoleObs = boussoleObservations.get(0);
        assertEquals("es_412", boussoleObs.getParam());
        assertEquals(43.367, boussoleObs.getLon(), 1.0e-6);
        assertEquals(7.9, boussoleObs.getLat(), 1.0e-6);
        assertEquals(748.971558, boussoleObs.getValue(), 1.0e-6);
        assertEquals(dateFormat.parse("23-SEP-2003 10:15:35"), boussoleObs.getDate());
    }

    @Test
    public void testParseFailureResponse() throws Exception {
        final InputStream jsonStream = JsonDeserializationTest.class.getResourceAsStream("FailureResponse.json");
        final MermaidResponse response = gson.fromJson(new InputStreamReader(jsonStream), MermaidResponse.class);
        assertNotNull(response);

        assertEquals(InsituResponse.STATUS_CODE.NOK, response.getStatus());
        assertNotNull(response.getFailureReasons());
        assertEquals("Non-existent vars in URL for: lon_min & stop_date", response.getFailureReasons().get(0));
    }
}