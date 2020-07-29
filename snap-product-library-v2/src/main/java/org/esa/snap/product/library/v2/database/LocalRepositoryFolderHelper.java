package org.esa.snap.product.library.v2.database;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.datamodel.quicklooks.QuicklookGenerator;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.ThreadExecutor;
import org.esa.snap.core.util.ThreadRunnable;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.remote.products.repository.ThreadStatus;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class contains methods to add a local folder in the local repository, scan the local repository.
 *
 * Created by jcoravu on 4/10/2019.
 */
public class LocalRepositoryFolderHelper {

    private static final Logger logger = Logger.getLogger(LocalRepositoryFolderHelper.class.getName());

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final boolean scanRecursively;
    private final boolean generateQuickLookImages;
    private final boolean validateZips;
    private Map<File, String> errorFiles;

    public LocalRepositoryFolderHelper(AllLocalFolderProductsRepository allLocalFolderProductsRepository, boolean scanRecursively,
                                       boolean generateQuickLookImages, boolean testZipFileForErrors) {

        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;

        this.scanRecursively = scanRecursively;
        this.generateQuickLookImages = generateQuickLookImages;
        this.validateZips = testZipFileForErrors;
        this.errorFiles = new HashMap<>();
    }

    protected void finishSavingProduct(SaveProductData saveProductData) {
    }

    public List<SaveProductData> addRepository(Path localRepositoryFolderPath, ThreadStatus threadStatus, ProgressMonitor progressMonitor) throws Exception {
        List<SaveProductData> savedProducts = null;
        if (Files.exists(localRepositoryFolderPath)) {
            // the local repository folder exists on the disk
            ThreadStatus.checkCancelled(threadStatus);

            savedProducts = scanFolderProducts(localRepositoryFolderPath, threadStatus, progressMonitor);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local repository folder '"+localRepositoryFolderPath.toString()+"' does not exist.");
            }
        }
        return savedProducts;
    }

    public boolean scanRepository(LocalRepositoryFolder localRepositoryFolder, ThreadStatus threadStatus, ProgressMonitor progressMonitor) throws Exception {
        boolean deleteLocalFolderRepository;
        if (Files.exists(localRepositoryFolder.getPath())) {
            // the local repository folder exists on the disk
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Scan the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
            }

            // scan the products saved into the database
            Map<Integer, Path> savedDatabaseProductsMap = scanDatabaseProducts(localRepositoryFolder, threadStatus, progressMonitor);

            // scan the local folder
            List<SaveProductData> savedLocalProducts = scanFolderProducts(localRepositoryFolder.getPath(), threadStatus, progressMonitor);

            deleteLocalFolderRepository = (savedDatabaseProductsMap.size() == 0 && savedLocalProducts.size() == 0);
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

    private Map<Integer, Path> scanDatabaseProducts(LocalRepositoryFolder localRepositoryFolder, ThreadStatus threadStatus, ProgressMonitor progressMonitor)
                                                    throws IOException, SQLException, InterruptedException {

        List<LocalProductMetadata> existingLocalRepositoryProducts = this.allLocalFolderProductsRepository.loadRepositoryProductsMetadata(localRepositoryFolder.getId());

        progressMonitor.beginTask("Cleaning up database...", existingLocalRepositoryProducts.size());

        Map<Integer, Path> existingProductsMap = new HashMap<>();
        for (int i = 0; i < existingLocalRepositoryProducts.size(); i++) {
            ThreadStatus.checkCancelled(threadStatus);

            LocalProductMetadata localProductMetadata = existingLocalRepositoryProducts.get(i);
            Path productPath = localRepositoryFolder.getPath().resolve(localProductMetadata.getRelativePath());
            if (Files.exists(productPath)) {
                // the product path exists on the local disk
                existingProductsMap.put(localProductMetadata.getId(), productPath);
            } else {
                // the product path does not exist on the local disk
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "The local product from the path '" + productPath.toString() + "' does not exist any more.");
                }
            }
            progressMonitor.worked(1);
        }

        ThreadStatus.checkCancelled(threadStatus);

        Set<Integer> deletedProductIds = this.allLocalFolderProductsRepository.deleteMissingProducts(localRepositoryFolder.getId(), existingProductsMap.keySet());

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Deleted " + deletedProductIds.size() + " products from the database corresponding to the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.");
        }
        return existingProductsMap;
    }

    private List<SaveProductData> scanFolderProducts(Path localRepositoryFolderPath, ThreadStatus threadStatus, ProgressMonitor progressMonitor)
                                                     throws Exception {

        long startTime = System.currentTimeMillis();

        List<SaveProductData> savedProducts = new ArrayList<>();

        final List<File> foldersList = new ArrayList<>(20);
        foldersList.add(localRepositoryFolderPath.toFile());

        if (this.scanRecursively) {
            ProductFunctions.DirectoryFileFilter folderFilter = new ProductFunctions.DirectoryFileFilter();
            List<File> subfoldersList = collectAllSubfolders(localRepositoryFolderPath.toFile(), folderFilter, 0, progressMonitor);
            foldersList.addAll(subfoldersList);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Scan " + foldersList.size()+" folders to read the local products.");
        }

        final ProductFunctions.ValidProductFileFilter fileFilter = new ProductFunctions.ValidProductFileFilter(false);
        final List<File> fileList = new ArrayList<>(foldersList.size());
        for (File file : foldersList) {
            final File[] files = file.listFiles(fileFilter);
            if (files != null) {
                fileList.addAll(Arrays.asList(files));
            }
            progressMonitor.setTaskName("Collecting " + fileList.size() + " files...");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Process " + fileList.size()+" files to read the local products.");
        }

        List<LocalProductMetadata> existingLocalRepositoryProducts = this.allLocalFolderProductsRepository.loadRepositoryProductsMetadata(localRepositoryFolderPath);

        ThreadStatus.checkCancelled(threadStatus);

        progressMonitor.beginTask("Scanning " + fileList.size() + " files...", fileList.size());

        Map<Integer, Product> productsToGenerateQuickLookImage = new HashMap<>(fileList.size());
        int newProductCount = 0;
        for (int i=0; i<fileList.size(); i++) {
            ThreadStatus.checkCancelled(threadStatus);

            String taskMsg = "Scanning " + i + " of " + fileList.size() + " files ";
            if (newProductCount > 0) {
                taskMsg += "(" + newProductCount + " new products)";
            }
            progressMonitor.setTaskName(taskMsg);

            Path productPath = fileList.get(i).toPath();

            boolean canContinue = true;
            if (this.validateZips) {
                if (ZipUtils.isZip(productPath) && !ZipUtils.isValid(productPath.toFile())) {
                    this.errorFiles.put(productPath.toFile(), "Corrupt zip file");
                    canContinue = false;
                }
            }
            if (canContinue) {
                try {
                    SaveProductData saveProductData = checkUnchangedProduct(localRepositoryFolderPath, existingLocalRepositoryProducts, productPath);

                    ThreadStatus.checkCancelled(threadStatus);

                    if (saveProductData == null) {
                        // read and save the product into the database
                        Product product = readProduct(productPath);
                        if (product != null) {
                            // the product has been read from the local folder
                            boolean canDisposeProduct = true;
                            try {
                                ThreadStatus.checkCancelled(threadStatus);

                                Polygon2D polygon2D = buildProductPolygon(product);
                                BufferedImage quickLookImage = null; // no quick look image when saving the product
                                saveProductData = this.allLocalFolderProductsRepository.saveLocalProduct(product, quickLookImage, polygon2D, productPath, localRepositoryFolderPath);
                                // the product has been saved into the database
                                newProductCount++;
                                savedProducts.add(saveProductData);
                                finishSavingProduct(saveProductData);

                                ThreadStatus.checkCancelled(threadStatus);

                                if (this.generateQuickLookImages) {
                                    productsToGenerateQuickLookImage.put(saveProductData.getProductId(), product);
                                    canDisposeProduct = false;
                                }
                            } finally {
                                if (canDisposeProduct) {
                                    product.dispose();
                                }
                            }
                        }
                    } else {
                        // unchanged product
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "The local product from the path '"+productPath.toString()+"' is unchanged.");
                        }
                        savedProducts.add(saveProductData);
                        if (this.generateQuickLookImages && !this.allLocalFolderProductsRepository.existsProductQuickLookImage(saveProductData.getProductId())) {
                            Product product = readProduct(productPath);
                            if (product != null) {
                                productsToGenerateQuickLookImage.put(saveProductData.getProductId(), product);
                            }
                        }
                    }
                } catch (Exception exception) {
                    this.errorFiles.put(productPath.toFile(), "Product unreadable");
                    logger.log(Level.SEVERE, "Failed to save the local product from the path '" + productPath.toString() + "'.", exception);
                }
            }
            progressMonitor.worked(1);
        }

        if (logger.isLoggable(Level.FINE)) {
            double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
            StringBuilder message = new StringBuilder();
            message.append("Finish reading the local products: local repository folder :")
                    .append(localRepositoryFolderPath.toString())
                    .append(", process folder count : ")
                    .append(foldersList.size())
                    .append(", process file count : ")
                    .append(fileList.size())
                    .append(", error file count: ")
                    .append(this.errorFiles.size())
                    .append(", new product count : ")
                    .append(newProductCount)
                    .append(", quick look image count to generate: ")
                    .append(productsToGenerateQuickLookImage.size())
                    .append(", elapsed time: ")
                    .append(elapsedSeconds)
                    .append(" seconds.");
            logger.log(Level.FINE, message.toString());
        }

        if (this.generateQuickLookImages) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Generate the quick look images for " + productsToGenerateQuickLookImage.size()+" local products.");
            }

            if (productsToGenerateQuickLookImage.size() > 0) {
                long quickLookImagesStartTime = System.currentTimeMillis();

                generateQuickLookImages(productsToGenerateQuickLookImage, threadStatus, progressMonitor);

                if (logger.isLoggable(Level.FINE)) {
                    double elapsedSeconds = (System.currentTimeMillis() - quickLookImagesStartTime) / 1000.0d;
                    StringBuilder message = new StringBuilder();
                    message.append("Finish generating the quick look images: local repository folder :")
                            .append(localRepositoryFolderPath.toString())
                            .append(", quick look image count: ")
                            .append(productsToGenerateQuickLookImage.size())
                            .append(", elapsed time: ")
                            .append(elapsedSeconds)
                            .append(" seconds.");
                    logger.log(Level.FINE, message.toString());
                }
            }
        }

        return savedProducts;
    }

    public Map<File, String> getErrorFiles() {
        return errorFiles;
    }

    private Product readProduct(Path productPath) throws IOException {
        Product product = ProductIO.readProduct(productPath.toFile());
        if (product == null) {
            // the local product has not been read
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The path '"+productPath.toString()+"' does not represent a valid product.");
            }
            return null; // no product to return
        } else if (product.getSceneGeoCoding() == null) {
            // the local product has not geo coding
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local product from the path '"+productPath.toString()+"' does not contain the geo-coding associated with the scene raster.");
            }
            product.dispose();
            return null; // no product to return
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The path '"+productPath.toString()+"' is a valid product with geo coding.");
            }
            return product; // the local product has geo coding
        }
    }

    private void generateQuickLookImages(Map<Integer, Product> productsToGenerateQuickLookImage, ThreadStatus threadStatus, ProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask("Generating Quicklooks...", productsToGenerateQuickLookImage.size());
        final ThreadExecutor executor = new ThreadExecutor();

        int index = 0;
        for (Map.Entry<Integer, Product> entry : productsToGenerateQuickLookImage.entrySet()) {
            int savedProductId  = entry.getKey().intValue();
            Product product = entry.getValue();
            index++;
            progressMonitor.setTaskName("Generating Quicklook... " + index + " of " + productsToGenerateQuickLookImage.size());

            ThreadStatus.checkCancelled(threadStatus);

            final StatusProgressMonitor qlPM = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
            qlPM.beginTask("Creating quicklook " + product.getName() + "... ", 100);

            final ThreadRunnable worker = new ThreadRunnable() {
                @Override
                public void process() {
                    try {
                        QuicklookGenerator quicklookGenerator = new QuicklookGenerator();
                        Band[] quicklookBands = quicklookGenerator.findQuicklookBands(product);
                        BufferedImage quickLookImage;
                        if (quicklookBands == null) {
                            quickLookImage = quicklookGenerator.createQuickLookFromBrowseProduct(product);
                        } else {
                            quickLookImage = quicklookGenerator.createQuickLookImage(product, quicklookBands, qlPM);
                        }
                        if (quickLookImage != null) {
                            allLocalFolderProductsRepository.writeQuickLookImage(savedProductId, quickLookImage);
                        }
                    } catch (Exception exception) {
                        logger.log(Level.SEVERE, "Failed to create the quick look image for product '" + product.getName() + "'.", exception);
                    } finally {
                        product.dispose();
                        qlPM.done();
                    }
                }
            };
            executor.execute(worker);

            progressMonitor.worked(1);
        }
        executor.complete();
    }

    private static List<File> collectAllSubfolders(final File dir, ProductFunctions.DirectoryFileFilter folderFilter, int count, final ProgressMonitor pm) {
        final List<File> dirList = new ArrayList<>(20);

        final File[] subDirs = dir.listFiles(folderFilter);
        if (subDirs != null && subDirs.length > 0) {
            count += subDirs.length;
            pm.setTaskName("Collecting " + count + " folders...");

            for (final File subDir : subDirs) {
                dirList.add(subDir);

                List<File> dirs = collectAllSubfolders(subDir, folderFilter, count, pm);
                dirList.addAll(dirs);
            }
        }
        return dirList;
    }

    private static LocalProductMetadata foundLocalProductMetadata(Path localRepositoryFolderPath, List<LocalProductMetadata> existingLocalRepositoryProducts, Path productPathToCheck) {
        for (int i = 0; i<existingLocalRepositoryProducts.size(); i++) {
            LocalProductMetadata localProductMetadata = existingLocalRepositoryProducts.get(i);
            Path path = localRepositoryFolderPath.resolve(localProductMetadata.getRelativePath());
            if (path.compareTo(productPathToCheck) == 0) {
                return localProductMetadata; // the same product path
            }
        }
        return null;
    }

    private static SaveProductData checkUnchangedProduct(Path localRepositoryFolderPath, List<LocalProductMetadata> existingLocalRepositoryProducts, Path productPathToCheck)
            throws IOException {

        LocalProductMetadata localProductMetadata = foundLocalProductMetadata(localRepositoryFolderPath, existingLocalRepositoryProducts, productPathToCheck);
        SaveProductData saveProductData = null;
        if (localProductMetadata != null) {
            // the product already exists into the database
            FileTime fileTime = Files.getLastModifiedTime(productPathToCheck);
            if (fileTime.toMillis() == localProductMetadata.getLastModifiedDate().getTime()) {
                // unchanged product
                saveProductData = new SaveProductData(localProductMetadata.getId(), null, null, null);
            }
        }
        return saveProductData;
    }

    private static Polygon2D buildProductPolygon(Product product) {
        int productWidth = product.getSceneRasterWidth();
        int productHeight = product.getSceneRasterHeight();
        PixelPos[] pixelPositions = new PixelPos[5];
        pixelPositions[0] = new PixelPos(0.0d, 0.0d);
        pixelPositions[1] = new PixelPos(productWidth, 0.0d);
        pixelPositions[2] = new PixelPos(productWidth, productHeight);
        pixelPositions[3] = new PixelPos(0.0d, productHeight);
        pixelPositions[4] = new PixelPos(0.0d, 0.0d);

        GeoPos[] geographicalPositions = new GeoPos[pixelPositions.length];
        GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
        for (int i = 0; i < pixelPositions.length; i++) {
            geographicalPositions[i] = sceneGeoCoding.getGeoPos(pixelPositions[i], null);
        }
        ProductUtils.normalizeGeoPolygon(geographicalPositions);

        Polygon2D polygon2D = new Polygon2D();
        for (int i = 0; i < geographicalPositions.length; i++) {
            polygon2D.append(geographicalPositions[i].getLon(), geographicalPositions[i].getLat());
        }
        return polygon2D;
    }
}
