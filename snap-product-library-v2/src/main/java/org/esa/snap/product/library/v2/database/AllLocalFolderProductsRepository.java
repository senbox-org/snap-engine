package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.SensorType;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
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

    public AllLocalFolderProductsRepository() {
        this.databaseParameters = new H2DatabaseParameters(DataAccess.getDatabaseFolder());
    }

    public AllLocalFolderProductsRepository(H2DatabaseParameters parameters) {
        this.databaseParameters = parameters;
    }

    public List<RepositoryQueryParameter> getParameters() {
        List<RepositoryQueryParameter> parameters = new ArrayList<>();

        final boolean required = false;

        parameters.add(new RepositoryQueryParameter(START_DATE_PARAMETER, Date.class, "Start date", null, required, null));
        parameters.add(new RepositoryQueryParameter(END_DATE_PARAMETER, Date.class, "End date", null, required, null));

        SensorType[] sensorTypes = SensorType.values();
        String[] sensorValues = new String[sensorTypes.length];
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

        return DataAccess.getProducts(localRepositoryFolder, remoteMissionName, parameterValues);
    }

    public SaveProductData saveRemoteProduct(RepositoryProduct productToSave, Path productPath, String remoteRepositoryName, Path localRepositoryFolderPath, Product product)
                                             throws IOException, SQLException {

        return DataAccess.saveRemoteProduct(productToSave, productPath, remoteRepositoryName, localRepositoryFolderPath, product);
    }

    public SaveProductData saveLocalProduct(Product productToSave, BufferedImage quickLookImage, AbstractGeometry2D polygon2D, Path productPath, Path localRepositoryFolderPath)
                                            throws IOException, SQLException {

        return DataAccess.saveLocalProduct(productToSave, quickLookImage, polygon2D, productPath, localRepositoryFolderPath);
    }

    public boolean existsProductQuickLookImage(int productId) {
        return DataAccess.existsProductQuickLookImage(productId, this.databaseParameters.getParentFolderPath());
    }

    public void writeQuickLookImage(int productId, BufferedImage quickLookImage) throws IOException {
        DataAccess.writeQuickLookImage(productId, quickLookImage, this.databaseParameters.getParentFolderPath());
    }

    public Set<Integer> deleteMissingProducts(short localRepositoryId, Set<Integer> savedProductIds) throws SQLException {
        return DataAccess.deleteMissingLocalRepositoryProducts(localRepositoryId, savedProductIds);
    }

    public List<LocalProductMetadata> loadRepositoryProductsMetadata(short localRepositoryId) throws SQLException {
        return DataAccess.loadProductRelativePaths(localRepositoryId);
    }

    public void deleteRepositoryFolder(LocalRepositoryFolder localRepositoryFolder) throws SQLException {
        DataAccess.deleteLocalRepositoryFolder(localRepositoryFolder);
    }

    public void deleteProduct(LocalRepositoryProduct repositoryProduct) throws SQLException {
        DataAccess.deleteProduct(repositoryProduct);
    }

    public void updateProductPath(LocalRepositoryProduct productToUpdate, Path productPath, Path localRepositoryFolderPath) throws SQLException, IOException {
        DataAccess.updateProductPath(productToUpdate, productPath, localRepositoryFolderPath);
    }

    public List<LocalRepositoryFolder> loadRepositoryFolders() throws SQLException {
        return DataAccess.listLocalRepositoryFolders();
    }

    public LocalRepositoryFolder saveLocalRepositoryFolder(Path path) {
        try {
            return DataAccess.saveLocalRepositoryFolderPath(path, null);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public LocalRepositoryParameterValues loadParameterValues() throws SQLException {
        final List<LocalRepositoryFolder> localRepositoryFolders = DataAccess.listLocalRepositoryFolders();
        final Map<Short, Set<String>> remoteAttributeNamesPerMission = DataAccess.listRemoteAttributeNames();
        final Set<String> localAttributeNames = DataAccess.listLocalAttributeNames();
        final List<String> remoteMissionNames = DataAccess.listRemoteMissionNames();
        return new LocalRepositoryParameterValues(remoteMissionNames, remoteAttributeNamesPerMission, localAttributeNames, localRepositoryFolders);
    }

    public List<LocalProductMetadata> loadRepositoryProductsMetadata(Path localRepositoryFolderPath) throws SQLException {
        final Short localRepositoryId = DataAccess.getLocalRepositoryId(localRepositoryFolderPath);
        List<LocalProductMetadata> existingLocalRepositoryProducts;
        if (localRepositoryId == null) {
            existingLocalRepositoryProducts = Collections.emptyList();
        } else {
            existingLocalRepositoryProducts = DataAccess.loadProductRelativePaths(localRepositoryId);
        }
        return existingLocalRepositoryProducts;
    }
}
