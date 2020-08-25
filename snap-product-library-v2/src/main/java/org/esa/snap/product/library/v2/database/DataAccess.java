package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.product.library.v2.database.model.RemoteMission;
import org.esa.snap.product.library.v2.database.model.RemoteRepository;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.SensorType;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;
import org.esa.snap.remote.products.repository.geometry.GeometryUtils;
import org.esa.snap.remote.products.repository.geometry.MultiPolygon2D;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;
import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The class contains methods to access the local database where are saved the products metadata.
 *
 * Created by jcoravu on 3/9/2019.
 */
public class DataAccess {

    private static final Logger logger = Logger.getLogger(DataAccess.class.getName());

    private static final int MAXIMUM_REMOTE_ATTRIBUTE_VALUE = 102400;
    private static final int MAXIMUM_LOCAL_ATTRIBUTE_VALUE = 1024;

    public static final String DATABASE_DEFINITION_LANGUAGE_SOURCE_FOLDER_PATH = "org/esa/snap/product/library/v2/database";
    public static final String DATABASE_SQL_FILE_NAME_PREFIX = "h2gis-database-script-";

    private static final Set<String> SIMPLE_PRODUCT_EXTENSIONS = new HashSet<String>() {{
        add(".png"); add(".bmp"); add(".gif");
    }};

    private static H2DatabaseParameters dbParams;

    public static void initialize() {
        dbParams = new H2DatabaseParameters(SystemUtils.getApplicationDataDir(true).toPath().resolve("product-library"));
    }

    public static void setDbParams(H2DatabaseParameters dbParams) {
        DataAccess.dbParams = dbParams;
    }

    public static Path getDatabaseFolder() {
        return dbParams.getParentFolderPath();
    }

    public static void upgradeDatabase() throws SQLException, IOException {
        try (Connection connection = getConnection()) {
            int currentDatabaseVersion = 0;
            // check if the 'version' table exists into the database
            if (existsTable("VERSIONS")) {
                // the 'version' table exists and load the current database version number
                currentDatabaseVersion = getCurrentDBVersion();
            }

            final LinkedHashMap<Integer, List<String>> allStatements =
                    DatabaseUtils.loadDatabaseStatements(DATABASE_DEFINITION_LANGUAGE_SOURCE_FOLDER_PATH, DATABASE_SQL_FILE_NAME_PREFIX, currentDatabaseVersion);
            if (allStatements.size() > 0) {
                if (!connection.getAutoCommit()) {
                    throw new IllegalStateException("The connection has an opened transaction.");
                }
                try {
                    if (currentDatabaseVersion == 0) {
                        H2GISFunctions.load(connection);
                    }
                    connection.setAutoCommit(false);
                    for (Map.Entry<Integer, List<String>> entry : allStatements.entrySet()) {
                        final Statement statement = connection.createStatement();
                        for (String sql : entry.getValue()) {
                            statement.addBatch(sql);
                        }
                        statement.addBatch(String.format("INSERT INTO versions (id) VALUES (%d)", entry.getKey()));
                        statement.executeBatch();
                    }
                    connection.commit();
                } catch (Exception e) {
                    // rollback the statements from the transaction
                    connection.rollback();
                }
            }
        }
    }

    static Short getLocalRepositoryId(Path localRepositoryFolderPath) throws SQLException {
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT id FROM local_repositories WHERE LOWER(folder_path) = ?");
            statement.setString(1, localRepositoryFolderPath.toString().toLowerCase());
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getShort("id");
            }
        }
        return null;
    }

    static List<LocalProductMetadata> loadProductRelativePaths(short localRepositoryId) throws SQLException {
        final List<LocalProductMetadata> repositoryProducts = new ArrayList<>();
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT id, relative_path, last_modified_date FROM products WHERE local_repository_id = ?");
            statement.setShort(1, localRepositoryId);
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                repositoryProducts.add(new LocalProductMetadata(resultSet.getInt("id"),
                                                                resultSet.getString("relative_path"),
                                                                resultSet.getTimestamp("last_modified_date")));
            }
        }
        return repositoryProducts;
    }

    static List<String> listRemoteMissionNames() throws SQLException {
        final List<String> remoteMissionNames = new ArrayList<>();
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT DISTINCT name FROM remote_missions ORDER BY name");
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                remoteMissionNames.add(resultSet.getString("name"));
            }
        }
        return remoteMissionNames;
    }

    static List<LocalRepositoryFolder> listLocalRepositoryFolders() throws SQLException {
        final List<LocalRepositoryFolder> results = new ArrayList<>();
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT id, folder_path FROM local_repositories");
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(new LocalRepositoryFolder(resultSet.getShort("id"),
                                                      Paths.get(resultSet.getString("folder_path"))));
            }
        }
        return results;
    }

    static Map<Short, Set<String>> listRemoteAttributeNames() throws SQLException {
        final Map<Short, Set<String>> attributes = new HashMap<>();
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT remote_mission_id, name FROM remote_attributes");
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                final Set<String> missionAttributes = attributes.computeIfAbsent(resultSet.getShort("remote_mission_id"), k -> new HashSet<>());
                missionAttributes.add(resultSet.getString("name"));
            }
        }
        return attributes;
    }

    static Set<String> listLocalAttributeNames() throws SQLException {
        final Set<String> attributes = new HashSet<>();
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT name FROM product_local_attributes ORDER BY name");
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                attributes.add(resultSet.getString("name"));
            }
        }
        return attributes;
    }

    static List<RepositoryProduct> getProducts(LocalRepositoryFolder localRepositoryFolder, String remoteMissionName,
                                               Map<String, Object> parameterValues) throws SQLException, IOException {

        List<RepositoryProduct> productList;
        try (Connection connection = getConnection()) {
            final Connection wrappedConnection = SFSUtilities.wrapConnection(connection);
            Map<Short, String> remoteRepositories = listRemoteRepositories();

            Date startDate = null;
            Date endDate = null;
            List<AttributeFilter> attributesToFind = null;
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
                    String selectedSensorType = (String)parameterValue;
                    if (selectedSensorType != null) {
                        SensorType[] sensorTypes = SensorType.values();
                        for (int i = 0; i < sensorTypes.length && sensorType == null; i++) {
                            if (selectedSensorType.equals(sensorTypes[i].getName())) {
                                sensorType = sensorTypes[i];
                            }
                        }
                        if (sensorType == null) {
                            throw new IllegalStateException("Unknown sensor type '" + selectedSensorType+"'.");
                        }
                    }
                } else if (parameterName.equalsIgnoreCase(AllLocalFolderProductsRepository.ATTRIBUTES_PARAMETER)) {
                    attributesToFind = (List<AttributeFilter>)parameterValue;
                } else {
                    throw new IllegalStateException("Unknown parameter '" + parameterName + "'.");
                }
            }
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.id, p.name, p.relative_path, p.entry_point, p.size_in_bytes, p.geometry, p.acquisition_date, p.last_modified_date, p.remote_mission_id");
            if (localRepositoryFolder == null) {
                // no local repository filter
                sql.append(", lr.folder_path");
            }
            if (remoteMissionName != null) {
                // the mission is specified
                sql.append(", rm.name AS remote_mission_name, rm.remote_repository_id");
            }
            sql.append(" FROM products AS p");
            if (localRepositoryFolder == null) {
                // no local repository filter
                sql.append(", local_repositories as lr");
            }
            if (remoteMissionName != null) {
                // the mission is specified
                sql.append(", remote_missions as rm");
            }
            sql.append(" WHERE ");
            if (remoteMissionName != null) {
                // the mission is specified
                sql.append(String.format("p.remote_mission_id = rm.id AND rm.name = '%s' AND ", remoteMissionName));
            }
            sql.append("p.local_repository_id = ").append(localRepositoryFolder == null ?  "lr.id" : localRepositoryFolder.getId());
            if (selectionArea != null) {
                final Polygon2D polygon = GeometryUtils.buildPolygon(selectionArea);
                sql.append(String.format(" AND ST_Intersects(p.geometry, '%s')", polygon.toWKT()));
            }
            if (startDate != null) {
                sql.append(" AND p.acquisition_date >= ?");
            }
            if (endDate != null) {
                sql.append(" AND p.acquisition_date <= ?");
            }
            if (sensorType != null) {
                sql.append(" AND p.sensor_type_id = ").append(sensorType.getValue());
            }

            if (remoteMissionName == null) {
                // no mission filter
                final StringBuilder outerSql = new StringBuilder();
                outerSql.append("SELECT q.id, q.name, q.relative_path, q.entry_point, q.size_in_bytes, q.geometry, q.acquisition_date, q.last_modified_date");
                if (localRepositoryFolder == null) {
                    // no local repository filter
                    outerSql.append(", q.folder_path");
                }
                outerSql.append(", left_rm.name AS remote_mission_name, left_rm.remote_repository_id FROM (")
                        .append(sql)
                        .append(") AS q LEFT OUTER JOIN remote_missions AS left_rm ON (q.remote_mission_id = left_rm.id)");
                sql = outerSql;
            }

            logger.log(Level.FINE, "The sql query is : " + sql.toString());

            Calendar calendar = Calendar.getInstance();
            try (PreparedStatement prepareStatement = wrappedConnection.prepareStatement(sql.toString())) {
                int parameterIndex = 1;
                if (startDate != null) {
                    LocalDateTime startDt = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    startDt = startDt.withHour(0).withMinute(0).withSecond(0);
                    prepareStatement.setTimestamp(parameterIndex++, Timestamp.valueOf(startDt));
                }
                if (endDate != null) {
                    LocalDateTime endDt = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    endDt = endDt.withHour(23).withMinute(59).withSecond(59);
                    prepareStatement.setTimestamp(parameterIndex, Timestamp.valueOf(endDt));
                }
                try (SpatialResultSet resultSet = prepareStatement.executeQuery().unwrap(SpatialResultSet.class)) {
                    productList = new ArrayList<>();
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String name = resultSet.getString("name");
                        String localPath = resultSet.getString("relative_path");
                        Path productLocalPath;
                        if (localRepositoryFolder == null) {
                            // no local repository filter
                            String localRepositoryFolderPath = resultSet.getString("folder_path");
                            productLocalPath = Paths.get(localRepositoryFolderPath, localPath);
                        } else {
                            productLocalPath = localRepositoryFolder.getPath().resolve(localPath);
                        }
                        org.esa.snap.remote.products.repository.RemoteMission remoteMission = null;
                        Object remoteRepositoryIdAsObject = resultSet.getObject("remote_repository_id");
                        if (remoteRepositoryIdAsObject != null) {
                            String missionName = resultSet.getString("remote_mission_name"); // the remote mission name my be null
                            if (missionName == null) {
                                throw new NullPointerException("The mission is null.");
                            }
                            short remoteRepositoryId = (Short) remoteRepositoryIdAsObject;
                            String remoteRepositoryName = remoteRepositories.get(remoteRepositoryId);
                            if (remoteRepositoryName == null) {
                                throw new NullPointerException("The remote repository is null.");
                            }
                            remoteMission = new org.esa.snap.remote.products.repository.RemoteMission(missionName, remoteRepositoryName);
                        }
                        Timestamp acquisitionDate = resultSet.getTimestamp("acquisition_date");
                        long sizeInBytes = resultSet.getLong("size_in_bytes");
                        Geometry productGeometry = resultSet.getGeometry("geometry");
                        //TODO Jean use GeometryUtils.convertProductGeometry(productGeometry)
                        //AbstractGeometry2D geometry = GeometryUtils.convertProductGeometry(productGeometry);
                        AbstractGeometry2D geometry = convertProductGeometry(productGeometry);
                        LocalRepositoryProduct localProduct = new LocalRepositoryProduct(id, name, acquisitionDate, productLocalPath, sizeInBytes, geometry);
                        localProduct.setRemoteMission(remoteMission);
                        productList.add(localProduct);
                    }
                }

                if (productList.size() > 0) {
                    try (Statement statement = connection.createStatement()) {
                        for (int i=productList.size()-1; i>=0; i--) {
                            LocalRepositoryProduct localProduct = (LocalRepositoryProduct)productList.get(i);
                            List<Attribute> productRemoteAttributes = getProductRemoteAttributes(localProduct.getId());
                            List<Attribute> productLocalAttributes = getProductLocalAttributes(localProduct.getId());
                            boolean foundAllAttributes = true;
                            if (attributesToFind != null && attributesToFind.size() > 0) {
                                foundAllAttributes = checkProductAttributesMatches(productRemoteAttributes, productLocalAttributes, attributesToFind);
                            }
                            if (foundAllAttributes) {
                                localProduct.setRemoteAttributes(productRemoteAttributes);
                                localProduct.setLocalAttributes(productLocalAttributes);
                            } else {
                                productList.remove(i);
                            }
                        }
                    }
                }
            }
        }

        if (productList.size() > 0) {
            final Path quickLookImagesFolder = dbParams.getParentFolderPath().resolve("quick-look-images");
            for (RepositoryProduct repositoryProduct : productList) {
                LocalRepositoryProduct localProduct = (LocalRepositoryProduct) repositoryProduct;
                Path quickLookImageFile = quickLookImagesFolder.resolve(localProduct.getId() + ".png");
                if (Files.exists(quickLookImageFile)) {
                    BufferedImage quickLookImage = ImageIO.read(quickLookImageFile.toFile());
                    localProduct.setQuickLookImage(quickLookImage);
                }
            }
        }

        return productList;
    }

    private static Polygon2D buildPolygon(Polygon polygon) {
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        Coordinate firstCoordinate = coordinates[0];
        Coordinate lastCoordinate = coordinates[coordinates.length-1];
        if (firstCoordinate.getX() != lastCoordinate.getX() || firstCoordinate.getY() != lastCoordinate.getY()) {
            throw new IllegalStateException("The first and last coordinates of the polygon do not match.");
        }
        Polygon2D polygon2D = new Polygon2D();
        for (Coordinate coordinate : coordinates) {
            polygon2D.append(coordinate.getX(), coordinate.getY());
        }
        return polygon2D;
    }

    //TODO Jean remove after fixing the bug: java.lang.LinkageError: loader constraint violation: loader (instance of org/netbeans/StandardModule$OneModuleClassLoader)
    // previously initiated loading for a different type with name "org/locationtech/jts/geom/Geometry"
    private static AbstractGeometry2D convertProductGeometry(Geometry productGeometry) {
        AbstractGeometry2D geometry;
        if (productGeometry instanceof MultiPolygon) {
            MultiPolygon multiPolygon = (MultiPolygon)productGeometry;
            MultiPolygon2D multiPolygon2D = new MultiPolygon2D();
            for (int p=0; p<multiPolygon.getNumGeometries(); p++) {
                if (multiPolygon.getGeometryN(p) instanceof Polygon) {
                    Polygon2D polygon2D = buildPolygon((Polygon)multiPolygon.getGeometryN(p));
                    multiPolygon2D.setPolygon(p, polygon2D);
                } else {
                    throw new IllegalStateException("The multipolygon first geometry is not a polygon.");
                }
            }
            geometry = multiPolygon2D;
        } else if (productGeometry instanceof Polygon) {
            geometry = buildPolygon((Polygon)productGeometry);
        } else {
            throw new IllegalStateException("The product geometry type '"+productGeometry.getClass().getName()+"' is not a '"+Polygon.class.getName()+"' type.");
        }
        return geometry;
    }

    private static boolean checkProductAttributesMatches(List<Attribute> existingRemoteAttributes, List<Attribute> existingLocalAttributes, List<AttributeFilter> attributesToFind) {
        boolean foundAllAttributes = true;
        for (int k = 0; k < attributesToFind.size() && foundAllAttributes; k++) {
            AttributeFilter filterAttribute = attributesToFind.get(k);
            boolean found = false;
            for (int j = 0; j < existingRemoteAttributes.size() && !found; j++) {
                Attribute productAttribute = existingRemoteAttributes.get(j);
                if (filterAttribute.matches(productAttribute)) {
                    found = true;
                }
            }
            if (!found) {
                for (int j = 0; j < existingLocalAttributes.size() && !found; j++) {
                    Attribute productAttribute = existingLocalAttributes.get(j);
                    if (filterAttribute.matches(productAttribute)) {
                        found = true;
                    }
                }
                if (!found) {
                    foundAllAttributes = false;
                }
            }
        }
        return foundAllAttributes;
    }

    private static List<Attribute> getProductRemoteAttributes(int productId) throws SQLException {
        return getProductAttributes(productId, false);
    }

    private static List<Attribute> getProductLocalAttributes(int productId) throws SQLException {
        return getProductAttributes(productId, true);
    }

    private static List<Attribute> getProductAttributes(int productId, boolean local) throws SQLException {
        final List<Attribute> productAttributes = new ArrayList<>();
        try (Connection connection = getConnection()) {
            final PreparedStatement statement =
                    connection.prepareStatement("SELECT name, value FROM " + (local ? "product_local_attributes" : "product_remote_attributes") + " WHERE product_id = ? ORDER BY name");
            statement.setInt(1, productId);
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                productAttributes.add(new Attribute(resultSet.getString("name"), resultSet.getString("value")));
            }
        }
        return productAttributes;
    }

    static void deleteLocalRepositoryFolder(LocalRepositoryFolder localRepositoryFolder) throws SQLException {
        try (Connection connection = getConnection()) {
            deleteRecordsFromTable(connection, "local_repositories", "id", (int) localRepositoryFolder.getId());
        }
    }

    static void deleteProduct(int productId) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                //deleteProductRemoteAttributes(productId, connection);
                //deleteProductLocalAttributes(productId, connection);
                deleteRecordsFromTable(connection, "products", "id", productId);
                // commit the data
                connection.commit();
            } catch (Exception e) {
                // rollback the statements from the transaction
                connection.rollback();
            }
        }
    }

    static void deleteProduct(LocalRepositoryProduct repositoryProduct) throws SQLException {
        deleteProduct(repositoryProduct.getId());
    }

    private static void deleteProductRemoteAttributes(int productId, Connection connection) throws SQLException {
        deleteRecordsFromTable(connection, "product_remote_attributes", "product_id", productId);
    }

    private static void deleteProductLocalAttributes(int productId, Connection connection) throws SQLException {
        deleteRecordsFromTable(connection, "product_local_attributes", "product_id", productId);
    }

    private static void deleteRecordsFromTable(Connection connection, String tableName, String columnName, Integer... values) throws SQLException {
        final PreparedStatement statement =
                connection.prepareStatement(
                        String.format("DELETE FROM %s WHERE %s %s",
                                      tableName,
                                      columnName,
                                      values != null && values.length > 1 ?
                                              " IN (" + Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(",")) + ")" :
                                              " = " + (values != null ? values[0] : "null")));

        statement.executeUpdate();
    }

    private static Path extractProductPathRelativeToLocalRepositoryFolder(Path productPath, Path localRepositoryFolderPath) {
        if (productPath == null) {
            throw new NullPointerException("The product folder path is null.");
        }
        if (localRepositoryFolderPath == null) {
            throw new NullPointerException("The local repository folder path is null.");
        }
        try {
            return localRepositoryFolderPath.relativize(productPath);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("The product folder path '%s' must be specified into the local repository folder path '%s'.",
                                                             productPath, localRepositoryFolderPath));
        }
    }

    static Set<Integer> deleteMissingLocalRepositoryProducts(short localRepositoryId, Set<Integer> savedProductIds) throws SQLException {
        try (Connection connection = getConnection()) {
            final PreparedStatement statement =
                    connection.prepareStatement(String.format("SELECT id FROM products WHERE local_repository_id = ? AND id NOT IN (%s)",
                            savedProductIds.stream().map(String::valueOf).collect(Collectors.joining(","))));
            statement.setShort(1, localRepositoryId);
            final ResultSet resultSet = statement.executeQuery();
            Set<Integer> missingProductIds = new HashSet<>();
            while (resultSet.next()) {
                final int id = resultSet.getInt("id");
                deleteProduct(id);
                missingProductIds.add(id);
            }
            return missingProductIds;
        }
    }

    static SaveProductData saveLocalProduct(Product localProductToSave, BufferedImage quickLookImage, AbstractGeometry2D polygon2D, Path productPath,
                                       Path localRepositoryFolderPath) throws IOException, SQLException {

        if (localProductToSave == null) {
            throw new NullPointerException("The product is null.");
        }
        if (polygon2D == null) {
            throw new NullPointerException("The product polygon is null.");
        }

        List<Attribute> localProductAttributes = extractLocalProductAttributes(localProductToSave);

        LocalRepositoryFolder localRepositoryFolder = null;
        Integer productId = 0;
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                localRepositoryFolder = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);
                Path relativePath = extractProductPathRelativeToLocalRepositoryFolder(productPath, localRepositoryFolder.getPath());

                FileTime fileTime = Files.getLastModifiedTime(productPath);
                long sizeInBytes = computeFileSize(localProductToSave, productPath);
                productId = getProductId(localRepositoryFolder.getId(), relativePath);
                if (productId == null) {
                    productId = insertProduct(localProductToSave, polygon2D, relativePath, localRepositoryFolder.getId(), fileTime, sizeInBytes, connection);
                } else {
                    deleteQuickLookImage(productId, dbParams.getParentFolderPath());
                    deleteProductRemoteAttributes(productId, connection);
                    deleteProductLocalAttributes(productId, connection);
                    updateProduct(productId, localProductToSave, polygon2D, relativePath, localRepositoryFolder.getId(), fileTime, sizeInBytes, connection);
                }

                if (localProductAttributes.size() > 0) {
                    insertProductLocalAttributes(productId, localProductAttributes, connection);
                }

                if (quickLookImage != null) {
                    writeQuickLookImage(productId, quickLookImage, dbParams.getParentFolderPath());
                }

                // commit the statements
                connection.commit();
            } catch (Exception e) {
                // rollback the statements from the transaction
                connection.rollback();
            }
        }
        return new SaveProductData(productId, null, localRepositoryFolder, localProductAttributes);
    }

    static LocalRepositoryFolder saveLocalRepositoryFolderPath(Path localRepositoryFolderPath, Connection connection) throws SQLException {
        Connection conn = connection != null ? connection : getConnection();
        try {
            final String path = localRepositoryFolderPath.toString();
            PreparedStatement statement = conn.prepareStatement("SELECT id, folder_path FROM local_repositories WHERE folder_path = ? OR INSTR(?, folder_path) > 0");
            statement.setString(1, path);
            statement.setString(2, path);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new LocalRepositoryFolder(resultSet.getShort("id"), Paths.get(resultSet.getString("folder_path")));
            }

            statement = conn.prepareStatement("INSERT INTO local_repositories (folder_path) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, path);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to insert the local repository, no rows affected.");
            } else {
                short localRepositoryId;
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        localRepositoryId = generatedKeys.getShort(1);
                    } else {
                        throw new SQLException("Failed to get the generated local repository id.");
                    }
                }
                return new LocalRepositoryFolder(localRepositoryId, localRepositoryFolderPath);
            }
        } finally {
            if (connection == null) {
                conn.close();
            }
        }
    }

    static void updateProductPath(LocalRepositoryProduct productToUpdate, Path productPath, Path localRepositoryFolderPath)
                                  throws SQLException, IOException {

        LocalRepositoryFolder localRepositoryFolder;
        try (Connection connection = getConnection()) {
            try {
                connection.setAutoCommit(false);
                localRepositoryFolder = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);
                final Path relativePath = extractProductPathRelativeToLocalRepositoryFolder(productPath, localRepositoryFolder.getPath());
                final FileTime fileTime = Files.getLastModifiedTime(productPath);
                final long sizeInBytes = FileIOUtils.computeFileSize(productPath);
                PreparedStatement statement = connection.prepareStatement("UPDATE products SET local_repository_id = ?, relative_path = ?, size_in_bytes = ?, last_modified_date = ? WHERE id = ?");
                statement.setShort(1, localRepositoryFolder.getId());
                statement.setString(2, relativePath.toString());
                statement.setLong(3, sizeInBytes);
                statement.setTimestamp(4, new java.sql.Timestamp(fileTime.toMillis()));
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to update the product, no rows affected.");
                }
                // commit the statements
                connection.commit();
            } catch (Exception e) {
                // rollback the statements from the transaction
                connection.rollback();
                throw e;
            }
        }
    }

    private static List<Attribute> extractLocalProductAttributes(Product product) {
        MetadataElement emptyMetadata = AbstractMetadata.addAbstractedMetadataHeader(null);
        MetadataElement productRootMetadata = AbstractMetadata.getAbstractedMetadata(product);
        MetadataAttribute[] attributes = emptyMetadata.getAttributes();
        //Set<String> localProductAttributes = new HashSet<>();
        Set<Attribute> localAttributes = new HashSet<>();
        for (MetadataAttribute attribute : attributes) {
            localAttributes.add(new Attribute(attribute.getName(), productRootMetadata.getAttributeString(attribute.getName())));
        }
        return new ArrayList<>(localAttributes);
    }

    static SaveProductData saveRemoteProduct(RepositoryProduct remoteProductToSave, Path productPath, String remoteRepositoryName,
                                       Path localRepositoryFolderPath, Product localProduct)
                                       throws IOException, SQLException {

        if (remoteProductToSave == null) {
            throw new NullPointerException("The product is null.");
        }

        List<Attribute> localProductAttributes = null;
        if (localProduct != null) {
            localProductAttributes = extractLocalProductAttributes(localProduct);
        }

        int productId;
        short remoteMissionId;
        RemoteRepository remoteRepository;
        LocalRepositoryFolder localRepositoryFolder;
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                localRepositoryFolder = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);
                Path relativePath = extractProductPathRelativeToLocalRepositoryFolder(productPath, localRepositoryFolder.getPath());

                short remoteRepositoryId = saveRemoteRepositoryName(remoteRepositoryName);
                remoteRepository = new RemoteRepository(remoteRepositoryId, remoteRepositoryName);
                remoteMissionId = saveRemoteMissionName(remoteRepositoryId, remoteProductToSave.getRemoteMission().getName(), remoteProductToSave.getRemoteAttributes());

                FileTime fileTime = Files.getLastModifiedTime(productPath);
                long sizeInBytes = FileIOUtils.computeFileSize(productPath);

                Integer existingProductId = getProductId(localRepositoryFolder.getId(), relativePath);
                if (existingProductId == null) {
                    // no existing product into the database
                    productId = insertProduct(remoteProductToSave, relativePath, remoteMissionId, localRepositoryFolder.getId(), fileTime, sizeInBytes, connection);
                } else {
                    // the product already exists into the database
                    productId = existingProductId;
                    deleteQuickLookImage(productId, dbParams.getParentFolderPath());
                    try (Statement statement = connection.createStatement()) {
                        deleteProductRemoteAttributes(productId, connection);
                        deleteProductLocalAttributes(productId, connection);
                    }
                    updateProduct(productId, remoteProductToSave, relativePath, remoteMissionId, localRepositoryFolder.getId(), fileTime, sizeInBytes, connection);
                }
                if (remoteProductToSave.getRemoteAttributes().size() > 0) {
                    insertProductRemoteAttributes(productId, remoteProductToSave.getRemoteAttributes(), connection);
                }
                if (localProductAttributes != null && localProductAttributes.size() > 0) {
                    insertProductLocalAttributes(productId, localProductAttributes, connection);
                }
                if (remoteProductToSave.getQuickLookImage() != null) {
                    writeQuickLookImage(productId, remoteProductToSave.getQuickLookImage(), dbParams.getParentFolderPath());
                }
                // commit the statements
                connection.commit();
            } catch (Exception e) {
                // rollback the statements from the transaction
                connection.rollback();
                throw e;
            }
        }
        RemoteMission remoteMission = new RemoteMission(remoteMissionId, remoteProductToSave.getRemoteMission().getName(), remoteRepository);
        return new SaveProductData(productId, remoteMission, localRepositoryFolder, localProductAttributes);
    }

    public static boolean existsProductQuickLookImage(int productId, Path databaseParentFolder) {
        Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");
        Path quickLookImageFile = quickLookImagesFolder.resolve(productId + ".png");
        return Files.exists(quickLookImageFile);
    }

    public static boolean checkSame(String name, Path currentPath) {
        try (Connection connection = getConnection()) {
            final PreparedStatement statement =
                    connection.prepareStatement(String.format("SELECT l.folder_path, p.relative_path FROM local_repositories l " +
                                                                      "join products p on p.local_repository_id = l.id where p.name ilike ? " +
                                                                      "or instr(?, left(l.folder_path || '%s' || p.relative_path, instr(l.folder_path || '%s' || p.relative_path, '%s',-1))) > 0",
                                                              File.separator, File.separator, File.separator));
            statement.setString(1, name + "%");
            statement.setString(2, currentPath.toString());
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String folder = resultSet.getString(1);
                String productPath = resultSet.getString(2);
                Path dbRoot = Paths.get(folder);
                Path dbExistingPath = Paths.get(productPath);
                Path currentRelative;
                try {
                    currentRelative = dbRoot.relativize(currentPath);
                    if (currentRelative.getParent() != null && dbExistingPath.getParent() != null) {
                        return currentRelative.getParent().toString().startsWith(dbExistingPath.getParent().toString());
                    }
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            return false;
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            return false;
        }
    }

    private static void deleteQuickLookImage(int productId, Path databaseParentFolder) throws IOException {
        Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");
        Path quickLookImageFile = quickLookImagesFolder.resolve(productId + ".png");
        Files.deleteIfExists(quickLookImageFile);
    }

    public static void writeQuickLookImage(int productId, BufferedImage quickLookImage, Path databaseParentFolder) throws IOException {
        Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");
        FileIOUtils.ensureExists(quickLookImagesFolder);
        Path quickLookImageFile = quickLookImagesFolder.resolve(productId + ".png");
        ImageIO.write(quickLookImage, "png", quickLookImageFile.toFile());
    }

    private static Integer getProductId(short localRepositoryId, Path relativePath) throws SQLException {
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT id FROM products WHERE local_repository_id = ? AND LOWER(relative_path) = ?");
            statement.setShort(1, localRepositoryId);
            statement.setString(2, relativePath.toString().toLowerCase());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getInt("id") : null;
        }
    }

    private static Map<Short, String> listRemoteRepositories() throws SQLException {
        final Map<Short, String> remoteRepositories = new LinkedHashMap<>();
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM remote_repositories ORDER BY name");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                remoteRepositories.put(resultSet.getShort("id"), resultSet.getString("name"));
            }
        }
        return remoteRepositories;
    }

    public static short saveRemoteRepositoryName(String remoteRepositoryName) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM remote_repositories WHERE LOWER(name) = ?");
            statement.setString(1, remoteRepositoryName.toLowerCase());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getShort(1);
            }
            statement = connection.prepareStatement("INSERT INTO remote_repositories (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, remoteRepositoryName);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to insert the remote repository, no rows affected.");
            } else {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getShort(1);
                    } else {
                        throw new SQLException("Failed to get the generated remote repository id.");
                    }
                }
            }
        } catch (SQLException ex) {
            logger.severe(ex.getMessage());
            return -1;
        }
    }

    private static short saveRemoteMissionName(int remoteRepositoryId, String remoteMissionName, List<Attribute> remoteAttributes) throws SQLException {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM remote_missions WHERE remote_repository_id = ? AND LOWER(name) = ?");
            statement.setInt(1, remoteRepositoryId);
            statement.setString(2, remoteMissionName.toLowerCase());
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getShort("id");
            }
            statement = connection.prepareStatement("INSERT INTO remote_missions (remote_repository_id, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, remoteRepositoryId);
            statement.setString(2, remoteMissionName);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to insert the remote mission, no rows affected.");
            } else {
                final short id;
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        id = generatedKeys.getShort(1);
                    } else {
                        throw new SQLException("Failed to get the generated remote mission id.");
                    }
                }
                statement = connection.prepareStatement("INSERT INTO remote_attributes (name, remote_mission_id) VALUES (?, ?)");
                for (Attribute attribute : remoteAttributes) {
                    statement.setString(1, attribute.getName());
                    statement.setShort(2, id);
                    statement.addBatch();
                }
                statement.executeBatch();
                return id;
            }
        }
    }

    private static int insertProduct(Product productToSave, AbstractGeometry2D geometry, Path relativePath, int localRepositoryId,
                                     FileTime fileTime, long sizeInBytes, Connection connection)
            throws SQLException {
        return addLocalProduct(connection, productToSave.getName(), null, localRepositoryId, relativePath.toString(),
                               sizeInBytes, productToSave.getStartTime() == null ? null : productToSave.getStartTime().getAsDate(),
                               fileTime.toMillis(), geometry.toWKT(), null, null, null);
    }

    private static int insertProduct(RepositoryProduct productToSave, Path relativePath, int remoteMissionId, int localRepositoryId,
                                     FileTime fileTime, long sizeInBytes, Connection connection)
            throws SQLException {
        return addLocalProduct(connection, productToSave.getName(), remoteMissionId, localRepositoryId, relativePath.toString(),
                               sizeInBytes, productToSave.getAcquisitionDate(), fileTime.toMillis(), productToSave.getPolygon().toWKT(),
                               productToSave.getDataFormatType().getValue(), productToSave.getPixelType().getValue(),
                               productToSave.getSensorType().getValue());
    }

    private static int addLocalProduct(Connection connection, String name, Integer remoteMissionId, int repositoryId, String relativePath,
                                       long size, Date acquisitionDate, long lastModified, String footprint,
                                       Integer dataFormatId, Integer pixelTypeId, Integer sensorTypeId) throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("INSERT INTO products " +
                                                                                "(name, remote_mission_id, local_repository_id, relative_path, size_in_bytes, acquisition_date," +
                                                                                "last_modified_date, geometry, data_format_type_id, pixel_type_id, sensor_type_id) " +
                                                                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, name);
        if (remoteMissionId != null) {
            statement.setInt(2, remoteMissionId);
        } else {
            statement.setNull(2, Types.INTEGER);
        }
        statement.setInt(3, repositoryId);
        statement.setString(4, relativePath);
        statement.setLong(5, size);
        if (acquisitionDate != null) {
            statement.setTimestamp(6, new Timestamp(acquisitionDate.getTime()));
        } else {
            statement.setNull(6, Types.TIMESTAMP);
        }
        statement.setTimestamp(7, new Timestamp(lastModified));
        statement.setString(8, footprint);
        if (dataFormatId != null) {
            statement.setInt(9, dataFormatId);
        } else {
            statement.setNull(9, Types.INTEGER);
        }
        if (pixelTypeId != null) {
            statement.setInt(10, pixelTypeId);
        } else {
            statement.setNull(10, Types.INTEGER);
        }
        if (sensorTypeId != null) {
            statement.setInt(11, sensorTypeId);
        } else {
            statement.setNull(11, Types.INTEGER);
        }
        int affectedRows = statement.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Failed to insert the product, no rows affected.");
        } else {
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to get the generated product id.");
                }
            }
        }
    }

    private static void updateProduct(int productId, Product productToSave, AbstractGeometry2D geometry, Path relativePath, int localRepositoryId,
                                      FileTime fileTime, long sizeInBytes, Connection connection)
                                      throws SQLException {
        final PreparedStatement statement =
                connection.prepareStatement("UPDATE products SET name = ?, remote_mission_id = NULL, local_repository_id = ?, relative_path = ?," +
                                            "entry_point = NULL, size_in_bytes = ?, acquisition_date = ?, last_modified_date = ?, geometry = ?," +
                                            "data_format_type_id = NULL, pixel_type_id = NULL, sensor_type_id = NULL WHERE id = ?");
        statement.setString(1, productToSave.getName());
        statement.setInt(2, localRepositoryId);
        statement.setString(3, relativePath.toString());
        statement.setLong(4, sizeInBytes);
        Date acquisitionDate = (productToSave.getStartTime() == null) ? null : productToSave.getStartTime().getAsDate();
        if (acquisitionDate != null) {
            statement.setTimestamp(5, new Timestamp(acquisitionDate.getTime()));
        } else {
            statement.setNull(5, Types.TIMESTAMP);
        }
        statement.setTimestamp(6, new Timestamp(fileTime.toMillis()));
        statement.setString(7, geometry.toWKT());
        statement.setInt(8, productId);
        statement.executeUpdate();
    }

    private static void updateProduct(int productId, RepositoryProduct productToSave, Path relativePath, int remoteMissionId,
                                      int localRepositoryId, FileTime fileTime, long sizeInBytes, Connection connection)
                                      throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("UPDATE products SET name = ?, remote_mission_id = ?," +
                "local_repository_id = ?, relative_path = ?, entry_point = NULL, size_in_bytes = ?, acquisition_date = ?, " +
                "last_modified_date = ?, geometry = ?, data_format_type_id = ?, pixel_type_id = ?, sensor_type_id = ? WHERE id = ?");
        statement.setInt(1, localRepositoryId);
        statement.setString(2, relativePath.toString());
        statement.setLong(3, sizeInBytes);
        statement.setTimestamp(4, new Timestamp(productToSave.getAcquisitionDate().getTime()));
        statement.setTimestamp(5, new Timestamp(fileTime.toMillis()));
        statement.setString(6, productToSave.getPolygon().toWKT());
        statement.setInt(7, productToSave.getDataFormatType().getValue());
        statement.setInt(8, productToSave.getPixelType().getValue());
        statement.setInt(9, productToSave.getSensorType().getValue());
        statement.setInt(10, productId);
        statement.executeUpdate();
    }

    private static void insertProductRemoteAttributes(int productId, List<Attribute> remoteAttributes, Connection connection) throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("INSERT INTO product_remote_attributes (product_id, name, value) VALUES (?, ?, ?)");
        for (Attribute attribute : remoteAttributes) {
            if (attribute.getValue().length() <= MAXIMUM_REMOTE_ATTRIBUTE_VALUE) {
                statement.setInt(1, productId);
                statement.setString(2, attribute.getName());
                statement.setString(3, attribute.getValue());
                statement.addBatch();
            }
        }
        statement.executeBatch();
    }

    private static void insertProductLocalAttributes(int productId,List<Attribute> localAttributes, Connection connection) throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("INSERT INTO product_local_attributes (product_id, name, value) VALUES (?, ?, ?)");
        for (Attribute attribute : localAttributes) {
            if (attribute.getValue().length() <= MAXIMUM_REMOTE_ATTRIBUTE_VALUE) {
                statement.setInt(1, productId);
                statement.setString(2, attribute.getName());
                statement.setString(3, attribute.getValue());
                statement.addBatch();
            }
        }
        statement.executeBatch();
    }

    private static int getCurrentDBVersion() throws SQLException {
        int currentDatabaseVersion = 0;
        try (Connection connection = getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("SELECT MAX(id) FROM versions");
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    currentDatabaseVersion = result.getInt(1);
                }
            }
        }
        return currentDatabaseVersion;
    }

    private static boolean existsTable(String tableName) throws SQLException {
        try (Connection connection = getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet result = databaseMetaData.getTables(null, null, tableName.toUpperCase(), null);
            return result.next();
        }
    }

    private static long computeFileSize(Product product, Path productPath) throws IOException {
        final Path computedPath;
        final String extension = FileUtils.getExtension(productPath.toString());
        if (ZipUtils.isZipped(productPath) || (extension != null && SIMPLE_PRODUCT_EXTENSIONS.contains(extension.toLowerCase()))) {
            computedPath = productPath;
        } else {
            final String[] formatNames = product.getProductReader().getReaderPlugIn().getFormatNames();
            if (Arrays.stream(formatNames).anyMatch(f -> f.startsWith("GeoTIFF"))) {
                computedPath = productPath;
            } else {
                computedPath = productPath.getParent();
            }
        }
        return FileIOUtils.computeFileSize(computedPath);
    }

    private static Connection getConnection() throws SQLException {
        final Connection connection = DriverManager.getConnection(dbParams.getUrl(), dbParams.getProperties());
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        return connection;
    }
}
