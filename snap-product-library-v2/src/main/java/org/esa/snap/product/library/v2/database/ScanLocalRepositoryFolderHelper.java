package org.esa.snap.product.library.v2.database;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.remote.products.repository.ThreadStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
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

    public ScanLocalRepositoryFolderHelper(AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
        super(allLocalFolderProductsRepository);
    }

    public List<SaveProductData> scanValidProductsFromFolder(LocalRepositoryFolder localRepositoryFolder, ThreadStatus threadStatus)
                                                             throws IOException, SQLException, InterruptedException {

        List<SaveProductData> savedProducts = null;
        if (Files.exists(localRepositoryFolder.getPath())) {
            // the local repository folder exists on the disk
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Scan the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
            }

            List<LocalProductMetadata> existingLocalRepositoryProducts = this.allLocalFolderProductsRepository.loadRepositoryProductsMetadata(localRepositoryFolder.getId());

            savedProducts = saveProductsFromFolder(localRepositoryFolder.getPath(), existingLocalRepositoryProducts, true, threadStatus);
            Set<Integer> savedProductIds = new HashSet<>(savedProducts.size());
            for (int i=0; i<savedProducts.size(); i++) {
                SaveProductData saveProductData = savedProducts.get(i);
                savedProductIds.add(saveProductData.getProductId());
            }
            Set<Integer> deletedProductIds = this.allLocalFolderProductsRepository.deleteMissingProducts(localRepositoryFolder.getId(), savedProductIds);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Deleted " + deletedProductIds.size() + " products from the database corresponding to the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
            }
        } else {
            // the local repository folder does not exist on the disk
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local repository folder '"+localRepositoryFolder.getPath().toString()+"' to scan does not exist.");
            }

            this.allLocalFolderProductsRepository.deleteRepositoryFolder(localRepositoryFolder);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Deleted the local repository folder folder '" + localRepositoryFolder.getPath().toString()+"' from the database.");
            }
        }
        return savedProducts;
    }
}
