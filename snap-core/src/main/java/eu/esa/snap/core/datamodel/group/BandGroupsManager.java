package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.datamodel.Product;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

public class BandGroupsManager {

    static String CONFIG_FILE_NAME = "user_band_groups.json";

    private static BandGroupsManager instance;
    private static Path groupsConfigFile;

    private final ArrayList<BandGroup> userGroups;
    private BandGroup productGroup;

    public static BandGroupsManager getInstance() throws IOException {
        if (instance == null) {
            instance = new BandGroupsManager();
            instance.load(groupsConfigFile);
        }
        return instance;
    }

    public static void releaseInstance() {
        instance = null;
    }

    public static void initialize(Path groupsDir) throws IOException {
        groupsConfigFile = groupsDir.resolve(CONFIG_FILE_NAME);
        if (!Files.exists(groupsConfigFile)) {
            groupsConfigFile = createEmptyConfigFile(groupsConfigFile);
        }
    }

    static Path createEmptyConfigFile(Path configFile) throws IOException {
        Files.createFile(configFile);
        FileWriter fileWriter = new FileWriter(configFile.toFile(), StandardCharsets.UTF_8);

        fileWriter.write("{\"bandGroups\" : []}");
        fileWriter.flush();
        fileWriter.close();

        return configFile;
    }

    protected BandGroupsManager() {
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
        final BandGroup[] bandGroups = get();
        try (FileOutputStream fileOutputStream = new FileOutputStream(groupsConfigFile.toFile())) {
            BandGroupIO.write(bandGroups, fileOutputStream);
        }
    }

    BandGroup[] get() {
        final BandGroup[] userGroupsArray = userGroups.toArray(new BandGroup[0]);
        if (productGroup == null) {
            return userGroupsArray;
        }

        final BandGroup[] allGroups = new BandGroup[userGroupsArray.length + 1];
        System.arraycopy(userGroupsArray, 0, allGroups, 0, userGroupsArray.length);
        allGroups[userGroupsArray.length] = productGroup;

        return allGroups;
    }

    BandGroup[] getMatchingProduct(Product product) {
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
        productGroup = product.getAutoGrouping();
    }

    public void removeGroupsOfProduct() {
        productGroup = null;
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
