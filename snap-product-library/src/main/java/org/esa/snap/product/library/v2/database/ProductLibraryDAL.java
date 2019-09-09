package org.esa.snap.product.library.v2.database;

import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.v2.activator.ProductLibraryActivator;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

    public static List<RepositoryProduct> loadProductList() throws SQLException, IOException {
        try (Connection connection = ProductLibraryActivator.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                System.out.println("read the product list");

                StringBuilder sql = new StringBuilder();
                sql.append("SELECT p.id, p.name, p.type, p.local_path, p.entry_point, p.size_in_bytes, p.acquisition_date, p.last_modified_date")
                        .append(" FROM ")
                        .append(DatabaseTableNames.PRODUCTS)
                        .append(" AS p ");

                List<RepositoryProduct> productList = new ArrayList<>();
                Map<Integer, LocalRepositoryProduct> map = new HashMap<>();
                try (ResultSet resultSet = statement.executeQuery(sql.toString())) {
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String name = resultSet.getString("name");
                        String type = resultSet.getString("type");
                        String localPath = resultSet.getString("local_path");
                        Date acquisitionDate = resultSet.getDate("acquisition_date");
                        long sizeInBytes = resultSet.getLong("size_in_bytes");
                        LocalRepositoryProduct localProduct = new LocalRepositoryProduct(name, type, acquisitionDate, localPath, sizeInBytes);
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
                    localProduct.setAttributes(remoteAttributeList);
                }

                return productList;
            }
        }
    }

    public static void saveProduct(RepositoryProduct productToSave, Path productFolderPath, String repositoryId, Path localRepositoryFolderPath)
                                   throws IOException, SQLException {

        //TODO Jean check if the productFolderPath belongs to the localRepositoryFolderPath

        try (Connection connection = ProductLibraryActivator.getConnection()) {
            System.out.println("save the product");

            int localRepositoryId = saveLocalRepositoryFolderPath(localRepositoryFolderPath, connection);

            int productId = insertProduct(productToSave, productFolderPath, localRepositoryId, connection);

            System.out.println(" saved product id = " + productId);

            insertRemoteProductAttributes(productId, productToSave.getAttributes(), connection);
        }
    }

    private static int saveLocalRepositoryFolderPath(Path localRepositoryFolderPath, Connection connection) throws SQLException {
        String localRepositoryPath = localRepositoryFolderPath.toString();
        int localRepositoryId = 0;
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id FROM ")
                    .append(DatabaseTableNames.LOCAL_REPOSITORIES)
                    .append(" WHERE LOWER(folder_path) = '")
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

    private static int insertProduct(RepositoryProduct productToSave, Path productFolderPath, int localRepositoryId, Connection connection)
                                     throws SQLException, IOException {

        FileTime fileTime = Files.getLastModifiedTime(productFolderPath);
        long sizeInBytes = FileIOUtils.computeFileSize(productFolderPath);

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ")
                .append(DatabaseTableNames.PRODUCTS)
                .append(" (name, type, local_repository_id, local_path, entry_point, size_in_bytes, acquisition_date")
                .append(", last_modified_date, geometry, data_format_type_id, pixel_type_id, sensor_type_id) VALUES ('")
                .append(productToSave.getName())
                .append("', '")
                .append("product_type") //TODO Jean set the product type
                .append("', ")
                .append(localRepositoryId)
                .append(", '")
                .append(productFolderPath.toString())
                .append("', '")
                .append(productToSave.getEntryPoint())
                .append("', ")
                .append(sizeInBytes)
                .append(", ")
                .append("?")
                .append(", ")
                .append("?")
                .append(", '")
                .append(productToSave.getGeometry())
                .append("', ")
                .append(productToSave.getDataFormatType().getValue())
                .append(", ")
                .append(productToSave.getPixelType().getValue())
                .append(", ")
                .append(productToSave.getSensorType().getValue())
                .append(")");
        System.out.println("sql='"+sql.toString()+"'");

        int productId;
        try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            statement.setDate(1, new java.sql.Date(productToSave.getAcquisitionDate().getTime()));
            statement.setDate(2, new java.sql.Date(fileTime.toMillis()));

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
