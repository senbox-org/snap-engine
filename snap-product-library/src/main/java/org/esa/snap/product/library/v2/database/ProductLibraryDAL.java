package org.esa.snap.product.library.v2.database;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class ProductLibraryDAL {

    private ProductLibraryDAL() {
    }

    public static List<RemoteMission> loadMissions() throws SQLException, IOException {
        List<RemoteMission> missions = new ArrayList<>();
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT rm.id, rm.name FROM ")
                        .append(DatabaseTableNames.REMOTE_MISSIONS)
                        .append(" AS rm ");
                try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                    while (resultSet.next()) {
                        short id = resultSet.getShort("id");
                        String name = resultSet.getString("name");
                        missions.add(new RemoteMission(id, name));
                    }
                }
            }
        }
        return missions;
    }

    public static List<RepositoryProduct> loadProductList(RemoteMission mission, Map<String, Object> parameterValues) throws SQLException, IOException {
        List<RepositoryProduct> productList;
        Map<Integer, LocalRepositoryProduct> productsMap;
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            Connection wrappedConnection = SFSUtilities.wrapConnection(connection);

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.id, p.name, p.type, p.local_path, p.entry_point, p.size_in_bytes, p.geometry, p.acquisition_date, p.last_modified_date")
                .append(", lr.folder_path");
            if (mission == null) {
                sql.append(", rm.name AS mission_name");
            }
            sql.append(" FROM ")
                    .append(DatabaseTableNames.PRODUCTS)
                    .append(" AS p, ")
                    .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                    .append(" AS lr")
            ;
            if (mission == null) {
                sql.append(", ")
                        .append(DatabaseTableNames.REMOTE_MISSIONS)
                        .append(" AS rm ")
                        .append("WHERE p.remote_mission_id = rm.id");
            } else {
                sql.append(" WHERE p.remote_mission_id = ")
                        .append(mission.getId());
            }

            Date startDate = null;
            Date endDate = null;
            for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                String parameterName = entry.getKey();
                Object parameterValue = entry.getValue();
                if (parameterName.equalsIgnoreCase("footprint")) {
                    Rectangle2D selectionArea = (Rectangle2D)parameterValue;
                    Polygon2D polygon = new Polygon2D();
                    polygon.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
                    polygon.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY()); // the top right corner
                    polygon.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY() + selectionArea.getHeight()); // the bottom right corner
                    polygon.append(selectionArea.getX(), selectionArea.getY() + selectionArea.getHeight()); // the bottom left corner
                    polygon.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
                    sql.append(" AND ST_Intersects(p.geometry, '")
                            .append(polygon.toWKT())
                            .append("')");
                } else if (parameterName.equalsIgnoreCase("startDate")) {
                    startDate = (Date)parameterValue;
                    if (startDate == null) {
                        throw new NullPointerException("The start date is null.");
                    }
                } else if (parameterName.equalsIgnoreCase("endDate")) {
                    endDate = (Date)parameterValue;
                    if (endDate == null) {
                        throw new NullPointerException("The end date is null.");
                    }
                } else {
                    throw new IllegalStateException("Unknown parameter '" + parameterName + "'.");
                }
            }
            if (startDate != null) {
                sql.append(" AND p.acquisition_date >= ?");
            }
            if (endDate != null) {
                sql.append(" AND p.acquisition_date <= ?");
            }
            Calendar calendar = Calendar.getInstance();
            try (PreparedStatement statement = wrappedConnection.prepareStatement(sql.toString())) {
                if (startDate != null) {
                    calendar.setTimeInMillis(startDate.getTime());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    java.sql.Timestamp startTimestamp = new java.sql.Timestamp(calendar.getTimeInMillis());
                    statement.setTimestamp(1, startTimestamp);
                }
                if (endDate != null) {
                    calendar.setTimeInMillis(endDate.getTime());
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 0);
                    java.sql.Timestamp endTimestamp = new java.sql.Timestamp(calendar.getTimeInMillis());
                    statement.setTimestamp(2, endTimestamp);
                }
                try (SpatialResultSet resultSet = statement.executeQuery().unwrap(SpatialResultSet.class)) {
                    productList = new ArrayList<>();
                    productsMap = new HashMap<>();
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String name = resultSet.getString("name");
                        String type = resultSet.getString("type");
                        String localFolderPath = resultSet.getString("folder_path");
                        String localPath = resultSet.getString("local_path");
                        Path productLocalPath = Paths.get(localFolderPath, localPath);
                        String missionName = (mission == null) ? resultSet.getString("mission_name") : mission.getName();
                        Timestamp acquisitionDate = resultSet.getTimestamp("acquisition_date");
                        long sizeInBytes = resultSet.getLong("size_in_bytes");
                        Geometry geometry = resultSet.getGeometry("geometry");
                        Polygon2D polygon = buildPolygon(geometry);
                        LocalRepositoryProduct localProduct = new LocalRepositoryProduct(name, type, acquisitionDate, productLocalPath, sizeInBytes, polygon);
                        localProduct.setMission(missionName);
                        productList.add(localProduct);
                        productsMap.put(id, localProduct);
                    }
                }
            }

            if (productList.size() > 0) {
                Map<Integer, List<Attribute>> remoteAttributesMap = loadProductRemoteAttributes(wrappedConnection);
                Iterator<Map.Entry<Integer, LocalRepositoryProduct>> it = productsMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, LocalRepositoryProduct> entry = it.next();
                    int productId = entry.getKey().intValue();
                    LocalRepositoryProduct localProduct = entry.getValue();
                    List<Attribute> remoteAttributeList = remoteAttributesMap.get(productId);
                    localProduct.setAttributes(remoteAttributeList);
                }
            }
        }

        if (productList.size() > 0) {
            Path databaseParentFolder = H2DatabaseAccessor.getDatabaseParentFolder();
            Path quickLookImagesFolder = databaseParentFolder.resolve("quick-look-images");

            Iterator<Map.Entry<Integer, LocalRepositoryProduct>> it = productsMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, LocalRepositoryProduct> entry = it.next();
                int productId = entry.getKey().intValue();
                LocalRepositoryProduct localProduct = entry.getValue();
                Path quickLookImageFile = quickLookImagesFolder.resolve(Integer.toString(productId) + ".png");
                if (Files.exists(quickLookImageFile)) {
                    BufferedImage quickLookImage = ImageIO.read(quickLookImageFile.toFile());
                    localProduct.setQuickLookImage(quickLookImage);
                }
            }
        }

        return productList;
    }

    private static Map<Integer, List<Attribute>> loadProductRemoteAttributes(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT product_id, name, value FROM ")
                    .append(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES);
            Map<Integer, List<Attribute>> remoteAttributesMap = new HashMap<>();
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                while (resultSet.next()) {
                    int productId = resultSet.getInt("product_id");
                    String name = resultSet.getString("name");
                    String value = resultSet.getString("value");
                    List<Attribute> remoteAttributes = remoteAttributesMap.get(productId);
                    if (remoteAttributes == null) {
                        remoteAttributes = new ArrayList<>();
                        remoteAttributesMap.put(productId, remoteAttributes);
                    }
                    remoteAttributes.add(new Attribute(name, value));
                }
            }
            return remoteAttributesMap;
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

    private static int insertProduct(RepositoryProduct productToSave, Path relativePath, int remoteMissionId, int localRepositoryId,
                                     FileTime fileTime, long sizeInBytes, Connection connection)
                                     throws SQLException, IOException {

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" (name, type, remote_mission_id, local_repository_id, local_path, entry_point, size_in_bytes, acquisition_date")
                .append(", last_modified_date, geometry, data_format_type_id, pixel_type_id, sensor_type_id) VALUES ('")
                .append(productToSave.getName())
                .append("', '")
                .append("product_type") //TODO Jean set the product type
                .append("', ")
                .append(remoteMissionId)
                .append(", ")
                .append(localRepositoryId)
                .append(", '")
                .append(relativePath.toString())
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
