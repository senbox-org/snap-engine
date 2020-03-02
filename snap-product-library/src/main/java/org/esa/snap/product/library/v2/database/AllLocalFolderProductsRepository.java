package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.AbstractGeometry2D;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.SensorType;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class AllLocalFolderProductsRepository {

    public static final String START_DATE_PARAMETER = "startDate";
    public static final String END_DATE_PARAMETER = "endDate";
    public static final String FOOT_PRINT_PARAMETER = "footprint";
    public static final String SENSOR_TYPE_PARAMETER = "sensorType";
    public static final String ATTRIBUTES_PARAMETER = "attributes";

    private final H2DatabaseParameters databaseParameters;

    public AllLocalFolderProductsRepository(H2DatabaseParameters databaseParameters) {
        if (databaseParameters == null) {
            throw new NullPointerException("The database parameters are null.");
        }
        this.databaseParameters = databaseParameters;
    }

    public List<RepositoryQueryParameter> getParameters() {
        List<RepositoryQueryParameter> parameters = new ArrayList<RepositoryQueryParameter>();

        parameters.add(new RepositoryQueryParameter(START_DATE_PARAMETER, Date.class, "Start date", null, false, null));
        parameters.add(new RepositoryQueryParameter(END_DATE_PARAMETER, Date.class, "End date", null, false, null));

        SensorType[] sensorTypes = SensorType.values();
        String sensorValues[] = new String[sensorTypes.length];
        for (int i = 0; i < sensorTypes.length; i++) {
            sensorValues[i] = sensorTypes[i].getName();
        }
        parameters.add(new RepositoryQueryParameter(SENSOR_TYPE_PARAMETER, String.class, "Sensor", null, false, sensorValues));
        parameters.add(new RepositoryQueryParameter(ATTRIBUTES_PARAMETER, Attribute.class, "Attributes", null, false, null));
        parameters.add(new RepositoryQueryParameter(FOOT_PRINT_PARAMETER, Rectangle2D.class, "Area of interest", null, false, null));

        return parameters;
    }

    public List<RepositoryProduct> loadProductList(LocalRepositoryFolder localRepositoryFolder, String remoteMissionName, Map<String, Object> parameterValues)
                                                   throws SQLException, IOException {

        return LocalRepositoryDatabaseLayer.loadProductList(localRepositoryFolder, remoteMissionName, parameterValues, this.databaseParameters);
    }

    public SaveDownloadedProductData saveProduct(RepositoryProduct productToSave, Path productPath, String remoteRepositoryName, Path localRepositoryFolderPath)
            throws IOException, SQLException {

        return LocalRepositoryDatabaseLayer.saveProduct(productToSave, productPath, remoteRepositoryName, localRepositoryFolderPath, this.databaseParameters);
    }

    public SaveProductData saveProduct(Product productToSave, BufferedImage quickLookImage, AbstractGeometry2D polygon2D, Path productPath, Path localRepositoryFolderPath)
            throws IOException, SQLException {

        return LocalRepositoryDatabaseLayer.saveProduct(productToSave, quickLookImage, polygon2D, productPath, localRepositoryFolderPath, this.databaseParameters);
    }

    public Set<Integer> deleteMissingProducts(short localRepositoryId, Set<Integer> savedProductIds) throws SQLException {
        return LocalRepositoryDatabaseLayer.deleteMissingLocalRepositoryProducts(localRepositoryId, savedProductIds, this.databaseParameters);
    }

    public List<LocalProductMetadata> loadRepositoryProductsMetadata(short localRepositoryId) throws SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection(this.databaseParameters)) {
            try (Statement statement = connection.createStatement()) {
                return LocalRepositoryDatabaseLayer.loadProductRelativePaths(localRepositoryId, statement);
            }
        }
    }

    public void deleteRepositoryFolder(LocalRepositoryFolder localRepositoryFolder) throws SQLException {
        LocalRepositoryDatabaseLayer.deleteLocalRepositoryFolder(localRepositoryFolder, this.databaseParameters);
    }

    public void deleteProduct(LocalRepositoryProduct repositoryProduct) throws SQLException {
        LocalRepositoryDatabaseLayer.deleteProduct(repositoryProduct, this.databaseParameters);
    }

    public List<LocalRepositoryFolder> loadRepositoryFolders() throws SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection(this.databaseParameters)) {
            try (Statement statement = connection.createStatement()) {
                return LocalRepositoryDatabaseLayer.loadLocalRepositoryFolders(statement);
            }
        }
    }

    public LocalRepositoryParameterValues loadParameterValues() throws SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection(this.databaseParameters)) {
            try (Statement statement = connection.createStatement()) {
                List<LocalRepositoryFolder> localRepositoryFolders = LocalRepositoryDatabaseLayer.loadLocalRepositoryFolders(statement);
                Map<Short, Set<String>> attributeNamesPerMission = LocalRepositoryDatabaseLayer.loadAttributesNamesPerMission(statement);
                List<String> remoteMissionNames = LocalRepositoryDatabaseLayer.loadUniqueRemoteMissionNames(statement);
                return new LocalRepositoryParameterValues(remoteMissionNames, attributeNamesPerMission, localRepositoryFolders);
            }
        }
    }

    public List<LocalProductMetadata> loadRepositoryProductsMetadata(Path localRepositoryFolderPath) throws SQLException {
        try (Connection connection = H2DatabaseAccessor.getConnection(this.databaseParameters)) {
            try (Statement statement = connection.createStatement()) {
                Short localRepositoryId = LocalRepositoryDatabaseLayer.loadLocalRepositoryId(localRepositoryFolderPath, statement);
                List<LocalProductMetadata> existingLocalRepositoryProducts;
                if (localRepositoryId == null) {
                    existingLocalRepositoryProducts = Collections.emptyList();
                } else {
                    existingLocalRepositoryProducts = LocalRepositoryDatabaseLayer.loadProductRelativePaths(localRepositoryId.shortValue(), statement);
                }
                return existingLocalRepositoryProducts;
            }
        }
    }
}
