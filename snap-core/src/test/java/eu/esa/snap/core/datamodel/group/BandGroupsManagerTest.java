package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static eu.esa.snap.core.datamodel.group.BandGroupsManager.CONFIG_FILE_NAME;
import static org.junit.Assert.*;

public class BandGroupsManagerTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @After
    public void tearDown() {
        BandGroupsManager.releaseInstance();
    }

    @Test
    @STTM("SNAP-3702")
    public void testInitialize() throws IOException {
        final File groupsDir = initialize();

        assertTrue(new File(groupsDir, CONFIG_FILE_NAME).isFile());
    }

    @Test
    @STTM("SNAP-3702")
    public void testInitialize_createsDir() throws IOException {
        final File groupsDir = tempDir.newFolder("bandGroups");
        final File subDir = new File(groupsDir, "sub_directory");

        BandGroupsManager.initialize(Paths.get(subDir.getAbsolutePath()));

        assertTrue(new File(subDir, CONFIG_FILE_NAME).isFile());
    }

    @Test
    @STTM("SNAP-3702")
    public void testCreateEmptyConfigFile() throws IOException {
        final File configFile = new File(tempDir.getRoot(), "test.json");
        assertFalse(configFile.isFile());

        BandGroupsManager.createEmptyConfigFile(Paths.get(configFile.getAbsolutePath()));
        assertTrue(configFile.isFile());

        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            final byte[] bytes = fileInputStream.readAllBytes();
            assertEquals("{\"bandGroups\" : []}", new String(bytes, StandardCharsets.UTF_8));
        }
    }

    @Test
    @STTM("SNAP-3702")
    public void testGetInstance_empty() throws IOException {
        initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        assertNotNull(bandGroupsManager);

        final BandGroupsManager secondManager = BandGroupsManager.getInstance();
        assertSame(bandGroupsManager, secondManager);

        final BandGroup[] groups = bandGroupsManager.get();
        assertEquals(0, groups.length);
    }

    @Test
    @STTM("SNAP-3702")
    public void testGetInstance_withGroups() throws IOException {
        final File userDir = initialize();
        writeGroupsFile(userDir);

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        assertNotNull(bandGroupsManager);

        final BandGroup[] groups = bandGroupsManager.get();
        assertEquals(1, groups.length);

        final BandGroup group = groups[0];
        assertEquals("vegetation", group.getName());
        assertArrayEquals(new String[]{"OGCI", "OGVI"}, group.get(0));
    }

    @Test
    @STTM("SNAP-3702,SNAP-3709")
    public void testAddGroupAndGet() throws IOException {
        initialize();

        final String[] expectedBandNames = {"refl_01", "refl_04", "refl_06"};
        final BandGroupImpl bandGroup = new BandGroupImpl("refl_to_use", expectedBandNames);

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(0, bandGroups.length);

        bandGroupsManager.add(bandGroup);

        bandGroups = bandGroupsManager.get();
        assertEquals(1, bandGroups.length);

        final BandGroup group = bandGroups[0];
        assertEquals("refl_to_use", group.getName());
        assertArrayEquals(expectedBandNames, group.get(0));

        assertTrue(bandGroups[0].isEditable());
    }

    @Test
    @STTM("SNAP-3702")
    public void testAddGroup_nameAlreadyExists() throws IOException {
        initialize();

        final BandGroupImpl bandGroup = new BandGroupImpl("refl_to_use", new String[]{"refl_01", "refl_04", "refl_06"});

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        bandGroupsManager.add(bandGroup);

        try {
            bandGroupsManager.add(bandGroup);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    @STTM("SNAP-3702")
    public void testAddGroupAndRemove() throws IOException {
        initialize();

        final BandGroupImpl bandGroup = new BandGroupImpl("new_data", new String[]{"dat_01", "dat_02"});

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        bandGroupsManager.add(bandGroup);

        BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(1, bandGroups.length);

        bandGroupsManager.remove("new_data");

        bandGroups = bandGroupsManager.get();
        assertEquals(0, bandGroups.length);
    }

    @Test
    @STTM("SNAP-3702")
    public void testAddGroupAndRemove_notExistentName() throws IOException {
        initialize();

        final BandGroupImpl bandGroup = new BandGroupImpl("new_data", new String[]{"dat_01", "dat_02"});

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        bandGroupsManager.add(bandGroup);

        bandGroupsManager.remove("not_existent");

        final BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(1, bandGroups.length);
    }

    @Test
    @STTM("SNAP-3702")
    public void testRemove_empty() throws IOException {
        initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        bandGroupsManager.remove("not_there");

        final BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(0, bandGroups.length);
    }

    @Test
    @STTM("SNAP-3702")
    public void testAddGroupAndGetMatchingProduct() throws IOException {
        initialize();

        final BandGroupImpl bandGroupDat = new BandGroupImpl("new_data", new String[]{"dat_01", "dat_02"});
        final BandGroupImpl bandGroupWat = new BandGroupImpl("new_wat", new String[]{"wat_01", "wat_02"});

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        bandGroupsManager.add(bandGroupDat);
        bandGroupsManager.add(bandGroupWat);

        final Product product = new Product("test", "testType", 3, 4);
        product.addBand("dat_01", ProductData.TYPE_UINT8);
        product.addBand("dat_02", ProductData.TYPE_UINT8);
        product.addBand("dat_03", ProductData.TYPE_UINT8);
        product.addBand("dat_04", ProductData.TYPE_UINT8);

        final BandGroup[] bandGroups = bandGroupsManager.getMatchingProduct(product);
        assertEquals(1, bandGroups.length);

        assertEquals("new_data", bandGroups[0].getName());
    }

    @Test
    @STTM("SNAP-3702")
    public void testAddGroupAndGetMatchingProduct_notMatching() throws IOException {
        initialize();

        final BandGroupImpl bandGroupWat = new BandGroupImpl("new_wat", new String[]{"wat_01", "wat_02"});

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
        bandGroupsManager.add(bandGroupWat);

        final Product product = new Product("test", "testType", 3, 4);
        product.addBand("dat_01", ProductData.TYPE_UINT8);
        product.addBand("dat_02", ProductData.TYPE_UINT8);
        product.addBand("dat_03", ProductData.TYPE_UINT8);
        product.addBand("dat_04", ProductData.TYPE_UINT8);

        final BandGroup[] bandGroups = bandGroupsManager.getMatchingProduct(product);
        assertEquals(0, bandGroups.length);
    }

    @Test
    @STTM("SNAP-3702")
    public void testGetMatchingProduct_noBandGroupsDefined() throws IOException {
        initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        final Product product = new Product("test", "testType", 3, 4);
        product.addBand("dat_01", ProductData.TYPE_UINT8);
        product.addBand("dat_02", ProductData.TYPE_UINT8);
        product.addBand("dat_03", ProductData.TYPE_UINT8);
        product.addBand("dat_04", ProductData.TYPE_UINT8);

        final BandGroup[] bandGroups = bandGroupsManager.getMatchingProduct(product);
        assertEquals(0, bandGroups.length);
    }

    @Test
    @STTM("SNAP-3702,SNAP-3709")
    public void testAddGroupsOfProduct() throws IOException {
        initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        final Product product = new Product("test", "testType", 3, 4);
        product.setAutoGrouping("Oa*_radiance:Oa*_radiance_unc:Oa*_radiance_err");

        bandGroupsManager.addGroupsOfProduct(product);

        final BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(1, bandGroups.length);

        String[] bandNames = bandGroups[0].get(0);
        assertEquals(1, bandNames.length);
        assertEquals("Oa*_radiance", bandNames[0]);

        assertFalse(bandGroups[0].isEditable());
    }

    @Test
    @STTM("SNAP-3702")
    public void testAddGroupsOfProductAndUserGroups() throws IOException {
        initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        final Product product = new Product("test", "testType", 3, 4);
        product.setAutoGrouping("Oa*_radiance:Oa*_radiance_unc:Oa*_radiance_err");

        bandGroupsManager.addGroupsOfProduct(product);

        final BandGroupImpl bandGroupDat = new BandGroupImpl("new_data", new String[]{"dat_01", "dat_02"});
        final BandGroupImpl bandGroupVeg = new BandGroupImpl("veggie", new String[]{"FAPAR", "LAI"});
        bandGroupsManager.add(bandGroupDat);
        bandGroupsManager.add(bandGroupVeg);

        final BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(3, bandGroups.length);

        String[] bandNames = bandGroups[1].get(0);
        assertEquals(2, bandNames.length);
        assertEquals("LAI", bandNames[1]);

        bandNames = bandGroups[2].get(0);
        assertEquals(1, bandNames.length);
        assertEquals("Oa*_radiance", bandNames[0]);
    }

    @Test
    @STTM("SNAP-3702")
    public void testAddAndRemoveGroupsOfProduct() throws IOException {
        initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        final Product product = new Product("test", "testType", 3, 4);
        product.setAutoGrouping("Oa*_radiance:Oa*_radiance_unc:Oa*_radiance_err");

        bandGroupsManager.addGroupsOfProduct(product);

        BandGroup[] bandGroups = bandGroupsManager.get();
        assertEquals(1, bandGroups.length);

        bandGroupsManager.removeGroupsOfProduct();

        bandGroups = bandGroupsManager.get();
        assertEquals(0, bandGroups.length);
    }

    @Test
    @STTM("SNAP-3709")
    public void testGetGroupsOfProduct() throws IOException {
        initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        assertNull(bandGroupsManager.getGroupsOfProduct());

        final Product product = new Product("test", "testType", 3, 4);
        product.setAutoGrouping("Oa*_radiance:Oa*_radiance_unc:Oa*_radiance_err");

        bandGroupsManager.addGroupsOfProduct(product);

        final BandGroupImpl groupsOfProduct = bandGroupsManager.getGroupsOfProduct();
        assertNotNull(groupsOfProduct);
    }

    @Test
    @STTM("SNAP-3702")
    public void testSave_empty() throws IOException {
        final File targetDir = initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        bandGroupsManager.save();

        final File configFile = new File(targetDir, CONFIG_FILE_NAME);
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            final byte[] bytes = fileInputStream.readAllBytes();
            assertEquals("{\"bandGroups\":[]}", new String(bytes, StandardCharsets.UTF_8));
        }
    }

    @Test
    @STTM("SNAP-3702")
    public void testSave_twoBandGroups() throws IOException {
        final File targetDir = initialize();

        final BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();

        final BandGroupImpl bandGroupVeg = new BandGroupImpl("veggie", new String[]{"FAPAR", "LAI"});
        bandGroupsManager.add(bandGroupVeg);

        BandGroupImpl parsedGroup = (BandGroupImpl) BandGroup.parse("Oa*_radiance:Oa*_radiance_unc:Oa*_radiance_err");
        bandGroupsManager.add(parsedGroup);

        bandGroupsManager.save();

        final File configFile = new File(targetDir, CONFIG_FILE_NAME);
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            final byte[] bytes = fileInputStream.readAllBytes();
            assertEquals("{\"bandGroups\":[{\"paths\":[[\"FAPAR\",\"LAI\"]],\"name\":\"veggie\"},{\"paths\":[[\"Oa*_radiance\"],[\"Oa*_radiance_unc\"],[\"Oa*_radiance_err\"]],\"name\":\"\"}]}", new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private File initialize() throws IOException {
        final File groupsDir = tempDir.newFolder("bandGroups");

        BandGroupsManager.initialize(Paths.get(groupsDir.getAbsolutePath()));
        return groupsDir;
    }

    private static void writeGroupsFile(File targetDir) throws IOException {
        final File configFile = new File(targetDir, CONFIG_FILE_NAME);
        final FileWriter fileWriter = new FileWriter(configFile, StandardCharsets.UTF_8);

        final String outData = "{\"bandGroups\" : [" +
                "  {" +
                "    \"name\" : \"vegetation\"" +
                "    \"paths\" : [" +
                "      [\"OGCI\", \"OGVI\"]" +
                "    ]" +
                "  }" +
                "]" +
                "}";

        fileWriter.write(outData);
        fileWriter.flush();
        fileWriter.close();
    }
}
