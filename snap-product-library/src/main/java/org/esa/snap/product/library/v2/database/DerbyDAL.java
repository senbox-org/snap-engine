package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.v2.activator.DerbyDatabaseActivator;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class DerbyDAL {

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private DerbyDAL() {
    }

    public static List<RepositoryProduct> loadProductList() throws SQLException, IOException {
        try (Connection connection = DerbyDatabaseActivator.getConnection(false)) {
            try (Statement statement = connection.createStatement()) {
                System.out.println("read the product list");

                StringBuilder sql = new StringBuilder();
                sql.append("SELECT p.id, p.name, p.size_in_bytes, p.acquisition_date, p.path, p.quick_look_image, rrm.mission")
                        .append(" FROM ")
                        .append(DatabaseTableNames.PRODUCTS)
                        .append(" AS p, ")
                        .append(DatabaseTableNames.REMOTE_REPOSITORY_MISSIONS)
                        .append(" AS rrm ")
                        .append("WHERE p.remote_repository_mission_id = rrm.id");

                List<RepositoryProduct> productList = new ArrayList<>();
                Map<Integer, LocalRepositoryProduct> map = new HashMap<>();
                try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String name = resultSet.getString("name");
                        String mission = resultSet.getString("mission");
                        String path = resultSet.getString("path");
                        Date acquisitionDate = resultSet.getDate("acquisition_date");
                        long sizeInBytes = resultSet.getLong("size_in_bytes");
                        LocalRepositoryProduct localProduct = new LocalRepositoryProduct(name, mission, acquisitionDate, path, sizeInBytes);
                        Blob quickLookImageBlob = resultSet.getBlob("quick_look_image");
                        if (quickLookImageBlob != null) {
                            BufferedImage quickLookImage = ImageIO.read(quickLookImageBlob.getBinaryStream());
                            localProduct.setQuickLookImage(quickLookImage);
                        }
                        productList.add(localProduct);
                        map.put(id, localProduct);
                    }
                }

                sql = new StringBuilder();
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

                Iterator<Map.Entry<Integer, LocalRepositoryProduct>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, LocalRepositoryProduct> entry = it.next();
                    int productId = entry.getKey().intValue();
                    LocalRepositoryProduct localProduct = entry.getValue();
                    List<Attribute> remoteAttributeList = remoteAttributesMap.get(productId);
                    if (remoteAttributeList != null) {
                        Attribute[] remoteAttributes = new Attribute[remoteAttributeList.size()];
                        remoteAttributeList.toArray(remoteAttributes);
                        localProduct.setRemoteAttributes(remoteAttributes);
                    }
                }

                return productList;
            }
        }
    }

    public static void saveProduct(RepositoryProduct productToSave, Path productMetadataFilePath, String repositoryId, Path localRepositoryFolderPath)
                                   throws IOException, SQLException {

        Product sourceProduct = ProductIO.readProduct(productMetadataFilePath.toFile());
        if (sourceProduct != null) {
            try {
                try (Connection connection = DerbyDatabaseActivator.getConnection(false)) {
                    System.out.println("save the product");

                    int remoteRepositoryId = saveRemoteRepository(repositoryId, connection);
                    int remoteRepositoryMissionId = saveRemoteRepositoryMission(remoteRepositoryId, productToSave.getMission(), connection);
                    int localRepositoryId = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);

                    int productId = insertProduct(productToSave, sourceProduct, productMetadataFilePath, remoteRepositoryMissionId, localRepositoryId, connection);

                    System.out.println(" saved product id = "+productId);

                    insertRemoteProductAttributes(productId, productToSave.getAttributes(), connection);
                }
            } finally {
                sourceProduct.dispose();
            }
        }
    }

    private static int saveRemoteRepositoryMission(int remoteRepositoryId, String mission, Connection connection) throws SQLException {
        int remoteRepositoryMissionId = 0;
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.REMOTE_REPOSITORY_MISSIONS)
                    .append(" WHERE remote_repository_id = ")
                    .append(remoteRepositoryId)
                    .append(" AND LOWER(mission) = '")
                    .append(mission.toLowerCase())
                    .append("'");
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                if (resultSet.next()) {
                    remoteRepositoryMissionId = resultSet.getInt("id");
                }
            }
        }
        if (remoteRepositoryMissionId == 0) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.REMOTE_REPOSITORY_MISSIONS)
                    .append(" (remote_repository_id, mission) VALUES (")
                    .append(remoteRepositoryId)
                    .append(", '")
                    .append(mission)
                    .append("')");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the remote repository mission, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            remoteRepositoryMissionId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to get the generated remote repository mission id.");
                        }
                    }
                }
            }
        }
        return remoteRepositoryMissionId;
    }

    private static int saveRemoteRepository(String repositoryName, Connection connection) throws SQLException {
        int remoteRepositoryId = 0;
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.REMOTE_REPOSITORIES)
                    .append(" WHERE LOWER(name) = '")
                    .append(repositoryName.toLowerCase())
                    .append("'");
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                if (resultSet.next()) {
                    remoteRepositoryId = resultSet.getInt("id");
                }
            }
        }
        if (remoteRepositoryId == 0) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.REMOTE_REPOSITORIES)
                    .append(" (name) VALUES ('")
                    .append(repositoryName)
                    .append("')");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the remote repository, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            remoteRepositoryId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to get the generated remote repository id.");
                        }
                    }
                }
            }
        }
        return remoteRepositoryId;
    }

    private static int saveLocalRepositoryFolderPath(Path localRepositoryFolderPath, Connection connection) throws SQLException {
        String localRepositoryPath = localRepositoryFolderPath.toString();
        int localRepositoryId = 0;
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                    .append(" WHERE LOWER(path) = '")
                    .append(localRepositoryPath.toLowerCase())
                    .append("'");
            try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                if (resultSet.next()) {
                    localRepositoryId = resultSet.getInt("id");
                }
            }
        }
        if (localRepositoryId == 0) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ")
                    .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                    .append(" (path) VALUES ('")
                    .append(localRepositoryPath)
                    .append("')");
            try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert the local repository, no rows affected.");
                } else {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            localRepositoryId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to get the generated local repository id.");
                        }
                    }
                }
            }
        }
        return localRepositoryId;
    }

    //TODO Jean remove
    public static class ByteArrayOutputStream extends java.io.ByteArrayOutputStream {

        public ByteArrayOutputStream() {
        }

        @Override
        public synchronized byte[] toByteArray() {
            return this.buf;
        }

    }

    private static int insertProduct(RepositoryProduct productToSave, Product sourceProduct, Path productMetadataFilePath,
                                     int remoteRepositoryMissionId, int localRepositoryId, Connection connection)
                                     throws SQLException, IOException {

        FileTime fileTime = Files.getLastModifiedTime(productMetadataFilePath);
        Date lastModifiedDate = new Date(fileTime.toMillis());
        Path productFolderPath = productMetadataFilePath.getParent();
        String metadataFileName = productMetadataFilePath.getFileName().toString();
        long sizeInBytes = Files.size(productFolderPath);


        ByteArrayOutputStream quickLookImageOutputStream = null;
        if (productToSave.getQuickLookImage() != null) {
            quickLookImageOutputStream = new ByteArrayOutputStream();
            ImageIO.write(productToSave.getQuickLookImage(), "png", quickLookImageOutputStream);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" (name, remote_repository_mission_id, local_repository_id, type, path, metadata_filename, size_in_bytes, acquisition_date")
                .append(", last_modified_date, quick_look_image, first_near_latitude, first_near_longitude, first_far_latitude, first_far_longitude")
                .append(", last_near_latitude, last_near_longitude, last_far_latitude, last_far_longitude")
                .append(", geometric_boundary) VALUES ('")
                .append(sourceProduct.getName())
                .append("', ")
                .append(remoteRepositoryMissionId)
                .append(", ")
                .append(localRepositoryId)
                .append(", '")
                .append(sourceProduct.getProductType())
                .append("', '")
                .append(productFolderPath.toString())
                .append("', '")
                .append(metadataFileName)
                .append("', ")
                .append(sizeInBytes)
                .append(", ")
                .append("TIMESTAMP('" + TIMESTAMP_FORMAT.format(productToSave.getAcquisitionDate()) + "')")
                .append(", ")
                .append("TIMESTAMP('" + TIMESTAMP_FORMAT.format(lastModifiedDate) + "')")
                .append(", ")
                .append("?") // quick look image
                .append(", ")
                .append(0)
                .append(", ")
                .append(0)
                .append(", ")
                .append(0)
                .append(", ")
                .append(0)
                .append(", ")
                .append(0)
                .append(", ")
                .append(0)
                .append(", ")
                .append(0)
                .append(", ")
                .append(0)
                .append(", '")
                .append("geometric_boundary_value")
                .append("')");
        System.out.println("sql='"+sql.toString()+"'");

        int productId;
        try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            if (quickLookImageOutputStream != null) {
                statement.setBlob(1, (Blob) null);
            } else {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(quickLookImageOutputStream.toByteArray());
                statement.setBlob(1, inputStream);
            }
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

    private static void insertRemoteProductAttributes(int productId, Attribute[] remoteAttributes, Connection connection) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCT_REMOTE_ATTRIBUTES)
                .append(" (product_id, name, value) VALUES (")
                .append(productId)
                .append(", ?, ?)");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i=0; i<remoteAttributes.length; i++) {
                statement.setString(1, remoteAttributes[i].getName());
                statement.setString(2, remoteAttributes[i].getValue());
                statement.executeUpdate();
            }
        }
    }
}
