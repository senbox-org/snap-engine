package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BandGroupIOTest {

    @Test
    @STTM("SNAP-3702")
    public void testRead_noEntries() throws IOException, ParseException {
        final ByteArrayInputStream emptyStream = new ByteArrayInputStream("{\"bandGroups\" : []}".getBytes());

        BandGrouping[] bandGroupings = BandGroupIO.read(emptyStream);
        assertEquals(0, bandGroupings.length);
    }

    @Test
    @STTM("SNAP-3702")
    public void testRead_one() throws IOException, ParseException {
        final String streamData = "{\"bandGroups\" : [" +
                "  {" +
                "    \"name\" : \"Jolanda\"" +
                "    \"paths\" : [" +
                "      [\"help\", \"me\"]" +
                "    ]" +
                "  }" +
                "]" +
                "}";
        final ByteArrayInputStream emptyStream = new ByteArrayInputStream(streamData.getBytes());

        final BandGrouping[] bandGroupings = BandGroupIO.read(emptyStream);
        assertEquals(1, bandGroupings.length);

        final BandGrouping bandGrouping = bandGroupings[0];
        assertEquals("Jolanda", bandGrouping.getName());
        final String[] strings = bandGrouping.get(0);
        assertArrayEquals(new String[]{"help", "me"}, strings);
    }
}
