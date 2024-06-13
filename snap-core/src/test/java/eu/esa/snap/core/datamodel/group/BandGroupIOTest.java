package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BandGroupIOTest {

    @Test
    @STTM("SNAP-3702")
    public void testRead_noEntries() throws IOException, ParseException {
        final ByteArrayInputStream emptyStream = new ByteArrayInputStream("{\"bandGroups\" : []}".getBytes(StandardCharsets.UTF_8));

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
        final ByteArrayInputStream jsonStream = new ByteArrayInputStream(streamData.getBytes(StandardCharsets.UTF_8));

        final BandGrouping[] bandGroupings = BandGroupIO.read(jsonStream);
        assertEquals(1, bandGroupings.length);

        final BandGrouping bandGrouping = bandGroupings[0];
        assertEquals("Jolanda", bandGrouping.getName());
        final String[] strings = bandGrouping.get(0);
        assertArrayEquals(new String[]{"help", "me"}, strings);
    }

    @Test
    @STTM("SNAP-3702")
    public void testRead_one_noName() throws IOException, ParseException {
        final String streamData = "{\"bandGroups\" : [" +
                "  {" +
                "    \"paths\" : [" +
                "      [\"nameless\", \"clueless\"]" +
                "    ]" +
                "  }" +
                "]" +
                "}";
        final ByteArrayInputStream jsonStream = new ByteArrayInputStream(streamData.getBytes(StandardCharsets.UTF_8));

        final BandGrouping[] bandGroupings = BandGroupIO.read(jsonStream);
        assertEquals(1, bandGroupings.length);

        final BandGrouping bandGrouping = bandGroupings[0];
        assertEquals("", bandGrouping.getName());
        final String[] strings = bandGrouping.get(0);
        assertArrayEquals(new String[]{"nameless", "clueless"}, strings);
    }

    @Test
    @STTM("SNAP-3702")
    public void testRead_three() throws IOException, ParseException {
        final String streamData = "{\"bandGroups\" : [" +
                "  {" +
                "    \"name\" : \"Tick\"" +
                "    \"paths\" : [" +
                "      [\"the\", \"duck\"]" +
                "    ]" +
                "  }," +
                "  {" +
                "    \"name\" : \"Trick\"" +
                "    \"paths\" : [" +
                "      [\"another\", \"duck\"]," +
                "      [\"Faehnlein\", \"Fieselschweif\"]" +
                "    ]" +
                "  }," +
                "  {" +
                "    \"name\" : \"Track\"" +
                "    \"paths\" : [" +
                "      [\"young\", \"duck\"]," +
                "      [\"living\", \"Entenhausen\"]" +
                "    ]" +
                "  } " +
                "]" +
                "}";
        final ByteArrayInputStream jsonStream = new ByteArrayInputStream(streamData.getBytes(StandardCharsets.UTF_8));

        final BandGrouping[] bandGroupings = BandGroupIO.read(jsonStream);
        assertEquals(3, bandGroupings.length);

        BandGrouping bandGrouping = bandGroupings[0];
        assertEquals("Tick", bandGrouping.getName());
        String[] strings = bandGrouping.get(0);
        assertArrayEquals(new String[]{"the", "duck"}, strings);

        bandGrouping = bandGroupings[1];
        assertEquals("Trick", bandGrouping.getName());
        strings = bandGrouping.get(1);
        assertArrayEquals(new String[]{"Faehnlein", "Fieselschweif"}, strings);

        bandGrouping = bandGroupings[2];
        assertEquals("Track", bandGrouping.getName());
        strings = bandGrouping.get(0);
        assertArrayEquals(new String[]{"young", "duck"}, strings);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_noBandGroups() throws IOException, ParseException {
        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGrouping[0], jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[]}", jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_one() throws IOException, ParseException {
        final BandGroupingImpl bandGrouping = new BandGroupingImpl(new String[][]{{"L1B"},
                {"dim", "rad"}});
        bandGrouping.setName("Argonaut");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupingImpl[]{bandGrouping}, jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[{\"paths\":[[\"L1B\"],[\"dim\",\"rad\"]],\"name\":\"Argonaut\"}]}", jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_oneNothingGrouped() throws IOException, ParseException {
        final BandGroupingImpl bandGrouping = new BandGroupingImpl(new String[0][]);
        bandGrouping.setName("Empty");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupingImpl[]{bandGrouping}, jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[{\"paths\":[],\"name\":\"Empty\"}]}", jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_three() throws IOException, ParseException {
        final BandGroupingImpl grouping_1 = new BandGroupingImpl(new String[][]{{"L1B"},
                {"dim", "rad"}});
        grouping_1.setName("Anna");

        final BandGroupingImpl grouping_2 = new BandGroupingImpl(new String[][]{{"_err", "_unc"},
                {"geo_", "in-situ_"}});
        grouping_2.setName("Bert");

        final BandGroupingImpl grouping_3 = new BandGroupingImpl(new String[][]{{"sensor_sync"}});
        grouping_3.setName("Christa");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupingImpl[]{grouping_1, grouping_2, grouping_3}, jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[{\"paths\":[[\"L1B\"],[\"dim\",\"rad\"]],\"name\":\"Anna\"},{\"paths\":[[\"_err\",\"_unc\"],[\"geo_\",\"in-situ_\"]],\"name\":\"Bert\"},{\"paths\":[[\"sensor_sync\"]],\"name\":\"Christa\"}]}",
                jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWriteAndRead() throws IOException, ParseException {
        final BandGroupingImpl grouping_1 = new BandGroupingImpl(new String[][]{{"L3*"},
                {"vza", "zenith"}});
        grouping_1.setName("Anna");

        final BandGroupingImpl grouping_2 = new BandGroupingImpl(new String[][]{{"_err", "_unc"},
                {"L2B", "L2C"}});
        grouping_2.setName("Bert");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupingImpl[]{grouping_1, grouping_2}, jsonStream);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonStream.toByteArray());
        final BandGrouping[] read = BandGroupIO.read(inputStream);

        assertEquals(2, read.length);
        assertEquals("Anna", read[0].getName());
        assertArrayEquals(new String[]{"L3*"}, read[0].get(0));

        assertEquals("Bert", read[1].getName());
        assertArrayEquals(new String[]{"L2B", "L2C"}, read[1].get(1));
    }

    @Test
    @STTM("SNAP-3702")
    public void testReadAndWrite() throws IOException, ParseException {
        final String streamData = "{\"bandGroups\" : [" +
                "  {" +
                "    \"name\" : \"Tick\"" +
                "    \"paths\" : [" +
                "      [\"the\", \"duck\"]" +
                "    ]" +
                "  }," +
                "  {" +
                "    \"name\" : \"Trick\"" +
                "    \"paths\" : [" +
                "      [\"another\", \"duck\"]," +
                "      [\"Faehnlein\", \"Fieselschweif\"]" +
                "    ]" +
                "  }," +
                "  {" +
                "    \"name\" : \"Track\"" +
                "    \"paths\" : [" +
                "      [\"young\", \"duck\"]," +
                "      [\"living\", \"Entenhausen\"]" +
                "    ]" +
                "  } " +
                "]" +
                "}";
        final ByteArrayInputStream jsonStream = new ByteArrayInputStream(streamData.getBytes(StandardCharsets.UTF_8));

        final BandGrouping[] bandGroupings = BandGroupIO.read(jsonStream);

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        BandGroupIO.write(bandGroupings, outStream);

        assertEquals("{\"bandGroups\":[{\"paths\":[[\"the\",\"duck\"]],\"name\":\"Tick\"},{\"paths\":[[\"another\",\"duck\"],[\"Faehnlein\",\"Fieselschweif\"]],\"name\":\"Trick\"},{\"paths\":[[\"young\",\"duck\"],[\"living\",\"Entenhausen\"]],\"name\":\"Track\"}]}",
                outStream.toString());
    }
}
