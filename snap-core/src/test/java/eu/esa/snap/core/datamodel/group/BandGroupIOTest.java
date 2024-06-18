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

        BandGroup[] bandGroups = BandGroupIO.read(emptyStream);
        assertEquals(0, bandGroups.length);
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

        final BandGroup[] bandGroups = BandGroupIO.read(jsonStream);
        assertEquals(1, bandGroups.length);

        final BandGroup bandGroup = bandGroups[0];
        assertEquals("Jolanda", bandGroup.getName());
        final String[] strings = bandGroup.get(0);
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

        final BandGroup[] bandGroups = BandGroupIO.read(jsonStream);
        assertEquals(1, bandGroups.length);

        final BandGroup bandGroup = bandGroups[0];
        assertEquals("", bandGroup.getName());
        final String[] strings = bandGroup.get(0);
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

        final BandGroup[] bandGroups = BandGroupIO.read(jsonStream);
        assertEquals(3, bandGroups.length);

        BandGroup bandGroup = bandGroups[0];
        assertEquals("Tick", bandGroup.getName());
        String[] strings = bandGroup.get(0);
        assertArrayEquals(new String[]{"the", "duck"}, strings);

        bandGroup = bandGroups[1];
        assertEquals("Trick", bandGroup.getName());
        strings = bandGroup.get(1);
        assertArrayEquals(new String[]{"Faehnlein", "Fieselschweif"}, strings);

        bandGroup = bandGroups[2];
        assertEquals("Track", bandGroup.getName());
        strings = bandGroup.get(0);
        assertArrayEquals(new String[]{"young", "duck"}, strings);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_noBandGroups() throws IOException, ParseException {
        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroup[0], jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[]}", jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_one() throws IOException, ParseException {
        final BandGroupImpl bandGrouping = new BandGroupImpl(new String[][]{{"L1B"},
                {"dim", "rad"}});
        bandGrouping.setName("Argonaut");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupImpl[]{bandGrouping}, jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[{\"paths\":[[\"L1B\"],[\"dim\",\"rad\"]],\"name\":\"Argonaut\"}]}", jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_oneNothingGrouped() throws IOException, ParseException {
        final BandGroupImpl bandGrouping = new BandGroupImpl(new String[0][]);
        bandGrouping.setName("Empty");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupImpl[]{bandGrouping}, jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[{\"paths\":[],\"name\":\"Empty\"}]}", jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWrite_three() throws IOException, ParseException {
        final BandGroupImpl grouping_1 = new BandGroupImpl(new String[][]{{"L1B"},
                {"dim", "rad"}});
        grouping_1.setName("Anna");

        final BandGroupImpl grouping_2 = new BandGroupImpl(new String[][]{{"_err", "_unc"},
                {"geo_", "in-situ_"}});
        grouping_2.setName("Bert");

        final BandGroupImpl grouping_3 = new BandGroupImpl(new String[][]{{"sensor_sync"}});
        grouping_3.setName("Christa");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupImpl[]{grouping_1, grouping_2, grouping_3}, jsonStream);

        final String jsonContent = jsonStream.toString();
        assertEquals("{\"bandGroups\":[{\"paths\":[[\"L1B\"],[\"dim\",\"rad\"]],\"name\":\"Anna\"},{\"paths\":[[\"_err\",\"_unc\"],[\"geo_\",\"in-situ_\"]],\"name\":\"Bert\"},{\"paths\":[[\"sensor_sync\"]],\"name\":\"Christa\"}]}",
                jsonContent);
    }

    @Test
    @STTM("SNAP-3702")
    public void testWriteAndRead() throws IOException, ParseException {
        final BandGroupImpl grouping_1 = new BandGroupImpl(new String[][]{{"L3*"},
                {"vza", "zenith"}});
        grouping_1.setName("Anna");

        final BandGroupImpl grouping_2 = new BandGroupImpl(new String[][]{{"_err", "_unc"},
                {"L2B", "L2C"}});
        grouping_2.setName("Bert");

        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream();

        BandGroupIO.write(new BandGroupImpl[]{grouping_1, grouping_2}, jsonStream);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonStream.toByteArray());
        final BandGroup[] read = BandGroupIO.read(inputStream);

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

        final BandGroup[] bandGroups = BandGroupIO.read(jsonStream);

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        BandGroupIO.write(bandGroups, outStream);

        assertEquals("{\"bandGroups\":[{\"paths\":[[\"the\",\"duck\"]],\"name\":\"Tick\"},{\"paths\":[[\"another\",\"duck\"],[\"Faehnlein\",\"Fieselschweif\"]],\"name\":\"Trick\"},{\"paths\":[[\"young\",\"duck\"],[\"living\",\"Entenhausen\"]],\"name\":\"Track\"}]}",
                outStream.toString());
    }
}
