package org.esa.snap.product.library.v2.database;

import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.remote.products.repository.ThreadStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jcoravu on 9/10/2019.
 */
public class ScanLocalRepositoryFolderHelper extends AbstractLocalRepositoryFolderHelper {

    private static final Logger logger = Logger.getLogger(ScanLocalRepositoryFolderHelper.class.getName());

    public ScanLocalRepositoryFolderHelper(AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
        super(allLocalFolderProductsRepository);
    }

    public boolean scanValidProductsFromFolder(LocalRepositoryFolder localRepositoryFolder, ThreadStatus threadStatus)
                                               throws IOException, SQLException, InterruptedException {

        boolean deleteLocalFolderRepository;
        if (Files.exists(localRepositoryFolder.getPath())) {
            // the local repository folder exists on the disk
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Scan the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
            }

            // scan the products saved into the database
            Map<Integer, Path> savedDatabaseProductsMap = scanDatabaseProducts(localRepositoryFolder, threadStatus);

            // scan the local folder
            Map<Integer, Path> savedLocalFolderProductsMap = scanLocalFolderProducts(localRepositoryFolder, savedDatabaseProductsMap.values(), threadStatus);

            deleteLocalFolderRepository = (savedDatabaseProductsMap.size() == 0 && savedLocalFolderProductsMap.size() == 0);
        } else {
            // the local repository folder does not exist on the disk
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local repository folder '" + localRepositoryFolder.getPath().toString() + "' to scan does not exist.");
            }
            deleteLocalFolderRepository = true;
        }

        if (deleteLocalFolderRepository) {
            this.allLocalFolderProductsRepository.deleteRepositoryFolder(localRepositoryFolder);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Deleted the local repository folder folder '" + localRepositoryFolder.getPath().toString() + "' from the database.");
            }
        }

        return deleteLocalFolderRepository;
    }

    private Map<Integer, Path> scanDatabaseProducts(LocalRepositoryFolder localRepositoryFolder, ThreadStatus threadStatus)
                                                    throws IOException, SQLException, InterruptedException {

        Map<Integer, Path> savedProductsMap = new HashMap<>();
        List<LocalProductMetadata> existingLocalRepositoryProducts = this.allLocalFolderProductsRepository.loadRepositoryProductsMetadata(localRepositoryFolder.getId());
        for (int i = 0; i < existingLocalRepositoryProducts.size(); i++) {
            LocalProductMetadata localProductMetadata = existingLocalRepositoryProducts.get(i);
            Path productPath = localRepositoryFolder.getPath().resolve(localProductMetadata.getRelativePath());
            if (Files.exists(productPath)) {
                // the product path exists on the local disk
                ThreadStatus.checkCancelled(threadStatus);

                FileTime fileTime = Files.getLastModifiedTime(productPath);
                if (fileTime.toMillis() == localProductMetadata.getLastModifiedDate().getTime()) {
                    // unchanged product
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "The local product from the path '" + productPath.toString() + "' is unchanged.");
                    }
                    savedProductsMap.put(localProductMetadata.getId(), productPath);
                } else {
                    // the product has been changed on the local disk and read, save the product into the database
                    SaveProductData saveProductData = readAndSaveProduct(localRepositoryFolder.getPath(), productPath, threadStatus);
                    if (saveProductData == null) {
                        // no product has been loaded from the path
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "The existing local product from the path '" + productPath.toString() + "' is invalid.");
                        }
                    } else {
                        // the product has been saved into the database
                        savedProductsMap.put(saveProductData.getProductId(), productPath);
                        finishSavingProduct(saveProductData);
                    }
                }
            } else {
                // the product path does not exist on the local disk
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "The local product from the path '" + productPath.toString() + "' does not exist any more.");
                }
            }
        }

        Set<Integer> deletedProductIds = this.allLocalFolderProductsRepository.deleteMissingProducts(localRepositoryFolder.getId(), savedProductsMap.keySet());

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Deleted " + deletedProductIds.size() + " products from the database corresponding to the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
        }
        return savedProductsMap;
    }

    private Map<Integer, Path> scanLocalFolderProducts(LocalRepositoryFolder localRepositoryFolder, Collection<Path> savedDatabaseProducts, ThreadStatus threadStatus)
                                                       throws IOException, InterruptedException {

        Map<Integer, Path> savedProductsMap = new HashMap<>();

        boolean scanOnlyFirstLevel = true;
        Stack<Path> stack = new Stack<>();
        stack.push(localRepositoryFolder.getPath());
        while (!stack.isEmpty()) {
            Path currentPath = stack.pop();

            ThreadStatus.checkCancelled(threadStatus);
            if (Files.isDirectory(currentPath)) {
                // the current path is a folder
                try (Stream<Path> stream = Files.list(currentPath)) {

                    ThreadStatus.checkCancelled(threadStatus);

                    Iterator<Path> it = stream.iterator();
                    while (it.hasNext()) {

                        ThreadStatus.checkCancelled(threadStatus);

                        Path productPath = it.next();
                        try {
                            boolean foundProduct = false;
                            for (Path savedProductPath : savedDatabaseProducts) {
                                if (savedProductPath.compareTo(productPath) == 0) {
                                    foundProduct = true; // the same product path
                                    break;
                                }
                            }

                            ThreadStatus.checkCancelled(threadStatus);

                            if (!foundProduct) {
                                // read and save the product into the database
                                SaveProductData saveProductData = readAndSaveProduct(localRepositoryFolder.getPath(), productPath, threadStatus);
                                if (saveProductData == null) {
                                    // no product has been loaded from the path
                                    if (logger.isLoggable(Level.FINE)) {
                                        logger.log(Level.FINE, "The local product to import from the path '" + productPath.toString() + "' is invalid.");
                                    }
                                    if (!scanOnlyFirstLevel) {
                                        if (Files.isDirectory(productPath)) {
                                            stack.push(productPath);
                                        }
                                    }
                                } else {
                                    // the product has been saved into the database
                                    savedProductsMap.put(saveProductData.getProductId(), productPath);
                                    finishSavingProduct(saveProductData);
                                }
                            }
                        } catch (Exception exception) {
                            logger.log(Level.SEVERE, "Failed to save the local product from the path '" + productPath.toString() + "'.", exception);
                        }
                    }
                }
            } else {
                // the current path is not a folder
                throw new IllegalStateException("The path '" + currentPath.toString() + "' is not a folder.");
            }
        }

        return savedProductsMap;
    }
}
