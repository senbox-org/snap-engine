package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class BandGroupsManager {

    static final String CONFIG_FILE_NAME = "user_band_groups.json";

    private static BandGroupsManager instance;
    private static Path groupsConfigFile;

    private final ArrayList<BandGroup> userGroups;
    private BandGroupImpl productGroup;

    public static synchronized BandGroupsManager getInstance() throws IOException {
        if (instance == null) {
            if (groupsConfigFile == null) {
                // @todo 2 tb/tb move this to engine initialisation 2024-07-11
                final File appDataDir = SystemUtils.getApplicationDataDir();
                final Path appDataPath = Paths.get(appDataDir.getAbsolutePath());
                final Path configDir = appDataPath.resolve("config");
                initialize(configDir);
            }

            instance = new BandGroupsManager();
            instance.load(groupsConfigFile);
        }
        return instance;
    }

    public static void releaseInstance() {
        instance = null;
        groupsConfigFile = null;
    }

    public static void initialize(Path groupsDir) throws IOException {
        if (!Files.isDirectory(groupsDir)) {
            Files.createDirectories(groupsDir);
        }

        groupsConfigFile = groupsDir.resolve(CONFIG_FILE_NAME);
        if (!Files.exists(groupsConfigFile)) {
            groupsConfigFile = createEmptyConfigFile(groupsConfigFile);
        }
    }

    static Path createEmptyConfigFile(Path configFile) throws IOException {
        Files.createFile(configFile);
        try (FileWriter fileWriter = new FileWriter(configFile.toFile(), StandardCharsets.UTF_8)) {
            fileWriter.write("{\"bandGroups\" : []}");
            fileWriter.flush();
        }

        return configFile;
    }

    public BandGroupsManager() {
        userGroups = new ArrayList<>();
        productGroup = null;
    }

    void load(Path groupsConfigFile) throws IOException {
        try {
            final BandGroup[] groups = BandGroupIO.read(new FileInputStream(groupsConfigFile.toFile()));
            Collections.addAll(userGroups, groups);
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    public void save() throws IOException {
        final BandGroup[] bandGroups = userGroups.toArray(new BandGroup[0]);
        try (FileOutputStream fileOutputStream = new FileOutputStream(groupsConfigFile.toFile())) {
            BandGroupIO.write(bandGroups, fileOutputStream);
        }
    }

    public BandGroup[] get() {
        final BandGroup[] userGroupsArray = userGroups.toArray(new BandGroup[0]);
        if (productGroup == null) {
            return userGroupsArray;
        }

        final BandGroup[] allGroups = new BandGroup[userGroupsArray.length + 1];
        System.arraycopy(userGroupsArray, 0, allGroups, 0, userGroupsArray.length);
        allGroups[userGroupsArray.length] = productGroup;

        return allGroups;
    }

    public BandGroup[] getGroupsMatchingProduct(Product product) {
        final ArrayList<BandGroup> resultList = new ArrayList<>();
        for (final BandGroup group : userGroups) {
            String[] names = group.getMatchingBandNames(product);
            if (names.length > 0) {
                resultList.add(group);
            }
        }
        return resultList.toArray(new BandGroup[0]);
    }

    public void add(BandGroupImpl bandGroup) {
        for (final BandGroup existingGroup : userGroups) {
            if (existingGroup.getName().equals(bandGroup.getName())) {
                throw new IllegalArgumentException("A band group with the name already exists: " + bandGroup.getName());
            }
        }
        userGroups.add(bandGroup);
    }

    public void addGroupsOfProduct(Product product) {
        if (product == null) {
            return;
        }
        productGroup = (BandGroupImpl) product.getAutoGrouping();

        if (productGroup == null) {
            return;
        }
        productGroup.setEditable(false);
    }

    public void removeGroupsOfProduct() {
        productGroup = null;
    }

    public BandGroupImpl getGroupsOfProduct() {
        return productGroup;
    }

    public void remove(String bandGroupName) {
        int index = 0;
        boolean found = false;
        for (final BandGroup existingGroup : userGroups) {
            if (existingGroup.getName().equals(bandGroupName)) {
                found = true;
                break;
            }
            ++index;
        }
        if (found) {
            userGroups.remove(index);
        }
    }
}
