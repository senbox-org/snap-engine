package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;
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
 * The local repository provider used to access the local products.
 *
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

        boolean required = false;

        parameters.add(new RepositoryQueryParameter(START_DATE_PARAMETER, Date.class, "Start date", null, required, null));
        parameters.add(new RepositoryQueryParameter(END_DATE_PARAMETER, Date.class, "End date", null, required, null));

        SensorType[] sensorTypes = SensorType.values();
        String sensorValues[] = new String[sensorTypes.length];
        for (int i = 0; i < sensorTypes.length; i++) {
            sensorValues[i] = sensorTypes[i].getName();
        }
        parameters.add(new RepositoryQueryParameter(SENSOR_TYPE_PARAMETER, String.class, "Sensor", null, required, sensorValues));
        parameters.add(new RepositoryQueryParameter(ATTRIBUTES_PARAMETER, Attribute.class, "Attributes", null, required, null));
        parameters.add(new RepositoryQueryParameter(FOOT_PRINT_PARAMETER, Rectangle2D.class, "Area of interest", null, required, null));

        return parameters;
    }

    public List<RepositoryProduct> loadProductList(LocalRepositoryFolder localRepositoryFolder, String remoteMissionName, Map<String, Object> parameterValues)
                                                   throws SQLException, IOException {

        return LocalRepositoryDatabaseLayer.loadProductList(localRepositoryFolder, remoteMissionName, parameterValues, this.databaseParameters);
    }

    public SaveProductData saveRemoteProduct(RepositoryProduct productToSave, Path productPath, String remoteRepositoryName, Path localRepositoryFolderPath, Product product)
                                             throws IOException, SQLException {

        return LocalRepositoryDatabaseLayer.saveRemoteProduct(productToSave, productPath, remoteRepositoryName, localRepositoryFolderPath, product, this.databaseParameters);
    }

    public SaveProductData saveLocalProduct(Product productToSave, BufferedImage quickLookImage, AbstractGeometry2D polygon2D, Path productPath, Path localRepositoryFolderPath)
                                            throws IOException, SQLException {

        return LocalRepositoryDatabaseLayer.saveLocalProduct(productToSave, quickLookImage, polygon2D, productPath, localRepositoryFolderPath, this.databaseParameters);
    }

    public boolean existsProductQuickLookImage(int productId) {
        return LocalRepositoryDatabaseLayer.existsProductQuickLookImage(productId, this.databaseParameters.getParentFolderPath());
    }

    public void writeQuickLookImage(int productId, BufferedImage quickLookImage) throws IOException {
        LocalRepositoryDatabaseLayer.writeQuickLookImage(productId, quickLookImage, this.databaseParameters.getParentFolderPath());
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

    public void updateProductPath(LocalRepositoryProduct productToUpdate, Path productPath, Path localRepositoryFolderPath) throws SQLException, IOException {
        LocalRepositoryDatabaseLayer.updateProductPath(productToUpdate, productPath, localRepositoryFolderPath, this.databaseParameters);
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
                Map<Short, Set<String>> remoteAttributeNamesPerMission = LocalRepositoryDatabaseLayer.loadRemoteAttributesNamesPerMission(statement);
                Set<String> localAttributeNames = LocalRepositoryDatabaseLayer.loadLocalAttributesNames(statement);
                List<String> remoteMissionNames = LocalRepositoryDatabaseLayer.loadUniqueRemoteMissionNames(statement);
                return new LocalRepositoryParameterValues(remoteMissionNames, remoteAttributeNamesPerMission, localAttributeNames, localRepositoryFolders);
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
