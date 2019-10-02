package org.esa.snap.product.library.v2.database;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.v2.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.AttributeFilter;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.SensorType;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class ProductLibraryDAL {

    private ProductLibraryDAL() {
    }

    public static List<RemoteMission> loadMissions(Statement statement) throws SQLException, IOException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT rm.id, rm.name FROM ")
                .append(DatabaseTableNames.REMOTE_MISSIONS)
                .append(" AS rm ");
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            List<RemoteMission> missions = new ArrayList<>();
            while (resultSet.next()) {
                short id = resultSet.getShort("id");
                String name = resultSet.getString("name");
                missions.add(new RemoteMission(id, name));
            }
            return missions;
        }
    }

    public static List<LocalRepositoryFolder> loadLocalRepositoryFolders(Statement statement) throws SQLException, IOException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, folder_path FROM ")
                .append(DatabaseTableNames.LOCAL_REPOSITORIES);
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            List<LocalRepositoryFolder> results = new ArrayList<>();
            while (resultSet.next()) {
                short localRepositoryId = resultSet.getShort("id");
                String folderPath = resultSet.getString("folder_path");
                Path localRepositoryFolderPath = Paths.get(folderPath);
                LocalRepositoryFolder localRepositoryFolder = new LocalRepositoryFolder(localRepositoryId, localRepositoryFolderPath);
                results.add(localRepositoryFolder);
            }
            return results;
        }
    }

    public static Map<Short, Set<String>> loadAttributesNamesPerMission(Statement statement) throws SQLException, IOException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ra.remote_mission_id, ra.name FROM ")
                .append(DatabaseTableNames.REMOTE_ATTRIBUTES)
                .append(" AS ra ");
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            Map<Short, Set<String>> attributeNamesPerMission = new HashMap<>();
            while (resultSet.next()) {
                short missionId = resultSet.getShort("remote_mission_id");
                String attributeName = resultSet.getString("name");
                Set<String> missionAttributes = attributeNamesPerMission.get(missionId);
                if (missionAttributes == null) {
                    missionAttributes = new HashSet<>();
                    attributeNamesPerMission.put(missionId, missionAttributes);
                }
                missionAttributes.add(attributeName);
            }
            return attributeNamesPerMission;
        }
    }

    public static List<RepositoryProduct> loadProductList(LocalRepositoryFolder localRepositoryFolder, RemoteMission mission, Map<String, Object> parameterValues)
                                                          throws SQLException, IOException {

        List<RepositoryProduct> productList;
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            Connection wrappedConnection = SFSUtilities.wrapConnection(connection);

            Date startDate = null;
            Date endDate = null;
            List<AttributeFilter> attributes = null;
            Rectangle2D selectionArea = null;
            SensorType sensorType = null;
            for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                String parameterName = entry.getKey();
                Object parameterValue = entry.getValue();
                if (parameterName.equalsIgnoreCase(AllLocalFolderProductsRepository.FOOT_PRINT_PARAMETER)) {
                    selectionArea = (Rectangle2D)parameterValue;
                } else if (parameterName.equalsIgnoreCase(AllLocalFolderProductsRepository.START_DATE_PARAMETER)) {
                    startDate = (Date)parameterValue;
                    if (startDate == null) {
                        throw new NullPointerException("The start date is null.");
                    }
                } else if (parameterName.equalsIgnoreCase(AllLocalFolderProductsRepository.END_DATE_PARAMETER)) {
                    endDate = (Date)parameterValue;
                    if (endDate == null) {
                        throw new NullPointerException("The end date is null.");
                    }
                } else if (parameterName.equalsIgnoreCase(AllLocalFolderProductsRepository.SENSOR_TYPE_PARAMETER)) {
                    sensorType = (SensorType)parameterValue;
                } else if (parameterName.equalsIgnoreCase(AllLocalFolderProductsRepository.ATTRIBUTES_PARAMETER)) {
                    attributes = (List<AttributeFilter>)parameterValue;
                } else {
                    throw new IllegalStateException("Unknown parameter '" + parameterName + "'.");
                }
            }
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.id, p.name, p.local_repository_relative_path, p.entry_point, p.size_in_bytes, p.geometry, p.acquisition_date, p.last_modified_date");
            if (localRepositoryFolder == null) {
                // no local repository filter
                sql.append(", lr.folder_path");
            }
            if (mission == null) {
                // no mission filter
                sql.append(", rm.name AS mission_name");
            }
            sql.append(" FROM ")
                    .append(DatabaseTableNames.PRODUCTS)
                    .append(" AS p");
            if (localRepositoryFolder == null) {
                // no local repository filter
                sql.append(", ")
                        .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                        .append(" AS lr");
            }
            if (mission == null) {
                // no mission filter
                sql.append(", ")
                        .append(DatabaseTableNames.REMOTE_MISSIONS)
                        .append(" AS rm ")
                        .append("WHERE p.remote_mission_id = rm.id");
            } else {
                sql.append(" WHERE p.remote_mission_id = ")
                        .append(mission.getId());
            }
            sql.append(" AND p.local_repository_id = ");
            if (localRepositoryFolder == null) {
                // no local repository filter
                sql.append(" lr.id");
            } else {
                sql.append(localRepositoryFolder.getId());
            }

            if (selectionArea != null) {
                Polygon2D polygon = new Polygon2D();
                polygon.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
                polygon.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY()); // the top right corner
                polygon.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY() + selectionArea.getHeight()); // the bottom right corner
                polygon.append(selectionArea.getX(), selectionArea.getY() + selectionArea.getHeight()); // the bottom left corner
                polygon.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
                sql.append(" AND ST_Intersects(p.geometry, '")
                        .append(polygon.toWKT())
                        .append("')");
            }
            if (startDate != null) {
                sql.append(" AND p.acquisition_date >= ?");
            }
            if (endDate != null) {
                sql.append(" AND p.acquisition_date <= ?");
            }
            if (sensorType != null) {
                sql.append(" AND p.sensor_type_id = ")
                    .append(sensorType.getValue());
            }
            Calendar calendar = Calendar.getInstance();
            try (PreparedStatement prepareStatement = wrappedConnection.prepareStatement(sql.toString())) {
                if (startDate != null) {
                    calendar.setTimeInMillis(startDate.getTime());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    java.sql.Timestamp startTimestamp = new java.sql.Timestamp(calendar.getTimeInMillis());
                    prepareStatement.setTimestamp(1, startTimestamp);
                }
                if (endDate != null) {
                    calendar.setTimeInMillis(endDate.getTime());
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 0);
                    java.sql.Timestamp endTimestamp = new java.sql.Timestamp(calendar.getTimeInMillis());
                    prepareStatement.setTimestamp(2, endTimestamp);
                }
                try (SpatialResultSet resultSet = prepareStatement.executeQuery().unwrap(SpatialResultSet.class)) {
                    productList = new ArrayList<>();
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String name = resultSet.getString("name");
                        String type = null;
                        String localPath = resultSet.getString("local_repository_relative_path");
                        Path productLocalPath;
                        if (localRepositoryFolder == null) {
                            String localRepositoryFolderPath = resultSet.getString("folder_path");
                            productLocalPath = Paths.get(localRepositoryFolderPath, localPath);
                        } else {
                            productLocalPath = localRepositoryFolder.getPath().resolve(localPath);
                        }
                        String missionName = (mission == null) ? resultSet.getString("mission_name") : mission.getName();
                        Timestamp acquisitionDate = resultSet.getTimestamp("acquisition_date");
                        long sizeInBytes = resultSet.getLong("size_in_bytes");
                        Geometry geometry = resultSet.getGeometry("geometry");
                        Polygon2D polygon = buildPolygon(geometry);
                        LocalRepositoryProduct localProduct = new LocalRepositoryProduct(id, name, type, acquisitionDate, productLocalPath, sizeInBytes, polygon);
                        localProduct.setMission(missionName);
                        productList.add(localProduct);
                    }
                }

                if (productList.size() > 0) {
                    try (Statement statement = connection.createStatement()) {
                        for (int i=productList.size()-1; i>=0; i--) {
                            LocalRepositoryProduct localProduct = (LocalRepositoryProduct)productList.get(i);
                            List<Attribute> remoteAttributes = loadProductRemoteAttributes(localProduct.getId(), statement);
                            boolean foundAllAttributes = true;
                            if (attributes != null && attributes.size() > 0) {
                                foundAllAttributes = checkProductAttributesMatches(remoteAttributes, attributes);
                            }
                            if (foundAllAttributes) {
                                localProduct.setAttributes(remoteAttributes);
                            } else {
                                productList.remove(i);
                            }
                        }
                    }
                }
            }
        }

        if (productList.size() > 0) {
            Path databaseParentFolder = H2DatabaseAccessor.getDatabaseParentFolder();
            Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");

            for (int i=0; i<productList.size(); i++) {
                LocalRepositoryProduct localProduct = (LocalRepositoryProduct)productList.get(i);
                Path quickLookImageFile = quickLookImagesFolder.resolve(Integer.toString(localProduct.getId()) + ".png");
                if (Files.exists(quickLookImageFile)) {
                    BufferedImage quickLookImage = ImageIO.read(quickLookImageFile.toFile());
                    localProduct.setQuickLookImage(quickLookImage);
                }
            }
        }

        return productList;
    }

    private static boolean checkProductAttributesMatches(List<Attribute> remoteAttributes, List<AttributeFilter> attributes) {
        boolean foundAllAttributes = true;
        for (int k = 0; k < attributes.size() && foundAllAttributes; k++) {
            AttributeFilter filterAttribute = attributes.get(k);
            boolean found = false;
            for (int j = 0; j < remoteAttributes.size() && !found; j++) {
                Attribute productAttribute = remoteAttributes.get(j);
                if (filterAttribute.matches(productAttribute)) {
                    found = true;
                }
            }
            if (!found) {
                foundAllAttributes = false;
            }
        }
        return foundAllAttributes;
    }

    private static List<Attribute> loadProductRemoteAttributes(int productId, Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT name, value FROM ")
                .append(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES)
                .append(" WHERE product_id = ")
                .append(productId);
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            List<Attribute> remoteAttributes = new ArrayList<>();
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String value = resultSet.getString("value");
                remoteAttributes.add(new Attribute(name, value));
            }
            return remoteAttributes;
        }
    }

    private static Polygon2D buildPolygon(Geometry geometry) {
        if (!(geometry instanceof Polygon)) {
            throw new IllegalStateException("The product geometry type '"+geometry.getClass().getName()+"' is not a '"+Polygon.class.getName()+"' type.");
        }
        Coordinate[] coordinates = ((Polygon)geometry).getExteriorRing().getCoordinates();
        Coordinate firstCoordinate = coordinates[0];
        Coordinate lastCoordinate = coordinates[coordinates.length-1];
        if (firstCoordinate.x != lastCoordinate.x || firstCoordinate.y != lastCoordinate.y) {
            throw new IllegalStateException("The first and last coordinates of the polygon do not match.");
        }
        Polygon2D polygon = new Polygon2D();
        for (Coordinate coordinate : coordinates) {
            polygon.append(coordinate.x, coordinate.y);
        }
        return polygon;
    }

    public static void deleteProduct(LocalRepositoryProduct repositoryProduct) throws IOException, SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                StringBuilder sql = new StringBuilder();
                sql.append("DELETE FROM ")
                        .append(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES)
                        .append(" WHERE product_id = ")
                        .append(repositoryProduct.getId());
                statement.executeUpdate(sql.toString());

                sql = new StringBuilder();
                sql.append("DELETE FROM ")
                        .append(DatabaseTableNames.PRODUCTS)
                        .append(" WHERE id = ")
                        .append(repositoryProduct.getId());
                statement.executeUpdate(sql.toString());
                // commit the data
                connection.commit();
            } catch (Exception exception) {
                // rollback the statements from the transaction
                connection.rollback();
                throw exception;
            }
        }
    }

    public static SaveProductData saveProduct(RepositoryProduct productToSave, Path productFolderPath, String remoteRepositoryName, Path localRepositoryFolderPath)
                                   throws IOException, SQLException {

        Path parent = productFolderPath;
        while (parent != null && parent.compareTo(localRepositoryFolderPath) != 0) {
            parent = productFolderPath.getParent();
        }
        Path relativePath;
        if (parent == null) {
            throw new IllegalArgumentException("The product folder path '"+ productFolderPath.toString()+"' must be specified into the local repository folder path '"+localRepositoryFolderPath.toString()+"'.");
        } else {
            int localRepositoryNameCount = localRepositoryFolderPath.getNameCount();
            relativePath = productFolderPath.subpath(localRepositoryNameCount, productFolderPath.getNameCount());
        }
        int productId;
        short remoteMissionId;
        short localRepositoryId;
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            connection.setAutoCommit(false);
            try {
                short remoteRepositoryId = saveRemoteRepositoryName(remoteRepositoryName, connection);
                remoteMissionId = saveRemoteMissionName(remoteRepositoryId, productToSave.getMission(), productToSave.getAttributes(), connection);
                localRepositoryId = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);
                FileTime fileTime = Files.getLastModifiedTime(productFolderPath);
                long sizeInBytes = FileIOUtils.computeFileSize(productFolderPath);

                productId = insertProduct(productToSave, relativePath, remoteMissionId, localRepositoryId, fileTime, sizeInBytes, connection);
                insertRemoteProductAttributes(productId, productToSave.getAttributes(), connection);
                // commit the statements
                connection.commit();
            } catch (Exception exception) {
                // rollback the statements from the transaction
                connection.rollback();
                throw exception;
            }
        }

        BufferedImage quickLookImage = productToSave.getQuickLookImage();
        if (quickLookImage != null) {
            Path databaseParentFolder = H2DatabaseAccessor.getDatabaseParentFolder();
            Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");
            FileIOUtils.ensureExists(quickLookImagesFolder);
            Path quickLookImageFile = quickLookImagesFolder.resolve(Integer.toString(productId) + ".png");
            ImageIO.write(quickLookImage, "png", quickLookImageFile.toFile());
        }
        RemoteMission remoteMission = new RemoteMission(remoteMissionId, productToSave.getMission());
        LocalRepositoryFolder localRepositoryFolder = new LocalRepositoryFolder(localRepositoryId, localRepositoryFolderPath);
        return new SaveProductData(remoteMission, localRepositoryFolder);
    }

    private static short saveRemoteRepositoryName(String remoteRepositoryName, Connection connection) throws SQLException {
        short remoteRepositoryId = 0;
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.REMOTE_REPOSITORIES)
                    .append(" WHERE LOWER(name) = '")
                    .append(remoteRepositoryName.toLowerCase())
                    .append("'");
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                if (resultSet.next()) {
                    remoteRepositoryId = resultSet.getShort("id");
                }
            }
        }
        if (remoteRepositoryId == 0) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.REMOTE_REPOSITORIES)
                    .append(" (name) VALUES ('")
                    .append(remoteRepositoryName)
                    .append("')");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the remote repository, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            remoteRepositoryId = generatedKeys.getShort(1);
                        } else {
                            throw new SQLException("Failed to get the generated remote repository id.");
                        }
                    }
                }
            }
        }
        return remoteRepositoryId;
    }

    private static short saveRemoteMissionName(int remoteRepositoryId, String remoteMissionName, List<Attribute> remoteAttributes, Connection connection) throws SQLException {
        short remoteMissionId = 0;
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.REMOTE_MISSIONS)
                    .append(" WHERE remote_repository_id = ")
                    .append(remoteRepositoryId)
                    .append(" AND LOWER(name) = '")
                    .append(remoteMissionName.toLowerCase())
                    .append("'");
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                if (resultSet.next()) {
                    remoteMissionId = resultSet.getShort("id");
                }
            }
        }
        if (remoteMissionId == 0) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.REMOTE_MISSIONS)
                    .append(" (remote_repository_id, name) VALUES (")
                    .append(remoteRepositoryId)
                    .append(", '")
                    .append(remoteMissionName)
                    .append("')");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the remote mission, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            remoteMissionId = generatedKeys.getShort(1);
                        } else {
                            throw new SQLException("Failed to get the generated remote mission id.");
                        }
                    }
                }
            }
            sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.REMOTE_ATTRIBUTES)
                    .append(" (name, remote_mission_id) VALUES (?, ")
                    .append(remoteMissionId)
                    .append(")");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                for (int i=0; i<remoteAttributes.size(); i++) {
                    Attribute attribute = remoteAttributes.get(i);
                    statement.setString(1, attribute.getName());
                    statement.executeUpdate();
                }
            }
        }
        return remoteMissionId;
    }

    private static short saveLocalRepositoryFolderPath(Path localRepositoryFolderPath, Connection connection) throws SQLException {
        String localRepositoryPath = localRepositoryFolderPath.toString();
        short localRepositoryId = 0;
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                    .append(" WHERE LOWER(folder_path) = '")
                    .append(localRepositoryPath.toLowerCase())
                    .append("'");
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                if (resultSet.next()) {
                    localRepositoryId = resultSet.getShort("id");
                }
            }
        }
        if (localRepositoryId == 0) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                    .append(" (folder_path) VALUES ('")
                    .append(localRepositoryPath)
                    .append("')");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the local repository, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            localRepositoryId = generatedKeys.getShort(1);
                        } else {
                            throw new SQLException("Failed to get the generated local repository id.");
                        }
                    }
                }
            }
        }
        return localRepositoryId;
    }

    private static int insertProduct(RepositoryProduct productToSave, Path localRepositoryRelativePath, int remoteMissionId, int localRepositoryId,
                                     FileTime fileTime, long sizeInBytes, Connection connection)
                                     throws SQLException, IOException {

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" (name, remote_mission_id, local_repository_id, local_repository_relative_path, entry_point, size_in_bytes, acquisition_date")
                .append(", last_modified_date, geometry, data_format_type_id, pixel_type_id, sensor_type_id) VALUES ('")
                .append(productToSave.getName())
                .append("', ")
                .append(remoteMissionId)
                .append(", ")
                .append(localRepositoryId)
                .append(", '")
                .append(localRepositoryRelativePath.toString())
                .append("', '")
                .append(productToSave.getEntryPoint())
                .append("', ")
                .append(sizeInBytes)
                .append(", ")
                .append("?")
                .append(", ")
                .append("?")
                .append(", '")
                .append(productToSave.getPolygon().toWKT())
                .append("', ")
                .append(productToSave.getDataFormatType().getValue())
                .append(", ")
                .append(productToSave.getPixelType().getValue())
                .append(", ")
                .append(productToSave.getSensorType().getValue())
                .append(")");

        int productId;
        try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, new java.sql.Timestamp(productToSave.getAcquisitionDate().getTime()));
            statement.setTimestamp(2, new java.sql.Timestamp(fileTime.toMillis()));

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to insert the product, no rows affected.");
            } else {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        productId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to get the generated product id.");
                    }
                }
            }
        }
        return productId;
    }

    private static void insertRemoteProductAttributes(int productId, List<Attribute> remoteAttributes, Connection connection) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES)
                .append(" (product_id, name, value) VALUES (")
                .append(productId)
                .append(", ?, ?)");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i=0; i<remoteAttributes.size(); i++) {
                Attribute attribute = remoteAttributes.get(i);
                statement.setString(1, attribute.getName());
                statement.setString(2, attribute.getValue());
                statement.executeUpdate();
            }
        }
    }
}
