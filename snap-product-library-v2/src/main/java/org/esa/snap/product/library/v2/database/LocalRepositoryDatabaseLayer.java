package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.product.library.v2.database.model.RemoteMission;
import org.esa.snap.product.library.v2.database.model.RemoteRepository;
import org.esa.snap.remote.products.repository.*;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;
import org.esa.snap.remote.products.repository.geometry.GeometryUtils;
import org.esa.snap.remote.products.repository.geometry.MultiPolygon2D;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class contains methods to access the local database where are saved the products metadata.
 *
 * Created by jcoravu on 3/9/2019.
 */
class LocalRepositoryDatabaseLayer {

    private static final Logger logger = Logger.getLogger(LocalRepositoryDatabaseLayer.class.getName());

    private static final int MAXIMUM_REMOTE_ATTRIBUTE_VALUE = 102400;
    private static final int MAXIMUM_LOCAL_ATTRIBUTE_VALUE = 1024;

    private LocalRepositoryDatabaseLayer() {
    }

    static Short loadLocalRepositoryId(Path localRepositoryFolderPath, Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id FROM ")
                .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                .append(" WHERE LOWER(folder_path) = '")
                .append(localRepositoryFolderPath.toString().toLowerCase())
                .append("'");
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            if (resultSet.next()) {
                return resultSet.getShort("id");
            }
        }
        return null;
    }

    static List<LocalProductMetadata> loadProductRelativePaths(short localRepositoryId, Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, relative_path, last_modified_date FROM ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" WHERE local_repository_id = ")
                .append(localRepositoryId);
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            List<LocalProductMetadata> repositoryProducts = new ArrayList<>();
            while (resultSet.next()) {
                int productId = resultSet.getInt("id");
                String relativePath = resultSet.getString("relative_path");
                Timestamp lastModifiedDate = resultSet.getTimestamp("last_modified_date");
                repositoryProducts.add(new LocalProductMetadata(productId, relativePath, lastModifiedDate));
            }
            return repositoryProducts;
        }
    }

    static List<String> loadUniqueRemoteMissionNames(Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT name FROM ")
                .append(DatabaseTableNames.REMOTE_MISSIONS)
                .append(" GROUP BY name");
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            List<String> remoteMissionNames = new ArrayList<>();
            while (resultSet.next()) {
                remoteMissionNames.add(resultSet.getString("name"));
            }
            return remoteMissionNames;
        }
    }

    static List<LocalRepositoryFolder> loadLocalRepositoryFolders(Statement statement) throws SQLException {
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

    static Map<Short, Set<String>> loadRemoteAttributesNamesPerMission(Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT remote_mission_id, name FROM ")
                .append(DatabaseTableNames.REMOTE_ATTRIBUTES);
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

    static Set<String> loadLocalAttributesNames(Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT name FROM ")
           .append(DatabaseTableNames.PRODUCT_LOCAL_ATTRIBUTES)
           .append(" GROUP BY name");
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            Set<String> localAttributeNames = new HashSet<>();
            while (resultSet.next()) {
                String attributeName = resultSet.getString("name");
                localAttributeNames.add(attributeName);
            }
            return localAttributeNames;
        }
    }

    static List<RepositoryProduct> loadProductList(LocalRepositoryFolder localRepositoryFolder, String remoteMissionName,
                                                   Map<String, Object> parameterValues, H2DatabaseParameters databaseParameters)
                                                   throws SQLException, IOException {

        List<RepositoryProduct> productList;
        try (Connection connection = H2DatabaseAccessor.getConnection(databaseParameters)) {
            Connection wrappedConnection = SFSUtilities.wrapConnection(connection);

            Map<Short, String> remoteRepositories = loadRemoteRepositories(connection);

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
                        SensorType sensorTypes[] = SensorType.values();
                        for (int i=0; i<sensorTypes.length && sensorType == null; i++) {
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
            sql.append(" FROM ")
               .append(DatabaseTableNames.PRODUCTS)
               .append(" AS p");
            if (localRepositoryFolder == null) {
                // no local repository filter
                sql.append(", ")
                   .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                   .append(" AS lr");
            }
            if (remoteMissionName != null) {
                // the mission is specified
                sql.append(", ")
                   .append(DatabaseTableNames.REMOTE_MISSIONS)
                   .append(" AS rm");
            }
            sql.append(" WHERE ");
            if (remoteMissionName != null) {
                // the mission is specified
                sql.append("p.remote_mission_id = rm.id AND rm.name = '")
                   .append(remoteMissionName)
                   .append("' AND ");
            }
            sql.append("p.local_repository_id = ");
            if (localRepositoryFolder == null) {
                // no local repository filter
                sql.append("lr.id");
            } else {
                sql.append(localRepositoryFolder.getId());
            }
            if (selectionArea != null) {
                Polygon2D polygon = GeometryUtils.buildPolygon(selectionArea);
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

            if (remoteMissionName == null) {
                // no mission filter
                StringBuilder outerSql = new StringBuilder();
                outerSql.append("SELECT q.id, q.name, q.relative_path, q.entry_point, q.size_in_bytes, q.geometry, q.acquisition_date, q.last_modified_date");
                if (localRepositoryFolder == null) {
                    // no local repository filter
                    outerSql.append(", q.folder_path");
                }
                outerSql.append(", left_rm.name AS remote_mission_name, left_rm.remote_repository_id")
                        .append(" FROM (")
                        .append(sql)
                        .append(") AS q")
                        .append(" LEFT OUTER JOIN ")
                        .append(DatabaseTableNames.REMOTE_MISSIONS)
                        .append(" AS left_rm ON (q.remote_mission_id = left_rm.id)");
                sql = outerSql;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The sql query is : " + sql.toString());
            }

            Calendar calendar = Calendar.getInstance();
            try (PreparedStatement prepareStatement = wrappedConnection.prepareStatement(sql.toString())) {
                int parameterIndex = 1;
                if (startDate != null) {
                    calendar.setTimeInMillis(startDate.getTime());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    java.sql.Timestamp startTimestamp = new java.sql.Timestamp(calendar.getTimeInMillis());
                    prepareStatement.setTimestamp(parameterIndex++, startTimestamp);
                }
                if (endDate != null) {
                    calendar.setTimeInMillis(endDate.getTime());
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 0);
                    java.sql.Timestamp endTimestamp = new java.sql.Timestamp(calendar.getTimeInMillis());
                    prepareStatement.setTimestamp(parameterIndex++, endTimestamp);
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
                            short remoteRepositoryId = ((Short)remoteRepositoryIdAsObject).shortValue();
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
                            List<Attribute> productRemoteAttributes = loadProductRemoteAttributes(localProduct.getId(), statement);
                            List<Attribute> productLocalAttributes = loadProductLocalAttributes(localProduct.getId(), statement);
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
            Path quickLookImagesFolder = databaseParameters.getParentFolderPath().resolve("quick-look-images");

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

    private static List<Attribute> loadProductRemoteAttributes(int productId, Statement statement) throws SQLException {
        return loadProductAttributes(productId, DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES, statement);
    }

    private static List<Attribute> loadProductLocalAttributes(int productId, Statement statement) throws SQLException {
        return loadProductAttributes(productId, DatabaseTableNames.PRODUCT_LOCAL_ATTRIBUTES, statement);
    }

    private static List<Attribute> loadProductAttributes(int productId, String tableName, Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT name, value FROM ")
                .append(tableName)
                .append(" WHERE product_id = ")
                .append(productId);
        try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
            List<Attribute> productAttributes = new ArrayList<>();
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String value = resultSet.getString("value");
                productAttributes.add(new Attribute(name, value));
            }
            return productAttributes;
        }
    }

    static void deleteLocalRepositoryFolder(LocalRepositoryFolder localRepositoryFolder, H2DatabaseParameters databaseParameters) throws SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection(databaseParameters)) {
            Set<Integer> productIds;
            try (Statement statement = connection.createStatement()) {
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT id FROM ")
                        .append(DatabaseTableNames.PRODUCTS)
                        .append(" WHERE local_repository_id = ")
                        .append(localRepositoryFolder.getId());
                try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                    productIds = new HashSet<>();
                    while (resultSet.next()) {
                        productIds.add(resultSet.getInt("id"));
                    }
                }
            }

            boolean success = false;
            connection.setAutoCommit(false);
            try {
                deleteRecordsFromTable(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES, "product_id", productIds, connection);
                deleteRecordsFromTable(DatabaseTableNames.PRODUCT_LOCAL_ATTRIBUTES, "product_id", productIds, connection);

                try (Statement statement = connection.createStatement()) {
                    deleteRecordsFromTable(DatabaseTableNames.PRODUCTS, "local_repository_id", localRepositoryFolder.getId(), statement);
                    deleteRecordsFromTable(DatabaseTableNames.LOCAL_REPOSITORIES, "id", localRepositoryFolder.getId(), statement);
                }

                // commit the data
                connection.commit();

                success = true;
            } finally {
                if (!success) {
                    // rollback the statements from the transaction
                    connection.rollback();
                }
            }
        }
    }

    static void deleteProduct(LocalRepositoryProduct repositoryProduct, H2DatabaseParameters databaseParameters) throws SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection(databaseParameters)) {
            boolean success = false;
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                deleteProduct(repositoryProduct.getId(), statement);

                // commit the data
                connection.commit();

                success = true;
            } finally {
                if (!success) {
                    // rollback the statements from the transaction
                    connection.rollback();
                }
            }
        }
    }

    private static void deleteProduct(int productId, Statement statement) throws SQLException {
        deleteProductRemoteAttributes(productId, statement);
        deleteProductLocalAttributes(productId, statement);
        deleteRecordsFromTable(DatabaseTableNames.PRODUCTS, "id", productId, statement);
    }

    private static void deleteProductRemoteAttributes(int productId, Statement statement) throws SQLException {
        deleteRecordsFromTable(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES, "product_id", productId, statement);
    }

    private static void deleteProductLocalAttributes(int productId, Statement statement) throws SQLException {
        deleteRecordsFromTable(DatabaseTableNames.PRODUCT_LOCAL_ATTRIBUTES, "product_id", productId, statement);
    }

    private static int deleteRecordsFromTable(String tableName, String columnName, int columnValue, Statement statement) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ")
                .append(tableName)
                .append(" WHERE ")
                .append(columnName)
                .append(" = ")
                .append(columnValue);
        return statement.executeUpdate(sql.toString());
    }

    private static void deleteRecordsFromTable(String tableName, String columnName, Set<Integer> productIds, Connection connection) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ")
                .append(tableName)
                .append(" WHERE ")
                .append(columnName)
                .append(" = ?");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString(), Statement.NO_GENERATED_KEYS)) {
            for (Integer productId : productIds) {
                preparedStatement.setInt(1, productId.intValue());
                preparedStatement.executeUpdate();
            }
        }
    }

    private static Path extractProductPathRelativeToLocalRepositoryFolder(Path productPath, Path localRepositoryFolderPath) {
        if (productPath == null) {
            throw new NullPointerException("The product folder path is null.");
        }
        if (localRepositoryFolderPath == null) {
            throw new NullPointerException("The local repository folder path is null.");
        }
        Path parent = productPath;
        while (parent != null && parent.compareTo(localRepositoryFolderPath) != 0) {
            parent = parent.getParent();
        }
        Path relativePath;
        if (parent == null) {
            throw new IllegalArgumentException("The product folder path '"+ productPath.toString()+"' must be specified into the local repository folder path '"+localRepositoryFolderPath.toString()+"'.");
        } else {
            int localRepositoryNameCount = localRepositoryFolderPath.getNameCount();
            relativePath = productPath.subpath(localRepositoryNameCount, productPath.getNameCount());
        }
        return relativePath;
    }

    static Set<Integer> deleteMissingLocalRepositoryProducts(short localRepositoryId, Set<Integer> savedProductIds, H2DatabaseParameters databaseParameters) throws SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection(databaseParameters)) {
            boolean success = false;
            connection.setAutoCommit(false);
            try {
                Set<Integer> missingProductIds;
                try (Statement statement = connection.createStatement()) {
                    StringBuilder sql = new StringBuilder();
                    sql.append("SELECT id FROM ")
                            .append(DatabaseTableNames.PRODUCTS)
                            .append(" WHERE local_repository_id = ")
                            .append(localRepositoryId);
                    try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                        missingProductIds = new HashSet<>();
                        while (resultSet.next()) {
                            int productId = resultSet.getInt("id");
                            if (!savedProductIds.contains(productId)) {
                                missingProductIds.add(productId);
                            }
                        }
                    }

                    if (missingProductIds.size() > 0) {
                        for (Integer productId : missingProductIds) {
                            deleteProduct(productId.intValue(), statement);
                        }
                    }
                }

                // commit the statements
                connection.commit();

                success = true;

                return missingProductIds;
            } finally {
                if (!success) {
                    // rollback the statements from the transaction
                    connection.rollback();
                }
            }
        }
    }

    static SaveProductData saveLocalProduct(Product localProductToSave, BufferedImage quickLookImage, AbstractGeometry2D polygon2D, Path productPath,
                                       Path localRepositoryFolderPath, H2DatabaseParameters databaseParameters)
                                       throws IOException, SQLException {

        if (localProductToSave == null) {
            throw new NullPointerException("The product is null.");
        }
        if (polygon2D == null) {
            throw new NullPointerException("The product polygon is null.");
        }

        List<Attribute> localProductAttributes = extractLocalProductAttributes(localProductToSave);

        int productId;
        LocalRepositoryFolder localRepositoryFolder;
        try (Connection connection = H2DatabaseAccessor.getConnection(databaseParameters)) {
            boolean success = false;
            connection.setAutoCommit(false);
            try {
                localRepositoryFolder = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);
                Path relativePath = extractProductPathRelativeToLocalRepositoryFolder(productPath, localRepositoryFolder.getPath());

                FileTime fileTime = Files.getLastModifiedTime(productPath);
                long sizeInBytes = FileIOUtils.computeFileSize(productPath);

                Integer existingProductId = loadProductId(localRepositoryFolder.getId(), relativePath, connection);
                if (existingProductId == null) {
                    productId = insertProduct(localProductToSave, polygon2D, relativePath, localRepositoryFolder.getId(), fileTime, sizeInBytes, connection);
                } else {
                    productId = existingProductId.intValue();
                    deleteQuickLookImage(productId, databaseParameters.getParentFolderPath());
                    try (Statement statement = connection.createStatement()) {
                        deleteProductRemoteAttributes(productId, statement);
                        deleteProductLocalAttributes(productId, statement);
                    }
                    updateProduct(productId, localProductToSave, polygon2D, relativePath, localRepositoryFolder.getId(), fileTime, sizeInBytes, connection);
                }

                if (localProductAttributes.size() > 0) {
                    insertProductLocalAttributes(productId, localProductAttributes, connection);
                }

                if (quickLookImage != null) {
                    writeQuickLookImage(productId, quickLookImage, databaseParameters.getParentFolderPath());
                }

                // commit the statements
                connection.commit();

                success = true;
            } finally {
                if (!success) {
                    // rollback the statements from the transaction
                    connection.rollback();
                }
            }
        }
        return new SaveProductData(productId, null, localRepositoryFolder, localProductAttributes);
    }

    private static LocalRepositoryFolder saveLocalRepositoryFolderPath(Path localRepositoryFolderPath, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            List<LocalRepositoryFolder> localRepositoryFolders = loadLocalRepositoryFolders(statement);

            for (int i = 0; i < localRepositoryFolders.size(); i++) {
                LocalRepositoryFolder localRepositoryFolder = localRepositoryFolders.get(i);
                Path parent = localRepositoryFolderPath;
                while (parent != null && parent.compareTo(localRepositoryFolder.getPath()) != 0) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    return localRepositoryFolder;
                }
            }
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                .append(" (folder_path) VALUES ('")
                .append(localRepositoryFolderPath.toString())
                .append("')");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to insert the local repository, no rows affected.");
            } else {
                short localRepositoryId;
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        localRepositoryId = generatedKeys.getShort(1);
                    } else {
                        throw new SQLException("Failed to get the generated local repository id.");
                    }
                }
                return new LocalRepositoryFolder(localRepositoryId, localRepositoryFolderPath);
            }
        }
    }

    static void updateProductPath(LocalRepositoryProduct productToUpdate, Path productPath, Path localRepositoryFolderPath, H2DatabaseParameters databaseParameters)
                                  throws SQLException, IOException {

        LocalRepositoryFolder localRepositoryFolder;
        try (Connection connection = H2DatabaseAccessor.getConnection(databaseParameters)) {
            boolean success = false;
            connection.setAutoCommit(false);
            try {
                localRepositoryFolder = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);
                Path relativePath = extractProductPathRelativeToLocalRepositoryFolder(productPath, localRepositoryFolder.getPath());

                FileTime fileTime = Files.getLastModifiedTime(productPath);
                long sizeInBytes = FileIOUtils.computeFileSize(productPath);

                StringBuilder sql = new StringBuilder();
                sql.append("UPDATE ")
                    .append(DatabaseTableNames.PRODUCTS)
                    .append(" SET local_repository_id = ")
                    .append(localRepositoryFolder.getId())
                    .append(", relative_path = '")
                    .append(relativePath.toString())
                    .append("', size_in_bytes = ")
                    .append(sizeInBytes)
                    .append(", last_modified_date = ")
                    .append("?") // last modified date
                    .append(" WHERE id = ")
                    .append(productToUpdate.getId());
                try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                    statement.setTimestamp(1, new java.sql.Timestamp(fileTime.toMillis()));
                    int affectedRows = statement.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Failed to update the product, no rows affected.");
                    }
                }

                // commit the statements
                connection.commit();

                success = true;
            } finally {
                if (!success) {
                    // rollback the statements from the transaction
                    connection.rollback();
                }
            }
        }
    }

    private static List<Attribute> extractLocalProductAttributes(Product product) {
        MetadataElement emptyMetadata = AbstractMetadata.addAbstractedMetadataHeader(null);
        MetadataElement productRootMetadata = AbstractMetadata.getAbstractedMetadata(product);
        MetadataAttribute[] attributes = emptyMetadata.getAttributes();
        Set<String> localProductAttributes = new HashSet<>();
        List<Attribute> localAttributes = new ArrayList<>();
        for (int i=0; i<attributes.length; i++) {
            String attributeName = attributes[i].getName();
            if (localProductAttributes.add(attributeName)) {
                String attributeValue = productRootMetadata.getAttributeString(attributeName);
                localAttributes.add(new Attribute(attributeName, attributeValue));
            }
        }
        return localAttributes;
    }

    static SaveProductData saveRemoteProduct(RepositoryProduct remoteProductToSave, Path productPath, String remoteRepositoryName,
                                       Path localRepositoryFolderPath, Product localProduct, H2DatabaseParameters databaseParameters)
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
        try (Connection connection = H2DatabaseAccessor.getConnection(databaseParameters)) {
            boolean success = false;
            connection.setAutoCommit(false);
            try {
                localRepositoryFolder = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);
                Path relativePath = extractProductPathRelativeToLocalRepositoryFolder(productPath, localRepositoryFolder.getPath());

                short remoteRepositoryId = saveRemoteRepositoryName(remoteRepositoryName, connection);
                remoteRepository = new RemoteRepository(remoteRepositoryId, remoteRepositoryName);

                remoteMissionId = saveRemoteMissionName(remoteRepositoryId, remoteProductToSave.getRemoteMission().getName(), remoteProductToSave.getRemoteAttributes(), connection);

                FileTime fileTime = Files.getLastModifiedTime(productPath);
                long sizeInBytes = FileIOUtils.computeFileSize(productPath);

                Integer existingProductId = loadProductId(localRepositoryFolder.getId(), relativePath, connection);
                if (existingProductId == null) {
                    // no existing product into the database
                    productId = insertProduct(remoteProductToSave, relativePath, remoteMissionId, localRepositoryFolder.getId(), fileTime, sizeInBytes, connection);
                } else {
                    // the product already exists into the database
                    productId = existingProductId.intValue();
                    deleteQuickLookImage(productId, databaseParameters.getParentFolderPath());
                    try (Statement statement = connection.createStatement()) {
                        deleteProductRemoteAttributes(productId, statement);
                        deleteProductLocalAttributes(productId, statement);
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
                    writeQuickLookImage(productId, remoteProductToSave.getQuickLookImage(), databaseParameters.getParentFolderPath());
                }

                // commit the statements
                connection.commit();

                success = true;
            } finally {
                if (!success) {
                    // rollback the statements from the transaction
                    connection.rollback();
                }
            }
        }
        RemoteMission remoteMission = new RemoteMission(remoteMissionId, remoteProductToSave.getRemoteMission().getName(), remoteRepository);
        return new SaveProductData(productId, remoteMission, localRepositoryFolder, localProductAttributes);
    }

    public static boolean existsProductQuickLookImage(int productId, Path databaseParentFolder) {
        Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");
        Path quickLookImageFile = quickLookImagesFolder.resolve(Integer.toString(productId) + ".png");
        return Files.exists(quickLookImageFile);
    }

    private static void deleteQuickLookImage(int productId, Path databaseParentFolder) throws IOException {
        Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");
        Path quickLookImageFile = quickLookImagesFolder.resolve(Integer.toString(productId) + ".png");
        Files.deleteIfExists(quickLookImageFile);
    }

    public static void writeQuickLookImage(int productId, BufferedImage quickLookImage, Path databaseParentFolder) throws IOException {
        Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");
        FileIOUtils.ensureExists(quickLookImagesFolder);
        Path quickLookImageFile = quickLookImagesFolder.resolve(Integer.toString(productId) + ".png");
        ImageIO.write(quickLookImage, "png", quickLookImageFile.toFile());
    }

    private static Integer loadProductId(short localRepositoryId, Path relativePath, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.PRODUCTS)
                    .append(" WHERE local_repository_id = ")
                    .append(localRepositoryId)
                    .append(" AND LOWER(relative_path) = '")
                    .append(relativePath.toString().toLowerCase())
                    .append("'");
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }
        return null;
    }

    private static Map<Short, String> loadRemoteRepositories(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id, name FROM ")
                    .append(DatabaseTableNames.REMOTE_REPOSITORIES);
            Map<Short, String> remoteRepositories = new HashMap<>();
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                while (resultSet.next()) {
                    short remoteRepositoryId = resultSet.getShort("id");
                    String remoteRepositoryName = resultSet.getString("name");
                    remoteRepositories.put(remoteRepositoryId, remoteRepositoryName);
                }
            }
            return remoteRepositories;
        }
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
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = preparedStatement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the remote repository, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
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
            // insert the mission id
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.REMOTE_MISSIONS)
                    .append(" (remote_repository_id, name) VALUES (")
                    .append(remoteRepositoryId)
                    .append(", '")
                    .append(remoteMissionName)
                    .append("')");
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = preparedStatement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the remote mission, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
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

    private static void updateProduct(int productId, Product productToSave, AbstractGeometry2D geometry, Path relativePath, int localRepositoryId,
                                      FileTime fileTime, long sizeInBytes, Connection connection)
                                      throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" SET name = '")
                .append(productToSave.getName())
                .append("', remote_mission_id = ")
                .append("NULL") // remote mission
                .append(", local_repository_id = ")
                .append(localRepositoryId)
                .append(", relative_path = '")
                .append(relativePath.toString())
                .append("', entry_point = '")
                .append("NULL") // entry point
                .append("', size_in_bytes = ")
                .append(sizeInBytes)
                .append(", acquisition_date = ")
                .append("?") // acquisition date
                .append(", last_modified_date = ")
                .append("?") // last modified date
                .append(", geometry = '")
                .append(geometry.toWKT())
                .append("', data_format_type_id = ")
                .append("NULL") // format data type
                .append(", pixel_type_id = ")
                .append("NULL") // pixel type
                .append(", sensor_type_id = ")
                .append("NULL") // sensor type
                .append(" WHERE id = ")
                .append(productId);
        ProductData.UTC startTime = productToSave.getStartTime();
        Date acquisitionDate = (startTime == null) ? null : startTime.getAsDate();
        executeUpdateProductStatement(sql.toString(), acquisitionDate, fileTime, connection);
    }

    private static void executeUpdateProductStatement(String sql, Date acquisitionDate, FileTime fileTime, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            java.sql.Timestamp acquisitionDateTimestamp = null;
            if (acquisitionDate != null) {
                acquisitionDateTimestamp = new java.sql.Timestamp(acquisitionDate.getTime());
            }
            statement.setTimestamp(1, acquisitionDateTimestamp);
            statement.setTimestamp(2, new java.sql.Timestamp(fileTime.toMillis()));

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to update the product, no rows affected.");
            }
        }
    }

    private static int insertProduct(Product productToSave, AbstractGeometry2D geometry, Path relativePath, int localRepositoryId,
                                     FileTime fileTime, long sizeInBytes, Connection connection)
                                     throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" (name, remote_mission_id, local_repository_id, relative_path, entry_point, size_in_bytes, acquisition_date")
                .append(", last_modified_date, geometry, data_format_type_id, pixel_type_id, sensor_type_id) VALUES ('")
                .append(productToSave.getName())
                .append("', ")
                .append("NULL") // remote mission
                .append(", ")
                .append(localRepositoryId)
                .append(", '")
                .append(relativePath.toString())
                .append("', '")
                .append("NULL") // entry point
                .append("', ")
                .append(sizeInBytes)
                .append(", ")
                .append("?") // acquisition date
                .append(", ")
                .append("?") // last modified date
                .append(", '")
                .append(geometry.toWKT())
                .append("', ")
                .append("NULL") // format data type
                .append(", ")
                .append("NULL") // pixel type
                .append(", ")
                .append("NULL") // sensor type
                .append(")");
        ProductData.UTC startTime = productToSave.getStartTime();
        Date acquisitionDate = (startTime == null) ? null : startTime.getAsDate();
        return executeInsertProductStatement(sql.toString(), acquisitionDate, fileTime, connection);
    }

    private static int executeInsertProductStatement(String sql, Date acquisitionDate, FileTime fileTime, Connection connection) throws SQLException {
        int productId;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            java.sql.Timestamp acquisitionDateTimestamp = null;
            if (acquisitionDate != null) {
                acquisitionDateTimestamp = new java.sql.Timestamp(acquisitionDate.getTime());
            }
            preparedStatement.setTimestamp(1, acquisitionDateTimestamp);
            preparedStatement.setTimestamp(2, new java.sql.Timestamp(fileTime.toMillis()));

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to insert the product, no rows affected.");
            } else {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
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

    private static void updateProduct(int productId, RepositoryProduct productToSave, Path relativePath, int remoteMissionId,
                                      int localRepositoryId, FileTime fileTime, long sizeInBytes, Connection connection)
                                      throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" SET name = '")
                .append(productToSave.getName())
                .append("', remote_mission_id = ")
                .append(remoteMissionId)
                .append(", local_repository_id = ")
                .append(localRepositoryId)
                .append(", relative_path = '")
                .append(relativePath.toString())
                .append("', entry_point = ")
                .append("NULL")
                .append(", size_in_bytes = ")
                .append(sizeInBytes)
                .append(", acquisition_date = ")
                .append("?")
                .append(", last_modified_date = ")
                .append("?")
                .append(", geometry = '")
                .append(productToSave.getPolygon().toWKT())
                .append("', data_format_type_id = ")
                .append(productToSave.getDataFormatType().getValue())
                .append(", pixel_type_id = ")
                .append(productToSave.getPixelType().getValue())
                .append(", sensor_type_id = ")
                .append(productToSave.getSensorType().getValue())
                .append(" WHERE id = ")
                .append(productId);
        executeUpdateProductStatement(sql.toString(), productToSave.getAcquisitionDate(), fileTime, connection);
    }

    private static int insertProduct(RepositoryProduct productToSave, Path relativePath, int remoteMissionId, int localRepositoryId,
                                     FileTime fileTime, long sizeInBytes, Connection connection)
                                     throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" (name, remote_mission_id, local_repository_id, relative_path, entry_point, size_in_bytes, acquisition_date")
                .append(", last_modified_date, geometry, data_format_type_id, pixel_type_id, sensor_type_id) VALUES ('")
                .append(productToSave.getName())
                .append("', ")
                .append(remoteMissionId)
                .append(", ")
                .append(localRepositoryId)
                .append(", '")
                .append(relativePath.toString())
                .append("', ")
                .append("NULL")
                .append(", ")
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
        return executeInsertProductStatement(sql.toString(), productToSave.getAcquisitionDate(), fileTime, connection);
    }

    private static void insertProductRemoteAttributes(int productId, List<Attribute> remoteAttributes, Connection connection) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES)
                .append(" (product_id, name, value) VALUES (")
                .append(productId)
                .append(", ?, ?)");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i=0; i<remoteAttributes.size(); i++) {
                Attribute attribute = remoteAttributes.get(i);
                if (attribute.getValue().length() <= MAXIMUM_REMOTE_ATTRIBUTE_VALUE) {
                    preparedStatement.setString(1, attribute.getName());
                    preparedStatement.setString(2, attribute.getValue());
                    preparedStatement.executeUpdate();
                }
            }
        }
    }

    private static void insertProductLocalAttributes(int productId,List<Attribute> localAttributes, Connection connection) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCT_LOCAL_ATTRIBUTES)
                .append(" (product_id, name, value) VALUES (")
                .append(productId)
                .append(", ?, ?)");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i=0; i<localAttributes.size(); i++) {
                Attribute attribute = localAttributes.get(i);
                if (attribute.getValue().length() <= MAXIMUM_LOCAL_ATTRIBUTE_VALUE) {
                    preparedStatement.setString(1, attribute.getName());
                    preparedStatement.setString(2, attribute.getValue());
                    preparedStatement.executeUpdate();
                }
            }
        }
    }
}
