package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.FileIOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 9/10/2019.
 */
public class ScanLocalRepositoryFolderHelper extends AddLocalRepositoryFolderHelper {

    private static final Logger logger = Logger.getLogger(ScanLocalRepositoryFolderHelper.class.getName());

    public ScanLocalRepositoryFolderHelper() {
    }

    @Override
    protected void missingProductGeoCoding(Path path, Product product) throws IOException {
        super.missingProductGeoCoding(path, product);

        if (logger.isLoggable(Level.FINE)) {
            if (Files.isDirectory(path)) {
                logger.log(Level.FINE, "Delete the folder '"+path.toString()+"' because the product does not contain the geo-coding.");
            } else {
                logger.log(Level.FINE, "Delete the file '"+path.toString()+"' because the product does not contain the geo-coding.");
            }
        }
        FileIOUtils.deleteFolder(path);
    }

    @Override
    protected void invalidProduct(Path path) throws IOException {
        super.invalidProduct(path);

        if (logger.isLoggable(Level.FINE)) {
            if (Files.isDirectory(path)) {
                logger.log(Level.FINE, "Delete the folder '"+path.toString()+"' because it does not represent a valid product.");
            } else {
                logger.log(Level.FINE, "Delete the file '"+path.toString()+"' because it does not represent a valid product.");
            }
        }
        FileIOUtils.deleteFolder(path);
    }

    public List<SaveProductData> scanValidProductsFromFolder(LocalRepositoryFolder localRepositoryFolder) throws IOException, SQLException {
        List<SaveProductData> savedProducts = null;
        if (Files.exists(localRepositoryFolder.getPath())) {
            // the local repository folder exists on the disk
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Scan the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
            }

            List<LocalProductMetadata> existingLocalRepositoryProducts;
            try (Connection connection = H2DatabaseAccessor.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    existingLocalRepositoryProducts = ProductLibraryDAL.loadProductRelativePaths(localRepositoryFolder.getId(), statement);
                }
            }

            savedProducts = saveProductsFromFolder(localRepositoryFolder.getPath(), existingLocalRepositoryProducts);
            Set<Integer> savedProductIds = new HashSet<>(savedProducts.size());
            for (int i=0; i<savedProducts.size(); i++) {
                SaveProductData saveProductData = savedProducts.get(i);
                savedProductIds.add(saveProductData.getProductId());
            }
            Set<Integer> deletedProductIds = ProductLibraryDAL.deleteMissingLocalRepositoryProducts(localRepositoryFolder.getId(), savedProductIds);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Deleted " + deletedProductIds.size() + " products from the database corresponding to the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local repository folder '"+localRepositoryFolder.getPath().toString()+"' to scan does not exist.");
            }

            ProductLibraryDAL.deleteLocalRepositoryFolder(localRepositoryFolder);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Deleted the local repository folder folder '" + localRepositoryFolder.getPath().toString()+"' from the database.");
            }
        }
        return savedProducts;
    }
}
